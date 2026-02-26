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
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchUsersInvitationServiceTest {

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Mock
    private DashboardGroupService dashboardGroupService;

    private DashboardGroupEntity createDashboardGroupEntity(String name, String contextKey) {
        DashboardGroupEntity entity = new DashboardGroupEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setContextKey(contextKey);
        return entity;
    }

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

        // Set up dashboard group mocks for the CSV with dashboardGroup column
        DashboardGroupEntity groupA = createDashboardGroupEntity("GroupA", "some_data_domain_key");
        DashboardGroupEntity groupB = createDashboardGroupEntity("GroupB", "some_data_domain_key");
        when(dashboardGroupService.findAllGroupsByContextKey("some_data_domain_key")).thenReturn(List.of(groupA, groupB));

        String testResourcesPath = new File(resource.getFile()).getAbsolutePath();
        BatchUsersInvitationService batchUsersInvitationService1 = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, testResourcesPath);

        ContextsDto availableContexts = new ContextsDto();
        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey("some_data_domain_key");
        contextDto.setName("Some Data Domain");
        contextDto.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(contextDto));
        List<BatchUpdateContextRolesForUserDto> parsedUsers = batchUsersInvitationService1.fetchDataFromFile(false, availableContexts);

        assertEquals(7, parsedUsers.size());

        //First user - john.doe with GroupA,GroupB
        BatchUpdateContextRolesForUserDto batchUpdateContextRolesForUserDto = parsedUsers.get(0);
        assertEquals("john.doe@example.com", batchUpdateContextRolesForUserDto.getEmail());
        assertEquals("NONE", batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName());
        assertEquals(1, batchUpdateContextRolesForUserDto.getDataDomainRoles().size());
        assertEquals("DATA_DOMAIN_VIEWER", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName());
        assertEquals("some_data_domain_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey());
        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());
        // Verify dashboard group names from CSV
        assertNotNull(batchUpdateContextRolesForUserDto.getDashboardGroupNamesFromCsv());
        assertEquals(List.of("GroupA", "GroupB"), batchUpdateContextRolesForUserDto.getDashboardGroupNamesFromCsv().get("some_data_domain_key"));

        //Second user - jane.smith with GroupA
        batchUpdateContextRolesForUserDto = parsedUsers.get(1);
        assertEquals("jane.smith@example.com", batchUpdateContextRolesForUserDto.getEmail());
        assertEquals("NONE", batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName());
        assertEquals(1, batchUpdateContextRolesForUserDto.getDataDomainRoles().size());
        assertEquals("DATA_DOMAIN_VIEWER", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName());
        assertEquals("some_data_domain_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey());
        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());
        assertEquals(List.of("GroupA"), batchUpdateContextRolesForUserDto.getDashboardGroupNamesFromCsv().get("some_data_domain_key"));

        //Fourth user - bob.williams (admin, no groups)
        batchUpdateContextRolesForUserDto = parsedUsers.get(3);
        assertEquals("bob.williams@example.com", batchUpdateContextRolesForUserDto.getEmail());
        assertEquals("NONE", batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName());
        assertEquals(1, batchUpdateContextRolesForUserDto.getDataDomainRoles().size());
        assertEquals("DATA_DOMAIN_ADMIN", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName());
        assertEquals("some_data_domain_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey());
        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());
        assertTrue(batchUpdateContextRolesForUserDto.getDashboardGroupNamesFromCsv().isEmpty()
                || !batchUpdateContextRolesForUserDto.getDashboardGroupNamesFromCsv().containsKey("some_data_domain_key")
                || batchUpdateContextRolesForUserDto.getDashboardGroupNamesFromCsv().get("some_data_domain_key").isEmpty());

        //Sixth user - laura.anderson (HELLODATA_ADMIN)
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

        // Set up dashboard group mocks - only data_domain_one_key has groups in CSV
        DashboardGroupEntity groupA = createDashboardGroupEntity("GroupA", "data_domain_one_key");
        when(dashboardGroupService.findAllGroupsByContextKey("data_domain_one_key")).thenReturn(List.of(groupA));

        String testResourcesPath = new File(resource.getFile()).getAbsolutePath();
        BatchUsersInvitationService batchUsersInvitationService1 = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, testResourcesPath);

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

        // Verify dashboard groups - GroupA assigned for data_domain_one_key
        assertNotNull(batchUpdateContextRolesForUserDto.getDashboardGroupNamesFromCsv());
        assertEquals(List.of("GroupA"), batchUpdateContextRolesForUserDto.getDashboardGroupNamesFromCsv().get("data_domain_one_key"));
    }

    @Test
    void fetchDataFromFile_oldFormatWithoutDashboardGroupColumn() {
        URL resource = getClass().getClassLoader().getResource("./csv/old_format");
        assertNotNull(resource, "The test resources directory should exist in the classpath");
        MetaInfoResourceEntity metaInfoResourceEntity = new MetaInfoResourceEntity();
        metaInfoResourceEntity.setContextKey("some_data_domain_key");
        metaInfoResourceEntity.setModuleType(ModuleType.SUPERSET);
        List<RolePermissions> existingRoles = List.of(
                new RolePermissions(1, "D_test_dashboard_6", List.of()),
                new RolePermissions(2, "D_example_dashboard_2", List.of()),
                new RolePermissions(3, "RLS_01", List.of()));
        RoleResource roleResource = new RoleResource("superset instance", ModuleType.SUPERSET, existingRoles);
        metaInfoResourceEntity.setMetainfo(roleResource);
        when(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_ROLES)).thenReturn(List.of(metaInfoResourceEntity));

        String testResourcesPath = new File(resource.getFile()).getAbsolutePath();
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, testResourcesPath);

        ContextsDto availableContexts = new ContextsDto();
        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey("some_data_domain_key");
        contextDto.setName("Some Data Domain");
        contextDto.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(contextDto));

        List<BatchUpdateContextRolesForUserDto> parsedUsers = service.fetchDataFromFile(false, availableContexts);

        assertEquals(2, parsedUsers.size());

        // Verify old format works - no dashboard groups
        BatchUpdateContextRolesForUserDto viewer = parsedUsers.get(0);
        assertEquals("john.doe@example.com", viewer.getEmail());
        assertTrue(viewer.getDashboardGroupNamesFromCsv().isEmpty(), "Old format should have no dashboard groups");

        BatchUpdateContextRolesForUserDto admin = parsedUsers.get(1);
        assertEquals("bob.williams@example.com", admin.getEmail());
        assertTrue(admin.getDashboardGroupNamesFromCsv().isEmpty(), "Old format should have no dashboard groups");
    }

    @Test
    void fetchDataFromFile_invalidDashboardGroupName_shouldThrowException() {
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
        when(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_ROLES)).thenReturn(List.of(metaInfoResourceEntity));

        // Return only GroupA - GroupB does not exist, which should cause validation failure
        DashboardGroupEntity groupA = createDashboardGroupEntity("GroupA", "some_data_domain_key");
        when(dashboardGroupService.findAllGroupsByContextKey("some_data_domain_key")).thenReturn(List.of(groupA));

        String testResourcesPath = new File(resource.getFile()).getAbsolutePath();
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, testResourcesPath);

        ContextsDto availableContexts = new ContextsDto();
        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey("some_data_domain_key");
        contextDto.setName("Some Data Domain");
        contextDto.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(contextDto));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.fetchDataFromFile(false, availableContexts),
                "Should throw exception when dashboard group name does not exist");
        // Verify the error message contains details about the missing group
        assertTrue(exception.getMessage().contains("GroupB"), "Error should mention the missing group 'GroupB'");
        assertTrue(exception.getMessage().contains("some_data_domain_key"), "Error should mention the context key");
    }

    @Test
    void fetchDataFromFile_multipleValidationErrors_shouldCollectAllErrors() {
        URL resource = getClass().getClassLoader().getResource("./csv/invalid_mixed");
        assertNotNull(resource, "The test resources directory should exist in the classpath");

        MetaInfoResourceEntity metaInfoResourceEntity = new MetaInfoResourceEntity();
        metaInfoResourceEntity.setContextKey("some_data_domain_key");
        metaInfoResourceEntity.setModuleType(ModuleType.SUPERSET);
        List<RolePermissions> existingRoles = List.of(
                new RolePermissions(1, "D_test_dashboard_6", List.of()));
        RoleResource roleResource = new RoleResource("superset instance", ModuleType.SUPERSET, existingRoles);
        metaInfoResourceEntity.setMetainfo(roleResource);
        when(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_ROLES))
                .thenReturn(List.of(metaInfoResourceEntity));

        // Only GroupA exists - MissingGroup does not
        DashboardGroupEntity groupA = createDashboardGroupEntity("GroupA", "some_data_domain_key");
        when(dashboardGroupService.findAllGroupsByContextKey("some_data_domain_key")).thenReturn(List.of(groupA));

        String testResourcesPath = new File(resource.getFile()).getAbsolutePath();
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, testResourcesPath);

        ContextsDto availableContexts = new ContextsDto();
        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey("some_data_domain_key");
        contextDto.setName("Some Data Domain");
        contextDto.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(contextDto));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.fetchDataFromFile(false, availableContexts));

        String errorMessage = exception.getMessage();
        // Should contain context key error for wrong_context_key
        assertTrue(errorMessage.contains("wrong_context_key"),
                "Error should mention invalid context key 'wrong_context_key'. Actual: " + errorMessage);
        // Should contain superset role error for WRONG_ROLE
        assertTrue(errorMessage.contains("WRONG_ROLE"),
                "Error should mention invalid superset role 'WRONG_ROLE'. Actual: " + errorMessage);
        // Should contain dashboard group error for MissingGroup
        assertTrue(errorMessage.contains("MissingGroup"),
                "Error should mention missing dashboard group 'MissingGroup'. Actual: " + errorMessage);
        // Should report multiple errors at once
        assertTrue(errorMessage.contains("CSV validation failed"),
                "Error should start with CSV validation failed prefix. Actual: " + errorMessage);
    }

    @Test
    void buildDefaultCommentPermissions_forHelloDataAdmin_shouldGrantFullAccessToAllDomains() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

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
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

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
    void buildDefaultCommentPermissions_forDataDomainEditor_shouldGrantReadOnlyAccess() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

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
        assertFalse(perm.isWriteComments(), "DATA_DOMAIN_EDITOR should NOT have write access");
        assertFalse(perm.isReviewComments(), "DATA_DOMAIN_EDITOR should NOT have review access");
    }

    @Test
    void buildDefaultCommentPermissions_forDataDomainAdmin_shouldGrantFullAccess() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

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
    void buildDefaultCommentPermissions_forBusinessDomainAdmin_shouldGrantFullAccessToAllDomains() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("bdadmin@example.com");
        RoleDto businessRole = new RoleDto();
        businessRole.setName("BUSINESS_DOMAIN_ADMIN");
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

        // Act
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "buildDefaultCommentPermissions", BatchUpdateContextRolesForUserDto.class, ContextsDto.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<DashboardCommentPermissionDto> result = (List<DashboardCommentPermissionDto>) method.invoke(service, user, availableContexts);

        // Assert
        assertEquals(2, result.size());
        for (DashboardCommentPermissionDto perm : result) {
            assertTrue(perm.isReadComments(), "BUSINESS_DOMAIN_ADMIN should have read access");
            assertTrue(perm.isWriteComments(), "BUSINESS_DOMAIN_ADMIN should have write access");
            assertTrue(perm.isReviewComments(), "BUSINESS_DOMAIN_ADMIN should have review access");
        }
    }

    @Test
    void buildDefaultCommentPermissions_forBusinessSpecialist_shouldGrantReadOnlyAccess() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("specialist@example.com");
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        user.setBusinessDomainRole(businessRole);

        UserContextRoleDto ddRole = new UserContextRoleDto();
        RoleDto specialistRole = new RoleDto();
        specialistRole.setName("DATA_DOMAIN_BUSINESS_SPECIALIST");
        ddRole.setRole(specialistRole);
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
        assertTrue(perm.isReadComments(), "DATA_DOMAIN_BUSINESS_SPECIALIST should have read access");
        assertFalse(perm.isWriteComments(), "DATA_DOMAIN_BUSINESS_SPECIALIST should NOT have write access");
        assertFalse(perm.isReviewComments(), "DATA_DOMAIN_BUSINESS_SPECIALIST should NOT have review access");
    }

    @Test
    void mapSupersetRolesToDashboards_forViewerWithMatchingRoles_shouldMapDashboards() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

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
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

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
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

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

    @Test
    void resolveDashboardGroupNamesToIds_shouldResolveCorrectly() throws Exception {
        // Arrange
        DashboardGroupEntity group1 = createDashboardGroupEntity("GroupA", "dd1");
        DashboardGroupEntity group2 = createDashboardGroupEntity("GroupB", "dd1");
        when(dashboardGroupService.findAllGroupsByContextKey("dd1")).thenReturn(List.of(group1, group2));

        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("test@example.com");
        Map<String, List<String>> groupNames = new HashMap<>();
        groupNames.put("dd1", List.of("GroupA", "GroupB"));
        user.setDashboardGroupNamesFromCsv(groupNames);

        // Act
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "resolveDashboardGroupNamesToIds", BatchUpdateContextRolesForUserDto.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, List<String>> result = (Map<String, List<String>>) method.invoke(service, user);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("dd1"));
        assertEquals(2, result.get("dd1").size());
        assertTrue(result.get("dd1").contains(group1.getId().toString()));
        assertTrue(result.get("dd1").contains(group2.getId().toString()));
    }

    @Test
    void resolveDashboardGroupNamesToIds_nonExistentGroup_shouldThrow() throws Exception {
        // Arrange
        DashboardGroupEntity group1 = createDashboardGroupEntity("GroupA", "dd1");
        when(dashboardGroupService.findAllGroupsByContextKey("dd1")).thenReturn(List.of(group1));

        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("test@example.com");
        Map<String, List<String>> groupNames = new HashMap<>();
        groupNames.put("dd1", List.of("GroupA", "NonExistentGroup"));
        user.setDashboardGroupNamesFromCsv(groupNames);

        // Act & Assert
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "resolveDashboardGroupNamesToIds", BatchUpdateContextRolesForUserDto.class);
        method.setAccessible(true);
        assertThrows(Exception.class, () -> {
            try {
                method.invoke(service, user);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void resolveDashboardGroupNamesToIds_emptyGroups_shouldReturnEmpty() throws Exception {
        // Arrange
        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

        BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
        user.setEmail("test@example.com");
        // No dashboard groups set

        // Act
        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "resolveDashboardGroupNamesToIds", BatchUpdateContextRolesForUserDto.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, List<String>> result = (Map<String, List<String>>) method.invoke(service, user);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void buildDefaultCommentPermissions_allRoles_shouldHaveCorrectPermissions() throws Exception {
        // This test verifies the complete permission matrix for all roles:
        // HELLODATA_ADMIN         -> read=true,  write=true,  review=true
        // BUSINESS_DOMAIN_ADMIN   -> read=true,  write=true,  review=true
        // DATA_DOMAIN_ADMIN       -> read=true,  write=true,  review=true
        // DATA_DOMAIN_EDITOR      -> read=true,  write=false, review=false
        // DATA_DOMAIN_VIEWER      -> read=true,  write=false, review=false
        // DATA_DOMAIN_BUSINESS_SPECIALIST -> read=true, write=false, review=false
        // NONE                    -> read=false, write=false, review=false

        BatchUsersInvitationService service = new BatchUsersInvitationService(
                new CsvParserService(), null, metaInfoResourceService, null, null, dashboardGroupService, "/tmp");

        Method method = BatchUsersInvitationService.class.getDeclaredMethod(
                "buildDefaultCommentPermissions", BatchUpdateContextRolesForUserDto.class, ContextsDto.class);
        method.setAccessible(true);

        ContextsDto availableContexts = new ContextsDto();
        ContextDto context = new ContextDto();
        context.setContextKey("dd1");
        context.setType(HdContextType.DATA_DOMAIN);
        availableContexts.setContexts(List.of(context));

        // Define expected permissions: businessRole, dataDomainRole, expectedRead, expectedWrite, expectedReview
        Object[][] testCases = {
                {"HELLODATA_ADMIN", "DATA_DOMAIN_ADMIN", true, true, true},
                {"BUSINESS_DOMAIN_ADMIN", "DATA_DOMAIN_ADMIN", true, true, true},
                {"NONE", "DATA_DOMAIN_ADMIN", true, true, true},
                {"NONE", "DATA_DOMAIN_EDITOR", true, false, false},
                {"NONE", "DATA_DOMAIN_VIEWER", true, false, false},
                {"NONE", "DATA_DOMAIN_BUSINESS_SPECIALIST", true, false, false},
                {"NONE", "NONE", false, false, false},
        };

        for (Object[] testCase : testCases) {
            String businessRoleName = (String) testCase[0];
            String dataDomainRoleName = (String) testCase[1];
            boolean expectedRead = (boolean) testCase[2];
            boolean expectedWrite = (boolean) testCase[3];
            boolean expectedReview = (boolean) testCase[4];

            BatchUpdateContextRolesForUserDto user = new BatchUpdateContextRolesForUserDto();
            user.setEmail("test@example.com");
            RoleDto businessRole = new RoleDto();
            businessRole.setName(businessRoleName);
            user.setBusinessDomainRole(businessRole);

            UserContextRoleDto ddRole = new UserContextRoleDto();
            RoleDto role = new RoleDto();
            role.setName(dataDomainRoleName);
            ddRole.setRole(role);
            ContextDto ddContext = new ContextDto();
            ddContext.setContextKey("dd1");
            ddRole.setContext(ddContext);
            user.setDataDomainRoles(List.of(ddRole));

            @SuppressWarnings("unchecked")
            List<DashboardCommentPermissionDto> result = (List<DashboardCommentPermissionDto>) method.invoke(service, user, availableContexts);

            assertEquals(1, result.size(),
                    "Expected 1 permission for business=%s, dd=%s".formatted(businessRoleName, dataDomainRoleName));
            DashboardCommentPermissionDto perm = result.get(0);
            assertEquals(expectedRead, perm.isReadComments(),
                    "Read permission mismatch for business=%s, dd=%s".formatted(businessRoleName, dataDomainRoleName));
            assertEquals(expectedWrite, perm.isWriteComments(),
                    "Write permission mismatch for business=%s, dd=%s".formatted(businessRoleName, dataDomainRoleName));
            assertEquals(expectedReview, perm.isReviewComments(),
                    "Review permission mismatch for business=%s, dd=%s".formatted(businessRoleName, dataDomainRoleName));
        }
    }
}

