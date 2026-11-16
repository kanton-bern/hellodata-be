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
package ch.bedag.dap.hellodata.portal.base.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Standalone Thymeleaf engine for the Superset PDF export. Kept separate from the email
 * {@code SpringTemplateEngine} so the two template roots never collide and openhtmltopdf gets
 * well-formed XHTML. Injected via {@code @Qualifier("pdfTemplateEngine")}.
 *
 * <p>Templates are read from the editable on-disk location ({@code hello-data.pdf-export.template-location},
 * seeded by {@link ch.bedag.dap.hellodata.portal.superset.pdfexport.PdfTemplateProvisioner}) first, then
 * fall back to the packaged classpath defaults — so a missing or removed file can never break the export.
 * The file resolver is non-cacheable so template edits take effect on the next export without a restart.
 */
@Configuration
public class PdfExportTemplateConfig {

    /** Empty default here; the running app sets it via application.yml (/storage/pdf-template). When
     *  blank (e.g. a plain unit test constructing this config) only the classpath resolver is used. */
    @Value("${hello-data.pdf-export.template-location:}")
    private String templateLocation;

    @Bean
    public TemplateEngine pdfTemplateEngine() {
        Set<ITemplateResolver> resolvers = new LinkedHashSet<>();

        if (templateLocation != null && !templateLocation.isBlank()) {
            FileTemplateResolver fileResolver = new FileTemplateResolver();
            fileResolver.setPrefix(templateLocation.endsWith("/") ? templateLocation : templateLocation + "/");
            fileResolver.setSuffix(".html");
            fileResolver.setTemplateMode(TemplateMode.HTML);
            fileResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
            fileResolver.setCacheable(false);      // pick up on-disk edits without a restart
            fileResolver.setCheckExistence(true);  // fall through to the classpath default when a file is absent
            fileResolver.setOrder(1);
            resolvers.add(fileResolver);
        }

        ClassLoaderTemplateResolver classpathResolver = new ClassLoaderTemplateResolver();
        classpathResolver.setPrefix("pdfexport/templates/");
        classpathResolver.setSuffix(".html");
        classpathResolver.setTemplateMode(TemplateMode.HTML);
        classpathResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        classpathResolver.setCacheable(true);
        classpathResolver.setCheckExistence(true);
        classpathResolver.setOrder(2);
        resolvers.add(classpathResolver);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolvers(resolvers);
        return engine;
    }
}
