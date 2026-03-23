/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.portal.csv.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Log4j2
@Service
public class BatchExportScheduler {

    static final String BACKUP_FOLDER = "backup";
    static final String FILE_PREFIX = "batch-users-export-";
    static final String FILE_EXTENSION = ".bak";
    static final int RETENTION_DAYS = 7;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BatchExportService batchExportService;
    private final Path backupDirectory;

    public BatchExportScheduler(BatchExportService batchExportService,
                                @Value("${hello-data.batch-users-file.location}") String batchUsersFileLocation) {
        this.batchExportService = batchExportService;
        this.backupDirectory = Path.of(batchUsersFileLocation, BACKUP_FOLDER);
    }

    @Scheduled(cron = "0 0 23 * * *")
    public void exportBackup() {
        try {
            log.info("Starting scheduled batch users backup");
            Files.createDirectories(backupDirectory);

            String csvContent = batchExportService.generateBatchExportCsv();
            String fileName = FILE_PREFIX + LocalDate.now().format(DATE_FORMAT) + FILE_EXTENSION;
            Path backupFile = backupDirectory.resolve(fileName);

            Files.writeString(backupFile, csvContent, StandardCharsets.UTF_8);
            log.info("Batch users backup written to {}", backupFile);

            cleanupOldBackups();
        } catch (Exception e) {
            log.error("Failed to create batch users backup", e);
        }
    }

    void cleanupOldBackups() {
        try (Stream<Path> files = Files.list(backupDirectory)) {
            LocalDate cutoff = LocalDate.now().minusDays(RETENTION_DAYS);
            files.filter(this::isBackupFile)
                    .filter(path -> getBackupDate(path).isBefore(cutoff))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("Deleted old backup: {}", path.getFileName());
                        } catch (IOException e) {
                            log.warn("Failed to delete old backup: {}", path.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to list backup directory for cleanup", e);
        }
    }

    private boolean isBackupFile(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_EXTENSION);
    }

    private LocalDate getBackupDate(Path path) {
        String name = path.getFileName().toString();
        String dateStr = name.substring(FILE_PREFIX.length(), name.length() - FILE_EXTENSION.length());
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (Exception e) {
            // If date can't be parsed, treat as very old so it gets cleaned up
            return LocalDate.MIN;
        }
    }
}
