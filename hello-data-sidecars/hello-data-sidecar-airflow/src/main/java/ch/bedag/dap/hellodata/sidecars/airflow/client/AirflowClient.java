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
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowPermissionsResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRolesResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUser;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserRolesUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUsersResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

@Log4j2
public class AirflowClient {

    private final String host;
    private final int port;

    private final String username;

    private final String password;

    private final CloseableHttpClient client;

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

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    /**
     * Get List of users available in Airflow
     */
    public AirflowUsersResponse users() throws URISyntaxException, IOException {
        int offset = 0;
        int limit = 100;
        AirflowUsersResponse airflowUsersResponse = getAirflowUsersResponse(offset, limit);
        int totalEntries = airflowUsersResponse.getTotalEntries();
        int fetchedEntries = airflowUsersResponse.getUsers().size();
        log.info("Total Airflow-Users: {}", totalEntries);
        while (fetchedEntries < totalEntries) {
            offset += limit;
            AirflowUsersResponse currentRequest = getAirflowUsersResponse(offset, limit);
            airflowUsersResponse.getUsers().addAll(currentRequest.getUsers());
            fetchedEntries += currentRequest.getUsers().size();
            log.info("Fetched users: {}", fetchedEntries);
        }
        log.info("Finished loading users. {} out of {}", fetchedEntries, totalEntries);
        return airflowUsersResponse;
    }

    private AirflowUsersResponse getAirflowUsersResponse(int offset, int limit) throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getListUsersRequest(host, port, username, password, offset, limit);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("users() response json \n{}", new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowUsersResponse.class);
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
        HttpUriRequest request = AirflowApiRequestBuilder.getListRolesRequest(host, port, username, password);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("roles({}) response json \n{}", username, new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowRolesResponse.class);
    }

    /**
     * Create a new user with unique username and email.
     */
    public void createRole(AirflowRole airflowRole) throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getCreateRoleRequest(host, port, username, password, airflowRole);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("createRole({}) response json \n{}", airflowRole, new String(bytes));
        //return getObjectMapper().readValue(bytes, AirflowRole.class);
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
        HttpUriRequest request = AirflowApiRequestBuilder.getDagsRequest(host, port, username, password);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("dags({}) response json \n{}", username, new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowDagsResponse.class);
    }

    public AirflowDagRunsResponse dagRuns(String dagId) throws URISyntaxException, IOException {
        HttpUriRequest request = AirflowApiRequestBuilder.getDagRunsRequest(host, port, username, password, dagId, "-start_date", "1");
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("dagRuns({}) response json \n{}", username, new String(bytes));
        return getObjectMapper().readValue(bytes, AirflowDagRunsResponse.class);
    }

    private ApiResponse executeRequest(HttpUriRequest request) throws IOException {
        try (CloseableHttpResponse response = client.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            String bodyAsString = EntityUtils.toString(response.getEntity());
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
