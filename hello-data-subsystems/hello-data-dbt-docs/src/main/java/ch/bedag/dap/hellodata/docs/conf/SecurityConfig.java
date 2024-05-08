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
package ch.bedag.dap.hellodata.docs.conf;

import ch.bedag.dap.hellodata.docs.repository.UserRepository;
import ch.bedag.dap.hellodata.docs.security.DbtDocsJwtAuthenticationConverter;
import ch.bedag.dap.hellodata.docs.security.DocsAuthorizationManager;
import ch.bedag.dap.hellodata.docs.service.KeycloakLogoutHandler;
import ch.bedag.dap.hellodata.docs.service.SecurityService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;
import static org.springframework.security.config.Customizer.withDefaults;

@Log4j2
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "auth.access_token";
    private static final String HD_AUTH_REQUEST_PARAM = "auth.access_token";
    private static final String HD_AUTH_FORWARDED_ACCESS_TOKEN = "X-Forwarded-Access-Token";

    private static final List<String> CORS_ALLOWED_HEADERS =
            List.of("Access-Control-Allow-Methods", "Access-Control-Allow-Origin", "Authorization", "Access-Control-Allow-Headers", "Origin,Accept", "X-Requested-With",
                    "Content-Type", "Access-Control-Request-Method", "Access-Control-Request-Headers");

    private static final String[] AUTH_WHITELIST = {
            // -- Swagger UI v2
            "/v2/api-docs", "/swagger-resources", "/swagger-resources/**", "/configuration/ui", "/configuration/security", "/swagger-ui.html", "/webjars/**",
            // -- Swagger UI v3 (OpenAPI)
            "/v3/api-docs/**", "/swagger-ui/**", "/",
            // other public endpoints of your API may be appended to this array
            "/error",
            // actuator endpoints
            "/actuator/**" };
    private final KeycloakLogoutHandler keycloakLogoutHandler;
    private final Environment env;

    @Value("${hello-data.cors.allowed-origins}")
    private String allowedOrigins;

    public static String extractAccessTokenFromReferer(String refererHeader) {
        if (refererHeader == null) {
            return null;
        }
        // Parse the referer URL
        URI refererUri = URI.create(refererHeader);

        // Build URI components from the referer URL
        UriComponents uriComponents = UriComponentsBuilder.newInstance().uri(refererUri).build();

        // Extract the query parameters
        MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
        List<String> accessTokenValues = queryParams.get(HD_AUTH_REQUEST_PARAM);

        if (accessTokenValues != null && !accessTokenValues.isEmpty()) {
            return accessTokenValues.get(0); // Return the first access token value
        }

        return null; // Access token not found in the referer URL
    }

    @PostConstruct
    public void init() {
        log.info("[CORS configuration] List of allowed origins {}", allowedOrigins);
        log.info("[CORS configuration] List of allowed headers {}", CORS_ALLOWED_HEADERS);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DocsAuthorizationManager authorizationManager, DbtDocsJwtAuthenticationConverter authenticationConverter) throws
            Exception {
        http.headers(headerSpec -> headerSpec.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)); // disabled to allow embedding into "portal"
        configureCors(http);
        configureCsrf(http);
        http.authorizeHttpRequests(auth -> {
            AntPathRequestMatcher[] matchers = new AntPathRequestMatcher[AUTH_WHITELIST.length];
            for (int i = 0; i < AUTH_WHITELIST.length; i++) {
                matchers[i] = new AntPathRequestMatcher(AUTH_WHITELIST[i]);
            }
            auth.requestMatchers(matchers).permitAll();
            auth.requestMatchers(new AntPathRequestMatcher("/**", "OPTIONS")).permitAll();
        });

        http.oauth2Login(withDefaults());
        http.logout(httpSecurityLogoutConfigurer -> httpSecurityLogoutConfigurer.addLogoutHandler(keycloakLogoutHandler).logoutSuccessUrl("/"));
        http.oauth2ResourceServer(httpSecurityOAuth2ResourceServerConfigurer -> {
            httpSecurityOAuth2ResourceServerConfigurer.jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(authenticationConverter));
            httpSecurityOAuth2ResourceServerConfigurer.bearerTokenResolver(this::tokenExtractor);
        });

        http.authorizeHttpRequests(authorize -> authorize.anyRequest().access(authorizationManager));
        return http.build();
    }

    public String tokenExtractor(HttpServletRequest request) {
        String accessTokenFromReferer = extractAccessTokenFromReferer(request.getHeader("referer"));
        if (accessTokenFromReferer != null) {
            return accessTokenFromReferer;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            return header.replace("Bearer ", "");
        }

        // required in combination with the oauth2-proxy component, which passes along auth headers
        header = request.getHeader(HD_AUTH_FORWARDED_ACCESS_TOKEN);
        if (header != null) {
            return header;
        }

        Cookie cookie = WebUtils.getCookie(request, ACCESS_TOKEN_COOKIE_NAME);
        if (cookie != null) {
            return cookie.getValue();
        }
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String paramName = params.nextElement();
            if (paramName.equalsIgnoreCase(HD_AUTH_REQUEST_PARAM)) {
                return request.getParameter(paramName);
            }
        }
        return null;
    }

    private void configureCors(HttpSecurity http) throws Exception {
        if (env.matchesProfiles("disable-cors")) {
            http.cors(cors -> cors.disable());
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

    private void configureCsrf(HttpSecurity http) throws Exception {
        if (env.matchesProfiles("disable-csrf")) {
            http.csrf(csrf -> csrf.disable());//NOSONAR
        } else {
            http.csrf(withDefaults());
        }
    }

    @Bean
    DocsAuthorizationManager authorizationManager(SecurityService securityService) {
        return new DocsAuthorizationManager(securityService);
    }

    @Bean
    DbtDocsJwtAuthenticationConverter authenticationConverter(UserRepository userRepository) {
        return new DbtDocsJwtAuthenticationConverter(userRepository);
    }
}
