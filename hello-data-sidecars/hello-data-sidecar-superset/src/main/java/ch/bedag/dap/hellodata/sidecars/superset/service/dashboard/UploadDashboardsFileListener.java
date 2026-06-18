package ch.bedag.dap.hellodata.sidecars.superset.service.dashboard;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardUpload;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboardResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.client.exception.UnexpectedResponseException;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import ch.bedag.dap.hellodata.sidecars.superset.service.resource.DashboardResourceProviderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Log4j2
@Service
@RequiredArgsConstructor
public class UploadDashboardsFileListener {

    private static final String CHUNK_SUFFIX = ".tmp";
    private static final int FILE_BUFFER_SIZE = 1024 * 1024;
    private static final String FOLDER_NAMES_REGEX_PATTERN = "[^A-Za-z0-9\\-_]";
    private static final String JSON_KEY_ERRORS = "errors";
    private static final String JSON_KEY_MESSAGE = "message";
    private static final String JSON_KEY_EXTRA = "extra";
    private static final String JSON_KEY_ISSUE_CODES = "issue_codes";
    private static final String RAW_MESSAGE_PREFIX = "message=";
    private final Connection natsConnection;
    private final SupersetClientProvider supersetClientProvider;
    private final ObjectMapper objectMapper;
    private final DashboardResourceProviderService dashboardResourceProviderService;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Value("${hello-data.dashboard-export-check-script-location}")
    private String pythonExportCheckScriptLocation;

    @Value("${hello-data.dashboard-export-check-script-enabled}")
    private Boolean pythonExportCheckScriptEnabled;

    @Value("${hello-data.dashboard-import-default-sql-alchemy}")
    private String defaultSqlAlchemyUri;

    @Value("${hello-data.tmp-dir:/tmp}")
    private String tmpDir;

    @PostConstruct
    public void listenForRequests() {
        String supersetSidecarSubject = SlugifyUtil.slugify(instanceName + RequestReplySubject.UPLOAD_DASHBOARDS_FILE.getSubject());
        log.debug("/*-/*- Listening for messages on subject {}", supersetSidecarSubject);
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            log.debug("\t-=-=-=-= Received message from NATS: {}", new String(msg.getData()));
            String binaryFileId = null;
            try (SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance()) {
                DashboardUpload dashboardUpload = objectMapper.readValue(msg.getData(), DashboardUpload.class);
                saveChunk(dashboardUpload);
                File destinationFile;
                if (dashboardUpload.isLastChunk()) {
                    destinationFile =
                            File.createTempFile(StringUtils.isBlank(dashboardUpload.getFilename()) ? dashboardUpload.getBinaryFileId() : dashboardUpload.getFilename(), //NOSONAR
                                    "", new File(tmpDir)); //NOSONAR
                    log.debug("Created temp file for chunk {}", destinationFile);
                    binaryFileId = dashboardUpload.getBinaryFileId();
                    assembleChunks(binaryFileId, dashboardUpload.getFilename(), dashboardUpload.getChunkNumber(), dashboardUpload.getFileSize(), destinationFile.toPath());
                } else {
                    log.debug("Saved chunk, waiting for another one {}", dashboardUpload.getChunkNumber());
                    ackMessage(msg);
                    return;
                }
                useDefaultSqlAlchemyUri(dashboardUpload, destinationFile);
                remapDatabaseInZip(supersetClient, destinationFile, dashboardUpload);
                JsonObject passwordsObject = getPasswordsObject(destinationFile);
                File backupFile = null;
                boolean pruned = false;
                try {
                    if (dashboardUpload.isPruneChartsAndDatasets()) {
                        backupFile = backupDashboardIfExists(supersetClient, destinationFile);
                        if (backupFile != null) {
                            pruneExistingDashboardAssets(supersetClient, destinationFile);
                            pruned = true;
                        }
                    }
                    log.debug("Passwords parameter send to API ");
                    supersetClient.importDashboard(destinationFile, passwordsObject, true);
                    verifyImportedDashboards(supersetClient, destinationFile);
                } catch (Exception e) {
                    if (pruned && backupFile != null) {
                        log.error("Failed to import dashboard. Restoring from backup...", e);
                        try {
                            supersetClient.importDashboard(backupFile, passwordsObject, true);
                            log.info("Successfully restored dashboard from backup");
                        } catch (Exception restoreEx) {
                            log.error("Failed to restore dashboard from backup!", restoreEx);
                        }
                    }
                    throw e;
                } finally {
                    if (backupFile != null && backupFile.exists() && !backupFile.delete()) {
                        log.warn("Could not delete backup file: {}", backupFile.getAbsolutePath());
                    }
                }
                ackMessage(msg);
                dashboardResourceProviderService.publishDashboards();
            } catch (URISyntaxException | IOException | RuntimeException e) {
                log.error("Error uploading dashboards", e);
                String userFriendlyMessage = extractUserFriendlyErrorMessage(e);
                natsConnection.publish(msg.getReplyTo(), userFriendlyMessage.getBytes(StandardCharsets.UTF_8));
            } finally {
                if (binaryFileId != null) {
                    deleteTempBinaryFileData(binaryFileId);
                }
            }
        });
        dispatcher.subscribe(supersetSidecarSubject);
    }

    public void saveChunk(DashboardUpload chunk) throws IOException {
        Path uploadFolderPath = createTempFolder(chunk.getBinaryFileId());
        Path path = Paths.get(uploadFolderPath.toString(), chunk.getChunkNumber() + CHUNK_SUFFIX);
        Files.write(path, chunk.getContent());
    }

    private void useDefaultSqlAlchemyUri(DashboardUpload dashboardUpload, File destinationFile) throws IOException {
        File tempZip = File.createTempFile("modified-", dashboardUpload.getFilename(), new File(tmpDir)); //NOSONAR
        replaceSqlalchemyUrisInZip(destinationFile, tempZip, defaultSqlAlchemyUri);
        Files.move(tempZip.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void remapDatabaseInZip(SupersetClient supersetClient, File destinationFile, DashboardUpload dashboardUpload) throws IOException {
        TargetDatabase targetDb = resolveTargetDatabase(supersetClient);
        if (targetDb == null) {
            log.warn("No target database found in Superset, skipping database remapping");
            return;
        }
        log.info("Remapping database in zip to target: name='{}', uuid='{}'", targetDb.name, targetDb.uuid);

        File tempZip = File.createTempFile("remapped-", dashboardUpload.getFilename(), new File(tmpDir)); //NOSONAR
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        try (
                ZipFile zipFile = new ZipFile(destinationFile);
                FileOutputStream fos = new FileOutputStream(tempZip);
                ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)
        ) {
            // First pass: find the source database name from the zip
            String sourceDatabaseName = findSourceDatabaseName(zipFile);
            if (sourceDatabaseName == null) {
                log.warn("No database entry found in zip, skipping remapping");
                return;
            }

            if (sourceDatabaseName.equals(targetDb.name)) {
                log.info("Source database name '{}' already matches target, skipping remapping", sourceDatabaseName);
                return;
            }

            log.info("Remapping database from '{}' to '{}'", sourceDatabaseName, targetDb.name);

            Enumeration<? extends ZipEntry> entries = zipFile.entries(); //NOSONAR
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    if (entryName.contains("/databases/") && !entry.isDirectory()) {
                        // Remap database yaml: replace name, uuid, and rename file
                        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        Map<String, Object> parsed = yamlMapper.readValue(content, Map.class);
                        parsed.put("database_name", targetDb.name);
                        parsed.put("uuid", targetDb.uuid);

                        String updatedYaml = yamlMapper.writeValueAsString(parsed);
                        String newEntryName = entryName.replace("/databases/" + sourceDatabaseName + ".yaml",
                                "/databases/" + targetDb.name + ".yaml");
                        log.info("Renaming database entry: {} -> {}", entryName, newEntryName);

                        zos.putNextEntry(new ZipEntry(newEntryName));
                        zos.write(updatedYaml.getBytes(StandardCharsets.UTF_8));
                        zos.closeEntry();

                    } else if (entryName.contains("/datasets/" + sourceDatabaseName + "/") && !entry.isDirectory()) {
                        // Remap dataset: rename folder and update catalog field
                        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        Map<String, Object> parsed = yamlMapper.readValue(content, Map.class);
                        if (parsed.containsKey("catalog")) {
                            parsed.put("catalog", targetDb.name);
                        }
                        parsed.put("database_uuid", targetDb.uuid);

                        String updatedYaml = yamlMapper.writeValueAsString(parsed);
                        String newEntryName = entryName.replace("/datasets/" + sourceDatabaseName + "/",
                                "/datasets/" + targetDb.name + "/");
                        log.info("Remapping dataset entry: {} -> {}", entryName, newEntryName);

                        zos.putNextEntry(new ZipEntry(newEntryName));
                        zos.write(updatedYaml.getBytes(StandardCharsets.UTF_8));
                        zos.closeEntry();

                    } else {
                        // Copy other files as-is
                        zos.putNextEntry(new ZipEntry(entryName));
                        inputStream.transferTo(zos);
                        zos.closeEntry();
                    }
                }
            }
        }
        Files.move(tempZip.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private String findSourceDatabaseName(ZipFile zipFile) throws IOException {
        Enumeration<? extends ZipEntry> entries = zipFile.entries(); //NOSONAR
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().contains("/databases/") && !entry.isDirectory()) {
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, Object> parsed = yamlMapper.readValue(content, Map.class);
                    Object name = parsed.get("database_name");
                    if (name instanceof String) {
                        return (String) name;
                    }
                }
            }
        }
        return null;
    }

    private TargetDatabase resolveTargetDatabase(SupersetClient supersetClient) {
        try {
            JsonArray databases = supersetClient.listDatabases();
            if (databases == null || databases.isEmpty()) {
                log.warn("No databases found in Superset");
                return null;
            }

            // Filter out 'examples' database, pick the oldest (lowest id) remaining one
            JsonElement target = null;
            int lowestId = Integer.MAX_VALUE;
            for (JsonElement db : databases) {
                JsonObject dbObj = db.getAsJsonObject();
                String name = dbObj.has("database_name") ? dbObj.get("database_name").getAsString() : "";
                if ("examples".equalsIgnoreCase(name)) {
                    continue;
                }
                int id = dbObj.get("id").getAsInt();
                if (id < lowestId) {
                    lowestId = id;
                    target = db;
                }
            }

            if (target == null) {
                log.warn("No non-examples database found in Superset");
                return null;
            }

            // Fetch full details to get uuid
            JsonElement detail = supersetClient.getDatabaseById(lowestId);
            JsonObject detailObj = detail.getAsJsonObject();
            String name = detailObj.get("database_name").getAsString();
            String uuid = detailObj.has("uuid") && !detailObj.get("uuid").isJsonNull()
                    ? detailObj.get("uuid").getAsString() : null;

            if (uuid == null) {
                log.warn("Target database '{}' has no uuid, skipping remapping", name);
                return null;
            }

            return new TargetDatabase(name, uuid);
        } catch (Exception e) {
            log.error("Failed to resolve target database from Superset API", e);
            return null;
        }
    }

    private record TargetDatabase(String name, String uuid) {
    }

    private void replaceSqlalchemyUrisInZip(File sourceZip, File targetZip, String newSqlalchemyUri) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try (
                ZipFile zipFile = new ZipFile(sourceZip);
                FileOutputStream fos = new FileOutputStream(targetZip);
                ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)
        ) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries(); //NOSONAR

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    if (entryName.contains("/databases/") && !entry.isDirectory()) {
                        log.info("Replacing sqlalchemy_uri in: {} to {}", entryName, newSqlalchemyUri.substring(0, 30));

                        // Read and parse YAML
                        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        Map<String, Object> parsed = yamlMapper.readValue(content, Map.class);

                        // Replace sqlalchemy_uri
                        parsed.put("sqlalchemy_uri", newSqlalchemyUri);

                        // Convert back to YAML
                        String updatedYaml = yamlMapper.writeValueAsString(parsed);

                        // Add to output zip
                        ZipEntry newEntry = new ZipEntry(entryName);
                        zos.putNextEntry(newEntry);
                        zos.write(updatedYaml.getBytes(StandardCharsets.UTF_8));
                        zos.closeEntry();

                    } else {
                        // Copy other files as-is
                        ZipEntry newEntry = new ZipEntry(entryName);
                        zos.putNextEntry(newEntry);
                        inputStream.transferTo(zos);
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    private JsonObject getPasswordsObject(File destinationFile) throws IOException {
        JsonObject jsonObject = new JsonObject();
        try (ZipFile zipFile = new ZipFile(destinationFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries(); //NOSONAR
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String name = zipEntry.getName();
                if (name.contains("/databases/") && !zipEntry.isDirectory()) {
                    log.info("Reading database entry: {}", name);
                    try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                        Map<String, Object> parsed = yamlMapper.readValue(content, Map.class);
                        fetchSqlAlchemyUriUser(parsed, name, jsonObject);
                    }
                }
            }
        }
        return jsonObject;
    }

    private void fetchSqlAlchemyUriUser(Map<String, Object> parsed, String name, JsonObject jsonObject) {
        Object uriObj = parsed.get("sqlalchemy_uri");
        if (uriObj instanceof String sqlalchemyUri) {
            String username = extractUsernameFromSqlalchemyUri(sqlalchemyUri);
            if (username != null) {
                String envVarName = username.toUpperCase() + "_PASSWORD";
                //take password from environmental variable - has to be prepared earlier
                String password = System.getenv(envVarName);
                String databasePath = name.substring(name.indexOf("databases/"));
                if (password != null) {
                    log.info("Using password from env var '{}' for database '{}'", envVarName, name);
                    jsonObject.addProperty(databasePath, password);
                } else {
                    log.warn("Environment variable '{}' not set, skipping database '{}'", envVarName, name);
                    jsonObject.addProperty(databasePath, "dummy");
                }
            } else {
                log.warn("Could not extract username from URI: {}", sqlalchemyUri);
            }
        }
    }

    private String extractUsernameFromSqlalchemyUri(String uri) {
        try {
            String afterProtocol = uri.substring(uri.indexOf("://") + 3);
            int colonIndex = afterProtocol.indexOf(':');
            int atIndex = afterProtocol.indexOf('@');

            if (colonIndex != -1 && atIndex != -1 && colonIndex < atIndex) {
                return afterProtocol.substring(0, colonIndex);
            }
        } catch (Exception e) {
            log.error("Failed to parse sqlalchemy_uri: {}", uri, e);
        }
        return null;
    }

    private void ackMessage(Message msg) {
        natsConnection.publish(msg.getReplyTo(), "OK".getBytes(StandardCharsets.UTF_8));
        msg.ack();
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
        return Paths.get(tmpDir, "dashboards_upload", uploadFolder);
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
        if (BooleanUtils.isNotTrue(pythonExportCheckScriptEnabled)) {
            log.info("Python export check script disabled, skipping validation.");
            return;
        }
        // Command to execute Python script
        String[] cmd = {"python3", pythonExportCheckScriptLocation, "-i", destinationPath.toString()};
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

    private String extractUserFriendlyErrorMessage(Exception e) {
        if (e instanceof UnexpectedResponseException ure) {
            return extractMessageFromSupersetError(e.getMessage(), ure.getCode());
        }
        if (e instanceof UploadDashboardsFileException) {
            return e.getMessage();
        }
        return "Dashboard import failed: " + e.getMessage();
    }

    private String extractMessageFromSupersetError(String rawMessage, int statusCode) {
        try {
            String jsonPart = extractJsonFromRawMessage(rawMessage);
            if (jsonPart == null) {
                return rawMessage;
            }
            String extracted = parseErrorJson(jsonPart);
            if (extracted != null) {
                return extracted;
            }
        } catch (Exception parseException) {
            log.debug("Could not parse Superset error JSON, returning raw message", parseException);
        }
        return "Dashboard import failed (HTTP " + statusCode + ")";
    }

    private String extractJsonFromRawMessage(String rawMessage) {
        int messageStart = rawMessage.indexOf(RAW_MESSAGE_PREFIX);
        if (messageStart == -1) {
            return null;
        }
        return rawMessage.substring(messageStart + RAW_MESSAGE_PREFIX.length());
    }

    private String parseErrorJson(String jsonPart) {
        JsonElement jsonElement = JsonParser.parseString(jsonPart);
        if (!jsonElement.isJsonObject()) {
            return null;
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String errorsMessage = extractFromErrorsArray(jsonObject);
        if (errorsMessage != null) {
            return errorsMessage;
        }
        if (jsonObject.has(JSON_KEY_MESSAGE)) {
            return jsonObject.get(JSON_KEY_MESSAGE).getAsString();
        }
        return null;
    }

    private String extractFromErrorsArray(JsonObject jsonObject) {
        if (!jsonObject.has(JSON_KEY_ERRORS) || !jsonObject.get(JSON_KEY_ERRORS).isJsonArray()) {
            return null;
        }
        JsonArray errors = jsonObject.getAsJsonArray(JSON_KEY_ERRORS);
        if (errors.isEmpty()) {
            return null;
        }
        StringBuilder messages = new StringBuilder();
        for (JsonElement error : errors) {
            if (error.isJsonObject()) {
                appendErrorMessage(error.getAsJsonObject(), messages);
            }
        }
        return messages.isEmpty() ? null : messages.toString().trim();
    }

    private void appendErrorMessage(JsonObject errorObj, StringBuilder messages) {
        if (!errorObj.has(JSON_KEY_MESSAGE)) {
            return;
        }
        if (!messages.isEmpty()) {
            messages.append("; ");
        }
        messages.append(errorObj.get(JSON_KEY_MESSAGE).getAsString());
        appendIssueCodes(errorObj, messages);
    }

    private void appendIssueCodes(JsonObject errorObj, StringBuilder messages) {
        if (!errorObj.has(JSON_KEY_EXTRA) || !errorObj.get(JSON_KEY_EXTRA).isJsonObject()) {
            return;
        }
        JsonObject extra = errorObj.getAsJsonObject(JSON_KEY_EXTRA);
        if (!extra.has(JSON_KEY_ISSUE_CODES) || !extra.get(JSON_KEY_ISSUE_CODES).isJsonArray()) {
            return;
        }
        for (JsonElement issueCode : extra.getAsJsonArray(JSON_KEY_ISSUE_CODES)) {
            if (issueCode.isJsonObject() && issueCode.getAsJsonObject().has(JSON_KEY_MESSAGE)) {
                messages.append(" - ").append(issueCode.getAsJsonObject().get(JSON_KEY_MESSAGE).getAsString());
            }
        }
    }

    private List<String> findDashboardUuidsFromZip(File destinationFile) throws IOException {
        return findUuidsFromZip(destinationFile, "/dashboards/");
    }

    /**
     * Collects the UUIDs of all assets of a given type from a Superset export ZIP.
     *
     * @param pathSegment path segment identifying the asset folder, e.g. {@code "/dashboards/"},
     *                    {@code "/charts/"} or {@code "/datasets/"}
     */
    private List<String> findUuidsFromZip(File destinationFile, String pathSegment) throws IOException {
        List<String> uuids = new ArrayList<>();
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try (ZipFile zipFile = new ZipFile(destinationFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().contains(pathSegment) && !entry.isDirectory() && entry.getName().endsWith(".yaml")) {
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        Map<String, Object> parsed = yamlMapper.readValue(content, Map.class);
                        Object uuid = parsed.get("uuid");
                        if (uuid instanceof String) {
                            uuids.add((String) uuid);
                        }
                    }
                }
            }
        }
        return uuids;
    }

    private File backupDashboardIfExists(SupersetClient supersetClient, File destinationFile) {
        try {
            List<String> uuids = findDashboardUuidsFromZip(destinationFile);
            if (uuids.isEmpty()) {
                log.warn("No dashboard UUIDs found in zip, cannot backup");
                return null;
            }
            for (String uuid : uuids) {
                Integer existingDashboardId = findDashboardIdByUuid(supersetClient, uuid);
                if (existingDashboardId != null) {
                    log.info("Found existing dashboard ID {} for UUID {}, exporting backup...", existingDashboardId, uuid);
                    File backupFile = File.createTempFile("superset-dashboard-backup-", ".zip", new File(tmpDir));
                    return supersetClient.exportDashboard(existingDashboardId, backupFile);
                }
            }
        } catch (Exception e) {
            log.error("Failed to backup existing dashboard", e);
        }
        return null;
    }

    private Integer findDashboardIdByUuid(SupersetClient supersetClient, String uuid) {
        try {
            // Superset does not allow filtering dashboards by uuid (not in search_columns),
            // but uuid is returned in list_columns, so fetch id+uuid and match client-side.
            JsonArray columns = new JsonArray();
            columns.add("id");
            columns.add("uuid");

            SupersetDashboardResponse response = supersetClient.dashboards(columns, null);
            if (response != null && response.getResult() != null) {
                return response.getResult().stream()
                        .filter(dashboard -> uuid.equals(dashboard.getUuid()))
                        .map(SupersetDashboard::getId)
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.error("Error finding dashboard by UUID: {}", uuid, e);
        }
        return null;
    }

    private void pruneExistingDashboardAssets(SupersetClient supersetClient, File destinationFile) {
        try {
            List<String> uuids = findDashboardUuidsFromZip(destinationFile);
            // Assets whose UUID is present in the new export are kept: the import overwrites them
            // in place, preserving their IDs so that permalinks / dashboard comment pointers survive.
            Set<String> newChartUuids = new HashSet<>(findUuidsFromZip(destinationFile, "/charts/"));
            Set<String> newDatasetUuids = new HashSet<>(findUuidsFromZip(destinationFile, "/datasets/"));
            for (String uuid : uuids) {
                Integer existingDashboardId = findDashboardIdByUuid(supersetClient, uuid);
                if (existingDashboardId != null) {
                    log.info("Pruning assets for existing dashboard ID {}", existingDashboardId);

                    // 1. Get all charts of this dashboard
                    JsonArray charts = supersetClient.getDashboardCharts(existingDashboardId);
                    List<Integer> chartIdsToDelete = new ArrayList<>();
                    List<Integer> datasetIdsToCheck = new ArrayList<>();

                    for (JsonElement chartEl : charts) {
                        JsonObject chartObj = chartEl.getAsJsonObject();
                        int chartId = chartObj.get("id").getAsInt();

                        // Skip charts that are still part of the new export - they will be overwritten
                        // in place, keeping their IDs (and therefore any permalinks pointing to them).
                        String chartUuid = resolveChartUuid(supersetClient, chartId);
                        if (chartUuid != null && newChartUuids.contains(chartUuid)) {
                            log.info("Chart ID {} (UUID {}) is part of the new export. Keeping it for in-place overwrite.", chartId, chartUuid);
                            continue;
                        }

                        // Skip charts that are also used by other dashboards
                        if (isChartUsedByOtherDashboards(supersetClient, chartId, existingDashboardId)) {
                            log.info("Chart ID {} is used by other dashboards. Skipping deletion.", chartId);
                            continue;
                        }
                        chartIdsToDelete.add(chartId);

                        if (chartObj.has("datasource_id") && !chartObj.get("datasource_id").isJsonNull()) {
                            int datasourceId = chartObj.get("datasource_id").getAsInt();
                            String datasourceType = chartObj.has("datasource_type") && !chartObj.get("datasource_type").isJsonNull()
                                    ? chartObj.get("datasource_type").getAsString() : "";
                            if ("table".equalsIgnoreCase(datasourceType)) {
                                datasetIdsToCheck.add(datasourceId);
                            }
                        }
                    }

                    // 2. Delete the charts
                    if (!chartIdsToDelete.isEmpty()) {
                        log.info("Deleting {} charts for dashboard {}: {}", chartIdsToDelete.size(), existingDashboardId, chartIdsToDelete);
                        supersetClient.deleteCharts(chartIdsToDelete);
                    }

                    // 3. For each dataset, delete it only if it is not part of the new export and is not
                    //    referenced by any other chart.
                    for (Integer datasetId : datasetIdsToCheck) {
                        String datasetUuid = resolveDatasetUuid(supersetClient, datasetId);
                        if (datasetUuid != null && newDatasetUuids.contains(datasetUuid)) {
                            log.info("Dataset ID {} (UUID {}) is part of the new export. Keeping it for in-place overwrite.", datasetId, datasetUuid);
                            continue;
                        }

                        JsonArray referencingCharts = supersetClient.getChartsForDatasource(datasetId);
                        boolean referencedElsewhere = false;
                        for (JsonElement refChartEl : referencingCharts) {
                            JsonObject refChartObj = refChartEl.getAsJsonObject();
                            int refChartId = refChartObj.get("id").getAsInt();
                            if (!chartIdsToDelete.contains(refChartId)) {
                                referencedElsewhere = true;
                                break;
                            }
                        }

                        if (!referencedElsewhere) {
                            log.info("Dataset ID {} is not referenced by any other charts. Deleting dataset...", datasetId);
                            try {
                                supersetClient.deleteDataset(datasetId);
                            } catch (Exception e) {
                                log.error("Failed to delete dataset ID {}", datasetId, e);
                            }
                        } else {
                            log.info("Dataset ID {} is referenced by other charts. Skipping deletion.", datasetId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during pruneExistingDashboardAssets", e);
        }
    }

    private String resolveChartUuid(SupersetClient supersetClient, int chartId) {
        try {
            return supersetClient.getChartUuid(chartId);
        } catch (Exception e) {
            // On uncertainty, treat the chart as not matching the export so the existing
            // "used by other dashboards" guard still protects shared charts.
            log.warn("Could not determine UUID for chart ID {}. Treating it as not part of the new export.", chartId, e);
            return null;
        }
    }

    private String resolveDatasetUuid(SupersetClient supersetClient, int datasetId) {
        try {
            return supersetClient.getDatasetUuid(datasetId);
        } catch (Exception e) {
            log.warn("Could not determine UUID for dataset ID {}. Treating it as not part of the new export.", datasetId, e);
            return null;
        }
    }

    private boolean isChartUsedByOtherDashboards(SupersetClient supersetClient, int chartId, int currentDashboardId) {
        try {
            List<Integer> dashboardIds = supersetClient.getChartDashboardIds(chartId);
            return dashboardIds.stream().anyMatch(id -> id != currentDashboardId);
        } catch (Exception e) {
            // On uncertainty, do not delete the chart to avoid affecting other dashboards
            log.warn("Could not determine dashboards for chart ID {}. Treating it as shared and skipping deletion.", chartId, e);
            return true;
        }
    }

    private void verifyImportedDashboards(SupersetClient supersetClient, File destinationFile) throws IOException {
        List<String> uuids = findDashboardUuidsFromZip(destinationFile);
        if (uuids.isEmpty()) {
            log.warn("No dashboard UUIDs found in zip, skipping import verification");
            return;
        }
        for (String uuid : uuids) {
            Integer dashboardId = findDashboardIdByUuid(supersetClient, uuid);
            if (dashboardId == null) {
                throw new IllegalStateException("Import verification failed: dashboard with UUID " + uuid + " not found after import");
            }
            JsonArray charts;
            try {
                charts = supersetClient.getDashboardCharts(dashboardId);
            } catch (URISyntaxException | IOException e) {
                throw new IllegalStateException("Import verification failed: could not read charts for dashboard ID " + dashboardId, e);
            }
            if (charts == null || charts.isEmpty()) {
                throw new IllegalStateException("Import verification failed: dashboard ID " + dashboardId + " has no charts after import");
            }
            log.info("Import verification passed for dashboard ID {} (UUID {}) with {} chart(s)", dashboardId, uuid, charts.size());
        }
    }
}
