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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject.REQUEST_USERS_SYNC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsersSyncTriggerServiceTest {

    private final Connection natsConnection = mock(Connection.class);
    private final UsersSyncTriggerService service = new UsersSyncTriggerService(natsConnection, new ObjectMapper());

    @Test
    void sendsSubsystemReadyOnlyOnceWhenConfirmed() throws InterruptedException {
        Message ok = reply("OK");
        when(natsConnection.request(eq(REQUEST_USERS_SYNC.getSubject()), any(byte[].class), any(Duration.class))).thenReturn(ok);

        service.subsystemReady(ModuleType.SUPERSET, "superset");
        service.subsystemReady(ModuleType.SUPERSET, "superset");

        verify(natsConnection, times(1)).request(eq(REQUEST_USERS_SYNC.getSubject()),
                argThat((byte[] body) -> new String(body, StandardCharsets.UTF_8).contains("\"moduleType\":\"SUPERSET\"")), any(Duration.class));
    }

    @Test
    void sendsOncePerDataDomain() throws InterruptedException {
        Message ok = reply("OK");
        when(natsConnection.request(eq(REQUEST_USERS_SYNC.getSubject()), any(byte[].class), any(Duration.class))).thenReturn(ok);

        service.dataDomainRolesReady(ModuleType.CLOUDBEAVER, "cloudbeaver", "dd1");
        service.dataDomainRolesReady(ModuleType.CLOUDBEAVER, "cloudbeaver", "dd1");
        service.dataDomainRolesReady(ModuleType.CLOUDBEAVER, "cloudbeaver", "dd2");

        verify(natsConnection, times(2)).request(eq(REQUEST_USERS_SYNC.getSubject()), any(byte[].class), any(Duration.class));
    }

    @Test
    void sendsAgainWhenPortalDidNotConfirm() throws InterruptedException {
        Message ok = reply("OK");
        Message error = reply("ERROR: boom");
        when(natsConnection.request(eq(REQUEST_USERS_SYNC.getSubject()), any(byte[].class), any(Duration.class)))
                .thenReturn(null) // no portal listening / timeout
                .thenReturn(error)
                .thenReturn(ok);

        for (int i = 0; i < 4; i++) {
            service.subsystemReady(ModuleType.AIRFLOW, "airflow");
        }

        verify(natsConnection, times(3)).request(eq(REQUEST_USERS_SYNC.getSubject()), any(byte[].class), any(Duration.class));
    }

    @Test
    void sendsAgainWhenRequestThrows() throws InterruptedException {
        Message ok = reply("OK");
        when(natsConnection.request(eq(REQUEST_USERS_SYNC.getSubject()), any(byte[].class), any(Duration.class)))
                .thenThrow(new IllegalStateException("connection closed"))
                .thenReturn(ok);

        service.subsystemReady(ModuleType.DBT_DOCS, "dbt-docs");
        service.subsystemReady(ModuleType.DBT_DOCS, "dbt-docs");
        service.subsystemReady(ModuleType.DBT_DOCS, "dbt-docs");

        verify(natsConnection, times(2)).request(eq(REQUEST_USERS_SYNC.getSubject()), any(byte[].class), any(Duration.class));
    }

    private static Message reply(String data) {
        Message message = mock(Message.class);
        when(message.getData()).thenReturn(data.getBytes(StandardCharsets.UTF_8));
        return message;
    }
}
