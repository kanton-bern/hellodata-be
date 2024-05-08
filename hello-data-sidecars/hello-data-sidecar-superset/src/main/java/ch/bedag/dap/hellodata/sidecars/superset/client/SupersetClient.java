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

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboardByIdResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboardResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.permission.response.superset.SupersetPermissionResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolePermissionsResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolesResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.IdResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUserByIdResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUserUpdateResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUsersResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.exception.UnexpectedResponseException;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetDashboardPublishedFlagUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserActiveUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserRolesUpdate;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import static ch.bedag.dap.hellodata.sidecars.superset.client.SupersetApiRequestBuilder.getObjectMapper;

/**
 * A client for interacting with the Superset SupersetApiRequestBuilderUtil. This client provides methods
 * for exporting and importing dashboards, as well as retrieving a list of
 * available dashboards and others.
 */
@Log4j2
public class SupersetClient {

    private final String host;
    private final int port;
    private final String authToken;
    private final CloseableHttpClient client;
    private String csrfToken;
    private String sessionCookie;

    /**
     * Creates a new Superset API client with the given credentials.
     *
     * @param host     The hostname of the Superset server.
     * @param port     The port number of the Superset server.
     * @param username The username to use when authenticating.
     * @param password The password to use when authenticating.
     * @throws Exception If there was an error creating the client.
     */
    public SupersetClient(String host, int port, String username, String password) throws Exception {
        this(host, port, username, password, null);
    }

    /**
     * Creates a new Superset API client with the given credentials.
     *
     * @param host     The hostname of the Superset server.
     * @param port     The port number of the Superset server.
     * @param username The username to use when authenticating.
     * @param password The password to use when authenticating.
     * @param client   The underlying HttpClient to use.
     * @throws Exception If there was an error creating the client.
     */
    public SupersetClient(String host, int port, String username, String password, CloseableHttpClient client) throws Exception {
        if (client == null) {
            this.client = HttpClientBuilder.create().build();
        } else {
            this.client = client;
        }
        HttpUriRequest request = SupersetApiRequestBuilder.getAuthTokenRequest(host, port, username, password);
        ApiResponse resp = executeRequest(request);
        log.info("Auth token request executed {}", resp);
        JsonElement respBody = JsonParser.parseString(resp.getBody());
        this.authToken = respBody.getAsJsonObject().get("access_token").getAsString();
        this.host = host;
        this.port = port;
    }

    /**
     * Creates a new Superset API client with the given credentials.
     *
     * @param host The hostname of the Superset server.
     * @param port The port number of the Superset server.
     * @throws Exception If there was an error creating the client.
     */
    public SupersetClient(String host, int port, String authToken) throws Exception {
        this.client = HttpClientBuilder.create().build();
        this.authToken = authToken;
        this.host = host;
        this.port = port;
    }

    /**
     * Updates user roles.
     *
     * @return an updated user.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetUserUpdateResponse updateUserRoles(SupersetUserRolesUpdate supersetUserRoleUpdate, int userId) throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getUpdateUserRolesRequest(host, port, authToken, supersetUserRoleUpdate, userId);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("updateUser({}) response json \n{}", userId, new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetUserUpdateResponse.class);
    }

    /**
     * Updates user active flag (enable/disable user).
     *
     * @return an updated user.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetUserUpdateResponse updateUsersActiveFlag(SupersetUserActiveUpdate supersetUserActiveUpdate, int userId) throws URISyntaxException, ClientProtocolException,
            IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getUpdateUserActiveRequest(host, port, authToken, supersetUserActiveUpdate, userId);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("updateUsersActiveFlag({}) response json \n{}", userId, new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetUserUpdateResponse.class);
    }

    /**
     * Creates a user.
     *
     * @return a created user.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public IdResponse createUser(SubsystemUserUpdate userUpdate) throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getCreateUserRequest(host, port, authToken, userUpdate);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("createUser({}) response json \n{}", userUpdate.getEmail(), new String(bytes));
        return getObjectMapper().readValue(bytes, IdResponse.class);
    }

    /**
     * Deletes a user.
     *
     * @return 200 if successfully deleted the user.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public void deleteUser(int userId) throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getDeleteUserRequest(host, port, authToken, userId);
        ApiResponse resp = executeRequest(request);
        if (resp.getBody() != null) {
            byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
            log.debug("deleteUser({}) response json \n{}", userId, new String(bytes));
        }
    }

    /**
     * Returns a list of available permissions for a given role.
     *
     * @return A JSON array containing a list of permissions.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetRolePermissionsResponse rolePermissions(int roleId) throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListRolePermissionsRequest(roleId, host, port, authToken);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("rolePermissions({}) response json \n{}", roleId, new String(bytes));
        return getObjectMapper().readValue(resp.getBody().getBytes(StandardCharsets.UTF_8), SupersetRolePermissionsResponse.class);
    }

    /**
     * Returns a list of available users.
     *
     * @return A JSON array containing a list of users.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetUserByIdResponse user(int userId) throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getUserByIdRequest(userId, host, port, authToken);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("user({}) response json \n{}", userId, new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetUserByIdResponse.class);
    }

    /**
     * Returns a list of available users.
     *
     * @return A JSON array containing a list of users.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetUsersResponse users() throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListUsersRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("users() response json \n{}", new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetUsersResponse.class);
    }

    /**
     * Returns a list of available roles.
     *
     * @return A JSON array containing a list of roles.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetRolesResponse roles() throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListRolesRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("roles() response json \n{}", new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetRolesResponse.class);
    }

    /**
     * Returns a list of available permissions.
     *
     * @return A JSON array containing a list of permissions.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetPermissionResponse permissions() throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListPermissionsRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("permissions() response json \n{}", new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetPermissionResponse.class);
    }

    /**
     * Returns a list of available dashboards.
     *
     * @return A JSON array containing a list of dashboards.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetDashboardByIdResponse dashboard(int dashboardId) throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getDashboardRequest(dashboardId, host, port, authToken);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("dashboard({}) response json \n{}", dashboardId, new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetDashboardByIdResponse.class);
    }

    /**
     * Returns a list of available dashboards.
     *
     * @return A JSON array containing a list of dashboards.
     *
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetDashboardResponse dashboards() throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListDashboardsRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("dashboards() response json \n{}", new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetDashboardResponse.class);
    }

    /**
     * Exports a dashboard with the specified ID and saves it to the given file.
     *
     * @param dashboardId the ID of the dashboard to export
     * @param destination the file to save the exported dashboard to
     * @return the downloaded file
     *
     * @throws URISyntaxException      if there is an error in the URI syntax
     * @throws ClientProtocolException if there is an error in the HTTP protocol
     * @throws IOException             if there is an I/O error while sending or
     *                                 receiving the HTTP request/response
     */
    public File exportDashboard(int dashboardId, File destination) throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getExportDashboardRequest(host, port, authToken, dashboardId);

        return client.execute(request, new FileDownloadResponseHandler(destination));
    }

    /**
     * Imports a dashboard from a file.
     *
     * @param dashboardFile the file containing the dashboard definition to import
     * @param password      A JSON format database password, e.g:
     *                      {\"databases/database.yaml\":\"password\"}
     * @param override      override exist dashboard
     * @throws ClientProtocolException if there was a problem with the HTTP protocol
     * @throws URISyntaxException      if there was a problem with the URI syntax
     * @throws IOException             if there was a problem with the I/O
     */
    public void importDashboard(File dashboardFile, JsonElement password, boolean override) throws ClientProtocolException, URISyntaxException, IOException {
        csrf();
        HttpUriRequest request = SupersetApiRequestBuilder.getImportDashboardRequest(host, port, authToken, csrfToken, dashboardFile, override, password, sessionCookie);
        executeRequest(request);
    }

    /**
     * Updates dashboards published flag (publish/un-publish dashboard).
     *
     * @return an updated dashboard.
     *
     * @throws URISyntaxException If the Superset URL is invalid.
     * @throws IOException        If there was an error communicating with the
     *                            Superset server.
     */
    public SupersetDashboard updateDashboardsPublishedFlag(SupersetDashboardPublishedFlagUpdate supersetUserActiveUpdate, int dashboardId) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getUpdateDashboardPublishedFlagRequest(host, port, authToken, supersetUserActiveUpdate, dashboardId);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("updateUsersActiveFlag({}) response json \n{}", dashboardId, new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetDashboard.class);
    }

    private void csrf() throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getCsrfTokenRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        Optional<Header> setCookieHeader = Arrays.stream(resp.getHeaders()).filter(header -> header.getName().equalsIgnoreCase("set-cookie")).findFirst();
        setCookieHeader.ifPresent(header -> this.sessionCookie = header.getValue());
        log.debug("csrf response ==> {}", resp);
        JsonElement respBody = JsonParser.parseString(resp.getBody());
        this.csrfToken = respBody.getAsJsonObject().get("result").getAsString();
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
            return new ApiResponse(code, bodyAsString, response.getAllHeaders());
        }
    }

    @AllArgsConstructor
    private static class FileDownloadResponseHandler implements ResponseHandler<File> {
        private File target;

        @Override
        public File handleResponse(HttpResponse response) throws IOException {
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                throw new UnexpectedResponseException("", code, EntityUtils.toString(response.getEntity()));
            }
            try (InputStream source = response.getEntity().getContent()) {
                FileUtils.copyInputStreamToFile(source, this.target);
            }
            return this.target;
        }
    }

    @AllArgsConstructor
    @Data
    public static class ApiResponse {
        private int code;
        private String body;
        private Header[] headers;
    }
}
