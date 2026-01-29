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

import jakarta.servlet.*;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.security.web.debug.DebugFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.filter.CompositeFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Used to disable keycloak resource server and use custom one to mock jwt token
 */
@Configuration
@EnableWebSecurity(debug = true)
@EnableMethodSecurity(prePostEnabled = true)
public class TestSecurityConfig {

    @Bean
    static BeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor() {
        return registry -> registry.getBeanDefinition(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME).setBeanClassName(CompositeFilterChainProxy.class.getName());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable);
        http.addFilterBefore(accessTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        Converter<OffsetDateTime, LocalDateTime> offsetToLocal =
                ctx -> ctx.getSource() == null ? null : ctx.getSource().toLocalDateTime();
        mapper.addConverter(offsetToLocal);

        Converter<OffsetDateTime, Long> offsetToEpochMilli =
                ctx -> ctx.getSource() == null ? null : ctx.getSource().toInstant().toEpochMilli();
        mapper.addConverter(offsetToEpochMilli);
        return new ModelMapper();
    }

    @Bean
    public TestJwtProvider testJwtProvider() {
        return new TestJwtProvider();
    }

    @Bean
    public TestJwtAuthorizationFilter accessTokenFilter() {
        return new TestJwtAuthorizationFilter(testJwtProvider());
    }

    @Bean
    public FilterRegistrationBean<TestJwtAuthorizationFilter> accessTokenFilterRegistrationBean() {
        FilterRegistrationBean<TestJwtAuthorizationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(accessTokenFilter());
        registrationBean.addUrlPatterns("/*"); // Apply the filter to all requests
        return registrationBean;
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
