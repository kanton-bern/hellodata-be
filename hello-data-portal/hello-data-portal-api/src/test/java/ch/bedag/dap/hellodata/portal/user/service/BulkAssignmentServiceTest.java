package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.dashboard_group.repository.DashboardGroupRepository;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.email.service.EmailNotificationService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.data.BulkAssignmentRequestDto;
import ch.bedag.dap.hellodata.portal.user.data.BulkAssignmentResultDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextsDto;
import ch.bedag.dap.hellodata.portal.user.data.UpdateContextRolesForUserDto;
import ch.bedag.dap.hellodata.portal.user.data.UserContextRoleDto;
import ch.bedag.dap.hellodata.portal.user.data.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BulkAssignmentServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private RoleService roleService;
    @Mock
    private DashboardGroupRepository dashboardGroupRepository;
    @Mock
    private DashboardGroupService dashboardGroupService;
    @Mock
    private UserSelectedDashboardService userSelectedDashboardService;
    @Mock
    private EmailNotificationService emailNotificationService;
    @InjectMocks
    private BulkAssignmentService bulkAssignmentService;

    private List<RoleDto> allRoles;
    private ContextsDto availableContexts;
    private static final String TEST_GROUP_ID = "00000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        allRoles = createAllRoles();
        availableContexts = createAvailableContexts();

        when(roleService.getAll()).thenReturn(allRoles);
        when(userService.getAvailableContexts()).thenReturn(availableContexts);
        when(dashboardGroupService.getDashboardGroupMembership(any(UUID.class), anyString())).thenReturn(List.of());
        when(userSelectedDashboardService.getSelectedDashboardIds(any(UUID.class), anyString())).thenReturn(Set.of());
        when(dashboardGroupRepository.existsById(any(UUID.class))).thenReturn(true);

        UserDto mockUser = new UserDto();
        mockUser.setEmail("test@example.com");
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");
        when(userService.getUserById(anyString())).thenReturn(mockUser);
        when(userService.findHelloDataAdminUsers()).thenReturn(List.of());
    }

    @Test
    void testSingleUserSingleDomain() {
        UUID userId = UUID.randomUUID();
        mockExistingRoles(userId, Map.of("domain_a", "NONE", "domain_b", "NONE"));

        BulkAssignmentRequestDto request = new BulkAssignmentRequestDto();
        request.setUserIds(List.of(userId));
        request.setDomainAssignments(List.of(
                createAssignment("domain_a", "DATA_DOMAIN_VIEWER", List.of(1, 2), List.of())
        ));

        BulkAssignmentResultDto result = bulkAssignmentService.executeBulkAssignment(request);

        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getFailedCount());

        ArgumentCaptor<UpdateContextRolesForUserDto> captor = ArgumentCaptor.forClass(UpdateContextRolesForUserDto.class);
        verify(userService).updateContextRolesForUser(eq(userId), captor.capture(), eq(true));

        UpdateContextRolesForUserDto dto = captor.getValue();
        assertEquals(2, dto.getDataDomainRoles().size());

        // domain_a should have the new role
        UserContextRoleDto domainA = findByContextKey(dto.getDataDomainRoles(), "domain_a");
        assertEquals("DATA_DOMAIN_VIEWER", domainA.getRole().getName());

        // domain_b should preserve existing (NONE)
        UserContextRoleDto domainB = findByContextKey(dto.getDataDomainRoles(), "domain_b");
        assertEquals("NONE", domainB.getRole().getName());
    }

    @Test
    void testMultipleUsersMultipleDomains() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        mockExistingRoles(user1, Map.of("domain_a", "NONE", "domain_b", "NONE"));
        mockExistingRoles(user2, Map.of("domain_a", "DATA_DOMAIN_EDITOR", "domain_b", "DATA_DOMAIN_ADMIN"));

        BulkAssignmentRequestDto request = new BulkAssignmentRequestDto();
        request.setUserIds(List.of(user1, user2));
        request.setDomainAssignments(List.of(
                createAssignment("domain_a", "DATA_DOMAIN_ADMIN", List.of(), List.of()),
                createAssignment("domain_b", "DATA_DOMAIN_VIEWER", List.of(5), List.of())
        ));

        BulkAssignmentResultDto result = bulkAssignmentService.executeBulkAssignment(request);

        assertEquals(2, result.getUpdatedCount());
        assertEquals(0, result.getFailedCount());
        verify(userService, times(2)).updateContextRolesForUser(any(UUID.class), any(), anyBoolean());
    }

    @Test
    void testPreservesExistingRolesForUnselectedDomains() {
        UUID userId = UUID.randomUUID();
        mockExistingRoles(userId, Map.of("domain_a", "DATA_DOMAIN_EDITOR", "domain_b", "DATA_DOMAIN_ADMIN"));

        BulkAssignmentRequestDto request = new BulkAssignmentRequestDto();
        request.setUserIds(List.of(userId));
        request.setDomainAssignments(List.of(
                createAssignment("domain_a", "DATA_DOMAIN_VIEWER", List.of(), List.of())
        ));

        bulkAssignmentService.executeBulkAssignment(request);

        ArgumentCaptor<UpdateContextRolesForUserDto> captor = ArgumentCaptor.forClass(UpdateContextRolesForUserDto.class);
        verify(userService).updateContextRolesForUser(eq(userId), captor.capture(), eq(true));

        UpdateContextRolesForUserDto dto = captor.getValue();
        UserContextRoleDto domainB = findByContextKey(dto.getDataDomainRoles(), "domain_b");
        assertEquals("DATA_DOMAIN_ADMIN", domainB.getRole().getName());
    }

    @Test
    void testDashboardSelectionsIncludedForViewerRole() {
        UUID userId = UUID.randomUUID();
        mockExistingRoles(userId, Map.of("domain_a", "NONE", "domain_b", "NONE"));

        BulkAssignmentRequestDto request = new BulkAssignmentRequestDto();
        request.setUserIds(List.of(userId));
        request.setDomainAssignments(List.of(
                createAssignment("domain_a", "DATA_DOMAIN_VIEWER", List.of(10, 20, 30), List.of(TEST_GROUP_ID))
        ));

        bulkAssignmentService.executeBulkAssignment(request);

        ArgumentCaptor<UpdateContextRolesForUserDto> captor = ArgumentCaptor.forClass(UpdateContextRolesForUserDto.class);
        verify(userService).updateContextRolesForUser(eq(userId), captor.capture(), anyBoolean());

        UpdateContextRolesForUserDto dto = captor.getValue();
        assertNotNull(dto.getSelectedDashboardsForUser().get("domain_a"));
        assertEquals(3, dto.getSelectedDashboardsForUser().get("domain_a").size());
        assertEquals(List.of(TEST_GROUP_ID), dto.getSelectedDashboardGroupIdsForUser().get("domain_a"));
    }

    @Test
    void testCommentPermissionsSetBasedOnRole() {
        UUID userId = UUID.randomUUID();
        mockExistingRoles(userId, Map.of("domain_a", "NONE", "domain_b", "NONE"));

        BulkAssignmentRequestDto request = new BulkAssignmentRequestDto();
        request.setUserIds(List.of(userId));
        request.setDomainAssignments(List.of(
                createAssignment("domain_a", "DATA_DOMAIN_ADMIN", List.of(), List.of())
        ));

        bulkAssignmentService.executeBulkAssignment(request);

        ArgumentCaptor<UpdateContextRolesForUserDto> captor = ArgumentCaptor.forClass(UpdateContextRolesForUserDto.class);
        verify(userService).updateContextRolesForUser(eq(userId), captor.capture(), anyBoolean());

        var perms = captor.getValue().getCommentPermissions();
        var domainAPerm = perms.stream().filter(p -> "domain_a".equals(p.getContextKey())).findFirst().orElseThrow();
        assertTrue(domainAPerm.isReadComments());
        assertTrue(domainAPerm.isWriteComments());
        assertTrue(domainAPerm.isReviewComments());
    }

    @Test
    void testFailedUserDoesNotBlockOthers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        mockExistingRoles(user1, Map.of("domain_a", "NONE", "domain_b", "NONE"));
        when(userService.getContextRolesForUser(user2)).thenThrow(new RuntimeException("User not found"));

        BulkAssignmentRequestDto request = new BulkAssignmentRequestDto();
        request.setUserIds(List.of(user1, user2));
        request.setDomainAssignments(List.of(
                createAssignment("domain_a", "DATA_DOMAIN_VIEWER", List.of(), List.of())
        ));

        BulkAssignmentResultDto result = bulkAssignmentService.executeBulkAssignment(request);

        assertEquals(1, result.getUpdatedCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    void testLastUserFlagSendsBackUserList() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        mockExistingRoles(user1, Map.of("domain_a", "NONE", "domain_b", "NONE"));
        mockExistingRoles(user2, Map.of("domain_a", "NONE", "domain_b", "NONE"));

        BulkAssignmentRequestDto request = new BulkAssignmentRequestDto();
        request.setUserIds(List.of(user1, user2));
        request.setDomainAssignments(List.of(
                createAssignment("domain_a", "DATA_DOMAIN_VIEWER", List.of(), List.of())
        ));

        bulkAssignmentService.executeBulkAssignment(request);

        // First user: sendBackUserList = false
        verify(userService).updateContextRolesForUser(eq(user1), any(), eq(false));
        // Last user: sendBackUserList = true
        verify(userService).updateContextRolesForUser(eq(user2), any(), eq(true));
    }

    @Test
    void testKeepsExistingBusinessDomainRole() {
        UUID userId = UUID.randomUUID();
        List<UserContextRoleDto> existingRoles = new ArrayList<>();
        existingRoles.add(createExistingRole("BUSINESS_DOMAIN_ADMIN", "business_ctx", HdContextType.BUSINESS_DOMAIN));
        existingRoles.add(createExistingRole("DATA_DOMAIN_ADMIN", "domain_a", HdContextType.DATA_DOMAIN));
        when(userService.getContextRolesForUser(userId)).thenReturn(existingRoles);

        BulkAssignmentRequestDto request = new BulkAssignmentRequestDto();
        request.setUserIds(List.of(userId));
        request.setDomainAssignments(List.of(
                createAssignment("domain_a", "DATA_DOMAIN_VIEWER", List.of(), List.of())
        ));

        bulkAssignmentService.executeBulkAssignment(request);

        ArgumentCaptor<UpdateContextRolesForUserDto> captor = ArgumentCaptor.forClass(UpdateContextRolesForUserDto.class);
        verify(userService).updateContextRolesForUser(eq(userId), captor.capture(), anyBoolean());

        assertEquals("BUSINESS_DOMAIN_ADMIN", captor.getValue().getBusinessDomainRole().getName());
    }

    @Test
    void testSkipsUserWhenAssignmentsAlreadyMatch() {
        UUID userId = UUID.randomUUID();
        mockExistingRoles(userId, Map.of("domain_a", "DATA_DOMAIN_VIEWER", "domain_b", "NONE"));

        BulkAssignmentRequestDto request = new BulkAssignmentRequestDto();
        request.setUserIds(List.of(userId));
        request.setDomainAssignments(List.of(
                createAssignment("domain_a", "DATA_DOMAIN_VIEWER", List.of(), List.of())
        ));

        BulkAssignmentResultDto result = bulkAssignmentService.executeBulkAssignment(request);

        assertEquals(0, result.getUpdatedCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(0, result.getFailedCount());
        verify(userService, never()).updateContextRolesForUser(any(), any(), anyBoolean());
    }

    // --- Helpers ---

    private void mockExistingRoles(UUID userId, Map<String, String> contextKeyToRole) {
        List<UserContextRoleDto> roles = new ArrayList<>();
        for (Map.Entry<String, String> entry : contextKeyToRole.entrySet()) {
            roles.add(createExistingRole(entry.getValue(), entry.getKey(), HdContextType.DATA_DOMAIN));
        }
        when(userService.getContextRolesForUser(userId)).thenReturn(roles);
    }

    private UserContextRoleDto createExistingRole(String roleName, String contextKey, HdContextType type) {
        UserContextRoleDto dto = new UserContextRoleDto();
        RoleDto role = new RoleDto();
        role.setId(UUID.randomUUID());
        role.setName(roleName);
        role.setContextType(type);
        dto.setRole(role);
        ContextDto ctx = new ContextDto();
        ctx.setContextKey(contextKey);
        ctx.setName(contextKey);
        ctx.setType(type);
        dto.setContext(ctx);
        return dto;
    }

    private BulkAssignmentRequestDto.DomainAssignment createAssignment(String contextKey, String roleName,
                                                                        List<Integer> dashboardIds, List<String> groupIds) {
        BulkAssignmentRequestDto.DomainAssignment a = new BulkAssignmentRequestDto.DomainAssignment();
        a.setContextKey(contextKey);
        a.setRoleName(roleName);
        if (dashboardIds != null) {
            List<BulkAssignmentRequestDto.DashboardInfo> dashboards = dashboardIds.stream().map(id -> {
                BulkAssignmentRequestDto.DashboardInfo info = new BulkAssignmentRequestDto.DashboardInfo();
                info.setId(id);
                info.setTitle("Dashboard " + id);
                info.setInstanceName("default-instance");
                return info;
            }).toList();
            a.setDashboards(dashboards);
        }
        a.setDashboardGroupIds(groupIds);
        return a;
    }

    private List<RoleDto> createAllRoles() {
        List<RoleDto> roles = new ArrayList<>();
        for (HdRoleName name : HdRoleName.values()) {
            RoleDto r = new RoleDto();
            r.setId(UUID.randomUUID());
            r.setName(name.name());
            r.setContextType(name.getContextType());
            roles.add(r);
        }
        return roles;
    }

    private ContextsDto createAvailableContexts() {
        ContextsDto dto = new ContextsDto();
        List<ContextDto> contexts = new ArrayList<>();
        contexts.add(createContextDto("domain_a", HdContextType.DATA_DOMAIN));
        contexts.add(createContextDto("domain_b", HdContextType.DATA_DOMAIN));
        contexts.add(createContextDto("business_ctx", HdContextType.BUSINESS_DOMAIN));
        dto.setContexts(contexts);
        return dto;
    }

    private ContextDto createContextDto(String key, HdContextType type) {
        ContextDto ctx = new ContextDto();
        ctx.setId(UUID.randomUUID());
        ctx.setContextKey(key);
        ctx.setName(key);
        ctx.setType(type);
        return ctx;
    }

    private UserContextRoleDto findByContextKey(List<UserContextRoleDto> roles, String contextKey) {
        return roles.stream()
                .filter(r -> contextKey.equals(r.getContext().getContextKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No role found for context " + contextKey));
    }
}
