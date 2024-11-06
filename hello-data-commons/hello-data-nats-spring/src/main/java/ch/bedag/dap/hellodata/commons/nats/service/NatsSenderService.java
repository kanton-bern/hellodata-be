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

import ch.bedag.dap.hellodata.commons.nats.exception.NatsException;
import ch.bedag.dap.hellodata.commons.nats.util.NatsStreamUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.PublishOptions;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Log4j2
@Service
@RequiredArgsConstructor
public class NatsSenderService {
    private final Connection connection;
    private final ObjectMapper objectMapper;

    public <T> PublishAck publishMessageToJetStream(HDEvent event, T body) {
        if (!event.getDataClass().isInstance(body)) {
            throw new NatsException(String.format("Object '%s' must be of type %s when using the @%s event.", body, event.getDataClass(), event));
        }
        return publishMessageToJetStream(event.getStreamName(), event.getSubject(), body);
    }

    private <T> PublishAck publishMessageToJetStream(String streamName, String subjectName, T body) {
        try {
            NatsStreamUtil.createOrUpdateStream(connection.jetStreamManagement(), streamName, subjectName);
            JetStream js = connection.jetStream();

            String message;
            if (body instanceof String) {
                message = (String) body;
            } else {
                message = objectMapper.writeValueAsString(body);
            }

            log.trace("+=+=+=+= Publishing message {}, to the stream {} and subject {}", message, streamName, subjectName);
            log.debug("+=+=+=+= Publishing message to the stream {} and subject {}", streamName, subjectName);

            // Generate a MurmurHash3-based message ID
            String messageId = Hashing.murmur3_128().hashString(message, StandardCharsets.UTF_8).toString();

            NatsMessage natsMessage = NatsMessage.builder()
                    .subject(subjectName)
                    .data(message.getBytes(StandardCharsets.UTF_8))
                    .build();
            PublishOptions pubOpts = PublishOptions.builder()
                    .stream(streamName)
                    .messageId(messageId)
                    .build();

            return js.publish(natsMessage, pubOpts);
        } catch (JetStreamApiException | IOException ex) {
            throw new NatsException("Error publishing message", ex);
        }
    }
}
