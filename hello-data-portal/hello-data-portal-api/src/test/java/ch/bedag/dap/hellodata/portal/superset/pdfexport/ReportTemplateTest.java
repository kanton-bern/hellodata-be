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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTemplateTest {

    private static final double DPI = 150.0;

    /** A chart filling all 4x4 cells, drawn at width:100%, must fit one page: its rendered
     *  height plus the per-chart overhead cannot exceed the content height. */
    @Test
    void fullPageChartFitsOnePage() {
        for (ReportTemplate t : ReportTemplate.values()) {
            int[] px = t.chartScreenshotPx(ReportTemplate.GRID_COLS, ReportTemplate.GRID_ROWS_PER_PAGE, DPI);
            double aspect = (double) px[0] / px[1];
            double renderedHeightMm = t.contentWidthMm() / aspect;
            assertTrue(renderedHeightMm + ReportTemplate.CHART_OVERHEAD_MM <= t.contentHeightMm() + 0.5,
                    t + " full-page chart overflows: " + renderedHeightMm + "mm + overhead > " + t.contentHeightMm() + "mm");
        }
    }

    /** A 2x2 chart is half the width and (roughly) half the height of a 4x4 chart. */
    @Test
    void halfSpanChartIsAboutHalfSize() {
        ReportTemplate t = ReportTemplate.LANDSCAPE;
        int[] full = t.chartScreenshotPx(4, 4, DPI);
        int[] half = t.chartScreenshotPx(2, 2, DPI);
        assertEquals(full[0] / 2.0, half[0], 2.0, "2x2 width should be half of 4x4 width");
        assertTrue(half[1] < full[1], "2x2 height should be less than 4x4 height");
    }

    /** The captured width matches the requested DPI: px width / printed width (inches) ~= dpi. */
    @Test
    void capturedResolutionMatchesRequestedDpi() {
        ReportTemplate t = ReportTemplate.PORTRAIT;
        int[] px = t.chartScreenshotPx(ReportTemplate.GRID_COLS, ReportTemplate.GRID_ROWS_PER_PAGE, DPI);
        double widthInches = t.contentWidthMm() / 25.4;
        double actualDpi = px[0] / widthInches;
        assertEquals(DPI, actualDpi, 1.0, "captured width should be ~" + DPI + " px/inch");
    }

    /** Unknown/blank template ids fall back to portrait (keeps untrusted input off the template path). */
    @Test
    void unknownIdFallsBackToPortrait() {
        assertEquals(ReportTemplate.PORTRAIT, ReportTemplate.fromId("bogus"));
        assertEquals(ReportTemplate.PORTRAIT, ReportTemplate.fromId(null));
        assertEquals(ReportTemplate.LANDSCAPE, ReportTemplate.fromId("landscape"));
    }
}
