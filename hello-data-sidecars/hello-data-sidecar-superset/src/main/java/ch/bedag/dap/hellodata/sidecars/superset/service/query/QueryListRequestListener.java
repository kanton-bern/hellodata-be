package ch.bedag.dap.hellodata.sidecars.superset.service.query;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.query.response.superset.SupersetQueryResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
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

@Log4j2
@Service
@RequiredArgsConstructor
public class QueryListRequestListener {

    private final Connection natsConnection;
    private final SupersetClientProvider supersetClientProvider;
    private final ObjectMapper objectMapper;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @PostConstruct
    public void listenForRequests() {
        String supersetSidecarSubject = SlugifyUtil.slugify(instanceName + RequestReplySubject.GET_QUERY_LIST.getSubject());
        log.debug("/*-/*- Listening for messages on subject {}", supersetSidecarSubject);
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            log.debug("\t-=-=-=-= Received message from NATS: {}", new String(msg.getData()));
            try {
                JsonArray filter = objectMapper.readerFor(JsonArray.class).readValue(msg.getData());
                SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
                SupersetQueryResponse queries = supersetClient.queriesFiltered(filter);
                String result = objectMapper.writeValueAsString(queries.getResult());
                natsConnection.publish(msg.getReplyTo(), result.getBytes(StandardCharsets.UTF_8));
                msg.ack();
            } catch (URISyntaxException | IOException | RuntimeException e) {
                log.error("Error fetching query list", e);
                natsConnection.publish(msg.getReplyTo(), e.getMessage().getBytes(StandardCharsets.UTF_8));
            }
        });
        dispatcher.subscribe(supersetSidecarSubject);
    }

}
