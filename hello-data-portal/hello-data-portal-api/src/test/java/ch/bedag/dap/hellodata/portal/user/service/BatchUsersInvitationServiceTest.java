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

import ch.bedag.dap.hellodata.portal.excel.service.ExcelParserService;
import ch.bedag.dap.hellodata.portal.user.data.BatchUpdateContextRolesForUserDto;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BatchUsersInvitationServiceTest {

    @Test
    void fetchUsersFileTest() throws IOException {
        URL resource = getClass().getClassLoader().getResource("");
        assertNotNull(resource, "The test resources directory should exist in the classpath");

        String testResourcesPath = new File(resource.getFile()).getAbsolutePath();
        BatchUsersInvitationService batchUsersInvitationService1 = new BatchUsersInvitationService(new ExcelParserService(), testResourcesPath);
        List<BatchUpdateContextRolesForUserDto> parsedUsers = batchUsersInvitationService1.fetchUsersFile(false);

        assertEquals(parsedUsers.size(), 2);

        //First user
        BatchUpdateContextRolesForUserDto batchUpdateContextRolesForUserDto = parsedUsers.get(0);
        assertEquals(batchUpdateContextRolesForUserDto.getEmail(), "some.email@example.com");

        assertEquals(batchUpdateContextRolesForUserDto.getBusinessDomainRole().getName(), "NONE");

        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().size(), 2);
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getRole().getName(), "DATA_DOMAIN_VIEWER");
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(0).getContext().getContextKey(), "showcase");
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(1).getRole().getName(), "DATA_DOMAIN_VIEWER");
        assertEquals(batchUpdateContextRolesForUserDto.getDataDomainRoles().get(1).getContext().getContextKey(), "demo");

        assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("showcase").get(0).getTitle(), "Showcase dashboard 1");
        assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("showcase").get(1).getTitle(), "Showcase dashboard 2");
        assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("showcase").get(0).getId(), 6);
        assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("showcase").get(1).getId(), 1);

        assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("demo").get(0).getTitle(), "Demo dashboard 1");
        assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("demo").get(1).getTitle(), "Demo dashboard 2");
        assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("demo").get(0).getId(), 2);
        assertEquals(batchUpdateContextRolesForUserDto.getSelectedDashboardsForUser().get("demo").get(1).getId(), 5);


        //Second user
        BatchUpdateContextRolesForUserDto batchUpdateContextRolesForUserDto1 = parsedUsers.get(1);
        assertEquals(batchUpdateContextRolesForUserDto1.getEmail(), "some.other.email@example.com");

        assertEquals(batchUpdateContextRolesForUserDto1.getBusinessDomainRole().getName(), "NONE");

        assertEquals(batchUpdateContextRolesForUserDto1.getDataDomainRoles().size(), 2);
        assertEquals(batchUpdateContextRolesForUserDto1.getDataDomainRoles().get(0).getRole().getName(), "DATA_DOMAIN_VIEWER");
        assertEquals(batchUpdateContextRolesForUserDto1.getDataDomainRoles().get(0).getContext().getContextKey(), "showcase");
        assertEquals(batchUpdateContextRolesForUserDto1.getDataDomainRoles().get(1).getRole().getName(), "DATA_DOMAIN_VIEWER");
        assertEquals(batchUpdateContextRolesForUserDto1.getDataDomainRoles().get(1).getContext().getContextKey(), "demo");

        assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("showcase").get(0).getTitle(), "Showcase dashboard 1");
        assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("showcase").get(1).getTitle(), "Showcase dashboard 2");
        assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("showcase").get(0).getId(), 6);
        assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("showcase").get(1).getId(), 1);

        assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("demo").get(0).getTitle(), "Demo dashboard 1");
        assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("demo").get(1).getTitle(), "Demo dashboard 2");
        assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("demo").get(0).getId(), 2);
        assertEquals(batchUpdateContextRolesForUserDto1.getSelectedDashboardsForUser().get("demo").get(1).getId(), 5);
    }
}