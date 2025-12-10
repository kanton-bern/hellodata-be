package ch.bedag.dap.hellodata.sidecars.sftpgo.config;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Data
@Validated
@ConfigurationProperties("hello-data")
public class S3ConnectionsConfig {

    private List<S3Connection> s3Connections;
    private String adminVirtualFolder;
    private SftpGo sftpGo;

    public S3Connection getS3Connection(String contextKey) {
        return s3Connections.stream().filter(c -> c.contextKey.equals(contextKey))
                .findFirst().orElseThrow(() -> new RuntimeException(String.format("No s3 config for data domain: %s, available contexts: %s",
                        contextKey, s3Connections.stream().map(S3Connection::getContextKey).collect(Collectors.joining(", ")))));
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

    @Data
    public static class SftpGo {
        private String baseUrl;
        private String adminUsername;
        private String adminPassword;
        private boolean viewerDisabled;
    }

    public String computeMd5Hash(String contextKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            // combine all relevant properties into a single string
            String combined = (s3Connections == null ? "" :
                    s3Connections.stream()
                            .filter(connection -> connection.contextKey.equals(contextKey))
                            .map(c -> String.join("|",
                                    c.getContextKey(),
                                    c.getEndpoint(),
                                    c.getBucket(),
                                    c.getRegion(),
                                    c.getAccessKey(),
                                    c.getAccessSecret(),
                                    String.valueOf(c.isForcePathStyle())))
                            .collect(Collectors.joining(";")))
                    + "|" + (adminVirtualFolder != null ? adminVirtualFolder : "")
                    + "|" + (sftpGo != null ? String.join("|",
                    sftpGo.getBaseUrl(),
                    sftpGo.getAdminUsername(),
                    sftpGo.getAdminPassword(),
                    String.valueOf(sftpGo.isViewerDisabled())) : "");

            // compute MD5 hash
            byte[] digest = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute MD5 hash", e); //NOSONAR
        }
    }
}
