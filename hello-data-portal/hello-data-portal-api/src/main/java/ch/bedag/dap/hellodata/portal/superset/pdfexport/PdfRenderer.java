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

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/** Renders the corporate-design Thymeleaf template to a PDF. */
@Log4j2
@Service
public class PdfRenderer {

    private static final DateTimeFormatter GERMAN_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    /** Logo + font files are looked up here first (overridable branding); see PdfTemplateProvisioner. */
    private static final String EXTERNAL_LOGO = "branding/logo.png";

    private final TemplateEngine templateEngine;
    private final PdfBrandingProperties branding;
    /** Editable on-disk root ({@code hello-data.pdf-export.template-location}); blank = classpath only. */
    private final String templateLocation;

    public PdfRenderer(@Qualifier("pdfTemplateEngine") TemplateEngine templateEngine, PdfBrandingProperties branding,
                       @Value("${hello-data.pdf-export.template-location:}") String templateLocation) {
        this.templateEngine = templateEngine;
        this.branding = branding;
        this.templateLocation = templateLocation;
    }

    /** Full-dashboard export (charts grouped by tab, in layout order). */
    public byte[] render(DashboardExport export) {
        Context ctx = new Context();
        ctx.setVariable("export", export);
        ctx.setVariable("generatedAt", LocalDate.now().toString());
        applyBranding(ctx);
        return toPdf("dashboard", ctx, export.title(), ReportTemplate.PORTRAIT.pageSize());
    }

    /** Render the custom grid layout produced by the Angular builder. */
    public byte[] renderCustom(CustomLayout layout, ReportTemplate template) {
        Context ctx = new Context();
        ctx.setVariable("layout", layout);
        ctx.setVariable("generatedAt", LocalDate.now().toString());
        applyBranding(ctx);
        return toPdf(template.templateFile(), ctx, layout.title(), template.pageSize());
    }

    private void applyBranding(Context ctx) {
        // Resolved per render so a logo swapped on disk is picked up without a restart.
        ctx.setVariable("logoDataUri", "data:image/png;base64," + Base64.getEncoder().encodeToString(brandingAsset(EXTERNAL_LOGO, branding.logoClasspath())));
        ctx.setVariable("orgName", branding.orgName());
        ctx.setVariable("reportCaption", branding.reportCaption());
    }

    private byte[] toPdf(String template, Context ctx, String footerTitle, String pageSize) {
        String html = templateEngine.process(template, ctx);
        // Footer values live in @page CSS content strings, which Thymeleaf can't safely
        // inline; substitute them here (CSS-escaped) so a quote in the title can't break the CSS.
        html = html
                .replace("@@PAGE_SIZE@@", pageSize)
                .replace("@@PAGE_MARGIN@@", ReportTemplate.pageMargin())
                .replace("@@FOOTER_TITLE@@", cssEscape(footerTitle))
                .replace("@@FOOTER_DATE@@", GERMAN_DATE_TIME.format(LocalDateTime.now()));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerRoboto(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render PDF", e);
        }
    }

    private void registerRoboto(PdfRendererBuilder builder) {
        // Family stays "Roboto" (the CSS references it); operators swap the glyphs by dropping their own
        // Roboto-*.ttf into <location>/fonts (same filenames). Weights map Light/Regular/Medium/Bold.
        builder.useFont(() -> fontStream("Roboto-Light.ttf"), "Roboto", 300, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> fontStream("Roboto-Regular.ttf"), "Roboto", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> fontStream("Roboto-Medium.ttf"), "Roboto", 500, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> fontStream("Roboto-Bold.ttf"), "Roboto", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
    }

    private InputStream fontStream(String fileName) {
        return new ByteArrayInputStream(brandingAsset("fonts/" + fileName, "/pdfexport/fonts/" + fileName));
    }

    /** Read a branding asset (logo/font) from the editable on-disk location if present, else from the
     *  packaged classpath default. Resolved per render so on-disk swaps take effect without a restart. */
    private byte[] brandingAsset(String relativePath, String classpathPath) {
        if (templateLocation != null && !templateLocation.isBlank()) {
            Path onDisk = Path.of(templateLocation, relativePath);
            if (Files.isReadable(onDisk)) {
                try {
                    return Files.readAllBytes(onDisk);
                } catch (Exception e) { //NOSONAR - fall back to the packaged default on any read error
                    log.warn("Could not read PDF branding asset {}, using packaged default: {}", onDisk, e.getMessage());
                }
            }
        }
        try (InputStream in = classpathStream(classpathPath)) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load PDF branding asset: " + classpathPath, e);
        }
    }

    private InputStream classpathStream(String path) {
        InputStream in = getClass().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Missing classpath resource: " + path);
        }
        return in;
    }

    /** Escape a value for safe insertion inside a double-quoted CSS string literal. */
    private static String cssEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
