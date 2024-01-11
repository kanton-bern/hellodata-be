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
package ch.bedag.dap.hellodata.portal.faq.controller;

import ch.bedag.dap.hellodata.portal.base.HDControllerTest;
import ch.bedag.dap.hellodata.portal.faq.data.FaqCreateDto;
import ch.bedag.dap.hellodata.portal.faq.data.FaqUpdateDto;
import ch.bedag.dap.hellodata.portal.faq.service.FaqService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FaqController.class)
@ContextConfiguration(classes = { FaqController.class })
class FaqControllerTest extends HDControllerTest {

    @MockBean
    private FaqService faqService;

    @Test
    void createAnnouncement_userLoggedInNoPrivileges() throws Exception {
        // given
        FaqCreateDto createDto = new FaqCreateDto();
        createDto.setTitle("Test title");
        createDto.setMessage("Test message");
        createDto.setContextKey("Test context key");

        // when then
        mockMvc.perform(MockMvcRequestBuilders.post("/faq")
                                              .header("authorization", generateToken(new HashSet<>()))
                                              .contentType(MediaType.APPLICATION_JSON)
                                              .content(asJsonString(createDto))).andExpect(status().isForbidden());
    }

    @Test
    void createAnnouncement_userLoggedInHasPrivileges() throws Exception {
        // given
        FaqCreateDto createDto = new FaqCreateDto();
        createDto.setTitle("Test title");
        createDto.setMessage("Test message");
        createDto.setContextKey("Test context key");

        // when then
        mockMvc.perform(MockMvcRequestBuilders.post("/faq")
                                              .header("authorization", generateToken(Set.of("FAQ_MANAGEMENT")))
                                              .contentType(MediaType.APPLICATION_JSON)
                                              .content(asJsonString(createDto))).andExpect(status().isOk());
    }

    @Test
    void updateAnnouncement_userLoggedInNoPrivileges() throws Exception {
        // given
        FaqUpdateDto updateDto = new FaqUpdateDto();
        UUID uuid = UUID.randomUUID();
        updateDto.setId(uuid);
        updateDto.setTitle("Test title");
        updateDto.setMessage("Test message");
        updateDto.setContextKey("Test context key");

        // when then
        mockMvc.perform(
                       MockMvcRequestBuilders.put("/faq").header("authorization", generateToken(new HashSet<>())).contentType(MediaType.APPLICATION_JSON).content(asJsonString(updateDto)))
               .andExpect(status().isForbidden());
    }

    @Test
    void updateAnnouncement_userLoggedInHasPrivileges() throws Exception {
        // given
        FaqUpdateDto updateDto = new FaqUpdateDto();
        UUID uuid = UUID.randomUUID();
        updateDto.setId(uuid);
        updateDto.setTitle("Test title");
        updateDto.setMessage("Test message");
        updateDto.setContextKey("Test context key");

        // when then
        mockMvc.perform(MockMvcRequestBuilders.put("/faq")
                                              .header("authorization", generateToken(Set.of("FAQ_MANAGEMENT")))
                                              .contentType(MediaType.APPLICATION_JSON)
                                              .content(asJsonString(updateDto))).andExpect(status().isOk());
    }

    @Test
    void deleteFaq_userLoggedInNoPrivileges() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();

        // when then
        mockMvc.perform(MockMvcRequestBuilders.delete("/faq/" + uuid).header("authorization", generateToken())).andExpect(status().isForbidden());
    }

    @Test
    void deleteFaq_userLoggedInHasPrivileges() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();

        // when then
        mockMvc.perform(MockMvcRequestBuilders.delete("/faq/" + uuid).header("authorization", generateToken(Set.of("FAQ_MANAGEMENT")))).andExpect(status().isOk());
    }
}
