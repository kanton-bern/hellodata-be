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

import ch.bedag.dap.hellodata.portal.superset.pdfexport.CustomLayout.GridPage;
import ch.bedag.dap.hellodata.portal.superset.pdfexport.CustomLayout.Tile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayoutGridPackerTest {

    // Portrait grid-row height: (297 - 20 - 18) / 4 = 64mm; the page box is 4 * 64 = 256mm tall.
    private static final ReportTemplate T = ReportTemplate.PORTRAIT;
    private static final int CELL = T.cellHeightMm();

    private static LayoutGridPacker.Positioned cell(int x, int y, int cols, int rows, String name) {
        return new LayoutGridPacker.Positioned(x, y, cols, rows, new DashboardExport.Chart(name, new byte[0]));
    }

    private static Tile tile(GridPage page, String name) {
        return page.tiles().stream()
                .filter(t -> ((DashboardExport.Chart) t.item()).name().equals(name))
                .findFirst().orElseThrow();
    }

    private static void assertBox(Tile t, String left, String top, String width, String height) {
        assertEquals(left, t.left());
        assertEquals(top, t.top());
        assertEquals(width, t.width());
        assertEquals(height, t.height());
    }

    /** The page box is exactly GRID_ROWS_PER_PAGE grid rows tall. */
    @Test
    void pageBoxHeightIsFourGridRows() {
        GridPage page = LayoutGridPacker.toGridPage(List.of(cell(0, 0, 1, 1, "A")), T);
        assertEquals((4 * CELL) + "mm", page.heightCss());
    }

    /** Two full-height charts side by side occupy the left and right halves. */
    @Test
    void sideBySideSplitsTheWidth() {
        GridPage page = LayoutGridPacker.toGridPage(List.of(
                cell(0, 0, 2, 4, "A"),
                cell(2, 0, 2, 4, "B")), T);
        assertBox(tile(page, "A"), "0%", "0mm", "50%", (4 * CELL) + "mm");
        assertBox(tile(page, "B"), "50%", "0mm", "50%", (4 * CELL) + "mm");
    }

    /** A horizontal gap is simply un-covered space: the two tiles keep their exact columns. */
    @Test
    void horizontalGapLeavesColumnsUncovered() {
        GridPage page = LayoutGridPacker.toGridPage(List.of(
                cell(0, 0, 1, 1, "Left"),
                cell(3, 0, 1, 1, "Right")), T);
        assertEquals(2, page.tiles().size(), "gap columns produce no filler tiles");
        assertBox(tile(page, "Left"), "0%", "0mm", "25%", CELL + "mm");
        assertBox(tile(page, "Right"), "75%", "0mm", "25%", CELL + "mm");
    }

    /** A tall chart beside a stack of two: exact heights, no rowspan bookkeeping. */
    @Test
    void tallChartBesideStack() {
        GridPage page = LayoutGridPacker.toGridPage(List.of(
                cell(0, 0, 2, 4, "Tall"),
                cell(2, 0, 2, 2, "TopRight"),
                cell(2, 2, 2, 2, "BottomRight")), T);
        assertBox(tile(page, "Tall"), "0%", "0mm", "50%", (4 * CELL) + "mm");
        assertBox(tile(page, "TopRight"), "50%", "0mm", "50%", (2 * CELL) + "mm");
        assertBox(tile(page, "BottomRight"), "50%", (2 * CELL) + "mm", "50%", (2 * CELL) + "mm");
    }

    /** A vertical gap (empty grid row) is just space with no tile in it. */
    @Test
    void verticalGapLeavesRowEmpty() {
        GridPage page = LayoutGridPacker.toGridPage(List.of(
                cell(0, 0, 4, 1, "Top"),
                cell(0, 2, 4, 1, "Bottom")), T);
        assertEquals(2, page.tiles().size());
        assertBox(tile(page, "Top"), "0%", "0mm", "100%", CELL + "mm");
        assertBox(tile(page, "Bottom"), "0%", (2 * CELL) + "mm", "100%", CELL + "mm");
    }

    /** A tile is re-clamped so it can never spill past the page's right edge or bottom band. */
    @Test
    void tileIsClampedToPageBounds() {
        GridPage page = LayoutGridPacker.toGridPage(List.of(cell(3, 3, 2, 2, "Corner")), T);
        // Anchored in the last column/row, a 2x2 shrinks to the single remaining cell.
        assertBox(tile(page, "Corner"), "75%", (3 * CELL) + "mm", "25%", CELL + "mm");
    }

    /** clampRowsToBand keeps an item inside its 4-row page band. */
    @Test
    void clampRowsToBandRespectsPageBoundary() {
        // Starting at local row 3 (y=3), only 1 row of headroom remains in the band.
        assertEquals(1, LayoutGridPacker.clampRowsToBand(3, 4));
        // Starting at the top of a band, a 4-row item fits fully.
        assertEquals(4, LayoutGridPacker.clampRowsToBand(0, 4));
    }
}
