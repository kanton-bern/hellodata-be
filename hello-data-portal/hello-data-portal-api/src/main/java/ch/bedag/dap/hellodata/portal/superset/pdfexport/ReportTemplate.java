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

/** The selectable custom-layout PDF templates, differing only in A4 page orientation.
 *  Page/margin geometry lives here as the single source of truth: the {@code @page}
 *  CSS (size + margin) and each chart's cell-derived screenshot size are all computed
 *  from these constants, so they cannot drift. */
public enum ReportTemplate {
    PORTRAIT("portrait", "Portrait", "portrait_template", 210, 297),
    LANDSCAPE("landscape", "Landscape", "landscape_template", 297, 210);

    // A page is a GRID_COLS x GRID_ROWS_PER_PAGE block of cells; charts are placed and sized
    // in these cells, and a page break happens every GRID_ROWS_PER_PAGE grid rows.
    public static final int GRID_COLS = 4;
    public static final int GRID_ROWS_PER_PAGE = 4;

    // Shared A4 page margins (mm); injected into the @page CSS via pageMargin().
    static final int MARGIN_TOP_MM = 20;
    static final int MARGIN_BOTTOM_MM = 18;
    static final int MARGIN_SIDE_MM = 16;
    // Vertical space each chart's heading + image border/padding + bottom margin eats within its
    // cell; the image gets what's left. Raise it if a chart still nudges past the cells it was allotted.
    static final int CHART_OVERHEAD_MM = 15;
    // Default output resolution (pixels per inch) for exported chart images. A chart is captured
    // this many px per inch of its printed size.
    public static final int DEFAULT_SCREENSHOT_DPI = 150;
    // Exact millimetres per inch, to convert a DPI into pixels per millimetre.
    private static final double MM_PER_INCH = 25.4;

    private final String id;
    private final String displayName;
    private final String templateFile;
    private final int pageWidthMm;
    private final int pageHeightMm;

    ReportTemplate(String id, String displayName, String templateFile, int pageWidthMm, int pageHeightMm) {
        this.id = id;
        this.displayName = displayName;
        this.templateFile = templateFile;
        this.pageWidthMm = pageWidthMm;
        this.pageHeightMm = pageHeightMm;
    }

    /** Printable width (mm): page width minus the left/right margins. */
    public int contentWidthMm() {
        return pageWidthMm - 2 * MARGIN_SIDE_MM;
    }

    /** Printable height (mm): page height minus the top/bottom margins. */
    public int contentHeightMm() {
        return pageHeightMm - MARGIN_TOP_MM - MARGIN_BOTTOM_MM;
    }

    /** Height (mm) of one grid row: the printable height split into {@code GRID_ROWS_PER_PAGE}
     *  bands. Used as the fixed grid-row height so empty rows still reserve vertical space. */
    public int cellHeightMm() {
        return contentHeightMm() / GRID_ROWS_PER_PAGE;
    }

    /** CSS {@code @page margin} value (top right bottom left), shared by both orientations. */
    public static String pageMargin() {
        return MARGIN_TOP_MM + "mm " + MARGIN_SIDE_MM + "mm " + MARGIN_BOTTOM_MM + "mm " + MARGIN_SIDE_MM + "mm";
    }

    /** Screenshot pixel size for a chart occupying {@code cols x rows} grid cells: as wide as its
     *  columns and as tall as its rows, minus the per-chart heading overhead, captured at
     *  {@code dpi} pixels per inch of its printed size. Rendered at {@code width:100%} of its cell
     *  the capture then fills its slot without overflowing. Returns {@code [width, height]}. */
    public int[] chartScreenshotPx(int cols, int rows, double dpi) {
        int c = Math.max(1, Math.min(GRID_COLS, cols));
        int r = Math.max(1, Math.min(GRID_ROWS_PER_PAGE, rows));
        double pxPerMm = dpi / MM_PER_INCH;
        double cellW = contentWidthMm() / (double) GRID_COLS;
        double cellH = contentHeightMm() / (double) GRID_ROWS_PER_PAGE;
        double widthMm = c * cellW;
        double heightMm = Math.max(1, r * cellH - CHART_OVERHEAD_MM);
        return new int[] {
            (int) Math.round(widthMm * pxPerMm),
            (int) Math.round(heightMm * pxPerMm),
        };
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String templateFile() {
        return templateFile;
    }

    /** CSS {@code @page size} value derived from the page dimensions, e.g. {@code 210mm 297mm}. */
    public String pageSize() {
        return pageWidthMm + "mm " + pageHeightMm + "mm";
    }

    /** Resolve a request-supplied id to a known template, defaulting to portrait.
     *  Whitelisting keeps the untrusted request value off the template-name path. */
    public static ReportTemplate fromId(String id) {
        for (ReportTemplate t : values()) {
            if (t.id.equalsIgnoreCase(id)) {
                return t;
            }
        }
        return PORTRAIT;
    }
}
