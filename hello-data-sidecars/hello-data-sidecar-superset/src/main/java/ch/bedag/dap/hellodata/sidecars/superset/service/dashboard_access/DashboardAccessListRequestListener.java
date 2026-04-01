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
import java.util.List;

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

    private List<SupersetLog> getSupersetLogResponse(JsonArray filter, int page, int pageSize) throws URISyntaxException, IOException {
        JsonObject logFilter = new JsonObject();
        logFilter.addProperty("col", "action");
        logFilter.addProperty("opr", "eq");
        logFilter.addProperty("value", "log");
        filter.add(logFilter);
        JsonObject dashboardIdFilter = new JsonObject();
        dashboardIdFilter.addProperty("col", "dashboard_id");
        dashboardIdFilter.addProperty("opr", "gt");
        dashboardIdFilter.addProperty("value", 0);
        filter.add(dashboardIdFilter);

        SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
        SupersetLogResponse supersetLogResponse = supersetClient.logsFiltered(filter, page, pageSize);
        return supersetLogResponse.getResult().stream().filter(logEntry -> logEntry.getJson().contains("mount_dashboard")).toList();
    }
}
