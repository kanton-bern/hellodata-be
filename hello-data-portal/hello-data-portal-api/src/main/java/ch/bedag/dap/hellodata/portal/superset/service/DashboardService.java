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
package ch.bedag.dap.hellodata.portal.superset.service;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repositories.HdContextRepository;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardUpload;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardDto;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardWithMetadataDto;
import ch.bedag.dap.hellodata.portal.superset.data.UpdateSupersetDashboardMetadataDto;
import ch.bedag.dap.hellodata.portal.superset.entity.DashboardMetadataEntity;
import ch.bedag.dap.hellodata.portal.superset.repository.DashboardMetadataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int HALF_MB = 512 * 1024;

    private final MetaInfoResourceService metaInfoResourceService;
    private final DashboardMetadataRepository dashboardMetadataRepository;
    private final HdContextRepository contextRepository;
    private final ModelMapper modelMapper;
    private final Connection connection;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Set<SupersetDashboardDto> fetchMyDashboards() {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        Set<SupersetDashboardDto> dashboardsWithAccess = new HashSet<>();
        List<MetaInfoResourceEntity> metaInfoResources = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_DASHBOARDS);
        for (MetaInfoResourceEntity metaInfoResource : metaInfoResources) {
            if (metaInfoResource.getMetainfo() instanceof DashboardResource dashboardResource) {
                fetchDashboardsFromMetainfoResources(currentUserEmail, dashboardsWithAccess, metaInfoResource, dashboardResource);
            }
        }
        return dashboardsWithAccess;
    }

    private void fetchDashboardsFromMetainfoResources(String currentUserEmail, Set<SupersetDashboardDto> dashboardsWithAccess, MetaInfoResourceEntity metaInfoResource,
                                                      DashboardResource dashboardResource) {
        List<SupersetDashboard> dashboards = CollectionUtils.emptyIfNull(dashboardResource.getData()).stream().filter(SupersetDashboard::isPublished).toList();
        String instanceName = dashboardResource.getInstanceName();
        UserResource userResource =
                metaInfoResourceService.findByModuleTypeInstanceNameAndKind(ModuleType.SUPERSET, instanceName, ModuleResourceKind.HELLO_DATA_USERS, UserResource.class);
        Optional<SubsystemUser> subsystemUserFound =
                CollectionUtils.emptyIfNull(userResource.getData()).stream().filter(u -> u.getEmail().equalsIgnoreCase(currentUserEmail)).findFirst();
        if (subsystemUserFound.isPresent()) {
            SubsystemUser assignedUser = subsystemUserFound.get();
            List<SupersetRole> userRoles = assignedUser.getRoles();
            Set<String> rolesOfCurrentUser = userRoles.stream().map(SupersetRole::getName).collect(Collectors.toSet());
            boolean isAdmin = userRoles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(SlugifyUtil.BI_ADMIN_ROLE_NAME));
            boolean isEditor = userRoles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(SlugifyUtil.BI_EDITOR_ROLE_NAME));
            String instanceUrl = metaInfoResourceService.findInstanceUrl(ModuleType.SUPERSET, dashboardResource.getInstanceName());
            findDashboardsWithGivenRolesForCurrentUser(dashboardsWithAccess, metaInfoResource, dashboardResource, dashboards, rolesOfCurrentUser, isAdmin, isEditor, instanceUrl);
        }
    }

    @SuppressWarnings("java:S107")
    private void findDashboardsWithGivenRolesForCurrentUser(Set<SupersetDashboardDto> dashboardsWithAccess, MetaInfoResourceEntity metaInfoResource,
                                                            DashboardResource dashboardResource, List<SupersetDashboard> dashboards, Set<String> rolesOfCurrentUser,
                                                            boolean isAdmin, boolean isEditor, String instanceUrl) {
        for (SupersetDashboard dashboard : dashboards) {
            Set<String> dashboardRoles = dashboard.getRoles().stream().map(SupersetRole::getName).collect(Collectors.toSet());
            log.debug(String.format("Instance: '%s', Dashboard: '%s', requires any of following roles: %s", dashboardResource.getInstanceName(), dashboard.getDashboardTitle(),
                                    dashboardRoles));
            checkRBACRoles(dashboardsWithAccess, metaInfoResource, dashboardResource, rolesOfCurrentUser, isAdmin, isEditor, instanceUrl, dashboard, dashboardRoles);
        }
    }

    @SuppressWarnings("java:S107")
    private void checkRBACRoles(Set<SupersetDashboardDto> dashboardsWithAccess, MetaInfoResourceEntity metaInfoResource, DashboardResource dashboardResource,
                                Set<String> rolesOfCurrentUser, boolean isAdmin, boolean isEditor, String instanceUrl, SupersetDashboard dashboard, Set<String> dashboardRoles) {
        for (String dashboardRole : dashboardRoles) {
            boolean isViewer = rolesOfCurrentUser.contains(dashboardRole);
            if (isAdmin || isEditor || isViewer) { // ensure only RBAC roles are allowed (starting with D_ ...)
                HdContextEntity context = contextRepository.getByContextKey(metaInfoResource.getContextKey()).orElse(null);
                String contextName = context != null ? context.getName() : null;
                UUID contextId = context != null ? context.getId() : null;
                String contextKey = context != null ? context.getContextKey() : null;
                SupersetDashboardWithMetadataDto dashboardDto =
                        new SupersetDashboardWithMetadataDto(dashboard, dashboardResource.getInstanceName(), instanceUrl, isAdmin, isEditor, isViewer, contextName, contextId,
                                                             contextKey);
                fetchMetadata(dashboardResource.getInstanceName(), dashboard, dashboardDto);
                dashboardsWithAccess.add(dashboardDto);
            }
        }
    }

    private void fetchMetadata(String instanceName, SupersetDashboard dashboard, SupersetDashboardWithMetadataDto dashboardDto) {
        String changedOnUtc = dashboard.getChangedOnUtc();
        long changedOnUtcInSuperset = 0;
        if (changedOnUtc != null) {
            changedOnUtc = changedOnUtc.substring(0, 26) + "Z";
            changedOnUtcInSuperset = Instant.parse(changedOnUtc).toEpochMilli();
            dashboardDto.setModified(changedOnUtcInSuperset);
        }
        Optional<DashboardMetadataEntity> bySubsystemIdAndInstanceName = dashboardMetadataRepository.findBySubsystemIdAndInstanceName(dashboard.getId(), instanceName);
        if (bySubsystemIdAndInstanceName.isPresent()) {
            DashboardMetadataEntity dashboardMetadataEntity = bySubsystemIdAndInstanceName.get();
            dashboardDto.setBusinessProcess(dashboardMetadataEntity.getBusinessProcess());
            dashboardDto.setDepartment(dashboardMetadataEntity.getDepartment());
            dashboardDto.setResponsibility(dashboardMetadataEntity.getResponsibility());
            dashboardDto.setDataAnalyst(dashboardMetadataEntity.getDataAnalyst());
            dashboardDto.setScheduled(dashboardMetadataEntity.getScheduled());
            dashboardDto.setDatasource(dashboardMetadataEntity.getDatasource());
            LocalDateTime lastAccessDate = dashboardMetadataEntity.getModifiedDate() != null ? dashboardMetadataEntity.getModifiedDate() : dashboardMetadataEntity.getCreatedDate();
            ZonedDateTime zdt = ZonedDateTime.of(lastAccessDate, ZoneId.systemDefault());
            long metadataModifiedBy = zdt.toInstant().toEpochMilli();
            dashboardDto.setModified(Math.max(changedOnUtcInSuperset, metadataModifiedBy));
        }
    }

    @Transactional
    public void updateDashboard(String instanceName, int subsystemId, UpdateSupersetDashboardMetadataDto updateSupersetDashboardMetadataDto) {
        validateUserAllowedToUpdate(instanceName, subsystemId);

        Optional<DashboardMetadataEntity> bySubsystemIdAndInstanceName = dashboardMetadataRepository.findBySubsystemIdAndInstanceName(subsystemId, instanceName);
        DashboardMetadataEntity dashboardMetadataEntity;
        if (bySubsystemIdAndInstanceName.isPresent()) {
            dashboardMetadataEntity = bySubsystemIdAndInstanceName.get();
        } else {
            dashboardMetadataEntity = new DashboardMetadataEntity();
            dashboardMetadataEntity.setSubsystemId(subsystemId);
            dashboardMetadataEntity.setInstanceName(instanceName);
        }
        modelMapper.map(updateSupersetDashboardMetadataDto, dashboardMetadataEntity);
        dashboardMetadataRepository.save(dashboardMetadataEntity);
    }

    private void validateUserAllowedToUpdate(String instanceName, int subsystemId) {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        UserResource supersetUserResources =
                metaInfoResourceService.findByModuleTypeInstanceNameAndKind(ModuleType.SUPERSET, instanceName, ModuleResourceKind.HELLO_DATA_USERS, UserResource.class);
        DashboardResource supersetDashboardResources =
                metaInfoResourceService.findByModuleTypeInstanceNameAndKind(ModuleType.SUPERSET, instanceName, ModuleResourceKind.HELLO_DATA_DASHBOARDS, DashboardResource.class);
        Optional<SubsystemUser> currentUserFromSuperset =
                CollectionUtils.emptyIfNull(supersetUserResources.getData()).stream().filter(u -> u.getEmail().equalsIgnoreCase(currentUserEmail)).findFirst();
        Optional<SupersetDashboard> dashboardFromSuperset =
                CollectionUtils.emptyIfNull(supersetDashboardResources.getData()).stream().filter(d -> d.getId() == subsystemId).findFirst();
        if (dashboardFromSuperset.isEmpty()) {
            log.error("Could not find dashboard {} in superset {}", subsystemId, instanceName);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Dashboard with specified id not found in subsystem");
        }
        if (currentUserFromSuperset.isPresent()) {
            SubsystemUser subsystemUser = currentUserFromSuperset.get();
            List<SupersetRole> userRoles = CollectionUtils.emptyIfNull(subsystemUser.getRoles()).stream().toList();
            boolean isAdmin = userRoles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(SlugifyUtil.BI_ADMIN_ROLE_NAME));
            boolean isEditor = userRoles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(SlugifyUtil.BI_EDITOR_ROLE_NAME));
            if (isAdmin || isEditor) {
                return;
            }
            log.error("Current user {} doesn't have rights to update dashboard {} in instance {}", currentUserEmail, subsystemId, instanceName);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not allowed to update dashboard metadata");
        } else {
            log.error("Current user {} doesn't exist in superset {}", currentUserEmail, instanceName);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "User with specified email not found in subsystem");
        }
    }

    public void uploadDashboardsFile(MultipartFile file, String contextKey) {
        try (InputStream inputStream = file.getInputStream()) {
            String fileName = file.getName();

            String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextKey);
            String subject = SlugifyUtil.slugify(supersetInstanceName + RequestReplySubject.UPLOAD_DASHBOARDS_FILE.getSubject());
            String binaryFileId = UUID.randomUUID().toString();

            int chunkNumber = 0;
            int bytesRead;
            byte[] buffer = new byte[file.getSize() > HALF_MB ? HALF_MB : (int) file.getSize()];
            int bytesReadTotal = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                bytesReadTotal += bytesRead;
                boolean isLastChunk = bytesReadTotal == file.getSize();
                log.info("Sending file to superset, bytes read total: {}, file size: {}", bytesReadTotal, file.getSize());
                DashboardUpload dashboardUpload = new DashboardUpload(Arrays.copyOf(buffer, bytesRead), ++chunkNumber, fileName, binaryFileId, isLastChunk, file.getSize());
                sendToSidecar(dashboardUpload, subject);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error sending bytes to the superset instance " + contextKey, e); //NOSONAR
        } catch (IOException e) {
            throw new RuntimeException("Error sending bytes to the superset instance " + contextKey, e); //NOSONAR
        }
    }

    private void sendToSidecar(DashboardUpload dashboardUpload, String subject) throws InterruptedException, JsonProcessingException {
        log.debug("[uploadDashboardsFile] Sending request to subject: {}", subject);
        Message reply = connection.request(subject, objectMapper.writeValueAsString(dashboardUpload).getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(60));
        if (reply == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload dashboard. The reply is null, please verify superset sidecar or nats connection");
        }
        String responseContent = new String(reply.getData(), StandardCharsets.UTF_8);
        if ("OK".equalsIgnoreCase(responseContent)) {
            reply.ack();
            log.debug("[uploadDashboardsFile] Response received: " + new String(reply.getData()));
        } else {
            log.warn("Reply is NOK, please verify the uploaded file: {}", responseContent);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, responseContent);
        }
    }
}
