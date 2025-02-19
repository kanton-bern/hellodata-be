package ch.bedag.dap.hellodata.portal.user.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class BatchUsersCustomLogger {

    private static final String FILE_NAME = "users-output.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Value("${hello-data.batch-users-file.location}")
    private String logDirectory;
    private String logFilePath;

    @PostConstruct
    public void init() {
        logFilePath = logDirectory + File.separator + FILE_NAME;
        try {
            Files.createDirectories(Paths.get(logDirectory)); // Ensure directory exists
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory: " + logDirectory, e);
        }
    }

    public void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = timestamp + " - " + message;

        try (FileWriter writer = new FileWriter(logFilePath, true)) {
            writer.write(logEntry + System.lineSeparator());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to log file: " + logFilePath, e);
        }
    }
}
