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

import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.initialize.entity.ExampleUsersCreatedEntity;
import ch.bedag.dap.hellodata.portal.initialize.event.InitializationCompletedEvent;
import ch.bedag.dap.hellodata.portal.initialize.event.SyncAllUsersEvent;
import ch.bedag.dap.hellodata.portal.initialize.repository.ExampleUsersCreatedRepository;
import ch.bedag.dap.hellodata.portal.metainfo.entity.HdContextEntity;
import ch.bedag.dap.hellodata.portal.metainfo.repository.HdContextRepository;
import ch.bedag.dap.hellodata.portal.profiles.CreateExampleUsersProfile;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.conf.ExampleUsersProperties;
import ch.bedag.dap.hellodata.portal.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portal.user.repository.UserRepository;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApplicationEvent(InitializationCompletedEvent event) {
        initExampleUsers();
    }

    private void initExampleUsers() {
        log.info("Creating example users for all data domains setting is ON.");
        List<RoleDto> allRoles = roleService.getAll();
        String businessDomainName = helloDataContextConfig.getBusinessContext().getName();
        createBusinessDomainAdmin(businessDomainName);
        List<HdContextEntity> dataDomains = hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        boolean exampleUsersCreated = areExampleUsersCreated(dataDomains);
        if (exampleUsersCreated) {
            log.info("Example users already created, omitting...");
            return;
        }
        for (HdContextEntity dataDomain : dataDomains) {
            log.info("Creating example users for DD {}, context key: {}", dataDomain.getName(), dataDomain.getContextKey());
            String dataDomainName = dataDomain.getName();
            String dataDomainPrefixEmail = dataDomainName.trim().replace(' ', '-') + "-";
            createDataDomainAdmin(dataDomain.getContextKey(), dataDomainPrefixEmail, dataDomainName, allRoles);
            createDataDomainEditor(dataDomain.getContextKey(), dataDomainPrefixEmail, dataDomainName, allRoles);
            createDataDomainViewer(dataDomain.getContextKey(), dataDomainPrefixEmail, dataDomainName, allRoles);
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

    private boolean areExampleUsersCreated(List<HdContextEntity> dataDomains) {
        List<ExampleUsersCreatedEntity> all = exampleUsersCreatedRepository.findAllByOrderByCreatedDateAsc();
        if (all.isEmpty()) {
            return false;
        }
        // in case more entries found - remove them and leave one
        if (all.size() > 1) {
            for (int i = 0; i < all.size() - 1; i++) {
                ExampleUsersCreatedEntity entity = all.get(i);
                exampleUsersCreatedRepository.delete(entity);
                all.remove(entity);
            }
        }
        ExampleUsersCreatedEntity exampleUsersCreatedEntity = all.get(0);
        for (HdContextEntity dataDomain : dataDomains) {
            if (!exampleUsersCreatedEntity.getDataDomainList().contains(dataDomain.getContextKey())) {
                return false;
            }
        }
        return true;
    }

    private void createDataDomainViewer(String contextKey, String dataDomainPrefixEmail, String dataDomainName, List<RoleDto> allRoles) {
        String ddViewerUsername = dataDomainPrefixEmail + "viewer";
        String ddViewerEmail = (ddViewerUsername + "@" + emailPostfix).toLowerCase(Locale.ROOT);
        String ddViewerFirstName = HdContextType.DATA_DOMAIN.getTypeName() + " " + dataDomainName;
        String ddViewerLastName = "Viewer";

        List<UserRepresentation> userRepresentations = keycloak.realm(this.realmName).users().searchByEmail(ddViewerEmail, true);
        String ddViewerId;
        if (!userRepresentations.isEmpty()) {
            log.info("DATA DOMAIN VIEWER {} already exists in keycloak", ddViewerEmail);
            ddViewerId = userRepresentations.get(0).getId();
        } else {
            UserRepresentation ddViewer = generateUser(ddViewerUsername, ddViewerFirstName, ddViewerLastName, ddViewerEmail);
            setUserPassword(ddViewer, this.exampleUsersProperties.getDataDomainViewerPassword());
            ddViewerId = createUserInKeycloak(ddViewer);
        }
        if (!userRepository.existsByIdOrAuthId(UUID.fromString(ddViewerId), ddViewerId)) {
            UserEntity ddViewerEntity = saveUserToDatabase(ddViewerId, ddViewerEmail);
            roleService.setBusinessDomainRoleForUser(ddViewerEntity, HdRoleName.NONE);
            ddViewerEntity = userRepository.getReferenceById(ddViewerEntity.getId());
            Optional<RoleDto> ddViewerRoleResult = allRoles.stream()
                                                           .filter(role -> role.getContextType() == HdContextType.DATA_DOMAIN &&
                                                                           role.getName().equalsIgnoreCase(HdRoleName.DATA_DOMAIN_VIEWER.name()))
                                                           .findFirst();
            if (ddViewerRoleResult.isPresent()) {
                RoleDto ddViewerRole = ddViewerRoleResult.get();
                roleService.updateDomainRoleForUser(ddViewerEntity, ddViewerRole, contextKey);
            }
            userService.createUserInSubsystems(ddViewerId);
            log.info("CREATED DATA DOMAIN VIEWER EXAMPLE USER, email: {}, username: {}", ddViewerEmail, ddViewerUsername);
        } else {
            log.info("DATA DOMAIN VIEWER {} already exists in portal DB", ddViewerEmail);
        }
    }

    private void createDataDomainEditor(String contextKey, String dataDomainPrefixEmail, String dataDomainName, List<RoleDto> allRoles) {
        String ddEditorUsername = dataDomainPrefixEmail + "editor";
        String ddEditorEmail = (ddEditorUsername + "@" + emailPostfix).toLowerCase(Locale.ROOT);
        String ddEditorFirstName = HdContextType.DATA_DOMAIN.getTypeName() + " " + dataDomainName;
        String ddEditorLastName = "Editor";

        List<UserRepresentation> userRepresentations = keycloak.realm(this.realmName).users().searchByEmail(ddEditorEmail, true);
        String ddEditorId;
        if (!userRepresentations.isEmpty()) {
            log.info("DATA DOMAIN EDITOR {} already exists in keycloak", ddEditorEmail);
            ddEditorId = userRepresentations.get(0).getId();
        } else {
            UserRepresentation ddEditor = generateUser(ddEditorUsername, ddEditorFirstName, ddEditorLastName, ddEditorEmail);
            setUserPassword(ddEditor, this.exampleUsersProperties.getDataDomainEditorPassword());
            ddEditorId = createUserInKeycloak(ddEditor);
        }
        if (!userRepository.existsByIdOrAuthId(UUID.fromString(ddEditorId), ddEditorId)) {
            UserEntity ddEditorEntity = saveUserToDatabase(ddEditorId, ddEditorEmail);
            roleService.setBusinessDomainRoleForUser(ddEditorEntity, HdRoleName.NONE);
            ddEditorEntity = userRepository.getReferenceById(ddEditorEntity.getId());
            Optional<RoleDto> ddEditorRoleResult = allRoles.stream()
                                                           .filter(role -> role.getContextType() == HdContextType.DATA_DOMAIN &&
                                                                           role.getName().equalsIgnoreCase(HdRoleName.DATA_DOMAIN_EDITOR.name()))
                                                           .findFirst();
            if (ddEditorRoleResult.isPresent()) {
                RoleDto ddEditorRole = ddEditorRoleResult.get();
                roleService.updateDomainRoleForUser(ddEditorEntity, ddEditorRole, contextKey);
            }
            userService.createUserInSubsystems(ddEditorId);
            log.info("CREATED DATA DOMAIN EDITOR EXAMPLE USER, email: {}, username: {}", ddEditorEmail, ddEditorUsername);
        } else {
            log.info("DATA DOMAIN EDITOR {} already exists in portal DB", ddEditorEmail);
        }
    }

    private void createDataDomainAdmin(String contextKey, String dataDomainPrefixEmail, String dataDomainName, List<RoleDto> allRoles) {
        String dataDomainAdminUsername = dataDomainPrefixEmail + "admin";
        String ddAdminEmail = (dataDomainAdminUsername + "@" + emailPostfix).toLowerCase(Locale.ROOT);
        String ddAdminFirstName = HdContextType.DATA_DOMAIN.getTypeName() + " " + dataDomainName;
        String ddAdminLastName = "Admin";

        List<UserRepresentation> userRepresentations = keycloak.realm(this.realmName).users().searchByEmail(ddAdminEmail, true);
        String ddAdminId;
        if (!userRepresentations.isEmpty()) {
            log.info("DATA DOMAIN ADMIN {} already exists in keycloak", ddAdminEmail);
            ddAdminId = userRepresentations.get(0).getId();
        } else {
            UserRepresentation ddAdmin = generateUser(dataDomainAdminUsername, ddAdminFirstName, ddAdminLastName, ddAdminEmail);
            setUserPassword(ddAdmin, this.exampleUsersProperties.getDataDomainAdminPassword());
            ddAdminId = createUserInKeycloak(ddAdmin);
        }
        if (!userRepository.existsByIdOrAuthId(UUID.fromString(ddAdminId), ddAdminId)) {
            UserEntity ddAdminEntity = saveUserToDatabase(ddAdminId, ddAdminEmail);
            roleService.setBusinessDomainRoleForUser(ddAdminEntity, HdRoleName.NONE);
            ddAdminEntity = userRepository.getReferenceById(ddAdminEntity.getId());
            Optional<RoleDto> ddAdminRoleResult = allRoles.stream()
                                                          .filter(role -> role.getContextType() == HdContextType.DATA_DOMAIN &&
                                                                          role.getName().equalsIgnoreCase(HdRoleName.DATA_DOMAIN_ADMIN.name()))
                                                          .findFirst();
            if (ddAdminRoleResult.isPresent()) {
                RoleDto ddAdminRole = ddAdminRoleResult.get();
                roleService.updateDomainRoleForUser(ddAdminEntity, ddAdminRole, contextKey);
            }
            userService.createUserInSubsystems(ddAdminId);
            log.info("CREATED DATA DOMAIN ADMIN EXAMPLE USER, email: {}, username: {}", ddAdminEmail, dataDomainAdminUsername);
        } else {
            log.info("DATA DOMAIN ADMIN {} already exists in portal DB", ddAdminEmail);
        }
    }

    private void createBusinessDomainAdmin(String businessDomainName) {
        String adminUsername = businessDomainName.replace(' ', '-') + "-admin";
        String email = (adminUsername + "@" + emailPostfix).toLowerCase(Locale.ROOT);
        String firstName = HdContextType.BUSINESS_DOMAIN.getTypeName() + " " + businessDomainName;
        String lastName = "Admin";

        List<UserRepresentation> userRepresentations = keycloak.realm(this.realmName).users().searchByEmail(email, true);
        String userId;
        if (!userRepresentations.isEmpty()) {
            log.info("BUSINESS DOMAIN ADMIN {} already exists in keycloak", email);
            userId = userRepresentations.get(0).getId();
        } else {
            UserRepresentation user = generateUser(adminUsername, firstName, lastName, email);
            setUserPassword(user, this.exampleUsersProperties.getBusinessDomainAdminPassword());
            userId = createUserInKeycloak(user);
        }
        if (!userRepository.existsByIdOrAuthId(UUID.fromString(userId), userId)) {
            UserEntity bdAdminEntity = saveUserToDatabase(userId, email);
            roleService.setBusinessDomainRoleForUser(bdAdminEntity, HdRoleName.BUSINESS_DOMAIN_ADMIN);
            bdAdminEntity = userRepository.getReferenceById(bdAdminEntity.getId());
            roleService.setAllDataDomainRolesForUser(bdAdminEntity, HdRoleName.DATA_DOMAIN_ADMIN);
            userService.createUserInSubsystems(userId);
            log.info("CREATED BUSINESS DOMAIN ADMIN EXAMPLE USER, email: {}, username: {}", email, adminUsername);
        } else {
            log.info("BUSINESS DOMAIN ADMIN {} already exists in portal DB", email);
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
