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
package ch.bedag.dap.hellodata.portal.role.service;

import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.user.service.KeycloakService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps a portal user's Keycloak realm roles in sync with the roles the Airflow 3 api-server
 * expects. Airflow 3 no longer has a REST endpoint to create users/roles (the monitoring sidecar
 * is now read-only); instead FAB authenticates against Keycloak and maps Keycloak realm roles to
 * FAB roles at each login ({@code AUTH_ROLES_MAPPING} + a dynamic {@code DD_<domain>} pass-through
 * in {@code webserver_config.py}). So the portal — the one component with Keycloak admin access —
 * owns creating and assigning those realm roles.
 *
 * <p>Mapping (mirrors the pre-3 sidecar behaviour, where only admins reach Airflow):
 * <ul>
 *   <li>{@link HdRoleName#HELLODATA_ADMIN} anywhere &rarr; {@code airflow_admin} (FAB {@code Admin}).</li>
 *   <li>{@link HdRoleName#DATA_DOMAIN_ADMIN} on a data domain &rarr; {@code airflow_base} (FAB UI shell)
 *       plus {@code DD_<contextKey>} (per-domain DAG read/trigger via the {@code dag_policy}).</li>
 * </ul>
 *
 * <p>Feature-flagged off by default so existing Airflow 2 deployments are untouched, and requires the
 * portal admin client to hold {@code manage-realm}.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AirflowKeycloakRoleService {

    static final String ROLE_AIRFLOW_ADMIN = "airflow_admin";
    static final String ROLE_AIRFLOW_BASE = "airflow_base";
    static final String DATA_DOMAIN_ROLE_PREFIX = "DD_";
    private static final String AIRFLOW_ROLE_PREFIX = "airflow_";

    private final KeycloakService keycloakService;
    private final RoleService roleService;

    @Value("${hello-data.airflow3.role-sync-enabled:false}")
    private boolean roleSyncEnabled;

    /**
     * Reconcile the Airflow-managed realm roles ({@code airflow_*}, {@code DD_*}) of the given user's
     * Keycloak account to exactly what their portal context roles imply. Best-effort: never throws, so
     * a Keycloak hiccup cannot break the surrounding subsystem synchronization.
     */
    public void reconcileUserAirflowRoles(UserEntity user) {
        if (!roleSyncEnabled) {
            return;
        }
        String authId = user.getAuthId();
        if (!StringUtils.hasText(authId)) {
            log.debug("Skipping Airflow Keycloak role sync for {} - no authId yet", user.getEmail());
            return;
        }
        try {
            Set<String> desired = computeDesiredRoles(user);
            Set<String> currentManaged = keycloakService.getRealmRoleNamesOfUser(authId).stream()
                    .filter(this::isAirflowManaged).collect(Collectors.toSet());

            Set<String> toAdd = new HashSet<>(desired);
            toAdd.removeAll(currentManaged);
            Set<String> toRemove = new HashSet<>(currentManaged);
            toRemove.removeAll(desired);

            toAdd.forEach(keycloakService::createRealmRoleIfMissing);
            keycloakService.addRealmRolesToUser(authId, toAdd);
            keycloakService.removeRealmRolesFromUser(authId, toRemove);

            if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
                log.info("Reconciled Airflow Keycloak roles for {}: added {}, removed {}", user.getEmail(), toAdd, toRemove);
            }
        } catch (Exception e) {
            log.error("Could not reconcile Airflow Keycloak roles for {}", user.getEmail(), e);
        }
    }

    /**
     * Translate the user's portal context roles into the set of Keycloak realm roles Airflow 3 expects.
     * Package-private so it can be unit-tested without a live Keycloak.
     */
    Set<String> computeDesiredRoles(UserEntity user) {
        Set<String> desired = new HashSet<>();
        List<UserContextRoleEntity> contextRoles = roleService.getAllContextRolesForUser(user);
        for (UserContextRoleEntity contextRole : contextRoles) {
            HdRoleName roleName = contextRole.getRole() == null ? null : contextRole.getRole().getName();
            if (roleName == HdRoleName.HELLODATA_ADMIN) {
                desired.add(ROLE_AIRFLOW_ADMIN);
            } else if (roleName == HdRoleName.DATA_DOMAIN_ADMIN && StringUtils.hasText(contextRole.getContextKey())) {
                desired.add(ROLE_AIRFLOW_BASE);
                desired.add(DATA_DOMAIN_ROLE_PREFIX + contextRole.getContextKey());
            }
        }
        return desired;
    }

    private boolean isAirflowManaged(String roleName) {
        return roleName != null && (roleName.startsWith(AIRFLOW_ROLE_PREFIX) || roleName.startsWith(DATA_DOMAIN_ROLE_PREFIX));
    }
}
