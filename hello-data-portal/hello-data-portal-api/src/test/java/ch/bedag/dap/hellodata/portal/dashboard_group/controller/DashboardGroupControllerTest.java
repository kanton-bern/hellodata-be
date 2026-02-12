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
package ch.bedag.dap.hellodata.portal.dashboard_group.controller;

import ch.bedag.dap.hellodata.portal.base.HDControllerTest;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupCreateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupUpdateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntry;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardGroupController.class)
@ContextConfiguration(classes = {DashboardGroupController.class})
class DashboardGroupControllerTest extends HDControllerTest {

    @MockitoBean
    private DashboardGroupService dashboardGroupService;

    @Test
    void getAllDashboardGroups_userLoggedInNoPrivileges() throws Exception {
        // given / when / then
        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard-groups?page=0&size=10")
                .header("authorization", generateToken(new HashSet<>()))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    void getAllDashboardGroups_userLoggedInHasPrivileges() throws Exception {
        // given / when / then
        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard-groups?page=0&size=10")
                .header("authorization", generateToken(Set.of("DASHBOARD_GROUPS_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    void getDashboardGroupById_userLoggedInNoPrivileges() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard-groups/" + uuid)
                .header("authorization", generateToken(new HashSet<>()))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    void getDashboardGroupById_userLoggedInHasPrivileges() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard-groups/" + uuid)
                .header("authorization", generateToken(Set.of("DASHBOARD_GROUPS_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    void createDashboardGroup_userLoggedInNoPrivileges() throws Exception {
        // given
        DashboardGroupCreateDto createDto = new DashboardGroupCreateDto();
        createDto.setName("Test Group");
        createDto.setEntries(List.of(new DashboardGroupEntry("ctx1", 1, "Dashboard 1")));

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-groups")
                .header("authorization", generateToken(new HashSet<>()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(createDto))).andExpect(status().isForbidden());
    }

    @Test
    void createDashboardGroup_userLoggedInHasPrivileges() throws Exception {
        // given
        DashboardGroupCreateDto createDto = new DashboardGroupCreateDto();
        createDto.setName("Test Group");
        createDto.setEntries(List.of(new DashboardGroupEntry("ctx1", 1, "Dashboard 1")));

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-groups")
                .header("authorization", generateToken(Set.of("DASHBOARD_GROUPS_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(createDto))).andExpect(status().isOk());
    }

    @Test
    void updateDashboardGroup_userLoggedInNoPrivileges() throws Exception {
        // given
        DashboardGroupUpdateDto updateDto = new DashboardGroupUpdateDto();
        updateDto.setId(UUID.randomUUID());
        updateDto.setName("Updated Group");
        updateDto.setEntries(List.of(new DashboardGroupEntry("ctx1", 1, "Dashboard 1")));

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.put("/dashboard-groups")
                .header("authorization", generateToken(new HashSet<>()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDto))).andExpect(status().isForbidden());
    }

    @Test
    void updateDashboardGroup_userLoggedInHasPrivileges() throws Exception {
        // given
        DashboardGroupUpdateDto updateDto = new DashboardGroupUpdateDto();
        updateDto.setId(UUID.randomUUID());
        updateDto.setName("Updated Group");
        updateDto.setEntries(List.of(new DashboardGroupEntry("ctx1", 1, "Dashboard 1")));

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.put("/dashboard-groups")
                .header("authorization", generateToken(Set.of("DASHBOARD_GROUPS_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDto))).andExpect(status().isOk());
    }

    @Test
    void deleteDashboardGroup_userLoggedInNoPrivileges() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.delete("/dashboard-groups/" + uuid)
                .header("authorization", generateToken())).andExpect(status().isForbidden());
    }

    @Test
    void deleteDashboardGroup_userLoggedInHasPrivileges() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.delete("/dashboard-groups/" + uuid)
                .header("authorization", generateToken(Set.of("DASHBOARD_GROUPS_MANAGEMENT")))).andExpect(status().isOk());
    }
}
