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
package ch.bedag.dap.hellodata.cloudbeaver.gateway.config;

import ch.bedag.dap.hellodata.cloudbeaver.gateway.filters.AuthWebFilter;
import ch.bedag.dap.hellodata.cloudbeaver.gateway.filters.LoggingFilter;
import ch.bedag.dap.hellodata.cloudbeaver.gateway.filters.TokenAuthenticationFilter;
import ch.bedag.dap.hellodata.cloudbeaver.gateway.filters.WebFluxLogFilter;
import ch.bedag.dap.hellodata.cloudbeaver.gateway.repository.UserRepository;
import ch.bedag.dap.hellodata.cloudbeaver.gateway.security.CbJwtAuthenticationConverter;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

@Log4j2
@EnableRetry
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    public static final String ACCESS_TOKEN_COOKIE_NAME = "auth.access_token";
    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    private static final List<String> CORS_ALLOWED_HEADERS =
            List.of("Access-Control-Allow-Methods", "Access-Control-Allow-Origin", AUTHORIZATION_HEADER_NAME, "Access-Control-Allow-Headers", "Origin,Accept", "X-Requested-With",
                    "Content-Type", "Access-Control-Request-Method", "Access-Control-Request-Headers");

    private final Environment env;

    @Value("${hello-data.cors.allowed-origins}")
    private String allowedOrigins;

    @PostConstruct
    public void init() {
        log.info("[CORS configuration] List of allowed origins {}", allowedOrigins);
        log.info("[CORS configuration] List of allowed headers {}", CORS_ALLOWED_HEADERS);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, CbJwtAuthenticationConverter cbJwtAuthenticationConverter) {
        http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        http.addFilterBefore(webFluxLogFilter(), SecurityWebFiltersOrder.FIRST);
        http.addFilterBefore(new TokenAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION);
        http.addFilterAfter(new AuthWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION);
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(cbJwtAuthenticationConverter))));
        configureCsrf(http);
        configureCors(http);
        http.headers(headerSpec -> headerSpec.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable)); // disabled to allow embedding into "portal"
        return http.build();
    }

    private void configureCsrf(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable); //cloudbeaver has it's own
    }

    private void configureCors(ServerHttpSecurity http) {
        if (env.matchesProfiles("disable-cors")) {
            http.cors(ServerHttpSecurity.CorsSpec::disable);
        } else {
            List<String> allowedOriginList = Arrays.stream(allowedOrigins.split(",")).toList();
            http.cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration corsConfig = new CorsConfiguration();
                if (!allowedOriginList.isEmpty()) {
                    for (String allowedOrigin : allowedOriginList) {
                        corsConfig.addAllowedOrigin(allowedOrigin);
                    }
                } else {
                    corsConfig.addAllowedOrigin("*"); //NOSONAR
                }
                corsConfig.addAllowedMethod("GET");
                corsConfig.addAllowedMethod("POST");
                corsConfig.addAllowedMethod("PUT");
                corsConfig.addAllowedMethod("DELETE");
                corsConfig.addAllowedMethod("PATCH");
                corsConfig.addAllowedMethod("HEAD");
                corsConfig.addAllowedMethod("OPTIONS");

                corsConfig.setAllowedHeaders(CORS_ALLOWED_HEADERS);
                corsConfig.setMaxAge(3600L);
                corsConfig.setExposedHeaders(List.of("xsrf-token"));
                return corsConfig;
            }));
        }
    }

    private Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter(CbJwtAuthenticationConverter cbJwtAuthenticationConverter) {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(cbJwtAuthenticationConverter);
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    /**
     * Map authorities (which are synced into the database) with the currently logged in user.
     *
     * @return a {@link ReactiveOAuth2UserService} that has the groups from the IdP.
     */
    @Bean
    public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(UserRepository userRepository) {
        final OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();

        return userRequest ->
                // Delegate to the default implementation for loading a user
                delegate.loadUser(userRequest).flatMap(oidcUser -> {
                    log.debug("Loading user {}", oidcUser == null ? "unknown" : oidcUser.getEmail());
                    assert oidcUser != null;
                    return userRepository.findOneWithPermissionsByEmail(oidcUser.getEmail());
                }).flatMap(dbUser -> {
                    log.debug("--> Loaded roles ... {}", dbUser.getAuthorities());
                    return Mono.just(dbUser.getAuthorities());
                }).flatMap(rolesForUser -> {
                    Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
                    log.debug("--> mapping authorities... {}", rolesForUser);
                    for (String role : rolesForUser) {
                        mappedAuthorities.add(new SimpleGrantedAuthority(role.toUpperCase(Locale.ENGLISH)));
                    }
                    return Mono.just(new DefaultOidcUser(mappedAuthorities, userRequest.getIdToken()));
                });
    }

    @Bean
    public WebFilter webFluxLogFilter() {
        return new WebFluxLogFilter();
    }

    @Bean
    public GlobalFilter loggingFilter() {
        return new LoggingFilter();
    }
}
