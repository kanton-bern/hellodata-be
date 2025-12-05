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
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.initialize.entity.ExampleUsersCreatedEntity;
import ch.bedag.dap.hellodata.portal.initialize.event.InitializationCompletedEvent;
import ch.bedag.dap.hellodata.portal.initialize.event.SyncAllUsersEvent;
import ch.bedag.dap.hellodata.portal.initialize.repository.ExampleUsersCreatedRepository;
import ch.bedag.dap.hellodata.portal.profiles.CreateExampleUsersProfile;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.conf.ExampleUsersProperties;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
@CreateExampleUsersProfile
public class ExampleUsersInitializer extends AbstractUserInitializer implements ApplicationListener<InitializationCompletedEvent> {

    private final RoleService roleService;
    private final HelloDataContextConfig helloDataContextConfig;
    private final ExampleUsersProperties exampleUsersProperties;
    private final ExampleUsersCreatedRepository exampleUsersCreatedRepository;
    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HdContextRepository hdContextRepository;

    @Value("${hello-data.example-users.email-postfix}")
    private String emailPostfix;

    public ExampleUsersInitializer(Keycloak keycloak, RoleService roleService, HelloDataContextConfig helloDataContextConfig, ExampleUsersProperties exampleUsersProperties,
                                   ExampleUsersCreatedRepository exampleUsersCreatedRepository, UserRepository userRepository, UserService userService,
                                   ApplicationEventPublisher applicationEventPublisher, HdContextRepository hdContextRepository) {
        super(keycloak, userRepository);
        this.roleService = roleService;
        this.helloDataContextConfig = helloDataContextConfig;
        this.exampleUsersProperties = exampleUsersProperties;
        this.exampleUsersCreatedRepository = exampleUsersCreatedRepository;
        this.userService = userService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.hdContextRepository = hdContextRepository;
    }

    @Override
    @Transactional
    public void onApplicationEvent(InitializationCompletedEvent event) {
        initExampleUsers();
    }

    private void initExampleUsers() {
        log.debug("Creating example users for all data domains setting is ON.");
        List<RoleDto> allRoles = roleService.getAll();
        String businessDomainName = helloDataContextConfig.getBusinessContext().getName();
        createBusinessDomainAdmin(businessDomainName);
        List<HdContextEntity> dataDomains = hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        boolean exampleUsersCreated = areExampleUsersCreated(dataDomains);
        if (exampleUsersCreated) {
            log.debug("Example users already created, omitting...");
            return;
        }
        for (HdContextEntity dataDomain : dataDomains) {
            log.debug("Creating example users for DD {}, context key: {}", dataDomain.getName(), dataDomain.getContextKey());
            String dataDomainName = dataDomain.getName();
            String dataDomainPrefixEmail = dataDomainName.trim().replace(' ', '-') + "-";
            createDataDomainUser(dataDomain.getContextKey(), dataDomainPrefixEmail, dataDomainName, allRoles, HdRoleName.DATA_DOMAIN_ADMIN, "Admin", exampleUsersProperties.getDataDomainAdminPassword());
            createDataDomainUser(dataDomain.getContextKey(), dataDomainPrefixEmail, dataDomainName, allRoles, HdRoleName.DATA_DOMAIN_EDITOR, "Editor", exampleUsersProperties.getDataDomainEditorPassword());
            createDataDomainUser(dataDomain.getContextKey(), dataDomainPrefixEmail, dataDomainName, allRoles, HdRoleName.DATA_DOMAIN_VIEWER, "Viewer", exampleUsersProperties.getDataDomainViewerPassword());
            exampleUsersCreated = true;
        }
        if (exampleUsersCreated) {
            String commaSeparatedContextKeys = dataDomains.stream().map(HdContextEntity::getContextKey).collect(Collectors.joining(","));
            markExampleUsersCreated(commaSeparatedContextKeys);
            applicationEventPublisher.publishEvent(new SyncAllUsersEvent());
        }
    }

    private void markExampleUsersCreated(String commaSeparatedContextKeys) {
        List<ExampleUsersCreatedEntity> all = exampleUsersCreatedRepository.findAll();
        ExampleUsersCreatedEntity exampleUsersCreatedEntity;
        if (all.isEmpty()) {
            exampleUsersCreatedEntity = new ExampleUsersCreatedEntity();
        } else {
            exampleUsersCreatedEntity = all.get(0);
        }
        exampleUsersCreatedEntity.setDataDomainList(commaSeparatedContextKeys);
        exampleUsersCreatedRepository.save(exampleUsersCreatedEntity);
    }

    boolean areExampleUsersCreated(List<HdContextEntity> dataDomains) {
        List<ExampleUsersCreatedEntity> all = exampleUsersCreatedRepository.findAllByOrderByCreatedDateAsc();
        if (all.isEmpty()) {
            return false;
        }
        if (all.size() > 1) {
            for (int i = 0; i < all.size() - 1; i++) {
                exampleUsersCreatedRepository.delete(all.get(i));
            }
            all = exampleUsersCreatedRepository.findAllByOrderByCreatedDateAsc();
        }
        ExampleUsersCreatedEntity exampleUsersCreatedEntity = all.get(0);
        for (HdContextEntity dataDomain : dataDomains) {
            if (exampleUsersCreatedEntity.getDataDomainList() == null || !exampleUsersCreatedEntity.getDataDomainList().contains(dataDomain.getContextKey())) {
                return false;
            }
        }
        return true;
    }


    private void createBusinessDomainAdmin(String businessDomainName) {
        String adminUsername = businessDomainName.replace(' ', '-') + "-admin";
        String email = (adminUsername + "@" + emailPostfix).toLowerCase(Locale.ROOT);
        String firstName = HdContextType.BUSINESS_DOMAIN.getTypeName() + " " + businessDomainName;
        String lastName = "Admin";

        List<UserRepresentation> userRepresentations = keycloak.realm(this.realmName).users().searchByEmail(email, true);
        String userId;
        if (!userRepresentations.isEmpty()) {
            log.debug("BUSINESS DOMAIN ADMIN {} already exists in keycloak", email);
            userId = userRepresentations.get(0).getId();
        } else {
            UserRepresentation user = generateUser(adminUsername, firstName, lastName, email);
            setUserPassword(user, this.exampleUsersProperties.getBusinessDomainAdminPassword());
            userId = createUserInKeycloak(user);
        }
        if (!userRepository.existsByIdOrAuthId(UUID.fromString(userId), userId)) {
            UserEntity bdAdminEntity = saveUserToDatabase(userId, email, firstName, lastName, adminUsername);
            roleService.setBusinessDomainRoleForUser(bdAdminEntity, HdRoleName.BUSINESS_DOMAIN_ADMIN);
            bdAdminEntity = userRepository.getReferenceById(bdAdminEntity.getId());
            roleService.setAllDataDomainRolesForUser(bdAdminEntity, HdRoleName.DATA_DOMAIN_ADMIN);
            userService.createUserInSubsystems(userId);
            log.debug("CREATED BUSINESS DOMAIN ADMIN EXAMPLE USER, email: {}, username: {}", email, adminUsername);
        } else {
            log.debug("BUSINESS DOMAIN ADMIN {} already exists in portal DB", email);
        }
    }

    private void createDataDomainUser(String contextKey, String dataDomainPrefixEmail, String dataDomainName, List<RoleDto> allRoles, HdRoleName roleName, String lastName, String password) {
        String username = dataDomainPrefixEmail + roleName.name().toLowerCase(Locale.ROOT).replace("data_domain_", "");
        String email = (username + "@" + emailPostfix).toLowerCase(Locale.ROOT);
        String firstName = HdContextType.DATA_DOMAIN.getTypeName() + " " + dataDomainName;

        List<UserRepresentation> userRepresentations = keycloak.realm(this.realmName).users().searchByEmail(email, true);
        String userId;
        if (!userRepresentations.isEmpty()) {
            log.debug("{} {} already exists in keycloak", roleName, email);
            userId = userRepresentations.get(0).getId();
        } else {
            UserRepresentation user = generateUser(username, firstName, lastName, email);
            setUserPassword(user, password);
            userId = createUserInKeycloak(user);
        }
        try {
            if (!userRepository.existsByIdOrAuthId(UUID.fromString(userId), userId)) {
                UserEntity userEntity = saveUserToDatabase(userId, email, firstName, lastName, username);
                roleService.setBusinessDomainRoleForUser(userEntity, HdRoleName.NONE);
                userEntity = userRepository.getReferenceById(userEntity.getId());
                Optional<RoleDto> roleResult = allRoles.stream()
                        .filter(role -> role.getContextType() == HdContextType.DATA_DOMAIN &&
                                role.getName().equalsIgnoreCase(roleName.name()))
                        .findFirst();
                UserEntity finalUserEntity = userEntity;
                roleResult.ifPresent(role -> roleService.updateDomainRoleForUser(finalUserEntity, role, contextKey));
                userService.createUserInSubsystems(userId);
                log.debug("CREATED {} EXAMPLE USER, email: {}, username: {}", roleName, email, username);
            } else {
                log.debug("{} {} already exists in portal DB", roleName, email);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID for userId: {}", userId, e);
        }
    }

    private void setUserPassword(UserRepresentation user, String password) {
        // Set the user's password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));
    }
}
