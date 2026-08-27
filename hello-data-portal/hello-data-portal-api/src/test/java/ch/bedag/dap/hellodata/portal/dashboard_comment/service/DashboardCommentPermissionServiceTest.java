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
package ch.bedag.dap.hellodata.portal.dashboard_comment.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentPermissionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentPermissionRepository;
import ch.bedag.dap.hellodata.portalcommon.role.entity.PortalRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.SystemDefaultPortalRoleName;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserPortalRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCommentPermissionServiceTest {

    private static final String CONTEXT_KEY = "dd-1";

    @Mock
    private DashboardCommentPermissionRepository repository;
    @Mock
    private HdContextRepository hdContextRepository;

    private DashboardCommentPermissionService service;

    @BeforeEach
    void setUp() {
        service = new DashboardCommentPermissionService(repository, hdContextRepository);
        HdContextEntity context = new HdContextEntity();
        context.setContextKey(CONTEXT_KEY);
        context.setType(HdContextType.DATA_DOMAIN);
        when(hdContextRepository.findAll()).thenReturn(List.of(context));
        when(repository.findByUserIdAndContextKey(any(), eq(CONTEXT_KEY))).thenReturn(Optional.empty());
    }

    @ParameterizedTest
    @EnumSource(value = HdRoleName.class, names = {"DATA_DOMAIN_EDITOR", "DATA_DOMAIN_VIEWER", "DATA_DOMAIN_BUSINESS_SPECIALIST", "NONE"})
    void syncDefaultPermissionsForUser_nonAdminRoles_getNoPermissions(HdRoleName roleName) {
        UserEntity user = createUser(roleName, null);

        service.syncDefaultPermissionsForUser(user);

        DashboardCommentPermissionEntity saved = captureSavedPermission();
        assertThat(saved.isReadComments()).isFalse();
        assertThat(saved.isWriteComments()).isFalse();
        assertThat(saved.isReviewComments()).isFalse();
    }

    @Test
    void syncDefaultPermissionsForUser_dataDomainAdmin_getsFullAccess() {
        UserEntity user = createUser(HdRoleName.DATA_DOMAIN_ADMIN, null);

        service.syncDefaultPermissionsForUser(user);

        assertFullAccess(captureSavedPermission());
    }

    @Test
    void syncDefaultPermissionsForUser_helloDataAdmin_getsFullAccess() {
        UserEntity user = createUser(HdRoleName.NONE, SystemDefaultPortalRoleName.HELLODATA_ADMIN);

        service.syncDefaultPermissionsForUser(user);

        assertFullAccess(captureSavedPermission());
    }

    @Test
    void syncDefaultPermissionsForUser_businessDomainAdmin_getsFullAccess() {
        UserEntity user = createUser(HdRoleName.NONE, SystemDefaultPortalRoleName.BUSINESS_DOMAIN_ADMIN);

        service.syncDefaultPermissionsForUser(user);

        assertFullAccess(captureSavedPermission());
    }

    private void assertFullAccess(DashboardCommentPermissionEntity saved) {
        assertThat(saved.isReadComments()).isTrue();
        assertThat(saved.isWriteComments()).isTrue();
        assertThat(saved.isReviewComments()).isTrue();
    }

    private DashboardCommentPermissionEntity captureSavedPermission() {
        ArgumentCaptor<DashboardCommentPermissionEntity> captor = ArgumentCaptor.forClass(DashboardCommentPermissionEntity.class);
        verify(repository).save(captor.capture());
        DashboardCommentPermissionEntity saved = captor.getValue();
        assertThat(saved.getContextKey()).isEqualTo(CONTEXT_KEY);
        return saved;
    }

    private UserEntity createUser(HdRoleName contextRoleName, SystemDefaultPortalRoleName portalRoleName) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");

        RoleEntity role = new RoleEntity();
        role.setName(contextRoleName);
        UserContextRoleEntity contextRole = new UserContextRoleEntity();
        contextRole.setRole(role);
        contextRole.setContextKey(CONTEXT_KEY);
        user.setContextRoles(Set.of(contextRole));

        if (portalRoleName != null) {
            PortalRoleEntity portalRole = new PortalRoleEntity();
            portalRole.setName(portalRoleName.name());
            UserPortalRoleEntity userPortalRole = new UserPortalRoleEntity();
            userPortalRole.setRole(portalRole);
            user.setPortalRoles(Set.of(userPortalRole));
        }
        return user;
    }
}
