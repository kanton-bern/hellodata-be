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

import ch.bedag.dap.hellodata.cloudbeaver.gateway.config.SecurityConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collection;
import java.util.Locale;

@Log4j2
@Component
public class AddCbAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public static String toCbRolesHeader(Collection<? extends GrantedAuthority> authorities) {
        StringBuilder sb = new StringBuilder();
        for (GrantedAuthority grantedAuthority : authorities) {
            if (!sb.isEmpty()) {
                sb.append("|"); // roles are split with |
            }
            sb.append(grantedAuthority.getAuthority().toUpperCase(Locale.ENGLISH));
        }
        return sb.toString();
    }

    public static ServerWebExchange addCbAuthHeaders(ServerWebExchange exchange, JwtAuthenticationToken authenticationToken) {
        String email = (String) authenticationToken.getToken().getClaims().get("email");
        Object givenName = authenticationToken.getToken().getClaims().get("given_name");
        Object familyName = authenticationToken.getToken().getClaims().get("family_name");
        String cbRolesHeader = toCbRolesHeader(authenticationToken.getAuthorities());

        log.debug("Requested URI Path: {}", exchange.getRequest().getURI().getPath());
        log.debug("\taddCbAuthHeaders for user {}", authenticationToken);
        log.debug("\temail: {}", email);
        log.debug("\tgiven_name: {}", givenName);
        log.debug("\tfamily_name: {}", familyName);
        log.debug("\tauthorities: {}", cbRolesHeader);

        log.debug("\tX-User header: {}", email);
        log.debug("\tX-Role header: {}", cbRolesHeader);
        ServerHttpRequest serverHttpRequest = exchange.getRequest()
                .mutate()
                .headers(h -> {
                    // Using .set() inside the headers consumer ensures that
                    // any existing headers (from Ingress or previous hops) are overwritten,
                    // preventing duplicate headers like Upgrade=[websocket, websocket]
                    h.set("X-User", email);
                    h.set("X-Role", cbRolesHeader);
                    h.set("X-First-name", (String) givenName);
                    h.set("X-Last-name", (String) familyName);
                    // Remove Authorization header - CloudBeaver with reverseProxy auth
                    // only needs X-User/X-Role. Keeping the large JWT token unnecessarily
                    // increases header size which can cause the backend to reject with 400.
                    h.remove(SecurityConfig.AUTHORIZATION_HEADER_NAME);
                })
                .build();
        return exchange.mutate().request(serverHttpRequest).build();
    }


    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> exchange.getPrincipal()
                // Check if the principal exists and is of type JwtAuthenticationToken
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                // If authenticated, transform the exchange by adding headers
                .map(token -> addCbAuthHeaders(exchange, token))
                // If not authenticated or empty principal, fall back to the original exchange.
                // This ensures the chain continues and avoids breaking the WebSocket handshake
                // with a premature connection close (which causes error 1006/400)
                .defaultIfEmpty(exchange)
                // FlatMap into the filter chain. Mono<Void> from chain.filter(exchange)
                // is now correctly handled because flatMap receives the emitted exchange
                .flatMap(chain::filter);
    }
}
