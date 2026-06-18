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

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboardResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadDashboardsFileListenerPruneTest {

    private static final int DASHBOARD_ID = 10;
    private static final String DASHBOARD_UUID = "dash-uuid";

    // charts a and b are kept (present in the new export), chart c is removed
    private static final int CHART_A_ID = 1;
    private static final int CHART_B_ID = 2;
    private static final int CHART_C_ID = 3;
    private static final String CHART_A_UUID = "chart-a-uuid";
    private static final String CHART_B_UUID = "chart-b-uuid";
    private static final String CHART_C_UUID = "chart-c-uuid";

    private static final int DATASET_KEPT_ID = 100; // present in export
    private static final int DATASET_ORPHAN_ID = 300; // used only by removed chart c, absent from export
    private static final String DATASET_KEPT_UUID = "dataset-kept-uuid";
    private static final String DATASET_ORPHAN_UUID = "dataset-orphan-uuid";

    @Test
    void prunesOnlyAssetsAbsentFromNewExport() throws Exception {
        SupersetClient supersetClient = mock(SupersetClient.class);
        mockDashboardLookup(supersetClient);

        JsonArray charts = new JsonArray();
        charts.add(chart(CHART_A_ID, DATASET_KEPT_ID));
        charts.add(chart(CHART_B_ID, DATASET_KEPT_ID));
        charts.add(chart(CHART_C_ID, DATASET_ORPHAN_ID));
        when(supersetClient.getDashboardCharts(DASHBOARD_ID)).thenReturn(charts);

        when(supersetClient.getChartUuid(CHART_A_ID)).thenReturn(CHART_A_UUID);
        when(supersetClient.getChartUuid(CHART_B_ID)).thenReturn(CHART_B_UUID);
        when(supersetClient.getChartUuid(CHART_C_ID)).thenReturn(CHART_C_UUID);

        // chart c is not shared with other dashboards
        when(supersetClient.getChartDashboardIds(CHART_C_ID)).thenReturn(List.of(DASHBOARD_ID));

        when(supersetClient.getDatasetUuid(DATASET_ORPHAN_ID)).thenReturn(DATASET_ORPHAN_UUID);
        // the orphan dataset is only referenced by the removed chart c
        JsonArray orphanRefs = new JsonArray();
        orphanRefs.add(chart(CHART_C_ID, DATASET_ORPHAN_ID));
        when(supersetClient.getChartsForDatasource(DATASET_ORPHAN_ID)).thenReturn(orphanRefs);

        invokePrune(supersetClient, createExportZip());

        // only chart c is deleted, charts a and b are kept for in-place overwrite
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> deletedCharts = ArgumentCaptor.forClass(List.class);
        verify(supersetClient).deleteCharts(deletedCharts.capture());
        assertThat(deletedCharts.getValue()).containsExactly(CHART_C_ID);

        // only the orphan dataset is deleted; the kept dataset is never even inspected
        verify(supersetClient).deleteDataset(DATASET_ORPHAN_ID);
        verify(supersetClient, never()).deleteDataset(DATASET_KEPT_ID);
    }

    @Test
    void keepsDatasetThatIsStillPartOfNewExport() throws Exception {
        SupersetClient supersetClient = mock(SupersetClient.class);
        mockDashboardLookup(supersetClient);

        // chart c (removed) but it uses a dataset that is still part of the new export
        JsonArray charts = new JsonArray();
        charts.add(chart(CHART_C_ID, DATASET_KEPT_ID));
        when(supersetClient.getDashboardCharts(DASHBOARD_ID)).thenReturn(charts);
        when(supersetClient.getChartUuid(CHART_C_ID)).thenReturn(CHART_C_UUID);
        when(supersetClient.getChartDashboardIds(CHART_C_ID)).thenReturn(List.of(DASHBOARD_ID));
        when(supersetClient.getDatasetUuid(DATASET_KEPT_ID)).thenReturn(DATASET_KEPT_UUID);

        invokePrune(supersetClient, createExportZip());

        verify(supersetClient).deleteCharts(eq(List.of(CHART_C_ID)));
        // dataset is in the new export -> must not be deleted, reference lookup must be skipped
        verify(supersetClient, never()).deleteDataset(DATASET_KEPT_ID);
        verify(supersetClient, never()).getChartsForDatasource(DATASET_KEPT_ID);
    }

    private void mockDashboardLookup(SupersetClient supersetClient) throws Exception {
        SupersetDashboardResponse dashboardResponse = new SupersetDashboardResponse();
        SupersetDashboard dashboard = new SupersetDashboard();
        dashboard.setId(DASHBOARD_ID);
        dashboard.setUuid(DASHBOARD_UUID);
        dashboardResponse.setResult(List.of(dashboard));
        when(supersetClient.dashboards(any(), any())).thenReturn(dashboardResponse);
    }

    private JsonObject chart(int id, int datasourceId) {
        JsonObject chart = new JsonObject();
        chart.addProperty("id", id);
        chart.addProperty("datasource_id", datasourceId);
        chart.addProperty("datasource_type", "table");
        return chart;
    }

    private File createExportZip() throws IOException {
        File zip = File.createTempFile("dashboard-export-", ".zip");
        zip.deleteOnExit();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            writeYaml(zos, "dashboard_export/dashboards/dash.yaml", DASHBOARD_UUID);
            writeYaml(zos, "dashboard_export/charts/a.yaml", CHART_A_UUID);
            writeYaml(zos, "dashboard_export/charts/b.yaml", CHART_B_UUID);
            writeYaml(zos, "dashboard_export/datasets/db/kept.yaml", DATASET_KEPT_UUID);
        }
        return zip;
    }

    private void writeYaml(ZipOutputStream zos, String path, String uuid) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(("uuid: " + uuid + "\n").getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void invokePrune(SupersetClient supersetClient, File zip) throws Exception {
        UploadDashboardsFileListener listener = new UploadDashboardsFileListener(null, null, null, null);
        Method method = UploadDashboardsFileListener.class.getDeclaredMethod("pruneExistingDashboardAssets", SupersetClient.class, File.class);
        method.setAccessible(true);
        method.invoke(listener, supersetClient, zip);
    }
}
