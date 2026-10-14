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

import java.util.Arrays;
import java.util.List;

/** Page geometry of a {@link ReportTemplate} for the PDF builder, so its canvas can show each page in
 *  the shape and scale the PDF will have instead of keeping a second copy of these numbers. All
 *  lengths are millimetres; the grid is {@code gridCols} columns of {@code contentWidthMm / gridCols}
 *  by {@code gridRowsPerPage} rows of {@code cellHeightMm}. */
public record PdfTemplateDto(String id, String name, int pageWidthMm, int pageHeightMm,
                             int contentWidthMm, int cellHeightMm, int gridCols, int gridRowsPerPage,
                             int chartOverheadMm) {

    public static PdfTemplateDto of(ReportTemplate template) {
        return new PdfTemplateDto(template.id(), template.displayName(), template.pageWidthMm(), template.pageHeightMm(),
                template.contentWidthMm(), template.cellHeightMm(), ReportTemplate.GRID_COLS,
                ReportTemplate.GRID_ROWS_PER_PAGE, ReportTemplate.CHART_OVERHEAD_MM);
    }

    /** All selectable templates, in their declaration order (portrait first). */
    public static List<PdfTemplateDto> all() {
        return Arrays.stream(ReportTemplate.values()).map(PdfTemplateDto::of).toList();
    }
}
