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
package ch.bedag.dap.hellodata.cloudbeaver.gateway.filters;

import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Gateway filter that ensures WebSocket upgrade headers (Upgrade: websocket, Connection: Upgrade)
 * are present on the request. This is needed because Nginx Ingress may strip these headers
 * when proxying WebSocket connections to backend services.
 * <p>
 * Spring Cloud Gateway's {@code WebsocketRoutingFilter} / {@code HandshakeWebSocketService}
 * requires these headers to be present even on routes with {@code ws://} URI scheme.
 * Without them, the handshake fails with "Invalid 'Upgrade' header" (400 BAD_REQUEST).
 */
@Log4j2
@Component
public class EnsureWebSocketHeadersGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String upgradeHeader = headers.getUpgrade();
            boolean needsFixup = !"websocket".equalsIgnoreCase(upgradeHeader);

            if (needsFixup) {
                log.debug("WebSocket route matched but Upgrade header is missing or invalid (value: '{}'), injecting Upgrade/Connection headers", upgradeHeader);
            }

            log.debug("WS handshake request URI: {}, headers present: {}", exchange.getRequest().getURI(), headers.keySet());

            return chain.filter(
                    exchange.mutate()
                            .request(r -> r.headers(h -> {
                                // Always ensure Upgrade/Connection are set correctly (single value, no duplicates)
                                h.set(HttpHeaders.UPGRADE, "websocket");
                                h.set(HttpHeaders.CONNECTION, "Upgrade");
                                // Ensure Sec-WebSocket-Version is present (required by RFC 6455)
                                if (!h.containsKey("Sec-WebSocket-Version")) {
                                    h.set("Sec-WebSocket-Version", "13");
                                }
                            }))
                            .build()
            );
        };
    }
}
