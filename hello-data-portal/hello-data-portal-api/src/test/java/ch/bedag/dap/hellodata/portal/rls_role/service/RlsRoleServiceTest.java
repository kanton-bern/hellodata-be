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
package ch.bedag.dap.hellodata.portal.rls_role.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.rls_role.data.RlsRoleDto;
import ch.bedag.dap.hellodata.portal.rls_role.entity.RlsRoleNameEntity;
import ch.bedag.dap.hellodata.portal.rls_role.entity.UserRlsRoleEntity;
import ch.bedag.dap.hellodata.portal.rls_role.repository.RlsRoleNameRepository;
import ch.bedag.dap.hellodata.portal.rls_role.repository.UserRlsRoleRepository;
import ch.bedag.dap.hellodata.portal.user.data.DataDomainDto;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RlsRoleServiceTest {

    private static final String CONTEXT_KEY = "dd01";
    private static final String OTHER_CONTEXT_KEY = "dd02";

    @Mock
    private RlsRoleNameRepository rlsRoleNameRepository;
    @Mock
    private UserRlsRoleRepository userRlsRoleRepository;
    @Mock
    private DashboardGroupService dashboardGroupService;
    @Mock
    private HdContextRepository contextRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private RlsRoleService rlsRoleService;

    private MockedStatic<SecurityUtils> securityUtils;
    private final UUID currentUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void rlsRoleKeys_shouldContainFifteenRoles() {
        assertThat(RlsRoleService.RLS_ROLE_KEYS).hasSize(15).startsWith("RLS_01").endsWith("RLS_15");
        assertThat(RlsRoleService.isRlsRoleKey("RLS_07")).isTrue();
        assertThat(RlsRoleService.isRlsRoleKey("RLS_16")).isFalse();
        assertThat(RlsRoleService.isRlsRoleKey("D_dashboard_1")).isFalse();
    }

    @Test
    void getRlsRoles_shouldUseRoleKeyAsDefaultName() {
        when(rlsRoleNameRepository.findAllByContextKey(CONTEXT_KEY)).thenReturn(List.of(nameEntity("RLS_02", "Region Bern")));

        List<RlsRoleDto> result = rlsRoleService.getRlsRoles(CONTEXT_KEY);

        assertThat(result).hasSize(15);
        assertThat(result.get(0)).isEqualTo(new RlsRoleDto("RLS_01", "RLS_01"));
        assertThat(result.get(1)).isEqualTo(new RlsRoleDto("RLS_02", "Region Bern"));
    }

    @Test
    void updateRlsRoleNames_shouldStoreNamesAndDropDefaultOnes() {
        mockDataDomainAdmin();
        RlsRoleNameEntity existing = nameEntity("RLS_02", "Old name");
        when(rlsRoleNameRepository.findAllByContextKey(CONTEXT_KEY)).thenReturn(List.of(existing));

        rlsRoleService.updateRlsRoleNames(CONTEXT_KEY, List.of(new RlsRoleDto("RLS_01", " Region Bern "), new RlsRoleDto("RLS_02", "")));

        ArgumentCaptor<RlsRoleNameEntity> captor = ArgumentCaptor.forClass(RlsRoleNameEntity.class);
        verify(rlsRoleNameRepository).save(captor.capture());
        assertThat(captor.getValue().getRoleKey()).isEqualTo("RLS_01");
        assertThat(captor.getValue().getDisplayName()).isEqualTo("Region Bern");
        assertThat(captor.getValue().getContextKey()).isEqualTo(CONTEXT_KEY);
        verify(rlsRoleNameRepository).delete(existing);
    }

    @Test
    void updateRlsRoleNames_shouldRejectDataDomainAdminOfOtherDomain() {
        mockDataDomainAdmin();
        List<RlsRoleDto> rlsRoles = List.of(new RlsRoleDto("RLS_01", "Region Bern"));

        assertThatThrownBy(() -> rlsRoleService.updateRlsRoleNames(OTHER_CONTEXT_KEY, rlsRoles))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(rlsRoleNameRepository, never()).save(any());
    }

    @Test
    void updateRlsRoleNames_shouldRejectUnknownRoleKeysAndDuplicateNames() {
        mockDataDomainAdmin();
        List<RlsRoleDto> unknown = List.of(new RlsRoleDto("RLS_16", "Region Bern"));
        List<RlsRoleDto> duplicates = List.of(new RlsRoleDto("RLS_01", "Region"), new RlsRoleDto("RLS_02", "region"));

        assertThatThrownBy(() -> rlsRoleService.updateRlsRoleNames(CONTEXT_KEY, unknown)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> rlsRoleService.updateRlsRoleNames(CONTEXT_KEY, duplicates)).isInstanceOf(ResponseStatusException.class);
        verify(rlsRoleNameRepository, never()).save(any());
    }

    @Test
    void getManageableDataDomains_shouldReturnAllDomainsForSuperuser() {
        securityUtils.when(SecurityUtils::isSuperuser).thenReturn(true);
        when(userRepository.getByIdOrAuthId(currentUserId.toString())).thenReturn(mock(UserEntity.class));
        when(contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN))).thenReturn(List.of(context(CONTEXT_KEY, "B"), context(OTHER_CONTEXT_KEY, "A")));

        List<DataDomainDto> result = rlsRoleService.getManageableDataDomains();

        assertThat(result).extracting(DataDomainDto::getKey).containsExactly(OTHER_CONTEXT_KEY, CONTEXT_KEY);
    }

    @Test
    void getManageableDataDomains_shouldReturnOnlyOwnDomainsForDataDomainAdmin() {
        mockDataDomainAdmin();

        List<DataDomainDto> result = rlsRoleService.getManageableDataDomains();

        assertThat(result).extracting(DataDomainDto::getKey).containsExactly(CONTEXT_KEY);
    }

    @Test
    void saveSelectedRlsRoles_shouldReplaceSelectionAndIgnoreInvalidKeys() {
        UUID userId = UUID.randomUUID();

        rlsRoleService.saveSelectedRlsRoles(userId, CONTEXT_KEY, List.of("RLS_03", "RLS_01", "RLS_03", "Admin"));

        verify(userRlsRoleRepository).deleteAllByUserIdAndContextKey(userId, CONTEXT_KEY);
        ArgumentCaptor<UserRlsRoleEntity> captor = ArgumentCaptor.forClass(UserRlsRoleEntity.class);
        verify(userRlsRoleRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(UserRlsRoleEntity::getRoleKey).containsExactly("RLS_01", "RLS_03");
    }

    @Test
    void getEffectiveRlsRoleKeys_shouldMergeDirectAndGroupRoles() {
        UUID userId = UUID.randomUUID();
        UserRlsRoleEntity direct = new UserRlsRoleEntity();
        direct.setRoleKey("RLS_05");
        when(userRlsRoleRepository.findAllByUserIdAndContextKey(userId, CONTEXT_KEY)).thenReturn(List.of(direct));
        DashboardGroupEntity group = new DashboardGroupEntity();
        group.setRlsRoles(List.of("RLS_01", "RLS_05"));
        when(dashboardGroupService.findGroupsByContextKeyAndUserId(CONTEXT_KEY, userId.toString())).thenReturn(List.of(group));

        List<String> result = rlsRoleService.getEffectiveRlsRoleKeys(userId, CONTEXT_KEY);

        assertThat(result).containsExactly("RLS_01", "RLS_05");
    }

    private void mockDataDomainAdmin() {
        UserEntity currentUser = mock(UserEntity.class);
        when(currentUser.isBusinessDomainAdmin()).thenReturn(false);
        RoleEntity adminRole = mock(RoleEntity.class);
        when(adminRole.getName()).thenReturn(HdRoleName.DATA_DOMAIN_ADMIN);
        RoleEntity viewerRole = mock(RoleEntity.class);
        when(viewerRole.getName()).thenReturn(HdRoleName.DATA_DOMAIN_VIEWER);
        UserContextRoleEntity adminContextRole = mock(UserContextRoleEntity.class);
        lenient().when(adminContextRole.getContextKey()).thenReturn(CONTEXT_KEY);
        when(adminContextRole.getRole()).thenReturn(adminRole);
        UserContextRoleEntity viewerContextRole = mock(UserContextRoleEntity.class);
        lenient().when(viewerContextRole.getContextKey()).thenReturn(OTHER_CONTEXT_KEY);
        when(viewerContextRole.getRole()).thenReturn(viewerRole);
        when(currentUser.getContextRoles()).thenReturn(Set.of(adminContextRole, viewerContextRole));
        when(userRepository.getByIdOrAuthId(currentUserId.toString())).thenReturn(currentUser);
        lenient().when(contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN)))
                .thenReturn(List.of(context(CONTEXT_KEY, "Domain one"), context(OTHER_CONTEXT_KEY, "Domain two")));
    }

    private static HdContextEntity context(String contextKey, String name) {
        HdContextEntity context = new HdContextEntity();
        context.setContextKey(contextKey);
        context.setName(name);
        context.setType(HdContextType.DATA_DOMAIN);
        return context;
    }

    private static RlsRoleNameEntity nameEntity(String roleKey, String name) {
        RlsRoleNameEntity entity = new RlsRoleNameEntity();
        entity.setContextKey(CONTEXT_KEY);
        entity.setRoleKey(roleKey);
        entity.setDisplayName(name);
        return entity;
    }
}
