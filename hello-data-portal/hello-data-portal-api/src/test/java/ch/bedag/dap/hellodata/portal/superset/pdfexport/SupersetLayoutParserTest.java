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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupersetLayoutParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /** markdownCodes returns only the MARKDOWN cells' raw source, in visual order. */
    @Test
    void extractsMarkdownCodesInOrder() throws Exception {
        String json = """
            {
              "ROOT_ID": {"type":"ROOT","children":["GRID_ID"]},
              "GRID_ID": {"type":"GRID","children":["MD_1","CH_1","MD_2"]},
              "MD_1":    {"type":"MARKDOWN","meta":{"code":"# First"},"children":[]},
              "CH_1":    {"type":"CHART","meta":{"chartId":1},"children":[]},
              "MD_2":    {"type":"MARKDOWN","meta":{"code":"## Second"},"children":[]}
            }
            """;

        List<String> codes = SupersetLayoutParser.markdownCodes(mapper.readTree(json));

        assertEquals(List.of("# First", "## Second"), codes);
    }

    /** Markdown nested inside tabs is still found, in depth-first visual order. */
    @Test
    void extractsMarkdownNestedInTabs() throws Exception {
        String json = """
            {
              "ROOT_ID": {"type":"ROOT","children":["TABS_1"]},
              "TABS_1":  {"type":"TABS","children":["TAB_A"]},
              "TAB_A":   {"type":"TAB","meta":{"text":"Tab A"},"children":["MD_1"]},
              "MD_1":    {"type":"MARKDOWN","meta":{"code":"# Intro"},"children":[]}
            }
            """;

        assertEquals(List.of("# Intro"), SupersetLayoutParser.markdownCodes(mapper.readTree(json)));
    }

    @Test
    void blankOrMissingLayoutYieldsNoCodes() throws Exception {
        assertTrue(SupersetLayoutParser.markdownCodes(mapper.createObjectNode()).isEmpty());
        assertTrue(SupersetLayoutParser.markdownCodes(mapper.readTree("{\"FOO\":{}}")).isEmpty());
    }
}
