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
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.base.auth.HellodataAuthenticationConverter;
import ch.bedag.dap.hellodata.portal.dashboard_comment.service.DashboardCommentPermissionService;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.email.service.EmailNotificationService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.data.*;
import ch.bedag.dap.hellodata.portal.user.event.UserFullSyncEvent;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserContextRoleService {

    private final UserRepository userRepository;
    private final HdContextRepository contextRepository;
    private final RoleService roleService;
    private final EmailNotificationService emailNotificationService;
    private final DashboardCommentPermissionService dashboardCommentPermissionService;
    private final UserSelectedDashboardService userSelectedDashboardService;
    private final DashboardGroupService dashboardGroupService;
    private final ApplicationEventPublisher eventPublisher;
    private final HellodataAuthenticationConverter authenticationConverter;
    private final ModelMapper modelMapper;

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
        updateContextRolesForUserInternal(userId, updateContextRolesForUserDto, sendBackUserList);
    }

    /**
     * Updates context roles for a user from batch import.
     * This method bypasses the security check for HELLODATA_ADMIN role assignment
     * because batch import is a system-level operation that should be able to create admin users.
     */
    @Transactional
    public void updateContextRolesForUserFromBatch(UUID userId, UpdateContextRolesForUserDto updateContextRolesForUserDto, boolean sendBackUserList) {
        updateContextRolesForUserInternal(userId, updateContextRolesForUserDto, sendBackUserList);
    }

    private void updateContextRolesForUserInternal(UUID userId, UpdateContextRolesForUserDto updateContextRolesForUserDto, boolean sendBackUserList) {
        UserEntity userEntity = getUserEntity(userId);
        updateContextRoles(userId, updateContextRolesForUserDto);

        // Persist direct dashboard selections before syncing with Superset
        persistDirectDashboardSelections(userId, updateContextRolesForUserDto.getSelectedDashboardsForUser());

        // Update dashboard group memberships
        dashboardGroupService.updateDashboardGroupMemberships(userId, updateContextRolesForUserDto.getSelectedDashboardGroupIdsForUser());

        if (updateContextRolesForUserDto.getCommentPermissions() != null) {
            dashboardCommentPermissionService.updatePermissions(userId, updateContextRolesForUserDto.getCommentPermissions());
        }
        // Single unified sync: context roles + dashboards in one JetStream message (via event).
        // Dashboards are passed as null so the listener builds them from the persisted state (direct selections + groups)
        // for ALL contexts after commit. The request may only carry the edited context, and the Superset sidecar
        // treats the payload as the complete set - any context missing from it would lose its dashboard roles.
        eventPublisher.publishEvent(new UserFullSyncEvent(userId, sendBackUserList,
                updateContextRolesForUserDto.getContextToModuleRoleNamesMap(), null));

        // Invalidate user cache so permissions are refreshed immediately
        authenticationConverter.invalidateUserCache(userEntity.getEmail());

        try {
            notifyUserViaEmail(userId, updateContextRolesForUserDto);
        } catch (Exception e) {
            log.error("Failed to send role-change notification email for user {}", userId, e);
        }
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
    public List<DataDomainDto> getAvailableDataDomains() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        UserEntity userEntity = getUserEntity(userId);
        return extractDomainsFromContextRoles(userEntity.getContextRoles());
    }

    @Transactional
    public void removeUserFromDashboardGroupsForAllDomains(UUID userId) {
        List<HdContextEntity> allDataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        for (HdContextEntity dataDomain : allDataDomains) {
            dashboardGroupService.removeUserFromDashboardGroupsInDomain(userId.toString(), dataDomain.getContextKey());
        }
        userSelectedDashboardService.removeAllForUser(userId);
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
