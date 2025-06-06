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

import ch.bedag.dap.hellodata.commons.metainfomodel.repositories.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.conf.DefaultAdminProperties;
import ch.bedag.dap.hellodata.portal.user.entity.DefaultUserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.DefaultUserRepository;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Component
public class DefaultUserInitializer extends AbstractUserInitializer {

    private final DefaultAdminProperties defaultAdminProperties;
    private final DefaultUserRepository defaultUserRepository;
    private final RoleService roleService;
    private final HdContextRepository contextRepository;

    public DefaultUserInitializer(Keycloak keycloak, DefaultAdminProperties defaultAdminProperties, DefaultUserRepository defaultUserRepository, UserRepository userRepository,
                                  RoleService roleService, HdContextRepository contextRepository) {
        super(keycloak, userRepository);
        this.defaultAdminProperties = defaultAdminProperties;
        this.defaultUserRepository = defaultUserRepository;
        this.roleService = roleService;
        this.contextRepository = contextRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean initDefaultUsers() {
        boolean defaultUsersInitiated = false;
        String username = defaultAdminProperties.getUsername();
        String email = defaultAdminProperties.getEmail();
        if (email == null) {
            log.warn("No default admin properties set, omitting");
            return defaultUsersInitiated;
        }
        List<UserRepresentation> allUsersFromKeycloak = getAllUsersFromKeycloak();
        Optional<UserRepresentation> userByEmail = allUsersFromKeycloak.stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst();
        Optional<UserRepresentation> userByUsername = allUsersFromKeycloak.stream().filter(user -> user.getUsername().equalsIgnoreCase(username)).findFirst();

        if (userByEmail.isPresent() && userByUsername.isPresent() && !userByUsername.get().getId().equalsIgnoreCase(userByEmail.get().getId())) {
            log.info("Users fetched from the keycloak that apply to email {} or username {}:", email, username);
            allUsersFromKeycloak.stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(email) || user.getUsername().equalsIgnoreCase(username))
                    .forEach(userRepresentation -> log.info("Usr {}, username: {}, email: {}", userRepresentation.getId(), userRepresentation.getUsername(),
                            userRepresentation.getEmail()));
            throw new IllegalStateException(
                    String.format("There are already two different users in the keycloak for the provided username: %s and email: %s. Please change the configuration", username,
                            email));
        }

        // Check if the user has already been created in a previous run
        boolean userMarkedAsDefault = !defaultUserRepository.findByEmail(email).isEmpty();
        boolean userAlreadyCreated = userByUsername.isPresent();

        if (!userAlreadyCreated && !userMarkedAsDefault) {
            defaultUsersInitiated = createDefaultAdmin(defaultAdminProperties.getUsername(), defaultAdminProperties.getFirstName(), defaultAdminProperties.getLastName(),
                    defaultAdminProperties.getEmail());
        } else if (!userMarkedAsDefault) {
            defaultUsersInitiated = markAsDefaultUser(defaultAdminProperties.getEmail());
        }
        return defaultUsersInitiated;
    }

    private boolean markAsDefaultUser(String email) {
        Optional<UserEntity> userEntityByEmail = userRepository.findUserEntityByEmailIgnoreCase(email);
        if (userEntityByEmail.isPresent()) {
            //set as superuser
            UserEntity user = userEntityByEmail.get();
            setAsHellodataAdmin(user);
            userRepository.save(user);
            //mark as default user
            updateDefaultUser(user);
        } else {
            userNotFoundCreateUser();
        }
        return true;
    }

    private boolean createDefaultAdmin(String username, String firstName, String lastName, String email) {
        UserRepresentation user = generateUser(username, firstName, lastName, email);
        setDefaultAdminPassword(user);
        String userId = createUserInKeycloak(user);
        UserEntity userEntity = saveUserToDatabase(userId, defaultAdminProperties.getEmail(), firstName, lastName, username);
        setAsHellodataAdmin(userEntity);
        updateDefaultUser(userEntity);
        return true;
    }

    private void userNotFoundCreateUser() {
        log.warn("User {} not found in DB!", defaultAdminProperties.getEmail());
        Optional<UserRepresentation> first = getAllUsersFromKeycloak().stream().filter(user -> user.getEmail().equalsIgnoreCase(defaultAdminProperties.getEmail())).findFirst();
        if (first.isPresent()) {
            UserRepresentation userRepresentation = first.get();
            UserEntity userEntity = new UserEntity();
            userEntity.setId(UUID.fromString(userRepresentation.getId()));
            userEntity.setEmail(userRepresentation.getEmail());
            userEntity.setEnabled(true);
            UserEntity savedUser = userRepository.save(userEntity);
            setAsHellodataAdmin(savedUser);
            //mark as default user
            updateDefaultUser(savedUser);
        }
    }

    private void setAsHellodataAdmin(UserEntity savedUser) {
        if (contextRepository.count() > 0) {
            roleService.setBusinessDomainRoleForUser(savedUser, HdRoleName.HELLODATA_ADMIN);
            roleService.setAllDataDomainRolesForUser(savedUser, HdRoleName.DATA_DOMAIN_ADMIN);
        }
    }

    private void updateDefaultUser(UserEntity userEntity) {
        //update default_user
        DefaultUserEntity defaultUserEntity = new DefaultUserEntity();
        defaultUserEntity.setUser(userEntity);
        defaultUserRepository.saveAndFlush(defaultUserEntity);
    }

    private void setDefaultAdminPassword(UserRepresentation user) {
        setUserPassword(user, defaultAdminProperties.getPassword());
    }

    private void setUserPassword(UserRepresentation user, String password) {
        // Set the user's password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));
    }

    private List<UserRepresentation> getAllUsersFromKeycloak() {
        UsersResource usersResource = keycloak.realm(realmName).users();
        return usersResource.search(null, null, null, true, null).stream().filter(userRepresentation -> userRepresentation.getEmail() != null).toList();
    }
}
