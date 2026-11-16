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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/** Walks a dashboard's {@code position_json} (depth-first from ROOT_ID) to recover MARKDOWN cells
 *  in visual order. Ported from the PoC SupersetClient; pure and unit-testable. */
public final class SupersetLayoutParser {

    private SupersetLayoutParser() {
    }

    /** Raw markdown source of every MARKDOWN cell, in visual order. */
    public static List<String> markdownCodes(JsonNode positionJson) {
        List<String> out = new ArrayList<>();
        if (positionJson == null || !positionJson.has("ROOT_ID")) {
            return out;
        }
        walk(positionJson, "ROOT_ID", out);
        return out;
    }

    private static void walk(JsonNode layout, String nodeId, List<String> out) {
        JsonNode node = layout.path(nodeId);
        if (node.isMissingNode()) {
            return;
        }
        if ("MARKDOWN".equals(node.path("type").asText(""))) {
            String code = node.path("meta").path("code").asText("");
            if (!code.isBlank()) {
                out.add(code);
            }
        }
        for (JsonNode child : node.path("children")) {
            walk(layout, child.asText(), out);
        }
    }
}
