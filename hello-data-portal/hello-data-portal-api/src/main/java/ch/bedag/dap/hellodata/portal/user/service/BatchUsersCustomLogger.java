package ch.bedag.dap.hellodata.portal.user.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Log4j2
@Component
public class BatchUsersCustomLogger {

    private static final String FILE_NAME = "users-output.log";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Value("${hello-data.batch-users-file.location}")
    private String logDirectory;
    private String logFilePath;

    @PostConstruct
    public void init() {
        logFilePath = logDirectory + File.separator + FILE_NAME;
        try {
            Path dir = Paths.get(logDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir); // Ensure directory exists
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory: " + logDirectory, e);
        }
    }

    public void logMessage(String message) {
        File logFile = new File(logFilePath);

        // Check if the file size exceeds the limit
        if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
            try {
                // Reset the log file by clearing its contents
                new FileWriter(logFilePath, false).close();
                log.info("Log file reset because it exceeded 50MB.");
            } catch (IOException e) {
                throw new RuntimeException("Failed to reset log file: " + logFilePath, e);
            }
        }

        // Add timestamp to the log message
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = timestamp + " - " + message;

        // Append the log message to the file
        try (FileWriter writer = new FileWriter(logFilePath, true)) {
            writer.write(logEntry + System.lineSeparator());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to log file: " + logFilePath, e);
        }
    }
}
