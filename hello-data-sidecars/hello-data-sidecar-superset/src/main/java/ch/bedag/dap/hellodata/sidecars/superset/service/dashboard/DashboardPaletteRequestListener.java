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
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.DashboardPaletteResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.DashboardPaletteResponse.ChartRef;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the builder palette (charts + raw position_json for markdown extraction) of a single
 * dashboard on request from the portal. Small payload, so plain request/reply (no chunking).
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardPaletteRequestListener {

    private final Connection natsConnection;
    private final SupersetClientProvider supersetClientProvider;
    private final ObjectMapper objectMapper;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @PostConstruct
    public void listenForRequests() {
        String subject = SlugifyUtil.slugify(instanceName + RequestReplySubject.GET_DASHBOARD_PALETTE.getSubject());
        log.debug("/*-/*- Listening for dashboard-palette requests on subject {}", subject);
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            try (SupersetClient client = supersetClientProvider.getSupersetClientInstance()) {
                JsonObject req = JsonParser.parseString(new String(msg.getData(), StandardCharsets.UTF_8)).getAsJsonObject();
                int dashboardId = req.get("dashboardId").getAsInt();

                List<ChartRef> charts = new ArrayList<>();
                for (JsonElement el : client.getDashboardCharts(dashboardId)) {
                    JsonObject c = el.getAsJsonObject();
                    long id = c.has("id") && !c.get("id").isJsonNull() ? c.get("id").getAsLong() : 0L;
                    if (id == 0L) {
                        continue;
                    }
                    String name = c.has("slice_name") && !c.get("slice_name").isJsonNull() ? c.get("slice_name").getAsString() : "Chart " + id;
                    charts.add(new ChartRef(id, name));
                }
                String positionJson = client.getDashboardPositionJson(dashboardId);

                DashboardPaletteResponse response = new DashboardPaletteResponse(charts, positionJson);
                natsConnection.publish(msg.getReplyTo(), objectMapper.writeValueAsBytes(response));
                msg.ack();
            } catch (Exception e) { //NOSONAR
                log.error("Error building dashboard palette", e);
                JsonObject err = new JsonObject();
                err.addProperty("error", e.getMessage());
                natsConnection.publish(msg.getReplyTo(), err.toString().getBytes(StandardCharsets.UTF_8));
                msg.ack();
            }
        });
        dispatcher.subscribe(subject);
    }
}
