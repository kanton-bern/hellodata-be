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
package ch.bedag.dap.hellodata.monitoring.sba;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableAdminServer
@EnableScheduling
public class SBAdminApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(SBAdminApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application;
    }

    @Profile("insecure")
    @Configuration(proxyBeanMethods = false)
    public static class SecurityPermitAllConfig {

        private final AdminServerProperties adminServer;

        public SecurityPermitAllConfig(AdminServerProperties adminServer) {
            this.adminServer = adminServer;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests((authorizeRequests) -> authorizeRequests.anyRequest().permitAll())
                    .csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                            .ignoringRequestMatchers(new AntPathRequestMatcher(this.adminServer.path("/instances"), HttpMethod.POST.toString()),
                                    new AntPathRequestMatcher(this.adminServer.path("/instances/*"), HttpMethod.DELETE.toString()),
                                    new AntPathRequestMatcher(this.adminServer.path("/actuator/**"))));
            return http.build();
        }
    }

    @Profile("secure")
    @Configuration(proxyBeanMethods = false)
    public static class SecuritySecureConfig {

        private final AdminServerProperties adminServer;

        public SecuritySecureConfig(AdminServerProperties adminServer) {
            this.adminServer = adminServer;
        }

        @Bean
        protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
            successHandler.setTargetUrlParameter("redirectTo");
            successHandler.setDefaultTargetUrl(this.adminServer.path("/"));

            http.authorizeHttpRequests((authorizeRequests) -> authorizeRequests.requestMatchers(new AntPathRequestMatcher(this.adminServer.path("/assets/**")))
                            .permitAll()
                            .requestMatchers(new AntPathRequestMatcher(this.adminServer.path("/actuator/health/liveness")))
                            .permitAll()
                            .requestMatchers(new AntPathRequestMatcher(this.adminServer.path("/actuator/health/readiness")))
                            .permitAll()
                            .requestMatchers(new AntPathRequestMatcher(this.adminServer.path("/login")))
                            .permitAll()
                            .anyRequest()
                            .authenticated())

                    .formLogin((formLogin) -> formLogin.loginPage(this.adminServer.path("/login")).successHandler(successHandler))
                    .logout((logout) -> logout.logoutUrl(this.adminServer.path("/logout")))
                    .httpBasic(Customizer.withDefaults())
                    .csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                            .ignoringRequestMatchers(new AntPathRequestMatcher(this.adminServer.path("/instances"), HttpMethod.POST.toString()),
                                    new AntPathRequestMatcher(this.adminServer.path("/instances/*"), HttpMethod.DELETE.toString()),
                                    new AntPathRequestMatcher(this.adminServer.path("/actuator/**"))));

            return http.build();
        }
    }
}
