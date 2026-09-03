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

import ch.bedag.dap.hellodata.portal.base.config.PdfExportTemplateConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Renders the corporate-design templates without booting the Spring context: the PDF template
 *  engine and branding are wired by hand, exactly as the {@code @Configuration} does. */
class PdfRenderSmokeTest {

    private PdfRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new PdfRenderer(new PdfExportTemplateConfig().pdfTemplateEngine(), new PdfBrandingProperties(null, null, null));
    }

    private static byte[] logo() throws Exception {
        try (InputStream in = PdfRenderSmokeTest.class.getResourceAsStream("/pdfexport/branding/logo.png")) {
            return in.readAllBytes();
        }
    }

    @Test
    void rendersFullDashboardPdf() throws Exception {
        byte[] png = logo();
        DashboardExport export = new DashboardExport(
                "Abstimmung vom 08. März 2026",
                List.of(new DashboardExport.Section(
                        "Eidgenössische Vorlagen",
                        List.of(
                                new DashboardExport.Markdown("<div class=\"markdown\"><h1>Explore Trends</h1><p>Dive into the data.</p></div>"),
                                new DashboardExport.Chart("Eidgenössische Vorlage", png)))));

        byte[] pdf = renderer.render(export);

        String head = new String(pdf, 0, 8, StandardCharsets.ISO_8859_1);
        assertTrue(head.startsWith("%PDF-"), "not a PDF: " + head);
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).contains("Roboto"), "Roboto font not embedded");
        assertTrue(pdf.length > 20_000, "PDF suspiciously small: " + pdf.length);
    }

    @Test
    void rendersCustomLayoutPdf() throws Exception {
        byte[] png = logo();
        CustomLayout layout = new CustomLayout(
                "Mein Layout",
                List.of(new CustomLayout.GridPage("256mm", List.of(
                        new CustomLayout.Tile(new DashboardExport.Chart("Links", png), "0%", "0mm", "25%", "64mm", "49mm"),
                        new CustomLayout.Tile(new DashboardExport.Chart("Rechts", png), "75%", "0mm", "25%", "64mm", "49mm"),
                        new CustomLayout.Tile(new DashboardExport.Markdown("<div class=\"markdown\"><p>Notiz</p></div>"), "0%", "64mm", "100%", "64mm", "49mm")))));

        byte[] portrait = renderer.renderCustom(layout, ReportTemplate.PORTRAIT);
        assertTrue(new String(portrait, 0, 8, StandardCharsets.ISO_8859_1).startsWith("%PDF-"), "not a PDF");
        assertTrue(portrait.length > 20_000, "PDF suspiciously small: " + portrait.length);
        assertTrue(isPortrait(portrait), "portrait template should produce a portrait A4 page");

        byte[] landscape = renderer.renderCustom(layout, ReportTemplate.LANDSCAPE);
        assertTrue(new String(landscape, 0, 8, StandardCharsets.ISO_8859_1).startsWith("%PDF-"), "landscape not a PDF");
        assertTrue(!isPortrait(landscape), "landscape template should produce a landscape A4 page");
    }

    /** True when the first page is taller than it is wide (portrait). */
    private static boolean isPortrait(byte[] pdf) throws Exception {
        try (PDDocument doc = PDDocument.load(pdf)) {
            PDRectangle box = doc.getPage(0).getMediaBox();
            return box.getHeight() > box.getWidth();
        }
    }
}
