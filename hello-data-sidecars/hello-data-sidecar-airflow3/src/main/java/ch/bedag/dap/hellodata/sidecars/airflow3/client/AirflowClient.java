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
import ch.bedag.dap.hellodata.sidecars.airflow3.client.user.response.AirflowPermissionsResponse;
import ch.bedag.dap.hellodata.sidecars.airflow3.client.user.response.AirflowRolesResponse;
import ch.bedag.dap.hellodata.sidecars.airflow3.client.user.response.AirflowUsersResponse;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;

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
    private static final int PAGE_LIMIT = 100;

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
        AirflowDagsResponse response = new AirflowDagsResponse();
        response.setDags(fetchAllPages(
                (offset, limit) -> fetchPage(AirflowApiRequestBuilder.getDagsRequest(host, port, mintToken(), offset, limit), AirflowDagsResponse.class),
                AirflowDagsResponse::getDags, AirflowDagsResponse::getTotalEntries));
        response.setTotalEntries(response.getDags().size());
        return response;
    }

    public AirflowDagRunsResponse dagRuns(String dagId) throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getDagRunsRequest(host, port, mintToken(), dagId, "-start_date", "1");
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("dagRuns({}) response json \n{}", dagId, new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowDagRunsResponse.class);
    }

    /**
     * List all FAB users (GET /auth/fab/v1/users), following pagination. Read-only; used to publish
     * the workspace's Users resource.
     */
    public AirflowUsersResponse users() throws URISyntaxException, IOException {
        AirflowUsersResponse response = new AirflowUsersResponse();
        response.setUsers(fetchAllPages(
                (offset, limit) -> fetchPage(AirflowApiRequestBuilder.getUsersRequest(host, port, mintToken(), offset, limit), AirflowUsersResponse.class),
                AirflowUsersResponse::getUsers, AirflowUsersResponse::getTotalEntries));
        response.setTotalEntries(response.getUsers().size());
        return response;
    }

    /** List all FAB roles with their (action, resource) permissions (GET /auth/fab/v1/roles), following pagination. */
    public AirflowRolesResponse roles() throws URISyntaxException, IOException {
        AirflowRolesResponse response = new AirflowRolesResponse();
        response.setRoles(fetchAllPages(
                (offset, limit) -> fetchPage(AirflowApiRequestBuilder.getRolesRequest(host, port, mintToken(), offset, limit), AirflowRolesResponse.class),
                AirflowRolesResponse::getRoles, AirflowRolesResponse::getTotalEntries));
        response.setTotalEntries(response.getRoles().size());
        return response;
    }

    /** List all FAB permissions as (action, resource) pairs (GET /auth/fab/v1/permissions), following pagination. */
    public AirflowPermissionsResponse permissions() throws URISyntaxException, IOException {
        AirflowPermissionsResponse response = new AirflowPermissionsResponse();
        response.setActions(fetchAllPages(
                (offset, limit) -> fetchPage(AirflowApiRequestBuilder.getPermissionsRequest(host, port, mintToken(), offset, limit), AirflowPermissionsResponse.class),
                AirflowPermissionsResponse::getActions, AirflowPermissionsResponse::getTotalEntries));
        response.setTotalEntries(response.getActions().size());
        return response;
    }

    /**
     * Fetches all pages of an Airflow list endpoint. Airflow caps a page at fallback_page_limit / maximum_page_limit
     * (100 in our image), so a single request silently returns only the first page. Stops on an empty page or when
     * total_entries is reached, and moves on by the number of entries returned, so a lower server cap skips nothing.
     */
    static <R, T> List<T> fetchAllPages(PageFetcher<R> pageFetcher, Function<R, List<T>> itemsExtractor, ToIntFunction<R> totalExtractor)
            throws URISyntaxException, IOException {
        List<T> allItems = new ArrayList<>();
        int offset = 0;
        while (true) {
            R page = pageFetcher.fetch(offset, PAGE_LIMIT);
            List<T> items = itemsExtractor.apply(page);
            if (items == null || items.isEmpty()) {
                break;
            }
            allItems.addAll(items);
            offset += items.size();
            if (offset >= totalExtractor.applyAsInt(page)) {
                break;
            }
        }
        return allItems;
    }

    private <R> R fetchPage(HttpUriRequest request, Class<R> responseType) throws IOException {
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("{} response json \n{}", request.getURI(), new String(bytes));
        return getObjectMapper().readValue(bytes, responseType);
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
                .audience().single(AUDIENCE)
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

    @FunctionalInterface
    interface PageFetcher<R> {
        R fetch(int offset, int limit) throws URISyntaxException, IOException;
    }
}
