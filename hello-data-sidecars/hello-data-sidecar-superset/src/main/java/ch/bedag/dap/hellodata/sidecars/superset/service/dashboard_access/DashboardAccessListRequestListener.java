package ch.bedag.dap.hellodata.sidecars.superset.service.dashboard_access;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.logs.response.superset.SupersetLog;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.logs.response.superset.SupersetLogResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
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
                JsonElement jsonElement = JsonParser.parseString(new String(msg.getData(), StandardCharsets.UTF_8));
                List<SupersetLog> logs = getSupersetLogResponse(msg, jsonElement);
                log.debug("Received {} log entries from Superset", logs);
                String result = objectMapper.writeValueAsString(logs);
                natsConnection.publish(msg.getReplyTo(), result.getBytes(StandardCharsets.UTF_8));
                msg.ack();
            } catch (URISyntaxException | IOException | RuntimeException e) {
                log.error("Error fetching query list", e);
                natsConnection.publish(msg.getReplyTo(), e.getMessage().getBytes(StandardCharsets.UTF_8));
            }
        });
        dispatcher.subscribe(supersetSidecarSubject);
    }

    private List<SupersetLog> getSupersetLogResponse(Message msg, JsonElement jsonElement) throws URISyntaxException, IOException {
        JsonArray filter;
        if (jsonElement.isJsonArray()) {
            filter = jsonElement.getAsJsonArray();
        } else {
            throw new IllegalStateException("Expected a JSON array but received: " + new String(msg.getData(), StandardCharsets.UTF_8));
        }
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
        SupersetLogResponse supersetLogResponse = supersetClient.logsFiltered(filter);
        return supersetLogResponse.getResult().stream().filter(logEntry -> logEntry.getJson().contains("mount_dashboard")).toList();
    }
}
