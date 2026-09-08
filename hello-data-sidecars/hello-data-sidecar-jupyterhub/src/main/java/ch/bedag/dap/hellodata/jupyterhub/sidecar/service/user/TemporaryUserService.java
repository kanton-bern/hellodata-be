package ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user;

import ch.bedag.dap.hellodata.jupyterhub.sidecar.config.props.HellodataJupyterhubProperties;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user.dto.TemporaryUserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class TemporaryUserService {

    private final JdbcTemplate dwhJdbcTemplate;
    private final HellodataJupyterhubProperties hellodataProperties;

    // Not @Transactional: in PostgreSQL a failed statement aborts the transaction, blocking the rest.
    @Scheduled(cron = "0 0 0 * * ?") // Runs at midnight every day
    public void cleanupExpiredUsers() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String findExpiredUsersSql = "SELECT usename FROM pg_catalog.pg_user WHERE valuntil < '" + now.format(formatter) + "' AND usename LIKE 'temp_user_%'";
        List<String> expiredUsernames = dwhJdbcTemplate.queryForList(findExpiredUsersSql, String.class); //NOSONAR

        for (String username : expiredUsernames) {
            dropExpiredUser(username);
        }
    }

    private void dropExpiredUser(String username) {
        try {
            // REASSIGN first: DROP OWNED deletes the objects the user owns, not just its grants.
            String reassignOwnedSql = String.format("REASSIGN OWNED BY %s TO CURRENT_USER", username);
            dwhJdbcTemplate.execute(reassignOwnedSql); //NOSONAR

            String dropOwnedSql = String.format("DROP OWNED BY %s", username);
            dwhJdbcTemplate.execute(dropOwnedSql); //NOSONAR

            String dropUserSql = String.format("DROP USER IF EXISTS %s", username);
            dwhJdbcTemplate.execute(dropUserSql); //NOSONAR
            log.info("Dropped expired user: {}", username);
        } catch (DataAccessException e) {
            log.error("Could not drop expired user {}", username, e);
        }
    }

    @Transactional
    public TemporaryUserResponseDto createTemporaryUser() {
        String username = "temp_user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(hellodataProperties.getTempUserPasswordValidInDays());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String createUserSql = String.format("CREATE USER %s WITH PASSWORD '%s' VALID UNTIL '%s'", username, password, expiryDate.format(formatter));
        dwhJdbcTemplate.execute(createUserSql);

        TemporaryUserResponseDto responseDto = createResponseDto(hellodataProperties.getDwhUrl());
        String grantAccessSql = String.format("GRANT ALL PRIVILEGES ON DATABASE %s TO %s", responseDto.getDatabaseName(), username);
        dwhJdbcTemplate.execute(grantAccessSql);

        List<String> dwhTempUserSchemas = hellodataProperties.getDwhTempUserSchemas();
        for (String schema : dwhTempUserSchemas) {
            String grantSchemaAccessSql = String.format("GRANT USAGE ON SCHEMA %s TO %s", schema, username);
            dwhJdbcTemplate.execute(grantSchemaAccessSql);

            String grantAllTablesAccessSql = String.format("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA %s TO %s", schema, username);
            dwhJdbcTemplate.execute(grantAllTablesAccessSql);

            String grantAllSequencesAccessSql = String.format("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA %s TO %s", schema, username);
            dwhJdbcTemplate.execute(grantAllSequencesAccessSql);
        }

        log.debug("Username: {}, Password: {}", username, password);
        responseDto.setPassword(password);
        responseDto.setUsername(username);
        responseDto.setExpiryDate(expiryDate);
        return responseDto;
    }

    private TemporaryUserResponseDto createResponseDto(String jdbcUrl) {
        try {
            URI uri = new URI(jdbcUrl.replace("jdbc:", ""));
            String host = uri.getHost();
            int port = uri.getPort();
            String databaseName = uri.getPath().replaceFirst("/", "");
            // Remove query parameters if present
            int questionMarkIndex = databaseName.indexOf('?');
            if (questionMarkIndex != -1) {
                databaseName = databaseName.substring(0, questionMarkIndex);
            }
            TemporaryUserResponseDto responseDto = new TemporaryUserResponseDto();
            responseDto.setDatabaseName(databaseName);
            responseDto.setHost(host);
            responseDto.setPort(port);
            return responseDto;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + jdbcUrl);
        }
    }
}
