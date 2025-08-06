package ch.bedag.dap.hellodata.portal.superset.service;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoResourceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class QueryService {

    private final MetaInfoResourceService metaInfoResourceService;
    private final Connection connection;
    private final ObjectMapper objectMapper;

    public List<Object> fetchQueries(String contextKey) {
        try {
            String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextKey);
            String subject = SlugifyUtil.slugify(supersetInstanceName + RequestReplySubject.GET_QUERY_LIST.getSubject());
            log.debug("[fetchQueries] Sending request to subject: {}", subject);
            Message reply = connection.request(subject, "".getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(60));
            if (reply != null && reply.getData() != null) {
                reply.ack();
                return objectMapper.readValue(new String(reply.getData(), StandardCharsets.UTF_8), new TypeReference<>() {
                });
            }
            if (reply != null) {
                reply.ack();
            }
            return Collections.emptyList();
        } catch (InterruptedException | JsonProcessingException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching queries from the superset instance " + contextKey, e); //NOSONAR
        }
    }
}
