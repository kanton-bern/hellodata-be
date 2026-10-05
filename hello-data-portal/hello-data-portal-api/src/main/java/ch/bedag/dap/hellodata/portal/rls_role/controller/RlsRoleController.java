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
package ch.bedag.dap.hellodata.portal.rls_role.controller;

import ch.bedag.dap.hellodata.portal.rls_role.data.RlsRoleDto;
import ch.bedag.dap.hellodata.portal.rls_role.service.RlsRoleService;
import ch.bedag.dap.hellodata.portal.user.data.DataDomainDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rls-roles")
public class RlsRoleController {
    private final RlsRoleService rlsRoleService;

    @GetMapping("/data-domains")
    @PreAuthorize("hasAnyAuthority('RLS_ROLES_MANAGEMENT')")
    public List<DataDomainDto> getManageableDataDomains() {
        return rlsRoleService.getManageableDataDomains();
    }

    @GetMapping("/{contextKey}")
    @PreAuthorize("hasAnyAuthority('RLS_ROLES_MANAGEMENT', 'USER_MANAGEMENT', 'DASHBOARD_GROUPS_MANAGEMENT')")
    public List<RlsRoleDto> getRlsRoles(@PathVariable String contextKey) {
        return rlsRoleService.getRlsRolesForCurrentUser(contextKey);
    }

    @PutMapping("/{contextKey}")
    @PreAuthorize("hasAnyAuthority('RLS_ROLES_MANAGEMENT')")
    public List<RlsRoleDto> updateRlsRoleNames(@PathVariable String contextKey, @NotNull @Valid @RequestBody List<RlsRoleDto> rlsRoles) {
        return rlsRoleService.updateRlsRoleNames(contextKey, rlsRoles);
    }
}
