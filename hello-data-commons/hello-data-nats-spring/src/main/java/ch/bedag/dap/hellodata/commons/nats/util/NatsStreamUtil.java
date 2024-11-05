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
package ch.bedag.dap.hellodata.commons.nats.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@UtilityClass
public class NatsStreamUtil {

    public static StreamInfo createOrUpdateStream(JetStreamManagement jsm, String streamName, String subject) throws IOException, JetStreamApiException {
        StreamInfo streamInfo = getStreamInfoOrNull(jsm, streamName);
        log.debug("Create or update stream {}, streamInfo {}", streamName, streamInfo);
        if (streamInfo == null) {
            StreamInfo stream = createStream(jsm, streamName, StorageType.File, subject);
            log.debug("Created stream {}", getObjectMapper().writeValueAsString(stream));
            return stream;
        }

        StreamConfiguration streamConfiguration = streamInfo.getConfiguration();
        if (!streamConfiguration.getSubjects().contains(subject)) {
            streamConfiguration.getSubjects().add(subject);
            streamConfiguration = StreamConfiguration.builder(streamConfiguration).subjects(streamConfiguration.getSubjects()).build();
            streamInfo = jsm.updateStream(streamConfiguration);
            log.debug("Existing stream {} was updated, has subject(s) {}", streamName, streamInfo.getConfiguration().getSubjects());
            log.debug("Updated stream configuration {}", getObjectMapper().writeValueAsString(streamConfiguration));
        }

        return streamInfo;
    }

    public static StreamInfo getStreamInfoOrNull(JetStreamManagement jsm, String streamName) throws IOException, JetStreamApiException {
        try {
            return jsm.getStreamInfo(streamName);
        } catch (JetStreamApiException jsae) {
            if (jsae.getErrorCode() == HttpStatus.NOT_FOUND.value()) {
                return null;
            }
            throw jsae;
        }
    }

    private static StreamInfo createStream(JetStreamManagement jsm, String streamName, StorageType storageType, String... subjects) throws IOException, JetStreamApiException {
        int maxMessages = 2000;
        int messageMaxAgeMinutes = 5;
        StreamConfiguration sc = StreamConfiguration.builder()
                .name(streamName)
                .storageType(storageType)
                .subjects(subjects)
                .retentionPolicy(RetentionPolicy.Limits)
                .discardPolicy(DiscardPolicy.Old)
                .maxAge(Duration.ofMinutes(messageMaxAgeMinutes))
                .duplicateWindow(Duration.ofMinutes(messageMaxAgeMinutes))
                .maxMessages(maxMessages)
                .build();
        StreamInfo si = jsm.addStream(sc);
        log.debug("Created stream {} with subject(s) {}", streamName, si.getConfiguration().getSubjects());
        return si;
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
