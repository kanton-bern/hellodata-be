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
package ch.bedag.dap.hellodata.sidecars.superset.service.dashboard;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.ChartScreenshotChunk;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.ChartScreenshotRequest;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Renders chart screenshots on request from the portal and streams the PNG bytes back to the
 * request's reply-inbox, chunked to stay under the NATS max payload (mirrors the DashboardUpload
 * chunking, reversed). Each chunk carries the request's specId so the portal reassembles the same
 * chart placed at two sizes separately.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class ChartScreenshotRequestListener {

    private static final int MAX_CHUNK_SIZE = 512 * 1024;

    private final Connection natsConnection;
    private final SupersetClientProvider supersetClientProvider;
    private final ObjectMapper objectMapper;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Value("${hello-data.screenshot.timeout-seconds:90}")
    private long timeoutSeconds;

    @Value("${hello-data.screenshot.poll-interval-millis:2000}")
    private long pollIntervalMillis;

    /** Max charts screenshotted in parallel; bounds load on Superset's browser workers while keeping
     *  a multi-chart export well under the portal's overall timeout. */
    @Value("${hello-data.screenshot.concurrency:4}")
    private int concurrency;

    @PostConstruct
    public void listenForRequests() {
        String subject = SlugifyUtil.slugify(instanceName + RequestReplySubject.EXPORT_DASHBOARD_SCREENSHOTS.getSubject());
        log.debug("/*-/*- Listening for chart-screenshot requests on subject {}", subject);
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            String replyTo = msg.getReplyTo();
            try (SupersetClient admin = supersetClientProvider.getSupersetClientInstance()) {
                ChartScreenshotRequest request = objectMapper.readValue(msg.getData(), ChartScreenshotRequest.class);
                List<ChartScreenshotRequest.ChartSpec> charts = request.getCharts() == null ? List.of() : request.getCharts();
                // Render as the requesting user (their RLS applies) when an email is supplied; otherwise
                // render as the admin/technical account (the thumbnail selenium user).
                String email = request.getUserEmail();
                SupersetClient render = email == null || email.isBlank() ? admin : admin.asUser(email);
                // Render charts in parallel (bounded) so a multi-chart export stays well within the
                // portal's overall timeout; each renderAndStream handles its own errors and streams a
                // chunk, so the stream still completes even if some charts fail.
                int poolSize = Math.max(1, Math.min(concurrency, charts.size()));
                ExecutorService pool = Executors.newFixedThreadPool(poolSize);
                try {
                    List<Future<?>> futures = new ArrayList<>();
                    for (ChartScreenshotRequest.ChartSpec spec : charts) {
                        futures.add(pool.submit(() -> renderAndStream(render, spec, replyTo)));
                    }
                    for (Future<?> future : futures) {
                        future.get();
                    }
                    publish(replyTo, finalMarker(null));
                } finally {
                    pool.shutdownNow();
                    if (render != admin) {
                        render.close();
                    }
                }
                msg.ack();
            } catch (Exception e) { //NOSONAR - any failure must still close the stream
                log.error("Error rendering chart screenshots", e);
                publish(replyTo, finalMarker(e.getMessage()));
                msg.ack();
            }
        });
        dispatcher.subscribe(subject);
    }

    private void renderAndStream(SupersetClient client, ChartScreenshotRequest.ChartSpec spec, String replyTo) {
        try {
            String cacheKey = client.triggerChartScreenshot((int) spec.getChartId(), spec.getWidth(), spec.getHeight());
            byte[] png = poll(client, (int) spec.getChartId(), cacheKey);
            streamChart(spec, png, replyTo);
        } catch (Exception e) { //NOSONAR - isolate one chart's failure from the rest
            log.error("Screenshot failed for chart {} (spec {})", spec.getChartId(), spec.getSpecId(), e);
            publish(replyTo, new ChartScreenshotChunk(spec.getSpecId(), spec.getChartId(), new byte[0], 0, true, false, e.getMessage()));
        }
    }

    /** Poll Superset until the PNG is ready or the timeout elapses. */
    private byte[] poll(SupersetClient client, int chartId, String cacheKey) throws Exception {
        long deadline = System.nanoTime() + timeoutSeconds * 1_000_000_000L;
        while (true) {
            var result = client.fetchChartScreenshot(chartId, cacheKey);
            if (result.isPresent()) {
                return result.get();
            }
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException("Timed out waiting for screenshot of chart " + chartId);
            }
            Thread.sleep(pollIntervalMillis);
        }
    }

    /** Split one chart's PNG into &lt;=MAX_CHUNK_SIZE chunks; the final one flags lastChunk. */
    private void streamChart(ChartScreenshotRequest.ChartSpec spec, byte[] png, String replyTo) {
        int total = Math.max(1, (int) Math.ceil(png.length / (double) MAX_CHUNK_SIZE));
        for (int i = 0; i < total; i++) {
            int from = i * MAX_CHUNK_SIZE;
            int to = Math.min(png.length, from + MAX_CHUNK_SIZE);
            boolean last = i == total - 1;
            publish(replyTo, new ChartScreenshotChunk(spec.getSpecId(), spec.getChartId(), Arrays.copyOfRange(png, from, to), i + 1, last, false, null));
        }
    }

    private ChartScreenshotChunk finalMarker(String error) {
        return new ChartScreenshotChunk(null, 0, new byte[0], 0, false, true, error);
    }

    private void publish(String replyTo, ChartScreenshotChunk chunk) {
        try {
            natsConnection.publish(replyTo, objectMapper.writeValueAsBytes(chunk));
        } catch (Exception e) { //NOSONAR
            log.error("Failed to publish screenshot chunk for chart {}", chunk.getChartId(), e);
        }
    }
}
