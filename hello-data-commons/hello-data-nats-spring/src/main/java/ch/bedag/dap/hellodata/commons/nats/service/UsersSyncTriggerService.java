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
package ch.bedag.dap.hellodata.commons.nats.service;

import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.CommenceUsersSync;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject.REQUEST_USERS_SYNC;

/**
 * Lets a sidecar ask the portal for a users synchronization once its subsystem is ready.
 * Uses request/reply so a request only counts as sent when the portal confirmed it. When the portal is not reachable,
 * the request is sent again the next time the sidecar reports the same readiness.
 * The portal merges the requests coming from all sidecars.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class UsersSyncTriggerService {

    public static final String OK_REPLY = "OK";
    private static final Duration REPLY_TIMEOUT = Duration.ofSeconds(5);

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final Set<String> confirmedTriggers = ConcurrentHashMap.newKeySet();

    /**
     * Sent when the subsystem responded and its roles are published
     */
    public void subsystemReady(ModuleType moduleType, String instanceName) {
        requestOnce("subsystem", new CommenceUsersSync(moduleType, instanceName, null));
    }

    /**
     * Sent when the roles for the given data domain were created in the subsystem
     */
    public void dataDomainRolesReady(ModuleType moduleType, String instanceName, String contextKey) {
        requestOnce("data-domain:" + contextKey, new CommenceUsersSync(moduleType, instanceName, contextKey));
    }

    private void requestOnce(String triggerKey, CommenceUsersSync commenceUsersSync) {
        if (confirmedTriggers.contains(triggerKey)) {
            return;
        }
        if (requestUsersSync(commenceUsersSync)) {
            confirmedTriggers.add(triggerKey);
            log.info("Portal confirmed the users synchronization request: {}", commenceUsersSync);
        } else {
            log.warn("Portal did not confirm the users synchronization request {}, it will be sent again on the next run", commenceUsersSync);
        }
    }

    private boolean requestUsersSync(CommenceUsersSync commenceUsersSync) {
        try {
            byte[] body = objectMapper.writeValueAsString(commenceUsersSync).getBytes(StandardCharsets.UTF_8);
            Message reply = natsConnection.request(REQUEST_USERS_SYNC.getSubject(), body, REPLY_TIMEOUT);
            return reply != null && reply.getData() != null && OK_REPLY.equals(new String(reply.getData(), StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Could not request users synchronization from the portal", e);
            return false;
        }
    }
}
