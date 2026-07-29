/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package ch.bedag.dap.hellodata.portal.role.service;

import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.user.service.KeycloakService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * TEMPORARY (remove before merge). Verifies the portal derives and reconciles exactly the Keycloak
 * realm roles the Airflow 3 FAB OAuth login expects (airflow_admin / airflow_base / DD_&lt;domain&gt;).
 */
@ExtendWith(MockitoExtension.class)
class AirflowKeycloakRoleServiceTest {

    @Mock
    private KeycloakService keycloakService;
    @Mock
    private RoleService roleService;
    @InjectMocks
    private AirflowKeycloakRoleService service;

    private static UserContextRoleEntity contextRole(HdRoleName roleName, String contextKey) {
        RoleEntity role = new RoleEntity();
        role.setName(roleName);
        UserContextRoleEntity ucr = new UserContextRoleEntity();
        ucr.setRole(role);
        ucr.setContextKey(contextKey);
        return ucr;
    }

    private static UserEntity userWithAuthId() {
        UserEntity user = new UserEntity();
        user.setEmail("u@example.com");
        user.setAuthId("kc-user-1");
        return user;
    }

    @Test
    void hellodataAdminMapsToAirflowAdmin() {
        UserEntity user = userWithAuthId();
        when(roleService.getAllContextRolesForUser(user))
                .thenReturn(List.of(contextRole(HdRoleName.HELLODATA_ADMIN, null)));

        assertEquals(Set.of("airflow_admin"), service.computeDesiredRoles(user));
    }

    @Test
    void dataDomainAdminMapsToBasePlusDomainRole_viewerIgnored() {
        UserEntity user = userWithAuthId();
        when(roleService.getAllContextRolesForUser(user)).thenReturn(List.of(
                contextRole(HdRoleName.DATA_DOMAIN_ADMIN, "sales"),
                contextRole(HdRoleName.DATA_DOMAIN_VIEWER, "hr")));

        assertEquals(Set.of("airflow_base", "DD_sales"), service.computeDesiredRoles(user));
    }

    @Test
    void reconcileIsNoOpWhenDisabled() {
        service.reconcileUserAirflowRoles(userWithAuthId());
        verifyNoInteractions(keycloakService);
    }

    @Test
    void reconcileAddsDesiredCreatesMissingAndRemovesStaleManagedRoles() {
        ReflectionTestUtils.setField(service, "roleSyncEnabled", true);
        UserEntity user = userWithAuthId();
        when(roleService.getAllContextRolesForUser(user))
                .thenReturn(List.of(contextRole(HdRoleName.DATA_DOMAIN_ADMIN, "sales")));
        // Current: a stale managed domain role + a non-managed role that must be left alone.
        when(keycloakService.getRealmRoleNamesOfUser("kc-user-1"))
                .thenReturn(Set.of("DD_old", "some_other_role"));

        service.reconcileUserAirflowRoles(user);

        // desired = {airflow_base, DD_sales}; managed-current = {DD_old} -> add both, remove DD_old
        verify(keycloakService).createRealmRoleIfMissing("airflow_base");
        verify(keycloakService).createRealmRoleIfMissing("DD_sales");
        verify(keycloakService).addRealmRolesToUser("kc-user-1", Set.of("airflow_base", "DD_sales"));
        verify(keycloakService).removeRealmRolesFromUser("kc-user-1", Set.of("DD_old"));
    }
}
