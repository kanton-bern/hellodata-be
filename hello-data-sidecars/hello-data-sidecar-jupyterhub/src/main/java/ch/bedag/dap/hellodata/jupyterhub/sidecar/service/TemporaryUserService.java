package ch.bedag.dap.hellodata.jupyterhub.sidecar.service;

import ch.bedag.dap.hellodata.jupyterhub.sidecar.config.props.HellodataJupyterhubProperties;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.dto.TemporaryUserResponseDto;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class TemporaryUserService {

    private final JdbcTemplate dwhJdbcTemplate;
    private final HellodataJupyterhubProperties hellodataProperties;

    @Transactional
    public TemporaryUserResponseDto createTemporaryUser() {
        String username = "temp_user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(hellodataProperties.getTempUserPasswordValidInDays());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String createUserSql = String.format(
                "CREATE USER %s WITH PASSWORD '%s' VALID UNTIL '%s'",
                username, password, expiryDate.format(formatter)
                                            );

        dwhJdbcTemplate.execute(createUserSql);
        TemporaryUserResponseDto responseDto = createResponseDto(hellodataProperties.getDwhUrl());
        String grantAccessSql = String.format("GRANT ALL PRIVILEGES ON DATABASE %s TO %s", responseDto.getDatabaseName(), username);
        dwhJdbcTemplate.execute(grantAccessSql);
        log.debug("Username: {}, Password: {}", username, password);
        responseDto.setPassword(password);
        responseDto.setUsername(username);
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
