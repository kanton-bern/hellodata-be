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
package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.base.config.SystemProperties;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.event.UserFullSyncEvent;
import ch.bedag.dap.hellodata.portal.user.service.UserSelectedDashboardService.DashboardSelection;
import ch.bedag.dap.hellodata.portalcommon.role.entity.PortalRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.SystemDefaultPortalRoleName;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserPortalRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.PortalRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.role.repository.UserPortalRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class AutoProvisionService {

    private final SystemProperties systemProperties;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final NatsSenderService natsSenderService;
    private final ApplicationEventPublisher eventPublisher;
    private final MetaInfoResourceService metaInfoResourceService;
    private final UserSelectedDashboardService userSelectedDashboardService;
    private final PortalRoleRepository portalRoleRepository;
    private final UserPortalRoleRepository userPortalRoleRepository;
    private final HdContextRepository hdContextRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserEntity autoProvisionIfEnabled(String email, String firstName, String lastName, String keycloakSubject) {
        if (!systemProperties.isAutoProvisionViewerOnLogin()) {
            return null;
        }

        // Double-check user doesn't already exist (race condition guard)
        Optional<UserEntity> existingUser = userRepository.findUserEntityByEmailIgnoreCase(email);
        if (existingUser.isPresent()) {
            log.debug("User {} already exists in DB, skipping auto-provision", email);
            return existingUser.get();
        }

        log.info("Auto-provisioning new user with viewer roles and all dashboards: {}", email);

        UserEntity userEntity = new UserEntity();
        userEntity.setId(UUID.fromString(keycloakSubject));
        userEntity.setEmail(email.toLowerCase(Locale.ROOT));
        userEntity.setUsername(email.toLowerCase(Locale.ROOT));
        userEntity.setFirstName(firstName);
        userEntity.setLastName(lastName);
        userEntity.setEnabled(true);
        userEntity.setSuperuser(false);
        userEntity.setFederated(true);
        userRepository.saveAndFlush(userEntity);

        roleService.setBusinessDomainRoleForUser(userEntity, HdRoleName.NONE);
        roleService.setAllDataDomainRolesForUser(userEntity, HdRoleName.DATA_DOMAIN_VIEWER);

        // Create portal roles immediately so the user has DASHBOARDS/DATA_LINEAGE permissions
        // on the very first request (portal sidecar will reconcile these later via NATS)
        assignPortalRolesForViewer(userEntity);

        // Assign all published dashboards and build sync payload
        Map<String, List<DashboardForUserDto>> dashboardsPerContext = assignAllPublishedDashboards(userEntity.getId());

        // Notify subsystems about the new user
        SubsystemUserUpdate createUser = new SubsystemUserUpdate();
        createUser.setFirstName(firstName);
        createUser.setLastName(lastName);
        createUser.setUsername(email.toLowerCase(Locale.ROOT));
        createUser.setEmail(email.toLowerCase(Locale.ROOT));
        createUser.setActive(true);
        createUser.setSendBackUsersList(false);
        natsSenderService.publishMessageToJetStream(HDEvent.CREATE_USER, createUser);

        // Fire full sync (roles + dashboards) so subsystems pick up everything after this transaction commits.
        // sendBackUsersList=true triggers Superset sidecar to publish updated user list to metainfo,
        // which is needed for fetchMyDashboards() to see the new user's roles.
        eventPublisher.publishEvent(new UserFullSyncEvent(userEntity.getId(), true, new HashMap<>(), dashboardsPerContext));

        return userEntity;
    }

    private Map<String, List<DashboardForUserDto>> assignAllPublishedDashboards(UUID userId) {
        Map<String, List<DashboardForUserDto>> dashboardsPerContext = new HashMap<>();

        List<MetaInfoResourceEntity> allDashboardResources =
                metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS);

        for (MetaInfoResourceEntity entity : allDashboardResources) {
            if (!(entity.getMetainfo() instanceof DashboardResource dashboardResource)) {
                continue;
            }
            String contextKey = entity.getContextKey();
            if (contextKey == null) {
                continue;
            }

            List<DashboardSelection> selections = new ArrayList<>();
            List<DashboardForUserDto> dashboardDtos = new ArrayList<>();

            for (SupersetDashboard dashboard : dashboardResource.getData()) {
                if (!dashboard.isPublished()) {
                    continue;
                }
                selections.add(new DashboardSelection(
                        dashboard.getId(), dashboard.getDashboardTitle(), dashboardResource.getInstanceName()));

                DashboardForUserDto dto = new DashboardForUserDto();
                dto.setId(dashboard.getId());
                dto.setTitle(dashboard.getDashboardTitle());
                dto.setViewer(true);
                dto.setInstanceName(dashboardResource.getInstanceName());
                dashboardDtos.add(dto);
            }

            if (!selections.isEmpty()) {
                userSelectedDashboardService.saveSelectedDashboards(userId, contextKey, selections);
                dashboardsPerContext.merge(contextKey, dashboardDtos, (existing, newList) -> {
                    existing.addAll(newList);
                    return existing;
                });
            }
        }

        log.info("Assigned {} dashboard contexts with all published dashboards for user {}", dashboardsPerContext.size(), userId);
        return dashboardsPerContext;
    }

    private void assignPortalRolesForViewer(UserEntity userEntity) {
        Optional<PortalRoleEntity> viewerPortalRole =
                portalRoleRepository.findByName(SystemDefaultPortalRoleName.DATA_DOMAIN_VIEWER.name());
        if (viewerPortalRole.isEmpty()) {
            log.warn("Portal role DATA_DOMAIN_VIEWER not found, user will not have portal permissions until sidecar sync");
            return;
        }
        PortalRoleEntity role = viewerPortalRole.get();
        List<HdContextEntity> allDataDomains = hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        for (HdContextEntity dataDomain : allDataDomains) {
            UserPortalRoleEntity portalRoleEntity = new UserPortalRoleEntity();
            portalRoleEntity.setUser(userEntity);
            portalRoleEntity.setRole(role);
            portalRoleEntity.setContextKey(dataDomain.getContextKey());
            portalRoleEntity.setContextType(HdContextType.DATA_DOMAIN);
            userPortalRoleRepository.saveAndFlush(portalRoleEntity);
        }
        log.info("Assigned DATA_DOMAIN_VIEWER portal role for {} data domains for user {}", allDataDomains.size(), userEntity.getEmail());
    }
}
