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
package ch.bedag.dap.hellodata.sidecars.superset.service.query;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.query.response.superset.SupersetQueryResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryListRequestListenerTest {

    private final Connection natsConnection = mock(Connection.class);
    private final SupersetClientProvider supersetClientProvider = mock(SupersetClientProvider.class);
    private final SupersetClient supersetClient = mock(SupersetClient.class);

    private MessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        when(natsConnection.createDispatcher(any(MessageHandler.class))).thenReturn(mock(Dispatcher.class));
        when(supersetClientProvider.getSupersetClientInstance()).thenReturn(supersetClient);

        new QueryListRequestListener(natsConnection, supersetClientProvider, new ObjectMapper()).listenForRequests();

        ArgumentCaptor<MessageHandler> messageHandlerCaptor = ArgumentCaptor.forClass(MessageHandler.class);
        verify(natsConnection).createDispatcher(messageHandlerCaptor.capture());
        messageHandler = messageHandlerCaptor.getValue();
    }

    @Test
    void requestsQueriesOldestFirst() throws Exception {
        SupersetQueryResponse response = new SupersetQueryResponse();
        response.setResult(List.of());
        when(supersetClient.queriesFiltered(any(), anyInt(), anyInt(), anyString(), anyString())).thenReturn(response);

        messageHandler.onMessage(request("{\"filters\":[],\"page\":3,\"pageSize\":1000}"));

        verify(supersetClient).queriesFiltered(any(), eq(3), eq(1000), eq("changed_on"), eq("asc"));
    }

    private static Message request(String body) {
        Message message = mock(Message.class);
        when(message.getData()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(message.getReplyTo()).thenReturn("reply-subject");
        return message;
    }
}
