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
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.RoleResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.RolePermissions;
import ch.bedag.dap.hellodata.portal.csv.service.CsvParserService;
import ch.bedag.dap.hellodata.portal.user.data.BatchUpdateContextRolesForUserDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchUsersInvitationServiceTest {

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Test
    void fetchDataFromFileTest() throws IOException {
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
        assertEquals(batchUpdateContextRolesForUserDto.getEmail(), "john.doe@example.com");

        assertEquals(batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName(), "NONE");

        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().size(), 1);
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName(), "DATA_DOMAIN_VIEWER");
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey(), "some_data_domain_key");

        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());

        //Second user
        batchUpdateContextRolesForUserDto = parsedUsers.get(1);
        assertEquals(batchUpdateContextRolesForUserDto.getEmail(), "jane.smith@example.com");

        assertEquals(batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName(), "NONE");

        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().size(), 1);
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName(), "DATA_DOMAIN_VIEWER");
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey(), "some_data_domain_key");

        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());

        //Fourth user
        batchUpdateContextRolesForUserDto = parsedUsers.get(3);
        assertEquals(batchUpdateContextRolesForUserDto.getEmail(), "bob.williams@example.com");

        assertEquals(batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName(), "NONE");

        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().size(), 1);
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName(), "DATA_DOMAIN_ADMIN");
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey(), "some_data_domain_key");

        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());

        //Sixth user
        batchUpdateContextRolesForUserDto = parsedUsers.get(5);
        assertEquals(batchUpdateContextRolesForUserDto.getEmail(), "laura.anderson@example.com");

        assertEquals(batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName(), "HELLODATA_ADMIN");

        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().size(), 1);
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName(), "DATA_DOMAIN_ADMIN");
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey(), "some_data_domain_key");

        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());
    }

    @Test
    void fetchDataFromFile_test_multiple_rows_for_one_user() throws IOException {
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
        assertEquals(batchUpdateContextRolesForUserDto.getEmail(), "john.doe@example.com");

        assertEquals(batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName(), "NONE");

        assertEquals(2, batchUpdateContextRolesForUserDto.getDataDomainRoles().size());
        assertEquals("DATA_DOMAIN_VIEWER", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName());
        assertEquals("data_domain_one_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey());

        assertEquals("DATA_DOMAIN_ADMIN", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(1).getRole().getName());
        assertEquals("data_domain_two_key", batchUpdateContextRolesForUserDto.getDataDomainRoles().get(1).getContext().getContextKey());

        assertNull(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser());

    }
}