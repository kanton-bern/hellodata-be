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
package ch.bedag.dap.hellodata.portal.base.sse;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal server-sent-events hub that lets the portal-api push events to a specific logged-in user's
 * browser tabs in near real time. Keyed by the portal user id (== Keycloak subject == DB id), a user
 * can have several open emitters (multiple tabs); dead emitters are pruned on send/completion.
 *
 * <p>Currently used to signal {@code airflow-roles-changed} so an open Airflow 3 orchestration iframe
 * reloads and re-syncs the user's freshly reconciled roles instantly, instead of waiting for the next
 * token renewal (see {@code AirflowKeycloakRoleService}).
 */
@Log4j2
@Service
public class SseService {

    public static final String EVENT_AIRFLOW_ROLES_CHANGED = "airflow-roles-changed";

    /**
     * The stream is capped so a wedged connection cannot linger forever; the browser's EventSource
     * reconnects transparently and re-authenticates with a fresh cookie, so this is not user-visible.
     */
    private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<String, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> userEmitters = emittersByUserId.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        try {
            // Prime the stream so the browser considers the connection open right away.
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        log.debug("SSE subscribe for user {} ({} open emitter(s))", userId, userEmitters.size());
        return emitter;
    }

    public void sendToUser(String userId, String eventName, String data) {
        List<SseEmitter> userEmitters = emittersByUserId.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) { //NOSONAR - best effort, a broken client must not break the caller
                remove(userId, emitter);
            }
        }
        log.debug("Sent SSE '{}' to user {}", eventName, userId);
    }

    private void remove(String userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emittersByUserId.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emittersByUserId.remove(userId);
            }
        }
    }
}
