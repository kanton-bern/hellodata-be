package ch.bedag.dap.hellodata.sidecars.superset.service.dashboard;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardUpload;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class UploadDashboardsFileListener {

    private static final String CHUNK_SUFFIX = ".tmp";
    private static final int FILE_BUFFER_SIZE = 1024 * 1024;
    private static final String FOLDER_NAMES_REGEX_PATTERN = "[^A-Za-z0-9\\-_]";
    private final Connection natsConnection;
    private final SupersetClientProvider supersetClientProvider;
    private final ObjectMapper objectMapper;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Value("${hello-data.dashboard-export-check-script-location}")
    private String pythonExportCheckScriptLocation;

    @PostConstruct
    public void listenForRequests() {
        String supersetSidecarSubject = SlugifyUtil.slugify(instanceName + RequestReplySubject.UPLOAD_DASHBOARDS_FILE.getSubject());
        log.debug("/*-/*- Listening for messages on subject {}", supersetSidecarSubject);
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            String binaryFileId = null;
            try {
                SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
                DashboardUpload dashboardUpload = objectMapper.readValue(msg.getData(), DashboardUpload.class);
                saveChunk(dashboardUpload);
                File destinationFile;
                if (dashboardUpload.isLastChunk()) {
                    destinationFile =
                            File.createTempFile(StringUtils.isBlank(dashboardUpload.getFilename()) ? dashboardUpload.getBinaryFileId() : dashboardUpload.getFilename(), //NOSONAR
                                                ""); //NOSONAR
                    log.debug("Created temp file for chunk {}", destinationFile);
                    binaryFileId = dashboardUpload.getBinaryFileId();
                    assembleChunks(binaryFileId, dashboardUpload.getFilename(), dashboardUpload.getChunkNumber(), dashboardUpload.getFileSize(), destinationFile.toPath());
                } else {
                    log.debug("Saved chunk, waiting for another one {}", dashboardUpload.getChunkNumber());
                    ackMessage(msg);
                    return;
                }
                JsonObject passwordsObject = getPasswordsObject(destinationFile);
                log.info("Passwords parameter send to API {}", new Gson().toJson(passwordsObject));
                supersetClient.importDashboard(destinationFile, passwordsObject, true);
                log.debug("\t-=-=-=-= received message from the superset: {}", new String(msg.getData()));
                ackMessage(msg);
            } catch (URISyntaxException | IOException | RuntimeException e) {
                log.error("Error uploading dashboards", e);
                natsConnection.publish(msg.getReplyTo(), e.getMessage().getBytes(StandardCharsets.UTF_8));
            } finally {
                if (binaryFileId != null) {
                    deleteTempBinaryFileData(binaryFileId);
                }
            }
        });
        dispatcher.subscribe(supersetSidecarSubject);
    }

    private JsonObject getPasswordsObject(File destinationFile) throws IOException {
        JsonObject jsonElement = new JsonObject();
        try (ZipFile zipFile = new ZipFile(destinationFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries(); //NOSONAR
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String name = zipEntry.getName();
                if (name.contains("/databases/")) {
                    jsonElement.addProperty(name.substring(name.indexOf("databases/")), "dummy");
                }
            }
        }
        return jsonElement;
    }

    private void ackMessage(Message msg) {
        natsConnection.publish(msg.getReplyTo(), "OK".getBytes(StandardCharsets.UTF_8));
        msg.ack();
    }

    public void saveChunk(DashboardUpload chunk) throws IOException {
        Path uploadFolderPath = createTempFolder(chunk.getBinaryFileId());
        Path path = Paths.get(uploadFolderPath.toString(), chunk.getChunkNumber() + CHUNK_SUFFIX);
        Files.write(path, chunk.getContent());
    }

    private Path createTempFolder(String filename) {
        Path uploadFolderPath = getUploadFolderPath(filename);
        if (!Files.exists(uploadFolderPath)) {
            File file = uploadFolderPath.toFile();
            boolean created = file.mkdirs();
            log.debug("File {} created: {}", file.toPath(), created);
        }
        return uploadFolderPath;
    }

    /**
     * Regex pattern replace all but numbers, letters, dashes and underscores.
     */
    private Path getUploadFolderPath(String filename) {
        String uploadFolder = filename.replaceAll(FOLDER_NAMES_REGEX_PATTERN, "");
        return Paths.get(System.getProperty("java.io.tmpdir"), "dashboards_upload", uploadFolder);
    }

    private void assembleChunks(String binaryFileId, String filename, long totalChunks, long fileSize, Path destinationPath) throws IOException {
        Path chunksFolderPath = getUploadFolderPath(binaryFileId);

        if (!Files.exists(chunksFolderPath)) {
            throw new UploadDashboardsFileException("No chunks were found for filename : " + filename);
        }

        List<File> chunks = listChunks(chunksFolderPath);
        if (chunks.isEmpty() || chunks.size() != totalChunks || validateChunkSizeWrong(fileSize, chunks)) {
            String errMsg =
                    "Chunks list empty? - " + chunks.isEmpty() + " Chunk size different than total size? - " + (chunks.size() != totalChunks) + " Chunk size different? - " +
                    validateChunkSizeWrong(fileSize, chunks);
            throw new UploadDashboardsFileException("Chunks validation failed. Upload canceled. " + errMsg);
        }
        writeChunksToFile(destinationPath, chunks);
        validateZipFile(destinationPath);
    }

    private void validateZipFile(Path destinationPath) throws IOException {
        // Command to execute Python script
        String[] cmd = { "python3", pythonExportCheckScriptLocation, "-i", destinationPath.toString() };
        log.info("Python cmd {}", StringUtils.join(cmd, " "));

        // Create ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder(cmd); //NOSONAR

        // Start the process
        Process process = pb.start();
        try (InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream()); BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                log.info(line);
                stringBuilder.append(line).append("\n");
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            log.info("Python script executed with exit code: " + exitCode);
            if (exitCode != 0) {
                throw new UploadDashboardsFileException("Python script validation error: \n" + stringBuilder);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UploadDashboardsFileException("Error validating file", e);
        } catch (IOException e) {
            throw new UploadDashboardsFileException("Error validating file", e);
        }
    }

    private boolean validateChunkSizeWrong(long fileSize, List<File> chunks) {
        long sum = chunks.stream().mapToLong(File::length).sum();
        boolean isDifferent = sum != fileSize;
        if (isDifferent) {
            log.error("Chunks size problem, should have {} but is {}", fileSize, sum);
        }
        return isDifferent;
    }

    private void deleteTempBinaryFileData(String filename) {
        Path chunksFolderPath = getUploadFolderPath(filename);
        if (Files.exists(chunksFolderPath)) {
            List<File> chunks = listChunks(chunksFolderPath);
            int nrFilesDeleted = 0;
            for (File file : listChunks(chunksFolderPath)) {
                if (file.delete()) { //NOSONAR
                    nrFilesDeleted++;
                }
            }

            if (nrFilesDeleted == chunks.size()) {
                try {
                    FileUtils.deleteDirectory(new File(chunksFolderPath.toString()));
                } catch (IOException e) {
                    log.error("Chunks folder could not be deleted", e);
                }
            }
        }
    }

    private List<File> listChunks(Path chunksFolderPath) {
        File folderFile = new File(chunksFolderPath.toString());
        File[] fileArray = folderFile.listFiles();
        List<File> files = new ArrayList<>(fileArray != null ? Arrays.asList(fileArray) : List.of());
        files.sort((File o1, File o2) -> {
            //remove extension
            String chunkName1 = o1.getName().split("\\.")[0];
            String chunkName2 = o2.getName().split("\\.")[0];
            Long chunk1Number = Long.parseLong(chunkName1);
            Long chunk2Number = Long.parseLong(chunkName2);
            return chunk1Number.compareTo(chunk2Number);
        });
        return files;
    }

    private void writeChunksToFile(Path destinationPath, List<File> chunks) throws IOException {
        log.info("Writing chunks to file {}", destinationPath);
        destinationPath.getParent().toFile().mkdirs();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(destinationPath.toString()), FILE_BUFFER_SIZE)) {
            for (File file : chunks) {
                try (InputStream in = new BufferedInputStream(new FileInputStream(file), FILE_BUFFER_SIZE)) {
                    byte[] buffer = new byte[FILE_BUFFER_SIZE];
                    int length = in.read(buffer);
                    while (length > 0) {
                        out.write(buffer, 0, length);
                        length = in.read(buffer);
                    }
                }
            }
        }
    }
}
