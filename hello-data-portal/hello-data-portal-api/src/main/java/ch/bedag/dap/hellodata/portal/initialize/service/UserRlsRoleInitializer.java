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

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.initialize.event.InitializationCompletedEvent;
import ch.bedag.dap.hellodata.portal.rls_role.service.RlsRoleService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.UserContextRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Initializer that migrates the RLS roles (RLS_01 - RLS_15) users already have in Superset (i.e. assigned by the CSV batch import)
 * to the portal's user_rls_role table, so they are not revoked by the next synchronization.
 * Runs once on application startup after the main initialization is completed. Uses MigrationService to ensure it runs exactly once.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class UserRlsRoleInitializer implements ApplicationListener<InitializationCompletedEvent> {

    private static final String MIGRATION_KEY = "USER_RLS_ROLE_MIGRATION_V1";
    private static final String MIGRATION_DESCRIPTION = "Migrate existing RLS role assignments from Superset to user_rls_role table";

    private final RlsRoleService rlsRoleService;
    private final UserContextRoleRepository userContextRoleRepository;
    private final HdContextRepository hdContextRepository;
    private final MetaInfoResourceService metaInfoResourceService;
    private final MigrationService migrationService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApplicationEvent(InitializationCompletedEvent event) {
        migrateUserRlsRoles();
    }

    public void migrateUserRlsRoles() {
        if (migrationService.isMigrationCompleted(MIGRATION_KEY)) {
            log.debug("Migration '{}' already completed, skipping", MIGRATION_KEY);
            return;
        }
        log.info("Starting migration: {}", MIGRATION_DESCRIPTION);
        try {
            int totalMigrated = 0;
            for (HdContextEntity dataDomain : hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN))) {
                totalMigrated += migrateForDataDomain(dataDomain.getContextKey());
            }
            migrationService.recordMigrationSuccess(MIGRATION_KEY, MIGRATION_DESCRIPTION);
            log.info("Migration completed successfully. Total RLS role assignments migrated: {}", totalMigrated);
        } catch (Exception e) {
            migrationService.recordMigrationFailure(MIGRATION_KEY, MIGRATION_DESCRIPTION, e.getMessage());
            throw e;
        }
    }

    private int migrateForDataDomain(String contextKey) {
        String instanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextKey);
        if (instanceName == null) {
            log.debug("No superset instance found for context {}", contextKey);
            return 0;
        }
        List<String> eligibleRoles = List.of(HdRoleName.DATA_DOMAIN_VIEWER.name(), HdRoleName.DATA_DOMAIN_BUSINESS_SPECIALIST.name());
        List<UserContextRoleEntity> userContextRoles = userContextRoleRepository.findByContextKeyAndRoleNames(contextKey, eligibleRoles);
        int migrated = 0;
        for (UserContextRoleEntity userRole : userContextRoles) {
            migrated += migrateForUser(userRole.getUser(), contextKey, instanceName);
        }
        log.info("Migrated {} RLS role assignments for context {}", migrated, contextKey);
        return migrated;
    }

    private int migrateForUser(UserEntity user, String contextKey, String instanceName) {
        if (user == null || !user.isEnabled() || rlsRoleService.hasSelectedRlsRoles(user.getId(), contextKey)) {
            return 0;
        }
        SubsystemUser subsystemUser = metaInfoResourceService.findUserInInstance(user.getEmail(), instanceName);
        if (subsystemUser == null) {
            return 0;
        }
        List<String> rlsRoles = CollectionUtils.emptyIfNull(subsystemUser.getRoles()).stream()
                .map(SubsystemRole::getName)
                .filter(RlsRoleService::isRlsRoleKey)
                .distinct()
                .toList();
        if (rlsRoles.isEmpty()) {
            return 0;
        }
        rlsRoleService.saveSelectedRlsRoles(user.getId(), contextKey, rlsRoles);
        log.debug("Migrated RLS roles {} for user {} in context {}", rlsRoles, user.getEmail(), contextKey);
        return rlsRoles.size();
    }
}
