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
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserDelete;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.email.service.EmailNotificationService;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.UserAlreadyExistsException;
import ch.bedag.dap.hellodata.portal.user.data.*;
import ch.bedag.dap.hellodata.portal.user.event.UserFullSyncEvent;
import ch.bedag.dap.hellodata.portal.user.util.UserDtoMapper;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.DataDomainRoleDto;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.keycloak.representations.idm.UserRepresentation;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {

    private static final List<String> REQUIRED_ACTIONS = List.of("VERIFY_EMAIL", "UPDATE_PROFILE", "UPDATE_PASSWORD");
    private final KeycloakService keycloakService;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final MetaInfoResourceService metaInfoResourceService;
    private final NatsSenderService natsSenderService;
    private final HdContextRepository contextRepository;
    private final RoleService roleService;
    private final EmailNotificationService emailNotificationService;
    private final HelloDataContextConfig helloDataContextConfig;
    private final UserSelectedDashboardService userSelectedDashboardService;
    private final DashboardGroupService dashboardGroupService;
    private final ApplicationEventPublisher eventPublisher;

    private final UserLookupService userLookupService;
    private final UserPreferenceService userPreferenceService;
    private final UserContextRoleService userContextRoleService;

    @Transactional
    public String createUser(String email, String firstName, String lastName, AdUserOrigin origin) {
        email = email.toLowerCase(Locale.ROOT);
        log.info("Creating user. Email: {}, first name: {}, last name {}", email, firstName, lastName);
        validateEmailAlreadyExists(email);
        boolean isFederated = origin != AdUserOrigin.LOCAL;
        return handleUserCreation(email, firstName, lastName, isFederated);
    }

    @Transactional(readOnly = true)
    public boolean isUserDisabled(String userId) {
        UserEntity userEntity = getUserEntity(userId);
        return !userEntity.isEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isFirstLogin(String userId) {
        UserEntity userEntity = getUserEntity(userId);
        return userEntity.getLastAccess() == null;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        List<UserEntity> allPortalUsers = userRepository.findAll();
        return allPortalUsers.stream()
                .map(UserDtoMapper::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserWithBusinessRoleDto> getAllUsersWithBusinessDomainRole() {
        List<UserEntity> allPortalUsers = userRepository.findAll();
        Map<String, String> contextKeyToName = contextRepository.findAll().stream()
                .collect(Collectors.toMap(HdContextEntity::getContextKey, HdContextEntity::getName, (a, b) -> a));
        return allPortalUsers.stream()
                .map(user -> mapWithBusinessDomainRole(user, contextKeyToName))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserWithBusinessRoleDto> getAllUsersWithBusinessDomainRolePaginated(Pageable pageable, String search) {
        Page<UserEntity> usersPage;
        if (search == null || search.isEmpty()) {
            usersPage = userRepository.findAll(pageable);
        } else {
            usersPage = userRepository.findAll(pageable, search);
        }
        Map<String, String> contextKeyToName = contextRepository.findAll().stream()
                .collect(Collectors.toMap(HdContextEntity::getContextKey, HdContextEntity::getName, (a, b) -> a));
        return usersPage.map(user -> mapWithBusinessDomainRole(user, contextKeyToName));
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsersPageable(Pageable pageable, String search) {
        Page<UserEntity> allPortalUsers;
        if (search == null || search.isEmpty()) {
            allPortalUsers = userRepository.findAll(pageable);
        } else {
            allPortalUsers = userRepository.findAll(pageable, search);
        }
        return allPortalUsers.map(UserDtoMapper::map);
    }

    @Transactional
    public void deleteUserById(String userId) {
        UUID dbId = UUID.fromString(userId);
        validateNotAllowedIfCurrentUserIsNotSuperuser(dbId);
        if (SecurityUtils.getCurrentUserId() == null || userId.equals(SecurityUtils.getCurrentUserId().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete yourself");//NOSONAR
        }
        UserEntity userEntity = getUserEntity(dbId);
        // Remove user from all dashboard groups before deleting the user entity
        userContextRoleService.removeUserFromDashboardGroupsForAllDomains(dbId);
        userRepository.delete(userEntity);
        // The user is intentionally NOT removed from the auth provider (Keycloak): the realm can be
        // shared across environments and/or federated with AD, so the portal must not touch it.
        SubsystemUserDelete subsystemUserDelete = new SubsystemUserDelete();
        String email = userEntity.getEmail().toLowerCase(Locale.ROOT);
        subsystemUserDelete.setEmail(email);
        subsystemUserDelete.setUsername(email);
        natsSenderService.publishMessageToJetStream(HDEvent.DELETE_USER, subsystemUserDelete);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(String userId) {
        UserEntity userEntity = getUserEntity(userId);
        return UserDtoMapper.map(userEntity);
    }

    @Transactional
    public void updateLastAccess(String userId) {
        UserEntity userEntity = getUserEntity(userId);
        userEntity.setLastAccess(LocalDateTime.now());
        userRepository.save(userEntity);
    }

    @Transactional(readOnly = true)
    public void createUserInSubsystems(String userId) {
        createInSubsystems(userId);
    }

    @Transactional
    public UserDto disableUserById(String userId) {
        UUID dbId = UUID.fromString(userId);
        validateNotAllowedIfCurrentUserIsNotSuperuser(dbId);
        validateNotAllowedIfUserIsSuperuser(dbId);
        UserEntity userEntity = getUserEntity(userId);
        userEntity.setEnabled(false);
        userRepository.save(userEntity);
        // Access is revoked in the portal and in the subsystems (below); the auth provider (Keycloak)
        // is intentionally left untouched, as the realm can be shared across environments / federated with AD.
        SubsystemUserUpdate subsystemUserUpdate = getSubsystemUserUpdate(userEntity.getEmail(), userEntity.getUsername(), userEntity.getFirstName(), userEntity.getLastName());
        subsystemUserUpdate.setActive(false);
        natsSenderService.publishMessageToJetStream(HDEvent.DISABLE_USER, subsystemUserUpdate);
        emailNotificationService.notifyAboutUserDeactivation(userEntity.getFirstName(), userEntity.getEmail(), userPreferenceService.getSelectedLanguageByEmail(userEntity.getEmail()));
        return UserDtoMapper.map(userEntity);
    }

    @Transactional
    public UserDto enableUserById(String userId) {
        UUID dbId = UUID.fromString(userId);
        validateNotAllowedIfCurrentUserIsNotSuperuser(dbId);
        UserEntity userEntity = getUserEntity(userId);
        userEntity.setEnabled(true);
        userRepository.saveAndFlush(userEntity);
        // Access is restored in the portal and in the subsystems (below); the auth provider (Keycloak)
        // is intentionally left untouched, as the realm can be shared across environments / federated with AD.
        SubsystemUserUpdate subsystemUserUpdate = getSubsystemUserUpdate(userEntity.getEmail(), userEntity.getUsername(), userEntity.getFirstName(), userEntity.getLastName());
        subsystemUserUpdate.setActive(true);
        natsSenderService.publishMessageToJetStream(HDEvent.ENABLE_USER, subsystemUserUpdate);
        eventPublisher.publishEvent(new UserFullSyncEvent(dbId, true, new HashMap<>(), null));
        emailNotificationService.notifyAboutUserActivation(userEntity.getFirstName(), userEntity.getEmail(), userEntity.getSelectedLanguage());
        return UserDtoMapper.map(userEntity);
    }

    @Transactional
    public DashboardsDto getDashboardsMarkUser(String userId) {
        DashboardsDto result = new DashboardsDto();
        result.setDashboards(new ArrayList<>());
        List<MetaInfoResourceEntity> dashboardsWithContext = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS);
        UserEntity userEntity = getUserEntity(userId);
        UUID userUuid = userEntity.getId();

        // Collect valid dashboard IDs per context for stale cleanup
        Map<String, Set<Integer>> validDashboardIdsByContext = new HashMap<>();

        for (MetaInfoResourceEntity dashboardWithContext : dashboardsWithContext) {
            if (dashboardWithContext.getMetainfo() instanceof DashboardResource dashboardResource) {
                String contextKey = dashboardWithContext.getContextKey();
                SubsystemUser subsystemUser = metaInfoResourceService.findUserInInstance(userEntity.getEmail(), dashboardResource.getInstanceName());
                if (subsystemUser == null) {
                    log.warn("User {} not found in instance {}", userEntity.getEmail(), dashboardResource.getInstanceName());
                }

                // Get selected dashboard IDs from persisted table
                Set<Integer> selectedIds = userSelectedDashboardService.getSelectedDashboardIds(userUuid, contextKey);

                List<SupersetDashboard> data = dashboardResource.getData().stream().filter(SupersetDashboard::isPublished).toList();
                Set<Integer> contextValidIds = validDashboardIdsByContext.computeIfAbsent(contextKey, k -> new HashSet<>());

                for (SupersetDashboard supersetDashboard : data) {
                    contextValidIds.add(supersetDashboard.getId());
                    boolean isViewer = selectedIds.contains(supersetDashboard.getId());
                    result.getDashboards().add(createDashboardDtoFromSelection(dashboardResource, subsystemUser, supersetDashboard, contextKey, isViewer));
                }
            }
        }

        // Cleanup stale dashboard selections
        for (Map.Entry<String, Set<Integer>> entry : validDashboardIdsByContext.entrySet()) {
            userSelectedDashboardService.cleanupStaleDashboards(userUuid, entry.getKey(), entry.getValue());
            dashboardGroupService.cleanupStaleDashboardsInGroups(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public ContextsDto getAvailableContexts() {
        return userContextRoleService.getAvailableContexts();
    }

    @Transactional
    public void updateContextRolesForUser(UUID userId, UpdateContextRolesForUserDto updateContextRolesForUserDto, boolean sendBackUserList) {
        userContextRoleService.updateContextRolesForUser(userId, updateContextRolesForUserDto, sendBackUserList);
    }

    @Transactional
    public void updateContextRolesForUserFromBatch(UUID userId, UpdateContextRolesForUserDto updateContextRolesForUserDto, boolean sendBackUserList) {
        userContextRoleService.updateContextRolesForUserFromBatch(userId, updateContextRolesForUserDto, sendBackUserList);
    }

    @Transactional(readOnly = true)
    public List<UserContextRoleDto> getContextRolesForUser(UUID userId) {
        return userContextRoleService.getContextRolesForUser(userId);
    }

    @Transactional(readOnly = true)
    public boolean isUserSuperuser(UUID userId) {
        return userContextRoleService.isUserSuperuser(userId);
    }

    @Transactional(readOnly = true)
    public Set<String> getUserPortalPermissions(UUID userId) {
        return userContextRoleService.getUserPortalPermissions(userId);
    }

    @Transactional(readOnly = true)
    public Set<UserContextRoleEntity> getCurrentUserDataDomainRolesWithoutNone() {
        return userContextRoleService.getCurrentUserDataDomainRolesWithoutNone();
    }

    @Transactional(readOnly = true)
    public void validateUserHasAccessToContext(String contextKey, String reason) {
        userContextRoleService.validateUserHasAccessToContext(contextKey, reason);
    }

    @Transactional(readOnly = true)
    public List<AdUserDto> searchUserOmitCreated(String email) {
        return userLookupService.searchUserOmitCreated(email);
    }

    @Transactional(readOnly = true)
    public List<AdUserDto> searchUser(String email) {
        return userLookupService.searchUser(email);
    }

    @Transactional(readOnly = true)
    public List<DataDomainDto> getAvailableDataDomains() {
        return userContextRoleService.getAvailableDataDomains();
    }

    @Transactional(readOnly = true)
    public List<UserEntity> findHelloDataAdminUsers() {
        List<UserEntity> businessDomainAdmins = userRepository.findUsersByHdRoleName(HdRoleName.BUSINESS_DOMAIN_ADMIN).stream().filter(UserEntity::isEnabled).toList();
        if (!businessDomainAdmins.isEmpty()) {
            return businessDomainAdmins;
        }
        return userRepository.findUsersByHdRoleName(HdRoleName.HELLODATA_ADMIN).stream().filter(UserEntity::isEnabled).toList();
    }

    @Transactional
    public void setSelectedLanguage(String userId, Locale lang) {
        userPreferenceService.setSelectedLanguage(userId, lang);
    }

    @Transactional(readOnly = true)
    public Locale getSelectedLanguage(String userId) {
        return userPreferenceService.getSelectedLanguage(userId);
    }

    private String handleUserCreation(String email, String firstName, String lastName, boolean isFederated) {
        UserRepresentation userFoundInKeycloak = keycloakService.getUserRepresentationByEmail(email);
        String keycloakUserId;
        // If it is a local user we can create a new user in keycloak. Federated users are not created in keycloak
        if (null == userFoundInKeycloak) {
            if (isFederated) {
                //For federated users that are not in keycloak yet, we will just fake the keycloak id
                keycloakUserId = UUID.randomUUID().toString();
                log.info("Federated user {}, creating with random user id {}", email, keycloakUserId);
            } else {
                log.info("User {} doesn't not exist in the keycloak, creating", email);
                keycloakUserId = createKeycloakUser(email, firstName, lastName);
            }
        } else {
            //If the user already exists in keycloak, we will just use the id from keycloak
            log.info("User {} already exists in the keycloak, creating only in portal", userFoundInKeycloak.getId());
            keycloakUserId = userFoundInKeycloak.getId();
        }

        String username = userFoundInKeycloak == null ? email : userFoundInKeycloak.getUsername();
        String firstname = userFoundInKeycloak == null ? firstName : userFoundInKeycloak.getFirstName();
        String lastname = userFoundInKeycloak == null ? lastName : userFoundInKeycloak.getLastName();

        createPortalUserWithRoles(email, username, firstname, lastname, isFederated, keycloakUserId);

        if (isFederated) {
            createFederatedUserInSubsystems(email, username, firstname, lastname);
        } else {
            createInSubsystems(keycloakUserId);
        }
        return keycloakUserId;
    }

    private String createKeycloakUser(String email, String firstName, String lastName) {
        String keycloakUserId;
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setRequiredActions(REQUIRED_ACTIONS);
        keycloakUserId = keycloakService.createUser(user);
        log.info("Created keycloak user {} with id {}", email, keycloakUserId);
        return keycloakUserId;
    }

    private void createPortalUserWithRoles(String email, String username, String firstName, String lastName,
                                           boolean isFederated, String keycloakUserId) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(UUID.fromString(keycloakUserId));
        userEntity.setEmail(email);
        userEntity.setUsername(username);
        userEntity.setFirstName(firstName);
        userEntity.setLastName(lastName);
        userEntity.setEnabled(true);
        userEntity.setSuperuser(false);
        userEntity.setFederated(isFederated);
        userRepository.saveAndFlush(userEntity);
        roleService.setBusinessDomainRoleForUser(userEntity, HdRoleName.NONE);
        roleService.setAllDataDomainRolesForUser(userEntity, HdRoleName.NONE);
    }

    private void validateEmailAlreadyExists(String email) {
        Optional<UserEntity> userEntityByEmail = userRepository.findUserEntityByEmailIgnoreCase(email);
        if (userEntityByEmail.isPresent()) {
            throw new UserAlreadyExistsException();
        }
    }

    private void createInSubsystems(String userId) {
        SubsystemUserUpdate createUser = getSubsystemUserUpdate(userId);
        createUser.setSendBackUsersList(false);
        natsSenderService.publishMessageToJetStream(HDEvent.CREATE_USER, createUser);
    }

    private void createFederatedUserInSubsystems(String email, String userName, String firstName, String lastName) {
        SubsystemUserUpdate createUser = getSubsystemUserUpdate(email, userName, firstName, lastName);
        createUser.setSendBackUsersList(false);
        natsSenderService.publishMessageToJetStream(HDEvent.CREATE_USER, createUser);
    }

    private SubsystemUserUpdate getSubsystemUserUpdate(UserRepresentation representation) {
        SubsystemUserUpdate createUser = new SubsystemUserUpdate();
        createUser.setFirstName(representation.getFirstName());
        createUser.setLastName(representation.getLastName());
        createUser.setUsername(representation.getEmail().toLowerCase(Locale.ROOT));
        createUser.setEmail(representation.getEmail().toLowerCase(Locale.ROOT));
        createUser.setActive(representation.isEnabled());
        return createUser;
    }

    private SubsystemUserUpdate getSubsystemUserUpdate(String userId) {
        UserRepresentation representation = getUserRepresentation(userId);
        return getSubsystemUserUpdate(representation);
    }

    private SubsystemUserUpdate getSubsystemUserUpdate(String email, String username, String firstname, String lastname) {
        log.info("Creating user in subsystem. Email: {}, first name: {}, last name: {} (username: {})", email, firstname, lastname, username);
        SubsystemUserUpdate createUser = new SubsystemUserUpdate();
        createUser.setFirstName(firstname);
        createUser.setLastName(lastname);
        createUser.setUsername(email);
        createUser.setEmail(email);
        createUser.setActive(true);
        return createUser;
    }

    /**
     * Creates a DashboardForUserDto using persisted viewer status instead of deriving it from Superset roles
     */
    private DashboardForUserDto createDashboardDtoFromSelection(DashboardResource dashboardResource, SubsystemUser subsystemUser,
                                                                SupersetDashboard supersetDashboard, String contextKey, boolean isViewer) {
        DashboardForUserDto dashboardForUserDto = new DashboardForUserDto();
        dashboardForUserDto.setId(supersetDashboard.getId());
        dashboardForUserDto.setTitle(supersetDashboard.getDashboardTitle());
        if (subsystemUser != null) {
            dashboardForUserDto.setInstanceUserId(subsystemUser.getId());
        }
        dashboardForUserDto.setViewer(isViewer);
        dashboardForUserDto.setInstanceName(dashboardResource.getInstanceName());
        dashboardForUserDto.setChangedOnUtc(supersetDashboard.getChangedOnUtc());
        dashboardForUserDto.setCompositeId(dashboardResource.getInstanceName() + "_" + supersetDashboard.getId());
        dashboardForUserDto.setContextKey(contextKey);
        return dashboardForUserDto;
    }

    private UserWithBusinessRoleDto mapWithBusinessDomainRole(UserEntity userEntity, Map<String, String> contextKeyToName) {
        UserDto userDto = UserDtoMapper.map(userEntity);
        UserWithBusinessRoleDto userDtoWithBusinessRole;
        if (userDto != null) {
            userDtoWithBusinessRole = modelMapper.map(userDto, UserWithBusinessRoleDto.class);
            String businessContextKey = helloDataContextConfig.getBusinessContext().getKey();
            List<DataDomainRoleDto> dataDomainRoles = new ArrayList<>();
            for (UserContextRoleEntity ucr : userEntity.getContextRoles()) {
                if (ucr.getContextKey().equalsIgnoreCase(businessContextKey)) {
                    userDtoWithBusinessRole.setBusinessDomainRole(ucr.getRole().getName());
                } else {
                    String contextName = contextKeyToName.getOrDefault(ucr.getContextKey(), ucr.getContextKey());
                    dataDomainRoles.add(new DataDomainRoleDto(contextName, ucr.getContextKey(), ucr.getRole().getName()));
                }
            }
            userDtoWithBusinessRole.setDataDomainRoles(dataDomainRoles);
            return userDtoWithBusinessRole;
        } else {
            return null;
        }
    }

    /**
     * Only superuser can enable/disable other superusers
     *
     * @param userId user id
     */
    private void validateNotAllowedIfCurrentUserIsNotSuperuser(UUID userId) {
        UserEntity targetUser = getUserEntity(userId);
        if (!SecurityUtils.isSuperuser() && Boolean.TRUE.equals(targetUser.isSuperuser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed if user is a superuser");
        }
    }

    private void validateNotAllowedIfUserIsSuperuser(UUID userId) {
        UserEntity targetUser = getUserEntity(userId);
        if (Boolean.TRUE.equals(targetUser.isSuperuser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to disable a superuser");
        }
    }

    private UserRepresentation getUserRepresentation(String userId) {
        String authUserId = getAuthUserId(userId);
        return keycloakService.getUserRepresentationById(authUserId);
    }

    private String getAuthUserId(String userId) {
        UserEntity userEntity = getUserEntity(userId);
        return userEntity.getAuthId() == null ? userEntity.getId().toString() : userEntity.getAuthId();
    }

    private UserEntity getUserEntity(UUID userId) {
        return getUserEntity(userId.toString());
    }

    private UserEntity getUserEntity(String userId) {
        UserEntity userEntity = userRepository.getByIdOrAuthId(userId);
        if (userEntity == null) {
            throw new NotFoundException(String.format("User %s not found in the DB", userId));
        }
        return userEntity;
    }
}
