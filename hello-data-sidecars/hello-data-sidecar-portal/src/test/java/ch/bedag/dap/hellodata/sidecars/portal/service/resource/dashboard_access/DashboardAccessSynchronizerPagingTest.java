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
package ch.bedag.dap.hellodata.sidecars.portal.service.resource.dashboard_access;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.logs.response.superset.SupersetLog;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portalcommon.dashboard_access.entity.DashboardAccessEntity;
import ch.bedag.dap.hellodata.portalcommon.dashboard_access.repository.DashboardAccessRepository;
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

import static ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject.GET_DASHBOARD_ACCESS_LIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardAccessSynchronizerPagingTest {

    private static final String DATA_DOMAIN = "dd_one";
    private static final String OTHER_DATA_DOMAIN = "dd_two";
    private static final int CHART_ROWS_PER_VISIT = 11; // 9 load_chart + 2 render_chart, as observed in Superset

    private final LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).minusDays(2);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final HdContextRepository contextRepository = mock(HdContextRepository.class);
    private final DashboardAccessRepository dashboardAccessRepository = mock(DashboardAccessRepository.class);
    private final MetaInfoResourceService metaInfoResourceService = mock(MetaInfoResourceService.class);
    private final Connection connection = mock(Connection.class);

    private final Map<String, List<SupersetLog>> supersetLogs = new HashMap<>();
    private final List<DashboardAccessEntity> storedAccesses = new ArrayList<>();
    private final Set<Integer> failingPages = new HashSet<>();
    private final Set<String> unreachableDataDomains = new HashSet<>();

    private DashboardAccessSynchronizer synchronizer;

    @BeforeEach
    void setUp() throws InterruptedException {
        when(contextRepository.findAllByTypeIn(any())).thenReturn(List.of(dataDomain(DATA_DOMAIN), dataDomain(OTHER_DATA_DOMAIN)));
        when(metaInfoResourceService.findSupersetInstanceNameByContextKey(anyString())).thenAnswer(inv -> instanceName(inv.getArgument(0)));
        when(metaInfoResourceService.findAllByModuleTypeAndKindAndContextKey(any(), anyString(), anyString(), eq(DashboardResource.class)))
                .thenAnswer(inv -> new DashboardResource(instanceName(inv.getArgument(2)), List.of()));

        when(dashboardAccessRepository.findFirstByContextKeyOrderByDttmDesc(anyString()))
                .thenAnswer(inv -> accessesOf(inv.getArgument(0)).stream().max(Comparator.comparing(DashboardAccessEntity::getDttm)));
        when(dashboardAccessRepository.findByContextKeyAndDttm(anyString(), any()))
                .thenAnswer(inv -> accessesOf(inv.getArgument(0)).stream().filter(access -> access.getDttm().equals(inv.getArgument(1))).findFirst());
        when(dashboardAccessRepository.save(any(DashboardAccessEntity.class))).thenAnswer(inv -> {
            DashboardAccessEntity access = inv.getArgument(0);
            if (access.getId() == null) {
                access.setId(UUID.randomUUID());
                storedAccesses.add(access);
            }
            return access;
        });

        when(connection.request(anyString(), any(byte[].class), any(Duration.class))).thenAnswer(inv -> supersetSidecarReply(inv.getArgument(0), inv.getArgument(1)));

        synchronizer = new DashboardAccessSynchronizer(contextRepository, dashboardAccessRepository, metaInfoResourceService, connection, objectMapper);
    }

    @Test
    void savesAllVisitsWhenEachVisitHasSeveralChartLogRows() {
        synchronizedUntil(DATA_DOMAIN, start);
        addVisits(DATA_DOMAIN, start, 101);

        synchronizer.synchronizeDashboardAccessFromSupersets();

        assertThat(accessesOf(DATA_DOMAIN)).hasSize(1 + 101);
    }

    @Test
    void continuesPagingPastPageWithoutMountDashboard() {
        synchronizedUntil(DATA_DOMAIN, start);
        // An auto-refreshing dashboard fills a whole page with chart rows and no visit
        LocalDateTime afterRefreshes = addChartRows(DATA_DOMAIN, start, 1000);
        addVisits(DATA_DOMAIN, afterRefreshes, 5);

        synchronizer.synchronizeDashboardAccessFromSupersets();

        assertThat(accessesOf(DATA_DOMAIN)).hasSize(1 + 5);
    }

    @Test
    void losesNothingWhenPageFailsPartway() {
        synchronizedUntil(DATA_DOMAIN, start);
        addVisits(DATA_DOMAIN, start, 250);
        failingPages.add(1);

        synchronizer.synchronizeDashboardAccessFromSupersets();
        synchronizer.synchronizeDashboardAccessFromSupersets();

        assertThat(accessesOf(DATA_DOMAIN)).hasSize(1 + 250);
    }

    @Test
    void startsInitialSyncWithinRetentionPeriod() {
        addVisits(DATA_DOMAIN, start.minusDays(400), 1);
        addVisits(DATA_DOMAIN, start, 1);

        synchronizer.synchronizeDashboardAccessFromSupersets();

        assertThat(accessesOf(DATA_DOMAIN)).singleElement()
                .satisfies(access -> assertThat(access.getDttm().toLocalDateTime()).isAfter(start));
    }

    @Test
    void keepsSynchronizingOtherDataDomainsWhenOneSupersetIsUnreachable() {
        unreachableDataDomains.add(DATA_DOMAIN);
        addVisits(OTHER_DATA_DOMAIN, start, 3);

        synchronizer.synchronizeDashboardAccessFromSupersets();

        assertThat(accessesOf(OTHER_DATA_DOMAIN)).hasSize(3);
    }

    private void synchronizedUntil(String contextKey, LocalDateTime dttm) {
        DashboardAccessEntity access = new DashboardAccessEntity();
        access.setId(UUID.randomUUID());
        access.setContextKey(contextKey);
        access.setDttm(dttm.atOffset(ZoneOffset.UTC));
        storedAccesses.add(access);
    }

    private void addVisits(String contextKey, LocalDateTime after, int visits) {
        LocalDateTime dttm = after;
        for (int i = 0; i < visits; i++) {
            dttm = dttm.plusSeconds(1);
            supersetLogs.computeIfAbsent(contextKey, key -> new ArrayList<>()).add(logRow(dttm, "mount_dashboard"));
            dttm = addChartRows(contextKey, dttm, CHART_ROWS_PER_VISIT);
        }
    }

    private LocalDateTime addChartRows(String contextKey, LocalDateTime after, int rows) {
        LocalDateTime dttm = after;
        for (int i = 0; i < rows; i++) {
            dttm = dttm.plusSeconds(1);
            supersetLogs.computeIfAbsent(contextKey, key -> new ArrayList<>()).add(logRow(dttm, "load_chart"));
        }
        return dttm;
    }

    private List<DashboardAccessEntity> accessesOf(String contextKey) {
        return storedAccesses.stream().filter(access -> contextKey.equals(access.getContextKey())).toList();
    }

    // Answers like DashboardAccessListRequestListener: oldest first, only mount_dashboard rows, page size before that filter as rawCount
    private Message supersetSidecarReply(String subject, byte[] body) throws IOException {
        String contextKey = contextKeyOf(subject);
        if (unreachableDataDomains.contains(contextKey)) {
            throw new IllegalStateException("connection closed");
        }
        JsonNode request = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
        int page = request.get("page").asInt();
        if (failingPages.remove(page)) {
            return reply("{\"error\":\"Read timed out\"}".getBytes(StandardCharsets.UTF_8));
        }
        int pageSize = request.get("pageSize").asInt();
        Optional<LocalDateTime> after = dttmFilter(request.get("filters"));
        List<SupersetLog> rawPage = supersetLogs.getOrDefault(contextKey, List.of()).stream()
                .filter(log -> after.isEmpty() || log.getDttm().isAfter(after.get()))
                .sorted(Comparator.comparing(SupersetLog::getDttm))
                .skip((long) page * pageSize)
                .limit(pageSize)
                .toList();
        List<SupersetLog> visits = rawPage.stream().filter(log -> log.getJson().contains("mount_dashboard")).toList();
        ObjectNode response = objectMapper.createObjectNode();
        response.set("result", objectMapper.valueToTree(visits));
        response.put("count", visits.size());
        response.put("rawCount", rawPage.size());
        return reply(objectMapper.writeValueAsBytes(response));
    }

    private static Optional<LocalDateTime> dttmFilter(JsonNode filters) {
        if (filters == null) {
            return Optional.empty();
        }
        for (JsonNode filter : filters) {
            if ("dttm".equals(filter.get("col").asText()) && "gt".equals(filter.get("opr").asText())) {
                return Optional.of(LocalDateTime.parse(filter.get("value").asText()));
            }
        }
        return Optional.empty();
    }

    private static String contextKeyOf(String subject) {
        return subject.equals(SlugifyUtil.slugify(instanceName(DATA_DOMAIN) + GET_DASHBOARD_ACCESS_LIST.getSubject())) ? DATA_DOMAIN : OTHER_DATA_DOMAIN;
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

    private static SupersetLog logRow(LocalDateTime dttm, String eventName) {
        SubsystemUser user = new SubsystemUser();
        user.setUsername("viewer");
        user.setFirstName("Dashboard");
        user.setLastName("Viewer");
        SupersetLog log = new SupersetLog();
        log.setAction("log");
        log.setDashboardId(7);
        log.setDttm(dttm);
        log.setJson("{\"event_name\": \"" + eventName + "\"}");
        log.setUserId(1);
        log.setUser(user);
        return log;
    }

    private static Message reply(byte[] data) {
        Message message = mock(Message.class);
        when(message.getData()).thenReturn(data);
        return message;
    }
}
