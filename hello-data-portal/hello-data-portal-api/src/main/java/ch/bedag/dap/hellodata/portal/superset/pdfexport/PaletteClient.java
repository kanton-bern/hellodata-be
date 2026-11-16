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

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.DashboardPaletteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/** Fetches the builder palette (charts + markdown) of one dashboard from its Superset sidecar. */
@Log4j2
@Service
@RequiredArgsConstructor
public class PaletteClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final Connection connection;
    private final ObjectMapper objectMapper;

    public DashboardPaletteResponse fetchPalette(String instanceName, long dashboardId) {
        String subject = SlugifyUtil.slugify(instanceName + RequestReplySubject.GET_DASHBOARD_PALETTE.getSubject());
        try {
            byte[] payload = ("{\"dashboardId\":" + dashboardId + "}").getBytes(StandardCharsets.UTF_8);
            Message reply = connection.request(subject, payload, TIMEOUT);
            if (reply == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "No reply from superset sidecar " + instanceName);
            }
            JsonNode node = objectMapper.readTree(reply.getData());
            if (node.hasNonNull("error")) {
                throw new ResponseStatusException(BAD_GATEWAY, "Superset sidecar error: " + node.get("error").asText());
            }
            return objectMapper.treeToValue(node, DashboardPaletteResponse.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(BAD_GATEWAY, "Interrupted awaiting dashboard palette");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) { //NOSONAR
            throw new ResponseStatusException(BAD_GATEWAY, "Failed to fetch dashboard palette: " + e.getMessage());
        }
    }

    /** Markdown blocks for the palette, parsed from the dashboard's position_json. */
    public List<String> markdownBlocks(DashboardPaletteResponse palette) throws Exception {
        String raw = palette.getPositionJson() == null || palette.getPositionJson().isBlank() ? "{}" : palette.getPositionJson();
        JsonNode positionJson = objectMapper.readTree(raw);
        return SupersetLayoutParser.markdownCodes(positionJson);
    }
}
