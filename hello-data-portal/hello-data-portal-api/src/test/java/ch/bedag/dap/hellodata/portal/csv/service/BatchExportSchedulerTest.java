package ch.bedag.dap.hellodata.portal.csv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static ch.bedag.dap.hellodata.portal.csv.service.BatchExportScheduler.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class BatchExportSchedulerTest {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Mock
    private BatchExportService batchExportService;

    @TempDir
    Path tempDir;

    private BatchExportScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new BatchExportScheduler(batchExportService, tempDir.toString());
    }

    @Test
    void testExportBackupCreatesFile() throws IOException {
        String csvContent = "email;businessDomainRole;context;dataDomainRole;supersetRole\ntest@ex.com;NONE;ctx;NONE;\n";
        when(batchExportService.generateBatchExportCsv()).thenReturn(csvContent);

        scheduler.exportBackup();

        Path backupDir = tempDir.resolve(BACKUP_FOLDER);
        assertTrue(Files.exists(backupDir));

        String expectedFileName = FILE_PREFIX + LocalDate.now().format(DATE_FORMAT) + FILE_EXTENSION;
        Path expectedFile = backupDir.resolve(expectedFileName);
        assertTrue(Files.exists(expectedFile));
        assertEquals(csvContent, Files.readString(expectedFile, StandardCharsets.UTF_8));
    }

    @Test
    void testExportBackupCreatesBackupDirectory() {
        when(batchExportService.generateBatchExportCsv()).thenReturn("header\n");

        scheduler.exportBackup();

        assertTrue(Files.isDirectory(tempDir.resolve(BACKUP_FOLDER)));
    }

    @Test
    void testFileHasBakExtension() throws IOException {
        when(batchExportService.generateBatchExportCsv()).thenReturn("header\n");

        scheduler.exportBackup();

        try (var files = Files.list(tempDir.resolve(BACKUP_FOLDER))) {
            files.forEach(path -> assertTrue(path.toString().endsWith(FILE_EXTENSION),
                    "Backup file should have .bak extension: " + path.getFileName()));
        }
    }

    @Test
    void testCleanupRemovesOldBackups() throws IOException {
        Path backupDir = tempDir.resolve(BACKUP_FOLDER);
        Files.createDirectories(backupDir);

        // Create a file older than retention period
        String oldDate = LocalDate.now().minusDays(RETENTION_DAYS + 1).format(DATE_FORMAT);
        Path oldFile = backupDir.resolve(FILE_PREFIX + oldDate + FILE_EXTENSION);
        Files.writeString(oldFile, "old content");

        // Create a recent file
        String recentDate = LocalDate.now().minusDays(1).format(DATE_FORMAT);
        Path recentFile = backupDir.resolve(FILE_PREFIX + recentDate + FILE_EXTENSION);
        Files.writeString(recentFile, "recent content");

        scheduler.cleanupOldBackups();

        assertFalse(Files.exists(oldFile), "Old backup should be deleted");
        assertTrue(Files.exists(recentFile), "Recent backup should be kept");
    }

    @Test
    void testCleanupKeepsExactlyRetentionDayOldFile() throws IOException {
        Path backupDir = tempDir.resolve(BACKUP_FOLDER);
        Files.createDirectories(backupDir);

        // File exactly at retention boundary (7 days ago = cutoff, should NOT be deleted)
        String boundaryDate = LocalDate.now().minusDays(RETENTION_DAYS).format(DATE_FORMAT);
        Path boundaryFile = backupDir.resolve(FILE_PREFIX + boundaryDate + FILE_EXTENSION);
        Files.writeString(boundaryFile, "boundary content");

        scheduler.cleanupOldBackups();

        assertTrue(Files.exists(boundaryFile), "File at exact retention boundary should be kept");
    }

    @Test
    void testCleanupIgnoresNonBackupFiles() throws IOException {
        Path backupDir = tempDir.resolve(BACKUP_FOLDER);
        Files.createDirectories(backupDir);

        Path unrelatedFile = backupDir.resolve("some-other-file.csv");
        Files.writeString(unrelatedFile, "unrelated");

        scheduler.cleanupOldBackups();

        assertTrue(Files.exists(unrelatedFile), "Non-backup files should not be touched");
    }

    @Test
    void testExportDoesNotCrashWhenServiceFails() {
        when(batchExportService.generateBatchExportCsv()).thenThrow(new RuntimeException("DB down"));

        // Should not throw
        assertDoesNotThrow(() -> scheduler.exportBackup());
    }
}
