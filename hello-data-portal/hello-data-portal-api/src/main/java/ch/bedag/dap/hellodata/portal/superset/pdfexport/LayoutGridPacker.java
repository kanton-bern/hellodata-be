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
package ch.bedag.dap.hellodata.portal.superset.pdfexport;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Pure layout packing for the custom PDF export. Ported from the PoC SupersetClient
 *  (toGridPage/groupByPage/clamp*), with chart content sourced from a specId -&gt; PNG map
 *  instead of an inline screenshot call. Package-visible statics stay unit-testable. */
public final class LayoutGridPacker {

    private static final Parser MD_PARSER = Parser.builder().build();
    private static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder().build();

    private LayoutGridPacker() {
    }

    /** Build the CustomLayout view model from the builder payload and the fetched PNGs. */
    public static CustomLayout buildCustomLayout(PdfLayoutRequest request, Map<String, byte[]> pngs) {
        String title = request.title() == null || request.title().isBlank() ? "Dashboard " + request.dashboardId() : request.title();

        List<Positioned> positioned = new ArrayList<>();
        for (PdfLayoutRequest.Item item : request.items()) {
            int cols = clampCols(item.cols());
            int rows = clampRowsToBand(item.y(), item.rows());
            DashboardExport.Item content;
            if (item.isChart()) {
                String specId = specId(item.chartId(), cols, rows);
                byte[] png = pngs.get(specId);
                if (png == null) {
                    continue; // a chart that failed to render is dropped, as in the PoC
                }
                String name = item.name() == null || item.name().isBlank() ? "Chart " + item.chartId() : item.name();
                content = new DashboardExport.Chart(name, png);
            } else {
                content = new DashboardExport.Markdown(markdownToHtml(item.markdown()));
            }
            positioned.add(new Positioned(item.x(), Math.max(0, item.y()), cols, rows, content));
        }

        List<CustomLayout.GridPage> pages = groupByPage(positioned).stream().map(LayoutGridPacker::toGridPage).toList();
        return new CustomLayout(title, pages);
    }

    /** Stable screenshot key for a chart placed at a given cell span. */
    public static String specId(long chartId, int cols, int rows) {
        return chartId + "_" + cols + "x" + rows;
    }

    static String markdownToHtml(String markdown) {
        return MD_RENDERER.render(MD_PARSER.parse(markdown == null ? "" : markdown));
    }

    static int clampCols(int cols) {
        return Math.max(1, Math.min(ReportTemplate.GRID_COLS, cols));
    }

    /** Rows the item may occupy without crossing its 4-row page boundary. */
    static int clampRowsToBand(int y, int rows) {
        int top = Math.max(0, y);
        int roomInBand = ReportTemplate.GRID_ROWS_PER_PAGE - (top % ReportTemplate.GRID_ROWS_PER_PAGE);
        return Math.max(1, Math.min(Math.min(rows, ReportTemplate.GRID_ROWS_PER_PAGE), roomInBand));
    }

    /** Split positioned items into pages of GRID_ROWS_PER_PAGE grid rows, in page order. */
    static List<List<Positioned>> groupByPage(List<Positioned> items) {
        TreeMap<Integer, List<Positioned>> byPage = new TreeMap<>();
        for (Positioned p : items) {
            int page = Math.max(0, p.y()) / ReportTemplate.GRID_ROWS_PER_PAGE;
            byPage.computeIfAbsent(page, k -> new ArrayList<>()).add(p);
        }
        return new ArrayList<>(byPage.values());
    }

    /** Pack a page's items into a fixed GRID_COLS-wide table with colspan/rowspan + spacer cells. */
    static CustomLayout.GridPage toGridPage(List<Positioned> items) {
        int cols = ReportTemplate.GRID_COLS;
        int rowsPerPage = ReportTemplate.GRID_ROWS_PER_PAGE;
        int rowsUsed = 0;
        for (Positioned p : items) {
            int localY = Math.max(0, p.y()) % rowsPerPage;
            rowsUsed = Math.max(rowsUsed, Math.min(rowsPerPage, localY + p.rows()));
        }
        boolean[][] covered = new boolean[rowsUsed][cols];
        Positioned[][] start = new Positioned[rowsUsed][cols];
        for (Positioned p : items) {
            int r0 = Math.max(0, p.y()) % rowsPerPage;
            int c0 = Math.max(0, Math.min(cols - 1, p.x()));
            int r1 = Math.min(rowsUsed, r0 + Math.max(1, p.rows()));
            int c1 = Math.min(cols, c0 + Math.max(1, p.cols()));
            start[r0][c0] = p;
            for (int r = r0; r < r1; r++) {
                for (int c = c0; c < c1; c++) {
                    covered[r][c] = true;
                }
            }
        }
        List<CustomLayout.GridRow> rows = new ArrayList<>();
        for (int r = 0; r < rowsUsed; r++) {
            List<CustomLayout.GridCell> cells = new ArrayList<>();
            for (int c = 0; c < cols; c++) {
                Positioned p = start[r][c];
                if (p != null) {
                    int colspan = Math.min(cols - c, Math.max(1, p.cols()));
                    int rowspan = Math.min(rowsUsed - r, Math.max(1, p.rows()));
                    cells.add(new CustomLayout.GridCell(p.item(), colspan, rowspan));
                } else if (!covered[r][c]) {
                    cells.add(new CustomLayout.GridCell(null, 1, 1));
                }
            }
            rows.add(new CustomLayout.GridRow(cells));
        }
        return new CustomLayout.GridPage(rows);
    }

    /** A resolved grid cell (page-local position + span) awaiting packing. */
    record Positioned(int x, int y, int cols, int rows, DashboardExport.Item item) {
    }
}
