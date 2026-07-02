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

import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadDashboardsFileListenerReconcileTest {

    private static final int TARGET_DB_ID = 3;
    private static final String TARGET_DB_UUID = "db-uuid";
    private static final String SCHEMA = "csv";
    private static final String TABLE_NAME = "epolice_may";
    private static final String IMPORT_UUID = "a8f5dc54-0a07-4dc4-9636-aec221dd015f";
    private static final String EXISTING_UUID = "e983ffd6-84bd-4afe-a25b-9834da2d5b69";
    private static final int EXISTING_DATASET_ID = 75;

    @Test
    void deletesOrphanedConflictingDataset() throws Throwable {
        SupersetClient supersetClient = mock(SupersetClient.class);
        mockTargetDatabase(supersetClient);
        when(supersetClient.getDatasetsBySchemaAndTable(SCHEMA, TABLE_NAME))
                .thenReturn(existingDatasets(EXISTING_DATASET_ID, EXISTING_UUID, TARGET_DB_ID));
        // orphaned: no charts reference it
        when(supersetClient.getChartsForDatasource(EXISTING_DATASET_ID)).thenReturn(new JsonArray());

        invokeReconcile(supersetClient, createExportZip(SCHEMA, TABLE_NAME, IMPORT_UUID));

        verify(supersetClient).deleteDataset(EXISTING_DATASET_ID);
    }

    @Test
    void failsFastWhenConflictingDatasetIsInUse() throws Throwable {
        SupersetClient supersetClient = mock(SupersetClient.class);
        mockTargetDatabase(supersetClient);
        when(supersetClient.getDatasetsBySchemaAndTable(SCHEMA, TABLE_NAME))
                .thenReturn(existingDatasets(EXISTING_DATASET_ID, EXISTING_UUID, TARGET_DB_ID));
        JsonArray referencingCharts = new JsonArray();
        referencingCharts.add(new JsonObject());
        when(supersetClient.getChartsForDatasource(EXISTING_DATASET_ID)).thenReturn(referencingCharts);

        File zip = createExportZip(SCHEMA, TABLE_NAME, IMPORT_UUID);
        assertThatThrownBy(() -> invokeReconcile(supersetClient, zip))
                .isInstanceOf(UploadDashboardsFileException.class)
                .hasMessageContaining("csv.epolice_may")
                .hasMessageContaining("different identity");

        verify(supersetClient, never()).deleteDataset(anyInt());
    }

    @Test
    void keepsDatasetWithMatchingUuid() throws Throwable {
        SupersetClient supersetClient = mock(SupersetClient.class);
        mockTargetDatabase(supersetClient);
        // existing dataset has the SAME uuid as the import -> importer overwrites in place
        when(supersetClient.getDatasetsBySchemaAndTable(SCHEMA, TABLE_NAME))
                .thenReturn(existingDatasets(EXISTING_DATASET_ID, IMPORT_UUID, TARGET_DB_ID));

        invokeReconcile(supersetClient, createExportZip(SCHEMA, TABLE_NAME, IMPORT_UUID));

        verify(supersetClient, never()).deleteDataset(anyInt());
        verify(supersetClient, never()).getChartsForDatasource(anyInt());
    }

    @Test
    void ignoresSameNameDatasetOnDifferentDatabase() throws Throwable {
        SupersetClient supersetClient = mock(SupersetClient.class);
        mockTargetDatabase(supersetClient);
        // same schema/table but on a different database -> not the slot this import targets
        when(supersetClient.getDatasetsBySchemaAndTable(SCHEMA, TABLE_NAME))
                .thenReturn(existingDatasets(EXISTING_DATASET_ID, EXISTING_UUID, TARGET_DB_ID + 1));

        invokeReconcile(supersetClient, createExportZip(SCHEMA, TABLE_NAME, IMPORT_UUID));

        verify(supersetClient, never()).deleteDataset(anyInt());
        verify(supersetClient, never()).getChartsForDatasource(anyInt());
    }

    private void mockTargetDatabase(SupersetClient supersetClient) throws Exception {
        JsonArray databases = new JsonArray();
        JsonObject db = new JsonObject();
        db.addProperty("id", TARGET_DB_ID);
        db.addProperty("database_name", "dwh");
        databases.add(db);
        when(supersetClient.listDatabases()).thenReturn(databases);

        JsonObject detail = new JsonObject();
        detail.addProperty("database_name", "dwh");
        detail.addProperty("uuid", TARGET_DB_UUID);
        when(supersetClient.getDatabaseById(TARGET_DB_ID)).thenReturn(detail);
    }

    private JsonArray existingDatasets(int id, String uuid, int databaseId) {
        JsonObject dataset = new JsonObject();
        dataset.addProperty("id", id);
        dataset.addProperty("uuid", uuid);
        dataset.addProperty("schema", SCHEMA);
        dataset.addProperty("table_name", TABLE_NAME);
        JsonObject database = new JsonObject();
        database.addProperty("id", databaseId);
        dataset.add("database", database);
        JsonArray result = new JsonArray();
        result.add(dataset);
        return result;
    }

    private File createExportZip(String schema, String tableName, String uuid) throws IOException {
        File zip = File.createTempFile("dashboard-export-", ".zip");
        zip.deleteOnExit();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            zos.putNextEntry(new ZipEntry("dashboard_export/datasets/dwh/" + tableName + ".yaml"));
            String content = "table_name: " + tableName + "\nschema: " + schema + "\nuuid: " + uuid + "\n";
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return zip;
    }

    private void invokeReconcile(SupersetClient supersetClient, File zip) throws Throwable {
        UploadDashboardsFileListener listener = new UploadDashboardsFileListener(null, null, null, null);
        Method method = UploadDashboardsFileListener.class.getDeclaredMethod("reconcileConflictingDatasets", SupersetClient.class, File.class);
        method.setAccessible(true);
        try {
            method.invoke(listener, supersetClient, zip);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
