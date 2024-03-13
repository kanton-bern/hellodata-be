package ch.bedag.dap.hellodata.sidecars.superset.service.dashboard;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardUpload;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonPrimitive;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class UploadDashboardsFileListener {

    private static final String CHUNK_SUFFIX = ".tmp";
    private static final int FILE_BUFFER_SIZE = 1024 * 1024;
    private static final String FOLDER_NAMES_REGEX_PATTERN = "[^A-Za-z0-9\\-\\_]";
    private final Connection natsConnection;
    private final SupersetClientProvider supersetClientProvider;
    private final ObjectMapper objectMapper;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @PostConstruct
    public void listenForRequests() {
        String supersetSidecarSubject = SlugifyUtil.slugify(instanceName + RequestReplySubject.UPLOAD_DASHBOARDS_FILE.getSubject());
        log.debug("/*-/*- Listening for messages on subject {}", supersetSidecarSubject);
        Dispatcher dispatcher = natsConnection.createDispatcher((msg) -> {
            String binaryFileId = null;
            try {
                SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
                DashboardUpload dashboardUpload = objectMapper.readValue(msg.getData(), DashboardUpload.class);
                saveChunk(dashboardUpload);
                File contentFile;
                if (dashboardUpload.isLastChunk()) {
                    contentFile = File.createTempFile(dashboardUpload.getFilename(), "");
                    log.debug("Created temp file for chunk {}", contentFile);
                    binaryFileId = dashboardUpload.getBinaryFileId();
                    assembleChunks(binaryFileId, dashboardUpload.getFilename(), dashboardUpload.getChunkNumber(), dashboardUpload.getFileSize(), contentFile.toPath());
                } else {
                    log.debug("Saved chunk, waiting for another one {}", dashboardUpload.getChunkNumber());
                    ackMessage(msg);
                    return;
                }
                supersetClient.importDashboard(contentFile, new JsonPrimitive("{}"), true);
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
            throw new RuntimeException("No chunks were found for filename : " + filename);
        }

        List<File> chunks = listChunks(chunksFolderPath);
        if (chunks.isEmpty() || chunks.size() != totalChunks || validateChunkSize(fileSize, chunks)) {
            String errMsg =
                    "Chunks list empty? - " + chunks.isEmpty() + " Chunk size different than total size? - " + (chunks.size() != totalChunks) + " Chunk size different? - " +
                    validateChunkSize(fileSize, chunks);
            throw new RuntimeException("Chunks validation failed. Upload canceled. " + errMsg);
        }
        writeChunksToFile(destinationPath, chunks);
    }

    private boolean validateChunkSize(long fileSize, List<File> chunks) {
        return chunks.stream().mapToLong(File::length).sum() != fileSize;
    }

    private void deleteTempBinaryFileData(String filename) {
        Path chunksFolderPath = getUploadFolderPath(filename);
        if (Files.exists(chunksFolderPath)) {
            List<File> chunks = listChunks(chunksFolderPath);
            int nrFilesDeleted = 0;
            for (File file : listChunks(chunksFolderPath)) {
                if (file.delete()) {
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
        List<File> files = Arrays.asList(fileArray);
        Collections.sort(files, (File o1, File o2) -> {
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
