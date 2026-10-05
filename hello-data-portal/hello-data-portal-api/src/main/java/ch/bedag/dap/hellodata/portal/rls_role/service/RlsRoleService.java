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
package ch.bedag.dap.hellodata.portal.rls_role.service;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.rls_role.data.RlsRoleDto;
import ch.bedag.dap.hellodata.portal.rls_role.entity.RlsRoleNameEntity;
import ch.bedag.dap.hellodata.portal.rls_role.entity.UserRlsRoleEntity;
import ch.bedag.dap.hellodata.portal.rls_role.repository.RlsRoleNameRepository;
import ch.bedag.dap.hellodata.portal.rls_role.repository.UserRlsRoleRepository;
import ch.bedag.dap.hellodata.portal.user.data.DataDomainDto;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Manages the row level security (RLS) roles RLS_01 - RLS_15 that exist in every Superset instance:
 * their meaningful names per data domain and their assignment to users (directly or through dashboard groups).
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RlsRoleService {

    public static final int RLS_ROLES_COUNT = 15;
    public static final List<String> RLS_ROLE_KEYS = IntStream.rangeClosed(1, RLS_ROLES_COUNT)
            .mapToObj(i -> "%s%02d".formatted(SlugifyUtil.RLS_ROLE_PREFIX, i))
            .toList();
    private static final List<HdRoleName> ELIGIBLE_ROLES = List.of(HdRoleName.DATA_DOMAIN_VIEWER, HdRoleName.DATA_DOMAIN_BUSINESS_SPECIALIST);

    private final RlsRoleNameRepository rlsRoleNameRepository;
    private final UserRlsRoleRepository userRlsRoleRepository;
    private final DashboardGroupService dashboardGroupService;
    private final HdContextRepository contextRepository;
    private final UserRepository userRepository;

    public static boolean isRlsRoleKey(String roleKey) {
        return RLS_ROLE_KEYS.contains(roleKey);
    }

    public static boolean isEligibleForRlsRoles(HdRoleName roleName) {
        return ELIGIBLE_ROLES.contains(roleName);
    }

    /**
     * @return all 15 RLS roles of the data domain, with the default name (the role key) if no name was set
     */
    @Transactional(readOnly = true)
    public List<RlsRoleDto> getRlsRoles(String contextKey) {
        Map<String, String> names = rlsRoleNameRepository.findAllByContextKey(contextKey).stream()
                .collect(Collectors.toMap(RlsRoleNameEntity::getRoleKey, RlsRoleNameEntity::getDisplayName, (a, b) -> a));
        return RLS_ROLE_KEYS.stream()
                .map(roleKey -> new RlsRoleDto(roleKey, names.getOrDefault(roleKey, roleKey)))
                .toList();
    }

    /**
     * Readable by user managers and dashboard group managers for every data domain, by RLS managers only for their own ones.
     */
    @Transactional(readOnly = true)
    public List<RlsRoleDto> getRlsRolesForCurrentUser(String contextKey) {
        Set<String> permissions = SecurityUtils.getCurrentUserPermissions();
        if (!permissions.contains("USER_MANAGEMENT") && !permissions.contains("DASHBOARD_GROUPS_MANAGEMENT")) {
            validateCurrentUserCanManage(contextKey);
        }
        return getRlsRoles(contextKey);
    }

    @Transactional
    public List<RlsRoleDto> updateRlsRoleNames(String contextKey, List<RlsRoleDto> rlsRoles) {
        validateCurrentUserCanManage(contextKey);
        validateRlsRoleNames(rlsRoles);
        Map<String, RlsRoleNameEntity> existing = rlsRoleNameRepository.findAllByContextKey(contextKey).stream()
                .collect(Collectors.toMap(RlsRoleNameEntity::getRoleKey, Function.identity(), (a, b) -> a));
        for (RlsRoleDto rlsRole : rlsRoles) {
            String name = rlsRole.getName() == null ? "" : rlsRole.getName().trim();
            RlsRoleNameEntity entity = existing.get(rlsRole.getRoleKey());
            if (name.isEmpty() || name.equals(rlsRole.getRoleKey())) {
                // default name - no need to store it
                if (entity != null) {
                    rlsRoleNameRepository.delete(entity);
                }
                continue;
            }
            if (entity == null) {
                entity = new RlsRoleNameEntity();
                entity.setContextKey(contextKey);
                entity.setRoleKey(rlsRole.getRoleKey());
            }
            entity.setDisplayName(name);
            rlsRoleNameRepository.save(entity);
        }
        log.info("Updated RLS role names for context {}", contextKey);
        return getRlsRoles(contextKey);
    }

    /**
     * @return data domains in which the current user can manage the RLS role names:
     * all of them for HelloDATA and business domain admins, otherwise the ones the user is a data domain admin of
     */
    @Transactional(readOnly = true)
    public List<DataDomainDto> getManageableDataDomains() {
        Set<String> manageableContextKeys = getManageableContextKeys();
        return contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN)).stream()
                .filter(context -> manageableContextKeys.contains(context.getContextKey()))
                .map(RlsRoleService::toDataDomainDto)
                .sorted(Comparator.comparing(DataDomainDto::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getSelectedRlsRoleKeys(UUID userId, String contextKey) {
        return userRlsRoleRepository.findAllByUserIdAndContextKey(userId, contextKey).stream()
                .map(UserRlsRoleEntity::getRoleKey)
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasSelectedRlsRoles(UUID userId, String contextKey) {
        return userRlsRoleRepository.existsByUserIdAndContextKey(userId, contextKey);
    }

    @Transactional
    public void saveSelectedRlsRoles(UUID userId, String contextKey, Collection<String> roleKeys) {
        userRlsRoleRepository.deleteAllByUserIdAndContextKey(userId, contextKey);
        List<String> validRoleKeys = CollectionUtils.emptyIfNull(roleKeys).stream()
                .filter(RlsRoleService::isRlsRoleKey)
                .distinct()
                .sorted()
                .toList();
        for (String roleKey : validRoleKeys) {
            UserRlsRoleEntity entity = new UserRlsRoleEntity();
            entity.setUserId(userId);
            entity.setContextKey(contextKey);
            entity.setRoleKey(roleKey);
            userRlsRoleRepository.save(entity);
        }
        log.info("Saved {} RLS roles for user {} in context {}", validRoleKeys.size(), userId, contextKey);
    }

    @Transactional
    public void removeAllForUserInContext(UUID userId, String contextKey) {
        userRlsRoleRepository.deleteAllByUserIdAndContextKey(userId, contextKey);
    }

    @Transactional
    public void removeAllForUser(UUID userId) {
        userRlsRoleRepository.deleteAllByUserId(userId);
    }

    /**
     * @return RLS roles of the user in the data domain: the directly assigned ones merged with the ones of the user's dashboard groups
     */
    @Transactional(readOnly = true)
    public List<String> getEffectiveRlsRoleKeys(UUID userId, String contextKey) {
        Set<String> roleKeys = new TreeSet<>(getSelectedRlsRoleKeys(userId, contextKey));
        List<DashboardGroupEntity> groups = dashboardGroupService.findGroupsByContextKeyAndUserId(contextKey, userId.toString());
        for (DashboardGroupEntity group : groups) {
            CollectionUtils.emptyIfNull(group.getRlsRoles()).stream()
                    .filter(RlsRoleService::isRlsRoleKey)
                    .forEach(roleKeys::add);
        }
        return new ArrayList<>(roleKeys);
    }

    private void validateRlsRoleNames(List<RlsRoleDto> rlsRoles) {
        if (rlsRoles == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RLS roles are required");
        }
        Set<String> names = new HashSet<>();
        for (RlsRoleDto rlsRole : rlsRoles) {
            if (!isRlsRoleKey(rlsRole.getRoleKey())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown RLS role: " + rlsRole.getRoleKey());
            }
            String name = rlsRole.getName() == null || rlsRole.getName().isBlank() ? rlsRole.getRoleKey() : rlsRole.getName().trim();
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "RLS role name must be unique: " + name);
            }
        }
    }

    private void validateCurrentUserCanManage(String contextKey) {
        if (!getManageableContextKeys().contains(contextKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to manage the RLS roles of data domain " + contextKey);
        }
    }

    private Set<String> getManageableContextKeys() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user not found");
        }
        UserEntity currentUser = userRepository.getByIdOrAuthId(currentUserId.toString());
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user not found");
        }
        List<HdContextEntity> dataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        if (SecurityUtils.isSuperuser() || BooleanUtils.isTrue(currentUser.isSuperuser()) || BooleanUtils.isTrue(currentUser.isBusinessDomainAdmin())) {
            return dataDomains.stream().map(HdContextEntity::getContextKey).collect(Collectors.toSet());
        }
        return CollectionUtils.emptyIfNull(currentUser.getContextRoles()).stream()
                .filter(contextRole -> contextRole.getRole().getName() == HdRoleName.DATA_DOMAIN_ADMIN)
                .map(UserContextRoleEntity::getContextKey)
                .collect(Collectors.toSet());
    }

    private static DataDomainDto toDataDomainDto(HdContextEntity context) {
        DataDomainDto dto = new DataDomainDto();
        dto.setId(context.getId());
        dto.setKey(context.getContextKey());
        dto.setName(context.getName());
        return dto;
    }
}
