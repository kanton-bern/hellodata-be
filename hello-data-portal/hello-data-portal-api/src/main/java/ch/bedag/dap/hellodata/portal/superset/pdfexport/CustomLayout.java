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
 * fixed-height box whose tiles are absolutely positioned at their exact grid coordinates, so the
 * PDF reproduces the builder's layout precisely -- side-by-side, stacked, and any empty gap --
 * regardless of each chart's rendered content height (which an HTML table's rowspans could not
 * keep aligned). openhtmltopdf renders CSS 2.1 absolute positioning reliably.
 */
public record CustomLayout(String title, List<GridPage> pages) {

    /** One page: a relatively-positioned box {@code heightCss} tall, holding absolutely-placed tiles. */
    public record GridPage(String heightCss, List<Tile> tiles) {
    }

    /** One placed chart/markdown tile. {@code left}/{@code width} are column percentages (e.g.
     *  {@code "25%"}), {@code top}/{@code height} are millimetres (e.g. {@code "64mm"}); all are
     *  pre-formatted CSS values so the template just drops them into the inline {@code style}.
     *  {@code imageHeight} is the fixed height (mm) of a chart's image box -- the tile height minus
     *  the heading/border overhead -- so the box always fills its grid slot and the screenshot is
     *  fitted inside it (openhtmltopdf has no {@code object-fit}); ignored for markdown tiles. */
    public record Tile(DashboardExport.Item item, String left, String top, String width, String height,
                       String imageHeight) {
    }
}
