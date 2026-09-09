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

import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemporaryUserServiceTest {

    private static final String SHARED_ROLE = "hd_jupyter_users";
    private static final String SHARED_SCHEMA = "jupyter_shared";

    @Mock
    private HellodataJupyterhubProperties hellodataProperties;

    @Mock
    private JdbcTemplate dwhJdbcTemplate;

    @InjectMocks
    private TemporaryUserService temporaryUserService;

    private List<String> dwhTempUserSchemas;

    @BeforeEach
    void setUp() {
        dwhTempUserSchemas = List.of("schema1", "schema2");

        when(hellodataProperties.getTempUserPasswordValidInDays()).thenReturn(7);
        when(hellodataProperties.getDwhUrl()).thenReturn("jdbc:postgresql://localhost:5432/testdwh");
        when(hellodataProperties.getDwhTempUserSchemas()).thenReturn(dwhTempUserSchemas);
        when(hellodataProperties.getDwhSharedRole()).thenReturn(SHARED_ROLE);
        when(hellodataProperties.getDwhSharedSchema()).thenReturn(SHARED_SCHEMA);
    }

    @Test
    void createTemporaryUser_ShouldCreateUserAsMemberOfSharedRole() {
        TemporaryUserResponseDto responseDto = temporaryUserService.createTemporaryUser();

        assertNotNull(responseDto);
        assertNotNull(responseDto.getUsername());
        assertNotNull(responseDto.getPassword());
        assertNotNull(responseDto.getExpiryDate());
        assertTrue(responseDto.getUsername().matches("temp_user_\\w{8}"));
        assertFalse(responseDto.getPassword().isEmpty());

        String username = responseDto.getUsername();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedExpiryDate = responseDto.getExpiryDate().format(formatter);

        // user is created with an expiring password
        verify(dwhJdbcTemplate).execute(startsWith("CREATE USER " + username));
        verify(dwhJdbcTemplate).execute(contains("VALID UNTIL '" + formattedExpiryDate + "'"));

        // access flows through the shared role, and the user runs each session as it
        verify(dwhJdbcTemplate).execute("GRANT " + SHARED_ROLE + " TO " + username);
        verify(dwhJdbcTemplate).execute("ALTER ROLE " + username + " SET role TO " + SHARED_ROLE);

        // the user itself receives no direct object privileges - everything is on the shared role
        verify(dwhJdbcTemplate, never()).execute(contains("GRANT ALL PRIVILEGES ON DATABASE testdwh TO " + username));
        verify(dwhJdbcTemplate, never()).execute(contains("ON ALL TABLES IN SCHEMA schema1 TO " + username));
    }

    @Test
    void createTemporaryUser_ShouldEnsureSharedRoleAndReadGrants() {
        temporaryUserService.createTemporaryUser();

        // shared role provisioned idempotently
        verify(dwhJdbcTemplate).execute(contains("CREATE ROLE " + SHARED_ROLE + " NOLOGIN"));
        verify(dwhJdbcTemplate).execute("GRANT CONNECT ON DATABASE testdwh TO " + SHARED_ROLE);

        // read-only grants on the modelled schemas go to the shared role, not the user
        for (String schema : dwhTempUserSchemas) {
            verify(dwhJdbcTemplate).execute("GRANT USAGE ON SCHEMA " + schema + " TO " + SHARED_ROLE);
            verify(dwhJdbcTemplate).execute("GRANT SELECT ON ALL TABLES IN SCHEMA " + schema + " TO " + SHARED_ROLE);
            verify(dwhJdbcTemplate).execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA " + schema + " TO " + SHARED_ROLE);
        }

        // shared read-write workspace owned via the shared role
        verify(dwhJdbcTemplate).execute("CREATE SCHEMA IF NOT EXISTS " + SHARED_SCHEMA);
        verify(dwhJdbcTemplate).execute("GRANT USAGE, CREATE ON SCHEMA " + SHARED_SCHEMA + " TO " + SHARED_ROLE);
    }
}
