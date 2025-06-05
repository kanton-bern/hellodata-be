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

import ch.bedag.dap.hellodata.portal.initialize.event.SyncAllUsersEvent;
import ch.bedag.dap.hellodata.portal.user.data.AdUserDto;
import ch.bedag.dap.hellodata.portal.user.data.AdUserOrigin;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
@AllArgsConstructor
public class KeycloakUserSyncService {

    private final KeycloakService keycloakService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Checks if any of keycloak users has a changed id (e.g: user removed directly in the keycloak and then added again)
     */
    @Async
    @Transactional
    @Scheduled(fixedDelayString = "${hello-data.auth-server.sync-users-schedule-hours}", timeUnit = TimeUnit.HOURS)
    public void syncUsers() {
        log.debug("[sync-users-with-keycloak] Started");
        List<UserEntity> allPortalUsers = userRepository.findAll();
        for (UserEntity userEntity : allPortalUsers) {
            UserRepresentation userRepresentation = keycloakService.getUserRepresentationByEmail(userEntity.getEmail());
            if (userRepresentation != null) {
                userEntity.setAuthId(userRepresentation.getId());
                userEntity.setEnabled(userRepresentation.isEnabled());
                userEntity.setUsername(userRepresentation.getEmail());
                userEntity.setFirstName(userRepresentation.getFirstName());
                userEntity.setLastName(userRepresentation.getLastName());
                userEntity.setSuperuser(userEntity.getSuperuser());//set flag to not fetch lazy loading relations
                List<AdUserDto> adUserDtos = userService.searchUserOmitCreated(userEntity.getEmail());
                log.debug("[sync-users-with-keycloak] Found users from providers: {}", adUserDtos);
                boolean isFederated = adUserDtos.stream().anyMatch(adUserDto -> adUserDto.getOrigin() == AdUserOrigin.LDAP);
                log.debug("[sync-users-with-keycloak] Is user {} federated: {}", userEntity.getEmail(), isFederated);
                log.debug("[sync-users-with-keycloak] Is user {} federated in keycloak: {}", userEntity.getEmail(), userRepresentation.getFederationLink());
                userEntity.setFederated(isFederated || userRepresentation.getFederationLink() != null);
            }
        }
        userRepository.saveAll(allPortalUsers);
        userRepository.flush();
        applicationEventPublisher.publishEvent(new SyncAllUsersEvent());
        log.debug("[sync-users-with-keycloak] Completed, starting users with subsystems sync....");
    }

}
