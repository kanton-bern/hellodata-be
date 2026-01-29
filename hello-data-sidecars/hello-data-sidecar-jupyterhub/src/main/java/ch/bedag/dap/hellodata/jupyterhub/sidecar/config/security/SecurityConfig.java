package ch.bedag.dap.hellodata.jupyterhub.sidecar.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    @Value("${security.user.username}")
    private String userUsername;
    @Value("${security.user.password}")
    private String userPassword;
    @Value("${security.api.username}")
    private String apiUsername;
    @Value("${security.api.password}")
    private String apiPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) //NOSONAR
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("USER", "API")
                                .requestMatchers("/api/**").hasAnyRole("USER", "API")
                                .requestMatchers("/swagger-ui/**").hasAnyRole("USER")
                                .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public UserDetailsService users() {
        // The builder will ensure the passwords are encoded before saving in memory
        UserDetails user = User.builder()
                .username(userUsername)
                .password(userPassword)
                .passwordEncoder(passwordEncoder()::encode)
                .roles("USER")
                .build();
        UserDetails admin = User.builder()
                .username(apiUsername)
                .password(apiPassword)
                .passwordEncoder(passwordEncoder()::encode)
                .roles("API")
                .build();
        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
