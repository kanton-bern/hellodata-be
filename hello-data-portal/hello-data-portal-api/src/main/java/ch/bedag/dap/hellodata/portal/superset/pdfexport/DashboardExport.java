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

import java.util.Base64;
import java.util.List;

/** View model handed to the corporate-design Thymeleaf PDF template. */
public record DashboardExport(String title, List<Section> sections) {

    /** A group of items under one tab (blank title = no heading / no-tab dashboard). */
    public record Section(String title, List<Item> items) {
    }

    /** One cell in a section: a chart image or a rendered markdown block. */
    public sealed interface Item permits Chart, Markdown {
        String type();
    }

    public record Chart(String name, byte[] png) implements Item {
        @Override
        public String type() {
            return "chart";
        }

        /** Base64 data URI so the image embeds directly in the HTML. */
        public String dataUri() {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
        }
    }

    /** A dashboard markdown/text cell, already converted to HTML. */
    public record Markdown(String html) implements Item {
        @Override
        public String type() {
            return "markdown";
        }
    }
}
