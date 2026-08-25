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

import java.util.List;

/**
 * View model for the custom-layout template: a title plus one grid per page. Each page is a
 * fixed 4-column grid rendered as one HTML table, so the PDF reproduces the builder's exact
 * cell positions -- side-by-side, stacked, and any empty gap cells -- and breaks every page.
 */
public record CustomLayout(String title, List<GridPage> pages) {

    /** One page: rows of a fixed 4-column grid. */
    public record GridPage(List<GridRow> rows) {
    }

    /** One grid row: the cells that start in this row, left to right. Cells covered by a
     *  {@code rowspan}/{@code colspan} from an earlier cell are omitted (as in HTML tables). */
    public record GridRow(List<GridCell> cells) {
    }

    /** One table cell. {@code item == null} is an empty spacer that preserves a gap.
     *  {@code colspan}/{@code rowspan} carry the chart's cell span. */
    public record GridCell(DashboardExport.Item item, int colspan, int rowspan) {
    }
}
