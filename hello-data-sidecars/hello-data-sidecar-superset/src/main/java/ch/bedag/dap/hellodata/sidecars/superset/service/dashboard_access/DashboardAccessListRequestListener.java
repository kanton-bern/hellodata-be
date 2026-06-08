package ch.bedag.dap.hellodata.sidecars.superset.service.dashboard_access;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.logs.response.superset.SupersetLog;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.logs.response.superset.SupersetLogResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardAccessListRequestListener {

    private static final int DEFAULT_PAGE_SIZE = 1000;

    private final Connection natsConnection;
    private final SupersetClientProvider supersetClientProvider;
    private final ObjectMapper objectMapper;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @PostConstruct
    public void listenForRequests() {
        String supersetSidecarSubject = SlugifyUtil.slugify(instanceName + RequestReplySubject.GET_DASHBOARD_ACCESS_LIST.getSubject());
        log.debug("/*-/*- Listening for messages on subject {}", supersetSidecarSubject);
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            log.debug("\t-=-=-=-= Received message from NATS: {}", new String(msg.getData()));
            try {
                String jsonString = new String(msg.getData(), StandardCharsets.UTF_8);
                JsonElement jsonElement = JsonParser.parseString(jsonString);
                JsonArray filter;
                int page = 0;
                int pageSize = DEFAULT_PAGE_SIZE;

                if (jsonElement.isJsonObject()) {
                    JsonObject request = jsonElement.getAsJsonObject();
                    filter = request.has("filters") ? request.getAsJsonArray("filters") : new JsonArray();
                    if (request.has("page")) {
                        page = request.get("page").getAsInt();
                    }
                    if (request.has("pageSize")) {
                        pageSize = request.get("pageSize").getAsInt();
                    }
                } else if (jsonElement.isJsonArray()) {
                    // Backward compatible: legacy callers send a plain JSON array of filters
                    filter = jsonElement.getAsJsonArray();
                } else {
                    throw new IllegalStateException("Expected a JSON array or object but received: " + jsonString);
                }

                List<SupersetLog> logs = getSupersetLogResponse(filter, page, pageSize);
                log.debug("Received {} log entries from Superset (page={}, pageSize={})", logs.size(), page, pageSize);

                ObjectNode responseNode = objectMapper.createObjectNode();
                ArrayNode resultArray = objectMapper.valueToTree(logs);
                responseNode.set("result", resultArray);
                responseNode.put("count", logs.size());

                String result = objectMapper.writeValueAsString(responseNode);
                natsConnection.publish(msg.getReplyTo(), result.getBytes(StandardCharsets.UTF_8));
                msg.ack();
            } catch (URISyntaxException | IOException | RuntimeException e) {
                log.error("Error fetching dashboard access list", e);
                ObjectNode errorResponse = objectMapper.createObjectNode();
                errorResponse.put("error", e.getMessage());
                natsConnection.publish(msg.getReplyTo(), errorResponse.toString().getBytes(StandardCharsets.UTF_8));
                msg.ack();
            }
        });
        dispatcher.subscribe(supersetSidecarSubject);
    }

    private List<SupersetLog> getSupersetLogResponse(JsonArray callerFilters, int page, int pageSize) throws URISyntaxException, IOException {
        try (SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance()) {
            List<SupersetLog> mountDashboardLogs = fetchMountDashboardLogs(supersetClient, callerFilters, page, pageSize);
            List<SupersetLog> dashboardRestApiLogs = fetchDashboardRestApiGetLogs(supersetClient, callerFilters, page, pageSize);
            return deduplicate(mountDashboardLogs, dashboardRestApiLogs);
        }
    }

    /**
     * Fetches action=log entries that contain a mount_dashboard event.
     * On Superset <4.1.3 the dashboard_id column is NULL — the id is parsed from source_id in the JSON payload as a fallback.
     */
    private List<SupersetLog> fetchMountDashboardLogs(SupersetClient supersetClient, JsonArray callerFilters, int page, int pageSize)
            throws URISyntaxException, IOException {
        JsonArray filter = copyFilters(callerFilters);
        JsonObject logFilter = new JsonObject();
        logFilter.addProperty("col", "action");
        logFilter.addProperty("opr", "eq");
        logFilter.addProperty("value", "log");
        filter.add(logFilter);

        SupersetLogResponse response = supersetClient.logsFiltered(filter, page, pageSize);
        return response.getResult().stream()
                .filter(entry -> entry.getJson() != null && entry.getJson().contains("mount_dashboard"))
                .peek(entry -> {
                    if (entry.getDashboardId() == null) {
                        JsonElement jsonEl = JsonParser.parseString(entry.getJson());
                        if (jsonEl.isJsonObject()) {
                            JsonObject obj = jsonEl.getAsJsonObject();
                            if (obj.has("source_id") && !obj.get("source_id").isJsonNull()) {
                                entry.setDashboardId(obj.get("source_id").getAsInt());
                            }
                        }
                    }
                })
                .filter(entry -> entry.getDashboardId() != null)
                .toList();
    }

    /**
     * Fetches action=DashboardRestApi.get entries as a fallback for Superset versions
     * that do not populate dashboard_id on action=log events.
     */
    private List<SupersetLog> fetchDashboardRestApiGetLogs(SupersetClient supersetClient, JsonArray callerFilters, int page, int pageSize)
            throws URISyntaxException, IOException {
        JsonArray filter = copyFilters(callerFilters);
        JsonObject actionFilter = new JsonObject();
        actionFilter.addProperty("col", "action");
        actionFilter.addProperty("opr", "eq");
        actionFilter.addProperty("value", "DashboardRestApi.get");
        filter.add(actionFilter);
        JsonObject dashboardIdFilter = new JsonObject();
        dashboardIdFilter.addProperty("col", "dashboard_id");
        dashboardIdFilter.addProperty("opr", "gt");
        dashboardIdFilter.addProperty("value", 0);
        filter.add(dashboardIdFilter);

        SupersetLogResponse response = supersetClient.logsFiltered(filter, page, pageSize);
        return response.getResult();
    }

    /**
     * Removes DashboardRestApi.get entries that are already represented by a mount_dashboard log entry.
     * Both events fire within seconds of each other for the same visit, so a 60-second window
     * keyed on (userId, dashboardId) is used to detect duplicates.
     */
    private List<SupersetLog> deduplicate(List<SupersetLog> mountDashboardLogs, List<SupersetLog> dashboardRestApiLogs) {
        Set<String> mountDashboardKeys = mountDashboardLogs.stream()
                .map(this::visitKey)
                .collect(Collectors.toSet());

        List<SupersetLog> result = new ArrayList<>(mountDashboardLogs);
        dashboardRestApiLogs.stream()
                .filter(entry -> !mountDashboardKeys.contains(visitKey(entry)))
                .forEach(result::add);
        return result;
    }

    /** Groups events into 60-second buckets per user+dashboard to detect same-visit duplicates. */
    private String visitKey(SupersetLog entry) {
        long bucket = entry.getDttm() != null ? entry.getDttm().toEpochSecond(ZoneOffset.UTC) / 60 : 0;
        return entry.getUserId() + ":" + entry.getDashboardId() + ":" + bucket;
    }

    private JsonArray copyFilters(JsonArray source) {
        JsonArray copy = new JsonArray();
        source.forEach(copy::add);
        return copy;
    }
}
