package ch.bedag.dap.hellodata.jupyterhub.sidecar.controller;

import ch.bedag.dap.hellodata.commons.nats.actuator.NatsHealthIndicator;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user.TemporaryUserService;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user.dto.TemporaryUserResponseDto;
import io.nats.client.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TemporaryUserController.class)
@ExtendWith(MockitoExtension.class)
class TemporaryUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemporaryUserService temporaryUserService;
    @MockBean
    private Connection connection;
    @MockBean
    private NatsHealthIndicator natsHealthIndicator;

    private TemporaryUserResponseDto responseDto;

    @BeforeEach
    void setUp() {
        // Set up a mock response
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
        // Arrange
        when(temporaryUserService.createTemporaryUser()).thenReturn(responseDto);

        // Format expiryDate to match the precision of the JSON serialization
        String formattedExpiryDate = responseDto.getExpiryDate().withNano((responseDto.getExpiryDate().getNano() / 1000) * 1000).toString();

        // Act and Assert
        mockMvc.perform(
                        get("/create-temporary-user")
                                .with(httpBasic("user", "password"))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.username").value(responseDto.getUsername()))
                .andExpect(jsonPath("$.password").value(responseDto.getPassword()))
                .andExpect(jsonPath("$.expiryDate").value(formattedExpiryDate))
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