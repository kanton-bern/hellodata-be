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

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repositories.HdContextRepository;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.security.Permission;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.*;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.SupersetDashboardsForUserUpdate;
import ch.bedag.dap.hellodata.portal.email.service.EmailNotificationService;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.data.*;
import ch.bedag.dap.hellodata.portalcommon.role.entity.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import jakarta.persistence.EntityExistsException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ch.bedag.dap.hellodata.commons.SlugifyUtil.DASHBOARD_ROLE_PREFIX;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {

    private static final List<String> REQUIRED_ACTIONS = List.of("VERIFY_EMAIL", "UPDATE_PROFILE", "UPDATE_PASSWORD");
    private final KeycloakService keycloakService;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final MetaInfoResourceService metaInfoResourceService;
    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final NatsSenderService natsSenderService;
    private final HdContextRepository contextRepository;
    private final RoleService roleService;
    private final EmailNotificationService emailNotificationService;
    private final UserLookupProviderManager userLookupProviderManager;

    @Transactional
    public String createUser(String email, String firstName, String lastName) {
        email = email.toLowerCase(Locale.ROOT);
        log.info("Creating user. Email: {}, first name: {}, last name {}", email, firstName, lastName);
        validateEmailAlreadyExists(email);
        String keycloakUserId;
        UserRepresentation userFoundInKeycloak = keycloakService.getUserRepresentationByEmail(email);
        if (userFoundInKeycloak == null) {
            log.info("User {} doesn't not exist in the keycloak, creating", email);
            UserRepresentation user = new UserRepresentation();
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setRequiredActions(REQUIRED_ACTIONS);
            keycloakUserId = keycloakService.createUser(user);
        } else {
            log.info("User {} already exists in the keycloak, creating only in portal", userFoundInKeycloak.getId());
            keycloakUserId = userFoundInKeycloak.getId();
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setId(UUID.fromString(keycloakUserId));
        userEntity.setEmail(email);
        userEntity.setUsername(userFoundInKeycloak == null ? email : userFoundInKeycloak.getUsername());
        userEntity.setFirstName(userFoundInKeycloak == null ? firstName : userFoundInKeycloak.getFirstName());
        userEntity.setLastName(userFoundInKeycloak == null ? lastName : userFoundInKeycloak.getLastName());
        userEntity.setEnabled(true);
        userEntity.setSuperuser(false);
        userRepository.saveAndFlush(userEntity);
        roleService.setBusinessDomainRoleForUser(userEntity, HdRoleName.NONE);
        roleService.setAllDataDomainRolesForUser(userEntity, HdRoleName.NONE);
        createUserInSubsystems(keycloakUserId);
        return keycloakUserId;
    }

    @Transactional(readOnly = true)
    public void validateEmailAlreadyExists(String email) {
        Optional<UserEntity> userEntityByEmail = userRepository.findUserEntityByEmailIgnoreCase(email);
        if (userEntityByEmail.isPresent()) {
            throw new EntityExistsException("@User email already exists");
        }
    }

    @Transactional(readOnly = true)
    public boolean isUserDisabled(String userId) {
        UserEntity userEntity = getUserEntity(userId);
        return !userEntity.isEnabled();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncAllUsers() {
        List<UserDto> allUsers = userRepository.getUserEntitiesByEnabled(true).stream().map(this::map).toList();
        log.info("[syncAllUsers] Found {} users to sync with surrounding systems.", allUsers.size());
        AtomicInteger counter = new AtomicInteger();
        List<UserContextRoleUpdate> list = allUsers.stream().map(user -> {
            UserEntity userEntity;
            UUID id = UUID.fromString(user.getId());
            if (!userRepository.existsByIdOrAuthId(id, id.toString())) {
                userEntity = new UserEntity();
                userEntity.setId(id);
                userEntity.setEmail(user.getEmail());
                userRepository.saveAndFlush(userEntity);
                roleService.createNoneContextRoles(userEntity);
            } else {
                userEntity = getUserEntity(id);
                if (CollectionUtils.isEmpty(userEntity.getContextRoles())) {
                    roleService.createNoneContextRoles(userEntity);
                }
            }
            boolean isLast = counter.incrementAndGet() == allUsers.size();
            return getUserContextRoleUpdate(userEntity, isLast);
        }).collect(Collectors.toList());
        List<List<UserContextRoleUpdate>> partition = partitionToBatches(list);

        // Proceed with publishing the partitions
        for (List<UserContextRoleUpdate> batch : partition) {
            UsersContextRoleUpdate usersContextRoleUpdate = new UsersContextRoleUpdate();
            usersContextRoleUpdate.setUserContextRoleUpdates(batch);
            natsSenderService.publishMessageToJetStream(HDEvent.SYNC_USERS, usersContextRoleUpdate);
            log.info("[syncUsers] Synchronized batch of {} users.", batch.size());
        }
        log.info("[syncAllUsers] Synchronized {} out of {} users with subsystems.", counter.get(), allUsers.size());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        List<UserEntity> allPortalUsers = userRepository.findAll();
        return allPortalUsers.stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsersPageable(Pageable pageable, String search) {
        Page<UserEntity> allPortalUsers;
        if (search == null || search.isEmpty()) {
            allPortalUsers = userRepository.findAll(pageable);
        } else {
            allPortalUsers = userRepository.findAll(pageable, search);
        }
        return allPortalUsers.map(this::map);
    }

    @Transactional
    public void deleteUserById(String userId) {
        UUID dbId = UUID.fromString(userId);
        validateNotAllowedIfCurrentUserIsNotSuperuser(dbId);
        if (SecurityUtils.getCurrentUserId() == null || userId.equals(SecurityUtils.getCurrentUserId().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete yourself");//NOSONAR
        }
        UserResource userResource = getUserResource(userId);
        Optional<UserEntity> userEntityResult = Optional.of(getUserEntity(dbId));
        userEntityResult.ifPresentOrElse(userRepository::delete, () -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with specified id not found");//NOSONAR
        });
        if (userResource == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with specified id not found in keycloak");//NOSONAR
        }
        SubsystemUserDelete subsystemUserDelete = new SubsystemUserDelete();
        UserRepresentation userRepresentation = userResource.toRepresentation();
        subsystemUserDelete.setEmail(userRepresentation.getEmail().toLowerCase(Locale.ROOT));
        subsystemUserDelete.setUsername(userRepresentation.getUsername());
        userResource.remove();
        natsSenderService.publishMessageToJetStream(HDEvent.DELETE_USER, subsystemUserDelete);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(String userId) {
        UserEntity userEntity = getUserEntity(userId);
        return map(userEntity);
    }

    @Transactional
    public void updateLastAccess(String userId) {
        UserEntity userEntity = getUserEntity(userId);
        userEntity.setLastAccess(LocalDateTime.now());
        userRepository.save(userEntity);
    }

    @Transactional(readOnly = true)
    public void createUserInSubsystems(String userId) {
        SubsystemUserUpdate createUser = getSubsystemUserUpdate(userId);
        createUser.setSendBackUsersList(false);
        natsSenderService.publishMessageToJetStream(HDEvent.CREATE_USER, createUser);
    }

    @Transactional
    public UserDto disableUserById(String userId) {
        UUID dbId = UUID.fromString(userId);
        validateNotAllowedIfCurrentUserIsNotSuperuser(dbId);
        UserEntity userEntity = getUserEntity(userId);
        userEntity.setEnabled(false);
        userRepository.save(userEntity);
        String authUserId = getAuthUserId(userId);
        UserResource userResource = keycloakService.getUserResourceById(authUserId);
        UserRepresentation representation = userResource.toRepresentation();
        representation.setEnabled(false);
        userResource.update(representation);
        userResource.logout();
        SubsystemUserUpdate subsystemUserUpdate = getSubsystemUserUpdate(representation);
        subsystemUserUpdate.setActive(false);
        natsSenderService.publishMessageToJetStream(HDEvent.DISABLE_USER, subsystemUserUpdate);
        emailNotificationService.notifyAboutUserDeactivation(representation.getFirstName(), representation.getEmail(), getSelectedLanguageByEmail(representation.getEmail()));
        return map(userEntity);
    }

    @Transactional
    public UserDto enableUserById(String userId) {
        UUID dbId = UUID.fromString(userId);
        validateNotAllowedIfCurrentUserIsNotSuperuser(dbId);
        String authUserId = getAuthUserId(userId);
        UserResource userResource = keycloakService.getUserResourceById(authUserId);
        UserRepresentation representation = userResource.toRepresentation();
        representation.setEnabled(true);
        userResource.update(representation);
        SubsystemUserUpdate subsystemUserUpdate = getSubsystemUserUpdate(representation);
        subsystemUserUpdate.setActive(true);
        natsSenderService.publishMessageToJetStream(HDEvent.ENABLE_USER, subsystemUserUpdate);
        UserEntity userEntity = getUserEntity(userId);
        userEntity.setEnabled(true);
        userRepository.saveAndFlush(userEntity);
        synchronizeContextRolesWithSubsystems(userEntity);
        emailNotificationService.notifyAboutUserActivation(representation.getFirstName(), representation.getEmail(), userEntity.getSelectedLanguage());
        return map(userEntity);
    }

    @Transactional(readOnly = true)
    public DashboardsDto getDashboardsMarkUser(String userId) {
        DashboardsDto result = new DashboardsDto();
        result.setDashboards(new ArrayList<>());
        List<MetaInfoResourceEntity> dashboardsWithContext = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS);
        UserEntity userEntity = getUserEntity(userId);
        for (MetaInfoResourceEntity dashboardWithContext : dashboardsWithContext) {
            if (dashboardWithContext.getMetainfo() instanceof DashboardResource dashboardResource) {
                SubsystemUser subsystemUser = metaInfoResourceService.findUserInInstance(userEntity.getEmail(), dashboardResource.getInstanceName());
                if (subsystemUser == null) {
                    log.warn("User {} not found in instance {}", userEntity.getEmail(), dashboardResource.getInstanceName());
                }
                List<SupersetDashboard> data = dashboardResource.getData().stream().filter(SupersetDashboard::isPublished).toList();
                for (SupersetDashboard supersetDashboard : data) {
                    result.getDashboards().add(createDashboardDto(dashboardResource, subsystemUser, supersetDashboard, dashboardWithContext.getContextKey()));
                }
            }
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
    public void updateContextRolesForUser(UUID userId, UpdateContextRolesForUserDto updateContextRolesForUserDto) {
        updateContextRoles(userId, updateContextRolesForUserDto);
        synchronizeDashboardsForUser(userId, updateContextRolesForUserDto.getSelectedDashboardsForUser());
        UserEntity userEntity = getUserEntity(userId);
        synchronizeContextRolesWithSubsystems(userEntity);
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

    @Transactional
    public void synchronizeContextRolesWithSubsystems(UserEntity userEntity, boolean sendBackUsersList) {
        UserContextRoleUpdate userContextRoleUpdate = getUserContextRoleUpdate(userEntity, sendBackUsersList);
        natsSenderService.publishMessageToJetStream(HDEvent.UPDATE_USER_CONTEXT_ROLE, userContextRoleUpdate);
    }

    @Transactional
    public void synchronizeContextRolesWithSubsystems(UserEntity userEntity) {
        synchronizeContextRolesWithSubsystems(userEntity, true);
    }

    @Transactional(readOnly = true)
    public Set<String> getUserPortalPermissions(UUID userId) {
        UserEntity userEntity = getUserEntity(userId);
        if (BooleanUtils.isTrue(userEntity.getSuperuser())) {
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

    @Transactional(readOnly = true)
    public void validateUserHasAccessToContext(String contextKey, String reason) {
        if (contextKey == null) {
            return;
        }
        List<String> contextKeys = getCurrentUserDataDomainRolesWithoutNone().stream().map(UserContextRoleEntity::getContextKey).toList();
        if (!contextKeys.contains(contextKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
        }
    }

    public List<AdUserDto> searchUser(String email) {
        if (email == null || email.length() < 3) {
            return Collections.emptyList();
        }

        List<AdUserDto> users = userLookupProviderManager.searchUserByEmail(email);
        Set<String> uniqueEmails = new HashSet<>();

        return users.stream()
                .filter(Objects::nonNull)
                .filter(user -> uniqueEmails.add(user.getEmail()))
                .collect(Collectors.toList());
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
        return userRepository.findUsersByHdRoleName(HdRoleName.BUSINESS_DOMAIN_ADMIN).stream().filter(userEntity -> userEntity.isEnabled()).toList();
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

    @Transactional(readOnly = true)
    public Locale getSelectedLanguageByEmail(String email) {
        return userRepository.findSelectedLanguageByEmail(email);
    }

    /**
     * partition list to batches and leave the last one as the biggest one, because it will trigger users list pushback
     */
    private List<List<UserContextRoleUpdate>> partitionToBatches(List<UserContextRoleUpdate> list) {
        List<List<UserContextRoleUpdate>> partition = new ArrayList<>(ListUtils.partition(list, 25));

        // Check if we have at least three partitions
        if (partition.size() >= 3) {
            // Remove the last three partitions
            List<UserContextRoleUpdate> lastPartition = partition.remove(partition.size() - 1);
            List<UserContextRoleUpdate> secondLastPartition = partition.remove(partition.size() - 1);
            List<UserContextRoleUpdate> thirdLastPartition = partition.remove(partition.size() - 1);

            // Merge the last three partitions into one
            List<UserContextRoleUpdate> mergedPartition = new ArrayList<>();
            mergedPartition.addAll(thirdLastPartition);
            mergedPartition.addAll(secondLastPartition);
            mergedPartition.addAll(lastPartition);

            // Add the merged partition back to the list
            partition.add(mergedPartition);
        }
        return partition;
    }

    private UserContextRoleUpdate getUserContextRoleUpdate(UserEntity userEntity, boolean sendBackUsersList) {
        UserContextRoleUpdate userContextRoleUpdate = new UserContextRoleUpdate();
        userContextRoleUpdate.setEmail(userEntity.getEmail());
        userContextRoleUpdate.setUsername(userEntity.getUsername());
        userContextRoleUpdate.setFirstName(userEntity.getFirstName());
        userContextRoleUpdate.setLastName(userEntity.getLastName());
        userContextRoleUpdate.setActive(userEntity.isEnabled());
        List<UserContextRoleEntity> allContextRolesForUser = roleService.getAllContextRolesForUser(userEntity);
        List<UserContextRoleUpdate.ContextRole> contextRoles = new ArrayList<>();
        allContextRolesForUser.forEach(contextRoleForUser -> {
            UserContextRoleUpdate.ContextRole contextRole = new UserContextRoleUpdate.ContextRole();
            contextRole.setContextKey(contextRoleForUser.getContextKey());
            contextRole.setRoleName(contextRoleForUser.getRole().getName());
            Optional<HdContextEntity> byContextKey = contextRepository.getByContextKey(contextRoleForUser.getContextKey());
            byContextKey.ifPresent(contextEntity -> contextRole.setParentContextKey(contextRole.getParentContextKey()));
            contextRoles.add(contextRole);
        });
        userContextRoleUpdate.setContextRoles(contextRoles);
        userContextRoleUpdate.setSendBackUsersList(sendBackUsersList);
        return userContextRoleUpdate;
    }

    private SubsystemUserUpdate getSubsystemUserUpdate(UserRepresentation representation) {
        SubsystemUserUpdate createUser = new SubsystemUserUpdate();
        createUser.setFirstName(representation.getFirstName());
        createUser.setLastName(representation.getLastName());
        createUser.setUsername(representation.getUsername().toLowerCase(Locale.ROOT));
        createUser.setEmail(representation.getEmail().toLowerCase(Locale.ROOT));
        createUser.setActive(representation.isEnabled());
        return createUser;
    }

    private SubsystemUserUpdate getSubsystemUserUpdate(String userId) {
        UserRepresentation representation = getUserRepresentation(userId);
        return getSubsystemUserUpdate(representation);
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
        } else if (!CollectionUtils.isEmpty(updateContextRolesForUserDto.getDataDomainRoles())) {
            for (UserContextRoleDto dataDomainRoleForContextDto : updateContextRolesForUserDto.getDataDomainRoles()) {
                roleService.updateDomainRoleForUser(userEntity, dataDomainRoleForContextDto.getRole(), dataDomainRoleForContextDto.getContext().getContextKey());
            }
            setRoleForAllRemainingDataDomainsToNone(updateContextRolesForUserDto, userEntity);
        }
        userEntity.setSuperuser(updateContextRolesForUserDto.getBusinessDomainRole().getName().equalsIgnoreCase(HdRoleName.HELLODATA_ADMIN.name()));
        userRepository.save(userEntity);
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
            Optional<RoleDto> first = roleService.getAll().stream().filter(roleDto -> HdRoleName.NONE.name().equalsIgnoreCase(roleDto.getName())).findFirst();
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
        UserRepresentation representation = getUserRepresentation(userId.toString());
        List<UserContextRoleDto> adminContextRoles = getAdminContextRoles(userEntity);
        if (!userEntity.isCreationEmailSent()) {
            emailNotificationService.notifyAboutUserCreation(representation.getFirstName(), representation.getEmail(), updateContextRolesForUserDto, adminContextRoles, userEntity.getSelectedLanguage());
            userEntity.setCreationEmailSent(true);
            userRepository.save(userEntity);
        } else {
            emailNotificationService.notifyAboutUserRoleChanged(representation.getFirstName(), representation.getEmail(), updateContextRolesForUserDto, adminContextRoles, userEntity.getSelectedLanguage());
        }
    }

    private List<UserContextRoleDto> getAdminContextRoles(UserEntity userEntity) {
        return userEntity.getContextRoles().stream().filter(contextRole -> contextRole.getRole().getName() == HdRoleName.DATA_DOMAIN_ADMIN).map(adminContextRole -> {
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

    private void synchronizeDashboardsForUser(UUID userId, Map<String, List<DashboardForUserDto>> selectedDashboardsForUser) {
        for (Map.Entry<String, List<DashboardForUserDto>> entry : selectedDashboardsForUser.entrySet()) {
            String contextKey = entry.getKey();
            String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextKey);
            updateDashboardRoleForUser(userId, entry.getValue(), supersetInstanceName);
        }
    }

    private void updateDashboardRoleForUser(UUID userId, List<DashboardForUserDto> dashboardForUserDtoList, String supersetInstanceName) {
        try {
            SupersetDashboardsForUserUpdate supersetDashboardsForUserUpdate = new SupersetDashboardsForUserUpdate();
            UserEntity userEntity = getUserEntity(userId);
            supersetDashboardsForUserUpdate.setSupersetUserName(userEntity.getUsername());
            supersetDashboardsForUserUpdate.setSupersetUserEmail(userEntity.getEmail());
            supersetDashboardsForUserUpdate.setDashboards(dashboardForUserDtoList);
            supersetDashboardsForUserUpdate.setSupersetFirstName(userEntity.getFirstName());
            supersetDashboardsForUserUpdate.setSupersetLastName(userEntity.getLastName());
            supersetDashboardsForUserUpdate.setActive(userEntity.isEnabled());
            String subject = SlugifyUtil.slugify(supersetInstanceName + RequestReplySubject.UPDATE_DASHBOARD_ROLES_FOR_USER.getSubject());
            log.info("[updateDashboardRoleForUser] Sending request to subject: {}", subject);
            Message reply =
                    connection.request(subject, objectMapper.writeValueAsString(supersetDashboardsForUserUpdate).getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(10));
            if (reply == null) {
                log.warn("Reply is null, please verify superset sidecar or nats connection");
            } else {
                reply.ack();
                log.info("[updateDashboardRoleForUser] Response received: " + new String(reply.getData()));
            }
        } catch (Exception e) {
            log.error("Error updating dashboard role for user", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // Re-interrupt the thread
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating user", e);
        }
    }

    private DashboardForUserDto createDashboardDto(DashboardResource dashboardResource, SubsystemUser subsystemUser, SupersetDashboard supersetDashboard, String contextKey) {
        String dashboardTitle = supersetDashboard.getDashboardTitle();
        SubsystemRole SubsystemRole = supersetDashboard.getRoles().stream().filter(role -> role.getName().startsWith(DASHBOARD_ROLE_PREFIX)).findFirst().orElse(null);
        boolean userHasSlugifyDashboardRole = false;
        if (SubsystemRole != null && subsystemUser != null) {
            userHasSlugifyDashboardRole = subsystemUser.getRoles().contains(SubsystemRole);
        }
        DashboardForUserDto dashboardForUserDto = new DashboardForUserDto();
        dashboardForUserDto.setId(supersetDashboard.getId());
        dashboardForUserDto.setTitle(dashboardTitle);
        if (subsystemUser != null) {
            boolean userHasDashboardViewerRole = subsystemUser.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase(SlugifyUtil.BI_VIEWER_ROLE_NAME));
            dashboardForUserDto.setInstanceUserId(subsystemUser.getId());
            dashboardForUserDto.setViewer(userHasSlugifyDashboardRole && userHasDashboardViewerRole);
        }
        dashboardForUserDto.setInstanceName(dashboardResource.getMetadata().instanceName());
        dashboardForUserDto.setChangedOnUtc(supersetDashboard.getChangedOnUtc());
        dashboardForUserDto.setCompositeId(dashboardResource.getMetadata().instanceName() + "_" + supersetDashboard.getId());
        dashboardForUserDto.setContextKey(contextKey);
        return dashboardForUserDto;
    }

    private UserDto map(UserEntity userEntity) {
        UserDto userDto = null;
        if (userEntity != null) {
            userDto = new UserDto();
            userDto.setId(userEntity.getId().toString());
            userDto.setEmail(userEntity.getEmail());
            userDto.setUsername(userEntity.getUsername());
            userDto.setEnabled(userEntity.isEnabled());
            userDto.setSuperuser(userEntity.getSuperuser());
            userDto.setInvitationsCount(userEntity.getInvitationsCount());
            userDto.setFirstName(userEntity.getFirstName());
            userDto.setLastName(userEntity.getLastName());
            if (userEntity.getLastAccess() != null) {
                ZonedDateTime zdt = ZonedDateTime.of(userEntity.getLastAccess(), ZoneId.systemDefault());
                userDto.setLastAccess(zdt.toInstant().toEpochMilli());
            }
            if (BooleanUtils.isTrue(userEntity.getSuperuser())) {
                userDto.setPermissions(Arrays.stream(Permission.values()).map(Enum::name).toList());
            } else {
                List<String> portalPermissions = userEntity.getPermissionsFromAllRoles();
                if (portalPermissions != null) {
                    userDto.setPermissions(portalPermissions);
                }
            }
        }
        return userDto;
    }

    /**
     * Only superuser can enable/disable other superusers
     *
     * @param userId user id
     */
    private void validateNotAllowedIfCurrentUserIsNotSuperuser(UUID userId) {
        UserEntity targetUser = getUserEntity(userId);
        if (!SecurityUtils.isSuperuser() && targetUser.getSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed if userId belongs to a superuser");
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

    private UserResource getUserResource(String userId) {
        String authUserId = getAuthUserId(userId);
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
