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
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class WebFluxLogFilter implements WebFilter, Ordered {
    final Logger log = LoggerFactory.getLogger(WebFluxLogFilter.class);

    @PostConstruct
    public void logAlive() {
        log.warn("Startup of WebFluxLogFilter");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange).doOnEach(signal -> {
            if (signal.isOnComplete() || signal.isOnError()) {
                log.warn("log :: \n\trequestId: {}, \n\tip: {}, \n\tmethod: {}, \n\tpath :{}, \n\theaders: {}, \n\tresponse :{}", exchange.getRequest().getId(),
                         exchange.getRequest().getRemoteAddress(), exchange.getRequest().getMethod(), exchange.getRequest().getURI(), exchange.getRequest()
                                                                                                                                              .getHeaders()
                                                                                                                                              .entrySet()
                                                                                                                                              .stream()
                                                                                                                                              .filter(stringListEntry -> !stringListEntry.getKey()
                                                                                                                                                                                         .equals(SecurityConfig.AUTHORIZATION_HEADER_NAME))
                                                                                                                                              .toList(),
                         exchange.getResponse().getStatusCode());
            }
        });
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.FIRST.getOrder();
    }
}
