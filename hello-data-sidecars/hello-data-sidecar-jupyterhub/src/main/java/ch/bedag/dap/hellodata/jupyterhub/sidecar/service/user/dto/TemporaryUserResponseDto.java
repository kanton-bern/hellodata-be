package ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TemporaryUserResponseDto {
    private String username;
    private String password;
    private String host;
    private int port;
    private String databaseName;
    private LocalDateTime expiryDate;
}
