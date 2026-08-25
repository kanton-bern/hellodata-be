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

import ch.bedag.dap.hellodata.portal.superset.pdfexport.CustomLayout.GridCell;
import ch.bedag.dap.hellodata.portal.superset.pdfexport.CustomLayout.GridPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LayoutGridPackerTest {

    private static LayoutGridPacker.Positioned cell(int x, int y, int cols, int rows, String name) {
        return new LayoutGridPacker.Positioned(x, y, cols, rows, new DashboardExport.Chart(name, new byte[0]));
    }

    private static String name(GridCell c) {
        return ((DashboardExport.Chart) c.item()).name();
    }

    /** Two charts side by side become one row of two single-cell columns. */
    @Test
    void sideBySideBecomesOneRow() {
        GridPage page = LayoutGridPacker.toGridPage(List.of(
                cell(0, 0, 2, 4, "A"),
                cell(2, 0, 2, 4, "B")));
        assertEquals(4, page.rows().size(), "a 4-row-tall pair fills all four grid rows");
        List<GridCell> firstRow = page.rows().get(0).cells();
        assertEquals(2, firstRow.size());
        assertEquals("A", name(firstRow.get(0)));
        assertEquals(2, firstRow.get(0).colspan());
        assertEquals(4, firstRow.get(0).rowspan());
        assertEquals("B", name(firstRow.get(1)));
        assertEquals(0, page.rows().get(1).cells().size());
    }

    /** A horizontal gap between two charts is preserved as an empty spacer cell. */
    @Test
    void horizontalGapKeepsSpacerCell() {
        GridPage page = LayoutGridPacker.toGridPage(List.of(
                cell(0, 0, 1, 1, "Left"),
                cell(3, 0, 1, 1, "Right")));
        List<GridCell> row = page.rows().get(0).cells();
        assertEquals(4, row.size());
        assertEquals("Left", name(row.get(0)));
        assertNull(row.get(1).item(), "gap column 1 is a spacer");
        assertNull(row.get(2).item(), "gap column 2 is a spacer");
        assertEquals("Right", name(row.get(3)));
    }

    /** A tall chart beside a stack of two shorter charts uses rowspan on the tall one. */
    @Test
    void tallChartBesideStackUsesRowspan() {
        GridPage page = LayoutGridPacker.toGridPage(List.of(
                cell(0, 0, 2, 4, "Tall"),
                cell(2, 0, 2, 2, "TopRight"),
                cell(2, 2, 2, 2, "BottomRight")));
        List<GridCell> topRow = page.rows().get(0).cells();
        assertEquals("Tall", name(topRow.get(0)));
        assertEquals(4, topRow.get(0).rowspan());
        assertEquals("TopRight", name(topRow.get(1)));
        assertEquals(2, topRow.get(1).rowspan());
        List<GridCell> row2 = page.rows().get(2).cells();
        assertEquals(1, row2.size());
        assertEquals("BottomRight", name(row2.get(0)));
    }

    /** A vertical gap (empty grid row) between two charts is preserved as an all-spacer row. */
    @Test
    void verticalGapKeepsEmptyRow() {
        GridPage page = LayoutGridPacker.toGridPage(List.of(
                cell(0, 0, 4, 1, "Top"),
                cell(0, 2, 4, 1, "Bottom")));
        assertEquals(3, page.rows().size());
        assertEquals("Top", name(page.rows().get(0).cells().get(0)));
        List<GridCell> middle = page.rows().get(1).cells();
        assertEquals(4, middle.size());
        middle.forEach(c -> assertNull(c.item(), "middle row is an empty gap"));
        assertEquals("Bottom", name(page.rows().get(2).cells().get(0)));
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
