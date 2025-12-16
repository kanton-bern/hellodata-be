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
package ch.bedag.dap.hellodata.portal.base;

import ch.bedag.dap.hellodata.portal.base.auth.HellodataAuthenticationConverter;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ch.bedag.dap.hellodata.portal.base.TestJwtProvider.JWT_EXPIRATION_IN_MS;
import static ch.bedag.dap.hellodata.portal.base.TestJwtProvider.JWT_SECRET;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {TestSecurityConfig.class, HellodataAuthenticationConverter.class})
public abstract class HDControllerTest {

    @Autowired
    protected MockMvc mockMvc;
    @MockitoBean
    protected UserRepository userRepository;
    @MockitoBean
    private InMemoryClientRegistrationRepository inMemoryClientRegistrationRepository;

    protected byte[] asJsonString(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);

        return mapper.writeValueAsBytes(object);
    }

    protected String generateToken(UUID userId, String firstname, String lastName, String email, boolean superuser, Set<String> permissions) {
        return "Bearer " + Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("email", email)
                .claim("given_name", firstname)
                .claim("family_name", lastName)
                .claim("is_superuser", superuser)
                .claim("authorities", !permissions.isEmpty() ? permissions.stream().collect(Collectors.joining(",")) : "NONE")
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_IN_MS))
                .signWith(SignatureAlgorithm.HS512, JWT_SECRET)
                .compact();
    }

    protected String generateToken(String firstname, String lastName, String email, boolean superuser, Set<String> permissions) {
        return generateToken(UUID.randomUUID(), firstname, lastName, email, superuser, permissions);
    }

    protected String generateToken(UUID userId, Set<String> permissions) {
        return generateToken(userId, "test", "lastname", "email@test.com", false, permissions);
    }

    protected String generateToken(UUID userId) {
        return generateToken(userId, "test", "lastname", "email@test.com", false, new HashSet<>());
    }

    protected String generateToken(Set<String> permissions) {
        return generateToken("test", "lastname", "email@test.com", false, permissions);
    }

    protected String generateToken() {
        return generateToken(new HashSet<>());
    }
}
