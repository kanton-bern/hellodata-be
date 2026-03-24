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
package ch.bedag.dap.hellodata.portal.csv.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.repository.DashboardGroupRepository;
import ch.bedag.dap.hellodata.portal.metainfo.data.DataDomainRoleDto;
import ch.bedag.dap.hellodata.portal.user.data.UserWithBusinessRoleDto;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class BatchExportService {

    static final String CSV_HEADER = "email;businessDomainRole;context;dataDomainRole;supersetRole;dashboardGroup";
    private static final char CSV_DELIMITER = ';';
    private static final String ROLE_DELIMITER = ",";
    private static final String ADMIN_ROLE = "Admin";
    private static final String BI_ROLE_PREFIX = "BI_";
    private static final String SQL_LAB_ROLE = "sql_lab";

    private final UserService userService;
    private final MetaInfoResourceService metaInfoResourceService;
    private final HdContextRepository contextRepository;
    private final DashboardGroupRepository dashboardGroupRepository;

    @Transactional(readOnly = true)
    public String generateBatchExportCsv() {
        List<UserWithBusinessRoleDto> allUsers = userService.getAllUsersWithBusinessDomainRole();
        Map<String, Map<String, List<String>>> supersetRolesMap = buildSupersetRolesMap();
        List<String> dataDomainContextKeys = getDataDomainContextKeys();
        Map<String, Map<String, List<String>>> dashboardGroupMap = buildDashboardGroupMap();

        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER).append('\n');

        for (UserWithBusinessRoleDto user : allUsers) {
            appendUserRows(csv, user, supersetRolesMap, dataDomainContextKeys, dashboardGroupMap);
        }

        return csv.toString();
    }

    void appendUserRows(StringBuilder csv, UserWithBusinessRoleDto user,
                        Map<String, Map<String, List<String>>> supersetRolesMap,
                        List<String> dataDomainContextKeys,
                        Map<String, Map<String, List<String>>> dashboardGroupMap) {
        String email = user.getEmail();
        String userId = user.getId();
        String bdRole = user.getBusinessDomainRole() != null
                ? user.getBusinessDomainRole().name()
                : HdRoleName.NONE.name();

        List<DataDomainRoleDto> ddRoles = user.getDataDomainRoles();
        Set<String> coveredContextKeys = new HashSet<>();

        if (ddRoles != null) {
            for (DataDomainRoleDto ddRole : ddRoles) {
                coveredContextKeys.add(ddRole.contextKey());
                String supersetRoles = formatSupersetRoles(supersetRolesMap, email, ddRole.contextKey());
                String dashboardGroups = formatDashboardGroups(dashboardGroupMap, userId, ddRole.contextKey());
                appendRow(csv, email, bdRole, ddRole.contextKey(), ddRole.role().name(), supersetRoles, dashboardGroups);
            }
        }

        for (String contextKey : dataDomainContextKeys) {
            if (!coveredContextKeys.contains(contextKey)) {
                String dashboardGroups = formatDashboardGroups(dashboardGroupMap, userId, contextKey);
                appendRow(csv, email, bdRole, contextKey, HdRoleName.NONE.name(), "", dashboardGroups);
            }
        }
    }

    @SuppressWarnings("unchecked")
    Map<String, Map<String, List<String>>> buildSupersetRolesMap() {
        List<MetaInfoResourceEntity> supersetUserResources =
                metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_USERS);

        Map<String, Map<String, List<String>>> result = new HashMap<>();

        for (MetaInfoResourceEntity resource : supersetUserResources) {
            String contextKey = resource.getContextKey();
            if (contextKey == null || contextKey.isEmpty()) {
                continue;
            }
            List<SubsystemUser> users = (List<SubsystemUser>) resource.getMetainfo().getData();
            for (SubsystemUser user : users) {
                String email = user.getEmail();
                if (email == null) {
                    continue;
                }
                List<String> filteredRoles = user.getRoles().stream()
                        .map(SubsystemRole::getName)
                        .filter(BatchExportService::isExportableSupersetRole)
                        .toList();

                result.computeIfAbsent(email.toLowerCase(Locale.ROOT), k -> new HashMap<>())
                        .merge(contextKey, new ArrayList<>(filteredRoles), (existing, incoming) -> {
                            existing.addAll(incoming);
                            return existing;
                        });
            }
        }
        return result;
    }

    static boolean isExportableSupersetRole(String roleName) {
        return !ADMIN_ROLE.equals(roleName)
                && !roleName.startsWith(BI_ROLE_PREFIX)
                && !SQL_LAB_ROLE.equals(roleName);
    }

    List<String> getDataDomainContextKeys() {
        return contextRepository.findAll().stream()
                .filter(c -> c.getType() == HdContextType.DATA_DOMAIN)
                .map(HdContextEntity::getContextKey)
                .sorted()
                .toList();
    }

    String formatSupersetRoles(Map<String, Map<String, List<String>>> supersetRolesMap,
                               String email, String contextKey) {
        Map<String, List<String>> contextRoles = supersetRolesMap.get(email.toLowerCase(Locale.ROOT));
        if (contextRoles == null) {
            return "";
        }
        List<String> roles = contextRoles.get(contextKey);
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return String.join(ROLE_DELIMITER, roles);
    }

    private void appendRow(StringBuilder csv, String email, String businessDomainRole,
                           String context, String dataDomainRole, String supersetRoles, String dashboardGroups) {
        csv.append(email).append(CSV_DELIMITER)
                .append(businessDomainRole).append(CSV_DELIMITER)
                .append(context).append(CSV_DELIMITER)
                .append(dataDomainRole).append(CSV_DELIMITER)
                .append(supersetRoles).append(CSV_DELIMITER)
                .append(dashboardGroups)
                .append('\n');
    }

    /**
     * Builds a map of userId → contextKey → list of dashboard group names.
     */
    Map<String, Map<String, List<String>>> buildDashboardGroupMap() {
        List<DashboardGroupEntity> allGroups = dashboardGroupRepository.findAll();
        Map<String, Map<String, List<String>>> result = new HashMap<>();

        for (DashboardGroupEntity group : allGroups) {
            if (group.getUsers() == null || group.getContextKey() == null) {
                continue;
            }
            for (var userEntry : group.getUsers()) {
                if (userEntry.getId() == null) {
                    continue;
                }
                result.computeIfAbsent(userEntry.getId(), k -> new HashMap<>())
                        .computeIfAbsent(group.getContextKey(), k -> new ArrayList<>())
                        .add(group.getName());
            }
        }
        return result;
    }

    String formatDashboardGroups(Map<String, Map<String, List<String>>> dashboardGroupMap,
                                 String userId, String contextKey) {
        if (userId == null) {
            return "";
        }
        Map<String, List<String>> contextGroups = dashboardGroupMap.get(userId);
        if (contextGroups == null) {
            return "";
        }
        List<String> groups = contextGroups.get(contextKey);
        if (groups == null || groups.isEmpty()) {
            return "";
        }
        return String.join(ROLE_DELIMITER, groups);
    }
}
