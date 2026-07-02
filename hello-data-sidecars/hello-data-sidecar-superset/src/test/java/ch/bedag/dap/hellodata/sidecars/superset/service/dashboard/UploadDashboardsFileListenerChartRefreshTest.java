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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UploadDashboardsFileListenerChartRefreshTest {

    @Test
    void buildChartImportZipCopiesAssetsAndSetsSliceMetadata() throws Exception {
        File exportZip = createDashboardExportZip(true);
        File chartsZip = (File) invoke("buildChartImportZip", newListener(), new Class[]{File.class}, exportZip);

        assertThat(chartsZip).isNotNull();
        Set<String> entries = zipEntryNames(chartsZip);
        assertThat(entries)
                .contains("dashboard_export/charts/a.yaml",
                        "dashboard_export/datasets/dwh/epolice.yaml",
                        "dashboard_export/databases/dwh.yaml",
                        "dashboard_export/metadata.yaml")
                // the dashboards/ folder must not be part of a chart import
                .doesNotContain("dashboard_export/dashboards/dash.yaml");

        String metadata = readEntry(chartsZip, "dashboard_export/metadata.yaml");
        Map<String, Object> parsed = new ObjectMapper(new YAMLFactory()).readValue(metadata, Map.class);
        assertThat(parsed.get("type")).isEqualTo("Slice");
    }

    @Test
    void buildChartImportZipReturnsNullWhenNoCharts() throws Exception {
        File exportZip = createDashboardExportZip(false);
        File chartsZip = (File) invoke("buildChartImportZip", newListener(), new Class[]{File.class}, exportZip);

        assertThat(chartsZip).isNull();
    }

    @Test
    void refreshChartsInPlaceImportsChartsWithOverwrite() throws Exception {
        SupersetClient supersetClient = mock(SupersetClient.class);
        File exportZip = createDashboardExportZip(true);

        invoke("refreshChartsInPlace", newListener(),
                new Class[]{SupersetClient.class, File.class, JsonObject.class},
                supersetClient, exportZip, new JsonObject());

        verify(supersetClient).importCharts(any(File.class), any(), eq(true));
    }

    @Test
    void refreshChartsInPlaceSkipsWhenNoCharts() throws Exception {
        SupersetClient supersetClient = mock(SupersetClient.class);
        File exportZip = createDashboardExportZip(false);

        invoke("refreshChartsInPlace", newListener(),
                new Class[]{SupersetClient.class, File.class, JsonObject.class},
                supersetClient, exportZip, new JsonObject());

        verify(supersetClient, org.mockito.Mockito.never()).importCharts(any(), any(), org.mockito.Mockito.anyBoolean());
    }

    private UploadDashboardsFileListener newListener() throws Exception {
        UploadDashboardsFileListener listener = new UploadDashboardsFileListener(null, null, null, null);
        Field tmpDir = UploadDashboardsFileListener.class.getDeclaredField("tmpDir");
        tmpDir.setAccessible(true);
        tmpDir.set(listener, System.getProperty("java.io.tmpdir"));
        return listener;
    }

    private Object invoke(String methodName, Object target, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = UploadDashboardsFileListener.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private File createDashboardExportZip(boolean withChart) throws IOException {
        File zip = File.createTempFile("dashboard-export-", ".zip");
        zip.deleteOnExit();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            writeEntry(zos, "dashboard_export/metadata.yaml", "version: 1.0.0\ntype: Dashboard\n");
            writeEntry(zos, "dashboard_export/databases/dwh.yaml", "uuid: db-uuid\n");
            writeEntry(zos, "dashboard_export/datasets/dwh/epolice.yaml", "uuid: ds-uuid\n");
            writeEntry(zos, "dashboard_export/dashboards/dash.yaml", "uuid: dash-uuid\n");
            if (withChart) {
                writeEntry(zos, "dashboard_export/charts/a.yaml", "uuid: chart-uuid\n");
            }
        }
        return zip;
    }

    private void writeEntry(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private Set<String> zipEntryNames(File zip) throws IOException {
        Set<String> names = new HashSet<>();
        try (ZipFile zipFile = new ZipFile(zip)) {
            zipFile.entries().asIterator().forEachRemaining(e -> names.add(e.getName()));
        }
        return names;
    }

    private String readEntry(File zip, String entryName) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip); InputStream in = zipFile.getInputStream(zipFile.getEntry(entryName))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
