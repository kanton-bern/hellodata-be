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

import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Deprecated(forRemoval = true)
@Log4j2
@Component
public class RemoveRequestCookieGatewayFilterFactory extends AbstractGatewayFilterFactory<RemoveRequestCookieGatewayFilterFactory.Config> {

    public RemoveRequestCookieGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            // Get the existing cookies
            String cookieHeader = headers.getFirst(HttpHeaders.COOKIE);
            if (cookieHeader != null) {
                // Remove the specific cookie
                //String updatedCookies = removeCookie(cookieHeader, SecurityConfig.ACCESS_TOKEN_COOKIE_NAME);
                // Set the modified cookies back to the headers
                //exchange.getRequest().mutate().header(HttpHeaders.COOKIE, updatedCookies);
                // Continue the filter chain
                return chain.filter(exchange);
            }
            // Continue the filter chain
            return chain.filter(exchange);
        };
    }

    protected String removeCookie(String cookieHeader, String cookieName) {
        // Split the cookies
        String[] cookies = cookieHeader.split(";");

        // Build the updated cookie header without the specified cookie
        StringBuilder updatedCookies = new StringBuilder();
        for (String cookie : cookies) {
            String trimmedCookie = cookie.trim();
            if (!trimmedCookie.startsWith(cookieName + "=")) {
                if (updatedCookies.length() > 0) {
                    updatedCookies.append("; ");
                }
                updatedCookies.append(trimmedCookie);
            }
        }

        return updatedCookies.toString();
    }

    public static class Config {
        private String cookieName;

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }
    }
}
