package ch.bedag.dap.hellodata.sidecars.portal.service.resource.query;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.query.response.superset.SupersetQuery;
import ch.bedag.dap.hellodata.portalcommon.query.entity.QueryEntity;
import ch.bedag.dap.hellodata.portalcommon.query.repository.QueryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.nats.client.Connection;
import io.nats.client.Message;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@AllArgsConstructor
public class QuerySynchronizer {

    private final HdContextRepository contextRepository;
    private final QueryRepository queryRepository;
    private final MetaInfoResourceService metaInfoResourceService;
    private final Connection connection;
    private final ObjectMapper objectMapper;

    @Transactional
    public void synchronizeQueriesFromSupersets() {
        List<HdContextEntity> dataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        log.debug("Synchronizing queries from data domains {}", String.join(" : ", dataDomains.stream().map(dd -> dd.getName()).toList()));
        for (HdContextEntity contextEntity : dataDomains) {
            log.info("Synchronizing queries for data domain {}", contextEntity.getName());
            fetchQueries(contextEntity.getContextKey())
                    .forEach(supersetQuery -> {
                        log.debug("Processing query: {}", supersetQuery);
                        QueryEntity queryEntity = new QueryEntity();
                        queryEntity.setContextKey(contextEntity.getContextKey());
                        queryEntity.setChangedOn(OffsetDateTime.parse(supersetQuery.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
                        queryEntity.setDatabaseName(supersetQuery.getDatabase().getDatabaseName());
                        queryEntity.setTrackingUrl(supersetQuery.getTrackingUrl());
                        queryEntity.setTmpTableName(supersetQuery.getTmpTableName());
                        queryEntity.setStatus(supersetQuery.getStatus());
                        queryEntity.setStartTime(supersetQuery.getStartTime());
                        queryEntity.setEndTime(supersetQuery.getEndTime());
                        queryEntity.setSqlTables(supersetQuery.getSqlTables());
                        queryEntity.setSql(supersetQuery.getSql());
                        queryEntity.setSchema(supersetQuery.getSchema());
                        queryEntity.setRows(supersetQuery.getRows());
                        queryEntity.setSubsystemId(supersetQuery.getId());
                        queryEntity.setExecutedSql(supersetQuery.getExecutedSql());
                        queryEntity.setTabName(supersetQuery.getTabName());
                        queryRepository.save(queryEntity);
                    });

        }
    }

    private List<SupersetQuery> fetchQueries(String contextKey) {
        try {
            String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextKey);
            String subject = SlugifyUtil.slugify(supersetInstanceName + RequestReplySubject.GET_QUERY_LIST.getSubject());
            log.debug("[fetchQueries] Sending request to subject: {}", subject);

            Optional<QueryEntity> foundEntity = queryRepository.findFirstByContextKeyOrderByChangedOnDesc(contextKey);
            JsonArray filter = new JsonArray();
            if (foundEntity.isPresent()) {
                QueryEntity queryEntity = foundEntity.get();
                OffsetDateTime changedOn = queryEntity.getChangedOn();
                JsonObject changedOnFilter = new JsonObject();
                changedOnFilter.addProperty("col", "changed_on");
                changedOnFilter.addProperty("opr", "gt");
                changedOnFilter.addProperty("value", changedOn.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
                filter.add(changedOnFilter);
            }

            Message reply = connection.request(subject, filter.getAsString().getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(60));
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
