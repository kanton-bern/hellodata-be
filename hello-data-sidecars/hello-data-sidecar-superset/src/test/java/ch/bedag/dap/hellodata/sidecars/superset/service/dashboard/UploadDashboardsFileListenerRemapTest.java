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
package ch.bedag.dap.hellodata.sidecars.superset.service.dashboard;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardUpload;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link UploadDashboardsFileListener} remaps every dataset of an export to the target database.
 * Superset names the datasets folder after {@code secure_filename(database_name)} ('BI Editor' -> 'BI_Editor'),
 * so datasets must not be matched by the raw database name - otherwise they keep the source database uuid,
 * Superset skips them on import and fails on native filters with {@code KeyError: '<dataset uuid>'}.
 */
class UploadDashboardsFileListenerRemapTest {

    private static final int TARGET_DB_ID = 1;
    private static final String TARGET_DB_NAME = "bvd_gsdm_dwh";
    private static final String TARGET_DB_UUID = "a56a824b-a40f-4207-a040-ae2829f1e4c1";
    private static final String SOURCE_DB_UUID = "65cea7bc-6ab7-4fea-b32c-99a0e212ee4f";
    private static final String DATASET_UUID = "c94c656a-f38a-47a9-a14d-443cb77852be";

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @TempDir
    Path tmp;

    @Test
    void remapsDatasetsWhenDatabaseNameIsNotAValidFileName() throws Throwable {
        File zip = createExportZip("BI Editor", SOURCE_DB_UUID, "BI_Editor");

        invokeRemap(mockTargetDatabase(TARGET_DB_NAME, TARGET_DB_UUID), zip);

        Map<String, Map<String, Object>> entries = readYamlEntries(zip);
        assertThat(entries).containsOnlyKeys(
                "dashboard_export/databases/" + TARGET_DB_NAME + ".yaml",
                "dashboard_export/datasets/" + TARGET_DB_NAME + "/my_table.yaml");
        assertThat(entries.get("dashboard_export/databases/" + TARGET_DB_NAME + ".yaml"))
                .containsEntry("database_name", TARGET_DB_NAME)
                .containsEntry("uuid", TARGET_DB_UUID);
        assertThat(entries.get("dashboard_export/datasets/" + TARGET_DB_NAME + "/my_table.yaml"))
                .containsEntry("uuid", DATASET_UUID)
                .containsEntry("database_uuid", TARGET_DB_UUID);
    }

    @Test
    void remapsWhenDatabaseNameMatchesButUuidDiffers() throws Throwable {
        File zip = createExportZip(TARGET_DB_NAME, SOURCE_DB_UUID, TARGET_DB_NAME);

        invokeRemap(mockTargetDatabase(TARGET_DB_NAME, TARGET_DB_UUID), zip);

        Map<String, Map<String, Object>> entries = readYamlEntries(zip);
        assertThat(entries.get("dashboard_export/databases/" + TARGET_DB_NAME + ".yaml")).containsEntry("uuid", TARGET_DB_UUID);
        assertThat(entries.get("dashboard_export/datasets/" + TARGET_DB_NAME + "/my_table.yaml")).containsEntry("database_uuid", TARGET_DB_UUID);
    }

    @Test
    void leavesZipUntouchedWhenDatabaseAlreadyMatchesTarget() throws Throwable {
        File zip = createExportZip(TARGET_DB_NAME, TARGET_DB_UUID, TARGET_DB_NAME);
        byte[] before = Files.readAllBytes(zip.toPath());

        invokeRemap(mockTargetDatabase(TARGET_DB_NAME, TARGET_DB_UUID), zip);

        assertThat(Files.readAllBytes(zip.toPath())).isEqualTo(before);
    }

    private SupersetClient mockTargetDatabase(String name, String uuid) throws Exception {
        SupersetClient supersetClient = mock(SupersetClient.class);
        JsonArray databases = new JsonArray();
        JsonObject db = new JsonObject();
        db.addProperty("id", TARGET_DB_ID);
        db.addProperty("database_name", name);
        databases.add(db);
        when(supersetClient.listDatabases()).thenReturn(databases);

        JsonObject detail = new JsonObject();
        detail.addProperty("database_name", name);
        detail.addProperty("uuid", uuid);
        when(supersetClient.getDatabaseById(TARGET_DB_ID)).thenReturn(detail);
        return supersetClient;
    }

    private File createExportZip(String databaseName, String databaseUuid, String databaseFileName) throws IOException {
        File zip = tmp.resolve("dashboard-export.zip").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            writeEntry(zos, "dashboard_export/databases/" + databaseFileName + ".yaml",
                    "database_name: " + databaseName + "\nuuid: " + databaseUuid + "\n");
            writeEntry(zos, "dashboard_export/datasets/" + databaseFileName + "/my_table.yaml",
                    "table_name: my_table\nschema: udm\nuuid: " + DATASET_UUID + "\ndatabase_uuid: " + databaseUuid + "\n");
        }
        return zip;
    }

    private void writeEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> readYamlEntries(File zip) throws IOException {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        try (ZipFile zipFile = new ZipFile(zip)) {
            for (ZipEntry entry : java.util.Collections.list(zipFile.entries())) {
                result.put(entry.getName(), yamlMapper.readValue(zipFile.getInputStream(entry), Map.class));
            }
        }
        return result;
    }

    private void invokeRemap(SupersetClient supersetClient, File zip) throws Throwable {
        UploadDashboardsFileListener listener = new UploadDashboardsFileListener(null, null, null, null);
        Field tmpDir = UploadDashboardsFileListener.class.getDeclaredField("tmpDir");
        tmpDir.setAccessible(true);
        tmpDir.set(listener, tmp.toString());
        DashboardUpload upload = new DashboardUpload();
        upload.setFilename("dashboard-export.zip");
        Method method = UploadDashboardsFileListener.class.getDeclaredMethod("remapDatabaseInZip", SupersetClient.class, File.class, DashboardUpload.class);
        method.setAccessible(true);
        try {
            method.invoke(listener, supersetClient, zip, upload);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
