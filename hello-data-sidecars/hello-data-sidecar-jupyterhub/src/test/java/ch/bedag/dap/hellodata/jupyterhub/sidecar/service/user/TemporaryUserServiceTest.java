package ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user;

import ch.bedag.dap.hellodata.jupyterhub.sidecar.config.props.HellodataJupyterhubProperties;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user.dto.TemporaryUserResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemporaryUserServiceTest {

    @Mock
    private HellodataJupyterhubProperties hellodataProperties;

    @Mock
    private JdbcTemplate dwhJdbcTemplate;

    @InjectMocks
    private TemporaryUserService temporaryUserService;

    private int tempUserPasswordValidInDays;
    private String dwhUrl;
    private List<String> dwhTempUserSchemas;

    @BeforeEach
    void setUp() {
        tempUserPasswordValidInDays = 7;
        dwhUrl = "jdbc:testDatabaseUrl";
        dwhTempUserSchemas = List.of("schema1", "schema2");

        when(hellodataProperties.getTempUserPasswordValidInDays()).thenReturn(tempUserPasswordValidInDays);
        when(hellodataProperties.getDwhUrl()).thenReturn(dwhUrl);
        when(hellodataProperties.getDwhTempUserSchemas()).thenReturn(dwhTempUserSchemas);
    }

    @Test
    void createTemporaryUser_ShouldCreateUserAndGrantAccess() {
        // given when
        TemporaryUserResponseDto responseDto = temporaryUserService.createTemporaryUser();

        // then
        assertNotNull(responseDto);
        assertNotNull(responseDto.getUsername());
        assertNotNull(responseDto.getPassword());
        assertNotNull(responseDto.getExpiryDate());

        String expectedUsernamePattern = "temp_user_\\w{8}";
        assertTrue(responseDto.getUsername().matches(expectedUsernamePattern));
        assertTrue(responseDto.getPassword().length() > 0);

        LocalDateTime expectedExpiryDate = LocalDateTime.now().plusDays(tempUserPasswordValidInDays);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedExpiryDate = expectedExpiryDate.format(formatter);

        verify(dwhJdbcTemplate).execute(startsWith("CREATE USER " + responseDto.getUsername()));
        verify(dwhJdbcTemplate).execute(contains("VALID UNTIL '" + formattedExpiryDate + "'"));

        // verify granting privileges on database
        verify(dwhJdbcTemplate).execute(contains("GRANT ALL PRIVILEGES ON DATABASE " + responseDto.getDatabaseName()));

        // verify schema access grants
        for (String schema : dwhTempUserSchemas) {
            verify(dwhJdbcTemplate).execute(contains("GRANT USAGE ON SCHEMA " + schema));
            verify(dwhJdbcTemplate).execute(contains("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA " + schema));
            verify(dwhJdbcTemplate).execute(contains("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA " + schema));
        }

    }
}