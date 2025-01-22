package ch.bedag.dap.hellodata.sidecars.sftpgo.config;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Log4j2
@Data
@Validated
@ConfigurationProperties("hello-data")
public class S3ConnectionsConfig {

    private List<S3Connection> s3Connections;

    public S3Connection getS3Connection(String contextKey) {
        return s3Connections.stream().filter(c -> c.contextKey.equals(contextKey))
                .findFirst().orElseThrow(() -> new RuntimeException(String.format("No s3 config for data domain: %s", contextKey)));
    }

    @Data
    public static class S3Connection {
        private String contextKey;
        private String endpoint;
        private String bucket;
        private String region;
        private String accessKey;
        private String accessSecret;
        private boolean forcePathStyle;
    }

}
