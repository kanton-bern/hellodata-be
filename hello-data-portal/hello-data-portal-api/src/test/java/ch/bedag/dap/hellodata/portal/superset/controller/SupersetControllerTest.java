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
package ch.bedag.dap.hellodata.portal.superset.controller;

import ch.bedag.dap.hellodata.commons.security.Permission;
import ch.bedag.dap.hellodata.portal.base.HDControllerTest;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardDto;
import ch.bedag.dap.hellodata.portal.superset.data.UpdateSupersetDashboardMetadataDto;
import ch.bedag.dap.hellodata.portal.superset.service.DashboardAccessService;
import ch.bedag.dap.hellodata.portal.superset.service.DashboardService;
import ch.bedag.dap.hellodata.portal.superset.service.QueryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupersetController.class)
@ContextConfiguration(classes = {SupersetController.class})
class SupersetControllerTest extends HDControllerTest {

    @MockitoBean
    private DashboardService dashboardService;
    @MockitoBean
    private QueryService queryService;
    @MockitoBean
    private DashboardAccessService dashboardAccessService;

    @Test
    void fetchMyDashboards() throws Exception {
        // given
        Set<SupersetDashboardDto> dashboards = new HashSet<>();
        SupersetDashboardDto supersetDashboardDto = new SupersetDashboardDto();
        supersetDashboardDto.setId(1);
        supersetDashboardDto.setDashboardTitle("Dashboard1");
        dashboards.add(supersetDashboardDto);
        SupersetDashboardDto supersetDashboardDto1 = new SupersetDashboardDto();
        supersetDashboardDto1.setId(2);
        supersetDashboardDto1.setDashboardTitle("Dashboard2");
        dashboards.add(supersetDashboardDto1);

        when(dashboardService.fetchMyDashboards()).thenReturn(dashboards);

        // when then
        mockMvc.perform(get("/superset/my-dashboards").header("authorization",
                        generateToken(Set.of(Permission.DASHBOARDS.name()))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [
                            {
                                "compositeId": null,
                                "contextId": null,
                                "contextKey": null,
                                "contextName": null,
                                "dashboardTitle": "Dashboard2",
                                "dashboardUrlPath": null,
                                "id": 2,
                                "instanceName": null,
                                "instanceUrl": null,
                                "published": false,
                                "roles": null,
                                "slug": null,
                                "status": null,
                                "thumbnailPath": null
                            },
                            {
                                "compositeId": null,
                                "contextId": null,
                                "contextKey": null,
                                "contextName": null,
                                "dashboardTitle": "Dashboard1",
                                "dashboardUrlPath": null,
                                "id": 1,
                                "instanceName": null,
                                "instanceUrl": null,
                                "published": false,
                                "roles": null,
                                "slug": null,
                                "status": null,
                                "thumbnailPath": null
                            }
                        ]
                        """));
    }

    @Test
    void updateDashboard() throws Exception {
        // given
        String instanceName = "instance";
        int subsystemId = 1;
        UpdateSupersetDashboardMetadataDto updateDto = new UpdateSupersetDashboardMetadataDto();
        updateDto.setBusinessProcess("Updated Metadata");

        // when then
        mockMvc.perform(patch("/superset/dashboards/{instanceName}/{subsystemId}", instanceName, subsystemId).header("authorization", generateToken(Set.of(Permission.DASHBOARDS.name())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDto))).andExpect(status().isOk());
    }
}

