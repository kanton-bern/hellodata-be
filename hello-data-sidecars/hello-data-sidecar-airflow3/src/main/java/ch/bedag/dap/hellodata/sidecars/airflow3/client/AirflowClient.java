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
package ch.bedag.dap.hellodata.sidecars.airflow3.client;

import ch.bedag.dap.hellodata.sidecars.airflow3.client.dag.AirflowDagRunsResponse;
import ch.bedag.dap.hellodata.sidecars.airflow3.client.dag.AirflowDagsResponse;
import ch.bedag.dap.hellodata.sidecars.airflow3.client.exception.UnexpectedResponseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.crypto.SecretKey;
import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Read-only client for the Airflow 3 stable REST API (v2), used by the monitoring sidecar to
 * publish DAG / DAG-run status. Airflow 3 dropped Basic auth and the FAB user/role/permission
 * endpoints, so this client only lists DAGs and their latest run, and authenticates by minting
 * a short-lived HS512 JWT (aud=apache-airflow, sub=<service user id>) signed with the shared
 * api-server secret (AIRFLOW__API_AUTH__JWT_SECRET) — no login round-trip.
 */
@Log4j2
public class AirflowClient implements Closeable {

    private static final String AUDIENCE = "apache-airflow";

    private final String host;
    private final int port;
    private final SecretKey signingKey;
    private final String apiUserId;
    private final long jwtTtlSeconds;
    private final CloseableHttpClient client;

    public AirflowClient(String host, int port, String jwtSecret, String apiUserId, long jwtTtlSeconds) {
        this.host = host;
        this.port = port;
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.apiUserId = apiUserId;
        this.jwtTtlSeconds = jwtTtlSeconds;
        this.client = HttpClientBuilder.create().build();
    }

    @Override
    public void close() throws IOException {
        if (this.client != null) {
            this.client.close();
        }
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Airflow 3 v2 responses add fields (e.g. dag_display_name, is_stale, last_parsed_time).
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    public AirflowDagsResponse dags() throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getDagsRequest(host, port, mintToken());
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("dags() response json \n{}", new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowDagsResponse.class);
    }

    public AirflowDagRunsResponse dagRuns(String dagId) throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getDagRunsRequest(host, port, mintToken(), dagId, "-start_date", "1");
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("dagRuns({}) response json \n{}", dagId, new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowDagRunsResponse.class);
    }

    /**
     * Mint a short-lived HS512 bearer token accepted by the Airflow 3 api-server. The header
     * `kid` is the fixed literal "not-used" (what the api-server emits for the symmetric key),
     * and `sub` is the FAB user id of the configured service user.
     */
    String mintToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().add("kid", "not-used").and()
                .subject(apiUserId)
                .audience().single(AUDIENCE).and()
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtTtlSeconds)))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    private ApiResponse executeRequest(HttpUriRequest request) throws IOException {
        try (CloseableHttpResponse response = client.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String bodyAsString = null;
            if (entity != null) {
                bodyAsString = EntityUtils.toString(entity);
            }
            if (code >= 300 || code < 200) {
                throw new UnexpectedResponseException(request.getURI().toString(), code, bodyAsString);
            }
            return new ApiResponse(code, bodyAsString);
        }
    }

    @AllArgsConstructor
    @Data
    public static class ApiResponse {
        private int code;
        private String body;
    }
}
