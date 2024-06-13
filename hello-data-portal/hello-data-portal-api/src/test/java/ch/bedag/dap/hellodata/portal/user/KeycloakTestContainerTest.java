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
package ch.bedag.dap.hellodata.portal.user;

import ch.bedag.dap.hellodata.commons.nats.actuator.NatsHealthIndicator;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.portal.initialize.service.RolesInitializer;
import ch.bedag.dap.hellodata.portal.monitoring.service.StorageSizeService;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.nats.client.Connection;
import io.restassured.RestAssured;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;

@Log4j2
@SuppressWarnings("unused")
@ActiveProfiles("tc-keycloak")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class KeycloakTestContainerTest {

    public static final KeycloakContainer KEYCLOAK_CONTAINER;

    static {
        KEYCLOAK_CONTAINER = new KeycloakContainer().withEnv("KEYCLOAK_USER", "admin")
                                                    .withEnv("KEYCLOAK_PASSWORD", "admin")
                                                    .withRealmImportFile("keycloak/realm.json")
                                                    .waitingFor(Wait.forHttp("/"));
        KEYCLOAK_CONTAINER.start();
    }

    @LocalServerPort
    private int port;
    @MockBean
    private Connection connection;
    @MockBean
    private NatsSenderService natsSenderService;
    @MockBean
    private RolesInitializer rolesInitializer;
    @MockBean
    private RoleService roleService;
    @MockBean
    private StorageSizeService storageSizeService;
    @MockBean
    private NatsHealthIndicator natsHealthIndicator;

    @DynamicPropertySource
    static void registerResourceServerIssuerProperty(DynamicPropertyRegistry registry) {
        registry.add("hello-data.auth-server.url", () -> {
            String authServerUrl = KEYCLOAK_CONTAINER.getAuthServerUrl();
            if (authServerUrl.endsWith("/")) {
                authServerUrl = authServerUrl.substring(0, authServerUrl.length() - 1);
            }
            return authServerUrl;
        });
    }

    @PostConstruct
    public void init() {
        RestAssured.baseURI = "http://localhost:" + port;
    }
}
