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
package ch.bedag.dap.hellodata.portal.role.controller;

import ch.bedag.dap.hellodata.portal.base.HDControllerTest;
import ch.bedag.dap.hellodata.portal.role.data.PortalRoleCreateDto;
import ch.bedag.dap.hellodata.portal.role.data.PortalRoleUpdateDto;
import ch.bedag.dap.hellodata.portal.role.service.PortalRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortalRoleController.class)
@ContextConfiguration(classes = {PortalRoleController.class})
class PortalRoleControllerTest extends HDControllerTest {
    @MockitoBean
    private PortalRoleService portalRoleService;

    @Test
    void createRole_userNotLoggedIn() throws Exception {
        // given
        PortalRoleCreateDto createDto = new PortalRoleCreateDto();
        createDto.setName("Test Role");

        // when then
        mockMvc.perform(MockMvcRequestBuilders.post("/portal-roles").contentType(MediaType.APPLICATION_JSON).content(asJsonString(createDto))).andExpect(status().isUnauthorized());
    }

    @Test
    void createRole_userLoggedIn() throws Exception {
        // given
        PortalRoleCreateDto createDto = new PortalRoleCreateDto();
        createDto.setName("Test Role");

        // when then
        mockMvc.perform(MockMvcRequestBuilders.post("/portal-roles")
                .header("authorization", generateToken(Set.of("ROLE_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(createDto))).andExpect(status().isOk());
    }

    @Test
    void updateRole_userNotLoggedIn() throws Exception {
        // given
        PortalRoleUpdateDto updateDto = new PortalRoleUpdateDto();
        UUID uuid = UUID.randomUUID();
        updateDto.setId(uuid);
        updateDto.setName("Updated Role");

        // when then
        mockMvc.perform(MockMvcRequestBuilders.patch("/portal-roles").contentType(MediaType.APPLICATION_JSON).content(asJsonString(updateDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateRole_userLoggedIn() throws Exception {
        // given
        PortalRoleUpdateDto updateDto = new PortalRoleUpdateDto();
        UUID uuid = UUID.randomUUID();
        updateDto.setId(uuid);
        updateDto.setName("Updated Role");

        // when then
        mockMvc.perform(MockMvcRequestBuilders.patch("/portal-roles")
                .header("authorization", generateToken(Set.of("ROLE_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDto))).andExpect(status().isOk());
    }

    @Test
    void deleteRole_userNotLoggedIn() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();

        // when then
        mockMvc.perform(MockMvcRequestBuilders.delete("/portal-roles/" + uuid).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteRole_userLoggedIn() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();

        // when then
        mockMvc.perform(MockMvcRequestBuilders.delete("/portal-roles/" + uuid).header("authorization", generateToken(Set.of("ROLE_MANAGEMENT")))).andExpect(status().isOk());
    }
}
