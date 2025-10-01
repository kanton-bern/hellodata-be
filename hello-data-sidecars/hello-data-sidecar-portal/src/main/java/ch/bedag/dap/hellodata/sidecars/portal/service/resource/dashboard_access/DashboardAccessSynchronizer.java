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
public class DashboardAccessSynchronizer {

    private final HdContextRepository contextRepository;
    private final DashboardAccessRepository dashboardAccessRepository;
    private final MetaInfoResourceService metaInfoResourceService;
    private final Connection connection;
    private final ObjectMapper objectMapper;

    @Scheduled(timeUnit = TimeUnit.MINUTES,
            fixedDelayString = "${hello-data.synchronize-dashboard-access-in-minutes:60}",
            initialDelayString = "${hello-data.synchronize-dashboard-access.initial-delay-minutes:2}")
    @Transactional
    public void synchronizeDashboardAccessFromSupersets() {
        List<HdContextEntity> dataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        log.debug("[fetchDashboardAccess] Synchronizing dashboard accesses from data domains {}", String.join(" : ", dataDomains.stream().map(HdContextEntity::getName).toList()));
        for (HdContextEntity contextEntity : dataDomains) {
            log.info("[fetchDashboardAccess] Started synchronizing dashboard accesses for data domain {}", contextEntity.getName());
            String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextEntity.getContextKey());
            for (SupersetLog supersetLog : fetchDashboardAccess(contextEntity.getContextKey(), supersetInstanceName)) {
                log.debug("[fetchDashboardAccess] Processing dashboard access: {}", supersetLog);
                DashboardAccessEntity dashboardAccessEntity = createDashboardAccessEntity(contextEntity, supersetLog);
                fillDashboardTitleAndSlug(contextEntity, supersetLog, dashboardAccessEntity);
                fillUserDetails(supersetLog, dashboardAccessEntity, supersetInstanceName);
                Optional<DashboardAccessEntity> found = dashboardAccessRepository.findByContextKeyAndDttm(contextEntity.getContextKey(), dashboardAccessEntity.getDttm());
                if (found.isPresent()) {
                    DashboardAccessEntity existingDashboardAccess = found.get();
                    log.warn("Updating dashboard access {}", existingDashboardAccess);
                    dashboardAccessEntity.setId(existingDashboardAccess.getId());
                }
                dashboardAccessRepository.save(dashboardAccessEntity);
                log.info("[fetchDashboardAccess] Saved dashboard access {}", dashboardAccessEntity);
            }
            log.info("Finished synchronizing queries for data domain {}", contextEntity.getName());

        }
    }

    private static DashboardAccessEntity createDashboardAccessEntity(HdContextEntity contextEntity, SupersetLog supersetLog) {
        DashboardAccessEntity dashboardAccessEntity = new DashboardAccessEntity();
        dashboardAccessEntity.setContextKey(contextEntity.getContextKey());
        LocalDateTime localDateTime = supersetLog.getDttm();
        ZoneId zoneId = ZoneId.systemDefault();
        ZoneOffset offset = zoneId.getRules().getOffset(localDateTime);
        OffsetDateTime offsetDateTime = localDateTime.atOffset(offset);
        dashboardAccessEntity.setDttm(offsetDateTime);
        dashboardAccessEntity.setDashboardId(supersetLog.getDashboardId());
        dashboardAccessEntity.setJson(supersetLog.getJson());
        dashboardAccessEntity.setReferrer(supersetLog.getReferrer());
        dashboardAccessEntity.setUsername(supersetLog.getUser().getUsername());
        dashboardAccessEntity.setUserId(supersetLog.getUserId());
        return dashboardAccessEntity;
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
    }

    private List<SupersetLog> fetchDashboardAccess(String contextKey, String supersetInstanceName) {
        try {
            String subject = SlugifyUtil.slugify(supersetInstanceName + RequestReplySubject.GET_DASHBOARD_ACCESS_LIST.getSubject());
            log.debug("[fetchDashboardAccess] Sending request to subject: {}", subject);

            Optional<DashboardAccessEntity> foundEntity = dashboardAccessRepository.findFirstByContextKeyOrderByDttmDesc(contextKey);
            ArrayNode filter = objectMapper.createArrayNode();

            if (foundEntity.isPresent()) {
                DashboardAccessEntity queryEntity = foundEntity.get();
                OffsetDateTime accessDate = queryEntity.getDttm();
                ObjectNode changedOnFilter = objectMapper.createObjectNode();
                changedOnFilter.put("col", "changed_on");
                changedOnFilter.put("opr", "gt");
                changedOnFilter.put("value", accessDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")));
                filter.add(changedOnFilter);
            }

            byte[] filterBytes = objectMapper.writeValueAsString(filter).getBytes(StandardCharsets.UTF_8);
            Message reply = connection.request(subject, filterBytes, Duration.ofSeconds(60));
            if (reply != null && reply.getData() != null) {
                reply.ack();
                List<SupersetLog> supersetLogs = objectMapper.readValue(new String(reply.getData(), StandardCharsets.UTF_8), new TypeReference<>() {
                });
                log.debug("[fetchDashboardAccess] Received dashboard accesses by filter {}, queries: {}", new String(filterBytes, StandardCharsets.UTF_8), supersetLogs);
                return supersetLogs;
            }
            if (reply != null) {
                reply.ack();
            }
            return Collections.emptyList();
        } catch (InterruptedException | JsonProcessingException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching dashboard accesses from the superset instance " + contextKey, e); //NOSONAR
        }
    }
}
