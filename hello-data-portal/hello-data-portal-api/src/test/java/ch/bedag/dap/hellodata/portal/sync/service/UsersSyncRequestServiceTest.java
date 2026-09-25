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

import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.CommenceUsersSync;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsersSyncRequestServiceTest {

    private final UsersSyncService usersSyncService = mock(UsersSyncService.class);
    private final Connection natsConnection = mock(Connection.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UsersSyncRequestService service = new UsersSyncRequestService(usersSyncService, natsConnection, objectMapper, true, 60, 600, 600);
    private final Instant start = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void doesNothingWithoutRequest() {
        service.startSynchronizationIfDue(start.plusSeconds(3600));

        verify(usersSyncService, never()).startSynchronization();
    }

    @Test
    void startsOnceAfterQuietPeriod() {
        service.registerRequest(start);

        service.startSynchronizationIfDue(start.plusSeconds(30));
        verify(usersSyncService, never()).startSynchronization();

        service.startSynchronizationIfDue(start.plusSeconds(60));
        service.startSynchronizationIfDue(start.plusSeconds(120));
        verify(usersSyncService, times(1)).startSynchronization();
    }

    @Test
    void mergesRequestsComingInDuringQuietPeriod() {
        service.registerRequest(start);
        service.registerRequest(start.plusSeconds(50));

        service.startSynchronizationIfDue(start.plusSeconds(70));
        verify(usersSyncService, never()).startSynchronization();

        service.startSynchronizationIfDue(start.plusSeconds(110));
        verify(usersSyncService, times(1)).startSynchronization();
    }

    @Test
    void startsAfterMaxDelayEvenIfRequestsKeepComing() {
        for (int second = 0; second < 600; second += 30) {
            service.registerRequest(start.plusSeconds(second));
            service.startSynchronizationIfDue(start.plusSeconds(second));
        }
        verify(usersSyncService, never()).startSynchronization();

        service.registerRequest(start.plusSeconds(600));
        service.startSynchronizationIfDue(start.plusSeconds(600));
        verify(usersSyncService, times(1)).startSynchronization();
    }

    @Test
    void keepsMinIntervalBetweenSynchronizations() {
        service.registerRequest(start);
        service.startSynchronizationIfDue(start.plusSeconds(60));
        verify(usersSyncService, times(1)).startSynchronization();

        // e.g. a crash looping sidecar asking again
        service.registerRequest(start.plusSeconds(120));
        service.startSynchronizationIfDue(start.plusSeconds(300));
        service.startSynchronizationIfDue(start.plusSeconds(600));
        verify(usersSyncService, times(1)).startSynchronization();

        service.startSynchronizationIfDue(start.plusSeconds(660));
        verify(usersSyncService, times(2)).startSynchronization();
    }

    @Test
    void ignoresRequestsWhenDisabled() {
        UsersSyncRequestService disabled = new UsersSyncRequestService(usersSyncService, natsConnection, objectMapper, false, 60, 600, 600);

        disabled.onCommenceUsersSync(new CommenceUsersSync(ModuleType.SUPERSET, "superset", null));
        disabled.startSynchronizationIfDue(Instant.now().plusSeconds(3600));

        verify(usersSyncService, never()).startSynchronization();
    }

    @Test
    void repliesOkAndRegistersRequest() throws Exception {
        Message msg = request(objectMapper.writeValueAsBytes(new CommenceUsersSync(ModuleType.CLOUDBEAVER, "cloudbeaver", "dd1")));

        service.handleRequest(msg);
        service.startSynchronizationIfDue(Instant.now().plusSeconds(3600));

        verify(natsConnection).publish(eq("reply-inbox"), argThat((byte[] body) -> "OK".equals(new String(body, StandardCharsets.UTF_8))));
        verify(usersSyncService, times(1)).startSynchronization();
    }

    @Test
    void repliesErrorForInvalidRequest() {
        Message msg = request("not json".getBytes(StandardCharsets.UTF_8));

        service.handleRequest(msg);
        service.startSynchronizationIfDue(Instant.now().plusSeconds(3600));

        verify(natsConnection).publish(eq("reply-inbox"), argThat((byte[] body) -> new String(body, StandardCharsets.UTF_8).startsWith("ERROR")));
        verify(usersSyncService, never()).startSynchronization();
    }

    private static Message request(byte[] data) {
        Message msg = mock(Message.class);
        when(msg.getData()).thenReturn(data);
        when(msg.getReplyTo()).thenReturn("reply-inbox");
        return msg;
    }
}
