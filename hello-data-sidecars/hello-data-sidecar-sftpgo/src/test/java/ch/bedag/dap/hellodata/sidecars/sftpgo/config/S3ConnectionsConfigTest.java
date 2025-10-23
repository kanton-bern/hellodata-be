package ch.bedag.dap.hellodata.sidecars.sftpgo.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class S3ConnectionsConfigTest {
    @Test
    void computeMd5Hash_shouldGenerateConsistentHashForSameConfig() {
        S3ConnectionsConfig.S3Connection conn = new S3ConnectionsConfig.S3Connection();
        conn.setContextKey("domain1");
        conn.setEndpoint("https://s3.amazonaws.com");
        conn.setBucket("bucket1");
        conn.setRegion("eu-central-1");
        conn.setAccessKey("access");
        conn.setAccessSecret("secret");
        conn.setForcePathStyle(true);
        S3ConnectionsConfig.SftpGo sftpGo = new S3ConnectionsConfig.SftpGo();
        sftpGo.setBaseUrl("http://localhost:8090");
        sftpGo.setAdminUsername("admin");
        sftpGo.setAdminPassword("password");
        sftpGo.setViewerDisabled(false);
        S3ConnectionsConfig config1 = new S3ConnectionsConfig();
        config1.setS3Connections(List.of(conn));
        config1.setAdminVirtualFolder("/folder");
        config1.setSftpGo(sftpGo);

        S3ConnectionsConfig config2 = new S3ConnectionsConfig();
        config2.setS3Connections(List.of(conn));
        config2.setAdminVirtualFolder("/folder");
        config2.setSftpGo(sftpGo);

        String hash1 = config1.computeMd5Hash();
        String hash2 = config2.computeMd5Hash();

        assertNotNull(hash1);
        assertEquals(hash1, hash2, "Hashes for identical configurations should match");
    }

    @Test
    void computeMd5Hash_shouldChangeWhenConfigChanges() {
        S3ConnectionsConfig.S3Connection conn = new S3ConnectionsConfig.S3Connection();
        conn.setContextKey("domain1");
        conn.setEndpoint("https://s3.amazonaws.com");
        conn.setBucket("bucket1");
        conn.setRegion("eu-central-1");
        conn.setAccessKey("access");
        conn.setAccessSecret("secret");
        conn.setForcePathStyle(true);
        S3ConnectionsConfig.SftpGo sftpGo = new S3ConnectionsConfig.SftpGo();
        sftpGo.setBaseUrl("http://localhost:8090");
        sftpGo.setAdminUsername("admin");
        sftpGo.setAdminPassword("password");
        sftpGo.setViewerDisabled(false);
        S3ConnectionsConfig config = new S3ConnectionsConfig();
        config.setS3Connections(List.of(conn));
        config.setAdminVirtualFolder("/folder");
        config.setSftpGo(sftpGo);

        String originalHash = config.computeMd5Hash();

        // Modify config
        conn.setBucket("bucket2");
        String newHash = config.computeMd5Hash();

        assertNotEquals(originalHash, newHash, "Hashes should differ when configuration changes");
    }
}