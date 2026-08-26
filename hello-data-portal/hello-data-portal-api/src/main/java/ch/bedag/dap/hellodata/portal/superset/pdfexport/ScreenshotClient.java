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
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.ChartScreenshotChunk;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.ChartScreenshotRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sends a screenshot request to a specific Superset sidecar and reassembles the streamed PNG
 *  chunks, keyed by specId. Reverse of the DashboardUpload chunking. */
@Log4j2
@Service
@RequiredArgsConstructor
public class ScreenshotClient {

    /** Actionable hint shown when Superset rejects the screenshot request outright (HTTP 404),
     *  which in practice means the THUMBNAILS feature (and a Superset worker to render them) is
     *  not enabled on that instance. */
    private static final String SCREENSHOTS_UNAVAILABLE =
            "Chart screenshots are unavailable for Superset instance '%s'. Enable the THUMBNAILS feature flag "
                    + "and a Superset worker to render screenshots, then retry the PDF export.";

    private final Connection connection;
    private final ObjectMapper objectMapper;

    /** @return specId -&gt; full PNG bytes, once every chart in the request has been received. */
    public Map<String, byte[]> fetchScreenshots(String instanceName, List<ChartScreenshotRequest.ChartSpec> specs, Duration timeout) {
        if (specs.isEmpty()) {
            return Map.of();
        }
        String subject = SlugifyUtil.slugify(instanceName + RequestReplySubject.EXPORT_DASHBOARD_SCREENSHOTS.getSubject());
        String inbox = connection.createInbox();
        Subscription sub = connection.subscribe(inbox);
        try {
            byte[] payload = objectMapper.writeValueAsBytes(new ChartScreenshotRequest(specs));
            connection.publish(subject, inbox, payload); // reply-to = our inbox

            Map<String, ByteArrayOutputStream> buffers = new HashMap<>();
            Map<String, String> errors = new HashMap<>();
            long deadline = System.nanoTime() + timeout.toNanos();

            while (true) {
                Duration wait = Duration.ofNanos(Math.max(0, deadline - System.nanoTime()));
                Message msg = sub.nextMessage(wait);
                if (msg == null) {
                    throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                            "Timed out waiting for chart screenshots from Superset instance '" + instanceName + "'.");
                }
                ChartScreenshotChunk chunk = objectMapper.readValue(msg.getData(), ChartScreenshotChunk.class);
                if (chunk.isLastMessage()) { // stream complete
                    if (chunk.getError() != null) {
                        throw renderFailure(instanceName, Map.of("*", chunk.getError()));
                    }
                    break;
                }
                if (chunk.getError() != null) { // one chart failed
                    errors.put(chunk.getSpecId(), chunk.getError());
                    continue;
                }
                buffers.computeIfAbsent(chunk.getSpecId(), k -> new ByteArrayOutputStream()).writeBytes(chunk.getContent());
            }
            if (!errors.isEmpty()) {
                throw renderFailure(instanceName, errors);
            }
            Map<String, byte[]> result = new LinkedHashMap<>();
            buffers.forEach((specId, buf) -> result.put(specId, buf.toByteArray()));
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Interrupted while awaiting chart screenshots from Superset instance '" + instanceName + "'.");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) { //NOSONAR - serialization / NATS failures surface as 502 upstream
            log.error("Failed to fetch chart screenshots from {}", instanceName, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to fetch chart screenshots from Superset instance '" + instanceName + "'.");
        } finally {
            try {
                sub.unsubscribe();
            } catch (RuntimeException e) {
                log.debug("Failed to unsubscribe screenshot inbox", e);
            }
        }
    }

    /** Build a clean 502 for a render failure: log the raw per-chart errors for debugging, but return
     *  a concise, actionable message. A 404 from Superset means the screenshot endpoint is gated off
     *  (THUMBNAILS feature disabled), so surface that specific hint. */
    private ResponseStatusException renderFailure(String instanceName, Map<String, String> errors) {
        log.warn("Chart screenshots failed for Superset instance {}: {}", instanceName, errors);
        boolean screenshotsDisabled = errors.values().stream().anyMatch(e -> e != null && e.contains("code=404"));
        String detail = screenshotsDisabled
                ? String.format(SCREENSHOTS_UNAVAILABLE, instanceName)
                : "Some charts could not be rendered for Superset instance '" + instanceName + "'.";
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, detail);
    }
}
