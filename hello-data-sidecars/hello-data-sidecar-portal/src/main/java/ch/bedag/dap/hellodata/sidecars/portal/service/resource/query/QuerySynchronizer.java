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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.nats.client.Connection;
import io.nats.client.Message;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
@AllArgsConstructor
public class QuerySynchronizer {

    private final HdContextRepository contextRepository;
    private final QueryRepository queryRepository;
    private final MetaInfoResourceService metaInfoResourceService;
    private final Connection connection;
    private final ObjectMapper objectMapper;

    @Scheduled(timeUnit = TimeUnit.MINUTES,
            fixedDelayString = "${hello-data.synchronize-query-in-minutes:60}",
            initialDelayString = "${hello-data.synchronize-query.initial-delay:2}")
    @Transactional
    public void synchronizeQueriesFromSupersets() {
        List<HdContextEntity> dataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        log.debug("Synchronizing queries from data domains {}", String.join(" : ", dataDomains.stream().map(dd -> dd.getName()).toList()));
        for (HdContextEntity contextEntity : dataDomains) {
            log.info("Started synchronizing queries for data domain {}", contextEntity.getName());
            fetchQueries(contextEntity.getContextKey())
                    .forEach(supersetQuery -> {
                        log.debug("Processing query: {}", supersetQuery);
                        QueryEntity queryEntity = new QueryEntity();
                        queryEntity.setContextKey(contextEntity.getContextKey());
                        LocalDateTime localDateTime = supersetQuery.getChangedOn();
                        ZoneId zoneId = ZoneId.systemDefault();
                        ZoneOffset offset = zoneId.getRules().getOffset(localDateTime);
                        OffsetDateTime offsetDateTime = localDateTime.atOffset(offset);
                        queryEntity.setChangedOn(offsetDateTime);
                        queryEntity.setDatabaseName(supersetQuery.getDatabase().getDatabaseName());
                        queryEntity.setTrackingUrl(supersetQuery.getTrackingUrl());
                        queryEntity.setTmpTableName(supersetQuery.getTmpTableName());
                        queryEntity.setStatus(supersetQuery.getStatus());
                        queryEntity.setStartTime(supersetQuery.getStartTime());
                        queryEntity.setEndTime(supersetQuery.getEndTime());
                        try {
                            queryEntity.setSqlTables(objectMapper.writeValueAsString(supersetQuery.getSqlTables()));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("Could not parse json string", e);
                        }
                        queryEntity.setSql(supersetQuery.getSql());
                        queryEntity.setSchema(supersetQuery.getSchema());
                        queryEntity.setRows(supersetQuery.getRows());
                        queryEntity.setSubsystemId(supersetQuery.getId());
                        queryEntity.setExecutedSql(supersetQuery.getExecutedSql());
                        queryEntity.setTabName(supersetQuery.getTabName());
                        queryEntity.setUserFullname(supersetQuery.getUser().getFirstName() + " " + supersetQuery.getUser().getLastName());
                        Optional<QueryEntity> found = queryRepository.findByContextKeyAndSubsystemId(contextEntity.getContextKey(), supersetQuery.getId());
                        if (found.isPresent()) {
                            QueryEntity existingQuery = found.get();
                            log.warn("Updating query {}", existingQuery);
                            queryEntity.setId(existingQuery.getId());
                        }
                        queryRepository.save(queryEntity);
                    });
            log.info("Finished synchronizing queries for data domain {}", contextEntity.getName());

        }
    }

    private List<SupersetQuery> fetchQueries(String contextKey) {
        try {
            String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextKey);
            String subject = SlugifyUtil.slugify(supersetInstanceName + RequestReplySubject.GET_QUERY_LIST.getSubject());
            log.debug("[fetchQueries] Sending request to subject: {}", subject);

            Optional<QueryEntity> foundEntity = queryRepository.findFirstByContextKeyOrderByChangedOnDesc(contextKey);
            ArrayNode filter = objectMapper.createArrayNode();

            if (foundEntity.isPresent()) {
                QueryEntity queryEntity = foundEntity.get();
                OffsetDateTime changedOn = queryEntity.getChangedOn();
                ObjectNode changedOnFilter = objectMapper.createObjectNode();
                changedOnFilter.put("col", "changed_on");
                changedOnFilter.put("opr", "gt");
                changedOnFilter.put("value", changedOn.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")));
                filter.add(changedOnFilter);
            }

            byte[] filterBytes = objectMapper.writeValueAsString(filter).getBytes(StandardCharsets.UTF_8);
            Message reply = connection.request(subject, filterBytes, Duration.ofSeconds(60));
            if (reply != null && reply.getData() != null) {
                reply.ack();
                List<SupersetQuery> supersetQueries = objectMapper.readValue(new String(reply.getData(), StandardCharsets.UTF_8), new TypeReference<>() {
                });
                log.debug("[fetchQueries] Received queries by filter {}, queries: {}", new String(filterBytes, StandardCharsets.UTF_8), supersetQueries);
                return supersetQueries;
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
