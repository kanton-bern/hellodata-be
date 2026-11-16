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
package ch.bedag.dap.hellodata.portal.rls_role.controller;

import ch.bedag.dap.hellodata.portal.base.HDControllerTest;
import ch.bedag.dap.hellodata.portal.rls_role.service.RlsRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RlsRoleController.class)
class RlsRoleControllerTest extends HDControllerTest {

    private static final String RLS_ROLES_BODY = """
            [{"roleKey": "RLS_01", "name": "Region Bern"}]""";

    @MockitoBean
    private RlsRoleService rlsRoleService;

    @Test
    void getManageableDataDomains_noPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rls-roles/data-domains")
                .header("authorization", generateToken(new HashSet<>()))).andExpect(status().isForbidden());
    }

    @Test
    void getManageableDataDomains_hasPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rls-roles/data-domains")
                .header("authorization", generateToken(Set.of("RLS_ROLES_MANAGEMENT")))).andExpect(status().isOk());
    }

    @Test
    void getRlsRoles_noPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rls-roles/dd01")
                .header("authorization", generateToken(Set.of("DASHBOARDS")))).andExpect(status().isForbidden());
    }

    @Test
    void getRlsRoles_userManagerHasPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rls-roles/dd01")
                .header("authorization", generateToken(Set.of("USER_MANAGEMENT")))).andExpect(status().isOk());
    }

    @Test
    void updateRlsRoleNames_userManagerWithoutRlsPrivilege() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/rls-roles/dd01")
                .header("authorization", generateToken(Set.of("USER_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(RLS_ROLES_BODY)).andExpect(status().isForbidden());
    }

    @Test
    void updateRlsRoleNames_hasPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/rls-roles/dd01")
                .header("authorization", generateToken(Set.of("RLS_ROLES_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(RLS_ROLES_BODY)).andExpect(status().isOk());
    }
}
