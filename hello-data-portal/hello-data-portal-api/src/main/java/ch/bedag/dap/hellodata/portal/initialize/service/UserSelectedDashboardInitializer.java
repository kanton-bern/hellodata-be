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
package ch.bedag.dap.hellodata.portal.initialize.service;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.initialize.event.InitializationCompletedEvent;
import ch.bedag.dap.hellodata.portal.user.service.UserSelectedDashboardService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.UserContextRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static ch.bedag.dap.hellodata.commons.SlugifyUtil.DASHBOARD_ROLE_PREFIX;

/**
 * Initializer that migrates existing dashboard viewer permissions from Superset resource table
 * to the portal's user_selected_dashboard table. This runs once on application startup after
 * the main initialization is completed, and only if the table is empty (idempotent).
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class UserSelectedDashboardInitializer implements ApplicationListener<InitializationCompletedEvent> {

    private final UserSelectedDashboardService userSelectedDashboardService;
    private final UserContextRoleRepository userContextRoleRepository;
    private final HdContextRepository hdContextRepository;
    private final MetaInfoResourceService metaInfoResourceService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApplicationEvent(InitializationCompletedEvent event) {
        migrateUserSelectedDashboardsInternal();
    }

    /**
     * Public method for testing - delegates to internal implementation
     */
    public void migrateUserSelectedDashboards() {
        migrateUserSelectedDashboardsInternal();
    }

    private void migrateUserSelectedDashboardsInternal() {
        // Check if migration already happened (idempotent)
        if (!userSelectedDashboardService.isEmpty()) {
            log.debug("user_selected_dashboard table is not empty, skipping migration");
            return;
        }

        log.info("Starting migration of user selected dashboards from Superset resource table...");

        List<HdContextEntity> dataDomains = hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        List<MetaInfoResourceEntity> allDashboards = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS);

        int totalMigrated = 0;
        for (HdContextEntity dataDomain : dataDomains) {
            int migratedForDomain = migrateForDataDomain(dataDomain, allDashboards);
            totalMigrated += migratedForDomain;
        }

        log.info("Migration completed. Total dashboard selections migrated: {}", totalMigrated);
    }

    private int migrateForDataDomain(HdContextEntity dataDomain, List<MetaInfoResourceEntity> allDashboards) {
        String contextKey = dataDomain.getContextKey();

        // Get users with VIEWER or BUSINESS_SPECIALIST role in this domain
        List<String> eligibleRoles = List.of(HdRoleName.DATA_DOMAIN_VIEWER.name(), HdRoleName.DATA_DOMAIN_BUSINESS_SPECIALIST.name());
        List<UserContextRoleEntity> userContextRoles = userContextRoleRepository.findByContextKeyAndRoleNames(contextKey, eligibleRoles);

        if (userContextRoles.isEmpty()) {
            log.debug("No eligible users found for context {}", contextKey);
            return 0;
        }

        // Get dashboards for this context
        List<MetaInfoResourceEntity> contextDashboards = allDashboards.stream()
                .filter(d -> contextKey.equals(d.getContextKey()))
                .toList();

        if (contextDashboards.isEmpty()) {
            log.debug("No dashboards found for context {}", contextKey);
            return 0;
        }

        int migrated = 0;
        for (UserContextRoleEntity userRole : userContextRoles) {
            migrated += processUserMigration(userRole, contextKey, contextDashboards);
        }

        log.info("Migrated {} dashboard selections for context {}", migrated, contextKey);
        return migrated;
    }

    private int processUserMigration(UserContextRoleEntity userRole, String contextKey, List<MetaInfoResourceEntity> contextDashboards) {
        UserEntity user = userRole.getUser();
        if (user == null || !user.isEnabled()) {
            return 0;
        }

        List<UserSelectedDashboardService.DashboardSelection> selections = collectDashboardSelections(user, contextDashboards);

        if (!selections.isEmpty()) {
            userSelectedDashboardService.saveSelectedDashboards(user.getId(), contextKey, selections);
            log.debug("Migrated {} dashboard selections for user {} in context {}",
                    selections.size(), user.getEmail(), contextKey);
            return selections.size();
        }
        return 0;
    }

    private List<UserSelectedDashboardService.DashboardSelection> collectDashboardSelections(UserEntity user, List<MetaInfoResourceEntity> contextDashboards) {
        List<UserSelectedDashboardService.DashboardSelection> selections = new ArrayList<>();
        for (MetaInfoResourceEntity dashboardEntity : contextDashboards) {
            if (dashboardEntity.getMetainfo() instanceof DashboardResource dashboardResource) {
                selections.addAll(processDashboardResource(user, dashboardResource));
            }
        }
        return selections;
    }

    private List<UserSelectedDashboardService.DashboardSelection> processDashboardResource(UserEntity user, DashboardResource dashboardResource) {
        List<UserSelectedDashboardService.DashboardSelection> resourceSelections = new ArrayList<>();
        String instanceName = dashboardResource.getInstanceName();
        SubsystemUser subsystemUser = metaInfoResourceService.findUserInInstance(user.getEmail(), instanceName);

        if (subsystemUser == null) {
            return resourceSelections;
        }

        List<SupersetDashboard> publishedDashboards = dashboardResource.getData().stream()
                .filter(SupersetDashboard::isPublished)
                .toList();

        for (SupersetDashboard dashboard : publishedDashboards) {
            if (userHasDashboardViewerRole(dashboard, subsystemUser)) {
                resourceSelections.add(new UserSelectedDashboardService.DashboardSelection(
                        dashboard.getId(),
                        dashboard.getDashboardTitle(),
                        instanceName
                ));
            }
        }
        return resourceSelections;
    }

    /**
     * Checks if user has the dashboard-specific viewer role in Superset.
     * This replicates the logic from UserService.createDashboardDto().
     */
    private boolean userHasDashboardViewerRole(SupersetDashboard dashboard, SubsystemUser subsystemUser) {
        // Find the dashboard-specific role (prefixed with DASHBOARD_ROLE_PREFIX)
        SubsystemRole dashboardRole = dashboard.getRoles().stream()
                .filter(role -> role.getName().startsWith(DASHBOARD_ROLE_PREFIX))
                .findFirst()
                .orElse(null);

        if (dashboardRole == null) {
            return false;
        }

        // Check if user has this dashboard role
        boolean hasDashboardRole = subsystemUser.getRoles().contains(dashboardRole);

        // Also check if user has the BI_VIEWER_ROLE
        boolean hasBiViewerRole = subsystemUser.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(SlugifyUtil.BI_VIEWER_ROLE_NAME));

        return hasDashboardRole && hasBiViewerRole;
    }
}
