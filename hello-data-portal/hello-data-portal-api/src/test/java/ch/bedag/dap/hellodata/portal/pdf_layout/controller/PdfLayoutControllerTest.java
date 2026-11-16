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
package ch.bedag.dap.hellodata.portal.pdf_layout.controller;

import ch.bedag.dap.hellodata.portal.base.HDControllerTest;
import ch.bedag.dap.hellodata.portal.pdf_layout.data.PdfLayoutSaveDto;
import ch.bedag.dap.hellodata.portal.pdf_layout.service.PdfLayoutService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PdfLayoutController.class)
class PdfLayoutControllerTest extends HDControllerTest {

    @MockitoBean
    private PdfLayoutService pdfLayoutService;

    @Test
    void getMyLayouts_userLoggedInNoPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/pdf-layouts")
                .header("authorization", generateToken(new HashSet<>()))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    void getMyLayouts_userLoggedInHasPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/pdf-layouts?contextKey=ctx1")
                .header("authorization", generateToken(Set.of("DASHBOARDS")))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    void getLayout_userLoggedInHasPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/pdf-layouts/" + UUID.randomUUID())
                .header("authorization", generateToken(Set.of("DASHBOARDS")))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    void createLayout_userLoggedInNoPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/pdf-layouts")
                .header("authorization", generateToken(new HashSet<>()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(saveDto()))).andExpect(status().isForbidden());
    }

    @Test
    void createLayout_userLoggedInHasPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/pdf-layouts")
                .header("authorization", generateToken(Set.of("DASHBOARDS")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(saveDto()))).andExpect(status().isCreated());
    }

    @Test
    void updateLayout_userLoggedInHasPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/pdf-layouts/" + UUID.randomUUID())
                .header("authorization", generateToken(Set.of("DASHBOARDS")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(saveDto()))).andExpect(status().isOk());
    }

    @Test
    void deleteLayout_userLoggedInHasPrivileges() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/pdf-layouts/" + UUID.randomUUID())
                .header("authorization", generateToken(Set.of("DASHBOARDS")))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    private static PdfLayoutSaveDto saveDto() {
        PdfLayoutSaveDto dto = new PdfLayoutSaveDto();
        dto.setName("Monthly");
        dto.setInstanceName("superset-ctx1");
        dto.setDashboardId(7);
        dto.setTemplate("portrait");
        dto.setPageCount(1);
        dto.setItems(List.of());
        return dto;
    }
}
