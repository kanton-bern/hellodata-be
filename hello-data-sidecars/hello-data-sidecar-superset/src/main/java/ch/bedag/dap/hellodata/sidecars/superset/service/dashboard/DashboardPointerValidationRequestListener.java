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
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.data.DashboardPointerValidationRequest;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.data.DashboardPointerValidationResponse;
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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardPointerValidationRequestListener {

    private final Connection natsConnection;
    private final SupersetClientProvider supersetClientProvider;
    private final ObjectMapper objectMapper;
    private final DashboardPointerValidationService pointerValidationService;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @PostConstruct
    public void listenForRequests() {
        String supersetSidecarSubject = SlugifyUtil.slugify(instanceName + RequestReplySubject.VALIDATE_DASHBOARD_POINTERS.getSubject());
        log.debug("/*-/*- Listening for messages on subject {}", supersetSidecarSubject);
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            try {
                DashboardPointerValidationRequest request = objectMapper.readValue(msg.getData(), DashboardPointerValidationRequest.class);
                Map<String, Boolean> validityByUrl = new HashMap<>();
                List<String> pointerUrls = request.getPointerUrls();
                if (pointerUrls != null && !pointerUrls.isEmpty()) {
                    try (SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance()) {
                        for (String pointerUrl : pointerUrls) {
                            if (pointerUrl != null && !validityByUrl.containsKey(pointerUrl)) {
                                validityByUrl.put(pointerUrl, pointerValidationService.isPointerValid(supersetClient, pointerUrl));
                            }
                        }
                    }
                }
                DashboardPointerValidationResponse response = new DashboardPointerValidationResponse(validityByUrl);
                natsConnection.publish(msg.getReplyTo(), objectMapper.writeValueAsBytes(response));
                msg.ack();
            } catch (Exception e) {
                log.error("Error validating dashboard pointers", e);
                // reply with an empty result so the portal can treat pointers as valid on failure
                try {
                    DashboardPointerValidationResponse empty = new DashboardPointerValidationResponse(new HashMap<>());
                    natsConnection.publish(msg.getReplyTo(), objectMapper.writeValueAsBytes(empty));
                } catch (Exception publishEx) {
                    log.error("Could not publish empty pointer validation response", publishEx);
                }
                msg.ack();
            }
        });
        dispatcher.subscribe(supersetSidecarSubject);
    }
}
