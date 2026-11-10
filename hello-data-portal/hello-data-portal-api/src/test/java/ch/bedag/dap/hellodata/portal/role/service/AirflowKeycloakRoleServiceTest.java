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
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirflowKeycloakRoleServiceTest {

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private AirflowKeycloakRoleService service;

    @BeforeEach
    void enableRoleSync() {
        ReflectionTestUtils.setField(service, "roleSyncEnabled", true);
    }

    @Test
    void usesThePortalIdWhenTheUserHasNoAuthIdYet() {
        // the example users and the users created in the portal get the id of their Keycloak account as portal id, the
        // authId is set only by the hourly Keycloak user sync
        UserEntity user = user(null);
        when(roleService.getAllContextRolesForUser(user)).thenReturn(List.of(contextRole(HdRoleName.HELLODATA_ADMIN, null)));
        when(keycloakService.getRealmRoleNamesOfUser(user.getId().toString())).thenReturn(Set.of("default-roles-hellodata"));

        service.reconcileUserAirflowRoles(user);

        verify(keycloakService).createRealmRoleIfMissing(AirflowKeycloakRoleService.ROLE_AIRFLOW_ADMIN);
        verify(keycloakService).addRealmRolesToUser(user.getId().toString(), Set.of(AirflowKeycloakRoleService.ROLE_AIRFLOW_ADMIN));
    }

    @Test
    void prefersTheAuthId() {
        UserEntity user = user("keycloak-id");
        when(roleService.getAllContextRolesForUser(user)).thenReturn(List.of(contextRole(HdRoleName.DATA_DOMAIN_ADMIN, "Default_Data_Domain")));
        when(keycloakService.getRealmRoleNamesOfUser("keycloak-id")).thenReturn(Set.of("DD_Extra_Data_Domain"));

        service.reconcileUserAirflowRoles(user);

        verify(keycloakService).addRealmRolesToUser("keycloak-id", Set.of(AirflowKeycloakRoleService.ROLE_AIRFLOW_BASE, "DD_Default_Data_Domain"));
        verify(keycloakService).removeRealmRolesFromUser("keycloak-id", Set.of("DD_Extra_Data_Domain"));
    }

    @Test
    void doesNothingWhenTheRoleSyncIsDisabled() {
        ReflectionTestUtils.setField(service, "roleSyncEnabled", false);

        service.reconcileUserAirflowRoles(user(null));

        verifyNoInteractions(keycloakService, roleService);
    }

    @Test
    void skipsAUserWithoutAnyId() {
        UserEntity user = new UserEntity();

        service.reconcileUserAirflowRoles(user);

        verify(keycloakService, never()).getRealmRoleNamesOfUser(anyString());
        verify(keycloakService, never()).addRealmRolesToUser(anyString(), any());
    }

    private static UserEntity user(String authId) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setAuthId(authId);
        user.setEmail("user@hellodata.ch");
        return user;
    }

    private static UserContextRoleEntity contextRole(HdRoleName roleName, String contextKey) {
        RoleEntity role = new RoleEntity();
        role.setName(roleName);
        UserContextRoleEntity contextRole = new UserContextRoleEntity();
        contextRole.setRole(role);
        contextRole.setContextKey(contextKey);
        return contextRole;
    }
}
