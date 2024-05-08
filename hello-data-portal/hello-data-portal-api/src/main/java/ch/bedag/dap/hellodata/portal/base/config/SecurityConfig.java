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
package ch.bedag.dap.hellodata.portal.base.config;

import ch.bedag.dap.hellodata.portal.user.service.KeycloakLogoutHandler;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import static org.springframework.security.config.Customizer.withDefaults;

@Log4j2
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String[] AUTH_WHITELIST = {
            // -- Swagger UI v2
            "/v2/api-docs", "/swagger-resources", "/swagger-resources/**", "/configuration/ui", "/configuration/security", "/swagger-ui.html", "/webjars/**",
            // -- Swagger UI v3 (OpenAPI)
            "/v3/api-docs/**", "/swagger-ui/**", "/",
            // other public endpoints of your API may be appended to this array
            "/actuator/**", };

    private static final List<String> CORS_ALLOWED_HEADERS =
            List.of("Access-Control-Allow-Methods", "Access-Control-Allow-Origin", "Authorization", "Access-Control-Allow-Headers", "Origin,Accept", "X-Requested-With",
                    "Content-Type", "Access-Control-Request-Method", "Access-Control-Request-Headers");

    private final Environment env;
    private final KeycloakLogoutHandler keycloakLogoutHandler;
    private final Converter hellodataAuthenticationConverter;

    @Value("${hello-data.cors.allowed-origins}")
    private String allowedOrigins;

    @PostConstruct
    public void init() {
        log.info("[CORS configuration] List of allowed origins {}", allowedOrigins);
        log.info("[CORS configuration] List of allowed headers {}", CORS_ALLOWED_HEADERS);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        configureFrameOptions(http);
        configureCors(http);
        configureCsrf(http);
        http.authorizeHttpRequests(auth -> {
            AntPathRequestMatcher[] matchers = new AntPathRequestMatcher[AUTH_WHITELIST.length];
            for (int i = 0; i < AUTH_WHITELIST.length; i++) {
                matchers[i] = new AntPathRequestMatcher(AUTH_WHITELIST[i]);
            }
            auth.requestMatchers(matchers).permitAll();
            auth.anyRequest().authenticated();
        });
        http.oauth2Login(withDefaults());
        http.logout(httpSecurityLogoutConfigurer -> httpSecurityLogoutConfigurer.addLogoutHandler(keycloakLogoutHandler).logoutSuccessUrl("/"));
        http.oauth2ResourceServer(httpSecurityOAuth2ResourceServerConfigurer -> httpSecurityOAuth2ResourceServerConfigurer.jwt(
                jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(hellodataAuthenticationConverter)));
        return http.build();
    }

    private void configureCsrf(HttpSecurity http) throws Exception {
        if (env.matchesProfiles("disable-csrf")) {
            http.csrf(csrf -> csrf.disable()); //NOSONAR
        } else {
            http.csrf(withDefaults());
        }
    }

    private void configureFrameOptions(HttpSecurity http) throws Exception {
        if (env.matchesProfiles("disable-frame-options")) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        } else {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny));//backend is not embedded into iframe
        }
    }

    private void configureCors(HttpSecurity http) throws Exception {
        if (env.matchesProfiles("disable-cors")) {
            http.cors(cors -> cors.disable()); //NOSONAR
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
}
