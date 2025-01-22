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
package ch.bedag.dap.hellodata.sidecars.superset.service.user;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolesResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserRolesUpdate;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;

@UtilityClass
public class RoleUtil {

    public static final String PUBLIC_ROLE_NAME = "Public";

    /**
     * superset api requires at list one element in an array
     *
     * @param allRoles                to check if user roles to set is empty
     * @param supersetUserRolesUpdate payload
     */
    public static void leaveOnlyBiViewerRoleIfNoneAttached(SupersetRolesResponse allRoles, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        if (CollectionUtils.isEmpty(supersetUserRolesUpdate.getRoles())) {
            supersetUserRolesUpdate.setRoles(allRoles.getResult()
                                                     .stream()
                                                     .filter(supersetRole -> supersetRole.getName().equalsIgnoreCase(SlugifyUtil.BI_VIEWER_ROLE_NAME))
                                                     .map(SubsystemRole::getId)
                                                     .toList());
        }
    }

    public static void removeAllDashboardRoles(SupersetRolesResponse allRoles, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        List<Integer> roles = allRoles.getResult().stream().filter(role -> role.getName().startsWith(SlugifyUtil.DASHBOARD_ROLE_PREFIX)).map(SubsystemRole::getId).toList();
        List<Integer> userRoles = supersetUserRolesUpdate.getRoles().stream().filter(supersetRoleId -> !roles.contains(supersetRoleId)).toList();
        supersetUserRolesUpdate.setRoles(userRoles);
    }

    public static void removeRoleFromUser(String roleName, SupersetRolesResponse allRoles, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        List<Integer> roles = allRoles.getResult().stream().filter(role -> role.getName().equalsIgnoreCase(roleName)).map(SubsystemRole::getId).toList();
        List<Integer> userRoles = supersetUserRolesUpdate.getRoles().stream().filter(supersetRoleId -> !roles.contains(supersetRoleId)).toList();
        supersetUserRolesUpdate.setRoles(userRoles);
    }

    public static void removePublicRoleIfAdded(SupersetRolesResponse allRoles, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        Optional<Integer> publicRole = allRoles.getResult().stream().filter(role -> role.getName().equalsIgnoreCase(PUBLIC_ROLE_NAME)).map(SubsystemRole::getId).findFirst();
        publicRole.ifPresent(publicRoleId -> supersetUserRolesUpdate.setRoles(supersetUserRolesUpdate.getRoles().stream().filter(roleId -> !publicRoleId.equals(roleId)).toList()));
    }
}
