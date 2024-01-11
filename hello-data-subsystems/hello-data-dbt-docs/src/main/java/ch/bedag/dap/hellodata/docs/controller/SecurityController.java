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
package ch.bedag.dap.hellodata.docs.controller;

import ch.bedag.dap.hellodata.docs.entities.Privilege;
import ch.bedag.dap.hellodata.docs.entities.Role;
import ch.bedag.dap.hellodata.docs.entities.User;
import ch.bedag.dap.hellodata.docs.model.PrivilegeDto;
import ch.bedag.dap.hellodata.docs.model.RoleDto;
import ch.bedag.dap.hellodata.docs.model.UserDto;
import ch.bedag.dap.hellodata.docs.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/security")
public class SecurityController {

    private final SecurityService securityService;

    @GetMapping(path = "roles")
    @Operation(summary = "Get all roles")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public List<RoleDto> getAllRoles() {
        return toDto(securityService.getAllRoles());
    }

    @GetMapping(path = "users")
    @Operation(summary = "Get all users")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public List<UserDto> getAllUsers() {
        return toUserDtoList(securityService.getAllUsers());
    }

    @GetMapping(path = "privileges")
    @Operation(summary = "Get all privileges")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public List<PrivilegeDto> getAllPrivileges() {
        return toPrivilegeDtoList(securityService.getAllPrivileges());
    }

    private List<PrivilegeDto> toPrivilegeDtoList(List<Privilege> privileges) {
        return privileges.stream().map(p -> new PrivilegeDto(p.getId(), p.getName())).toList();
    }

    private List<UserDto> toUserDtoList(List<User> users) {
        return users.stream().map(u -> new UserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), toDto(new ArrayList<>(u.getRoles())))).toList();
    }

    private List<RoleDto> toDto(List<Role> roles) {
        return roles.stream().map(r -> new RoleDto(r.getId(), r.getKey(), r.getName(), toDto(r.getPrivileges()))).toList();
    }

    private Collection<PrivilegeDto> toDto(Collection<Privilege> privileges) {
        return privileges.stream().map(p -> new PrivilegeDto(p.getId(), p.getName())).toList();
    }
}
