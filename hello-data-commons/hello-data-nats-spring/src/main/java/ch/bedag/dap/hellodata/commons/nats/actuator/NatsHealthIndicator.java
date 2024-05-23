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
package ch.bedag.dap.hellodata.commons.nats.actuator;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.util.StringUtils;

@Log4j2
public class NatsHealthIndicator extends AbstractHealthIndicator {
    private final Connection natsConnection;
    @Value("${spring.application.name}")
    private String appName;
    @Value("${hello-data.instance.name:}")
    private String instanceName;
    private String subject;
    private String subjectBase64;

    /**
     * Generate subject and listen for connection messages within the request/reply pattern
     */
    @PostConstruct
    public void listenForConnectionCheckRequests() {
        String subjectBase = SlugifyUtil.slugify(RequestReplySubject.NATS_CONNECTION_HEALTH_CHECK.getSubject());
        subject = this.appName + "-" + subjectBase + (StringUtils.hasText(instanceName) ? "-" + instanceName : "");
        subjectBase64 = new String(Base64.getEncoder().encode(subject.getBytes(StandardCharsets.UTF_8)));
        log.debug("[NATS connection check] Listening for messages on subject {}", subject);
        Dispatcher dispatcher = natsConnection.createDispatcher((msg) -> {
            String message = new String(msg.getData());
            log.debug("[NATS connection check] Received request for NATS connection check {}", message);
            natsConnection.publish(msg.getReplyTo(), "OK".getBytes(StandardCharsets.UTF_8));
            msg.ack();
        });
        dispatcher.subscribe(subjectBase64);
    }

    public NatsHealthIndicator(Connection natsConnection) {
        this.natsConnection = natsConnection;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        String connectionName = natsConnection.getOptions().getConnectionName();
        if (connectionName != null) {
            builder.withDetail("name", connectionName);
        }

        builder.withDetail("status", natsConnection.getStatus());
        switch (natsConnection.getStatus()) {
            case CONNECTED -> checkRequestReplyConnection(builder);
            case CONNECTING, RECONNECTING -> builder.outOfService();
            default -> // CLOSED, DISCONNECTED
                    builder.down();
        }
    }

    /**
     * Utilize the connection to be sure if request/reply pattern works (even though the connection is up)
     *
     * @param builder health builder to set status
     */
    private void checkRequestReplyConnection(Health.Builder builder) {
        log.debug("[NATS connection check] Sending request to subjectBase: {}", subject);
        Message reply;
        try {
            reply = natsConnection.request(subjectBase64, subject.getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(10));
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                log.error("[NATS connection check] Could not connect to NATS", exception);
                Thread.currentThread().interrupt();
            }
            log.error("[NATS connection check] Could not connect to NATS", exception);
            builder.down();
            return;
        }
        if (reply == null) {
            log.warn("[NATS connection check] Reply is null, please verify NATS connection");
            builder.down();
            return;
        }
        reply.ack();
        log.debug("[NATS connection check] Reply received: " + new String(reply.getData()));
        builder.up();
    }
}
