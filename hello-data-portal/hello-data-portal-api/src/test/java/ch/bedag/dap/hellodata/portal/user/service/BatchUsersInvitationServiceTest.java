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

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.RoleResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.RolePermissions;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.ModuleRoleNames;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.csv.service.CsvParserService;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentPermissionDto;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.user.data.BatchUpdateContextRolesForUserDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextsDto;
import ch.bedag.dap.hellodata.portal.user.data.UserContextRoleDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchUsersInvitationServiceTest {

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Test
    void fetchDataFromFileTest() { //NOSONAR
        URL resource = getClass().getClassLoader().getResource("./csv/many_users");
        assertNotNull(resource, "The test resources directory should exist in the classpath");
        MetaInfoResourceEntity metaInfoResourceEntity = new MetaInfoResourceEntity();
        metaInfoResourceEntity.setContextKey("some_data_domain_key");
        metaInfoResourceEntity.setModuleType(ModuleType.SUPERSET);
        List<RolePermissions> existingRoles = List.of(
                new RolePermissions(1, "D_test_dashboard_6", List.of()),
                new RolePermissions(2, "D_example_dashboard_2", List.of()),
                new RolePermissions(3, "RLS_01", List.of()),
                new RolePermissions(3, "RLS_02", List.of()),
                new RolePermissions(3, "RLS_03", List.of()),
                new RolePermissions(3, "RLS_04", List.of()),
                new RolePermissions(3, "RLS_05", List.of()),
                new RolePermissions(3, "RLS_06", List.of()));
        RoleResource roleResource = new RoleResource("superset instance", ModuleType.SUPERSET, existingRoles);
        metaInfoResourceEntity.setMetainfo(roleResource);
        List<MetaInfoResourceEntity> metaInfoResourceEntities = List.of(metaInfoResourceEntity);
        when(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_ROLES)).thenReturn(metaInfoResourceEntities);

        String testResourcesPath = new File(resource.getFile()).getAbsolutePath();
        BatchUsersInvitationService batchUsersInvitationService1 = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, testResourcesPath);

        ContextsDto availableContexts = new ContextsDto();
        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey("some_data_domain_key");
        contextDto.setName("Some Data Domain");
        contextDto.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(contextDto));
        List<BatchUpdateContextRolesForUserDto> parsedUsers = batchUsersInvitationService1.fetchDataFromFile(false, availableContexts);

        assertEquals(7, parsedUsers.size());

        //First user
        BatchUpdateContextRolesForUserDto batchUpdateContextRolesForUserDto = parsedUsers.get(0);
        assertEquals("john.doe@example.com", batchUpdateContextRolesForUserDto.getEmail());

        assertEquals("NONE", batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName());

        assertEquals(1, batchUpdateContextRolesForUserDto.getDataDomainRoles().size());
        assertEquals("DATA_DOMAIN_VIEWER", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName());
        assertEquals("some_data_domain_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey());

        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());

        //Second user
        batchUpdateContextRolesForUserDto = parsedUsers.get(1);
        assertEquals("jane.smith@example.com", batchUpdateContextRolesForUserDto.getEmail());

        assertEquals("NONE", batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName());

        assertEquals(1, batchUpdateContextRolesForUserDto.getDataDomainRoles().size());
        assertEquals("DATA_DOMAIN_VIEWER", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName());
        assertEquals("some_data_domain_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey());

        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());

        //Fourth user
        batchUpdateContextRolesForUserDto = parsedUsers.get(3);
        assertEquals("bob.williams@example.com", batchUpdateContextRolesForUserDto.getEmail());

        assertEquals("NONE", batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName());

        assertEquals(1, batchUpdateContextRolesForUserDto.getDataDomainRoles().size());
        assertEquals("DATA_DOMAIN_ADMIN", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName());
        assertEquals("some_data_domain_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey());

        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());

        //Sixth user
        batchUpdateContextRolesForUserDto = parsedUsers.get(5);
        assertEquals("laura.anderson@example.com", batchUpdateContextRolesForUserDto.getEmail());

        assertEquals("HELLODATA_ADMIN", batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName());

        assertEquals(1, batchUpdateContextRolesForUserDto.getDataDomainRoles().size());
        assertEquals("DATA_DOMAIN_ADMIN", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName());
        assertEquals("some_data_domain_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey());

        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());
    }

    @Test
    void fetchDataFromFile_test_multiple_rows_for_one_user() {
        URL resource = getClass().getClassLoader().getResource("./csv/one_user");
        assertNotNull(resource, "The test resources directory should exist in the classpath");
        MetaInfoResourceEntity metaInfoResourceEntity = new MetaInfoResourceEntity();
        metaInfoResourceEntity.setContextKey("data_domain_one_key");
        metaInfoResourceEntity.setModuleType(ModuleType.SUPERSET);

        MetaInfoResourceEntity metaInfoResourceEntity1 = new MetaInfoResourceEntity();
        metaInfoResourceEntity1.setContextKey("data_domain_two_key");
        metaInfoResourceEntity1.setModuleType(ModuleType.SUPERSET);


        List<RolePermissions> existingRoles = List.of(
                new RolePermissions(1, "D_test_dashboard_6", List.of()),
                new RolePermissions(2, "D_example_dashboard_2", List.of()),
                new RolePermissions(3, "RLS_01", List.of()),
                new RolePermissions(3, "RLS_02", List.of()),
                new RolePermissions(3, "RLS_03", List.of()),
                new RolePermissions(3, "RLS_04", List.of()),
                new RolePermissions(3, "RLS_05", List.of()),
                new RolePermissions(3, "RLS_06", List.of()));
        RoleResource roleResource = new RoleResource("superset instance1", ModuleType.SUPERSET, existingRoles);
        metaInfoResourceEntity.setMetainfo(roleResource);

        List<RolePermissions> existingRoles1 = List.of(
                new RolePermissions(1, "D_test_dashboard_6", List.of()),
                new RolePermissions(2, "D_example_dashboard_2", List.of()),
                new RolePermissions(3, "RLS_01", List.of()),
                new RolePermissions(3, "RLS_02", List.of()),
                new RolePermissions(3, "RLS_03", List.of()),
                new RolePermissions(3, "RLS_04", List.of()),
                new RolePermissions(3, "RLS_05", List.of()),
                new RolePermissions(3, "RLS_06", List.of()));
        RoleResource roleResource1 = new RoleResource("superset instance2", ModuleType.SUPERSET, existingRoles1);
        metaInfoResourceEntity1.setMetainfo(roleResource1);

        List<MetaInfoResourceEntity> metaInfoResourceEntities = List.of(metaInfoResourceEntity, metaInfoResourceEntity1);
        when(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_ROLES)).thenReturn(metaInfoResourceEntities);

        String testResourcesPath = new File(resource.getFile()).getAbsolutePath();
        BatchUsersInvitationService batchUsersInvitationService1 = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, testResourcesPath);

        ContextsDto availableContexts = new ContextsDto();
        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey("data_domain_one_key");
        contextDto.setName("Some Data Domain");
        contextDto.setType(HdContextType.DATA_DOMAIN);
        ContextDto contextDto1 = new ContextDto();
        contextDto1.setContextKey("data_domain_two_key");
        contextDto1.setName("Some Data Domain");
        contextDto1.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(contextDto, contextDto1));

        List<BatchUpdateContextRolesForUserDto> parsedUsers = batchUsersInvitationService1.fetchDataFromFile(false, availableContexts);

        assertEquals(1, parsedUsers.size());

        //First user
        BatchUpdateContextRolesForUserDto batchUpdateContextRolesForUserDto = parsedUsers.get(0);
        assertEquals("john.doe@example.com", batchUpdateContextRolesForUserDto.getEmail());

        assertEquals("NONE", batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName());

        assertEquals(2, batchUpdateContextRolesForUserDto.getDataDomainRoles().size());
        assertEquals("DATA_DOMAIN_VIEWER", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName());
        assertEquals("data_domain_one_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey());

        assertEquals("DATA_DOMAIN_ADMIN", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(1).getRole().getName());
        assertEquals("data_domain_two_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(1).getContext().getContextKey());

        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());

    }

    @Test
    void buildDefaultCommentPermissions_forHelloDataAdmin_shouldGrantFullAccessToAllDomains() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("admin@example.com");
        RoleDto businessRole = new RoleDto();
        businessRole.setName("HELLODATA_ADMIN");
        user.setBusinessDomainRole(businessRole);
        user.setDataDomainRoles(new ArrayList<>());

        ContextsDto availableContexts = new ContextsDto();
        ContextDto context1 = new ContextDto();
        context1.setContextKey("dd1");
        context1.setType(HdContextType.DATA_DOMAIN);
        ContextDto context2 = new ContextDto();
        context2.setContextKey("dd2");
        context2.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(context1, context2));

        // Act - use reflection to call private method
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "buildDefaultCommentPermissions", BatchUpdateContextRolesForUserDto.class, ContextsDto.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<DashboardCommentPermissionDto> result = (List<DashboardCommentPermissionDto>) method.invoke(service, user, availableContexts);

        // Assert
        assertEquals(2, result.size());
        for (DashboardCommentPermissionDto perm : result) {
            assertTrue(perm.isReadComments(), "HELLODATA_ADMIN should have read access");
            assertTrue(perm.isWriteComments(), "HELLODATA_ADMIN should have write access");
            assertTrue(perm.isReviewComments(), "HELLODATA_ADMIN should have review access");
        }
    }

    @Test
    void buildDefaultCommentPermissions_forDataDomainViewer_shouldGrantReadOnlyAccess() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("viewer@example.com");
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        user.setBusinessDomainRole(businessRole);

        UserContextRoleDto ddRole = new UserContextRoleDto();
        RoleDto viewerRole = new RoleDto();
        viewerRole.setName("DATA_DOMAIN_VIEWER");
        ddRole.setRole(viewerRole);
        ContextDto ddContext = new ContextDto();
        ddContext.setContextKey("dd1");
        ddRole.setContext(ddContext);
        user.setDataDomainRoles(List.of(ddRole));

        ContextsDto availableContexts = new ContextsDto();
        ContextDto context1 = new ContextDto();
        context1.setContextKey("dd1");
        context1.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(context1));

        // Act
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "buildDefaultCommentPermissions", BatchUpdateContextRolesForUserDto.class, ContextsDto.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<DashboardCommentPermissionDto> result = (List<DashboardCommentPermissionDto>) method.invoke(service, user, availableContexts);

        // Assert
        assertEquals(1, result.size());
        DashboardCommentPermissionDto perm = result.get(0);
        assertEquals("dd1", perm.getContextKey());
        assertTrue(perm.isReadComments(), "DATA_DOMAIN_VIEWER should have read access");
        assertFalse(perm.isWriteComments(), "DATA_DOMAIN_VIEWER should NOT have write access");
        assertFalse(perm.isReviewComments(), "DATA_DOMAIN_VIEWER should NOT have review access");
    }

    @Test
    void buildDefaultCommentPermissions_forDataDomainEditor_shouldGrantReadWriteAccess() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("editor@example.com");
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        user.setBusinessDomainRole(businessRole);

        UserContextRoleDto ddRole = new UserContextRoleDto();
        RoleDto editorRole = new RoleDto();
        editorRole.setName("DATA_DOMAIN_EDITOR");
        ddRole.setRole(editorRole);
        ContextDto ddContext = new ContextDto();
        ddContext.setContextKey("dd1");
        ddRole.setContext(ddContext);
        user.setDataDomainRoles(List.of(ddRole));

        ContextsDto availableContexts = new ContextsDto();
        ContextDto context1 = new ContextDto();
        context1.setContextKey("dd1");
        context1.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(context1));

        // Act
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "buildDefaultCommentPermissions", BatchUpdateContextRolesForUserDto.class, ContextsDto.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<DashboardCommentPermissionDto> result = (List<DashboardCommentPermissionDto>) method.invoke(service, user, availableContexts);

        // Assert
        assertEquals(1, result.size());
        DashboardCommentPermissionDto perm = result.get(0);
        assertEquals("dd1", perm.getContextKey());
        assertTrue(perm.isReadComments(), "DATA_DOMAIN_EDITOR should have read access");
        assertTrue(perm.isWriteComments(), "DATA_DOMAIN_EDITOR should have write access");
        assertFalse(perm.isReviewComments(), "DATA_DOMAIN_EDITOR should NOT have review access");
    }

    @Test
    void buildDefaultCommentPermissions_forDataDomainAdmin_shouldGrantFullAccess() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("ddadmin@example.com");
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        user.setBusinessDomainRole(businessRole);

        UserContextRoleDto ddRole = new UserContextRoleDto();
        RoleDto adminRole = new RoleDto();
        adminRole.setName("DATA_DOMAIN_ADMIN");
        ddRole.setRole(adminRole);
        ContextDto ddContext = new ContextDto();
        ddContext.setContextKey("dd1");
        ddRole.setContext(ddContext);
        user.setDataDomainRoles(List.of(ddRole));

        ContextsDto availableContexts = new ContextsDto();
        ContextDto context1 = new ContextDto();
        context1.setContextKey("dd1");
        context1.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(context1));

        // Act
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "buildDefaultCommentPermissions", BatchUpdateContextRolesForUserDto.class, ContextsDto.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<DashboardCommentPermissionDto> result = (List<DashboardCommentPermissionDto>) method.invoke(service, user, availableContexts);

        // Assert
        assertEquals(1, result.size());
        DashboardCommentPermissionDto perm = result.get(0);
        assertEquals("dd1", perm.getContextKey());
        assertTrue(perm.isReadComments(), "DATA_DOMAIN_ADMIN should have read access");
        assertTrue(perm.isWriteComments(), "DATA_DOMAIN_ADMIN should have write access");
        assertTrue(perm.isReviewComments(), "DATA_DOMAIN_ADMIN should have review access");
    }

    @Test
    void mapSupersetRolesToDashboards_forViewerWithMatchingRoles_shouldMapDashboards() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("viewer@example.com");
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        user.setBusinessDomainRole(businessRole);

        UserContextRoleDto ddRole = new UserContextRoleDto();
        RoleDto viewerRole = new RoleDto();
        viewerRole.setName("DATA_DOMAIN_VIEWER");
        ddRole.setRole(viewerRole);
        ContextDto ddContext = new ContextDto();
        ddContext.setContextKey("dd1");
        ddRole.setContext(ddContext);
        user.setDataDomainRoles(List.of(ddRole));

        // Set up module role names with D_ prefixed roles
        Map<String, List<ModuleRoleNames>> contextToModuleRoles = new HashMap<>();
        contextToModuleRoles.put("dd1", List.of(new ModuleRoleNames(ModuleType.SUPERSET, List.of("D_dashboard1", "D_dashboard2", "RLS_01"))));
        user.setContextToModuleRoleNamesMap(contextToModuleRoles);

        // Create dashboards with matching roles
        Map<String, List<SupersetDashboard>> contextToDashboards = new HashMap<>();
        List<SupersetDashboard> dashboards = new ArrayList<>();

        SupersetDashboard dashboard1 = new SupersetDashboard();
        dashboard1.setId(1);
        dashboard1.setDashboardTitle("Dashboard 1");
        dashboard1.setPublished(true);
        SubsystemRole role1 = new SubsystemRole();
        role1.setName("D_dashboard1");
        dashboard1.setRoles(List.of(role1));
        dashboards.add(dashboard1);

        SupersetDashboard dashboard2 = new SupersetDashboard();
        dashboard2.setId(2);
        dashboard2.setDashboardTitle("Dashboard 2");
        dashboard2.setPublished(true);
        SubsystemRole role2 = new SubsystemRole();
        role2.setName("D_dashboard2");
        dashboard2.setRoles(List.of(role2));
        dashboards.add(dashboard2);

        SupersetDashboard dashboard3 = new SupersetDashboard();
        dashboard3.setId(3);
        dashboard3.setDashboardTitle("Dashboard 3 - No Match");
        dashboard3.setPublished(true);
        SubsystemRole role3 = new SubsystemRole();
        role3.setName("D_other_dashboard");
        dashboard3.setRoles(List.of(role3));
        dashboards.add(dashboard3);

        contextToDashboards.put("dd1", dashboards);

        // Act
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "mapSupersetRolesToDashboards", BatchUpdateContextRolesForUserDto.class, Map.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, List<DashboardForUserDto>> result =
                (Map<String, List<DashboardForUserDto>>) method.invoke(service, user, contextToDashboards);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("dd1"));
        List<DashboardForUserDto> assignedDashboards = result.get("dd1");
        assertEquals(2, assignedDashboards.size(), "Should only assign 2 dashboards that match the D_ roles");

        assertTrue(assignedDashboards.stream().anyMatch(d -> d.getId() == 1), "Dashboard 1 should be assigned");
        assertTrue(assignedDashboards.stream().anyMatch(d -> d.getId() == 2), "Dashboard 2 should be assigned");
        assertFalse(assignedDashboards.stream().anyMatch(d -> d.getId() == 3), "Dashboard 3 should NOT be assigned");

        // Verify isViewer is set to true
        for (DashboardForUserDto dto : assignedDashboards) {
            assertTrue(dto.isViewer(), "isViewer should be true for assigned dashboards");
        }
    }

    @Test
    void mapSupersetRolesToDashboards_forAdmin_shouldNotMapDashboards() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("admin@example.com");
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        user.setBusinessDomainRole(businessRole);

        // User is DATA_DOMAIN_ADMIN - should NOT get dashboard assignments
        UserContextRoleDto ddRole = new UserContextRoleDto();
        RoleDto adminRole = new RoleDto();
        adminRole.setName("DATA_DOMAIN_ADMIN");
        ddRole.setRole(adminRole);
        ContextDto ddContext = new ContextDto();
        ddContext.setContextKey("dd1");
        ddRole.setContext(ddContext);
        user.setDataDomainRoles(List.of(ddRole));

        // Even with module role names, admin should not get dashboard assignments
        Map<String, List<ModuleRoleNames>> contextToModuleRoles = new HashMap<>();
        contextToModuleRoles.put("dd1", List.of(new ModuleRoleNames(ModuleType.SUPERSET, List.of("D_dashboard1"))));
        user.setContextToModuleRoleNamesMap(contextToModuleRoles);

        // Create dashboards
        Map<String, List<SupersetDashboard>> contextToDashboards = new HashMap<>();
        SupersetDashboard dashboard1 = new SupersetDashboard();
        dashboard1.setId(1);
        dashboard1.setDashboardTitle("Dashboard 1");
        dashboard1.setPublished(true);
        SubsystemRole role1 = new SubsystemRole();
        role1.setName("D_dashboard1");
        dashboard1.setRoles(List.of(role1));
        contextToDashboards.put("dd1", List.of(dashboard1));

        // Act
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "mapSupersetRolesToDashboards", BatchUpdateContextRolesForUserDto.class, Map.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, List<DashboardForUserDto>> result =
                (Map<String, List<DashboardForUserDto>>) method.invoke(service, user, contextToDashboards);

        // Assert - Admin should have empty dashboard assignments
        assertTrue(result.isEmpty(), "DATA_DOMAIN_ADMIN should not get dashboard assignments via batch import");
    }

    @Test
    void mapSupersetRolesToDashboards_forBusinessSpecialist_shouldMapDashboards() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("specialist@example.com");
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        user.setBusinessDomainRole(businessRole);

        // User is DATA_DOMAIN_BUSINESS_SPECIALIST - should get dashboard assignments
        UserContextRoleDto ddRole = new UserContextRoleDto();
        RoleDto specialistRole = new RoleDto();
        specialistRole.setName("DATA_DOMAIN_BUSINESS_SPECIALIST");
        ddRole.setRole(specialistRole);
        ContextDto ddContext = new ContextDto();
        ddContext.setContextKey("dd1");
        ddRole.setContext(ddContext);
        user.setDataDomainRoles(List.of(ddRole));

        Map<String, List<ModuleRoleNames>> contextToModuleRoles = new HashMap<>();
        contextToModuleRoles.put("dd1", List.of(new ModuleRoleNames(ModuleType.SUPERSET, List.of("D_dashboard1"))));
        user.setContextToModuleRoleNamesMap(contextToModuleRoles);

        // Create dashboards
        Map<String, List<SupersetDashboard>> contextToDashboards = new HashMap<>();
        SupersetDashboard dashboard1 = new SupersetDashboard();
        dashboard1.setId(1);
        dashboard1.setDashboardTitle("Dashboard 1");
        dashboard1.setPublished(true);
        SubsystemRole role1 = new SubsystemRole();
        role1.setName("D_dashboard1");
        dashboard1.setRoles(List.of(role1));
        contextToDashboards.put("dd1", List.of(dashboard1));

        // Act
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "mapSupersetRolesToDashboards", BatchUpdateContextRolesForUserDto.class, Map.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, List<DashboardForUserDto>> result =
                (Map<String, List<DashboardForUserDto>>) method.invoke(service, user, contextToDashboards);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("dd1"));
        assertEquals(1, result.get("dd1").size(), "BUSINESS_SPECIALIST should get dashboard assignments");
    }
}