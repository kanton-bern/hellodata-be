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
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
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
import ch.bedag.dap.hellodata.portal.base.auth.HellodataAuthenticationConverter;
import ch.bedag.dap.hellodata.portal.dashboard_comment.service.DashboardCommentPermissionService;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.email.service.EmailNotificationService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.UserAlreadyExistsException;
import ch.bedag.dap.hellodata.portal.user.data.*;
import ch.bedag.dap.hellodata.portal.user.event.UserContextRoleSyncEvent;
import ch.bedag.dap.hellodata.portal.user.event.UserFullSyncEvent;
import ch.bedag.dap.hellodata.portal.user.util.UserDtoMapper;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final UserLookupProviderManager userLookupProviderManager;
    private final HelloDataContextConfig helloDataContextConfig;
    private final DashboardCommentPermissionService dashboardCommentPermissionService;
    private final UserSelectedDashboardService userSelectedDashboardService;

    private final DashboardGroupService dashboardGroupService;
    private final UserDashboardSyncService userDashboardSyncService;
    private final ApplicationEventPublisher eventPublisher;
    private final HellodataAuthenticationConverter authenticationConverter;
    /**
     * A flag to indicate if the user should be deleted in the provider when deleting it in the portal
     */
    @Value("${hello-data.auth-server.delete-user-in-provider:false}")
    private boolean deleteUsersInProvider;

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
    public List<UserDto> getAllUsers() {
        List<UserEntity> allPortalUsers = userRepository.findAll();
        return allPortalUsers.stream()
                .map(UserDtoMapper::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserWithBusinessRoleDto> getAllUsersWithBusinessDomainRole() {
        List<UserEntity> allPortalUsers = userRepository.findAll();
        return allPortalUsers.stream()
                .map(this::mapWithBusinessDomainRole)
                .toList();
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
        Optional<UserEntity> userEntityResult = Optional.of(getUserEntity(dbId));
        AtomicBoolean isUserFederated = new AtomicBoolean(false);
        userEntityResult.ifPresentOrElse(
                userEntity -> {
                    isUserFederated.set(userEntity.isFederated());
                    userRepository.delete(userEntity);
                },
                () -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with specified id not found");//NOSONAR
                }
        );
        deleteKeycloakUser(userEntityResult.get(), isUserFederated.get());
        SubsystemUserDelete subsystemUserDelete = new SubsystemUserDelete();
        String email = userEntityResult.get().getEmail().toLowerCase(Locale.ROOT);
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
        if (!userEntity.isFederated()) {
            disableKeycloakUser(userId);
        }
        SubsystemUserUpdate subsystemUserUpdate = getSubsystemUserUpdate(userEntity.getEmail(), userEntity.getUsername(), userEntity.getFirstName(), userEntity.getLastName());
        subsystemUserUpdate.setActive(false);
        natsSenderService.publishMessageToJetStream(HDEvent.DISABLE_USER, subsystemUserUpdate);
        emailNotificationService.notifyAboutUserDeactivation(userEntity.getFirstName(), userEntity.getEmail(), getSelectedLanguageByEmail(userEntity.getEmail()));
        return UserDtoMapper.map(userEntity);
    }

    @Transactional
    public UserDto enableUserById(String userId) {
        UUID dbId = UUID.fromString(userId);
        validateNotAllowedIfCurrentUserIsNotSuperuser(dbId);
        UserEntity userEntity = getUserEntity(userId);
        userEntity.setEnabled(true);
        userRepository.saveAndFlush(userEntity);
        if (!userEntity.isFederated()) {
            enableKeycloakUser(userId);
        }
        SubsystemUserUpdate subsystemUserUpdate = getSubsystemUserUpdate(userEntity.getEmail(), userEntity.getUsername(), userEntity.getFirstName(), userEntity.getLastName());
        subsystemUserUpdate.setActive(true);
        natsSenderService.publishMessageToJetStream(HDEvent.ENABLE_USER, subsystemUserUpdate);
        eventPublisher.publishEvent(new UserContextRoleSyncEvent(dbId, true, new HashMap<>()));
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
        ContextsDto contextsDto = new ContextsDto();
        List<HdContextEntity> all = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN, HdContextType.BUSINESS_DOMAIN));
        List<ContextDto> contextDtos = all.stream().map(hdContextEntity -> modelMapper.map(hdContextEntity, ContextDto.class)).toList();
        contextsDto.setContexts(contextDtos);
        return contextsDto;
    }

    @Transactional
    public void updateContextRolesForUser(UUID userId, UpdateContextRolesForUserDto updateContextRolesForUserDto, boolean sendBackUserList) {
        boolean isCurrentUserHDAdmin = SecurityUtils.isSuperuser();
        if (!isCurrentUserHDAdmin && HdRoleName.HELLODATA_ADMIN.name().equalsIgnoreCase(updateContextRolesForUserDto.getBusinessDomainRole().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a HelloData Admin can assign the HelloData Admin role to another user");
        }
        UserEntity userEntity = getUserEntity(userId);
        updateContextRoles(userId, updateContextRolesForUserDto);

        // Persist direct dashboard selections before syncing with Superset
        persistDirectDashboardSelections(userId, updateContextRolesForUserDto.getSelectedDashboardsForUser());

        // Update dashboard group memberships
        dashboardGroupService.updateDashboardGroupMemberships(userId, updateContextRolesForUserDto.getSelectedDashboardGroupIdsForUser());

        // Merge direct + group dashboards
        Map<String, List<DashboardForUserDto>> mergedDashboards = userDashboardSyncService.mergeDashboardSelectionsWithGroups(userId, updateContextRolesForUserDto.getSelectedDashboardsForUser());

        if (updateContextRolesForUserDto.getCommentPermissions() != null) {
            dashboardCommentPermissionService.updatePermissions(userId, updateContextRolesForUserDto.getCommentPermissions());
        }
        // Single unified sync: context roles + dashboards in one JetStream message (via event)
        eventPublisher.publishEvent(new UserFullSyncEvent(userId, sendBackUserList,
                updateContextRolesForUserDto.getContextToModuleRoleNamesMap(), mergedDashboards));

        // Invalidate user cache so permissions are refreshed immediately
        authenticationConverter.invalidateUserCache(userEntity.getEmail());

        notifyUserViaEmail(userId, updateContextRolesForUserDto);
    }

    @Transactional(readOnly = true)
    public List<UserContextRoleDto> getContextRolesForUser(UUID userId) {
        List<UserContextRoleDto> result = new ArrayList<>();
        UserEntity userEntity = getUserEntity(userId);
        Set<UserContextRoleEntity> contextRoles = userEntity.getContextRoles();
        for (UserContextRoleEntity userContextRoleEntity : contextRoles) {
            UserContextRoleDto dto = new UserContextRoleDto();
            Optional<HdContextEntity> byContextKey = contextRepository.getByContextKey(userContextRoleEntity.getContextKey());
            byContextKey.ifPresent(context -> dto.setContext(modelMapper.map(context, ContextDto.class)));
            dto.setRole(modelMapper.map(userContextRoleEntity.getRole(), RoleDto.class));
            result.add(dto);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public boolean isUserSuperuser(UUID userId) {
        UserEntity userEntity = getUserEntity(userId);
        return BooleanUtils.isTrue(userEntity.isSuperuser());
    }

    @Transactional(readOnly = true)
    public Set<String> getUserPortalPermissions(UUID userId) {
        UserEntity userEntity = getUserEntity(userId);
        if (BooleanUtils.isTrue(userEntity.isSuperuser())) {
            return SecurityUtils.getCurrentUserPermissions();
        } else {
            List<String> portalPermissions = userEntity.getPermissionsFromAllRoles();
            if (portalPermissions == null) {
                return new HashSet<>();
            }
            return new HashSet<>(portalPermissions);
        }
    }

    @Transactional(readOnly = true)
    public Set<UserContextRoleEntity> getCurrentUserDataDomainRolesWithoutNone() {
        return getCurrentUserDataDomainRolesExceptNone();
    }

    @Transactional(readOnly = true)
    public void validateUserHasAccessToContext(String contextKey, String reason) {
        if (contextKey == null) {
            return;
        }
        List<String> contextKeys = getCurrentUserDataDomainRolesExceptNone().stream().map(UserContextRoleEntity::getContextKey).toList();
        if (!contextKeys.contains(contextKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
        }
    }

    @Transactional(readOnly = true)
    public List<AdUserDto> searchUserOmitCreated(String email) {
        List<AdUserDto> users = searchUserInternal(email);
        Map<String, AdUserDto> emailToUserDto = users.stream().collect(Collectors.toMap(AdUserDto::getEmail, user -> user, (existing, replacement) -> {
            if (existing.getOrigin() == AdUserOrigin.LOCAL && replacement.getOrigin() != AdUserOrigin.LOCAL) {
                return replacement;
            }
            return existing;
        }));

        List<String> usersAlreadyAdded = userRepository.findAllEmails().stream().map(eMail -> eMail.toLowerCase(Locale.ROOT)).toList();
        Set<String> uniqueEmails = new HashSet<>();
        List<AdUserDto> uniqueUsers = new ArrayList<>();
        for (Map.Entry<String, AdUserDto> entry : emailToUserDto.entrySet()) {
            String emailKey = entry.getKey().toLowerCase(Locale.ROOT);
            AdUserDto user = entry.getValue();
            if (uniqueEmails.add(emailKey) && !usersAlreadyAdded.contains(emailKey) && isValidEmail(user.getEmail())) {
                uniqueUsers.add(user);
            }
        }
        return uniqueUsers;
    }

    @Transactional(readOnly = true)
    public List<AdUserDto> searchUser(String email) {
        return searchUserInternal(email);
    }

    @Transactional(readOnly = true)
    public List<DataDomainDto> getAvailableDataDomains() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        UserEntity userEntity = getUserEntity(userId);
        return extractDomainsFromContextRoles(userEntity.getContextRoles());
    }

    @Transactional(readOnly = true)
    public List<UserEntity> findHelloDataAdminUsers() {
        return userRepository.findUsersByHdRoleName(HdRoleName.BUSINESS_DOMAIN_ADMIN).stream().filter(UserEntity::isEnabled).toList();
    }

    @Transactional
    public void setSelectedLanguage(String userId, Locale lang) {
        UserEntity userEntity = getUserEntity(userId);
        if (!userEntity.getId().equals(SecurityUtils.getCurrentUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        userEntity.setSelectedLanguage(lang);
        userRepository.save(userEntity);
    }

    @Transactional(readOnly = true)
    public Locale getSelectedLanguage(String userId) {
        UserEntity userEntity = getUserEntity(userId);
        return userEntity.getSelectedLanguage();
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

    private void deleteKeycloakUser(UserEntity userEntity, boolean isUserFederated) {
        UserResource userResource = getUserResource(userEntity);
        if (userResource != null && deleteUsersInProvider && !isUserFederated) {
            userResource.remove();
            log.info("User {} removed from provider", userEntity.getEmail());
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

    private void disableKeycloakUser(String userId) {
        String authUserId = getAuthUserId(userId);
        try {
            UserResource userResource = keycloakService.getUserResourceById(authUserId);
            UserRepresentation representation = userResource.toRepresentation();
            representation.setEnabled(false);
            userResource.update(representation);
            userResource.logout();
            log.debug("User {} disabled and logged out from keycloak", userId);
        } catch (NotFoundException nfe) {
            log.warn("User {} not found in keycloak, skipping keycloak-deactivation.", userId);
        }
    }

    private void enableKeycloakUser(String userId) {
        String authUserId = getAuthUserId(userId);
        try {
            UserResource userResource = keycloakService.getUserResourceById(authUserId);
            UserRepresentation representation = userResource.toRepresentation();
            representation.setEnabled(true);
            userResource.update(representation);
            log.debug("User {} enabled in keycloak", userId);
        } catch (NotFoundException nfe) {
            log.warn("User {} not found in keycloak, skipping keycloak-activation.", userId);
        }
    }

    private Set<UserContextRoleEntity> getCurrentUserDataDomainRolesExceptNone() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            String errMsg = "Current user not found";
            log.error(errMsg);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, errMsg);
        }
        Optional<UserEntity> userEntity = Optional.of(getUserEntity(currentUserId));
        return userEntity.map(user -> user.getContextRoles()
                .stream()
                .filter(userContextRoleEntity -> HdContextType.DATA_DOMAIN.equals(userContextRoleEntity.getRole().getContextType()))
                .filter(userContextRoleEntity -> !HdRoleName.NONE.equals(userContextRoleEntity.getRole().getName()))
                .collect(Collectors.toSet())).orElse(Collections.emptySet());
    }

    private List<AdUserDto> searchUserInternal(String email) {
        if (email == null || email.length() < 3) {
            return Collections.emptyList();
        }
        return userLookupProviderManager.searchUserByEmail(email);
    }

    private Locale getSelectedLanguageByEmail(String email) {
        return userRepository.findSelectedLanguageByEmail(email);
    }

    private boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
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

    private void updateContextRoles(UUID userId, UpdateContextRolesForUserDto updateContextRolesForUserDto) {
        UserEntity userEntity = getUserEntity(userId);
        if (updateContextRolesForUserDto.getBusinessDomainRole() != null) {
            roleService.updateBusinessRoleForUser(userEntity, updateContextRolesForUserDto.getBusinessDomainRole());
        } else {
            roleService.setBusinessDomainRoleForUser(userEntity, HdRoleName.NONE);
        }

        if (!updateContextRolesForUserDto.getBusinessDomainRole().getName().equalsIgnoreCase(HdRoleName.NONE.name())) {
            roleService.setAllDataDomainRolesForUser(userEntity, HdRoleName.DATA_DOMAIN_ADMIN);
            // User gets DATA_DOMAIN_ADMIN in all domains - remove from all dashboard groups
            removeUserFromDashboardGroupsForAllDomains(userId);
        } else if (!CollectionUtils.isEmpty(updateContextRolesForUserDto.getDataDomainRoles())) {
            for (UserContextRoleDto dataDomainRoleForContextDto : updateContextRolesForUserDto.getDataDomainRoles()) {
                roleService.updateDomainRoleForUser(userEntity, dataDomainRoleForContextDto.getRole(), dataDomainRoleForContextDto.getContext().getContextKey());
                // Check if role is not eligible for dashboard groups - remove user from groups in this domain
                removeUserFromDashboardGroupsIfNotEligible(userId, dataDomainRoleForContextDto);
            }
            setRoleForAllRemainingDataDomainsToNone(updateContextRolesForUserDto, userEntity);
        }
        userEntity.setSuperuser(updateContextRolesForUserDto.getBusinessDomainRole().getName().equalsIgnoreCase(HdRoleName.HELLODATA_ADMIN.name()));
        userRepository.save(userEntity);
    }

    private void removeUserFromDashboardGroupsIfNotEligible(UUID userId, UserContextRoleDto dataDomainRoleForContextDto) {
        String roleName = dataDomainRoleForContextDto.getRole().getName();
        boolean isEligibleRole = HdRoleName.DATA_DOMAIN_VIEWER.name().equalsIgnoreCase(roleName) ||
                HdRoleName.DATA_DOMAIN_BUSINESS_SPECIALIST.name().equalsIgnoreCase(roleName);
        if (!isEligibleRole) {
            String contextKey = dataDomainRoleForContextDto.getContext().getContextKey();
            dashboardGroupService.removeUserFromDashboardGroupsInDomain(userId.toString(), contextKey);
            userSelectedDashboardService.removeAllForUserInContext(userId, contextKey);
        }
    }

    private void removeUserFromDashboardGroupsForAllDomains(UUID userId) {
        List<HdContextEntity> allDataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        for (HdContextEntity dataDomain : allDataDomains) {
            dashboardGroupService.removeUserFromDashboardGroupsInDomain(userId.toString(), dataDomain.getContextKey());
        }
        userSelectedDashboardService.removeAllForUser(userId);
    }

    private void setRoleForAllRemainingDataDomainsToNone(UpdateContextRolesForUserDto updateContextRolesForUserDto, UserEntity userEntity) {
        List<HdContextEntity> allDataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        List<HdContextEntity> ddDomainsWithoutRoleForUser = allDataDomains.stream()
                .filter(availableDD -> updateContextRolesForUserDto.getDataDomainRoles()
                        .stream()
                        .noneMatch(ddRole -> ddRole.getContext()
                                .getContextKey()
                                .equalsIgnoreCase(
                                        availableDD.getContextKey())))
                .toList();
        if (!ddDomainsWithoutRoleForUser.isEmpty()) {
            Optional<RoleDto> first = roleService.getAll().stream()
                    .filter(roleDto -> HdRoleName.NONE.name().equalsIgnoreCase(roleDto.getName())).findFirst();
            if (first.isPresent()) {
                RoleDto noneRole = first.get();
                for (HdContextEntity dataDomain : ddDomainsWithoutRoleForUser) {
                    roleService.updateDomainRoleForUser(userEntity, noneRole, dataDomain.getContextKey());
                }
            }
        }
    }

    private void notifyUserViaEmail(UUID userId, UpdateContextRolesForUserDto updateContextRolesForUserDto) {
        UserEntity userEntity = getUserEntity(userId);
        List<UserContextRoleDto> adminContextRoles = getAdminContextRoles(userEntity);
        if (!userEntity.isCreationEmailSent()) {
            emailNotificationService.notifyAboutUserCreation(userEntity.getFirstName(), userEntity.getEmail(), updateContextRolesForUserDto, adminContextRoles, userEntity.getSelectedLanguage());
            userEntity.setCreationEmailSent(true);
            userRepository.save(userEntity);
        } else {
            emailNotificationService.notifyAboutUserRoleChanged(userEntity.getFirstName(), userEntity.getEmail(), updateContextRolesForUserDto, adminContextRoles, userEntity.getSelectedLanguage());
        }
    }

    private List<UserContextRoleDto> getAdminContextRoles(UserEntity userEntity) {
        return userEntity.getContextRoles().stream()
                .filter(contextRole -> contextRole.getRole().getName() == HdRoleName.DATA_DOMAIN_ADMIN).map(adminContextRole -> {
                    Optional<HdContextEntity> contextResult = contextRepository.getByContextKey(adminContextRole.getContextKey());
                    if (contextResult.isPresent()) {
                        HdContextEntity context = contextResult.get();
                        ContextDto contextDto = new ContextDto();
                        contextDto.setContextKey(context.getContextKey());
                        contextDto.setName(context.getName());
                        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
                        userContextRoleDto.setContext(contextDto);
                        RoleDto roleDto = new RoleDto();
                        roleDto.setName(adminContextRole.getRole().getName().name());
                        userContextRoleDto.setRole(roleDto);
                        return userContextRoleDto;
                    }
                    return null;
                }).filter(Objects::nonNull).toList();
    }

    /**
     * Persists direct dashboard selections (not from groups) to the database
     */
    private void persistDirectDashboardSelections(UUID userId, Map<String, List<DashboardForUserDto>> selectedDashboardsForUser) {
        if (selectedDashboardsForUser == null) {
            return;
        }
        for (Map.Entry<String, List<DashboardForUserDto>> entry : selectedDashboardsForUser.entrySet()) {
            String contextKey = entry.getKey();
            List<DashboardForUserDto> dashboards = entry.getValue();
            List<UserSelectedDashboardService.DashboardSelection> selections = dashboards.stream()
                    .filter(DashboardForUserDto::isViewer)
                    .map(d -> new UserSelectedDashboardService.DashboardSelection(d.getId(), d.getTitle(), d.getInstanceName()))
                    .toList();
            userSelectedDashboardService.saveSelectedDashboards(userId, contextKey, selections);
        }
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

    private UserWithBusinessRoleDto mapWithBusinessDomainRole(UserEntity userEntity) {
        UserDto userDto = UserDtoMapper.map(userEntity);
        UserWithBusinessRoleDto userDtoWithBusinessRole;
        if (userDto != null) {
            userDtoWithBusinessRole = modelMapper.map(userDto, UserWithBusinessRoleDto.class);
            Optional<UserContextRoleEntity> businessDomainRole = userEntity.getContextRoles().stream()
                    .filter(userContextRoleEntity -> userContextRoleEntity.getContextKey().equalsIgnoreCase(helloDataContextConfig.getBusinessContext().getKey())).findAny();
            businessDomainRole.ifPresent(userContextRoleEntity -> userDtoWithBusinessRole.setBusinessDomainRole(userContextRoleEntity.getRole().getName()));
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

    private List<DataDomainDto> extractDomainsFromContextRoles(Set<UserContextRoleEntity> contextRoles) {
        List<DataDomainDto> result = new ArrayList<>();
        for (UserContextRoleEntity contextRole : contextRoles) {
            if (HdContextType.DATA_DOMAIN.equals(contextRole.getRole().getContextType()) && !HdRoleName.NONE.equals(contextRole.getRole().getName())) {
                Optional<HdContextEntity> byContextKey = contextRepository.getByContextKey(contextRole.getContextKey());
                byContextKey.ifPresent(contextEntity -> {
                    DataDomainDto dataDomainDto = new DataDomainDto();
                    dataDomainDto.setId(contextEntity.getId());
                    dataDomainDto.setKey(contextEntity.getContextKey());
                    dataDomainDto.setName(contextEntity.getName());
                    result.add(dataDomainDto);
                });
            }
        }
        return result;
    }

    private UserResource getUserResource(@NotNull UserEntity userEntity) {
        String authUserId = userEntity.getAuthId() == null ? userEntity.getId().toString() : userEntity.getAuthId();
        return keycloakService.getUserResourceById(authUserId);
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
