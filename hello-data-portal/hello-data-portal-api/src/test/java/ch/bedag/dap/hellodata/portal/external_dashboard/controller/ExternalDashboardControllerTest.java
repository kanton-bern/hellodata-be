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
package ch.bedag.dap.hellodata.portal.external_dashboard.controller;

import ch.bedag.dap.hellodata.portal.base.HDControllerTest;
import ch.bedag.dap.hellodata.portal.external_dashboard.data.CreateExternalDashboardDto;
import ch.bedag.dap.hellodata.portal.external_dashboard.data.ExternalDashboardDto;
import ch.bedag.dap.hellodata.portal.external_dashboard.data.UpdateExternalDashboardDto;
import ch.bedag.dap.hellodata.portal.external_dashboard.service.ExternalDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExternalDashboardController.class)
@ContextConfiguration(classes = {ExternalDashboardController.class})
class ExternalDashboardControllerTest extends HDControllerTest {

    @MockitoBean
    private ExternalDashboardService externalDashboardService;

    @Test
    void fetchExternalDashboards_userNotLoggedIn() throws Exception {
        //given
        when(externalDashboardService.getExternalDashboards()).thenReturn(List.of());
        //when then
        mockMvc.perform(MockMvcRequestBuilders.get("/external-dashboards")).andExpect(status().isUnauthorized());
    }

    @Test
    void fetchExternalDashboards_userLoggedIn() throws Exception {
        //given
        when(externalDashboardService.getExternalDashboards()).thenReturn(List.of());

        //when then
        mockMvc.perform(MockMvcRequestBuilders.get("/external-dashboards").header("authorization", generateToken()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getExternalDashboard_userLoggedInNoPrivileges() throws Exception {
        //given
        UUID uuid = UUID.randomUUID();
        ExternalDashboardDto externalDashboardDto = new ExternalDashboardDto();
        externalDashboardDto.setId(uuid);
        externalDashboardDto.setTitle("test");
        externalDashboardDto.setUrl("test");

        when(externalDashboardService.getExternalDashboard(uuid)).thenReturn(externalDashboardDto);

        //when then
        mockMvc.perform(MockMvcRequestBuilders.get("/external-dashboards/" + uuid).header("authorization", generateToken()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getExternalDashboard_userLoggedInHasPrivileges() throws Exception {
        //given
        UUID uuid = UUID.randomUUID();
        ExternalDashboardDto externalDashboardDto = new ExternalDashboardDto();
        externalDashboardDto.setId(uuid);
        externalDashboardDto.setTitle("test");
        externalDashboardDto.setUrl("test");

        when(externalDashboardService.getExternalDashboard(uuid)).thenReturn(externalDashboardDto);

        //when then
        mockMvc.perform(MockMvcRequestBuilders.get("/external-dashboards/" + uuid)
                .header("authorization", generateToken(uuid, Set.of("EXTERNAL_DASHBOARDS_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    void createExternalDashboard_userLoggedInNoPrivileges() throws Exception {
        // given
        CreateExternalDashboardDto createDto = new CreateExternalDashboardDto();
        createDto.setTitle("test");
        createDto.setUrl("test");
        createDto.setContextKey("some context key");

        // ehen then
        mockMvc.perform(MockMvcRequestBuilders.post("/external-dashboards")
                .header("authorization", generateToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(createDto))).andExpect(status().isForbidden());
    }

    @Test
    void createExternalDashboard_userLoggedInHasPrivileges() throws Exception {
        // given
        CreateExternalDashboardDto createDto = new CreateExternalDashboardDto();
        createDto.setTitle("test");
        createDto.setUrl("test");
        createDto.setContextKey("some context key");

        // when then
        mockMvc.perform(MockMvcRequestBuilders.post("/external-dashboards")
                .header("authorization", generateToken(Set.of("EXTERNAL_DASHBOARDS_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(createDto))).andExpect(status().isOk());
    }

    @Test
    void updateExternalDashboard_userLoggedInNoPrivileges() throws Exception {
        // given
        UpdateExternalDashboardDto updateDto = new UpdateExternalDashboardDto();
        UUID uuid = UUID.randomUUID();
        updateDto.setId(uuid);
        updateDto.setTitle("test");
        updateDto.setUrl("test");
        updateDto.setContextKey("some context key");

        // when then
        mockMvc.perform(MockMvcRequestBuilders.put("/external-dashboards")
                .header("authorization", generateToken(new HashSet<>()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDto))).andExpect(status().isForbidden());
    }

    @Test
    void updateExternalDashboard_userLoggedInHasPrivileges() throws Exception {
        // given
        UpdateExternalDashboardDto updateDto = new UpdateExternalDashboardDto();
        UUID uuid = UUID.randomUUID();
        updateDto.setId(uuid);
        updateDto.setTitle("test");
        updateDto.setUrl("test");
        updateDto.setContextKey("some context key");

        // when then
        mockMvc.perform(MockMvcRequestBuilders.put("/external-dashboards")
                .header("authorization", generateToken(Set.of("EXTERNAL_DASHBOARDS_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDto))).andExpect(status().isOk());
    }

    @Test
    void deleteExternalDashboard_userLoggedInNoPrivileges() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();

        // when then
        mockMvc.perform(MockMvcRequestBuilders.delete("/external-dashboards/" + uuid).header("authorization", generateToken())).andExpect(status().isForbidden());
    }

    @Test
    void deleteExternalDashboard_userLoggedInHasPrivileges() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();

        // when then
        mockMvc.perform(MockMvcRequestBuilders.delete("/external-dashboards/" + uuid).header("authorization", generateToken(Set.of("EXTERNAL_DASHBOARDS_MANAGEMENT"))))
                .andExpect(status().isOk());
    }
}
