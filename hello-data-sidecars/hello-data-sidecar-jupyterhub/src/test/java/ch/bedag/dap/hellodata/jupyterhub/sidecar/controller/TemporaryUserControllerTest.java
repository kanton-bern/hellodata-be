package ch.bedag.dap.hellodata.jupyterhub.sidecar.controller;

import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user.TemporaryUserService;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user.dto.TemporaryUserResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {
        TemporaryUserController.class,
        TemporaryUserControllerTest.TestSecurityConfig.class
})
class TemporaryUserControllerTest {

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }

        @Bean
        public UserDetailsService users() {
            UserDetails user = User.builder()
                    .username("user")
                    .password("{noop}password")
                    .roles("USER")
                    .build();
            return new InMemoryUserDetailsManager(user);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TemporaryUserService temporaryUserService;

    private TemporaryUserResponseDto responseDto;

    @BeforeEach
    void setUp() {
        responseDto = new TemporaryUserResponseDto();
        responseDto.setUsername("temp_user_" + UUID.randomUUID().toString().substring(0, 8));
        responseDto.setPassword("password123");
        responseDto.setExpiryDate(LocalDateTime.now().plusDays(1));
        responseDto.setDatabaseName("test");
        responseDto.setPort(1234);
        responseDto.setHost("localhost");
    }

    @Test
    void createTemporaryUser_ShouldReturnTemporaryUserResponseDto() throws Exception {
        when(temporaryUserService.createTemporaryUser()).thenReturn(responseDto);

        mockMvc.perform(
                        get("/create-temporary-user")
                                .with(httpBasic("user", "password"))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.username").value(responseDto.getUsername()))
                .andExpect(jsonPath("$.password").value(responseDto.getPassword()))
                .andExpect(jsonPath("$.expiryDate").isNotEmpty())
                .andExpect(jsonPath("$.host").value(responseDto.getHost()))
                .andExpect(jsonPath("$.port").value(responseDto.getPort()))
                .andExpect(jsonPath("$.databaseName").value(responseDto.getDatabaseName()));
    }

    @Test
    void createTemporaryUser_ShouldReturn401_WhenNoAuthProvided() throws Exception {
        mockMvc.perform(get("/create-temporary-user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
