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

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Seeds the editable PDF-export template folder ({@code hello-data.pdf-export.template-location}) from
 * the packaged classpath defaults on startup. Only files that don't already exist are copied, so
 * operators' edits survive restarts while a newly added default template is still delivered. The
 * renderer reads from this folder first and falls back to the classpath (see PdfExportTemplateConfig),
 * so seeding is best-effort: if the folder can't be written the export still works off the defaults.
 */
@Log4j2
@Component
public class PdfTemplateProvisioner {

    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Value("${hello-data.pdf-export.template-location:}")
    private String templateLocation;

    @PostConstruct
    public void seedDefaults() {
        if (templateLocation == null || templateLocation.isBlank()) {
            return;
        }
        Path base = Path.of(templateLocation);
        // Each classpath root is copied to a target dir, preserving the sub-path below the root:
        //  - templates land at the folder root (so `fragments/pdf.html` resolves, and `@@…@@` tokens work)
        //  - the logo lands under branding/ and the fonts under fonts/, matching where PdfRenderer looks.
        seedTree("pdfexport/templates/", base);
        seedTree("pdfexport/branding/", base.resolve("branding"));
        seedTree("pdfexport/fonts/", base.resolve("fonts"));
        // The operator-facing README explaining what may be edited and which tokens to keep.
        copyIfAbsent(resolver.getResource("classpath:pdfexport/README.md"), base.resolve("README.md"));
    }

    /** Copy every file under a classpath directory into {@code targetDir}, keeping the relative sub-path
     *  and skipping files that already exist (so operator edits survive restarts). */
    private void seedTree(String classpathDir, Path targetDir) {
        String marker = "/" + classpathDir;
        try {
            for (Resource resource : resolver.getResources("classpath*:" + classpathDir + "**")) {
                if (!resource.isReadable()) {
                    continue; // directory entry
                }
                String url = resource.getURL().toString();
                int idx = url.indexOf(marker);
                if (idx < 0) {
                    continue;
                }
                String relativePath = url.substring(idx + marker.length());
                if (relativePath.isBlank() || relativePath.endsWith("/")) {
                    continue;
                }
                copyIfAbsent(resource, targetDir.resolve(relativePath));
            }
        } catch (Exception e) { //NOSONAR - seeding is best-effort; the classpath fallback keeps export working
            log.warn("Could not seed PDF export resources from {} into {}: {}", classpathDir, targetDir, e.getMessage());
        }
    }

    private void copyIfAbsent(Resource resource, Path target) {
        try {
            if (Files.exists(target)) {
                return;
            }
            Files.createDirectories(target.getParent());
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Seeded PDF export template {}", target);
        } catch (Exception e) { //NOSONAR - one file failing must not abort the rest
            log.warn("Could not seed PDF export template {}: {}", target, e.getMessage());
        }
    }
}
