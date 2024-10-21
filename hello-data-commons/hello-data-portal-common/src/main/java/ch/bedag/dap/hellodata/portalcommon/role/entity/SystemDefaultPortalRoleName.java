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
package ch.bedag.dap.hellodata.portalcommon.role.entity;

import ch.bedag.dap.hellodata.commons.security.Permission;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum SystemDefaultPortalRoleName {
    HELLODATA_ADMIN(Arrays.asList(Permission.values())),
    BUSINESS_DOMAIN_ADMIN(List.of(Permission.USER_MANAGEMENT, Permission.FAQ_MANAGEMENT, Permission.EXTERNAL_DASHBOARDS_MANAGEMENT, Permission.DOCUMENTATION_MANAGEMENT,
            Permission.ANNOUNCEMENT_MANAGEMENT, Permission.DASHBOARDS, Permission.DATA_LINEAGE, Permission.DATA_MARTS, Permission.DATA_DWH, Permission.DATA_ENG,
            Permission.DATA_JUPYTER, Permission.USERS_OVERVIEW)),
    DATA_DOMAIN_ADMIN(List.of(Permission.DASHBOARDS, Permission.DATA_LINEAGE, Permission.DATA_MARTS, Permission.DATA_DWH, Permission.DATA_ENG, Permission.DATA_JUPYTER)),
    DATA_DOMAIN_EDITOR(List.of(Permission.DASHBOARDS, Permission.DATA_LINEAGE, Permission.DATA_MARTS)),
    DATA_DOMAIN_VIEWER(List.of(Permission.DASHBOARDS, Permission.DATA_LINEAGE));

    private final List<Permission> permissions;
}
