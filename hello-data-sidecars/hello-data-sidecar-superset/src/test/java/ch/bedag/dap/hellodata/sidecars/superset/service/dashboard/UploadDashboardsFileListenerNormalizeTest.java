package ch.bedag.dap.hellodata.sidecars.superset.service.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link UploadDashboardsFileListener} normalizes double-encoded {@code query_context}
 * fields in a dashboard export zip before it is handed to the Superset import API. This reproduces the
 * failure the importer hits ({@code query_context["form_data"]["datasource"] = ...} on a String
 * form_data) and asserts we repair it in place.
 */
class UploadDashboardsFileListenerNormalizeTest {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private UploadDashboardsFileListener listener;

    @TempDir
    Path tmp;

    @BeforeEach
    void setUp() throws Exception {
        // @RequiredArgsConstructor: (Connection, SupersetClientProvider, ObjectMapper, DashboardResourceProviderService)
        listener = new UploadDashboardsFileListener(null, null, jsonMapper, null);
        setField(listener, "tmpDir", tmp.toString());
    }

    @Test
    void normalizesDoubleEncodedFormData() throws Exception {
        Map<String, Object> formData = orderedMap(
                "datasource", "2__table",
                "viz_type", "heatmap_v2",
                "groupby", "species");

        // form_data stored as a JSON *string* inside query_context -> this is what breaks the import
        Map<String, Object> queryContext = orderedMap(
                "datasource", orderedMap("id", 2, "type", "table"),
                "form_data", jsonMapper.writeValueAsString(formData),
                "queries", List.of(orderedMap("metrics", List.of("count"))));

        File zip = writeZip(Map.of(
                "dashboard_export/charts/Heatmap_1.yaml", chartYaml("uuid-1", "SC - Heatmap", queryContext),
                "dashboard_export/metadata.yaml", "version: 1.0.0\ntype: Dashboard\n"));

        invokeNormalize(zip);

        Map<String, Object> chart = readChart(zip, "dashboard_export/charts/Heatmap_1.yaml");
        Object qcRaw = chart.get("query_context");
        assertInstanceOf(String.class, qcRaw, "query_context should remain a JSON string field");

        Map<?, ?> qc = jsonMapper.readValue((String) qcRaw, Map.class);
        assertInstanceOf(Map.class, qc.get("form_data"), "form_data must be unwrapped to an object");
        assertEquals(formData, qc.get("form_data"), "form_data content must be preserved");
        // untouched siblings survive
        assertInstanceOf(Map.class, qc.get("datasource"));
        assertInstanceOf(List.class, qc.get("queries"));

        // non-chart entry is copied through unchanged
        assertEquals("version: 1.0.0\ntype: Dashboard\n",
                readEntry(zip, "dashboard_export/metadata.yaml"));
    }

    /**
     * Regression: when at least one chart IS normalized (so the rewritten zip replaces the original),
     * every OTHER chart in the zip must be copied through with its content intact - not emptied. The
     * original bug read a chart's bytes to inspect it, then tried to copy it from the now-consumed
     * stream, writing a 0-byte file and leaving the dashboard with no importable charts.
     */
    @Test
    void preservesUnchangedChartsWhenAnotherChartIsNormalized() throws Exception {
        // chart A needs normalization (double-encoded form_data)
        Map<String, Object> qcA = orderedMap(
                "form_data", jsonMapper.writeValueAsString(orderedMap("viz_type", "heatmap_v2")));
        // chart B is already well-formed and must be left untouched, not emptied
        Map<String, Object> qcB = orderedMap(
                "form_data", orderedMap("viz_type", "table"));
        String chartBYaml = chartYaml("uuid-b", "Clean Table", qcB);

        File zip = writeZip(Map.of(
                "dashboard_export/charts/Heatmap_A.yaml", chartYaml("uuid-a", "Heatmap", qcA),
                "dashboard_export/charts/Table_B.yaml", chartBYaml,
                "dashboard_export/metadata.yaml", "version: 1.0.0\ntype: Dashboard\n"));

        invokeNormalize(zip);

        // the untouched chart must still be present and byte-for-byte identical (NOT 0 bytes)
        String chartBAfter = readEntry(zip, "dashboard_export/charts/Table_B.yaml");
        assertFalse(chartBAfter.isEmpty(), "unchanged chart must not be emptied");
        assertEquals(chartBYaml, chartBAfter, "unchanged chart must be copied through intact");
        // and the normalized chart is still valid
        assertInstanceOf(Map.class,
                jsonMapper.readValue((String) readChart(zip, "dashboard_export/charts/Heatmap_A.yaml").get("query_context"), Map.class).get("form_data"));
    }

    @Test
    void leavesWellFormedFormDataUntouched() throws Exception {
        Map<String, Object> queryContext = orderedMap(
                "datasource", orderedMap("id", 2, "type", "table"),
                "form_data", orderedMap("datasource", "2__table", "viz_type", "table"));

        String chartYaml = chartYaml("uuid-2", "Plain Table", queryContext);
        File zip = writeZip(Map.of("dashboard_export/charts/Table_2.yaml", chartYaml));

        invokeNormalize(zip);

        // byte-for-byte identical: nothing was rewritten
        assertEquals(chartYaml, readEntry(zip, "dashboard_export/charts/Table_2.yaml"));
    }

    @Test
    void ignoresChartWithoutQueryContext() throws Exception {
        String chartYaml = "uuid: uuid-3\nslice_name: No Context\nparams:\n  viz_type: pie\n";
        File zip = writeZip(Map.of("dashboard_export/charts/Pie_3.yaml", chartYaml));

        invokeNormalize(zip);

        assertEquals(chartYaml, readEntry(zip, "dashboard_export/charts/Pie_3.yaml"));
    }

    @Test
    void unwrapsFullyDoubleEncodedQueryContext() throws Exception {
        Map<String, Object> queryContext = orderedMap(
                "datasource", orderedMap("id", 2, "type", "table"),
                "form_data", orderedMap("viz_type", "table"));
        // query_context itself double-encoded: a JSON string whose content is another JSON string
        String doubleEncoded = jsonMapper.writeValueAsString(jsonMapper.writeValueAsString(queryContext));

        Map<String, Object> chart = orderedMap("uuid", "uuid-4", "slice_name", "Double", "query_context", doubleEncoded);
        File zip = writeZip(Map.of("dashboard_export/charts/Double_4.yaml", yamlMapper.writeValueAsString(chart)));

        invokeNormalize(zip);

        Object qcRaw = readChart(zip, "dashboard_export/charts/Double_4.yaml").get("query_context");
        Map<?, ?> qc = jsonMapper.readValue((String) qcRaw, Map.class);
        assertInstanceOf(Map.class, qc.get("form_data"));
        assertInstanceOf(Map.class, qc.get("datasource"));
    }

    // --- helpers -------------------------------------------------------------

    private void invokeNormalize(File zip) throws Exception {
        Method m = UploadDashboardsFileListener.class.getDeclaredMethod("normalizeChartsInZip", File.class);
        m.setAccessible(true);
        m.invoke(listener, zip);
    }

    private String chartYaml(String uuid, String sliceName, Map<String, Object> queryContext) throws Exception {
        Map<String, Object> chart = orderedMap(
                "uuid", uuid,
                "slice_name", sliceName,
                "query_context", jsonMapper.writeValueAsString(queryContext)); // stored as JSON string, as Superset exports it
        return yamlMapper.writeValueAsString(chart);
    }

    private File writeZip(Map<String, String> entries) throws Exception {
        File zip = tmp.resolve("export-" + System.nanoTime() + ".zip").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zip;
    }

    private Map<String, Object> readChart(File zip, String entryName) throws Exception {
        return yamlMapper.readValue(readEntry(zip, entryName), Map.class);
    }

    private String readEntry(File zip, String entryName) throws Exception {
        try (ZipFile zf = new ZipFile(zip)) {
            ZipEntry entry = zf.getEntry(entryName);
            assertTrue(entry != null, "entry missing: " + entryName);
            try (InputStream in = zf.getInputStream(entry)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                in.transferTo(bos);
                return bos.toString(StandardCharsets.UTF_8);
            }
        }
    }

    private static Map<String, Object> orderedMap(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
