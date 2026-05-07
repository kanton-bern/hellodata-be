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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.nats.client.Connection;
import io.nats.client.Message;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
@AllArgsConstructor
public class QuerySynchronizer {

    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 40;

    private final HdContextRepository contextRepository;
    private final QueryRepository queryRepository;
    private final MetaInfoResourceService metaInfoResourceService;
    private final Connection connection;
    private final ObjectMapper objectMapper;

    @Scheduled(timeUnit = TimeUnit.MINUTES,
            fixedDelayString = "${hello-data.synchronize-query-in-minutes:60}",
            initialDelayString = "${hello-data.synchronize-query.initial-delay-minutes:2}")
    @Transactional
    public void synchronizeQueriesFromSupersets() {
        List<HdContextEntity> dataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        log.debug("Synchronizing queries from data domains {}", String.join(" : ", dataDomains.stream().map(HdContextEntity::getName).toList()));
        for (HdContextEntity contextEntity : dataDomains) {
            log.info("Started synchronizing queries for data domain {}", contextEntity.getName());
            for (SupersetQuery supersetQuery : fetchQueries(contextEntity.getContextKey())) {
                log.debug("Processing query: {}", supersetQuery);
                QueryEntity queryEntity = new QueryEntity();
                queryEntity.setContextKey(contextEntity.getContextKey());
                LocalDateTime localDateTime = supersetQuery.getChangedOn();
                OffsetDateTime offsetDateTime = localDateTime.atOffset(ZoneOffset.UTC);
                queryEntity.setChangedOn(offsetDateTime);
                queryEntity.setDatabaseName(supersetQuery.getDatabase() != null ? supersetQuery.getDatabase().getDatabaseName() : null);
                queryEntity.setTrackingUrl(supersetQuery.getTrackingUrl());
                queryEntity.setTmpTableName(supersetQuery.getTmpTableName());
                queryEntity.setStatus(supersetQuery.getStatus());
                queryEntity.setStartTime(supersetQuery.getStartTime());
                queryEntity.setEndTime(supersetQuery.getEndTime());
                try {
                    queryEntity.setSqlTables(objectMapper.writeValueAsString(supersetQuery.getSqlTables()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Could not parse json string", e); //NOSONAR
                }
                queryEntity.setSql(supersetQuery.getSql());
                queryEntity.setSchema(supersetQuery.getSchema());
                queryEntity.setRows(supersetQuery.getRows());
                queryEntity.setSubsystemId(supersetQuery.getId());
                queryEntity.setExecutedSql(supersetQuery.getExecutedSql());
                queryEntity.setTabName(supersetQuery.getTabName());
                if (supersetQuery.getUser() != null) {
                    queryEntity.setUserFullname(supersetQuery.getUser().getFirstName() + " " + supersetQuery.getUser().getLastName());
                }
                Optional<QueryEntity> found = queryRepository.findByContextKeyAndSubsystemId(contextEntity.getContextKey(), supersetQuery.getId());
                if (found.isPresent()) {
                    QueryEntity existingQuery = found.get();
                    log.debug("Updating query {}", existingQuery);
                    queryEntity.setId(existingQuery.getId());
                }
                queryRepository.save(queryEntity);
            }
            log.info("Finished synchronizing queries for data domain {}", contextEntity.getName());

        }
    }

    private List<SupersetQuery> fetchQueries(String contextKey) {
        try {
            String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextKey);
            String subject = SlugifyUtil.slugify(supersetInstanceName + RequestReplySubject.GET_QUERY_LIST.getSubject());
            log.debug("[fetchQueries] Sending request to subject: {}", subject);

            Optional<QueryEntity> foundEntity = queryRepository.findFirstByContextKeyOrderByChangedOnDesc(contextKey);
            ObjectNode filterNode = objectMapper.createObjectNode();

            if (foundEntity.isPresent()) {
                QueryEntity queryEntity = foundEntity.get();
                OffsetDateTime changedOn = queryEntity.getChangedOn();
                ObjectNode changedOnFilter = objectMapper.createObjectNode();
                changedOnFilter.put("col", "changed_on");
                changedOnFilter.put("opr", "gt");
                changedOnFilter.put("value", changedOn.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")));
                filterNode.putArray("filters").add(changedOnFilter);
            } else {
                filterNode.putArray("filters");
            }

            List<SupersetQuery> allQueries = new ArrayList<>();
            for (int page = 0; page < MAX_PAGES; page++) {
                filterNode.put("page", page);
                filterNode.put("pageSize", PAGE_SIZE);
                List<SupersetQuery> pageResults = fetchQueriesPage(subject, filterNode, contextKey, page);
                allQueries.addAll(pageResults);
                if (pageResults.size() < PAGE_SIZE) {
                    break;
                }
            }
            log.debug("[fetchQueries] Fetched total of {} queries for contextKey={}", allQueries.size(), contextKey);
            return allQueries;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted when fetching queries from the superset instance " + contextKey, e); //NOSONAR
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error fetching queries from the superset instance " + contextKey, e); //NOSONAR
        }
    }

    private List<SupersetQuery> fetchQueriesPage(String subject, ObjectNode filterNode, String contextKey, int page)
            throws JsonProcessingException, InterruptedException {
        byte[] requestBytes = objectMapper.writeValueAsString(filterNode).getBytes(StandardCharsets.UTF_8);
        Message reply = connection.request(subject, requestBytes, Duration.ofSeconds(60));
        if (reply == null || reply.getData() == null) {
            if (reply != null) {
                reply.ack();
            }
            return Collections.emptyList();
        }
        reply.ack();
        String content = new String(reply.getData(), StandardCharsets.UTF_8);
        try {
            JsonNode responseNode = objectMapper.readTree(content);
            if (responseNode.has("error")) {
                log.error("[fetchQueries] Error response from superset instance for contextKey={}: {}", contextKey, responseNode.get("error").asText());
                return Collections.emptyList();
            }
            if (responseNode.has("result")) {
                List<SupersetQuery> pageResults = objectMapper.readValue(
                        responseNode.get("result").toString(), new TypeReference<>() {
                        });
                int totalCount = responseNode.has("count") ? responseNode.get("count").asInt() : 0;
                log.debug("[fetchQueries] Page {} returned {} results (total count: {})", page, pageResults.size(), totalCount);
                return pageResults;
            }
            // Legacy response: plain JSON array (no pagination support on responder)
            return objectMapper.readValue(content, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("[fetchQueries] Non-JSON response from superset instance for contextKey={}: {}", contextKey, content, e);
            return Collections.emptyList();
        }
    }
}
