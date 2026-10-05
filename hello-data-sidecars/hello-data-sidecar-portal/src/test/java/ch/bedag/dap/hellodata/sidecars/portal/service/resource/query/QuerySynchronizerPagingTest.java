/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.sidecars.portal.service.resource.query;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.query.response.superset.SupersetQuery;
import ch.bedag.dap.hellodata.portalcommon.query.entity.QueryEntity;
import ch.bedag.dap.hellodata.portalcommon.query.repository.QueryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nats.client.Connection;
import io.nats.client.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject.GET_QUERY_LIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuerySynchronizerPagingTest {

    private static final String DATA_DOMAIN = "dd_one";
    private static final String OTHER_DATA_DOMAIN = "dd_two";
    private static final DateTimeFormatter FILTER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).minusDays(2);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final HdContextRepository contextRepository = mock(HdContextRepository.class);
    private final QueryRepository queryRepository = mock(QueryRepository.class);
    private final MetaInfoResourceService metaInfoResourceService = mock(MetaInfoResourceService.class);
    private final Connection connection = mock(Connection.class);

    private final Map<String, List<SupersetQuery>> supersetQueries = new HashMap<>();
    private final Map<String, QueryEntity> storedQueries = new HashMap<>();
    private final Set<Integer> failingRequests = new HashSet<>();
    private final Set<String> unreachableDataDomains = new HashSet<>();
    private final List<JsonNode> requests = new ArrayList<>();
    private Runnable afterFirstRequest = () -> {
    };
    private int nextQueryId = 1;

    private QuerySynchronizer synchronizer;

    @BeforeEach
    void setUp() throws InterruptedException {
        when(contextRepository.findAllByTypeIn(any())).thenReturn(List.of(dataDomain(DATA_DOMAIN), dataDomain(OTHER_DATA_DOMAIN)));
        when(metaInfoResourceService.findSupersetInstanceNameByContextKey(anyString())).thenAnswer(inv -> instanceName(inv.getArgument(0)));

        when(queryRepository.findFirstByContextKeyOrderByChangedOnDesc(anyString()))
                .thenAnswer(inv -> queriesOf(inv.getArgument(0)).stream().max(Comparator.comparing(QueryEntity::getChangedOn)));
        when(queryRepository.findByContextKeyAndSubsystemId(anyString(), anyInt()))
                .thenAnswer(inv -> Optional.ofNullable(storedQueries.get(storageKey(inv.getArgument(0), inv.getArgument(1)))));
        when(queryRepository.save(any(QueryEntity.class))).thenAnswer(inv -> {
            QueryEntity query = inv.getArgument(0);
            if (query.getId() == null) {
                query.setId(UUID.randomUUID());
                storedQueries.put(storageKey(query.getContextKey(), query.getSubsystemId()), query);
            }
            return query;
        });

        when(connection.request(anyString(), any(byte[].class), any(Duration.class))).thenAnswer(inv -> supersetSidecarReply(inv.getArgument(0), inv.getArgument(1)));

        synchronizer = new QuerySynchronizer(contextRepository, queryRepository, metaInfoResourceService, connection, objectMapper);
    }

    @Test
    void syncsBacklogLargerThanMaxPagesOverSeveralRuns() {
        addQueries(DATA_DOMAIN, start, 40_500);

        synchronizer.synchronizeQueriesFromSupersets();
        synchronizer.synchronizeQueriesFromSupersets();

        assertThat(queriesOf(DATA_DOMAIN)).hasSize(40_500);
    }

    @Test
    void losesNothingWhenPageFailsPartway() {
        addQueries(DATA_DOMAIN, start, 2_500);
        failingRequests.add(2);

        synchronizer.synchronizeQueriesFromSupersets();
        synchronizer.synchronizeQueriesFromSupersets();

        assertThat(queriesOf(DATA_DOMAIN)).hasSize(2_500);
    }

    @Test
    void doesNotSkipQueriesWhenOneFinishesBetweenPages() {
        addQueries(DATA_DOMAIN, start, 2_500);
        // a query that was still running when page 1 was read finishes and moves to the end of the changed_on order -
        // with offsets the query at the next page boundary would shift into the page that was already read
        afterFirstRequest = () -> supersetQueries.get(DATA_DOMAIN).get(499).setChangedOn(start.plusSeconds(10_000));

        synchronizer.synchronizeQueriesFromSupersets();

        assertThat(queriesOf(DATA_DOMAIN)).hasSize(2_500);
        assertThat(storedQueries.get(storageKey(DATA_DOMAIN, 500)).getChangedOn().toLocalDateTime()).isEqualTo(start.plusSeconds(10_000));
    }

    @Test
    void requestsAlwaysTheFirstPageAfterTheNewestQueryRead() {
        addQueries(DATA_DOMAIN, start, 2_500);

        synchronizer.synchronizeQueriesFromSupersets();

        List<JsonNode> dataDomainRequests = requestsFor(DATA_DOMAIN);
        assertThat(dataDomainRequests).hasSize(3);
        assertThat(dataDomainRequests).extracting(request -> request.get("page").asInt()).containsOnly(0);
        // one millisecond before the newest changed_on of the previous page, so queries sharing it are not skipped
        assertThat(dataDomainRequests.subList(1, dataDomainRequests.size())).extracting(request -> request.get("filters").get(0).get("value").asText()).containsExactly(
                start.plusSeconds(1_000).minusNanos(1_000_000).format(FILTER_FORMAT),
                start.plusSeconds(1_999).minusNanos(1_000_000).format(FILTER_FORMAT));
    }

    @Test
    void doesNotSkipQueriesSharingChangedOnAcrossPageBoundary() {
        addQueries(DATA_DOMAIN, start, 998);
        addQueriesAt(DATA_DOMAIN, start.plusSeconds(5_000), 5);
        addQueries(DATA_DOMAIN, start.plusSeconds(6_000), 500);

        synchronizer.synchronizeQueriesFromSupersets();

        assertThat(queriesOf(DATA_DOMAIN)).hasSize(1_503);
    }

    @Test
    void stopsRunWhenMoreThanPageSizeQueriesShareChangedOn() {
        addQueriesAt(DATA_DOMAIN, start.plusSeconds(1), 1_001);

        synchronizer.synchronizeQueriesFromSupersets();

        // the cursor cannot move past them, so the run stops instead of requesting the same page again and again
        assertThat(requestsFor(DATA_DOMAIN)).hasSize(2);
        assertThat(queriesOf(DATA_DOMAIN)).hasSize(1_000);
    }

    @Test
    void startsInitialSyncWithinRetentionPeriod() {
        addQueries(DATA_DOMAIN, start.minusDays(400), 1);
        addQueries(DATA_DOMAIN, start, 1);

        synchronizer.synchronizeQueriesFromSupersets();

        assertThat(queriesOf(DATA_DOMAIN)).singleElement()
                .satisfies(query -> assertThat(query.getChangedOn().toLocalDateTime()).isAfter(start));
    }

    @Test
    void keepsSynchronizingOtherDataDomainsWhenOneSupersetIsUnreachable() {
        unreachableDataDomains.add(DATA_DOMAIN);
        addQueries(OTHER_DATA_DOMAIN, start, 3);

        synchronizer.synchronizeQueriesFromSupersets();

        assertThat(queriesOf(OTHER_DATA_DOMAIN)).hasSize(3);
    }

    private void addQueries(String contextKey, LocalDateTime after, int queries) {
        for (int i = 1; i <= queries; i++) {
            SupersetQuery query = new SupersetQuery();
            query.setId(nextQueryId++);
            query.setChangedOn(after.plusSeconds(i));
            query.setSql("select " + i);
            query.setStatus("success");
            supersetQueries.computeIfAbsent(contextKey, key -> new ArrayList<>()).add(query);
        }
    }

    private void addQueriesAt(String contextKey, LocalDateTime changedOn, int queries) {
        for (int i = 1; i <= queries; i++) {
            SupersetQuery query = new SupersetQuery();
            query.setId(nextQueryId++);
            query.setChangedOn(changedOn);
            query.setSql("select " + i);
            query.setStatus("success");
            supersetQueries.computeIfAbsent(contextKey, key -> new ArrayList<>()).add(query);
        }
    }

    private List<JsonNode> requestsFor(String contextKey) {
        return requests.stream().filter(request -> contextKey.equals(request.get("contextKey").asText())).toList();
    }

    private List<QueryEntity> queriesOf(String contextKey) {
        return storedQueries.values().stream().filter(query -> contextKey.equals(query.getContextKey())).toList();
    }

    private static String storageKey(String contextKey, Integer subsystemId) {
        return contextKey + "/" + subsystemId;
    }

    // Answers like QueryListRequestListener: oldest first, total number of matching queries as count
    private Message supersetSidecarReply(String subject, byte[] body) throws IOException {
        String contextKey = contextKeyOf(subject);
        if (unreachableDataDomains.contains(contextKey)) {
            throw new IllegalStateException("connection closed");
        }
        JsonNode request = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
        int page = request.get("page").asInt();
        requests.add(((ObjectNode) request.deepCopy()).put("contextKey", contextKey));
        int requestNumber = requestsFor(contextKey).size();
        if (contextKey.equals(DATA_DOMAIN) && requestNumber == 2) {
            afterFirstRequest.run();
        }
        if (contextKey.equals(DATA_DOMAIN) && failingRequests.remove(requestNumber)) {
            return reply("{\"error\":\"Read timed out\"}".getBytes(StandardCharsets.UTF_8));
        }
        int pageSize = request.get("pageSize").asInt();
        Optional<LocalDateTime> after = changedOnFilter(request.get("filters"));
        List<SupersetQuery> matching = supersetQueries.getOrDefault(contextKey, List.of()).stream()
                .filter(query -> after.isEmpty() || query.getChangedOn().isAfter(after.get()))
                .sorted(Comparator.comparing(SupersetQuery::getChangedOn))
                .toList();
        List<SupersetQuery> queryPage = matching.stream().skip((long) page * pageSize).limit(pageSize).toList();
        ObjectNode response = objectMapper.createObjectNode();
        response.set("result", objectMapper.valueToTree(queryPage));
        response.put("count", matching.size());
        return reply(objectMapper.writeValueAsBytes(response));
    }

    private static Optional<LocalDateTime> changedOnFilter(JsonNode filters) {
        if (filters == null) {
            return Optional.empty();
        }
        for (JsonNode filter : filters) {
            if ("changed_on".equals(filter.get("col").asText()) && "gt".equals(filter.get("opr").asText())) {
                return Optional.of(LocalDateTime.parse(filter.get("value").asText()));
            }
        }
        return Optional.empty();
    }

    private static String contextKeyOf(String subject) {
        return subject.equals(SlugifyUtil.slugify(instanceName(DATA_DOMAIN) + GET_QUERY_LIST.getSubject())) ? DATA_DOMAIN : OTHER_DATA_DOMAIN;
    }

    private static String instanceName(String contextKey) {
        return "superset-" + contextKey;
    }

    private static HdContextEntity dataDomain(String contextKey) {
        HdContextEntity context = new HdContextEntity();
        context.setContextKey(contextKey);
        context.setName(contextKey);
        context.setType(HdContextType.DATA_DOMAIN);
        return context;
    }

    private static Message reply(byte[] data) {
        Message message = mock(Message.class);
        when(message.getData()).thenReturn(data);
        return message;
    }
}
