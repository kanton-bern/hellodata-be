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
package ch.bedag.dap.hellodata.sidecars.airflow.client;

import ch.bedag.dap.hellodata.sidecars.airflow.client.dag.AirflowDagRunsResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.dag.AirflowDagsResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.exception.UnexpectedResponseException;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

@Log4j2
public class AirflowClient implements Closeable {

    private final String host;
    private final int port;

    private final String username;

    private final String password;

    private final CloseableHttpClient client;

    private static final int PAGE_LIMIT = 100;

    public AirflowClient(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        //Basic-Auth
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        this.client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
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
        return objectMapper;
    }

    /**
     * Get List of users available in Airflow
     */
    public AirflowUsersResponse users() throws URISyntaxException, IOException {
        AirflowUsersResponse airflowUsersResponse = new AirflowUsersResponse();
        airflowUsersResponse.setUsers(fetchAllPages(
                (offset, limit) -> fetchPage(AirflowApiRequestBuilder.getListUsersRequest(host, port, username, password, offset, limit), AirflowUsersResponse.class),
                AirflowUsersResponse::getUsers, AirflowUsersResponse::getTotalEntries));
        airflowUsersResponse.setTotalEntries(airflowUsersResponse.getUsers().size());
        log.info("Finished loading users. {} fetched", airflowUsersResponse.getTotalEntries());
        return airflowUsersResponse;
    }

    /**
     * Gets a user.
     */
    public AirflowUserResponse getUser(String airflowUsername) throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getUserRequest(host, port, username, password, airflowUsername);
        try {
            ApiResponse resp = executeRequest(request);
            byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
            log.debug("getUser({}) response json \n{}", airflowUsername, new String(bytes));
            return getObjectMapper().readValue(bytes, AirflowUserResponse.class);
        } catch (UnexpectedResponseException e) {
            if (e.getCode() == 404) {
                log.warn("User not found by username {}", airflowUsername);
                return null;
            }
            throw e;
        }
    }

    /**
     * Create a new user with unique username and email.
     */
    public AirflowUserResponse createUser(AirflowUser airflowUser) throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getCreateUserRequest(host, port, username, password, airflowUser);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("createUser({}) response json \n{}", airflowUser.getEmail(), new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowUserResponse.class);
    }

    /**
     * Delete a user with the following username.
     */
    public void deleteUser(String airflowUsername) throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getDeleteUserRequest(host, port, username, password, airflowUsername);
        ApiResponse resp = executeRequest(request);
        if (resp.getBody() != null) {
            byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
            log.debug("deleteUser({}) response json \n{}", airflowUsername, new String(bytes));
        }
    }

    /**
     * Update (or rather set) the roles of a user
     **/
    public AirflowUserResponse updateUser(AirflowUserRolesUpdate userRolesUpdate, String usernameToUpdate) throws IOException, URISyntaxException {
        HttpUriRequest request = AirflowApiRequestBuilder.getUpdateUserRequest(host, port, username, password, userRolesUpdate, usernameToUpdate);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("updateUser({}) response json \n{}", username, new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowUserResponse.class);
    }

    /**
     * Get List of roles available in Airflow
     */
    public AirflowRolesResponse roles() throws URISyntaxException, IOException {
        AirflowRolesResponse airflowRolesResponse = new AirflowRolesResponse();
        airflowRolesResponse.setRoles(fetchAllPages(
                (offset, limit) -> fetchPage(AirflowApiRequestBuilder.getListRolesRequest(host, port, username, password, offset, limit), AirflowRolesResponse.class),
                AirflowRolesResponse::getRoles, AirflowRolesResponse::getTotalEntries));
        airflowRolesResponse.setTotalEntries(airflowRolesResponse.getRoles().size());
        return airflowRolesResponse;
    }

    /**
     * Create a new user with unique username and email.
     */
    public void createRole(AirflowRole airflowRole) throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getCreateRoleRequest(host, port, username, password, airflowRole);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("createRole({}) response json \n{}", airflowRole, new String(bytes));
    }

    /**
     * Get List of permissions available in Airflow
     */
    public AirflowPermissionsResponse permissions() throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getListPermissionsRequest(host, port, username, password);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("permissions({}) response json \n{}", username, new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowPermissionsResponse.class);
    }

    public AirflowDagsResponse dags() throws URISyntaxException, IOException {
        AirflowDagsResponse airflowDagsResponse = new AirflowDagsResponse();
        airflowDagsResponse.setDags(fetchAllPages(
                (offset, limit) -> fetchPage(AirflowApiRequestBuilder.getDagsRequest(host, port, username, password, offset, limit), AirflowDagsResponse.class),
                AirflowDagsResponse::getDags, AirflowDagsResponse::getTotalEntries));
        airflowDagsResponse.setTotalEntries(airflowDagsResponse.getDags().size());
        return airflowDagsResponse;
    }

    public AirflowDagRunsResponse dagRuns(String dagId) throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getDagRunsRequest(host, port, username, password, dagId, "-start_date", "1");
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("dagRuns({}) response json \n{}", username, new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowDagRunsResponse.class);
    }

    /**
     * Fetches all pages of an Airflow list endpoint. Airflow caps a page at maximum_page_limit (default 100),
     * so a single request silently returns only the first page. Stops on an empty page or when total_entries is
     * reached, so entries removed during paging cannot cause an endless loop.
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
