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
package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.base.auth.HellodataAuthenticationConverter;
import ch.bedag.dap.hellodata.portal.dashboard_comment.service.DashboardCommentPermissionService;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.email.service.EmailNotificationService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.rls_role.service.RlsRoleService;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.data.ContextDto;
import ch.bedag.dap.hellodata.portal.user.data.DataDomainDto;
import ch.bedag.dap.hellodata.portal.user.data.UpdateContextRolesForUserDto;
import ch.bedag.dap.hellodata.portal.user.data.UserContextRoleDto;
import ch.bedag.dap.hellodata.portal.user.event.UserFullSyncEvent;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContextRoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HdContextRepository contextRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private DashboardCommentPermissionService dashboardCommentPermissionService;

    @Mock
    private DashboardGroupService dashboardGroupService;

    @Mock
    private UserSelectedDashboardService userSelectedDashboardService;

    @Mock
    private RlsRoleService rlsRoleService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private HellodataAuthenticationConverter authenticationConverter;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private UserContextRoleService userContextRoleService;

    @Test
    void testGetAvailableDataDomains() {
        // given
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);

        UserContextRoleEntity contextRole = new UserContextRoleEntity();
        RoleEntity role = new RoleEntity();
        role.setName(HdRoleName.DATA_DOMAIN_ADMIN);
        role.setContextType(HdContextType.DATA_DOMAIN);
        contextRole.setRole(role);
        contextRole.setContextKey("test-context");

        userEntity.setContextRoles(Set.of(contextRole));

        HdContextEntity contextEntity = new HdContextEntity();
        contextEntity.setId(UUID.randomUUID());
        contextEntity.setContextKey("test-context");
        contextEntity.setName("Test Context");

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);
        when(contextRepository.getByContextKey("test-context")).thenReturn(Optional.of(contextEntity));

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            // when
            List<DataDomainDto> result = userContextRoleService.getAvailableDataDomains();

            // then
            assertEquals(1, result.size());
            assertEquals("test-context", result.get(0).getKey());
            assertEquals("Test Context", result.get(0).getName());
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_roleChangedToAdmin_removesUserFromDashboardGroups() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "ctx1";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setContextRoles(Collections.emptySet());

        HdContextEntity dataDomain = new HdContextEntity();
        dataDomain.setContextKey(contextKey);

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        updateDto.setBusinessDomainRole(businessRole);
        updateDto.setSelectedDashboardsForUser(Collections.emptyMap());

        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey(contextKey);

        RoleDto dataDomainRole = new RoleDto();
        dataDomainRole.setName("DATA_DOMAIN_ADMIN"); // Not eligible for dashboard groups

        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
        userContextRoleDto.setContext(contextDto);
        userContextRoleDto.setRole(dataDomainRole);
        updateDto.setDataDomainRoles(List.of(userContextRoleDto));

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userContextRoleService.updateContextRolesForUser(userId, updateDto, false);

            // then - verify user was removed from dashboard groups in this domain
            verify(dashboardGroupService).removeUserFromDashboardGroupsInDomain(userId.toString(), contextKey);
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_roleChangedToViewer_doesNotRemoveUserFromDashboardGroups() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "ctx1";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setContextRoles(Collections.emptySet());

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        updateDto.setBusinessDomainRole(businessRole);
        updateDto.setSelectedDashboardsForUser(Collections.emptyMap());

        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey(contextKey);

        RoleDto dataDomainRole = new RoleDto();
        dataDomainRole.setName("DATA_DOMAIN_VIEWER"); // Eligible for dashboard groups

        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
        userContextRoleDto.setContext(contextDto);
        userContextRoleDto.setRole(dataDomainRole);
        updateDto.setDataDomainRoles(List.of(userContextRoleDto));

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userContextRoleService.updateContextRolesForUser(userId, updateDto, false);

            // then - verify user was NOT removed from dashboard groups
            verify(dashboardGroupService, never()).removeUserFromDashboardGroupsInDomain(anyString(), anyString());
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_roleChangedToBusinessSpecialist_doesNotRemoveUserFromDashboardGroups() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "ctx1";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setContextRoles(Collections.emptySet());

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        updateDto.setBusinessDomainRole(businessRole);
        updateDto.setSelectedDashboardsForUser(Collections.emptyMap());

        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey(contextKey);

        RoleDto dataDomainRole = new RoleDto();
        dataDomainRole.setName("DATA_DOMAIN_BUSINESS_SPECIALIST"); // Eligible for dashboard groups

        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
        userContextRoleDto.setContext(contextDto);
        userContextRoleDto.setRole(dataDomainRole);
        updateDto.setDataDomainRoles(List.of(userContextRoleDto));

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userContextRoleService.updateContextRolesForUser(userId, updateDto, false);

            // then - verify user was NOT removed from dashboard groups
            verify(dashboardGroupService, never()).removeUserFromDashboardGroupsInDomain(anyString(), anyString());
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_businessDomainRoleNotNone_removesUserFromAllDomains() {
        // given
        UUID userId = UUID.randomUUID();

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setContextRoles(Collections.emptySet());

        HdContextEntity dd1 = new HdContextEntity();
        dd1.setContextKey("dd1");
        HdContextEntity dd2 = new HdContextEntity();
        dd2.setContextKey("dd2");

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("BUSINESS_DOMAIN_ADMIN"); // Non-NONE business role: sets DATA_DOMAIN_ADMIN everywhere
        updateDto.setBusinessDomainRole(businessRole);
        updateDto.setSelectedDashboardsForUser(Collections.emptyMap());

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);
        when(contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN))).thenReturn(List.of(dd1, dd2));

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userContextRoleService.updateContextRolesForUser(userId, updateDto, false);

            // then - verify user was removed from dashboard groups in ALL domains
            verify(dashboardGroupService).removeUserFromDashboardGroupsInDomain(userId.toString(), "dd1");
            verify(dashboardGroupService).removeUserFromDashboardGroupsInDomain(userId.toString(), "dd2");
            verify(userSelectedDashboardService).removeAllForUser(userId);
            verify(rlsRoleService).removeAllForUser(userId);
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_roleChangedToEditor_removesUserFromDashboardGroups() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "ctx1";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setContextRoles(Collections.emptySet());

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        updateDto.setBusinessDomainRole(businessRole);
        updateDto.setSelectedDashboardsForUser(Collections.emptyMap());

        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey(contextKey);

        RoleDto dataDomainRole = new RoleDto();
        dataDomainRole.setName("DATA_DOMAIN_EDITOR"); // Not eligible for dashboard groups

        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
        userContextRoleDto.setContext(contextDto);
        userContextRoleDto.setRole(dataDomainRole);
        updateDto.setDataDomainRoles(List.of(userContextRoleDto));

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userContextRoleService.updateContextRolesForUser(userId, updateDto, false);

            // then - verify user was removed from dashboard groups in this domain
            verify(dashboardGroupService).removeUserFromDashboardGroupsInDomain(userId.toString(), contextKey);
            verify(userSelectedDashboardService).removeAllForUserInContext(userId, contextKey);
            verify(rlsRoleService).removeAllForUserInContext(userId, contextKey);
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_selectionForOneContext_fullSyncRebuildsDashboardsForAllContexts() {
        // given
        UUID userId = UUID.randomUUID();
        String editedContext = "ctx-edited";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setContextRoles(Collections.emptySet());

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        updateDto.setBusinessDomainRole(businessRole);

        DashboardForUserDto dashboardDto = new DashboardForUserDto();
        dashboardDto.setId(42);
        dashboardDto.setTitle("Sales Dashboard");
        dashboardDto.setInstanceName("superset-main");
        dashboardDto.setViewer(true);
        updateDto.setSelectedDashboardsForUser(Map.of(editedContext, List.of(dashboardDto)));

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userContextRoleService.updateContextRolesForUser(userId, updateDto, false);

            // then: direct selections must be persisted for the edited context
            verify(userSelectedDashboardService).saveSelectedDashboards(eq(userId), eq(editedContext), any());

            // and: the published UserFullSyncEvent must carry null dashboards
            ArgumentCaptor<UserFullSyncEvent> eventCaptor = ArgumentCaptor.forClass(UserFullSyncEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            UserFullSyncEvent publishedEvent = eventCaptor.getValue();
            assertEquals(userId, publishedEvent.userId());
            org.junit.jupiter.api.Assertions.assertNull(publishedEvent.dashboardsPerContext(),
                    "Event must have null dashboards so listener rebuilds for all contexts");
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_persistsRlsRolesOnlyForEligibleContexts() {
        // given
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setContextRoles(Set.of(mockContextRole("ctx-viewer", HdRoleName.DATA_DOMAIN_VIEWER),
                mockContextRole("ctx-editor", HdRoleName.DATA_DOMAIN_EDITOR)));

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        updateDto.setBusinessDomainRole(businessRole);
        updateDto.setSelectedRlsRolesForUser(Map.of("ctx-viewer", List.of("RLS_01", "RLS_02"), "ctx-editor", List.of("RLS_03")));

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userContextRoleService.updateContextRolesForUser(userId, updateDto, false);

            // then
            verify(rlsRoleService).saveSelectedRlsRoles(userId, "ctx-viewer", List.of("RLS_01", "RLS_02"));
            verify(rlsRoleService, never()).saveSelectedRlsRoles(eq(userId), eq("ctx-editor"), anyCollection());
            verify(rlsRoleService).removeAllForUserInContext(userId, "ctx-editor");
        }
    }

    private static UserContextRoleEntity mockContextRole(String contextKey, HdRoleName roleName) {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getName()).thenReturn(roleName);
        UserContextRoleEntity contextRole = mock(UserContextRoleEntity.class);
        when(contextRole.getContextKey()).thenReturn(contextKey);
        when(contextRole.getRole()).thenReturn(role);
        return contextRole;
    }
}
