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
package ch.bedag.dap.hellodata.portal.sync.service;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.CommenceUsersSync;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static ch.bedag.dap.hellodata.commons.nats.service.UsersSyncTriggerService.OK_REPLY;
import static ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject.REQUEST_USERS_SYNC;

/**
 * Starts the users synchronization automatically when sidecars report that a subsystem (or the roles of a data domain) became ready.
 * Sidecars send a NATS request and get a reply once the request is registered, so they can send it again if the portal is not up yet.
 * Data domains show up one after another, so requests are merged and the synchronization starts once no new request came in
 * for the quiet period, or at the latest after the max delay.
 * Two automatic synchronizations are at least the min interval apart, so a restarting sidecar cannot flood the subsystems.
 */
@Log4j2
@Service
public class UsersSyncRequestService {

    private static final String QUEUE_GROUP = "portal-users-sync";

    private final UsersSyncService usersSyncService;
    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Duration quietPeriod;
    private final Duration maxDelay;
    private final Duration minInterval;

    private Instant firstRequestAt;
    private Instant lastRequestAt;
    private Instant lastSynchronizationAt;

    public UsersSyncRequestService(UsersSyncService usersSyncService,
                                   Connection natsConnection,
                                   ObjectMapper objectMapper,
                                   @Value("${hello-data.users-sync.auto.enabled:true}") boolean enabled,
                                   @Value("${hello-data.users-sync.auto.quiet-period-seconds:60}") long quietPeriodSeconds,
                                   @Value("${hello-data.users-sync.auto.max-delay-seconds:600}") long maxDelaySeconds,
                                   @Value("${hello-data.users-sync.auto.min-interval-seconds:600}") long minIntervalSeconds) {
        this.usersSyncService = usersSyncService;
        this.natsConnection = natsConnection;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.quietPeriod = Duration.ofSeconds(quietPeriodSeconds);
        this.maxDelay = Duration.ofSeconds(maxDelaySeconds);
        this.minInterval = Duration.ofSeconds(minIntervalSeconds);
    }

    @PostConstruct
    public void listenForRequests() {
        // queue group: only one portal instance answers each request
        Dispatcher dispatcher = natsConnection.createDispatcher(this::handleRequest);
        dispatcher.subscribe(REQUEST_USERS_SYNC.getSubject(), QUEUE_GROUP);
        log.debug("[autoSyncUsers] Listening for users synchronization requests on subject {}", REQUEST_USERS_SYNC.getSubject());
    }

    void handleRequest(Message msg) {
        try {
            onCommenceUsersSync(objectMapper.readValue(msg.getData(), CommenceUsersSync.class));
            reply(msg, OK_REPLY);
        } catch (IOException | RuntimeException e) {
            log.error("[autoSyncUsers] Could not handle users synchronization request", e);
            reply(msg, "ERROR: " + e.getMessage());
        }
    }

    private void reply(Message msg, String reply) {
        if (msg.getReplyTo() != null) {
            natsConnection.publish(msg.getReplyTo(), reply.getBytes(StandardCharsets.UTF_8));
        }
    }

    void onCommenceUsersSync(CommenceUsersSync commenceUsersSync) {
        if (!enabled) {
            log.debug("[autoSyncUsers] Automatic users synchronization disabled, ignoring request {}", commenceUsersSync);
            return;
        }
        log.info("[autoSyncUsers] Users synchronization requested by {}", commenceUsersSync);
        registerRequest(Instant.now());
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void startSynchronizationIfDue() {
        startSynchronizationIfDue(Instant.now());
    }

    synchronized void registerRequest(Instant now) {
        if (firstRequestAt == null) {
            firstRequestAt = now;
        }
        lastRequestAt = now;
    }

    synchronized void startSynchronizationIfDue(Instant now) {
        if (firstRequestAt == null) {
            return;
        }
        boolean quiet = !now.isBefore(lastRequestAt.plus(quietPeriod));
        boolean waitedTooLong = !now.isBefore(firstRequestAt.plus(maxDelay));
        boolean intervalPassed = lastSynchronizationAt == null || !now.isBefore(lastSynchronizationAt.plus(minInterval));
        if ((quiet || waitedTooLong) && intervalPassed) {
            log.info("[autoSyncUsers] Starting users synchronization requested by the sidecars");
            usersSyncService.startSynchronization();
            lastSynchronizationAt = now;
            firstRequestAt = null;
            lastRequestAt = null;
        }
    }
}
