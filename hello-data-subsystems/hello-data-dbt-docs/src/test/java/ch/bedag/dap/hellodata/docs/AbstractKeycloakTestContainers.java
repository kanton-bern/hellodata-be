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
package ch.bedag.dap.hellodata.docs;

import ch.bedag.dap.hellodata.docs.service.ProjectDocService;
import ch.bedag.dap.hellodata.docs.service.StorageTraverseService;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.servlet.*;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.security.web.debug.DebugFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Log4j2
@ActiveProfiles("test-containers")
@Import({AbstractKeycloakTestContainers.OverrideBean.class})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class AbstractKeycloakTestContainers {

    @SuppressWarnings("unused")
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL_CONTAINER = new PostgreSQLContainer("postgres:11.1")
            .withDatabaseName("test")
            .withUsername("sa")
            .withPassword("sa");

    static final KeycloakContainer KEYCLOAK_CONTAINER = new KeycloakContainer("quay.io/keycloak/keycloak:26.4")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withRealmImportFile("keycloak/realm.json")
            .waitingFor(
                    Wait.forHttp("/realms/hellodata")
                            .forPort(8080)
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(3))
            );

    static {
        KEYCLOAK_CONTAINER.start();
    }

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private EntityManager em;

    @LocalServerPort
    private int port;

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

    private static String getUserBearerToken(String userName, String password) {
        try {
            URI authorizationURI = new URIBuilder(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/hellodata/protocol/openid-connect/token").build();
            WebClient webclient = WebClient.builder().build();
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.put("grant_type", Collections.singletonList("password"));
            formData.put("client_id", Collections.singletonList("frontend-client"));

            formData.put("username", Collections.singletonList(userName));

            formData.put("password", Collections.singletonList(password));

            String result = webclient.post()
                    .uri(authorizationURI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JacksonJsonParser jsonParser = new JacksonJsonParser();

            return "Bearer " + jsonParser.parseMap(result).get("access_token").toString();
        } catch (URISyntaxException e) {
            log.error("Can't obtain an access token from Keycloak!", e);
        }

        return null;
    }

    @PostConstruct
    public void init() {
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @AfterEach
    @Transactional
    public void cleanUp() {
        log.debug("Truncate all tables");
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Metamodel metamodel = em.getMetamodel();

                em.createNativeQuery("SET session_replication_role = 'replica';").executeUpdate();
                for (EntityType<?> e : metamodel.getEntities()) {
                    String entityTableName = e.getName();
                    em.createNativeQuery("truncate table " + entityTableName + " cascade").executeUpdate();
                }
                em.createNativeQuery("SET session_replication_role = 'origin';").executeUpdate();
            }
        });
    }

    @TestConfiguration
    public static class OverrideBean {
        @Bean
        static BeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor() {
            return registry -> registry.getBeanDefinition(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)
                    .setBeanClassName(CompositeFilterChainProxy.class.getName());
        }

        @Bean
        @Primary
        public StorageTraverseService storageTraverseService() {
            return Mockito.mock(StorageTraverseService.class);
        }

        @Bean
        @Primary
        public ProjectDocService projectDocService() {
            return Mockito.mock(ProjectDocService.class);
        }

        static class CompositeFilterChainProxy extends FilterChainProxy {

            private final Filter doFilterDelegate;

            private final FilterChainProxy springSecurityFilterChain;

            CompositeFilterChainProxy(List<? extends Filter> filters) {
                this.doFilterDelegate = createDoFilterDelegate(filters);
                this.springSecurityFilterChain = findFilterChainProxy(filters);
            }

            private static Filter createDoFilterDelegate(List<? extends Filter> filters) {
                CompositeFilter delegate = new CompositeFilter();
                delegate.setFilters(filters);
                return delegate;
            }

            private static FilterChainProxy findFilterChainProxy(List<? extends Filter> filters) {
                for (Filter filter : filters) {
                    if (filter instanceof FilterChainProxy fcp) {
                        return fcp;
                    }
                    if (filter instanceof DebugFilter debugFilter) {
                        return debugFilter.getFilterChainProxy();
                    }
                }
                throw new IllegalStateException("Couldn't find FilterChainProxy in " + filters);
            }

            @Override
            public void afterPropertiesSet() {
                this.springSecurityFilterChain.afterPropertiesSet();
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                this.doFilterDelegate.doFilter(request, response, chain);
            }

            @Override
            public List<Filter> getFilters(String url) {
                return this.springSecurityFilterChain.getFilters(url);
            }

            @Override
            public List<SecurityFilterChain> getFilterChains() {
                return this.springSecurityFilterChain.getFilterChains();
            }

            @Override
            public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
                this.springSecurityFilterChain.setSecurityContextHolderStrategy(securityContextHolderStrategy);
            }

            @Override
            public void setFilterChainValidator(FilterChainValidator filterChainValidator) {
                this.springSecurityFilterChain.setFilterChainValidator(filterChainValidator);
            }

            @Override
            public void setFilterChainDecorator(FilterChainDecorator filterChainDecorator) {
                this.springSecurityFilterChain.setFilterChainDecorator(filterChainDecorator);
            }

            @Override
            public void setFirewall(HttpFirewall firewall) {
                this.springSecurityFilterChain.setFirewall(firewall);
            }

            @Override
            public void setRequestRejectedHandler(RequestRejectedHandler requestRejectedHandler) {
                this.springSecurityFilterChain.setRequestRejectedHandler(requestRejectedHandler);
            }
        }
    }

    protected String getAdminBearer() {
        String userName = "admin";
        String password = "admin";
        return getUserBearerToken(userName, password);
    }

    protected String getMomiUserBearer() {
        String userName = "momi-user";
        String password = "momi-user";
        return getUserBearerToken(userName, password);
    }

    protected String getNoRolesUserBearer() {
        String userName = "user-no-roles";
        String password = "user-no-roles";
        return getUserBearerToken(userName, password);
    }
}
