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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTemplateDtoTest {

    /** The builder canvas sizes its pages from these numbers; a change here changes the canvas too. */
    @Test
    void exposesTheGridGeometryOfEachTemplate() {
        List<PdfTemplateDto> all = PdfTemplateDto.all();
        assertEquals(List.of(
                new PdfTemplateDto("portrait", "Portrait", 210, 297, 178, 64, 4, 4, 15),
                new PdfTemplateDto("landscape", "Landscape", 297, 210, 265, 42, 4, 4, 15)), all);
    }

    /** The geometry is derived from ReportTemplate, which also lays out the PDF, so they cannot drift. */
    @Test
    void geometryMatchesTheRenderedGrid() {
        for (ReportTemplate t : ReportTemplate.values()) {
            PdfTemplateDto dto = PdfTemplateDto.of(t);
            assertEquals(t.contentWidthMm(), dto.contentWidthMm());
            assertEquals(t.cellHeightMm(), dto.cellHeightMm());
            assertTrue(dto.gridRowsPerPage() * dto.cellHeightMm() <= t.contentHeightMm(), t + " grid taller than the page");
        }
    }

    /**
     * The builder canvas (pdf-builder.component.scss, the .pdf-page rules) mirrors these text styles of
     * the PDF, scaled to the canvas, so a text block wraps and clips there as it will in the PDF. When one
     * of these rules changes, change the canvas styles the same way and then update this test.
     */
    @Test
    void textStylesMirroredByTheBuilderCanvasAreUnchanged() throws IOException {
        String css = pdfCss();
        assertRule(css, "body", "font-family: \"Roboto\", Arial, sans-serif", "font-size: 10pt", "font-weight: 400", "line-height: 1.4");
        assertRule(css, ".summary", "border: 1px solid #dedede", "padding: 8px 10px", "font-size: 9pt", "background-color: #faf1e3");
        assertRule(css, ".markdown", "margin: 6pt 0 14pt 0");
        assertRule(css, ".markdown h1", "font-size: 18pt", "font-weight: 300", "line-height: 1.2", "margin: 0 0 8px 0");
        assertRule(css, ".markdown h2", "font-size: 15pt", "font-weight: 300", "line-height: 1.2", "margin: 0 0 6px 0");
        assertRule(css, ".markdown h3", "font-size: 12pt", "font-weight: 500", "margin: 0 0 6px 0");
        assertRule(css, ".markdown p", "margin: 0 0 6px 0");
        assertRule(css, ".markdown ul, .markdown ol", "margin: 0 0 6px 3em", "padding: 0");
        assertRule(css, "h3.chart-name", "font-size: 12pt", "font-weight: 300", "line-height: 1.2", "margin: 0 0 8px 0");
        assertRule(css, ".chart .chart-image", "border: 1px solid #dedede", "padding: 6px");
        assertRule(css, ".grid-page .grid-tile", "padding: 0 6px 10pt 6px", "overflow: hidden");
    }

    /** The builder canvas sets its text in copies of the fonts the PDF embeds (Angular cannot serve
     *  assets from outside the UI project), so its lines break like the PDF's. Keep them identical. */
    @Test
    void builderCanvasUsesTheFontsThePdfEmbeds() throws IOException {
        Path uiFonts = Path.of("../hello-data-portal-ui/src/assets/fonts/PdfRoboto");
        for (String file : List.of("Roboto-Light.ttf", "Roboto-Regular.ttf", "Roboto-Medium.ttf", "Roboto-Bold.ttf")) {
            try (InputStream pdfFont = PdfTemplateDtoTest.class.getResourceAsStream("/pdfexport/fonts/" + file)) {
                assertArrayEquals(pdfFont.readAllBytes(), Files.readAllBytes(uiFonts.resolve(file)), file + " differs from the UI copy");
            }
        }
    }

    private static String pdfCss() throws IOException {
        try (InputStream in = PdfTemplateDtoTest.class.getResourceAsStream("/pdfexport/templates/fragments/pdf.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Asserts the rule for {@code selector} contains each expected declaration. */
    private static void assertRule(String css, String selector, String... declarations) {
        Matcher m = Pattern.compile("(?m)^\\s*" + Pattern.quote(selector) + "\\s*\\{([^}]*)}").matcher(css);
        assertTrue(m.find(), "no CSS rule for " + selector);
        Set<String> actual = Arrays.stream(m.group(1).split(";"))
                .map(d -> d.replaceAll("/\\*.*?\\*/", "").trim().replaceAll("\\s+", " "))
                .filter(d -> !d.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String d : declarations) {
            assertTrue(actual.contains(d), selector + " lost '" + d + "', has " + actual);
        }
    }
}
