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
package ch.bedag.dap.hellodata.sidecars.superset.client;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupersetApiRequestBuilderTest {

    private static final String HOST = "superset.example.com";
    private static final int PORT = 8088;
    private static final String AUTH_TOKEN = "auth-token";
    private static final String CSRF_TOKEN = "csrf-token";
    private static final String SESSION_COOKIE = "session=abc";

    @Test
    void deleteChartsRequestCarriesCsrfTokenAndSessionCookie() throws URISyntaxException {
        HttpUriRequest request = SupersetApiRequestBuilder.getDeleteChartsRequest(HOST, PORT, AUTH_TOKEN, List.of(1, 2, 3), CSRF_TOKEN, SESSION_COOKIE);

        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getFirstHeader("X-CSRFToken").getValue()).isEqualTo(CSRF_TOKEN);
        assertThat(request.getFirstHeader("Cookie").getValue()).isEqualTo(SESSION_COOKIE);
        assertThat(decodedQuery(request)).contains("!(1,2,3)");
    }

    @Test
    void deleteDatasetRequestCarriesCsrfTokenAndSessionCookie() throws URISyntaxException {
        HttpUriRequest request = SupersetApiRequestBuilder.getDeleteDatasetRequest(HOST, PORT, AUTH_TOKEN, 42, CSRF_TOKEN, SESSION_COOKIE);

        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getURI().getPath()).isEqualTo("/api/v1/dataset/42");
        assertThat(request.getFirstHeader("X-CSRFToken").getValue()).isEqualTo(CSRF_TOKEN);
        assertThat(request.getFirstHeader("Cookie").getValue()).isEqualTo(SESSION_COOKIE);
    }

    @Test
    void chartUuidRequestUsesListEndpointWithUuidColumnAndIdFilter() throws URISyntaxException {
        HttpUriRequest request = SupersetApiRequestBuilder.getChartUuidByIdRequest(HOST, PORT, AUTH_TOKEN, 153);

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getURI().getPath()).isEqualTo("/api/v1/chart/");
        String query = decodedQuery(request);
        assertThat(query).contains("\"columns\":[\"id\",\"uuid\"]");
        assertThat(query).contains("\"col\":\"id\"");
        assertThat(query).contains("\"opr\":\"eq\"");
        assertThat(query).contains("\"value\":153");
    }

    @Test
    void datasetUuidRequestUsesListEndpointWithUuidColumnAndIdFilter() throws URISyntaxException {
        HttpUriRequest request = SupersetApiRequestBuilder.getDatasetUuidByIdRequest(HOST, PORT, AUTH_TOKEN, 70);

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getURI().getPath()).isEqualTo("/api/v1/dataset/");
        String query = decodedQuery(request);
        assertThat(query).contains("\"columns\":[\"id\",\"uuid\"]");
        assertThat(query).contains("\"value\":70");
    }

    private static String decodedQuery(HttpUriRequest request) {
        return URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8);
    }
}
