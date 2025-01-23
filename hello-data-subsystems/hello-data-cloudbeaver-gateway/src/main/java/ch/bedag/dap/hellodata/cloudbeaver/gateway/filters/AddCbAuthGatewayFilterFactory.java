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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

@Log4j2
@Component
public class AddCbAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public static String toCbRolesHeader(Collection<GrantedAuthority> authorities) {
        StringBuilder sb = new StringBuilder();
        for (GrantedAuthority grantedAuthority : authorities) {
            if (sb.length() > 0) {
                sb.append("|"); // roles are split with |
            }
            sb.append(grantedAuthority.getAuthority().toUpperCase(Locale.ENGLISH));
        }
        return sb.toString();
    }

    public static ServerWebExchange addCbAuthHeaders(ServerWebExchange exchange, JwtAuthenticationToken authenticationToken) {
        return exchange.mutate().request((r) -> {
            r.headers((httpHeaders) -> {
                log.debug("Requested URI Path: {}", exchange.getRequest().getURI().getPath());
                log.debug("\taddCbAuthHeaders for user {}", authenticationToken);
                log.debug("\temail: {}", authenticationToken.getToken().getClaims().get("email"));
                log.debug("\tgiven_name: {}", authenticationToken.getToken().getClaims().get("given_name"));
                log.debug("\tfamily_name: {}", authenticationToken.getToken().getClaims().get("family_name"));
                log.debug("\tauthorities: {}", toCbRolesHeader(authenticationToken.getAuthorities()));

                httpHeaders.add("X-User", (String) authenticationToken.getToken().getClaims().get("email"));
                httpHeaders.add("X-Role", toCbRolesHeader(authenticationToken.getAuthorities()));
                httpHeaders.add("X-First-name", (String) authenticationToken.getToken().getClaims().get("given_name"));
                httpHeaders.add("X-Last-name", (String) authenticationToken.getToken().getClaims().get("family_name"));
                log.info("Added headers to request {}", httpHeaders);
            });
        }).build();
    }

    public static ServerWebExchange removeAuthorizationHeader(ServerWebExchange exchange) {
        return exchange.mutate().request((r) -> {
            r.headers((httpHeaders) -> {
                httpHeaders.remove(SecurityConfig.AUTHORIZATION_HEADER_NAME); //don't need to pass the Authorization header to the cloudbeaver
            });
        }).build();
    }

    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            Mono<ServerWebExchange> webExchange = exchange.getPrincipal()
                    .log("auth-gateway-filter-factory", Level.INFO)
                    .filter((principal) -> principal instanceof JwtAuthenticationToken)
                    .cast(JwtAuthenticationToken.class)
                    .map((token) -> addCbAuthHeaders(exchange, token))
                    .map((token) -> removeAuthorizationHeader(exchange))
                    .defaultIfEmpty(exchange);
            Objects.requireNonNull(chain);
            return webExchange.flatMap(chain::filter);
        };
    }
}
