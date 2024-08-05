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
package ch.bedag.dap.hellodata.jupyterhub.gateway.filters;

import ch.bedag.dap.hellodata.jupyterhub.gateway.config.SecurityConfig;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * This filter is used to propagate the auth.access_token cookie internally to the spring-cloud-gateway security filter as an Auth Header.
 * (to embed the iframe)
 */
@Log4j2
public class TokenAuthenticationFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.info("\n\n-->TokenAuthenticationFilter");
        log.info("\t--->Requested URI Path: \n\t{}", exchange.getRequest().getURI().getPath());
        log.info("\t--->existing cookies: \n\t{}",
                 exchange.getRequest().getCookies().entrySet().stream().map(stringListEntry -> "\n\t" + stringListEntry.getValue() + "\n").toList());

        MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();
        ServerHttpRequest request = exchange.getRequest().mutate().headers(httpHeaders -> {
            if (!cookies.isEmpty()) {
                List<HttpCookie> cookie = cookies.get(SecurityConfig.ACCESS_TOKEN_COOKIE_NAME);
                if (cookie != null && cookie.size() == 1) {
                    String token = cookie.get(0).getValue();
                    httpHeaders.add(SecurityConfig.AUTHORIZATION_HEADER_NAME, "Bearer " + token);
                }
            }
        }).build();
        log.info("\t--->Added headers: {}", exchange.getRequest().getHeaders().entrySet().stream().map(entry -> "\n\t" + entry.getKey() + ": " + entry.getValue() + "\n").toList());
        return chain.filter(exchange.mutate().request(request).build());
    }
}
