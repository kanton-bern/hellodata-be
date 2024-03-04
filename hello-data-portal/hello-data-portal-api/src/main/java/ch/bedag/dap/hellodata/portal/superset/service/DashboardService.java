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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
import org.springframework.web.server.ResponseStatusException;

@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int ONE_MB = 1024 * 1024;

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

    private void findDashboardsWithGivenRolesForCurrentUser(Set<SupersetDashboardDto> dashboardsWithAccess, MetaInfoResourceEntity metaInfoResource,
                                                            DashboardResource dashboardResource, List<SupersetDashboard> dashboards, Set<String> rolesOfCurrentUser,
                                                            boolean isAdmin, boolean isEditor, String instanceUrl) {
        for (SupersetDashboard dashboard : dashboards) {
            Set<String> dashboardRoles = dashboard.getRoles().stream().map(SupersetRole::getName).collect(Collectors.toSet());
            log.debug(String.format("Instance: '%s', Dashboard: '%s', requires any of following roles: %s", dashboardResource.getInstanceName(), dashboard.getDashboardTitle(),
                                    dashboardRoles));
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

    public void uploadDashboardsFile(String fileName, byte[] bytes, String contextKey) {
        try {
            String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextKey);
            String subject = SlugifyUtil.slugify(supersetInstanceName + RequestReplySubject.UPLOAD_DASHBOARDS_FILE.getSubject());
            String binaryFileId = UUID.randomUUID().toString();
            int fullFileSize = bytes.length;
            if (fullFileSize > ONE_MB) {
                List<byte[]> chunks = breakIntoChunks(bytes, ONE_MB);
                for (int i = 1; i <= chunks.size(); i++) {
                    DashboardUpload dashboardUpload = new DashboardUpload(bytes, false, i, fileName, binaryFileId, i == chunks.size(), fullFileSize);
                    sendToSidecar(dashboardUpload, subject);
                }
            } else {
                DashboardUpload dashboardUpload = new DashboardUpload(bytes, false, 0, fileName, binaryFileId, false, fullFileSize);
                sendToSidecar(dashboardUpload, subject);
            }
        } catch (InterruptedException | JsonProcessingException e) {
            throw new RuntimeException("Error sending bytes to the superset instance " + contextKey, e);
        }
    }

    private void sendToSidecar(DashboardUpload dashboardUpload, String subject) throws InterruptedException, JsonProcessingException {
        log.info("[uploadDashboardsFile] Sending request to subject: {}, chunked? {}", subject, dashboardUpload.isChunked());
        Message reply = connection.request(subject, objectMapper.writeValueAsString(dashboardUpload).getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(20));
        if (reply == null) {
            log.warn("Reply is null, please verify superset sidecar or nats connection");
        } else if ("OK".equalsIgnoreCase(new String(reply.getData(), StandardCharsets.UTF_8))) {
            reply.ack();
            log.info("[uploadDashboardsFile] Response received: " + new String(reply.getData()));
        } else {
            log.warn("Reply is NOK, please verify the uploaded file");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, new String(reply.getData(), StandardCharsets.UTF_8));
        }
    }

    public List<byte[]> breakIntoChunks(byte[] byteArray, int chunkSize) {
        List<byte[]> chunks = new ArrayList<>();
        int offset = 0;
        while (offset < byteArray.length) {
            int length = Math.min(chunkSize, byteArray.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(byteArray, offset, chunk, 0, length);
            chunks.add(chunk);
            offset += chunkSize;
        }
        return chunks;
    }
}
