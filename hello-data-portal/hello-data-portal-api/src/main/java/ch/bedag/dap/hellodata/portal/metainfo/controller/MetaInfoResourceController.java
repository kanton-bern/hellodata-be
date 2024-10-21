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

import ch.bedag.dap.hellodata.commons.metainfomodel.entities.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.metainfo.data.SubsystemUserDto;
import ch.bedag.dap.hellodata.portal.metainfo.data.SubsystemUsersResultDto;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.portal.user.data.UserDto;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.bedag.dap.hellodata.commons.SlugifyUtil.*;
import static ch.bedag.dap.hellodata.commons.security.Permission.USER_MANAGEMENT;
import static ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind.HELLO_DATA_APP_INFO;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/metainfo")
public class MetaInfoResourceController {

    private final MetaInfoResourceService metaInfoResourceService;
    private final UserService userService;

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
        List<SubsystemUsersResultDto> result = new ArrayList<>();
        List<HdResource> userPacksForSubsystems = metaInfoResourceService.findAllByKind(ModuleResourceKind.HELLO_DATA_USERS);
        for (HdResource usersPack : userPacksForSubsystems) {
            List<SubsystemUser> subsystemUsers = ((List<SubsystemUser>) usersPack.getData()).stream().toList();
            List<SubsystemUserDto> subsystemUserDtos = new ArrayList<>(subsystemUsers.size());
            for (SubsystemUser u : subsystemUsers) {
                SubsystemUserDto subsystemUserDto = new SubsystemUserDto(
                        u.getFirstName(), u.getLastName(), u.getEmail(), u.getUsername(), u.getRoles().stream().map(r -> r.getName()).toList(), usersPack.getInstanceName()
                );
                subsystemUserDtos.add(subsystemUserDto);
            }
            result.add(new SubsystemUsersResultDto(usersPack.getInstanceName(), subsystemUserDtos));
        }
        return result;
    }

    /**
     * Fetches users with roles for all subsystems
     *
     * @return list of resources
     */
    @PreAuthorize("hasAnyAuthority('USERS_OVERVIEW')")
    @GetMapping(value = "/resources/users-overview")
    public List<SubsystemUsersResultDto> getAllUsersWithRolesForDashboards() {
        List<SubsystemUsersResultDto> result = new ArrayList<>();
        List<AppInfoResource> supersetAppInfos = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_APP_INFO, AppInfoResource.class);
        Set<String> supersetsNames = supersetAppInfos.stream().map(superset -> superset.getInstanceName()).collect(Collectors.toSet());
        List<UserDto> allPortalUsers = userService.getAllUsers();
        List<String> allPortalUsersEmails = allPortalUsers.stream().map(u -> u.getEmail()).toList();
        List<MetaInfoResourceEntity> userPacksForSubsystems = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_USERS).stream().filter(uPack -> supersetsNames.contains(uPack.getInstanceName())).toList();
        for (MetaInfoResourceEntity usersPack : userPacksForSubsystems) {
            List<SubsystemUser> subsystemUsers = ((List<SubsystemUser>) usersPack.getMetainfo().getData()).stream().filter(u -> allPortalUsersEmails.contains(u.getEmail())).toList();
            List<SubsystemUserDto> subsystemUserDtos = new ArrayList<>(subsystemUsers.size());
            for (SubsystemUser subsystemUser : subsystemUsers) {
                addUser(usersPack, subsystemUser, allPortalUsers, subsystemUserDtos);
            }
            result.add(new SubsystemUsersResultDto(usersPack.getInstanceName(), subsystemUserDtos));
        }

        return result;
    }

    private void addUser(MetaInfoResourceEntity usersPack, SubsystemUser u, List<UserDto> allPortalUsers, List<SubsystemUserDto> subsystemUserDtos) {
        Optional<UserDto> portalUser = allPortalUsers.stream().filter(usr -> u.getEmail().equalsIgnoreCase(u.getEmail())).findFirst();
        if (portalUser.isPresent()) {
            List<String> roles = u.getRoles().stream().map(r -> r.getName()).filter(r -> complies(r)).sorted().toList();
            SubsystemUserDto subsystemUserDto = new SubsystemUserDto(
                    u.getFirstName(),
                    u.getLastName(),
                    u.getEmail(),
                    u.getUsername(),
                    roles,
                    usersPack.getInstanceName()
            );
            subsystemUserDtos.add(subsystemUserDto);

        }
    }

    private boolean complies(String r) {
        return r.startsWith(DASHBOARD_ROLE_PREFIX) || r.equalsIgnoreCase(BI_ADMIN_ROLE_NAME) || r.equalsIgnoreCase(BI_VIEWER_ROLE_NAME) || r.equalsIgnoreCase(BI_EDITOR_ROLE_NAME);
    }
}
