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
package ch.bedag.dap.hellodata.portal.user.event;

import ch.bedag.dap.hellodata.portal.user.service.UserSubsystemSyncService;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Centralized event listener for all user subsystem synchronization events.
 * This listener handles synchronization of user context roles and dashboards with external subsystems.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class UserSyncEventListener {

    private final UserSubsystemSyncService userSubsystemSyncService;
    private final UserRepository userRepository;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserDashboardSyncEvent(UserDashboardSyncEvent event) {
        log.info("Received UserDashboardSyncEvent for user {} and context {}", event.userId(), event.contextKey());
        try {
            userSubsystemSyncService.synchronizeUserWithDashboards(event.userId(), event.contextKey());
        } catch (Exception e) {
            log.error("Failed to sync dashboards for user {} in context {}: {}", event.userId(), event.contextKey(), e.getMessage());
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserContextRoleSyncEvent(UserContextRoleSyncEvent event) {
        log.info("Received UserContextRoleSyncEvent for user {}", event.userId());
        try {
            UserEntity userEntity = getUserEntity(event.userId());
            userSubsystemSyncService.synchronizeContextRoles(userEntity, event.sendBackUsersList(), event.extraModuleRoles());
        } catch (Exception e) {
            log.error("Failed to sync context roles for user {}: {}", event.userId(), e.getMessage());
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserFullSyncEvent(UserFullSyncEvent event) {
        log.info("Received UserFullSyncEvent for user {}", event.userId());
        try {
            UserEntity userEntity = getUserEntity(event.userId());
            userSubsystemSyncService.synchronizeUser(userEntity, event.sendBackUsersList(),
                    event.extraModuleRoles(), event.dashboardsPerContext());
        } catch (Exception e) {
            log.error("Failed to perform full sync for user {}: {}", event.userId(), e.getMessage());
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSyncAllUsersEvent(SyncAllUsersEvent event) {
        log.info("Received SyncAllUsersEvent - synchronizing all users with dashboards");
        try {
            userSubsystemSyncService.syncAllUsers();
        } catch (Exception e) {
            log.error("Failed to sync all users: {}", e.getMessage(), e);
        }
    }

    private UserEntity getUserEntity(UUID userId) {
        UserEntity userEntity = userRepository.getByIdOrAuthId(userId.toString());
        if (userEntity == null) {
            throw new NotFoundException(String.format("User %s not found in the DB", userId));
        }
        return userEntity;
    }
}
