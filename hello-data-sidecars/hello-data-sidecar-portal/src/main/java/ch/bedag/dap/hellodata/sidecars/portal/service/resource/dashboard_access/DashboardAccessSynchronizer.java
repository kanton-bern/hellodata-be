package ch.bedag.dap.hellodata.sidecars.portal.service.resource.dashboard_access;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.logs.response.superset.SupersetLog;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portalcommon.dashboard_access.entity.DashboardAccessEntity;
import ch.bedag.dap.hellodata.portalcommon.dashboard_access.repository.DashboardAccessRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
@AllArgsConstructor
public class DashboardAccessSynchronizer {

    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 40;
    private static final int RETENTION_DAYS = 365;

    private final HdContextRepository contextRepository;
    private final DashboardAccessRepository dashboardAccessRepository;
    private final MetaInfoResourceService metaInfoResourceService;
    private final Connection connection;
    private final ObjectMapper objectMapper;

    private static DashboardAccessEntity createDashboardAccessEntity(HdContextEntity contextEntity, SupersetLog supersetLog) {
        DashboardAccessEntity dashboardAccessEntity = new DashboardAccessEntity();
        dashboardAccessEntity.setContextKey(contextEntity.getContextKey());
        LocalDateTime localDateTime = supersetLog.getDttm();
        OffsetDateTime offsetDateTime = localDateTime.atOffset(ZoneOffset.UTC);
        dashboardAccessEntity.setDttm(offsetDateTime);
        dashboardAccessEntity.setDashboardId(supersetLog.getDashboardId());
        dashboardAccessEntity.setJson(supersetLog.getJson());
        dashboardAccessEntity.setReferrer(supersetLog.getReferrer());
        dashboardAccessEntity.setUsername(supersetLog.getUser().getUsername());
        dashboardAccessEntity.setUserId(supersetLog.getUserId());
        return dashboardAccessEntity;
    }

    @Scheduled(timeUnit = TimeUnit.MINUTES,
            fixedDelayString = "${hello-data.synchronize-dashboard-access-in-minutes:60}",
            initialDelayString = "${hello-data.synchronize-dashboard-access.initial-delay-minutes:2}")
    @Transactional
    public void synchronizeDashboardAccessFromSupersets() {
        List<HdContextEntity> dataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        log.info("[fetchDashboardAccess] Synchronizing dashboard accesses from data domains {}", String.join(" : ", dataDomains.stream().map(HdContextEntity::getName).toList()));
        for (HdContextEntity contextEntity : dataDomains) {
            log.info("[fetchDashboardAccess] Started synchronizing dashboard accesses for data domain {}", contextEntity.getName());
            String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextEntity.getContextKey());
            List<SupersetLog> dashboardAccesses;
            try {
                dashboardAccesses = fetchDashboardAccess(contextEntity.getContextKey(), supersetInstanceName);
            } catch (RuntimeException e) {
                log.error("[fetchDashboardAccess] Could not fetch dashboard accesses for data domain {}, they will be fetched on the next run", contextEntity.getName(), e);
                continue;
            }
            for (SupersetLog supersetLog : dashboardAccesses) {
                assembleAndSaveEntity(contextEntity, supersetLog, supersetInstanceName);
            }
            log.info("[fetchDashboardAccess] Finished synchronizing dashboard accesses for data domain {}", contextEntity.getName());
        }
        log.info("[fetchDashboardAccess] Finished synchronizing dashboard accesses from data domains {}", String.join(" : ", dataDomains.stream().map(HdContextEntity::getName).toList()));
    }

    @Scheduled(cron = "${hello-data.cleanup-dashboard-access-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupOldDashboardAccess() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(RETENTION_DAYS);
        int deleted = dashboardAccessRepository.deleteByDttmBefore(cutoff);
        log.info("[cleanupDashboardAccess] Deleted {} dashboard access entries older than {} days (before {})", deleted, RETENTION_DAYS, cutoff);
    }

    private void assembleAndSaveEntity(HdContextEntity contextEntity, SupersetLog supersetLog, String supersetInstanceName) {
        log.debug("[fetchDashboardAccess] Processing dashboard access: {}", supersetLog);
        DashboardAccessEntity dashboardAccessEntity = createDashboardAccessEntity(contextEntity, supersetLog);
        fillDashboardTitleAndSlug(contextEntity, supersetLog, dashboardAccessEntity);
        if (dashboardAccessEntity.getDashboardTitle() == null) {
            dashboardAccessEntity.setDashboardTitle("unknown [id: " + dashboardAccessEntity.getDashboardId() + "]");
        }
        fillUserDetails(supersetLog, dashboardAccessEntity, supersetInstanceName);
        Optional<DashboardAccessEntity> found = dashboardAccessRepository.findByContextKeyAndDttm(contextEntity.getContextKey(), dashboardAccessEntity.getDttm());
        if (found.isPresent()) {
            DashboardAccessEntity existingDashboardAccess = found.get();
            log.warn(" [fetchDashboardAccess]Updating dashboard access {}", existingDashboardAccess);
            dashboardAccessEntity.setId(existingDashboardAccess.getId());
        }
        try {
            dashboardAccessRepository.save(dashboardAccessEntity);
            log.debug("[fetchDashboardAccess] Saved dashboard access {}", dashboardAccessEntity);
        } catch (RuntimeException e) {
            log.error("[fetchDashboardAccess] Error saving dashboard access entity {}: {}", dashboardAccessEntity, e.getMessage(), e);
        }
    }

    private void fillDashboardTitleAndSlug(HdContextEntity contextEntity, SupersetLog supersetLog, DashboardAccessEntity dashboardAccessEntity) {
        DashboardResource dashboardResource = metaInfoResourceService.findAllByModuleTypeAndKindAndContextKey(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_DASHBOARDS, contextEntity.getContextKey(), DashboardResource.class);
        List<SupersetDashboard> dashboards = dashboardResource.getData();
        dashboards.stream().filter(dash -> supersetLog.getDashboardId().equals(dash.getId())).findFirst().ifPresent(dash -> {
            dashboardAccessEntity.setDashboardSlug(dash.getSlug());
            dashboardAccessEntity.setDashboardTitle(dash.getDashboardTitle());
        });
    }

    private void fillUserDetails(SupersetLog supersetLog, DashboardAccessEntity dashboardAccessEntity, String supersetInstanceName) {
        if (supersetLog.getUser().getFirstName() != null) {
            dashboardAccessEntity.setUserFullname(supersetLog.getUser().getFirstName() + " " + supersetLog.getUser().getLastName());
        } else {
            SubsystemUser userInInstance = metaInfoResourceService.findUserInInstance(supersetLog.getUser().getUsername(), supersetInstanceName);
            if (userInInstance != null) {
                dashboardAccessEntity.setUserFullname(userInInstance.getFirstName() + " " + userInInstance.getLastName());
            }
        }
        if (dashboardAccessEntity.getUserFullname() == null || dashboardAccessEntity.getUserFullname().isBlank()) {
            String fallback = supersetLog.getUser().getUsername();
            if (fallback == null || fallback.isBlank()) {
                fallback = "unknown [id: " + supersetLog.getUserId() + "]";
            }
            dashboardAccessEntity.setUserFullname(fallback);
        }
    }

    private List<SupersetLog> fetchDashboardAccess(String contextKey, String supersetInstanceName) {
        try {
            String subject = SlugifyUtil.slugify(supersetInstanceName + RequestReplySubject.GET_DASHBOARD_ACCESS_LIST.getSubject());
            log.debug("[fetchDashboardAccess] Sending request to subject: {}", subject);

            // The first sync only goes back as far as the cleanup would keep the accesses
            OffsetDateTime accessDate = dashboardAccessRepository.findFirstByContextKeyOrderByDttmDesc(contextKey)
                    .map(DashboardAccessEntity::getDttm)
                    .orElseGet(() -> OffsetDateTime.now(ZoneOffset.UTC).minusDays(RETENTION_DAYS));
            ObjectNode requestNode = objectMapper.createObjectNode();
            ObjectNode changedOnFilter = objectMapper.createObjectNode();
            changedOnFilter.put("col", "dttm");
            changedOnFilter.put("opr", "gt");
            changedOnFilter.put("value", accessDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")));
            requestNode.putArray("filters").add(changedOnFilter);

            List<SupersetLog> allLogs = new ArrayList<>();
            for (int page = 0; page < MAX_PAGES; page++) {
                requestNode.put("page", page);
                requestNode.put("pageSize", PAGE_SIZE);
                DashboardAccessPage pageResults = fetchDashboardAccessPage(subject, requestNode, supersetInstanceName, contextKey, page);
                allLogs.addAll(pageResults.result());
                // The result only holds the mount_dashboard rows, so the page size before that filter tells if there is more
                if (pageResults.rawCount() < PAGE_SIZE) {
                    break;
                }
            }
            log.debug("[fetchDashboardAccess] Fetched total of {} dashboard accesses for contextKey={}", allLogs.size(), contextKey);
            return allLogs;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted when fetching dashboard accesses from the superset instance " + contextKey, e); //NOSONAR
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error fetching dashboard accesses from the superset instance " + contextKey, e); //NOSONAR
        }
    }

    private DashboardAccessPage fetchDashboardAccessPage(String subject, ObjectNode requestNode, String supersetInstanceName, String contextKey, int page)
            throws JsonProcessingException, InterruptedException {
        byte[] requestBytes = objectMapper.writeValueAsString(requestNode).getBytes(StandardCharsets.UTF_8);
        Message reply = connection.request(subject, requestBytes, Duration.ofSeconds(60));
        if (reply == null || reply.getData() == null) {
            if (reply != null) {
                reply.ack();
            }
            throw new RuntimeException("No reply from superset instance " + supersetInstanceName + " (contextKey=" + contextKey + ") for page " + page); //NOSONAR
        }
        reply.ack();
        String content = new String(reply.getData(), StandardCharsets.UTF_8);
        log.debug("[fetchDashboardAccess] reply page {}: {}", page, content);
        try {
            JsonNode responseNode = objectMapper.readTree(content);
            if (responseNode.has("error")) {
                throw new RuntimeException("Error response from superset instance " + supersetInstanceName + " (contextKey=" + contextKey + ") for page " + page + ": " //NOSONAR
                        + responseNode.get("error").asText());
            }
            if (responseNode.has("result")) {
                List<SupersetLog> pageResults = objectMapper.readValue(
                        responseNode.get("result").toString(), new TypeReference<>() {
                        });
                int count = responseNode.has("count") ? responseNode.get("count").asInt() : 0;
                int rawCount = responseNode.has("rawCount") ? responseNode.get("rawCount").asInt() : pageResults.size();
                log.debug("[fetchDashboardAccess] Page {} returned {} results (count: {}, rawCount: {})", page, pageResults.size(), count, rawCount);
                return new DashboardAccessPage(pageResults, rawCount);
            }
            // Legacy response: plain JSON array (no pagination support on responder)
            List<SupersetLog> legacyResults = objectMapper.readValue(content, new TypeReference<>() {
            });
            return new DashboardAccessPage(legacyResults, legacyResults.size());
        } catch (JsonProcessingException e) {
            log.error("[fetchDashboardAccess] Non-JSON response from superset instance {} (contextKey={}): {}", supersetInstanceName, contextKey, content, e);
            throw e;
        }
    }

    private record DashboardAccessPage(List<SupersetLog> result, int rawCount) {
    }
}
