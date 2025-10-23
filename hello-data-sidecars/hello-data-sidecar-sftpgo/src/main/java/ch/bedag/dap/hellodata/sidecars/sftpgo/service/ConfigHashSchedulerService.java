package ch.bedag.dap.hellodata.sidecars.sftpgo.service;

import ch.bedag.dap.hellodata.sidecars.sftpgo.config.S3ConnectionsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Log4j2
@Service
@RequiredArgsConstructor
public class ConfigHashSchedulerService {

    public static final String SFTPGO_CONFIG_MD_5 = "/.sftpgo.config.md5";

    private final S3ConnectionsConfig s3ConnectionsConfig;

    public boolean hashChanged() {
        String currentHash = s3ConnectionsConfig.computeMd5Hash();
        String storedHash = loadStoredHash();
        if (storedHash == null) {
            log.warn("No hash found for current configuration, saving current hash. {}", currentHash);
            storeHash(currentHash);
        } else if (!currentHash.equals(storedHash)) {
            log.info("Configuration hash changed. Previous: {}, Current: {}", storedHash, currentHash);
            storeHash(currentHash);
            return true;
        } else {
            log.debug("Configuration hash unchanged.");
        }
        return false;
    }

    private void storeHash(String newHash) {
        String hashFilePath = s3ConnectionsConfig.getAdminVirtualFolder() + SFTPGO_CONFIG_MD_5;
        Path hashPath = Path.of(hashFilePath);
        try {
            Files.createDirectories(hashPath.getParent());
            Files.writeString(hashPath, newHash, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Saved new configuration hash {} to {}", newHash, hashFilePath);
        } catch (IOException e) {
            log.error("Failed to save configuration hash to {}", hashFilePath, e);
        }
    }

    private String loadStoredHash() {
        String hashFilePath = s3ConnectionsConfig.getAdminVirtualFolder() + SFTPGO_CONFIG_MD_5;
        Path hashPath = Path.of(hashFilePath);
        try {
            if (Files.exists(hashPath)) {
                return Files.readString(hashPath).trim();
            }
        } catch (IOException e) {
            log.warn("Failed to read hash file: {}", hashFilePath, e);
        }
        return null;
    }

}
