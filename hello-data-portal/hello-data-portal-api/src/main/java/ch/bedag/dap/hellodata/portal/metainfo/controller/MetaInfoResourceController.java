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
package ch.bedag.dap.hellodata.portal.metainfo.controller;

import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.portal.csv.service.BatchExportService;
import ch.bedag.dap.hellodata.portal.metainfo.data.DashboardUsersResultDto;
import ch.bedag.dap.hellodata.portal.metainfo.data.SubsystemUsersResultDto;
import ch.bedag.dap.hellodata.portal.metainfo.data.UserSubsystemRolesDto;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoUsersService;
import ch.bedag.dap.hellodata.portal.base.util.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.bedag.dap.hellodata.commons.security.Permission.USER_MANAGEMENT;
import static ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind.HELLO_DATA_APP_INFO;
import static ch.bedag.dap.hellodata.portal.base.config.RedisConfig.SUBSYSTEM_USERS_CACHE;
import static ch.bedag.dap.hellodata.portal.base.config.RedisConfig.USERS_WITH_DASHBOARD_CACHE;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/metainfo")
public class MetaInfoResourceController {

    private final MetaInfoResourceService metaInfoResourceService;
    private final MetaInfoUsersService metaInfoUsersService;
    private final BatchExportService batchExportService;

    @PreAuthorize("hasAnyAuthority('WORKSPACES')")
    @GetMapping(value = "/resources/filtered/by-app-info")
    public List<HdResource> findAllByAppInfo(@RequestParam String apiVersion, @RequestParam String instanceName, @RequestParam ModuleType moduleType) {
        return metaInfoResourceService.findAllByAppInfo(apiVersion, instanceName, moduleType);
    }

    /**
     * the 'kind' will be with slash included (e.g.: hellodata/AppInfo) so query parameter used instead of path param
     *
     * @param kind (e.g.: hellodata/AppInfo)
     * @return list of resources
     */
    @GetMapping(value = "/kinds/kind/resources")
    public List<HdResource> findAllByKind(@RequestParam String kind) {
        Set<String> currentUserPermissions = SecurityUtils.getCurrentUserPermissions();
        if (HELLO_DATA_APP_INFO.equalsIgnoreCase(kind) || currentUserPermissions.contains(USER_MANAGEMENT.name())) {
            return metaInfoResourceService.findAllByKind(kind);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot fetch data, not enough privileges");
    }

    /**
     * Fetches users with roles for all subsystems
     *
     * @return list of resources
     */
    @PreAuthorize("hasAnyAuthority('WORKSPACES')")
    @GetMapping(value = "/resources/subsystem-users")
    public List<SubsystemUsersResultDto> getAllUsersWithRoles() {
        return metaInfoUsersService.getAllUsersWithRoles();
    }

    @CacheEvict(value = SUBSYSTEM_USERS_CACHE)
    @PreAuthorize("hasAnyAuthority('WORKSPACES')")
    @GetMapping(value = "/resources/subsystem-users/clear-cache")
    public void clearSubsystemUsersCache() {
        log.info("Cleared up subsystem users cache by {}", SecurityUtils.getCurrentUserEmail());
    }

    /**
     * Fetches users with dashboards for all superset subsystems
     *
     * @return list of resources
     */
    @PreAuthorize("hasAnyAuthority('USERS_OVERVIEW')")
    @GetMapping(value = "/resources/users-dashboards-overview")
    public List<DashboardUsersResultDto> getAllUsersWithRolesForDashboards() {
        return metaInfoUsersService.getAllUsersWithRolesForDashboards();
    }

    @CacheEvict(value = USERS_WITH_DASHBOARD_CACHE)
    @PreAuthorize("hasAnyAuthority('USERS_OVERVIEW')")
    @GetMapping(value = "/resources/users-dashboards-overview/clear-cache")
    public void clearUsersWithRolesForDashboardsCache() {
        log.info("Cleared up users with dashboards cache by {}", SecurityUtils.getCurrentUserEmail());
    }

    /**
     * Exports all users with their context roles and superset roles as a CSV file
     * compatible with the batch users import format.
     *
     * @return CSV file as a downloadable response
     */
    @PreAuthorize("hasAnyAuthority('WORKSPACES')")
    @GetMapping(value = "/resources/subsystem-users/batch-export", produces = "text/csv")
    public ResponseEntity<byte[]> exportBatchUsersCsv() {
        log.info("Batch export of users requested by {}", SecurityUtils.getCurrentUserEmail());
        String csvContent = batchExportService.generateBatchExportCsv();
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", "batch-users-export.csv");

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('WORKSPACES')")
    @GetMapping(value = "/resources/subsystem-users/paginated")
    public ResponseEntity<Page<UserSubsystemRolesDto>> getSubsystemUsersPaginated(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageUtil.createPageable(page, size, sort, "email", org.springframework.data.domain.Sort.Direction.ASC);
        return ResponseEntity.ok(metaInfoUsersService.getSubsystemUsersPaginated(pageable, search));
    }

    @PreAuthorize("hasAnyAuthority('USERS_OVERVIEW')")
    @GetMapping(value = "/resources/users-dashboards-overview/paginated")
    public ResponseEntity<Page<UserSubsystemRolesDto>> getDashboardUsersPaginated(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageUtil.createPageable(page, size, sort, "email", org.springframework.data.domain.Sort.Direction.ASC);
        return ResponseEntity.ok(metaInfoUsersService.getDashboardUsersPaginated(pageable, search));
    }

    @PreAuthorize("hasAnyAuthority('WORKSPACES')")
    @GetMapping(value = "/resources/subsystem-users/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportSubsystemUsersCsv(@RequestParam(required = false) String search) {
        log.info("Subsystem users CSV export requested by {} with search='{}'", SecurityUtils.getCurrentUserEmail(), search);
        List<UserSubsystemRolesDto> users = metaInfoUsersService.getSubsystemUsersForExport(search);
        String csv = buildUserCentricCsv(users);
        return buildCsvResponse(csv, "subsystem-users-export.csv");
    }

    @PreAuthorize("hasAnyAuthority('USERS_OVERVIEW')")
    @GetMapping(value = "/resources/users-dashboards-overview/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportDashboardUsersCsv(@RequestParam(required = false) String search) {
        log.info("Dashboard users CSV export requested by {} with search='{}'", SecurityUtils.getCurrentUserEmail(), search);
        List<UserSubsystemRolesDto> users = metaInfoUsersService.getDashboardUsersForExport(search);
        String csv = buildUserCentricCsv(users);
        return buildCsvResponse(csv, "dashboard-users-export.csv");
    }

    private String buildUserCentricCsv(List<UserSubsystemRolesDto> users) {
        Set<String> columnKeys = collectSubsystemColumnKeys(users);

        StringBuilder sb = new StringBuilder();
        appendCsvHeader(sb, columnKeys);
        for (UserSubsystemRolesDto user : users) {
            appendCsvRow(sb, user, columnKeys);
        }
        return sb.toString();
    }

    private Set<String> collectSubsystemColumnKeys(List<UserSubsystemRolesDto> users) {
        Set<String> columnKeys = new LinkedHashSet<>();
        for (UserSubsystemRolesDto user : users) {
            if (user.subsystemRoles() != null) {
                columnKeys.addAll(user.subsystemRoles().keySet());
            }
        }
        return columnKeys;
    }

    private void appendCsvHeader(StringBuilder sb, Set<String> columnKeys) {
        sb.append("email;firstName;lastName;enabled;businessDomainRole;dataDomainRoles");
        for (String col : columnKeys) {
            sb.append(';').append(col);
        }
        sb.append('\n');
    }

    private void appendCsvRow(StringBuilder sb, UserSubsystemRolesDto user, Set<String> columnKeys) {
        sb.append(csvEscape(user.email())).append(';');
        sb.append(csvEscape(user.firstName())).append(';');
        sb.append(csvEscape(user.lastName())).append(';');
        sb.append(user.enabled()).append(';');
        String businessRole = user.businessDomainRole() != null ? user.businessDomainRole().name() : "";
        sb.append(businessRole).append(';');
        String ddr = formatDataDomainRoles(user);
        sb.append(csvEscape(ddr));
        for (String col : columnKeys) {
            sb.append(';');
            List<String> roles = user.subsystemRoles() != null
                    ? user.subsystemRoles().getOrDefault(col, List.of())
                    : List.of();
            sb.append(csvEscape(String.join(", ", roles)));
        }
        sb.append('\n');
    }

    private String formatDataDomainRoles(UserSubsystemRolesDto user) {
        if (user.dataDomainRoles() == null) {
            return "";
        }
        return user.dataDomainRoles().stream()
                .map(r -> {
                    String roleName = r.role() != null ? r.role().name() : "";
                    return r.contextName() + ":" + roleName;
                })
                .collect(Collectors.joining(", "));
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private ResponseEntity<byte[]> buildCsvResponse(String csv, String filename) {
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

}
