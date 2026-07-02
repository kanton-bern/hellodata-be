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
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.logs.response.superset.SupersetLogResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.permission.response.superset.SupersetPermissionResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.query.response.superset.SupersetQueryResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolePermissionsResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolesResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.IdResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUserByIdResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUserUpdateResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUsersResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.exception.BadReturnedValueException;
import ch.bedag.dap.hellodata.sidecars.superset.client.exception.UnexpectedResponseException;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetDashboardPublishedFlagUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserActiveUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserRolesUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static ch.bedag.dap.hellodata.sidecars.superset.client.SupersetApiRequestBuilder.getObjectMapper;

/**
 * A client for interacting with the Superset SupersetApiRequestBuilderUtil. This client provides methods
 * for exporting and importing dashboards, as well as retrieving a list of
 * available dashboards and others.
 */
@Log4j2
public class SupersetClient implements Closeable {

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
    public SupersetClient(String host, int port, String username, String password) throws IOException, URISyntaxException {
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
    public SupersetClient(String host, int port, String username, String password, CloseableHttpClient client) throws IOException, URISyntaxException {
        if (client == null) {
            this.client = HttpClientBuilder.create().build();
        } else {
            this.client = client;
        }
        HttpUriRequest request = SupersetApiRequestBuilder.getAuthTokenRequest(host, port, username, password);
        ApiResponse resp = executeRequest(request);
        log.debug("Auth token request executed {}", resp);
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
     */
    public SupersetClient(String host, int port, String authToken) {
        this.client = HttpClientBuilder.create().build();
        this.authToken = authToken;
        this.host = host;
        this.port = port;
    }

    @Override
    public void close() throws IOException {
        if (this.client != null) {
            this.client.close();
        }
    }

    /**
     * Updates user roles.
     *
     * @return an updated user.
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetUserUpdateResponse updateUserRoles(SupersetUserRolesUpdate supersetUserRoleUpdate, int userId) throws URISyntaxException, IOException {
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
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetUserUpdateResponse updateUsersActiveFlag(SupersetUserActiveUpdate supersetUserActiveUpdate, int userId) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getUpdateUserActiveRequest(host, port, authToken, supersetUserActiveUpdate, userId);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("updateUsersActiveFlag({}) response json \n{}", userId, new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetUserUpdateResponse.class);
    }

    /**
     * Get a user.
     *
     * @return a user.
     * @throws URISyntaxException        If the Superset URL is invalid.
     * @throws ClientProtocolException   If there was an error communicating with the
     *                                   Superset server.
     * @throws IOException               If there was an error communicating with the
     *                                   Superset server.
     * @throws BadReturnedValueException If query will return more than one user
     */
    public SupersetUsersResponse getUser(String username, String email) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getUserRequest(host, port, authToken, username, email);
        try {
            ApiResponse resp = executeRequest(request);
            byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
            log.debug("getUser({}, {}) response json \n{}", username, email, new String(bytes));
            SupersetUsersResponse supersetUsersResponse = getObjectMapper().readValue(bytes, SupersetUsersResponse.class);
            if (supersetUsersResponse.getResult() != null && supersetUsersResponse.getResult().size() > 1) {
                throw new BadReturnedValueException(String.format("Too many users returned for username %s and email %s. Should get list of 1 got %s",
                        username, email, supersetUsersResponse.getResult()));
            }
            return supersetUsersResponse;
        } catch (UnexpectedResponseException e) {
            if (e.getCode() == 404) {
                log.warn("User not found by email {} and username {}", email, username);
                return null;
            }
            throw e;
        }
    }

    /**
     * Creates a user.
     *
     * @return a created user.
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public IdResponse createUser(SubsystemUserUpdate userUpdate) throws URISyntaxException, IOException {
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
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public void deleteUser(int userId) throws URISyntaxException, IOException {
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
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetRolePermissionsResponse rolePermissions(int roleId) throws URISyntaxException, IOException {
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
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetUserByIdResponse user(int userId) throws URISyntaxException, IOException {
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
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetUsersResponse users() throws URISyntaxException, IOException {
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
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetRolesResponse roles() throws URISyntaxException, IOException {
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
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetPermissionResponse permissions() throws URISyntaxException, IOException {
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
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetDashboardByIdResponse dashboard(int dashboardId) throws URISyntaxException, IOException {
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
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetDashboardResponse dashboards() throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListDashboardsRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("dashboards() response json \n{}", new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetDashboardResponse.class);
    }

    /**
     * Returns a list of available queries.
     *
     * @return A JSON array containing a list of queries.
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetQueryResponse queries() throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListQueriesRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("queries() response json \n{}", new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetQueryResponse.class);
    }

    /**
     * Returns a list of available queries.
     *
     * @return A JSON array containing a list of queries.
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetQueryResponse queriesFiltered(JsonArray filters) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListQueriesRequestFiltered(host, port, authToken, filters);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("queriesFiltered() response json \n{}", new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetQueryResponse.class);
    }

    public SupersetQueryResponse queriesFiltered(JsonArray filters, int page, int pageSize) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListQueriesRequestFiltered(host, port, authToken, filters, page, pageSize);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("queriesFiltered() page {} response json \n{}", page, new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetQueryResponse.class);
    }

    public SupersetQueryResponse queriesFiltered(JsonArray columns, JsonArray filters, int page, int pageSize) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListQueriesRequestFiltered(host, port, authToken, columns, filters, page, pageSize);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("queriesFiltered() page {} response json \n{}", page, new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetQueryResponse.class);
    }

    /**
     * Returns a list of available logs.
     *
     * @return A JSON array containing a list of logs.
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public SupersetLogResponse logsFiltered(JsonArray filters) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getLisLogsRequestFiltered(host, port, authToken, filters);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("logsFiltered() response json \n{}", new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetLogResponse.class);
    }

    public SupersetLogResponse logsFiltered(JsonArray filters, int page, int pageSize) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getLisLogsRequestFiltered(host, port, authToken, filters, page, pageSize);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        log.debug("logsFiltered() page {} response json \n{}", page, new String(bytes));
        return getObjectMapper().readValue(bytes, SupersetLogResponse.class);
    }

    /**
     * Lists all database connections registered in Superset.
     *
     * @return a JsonArray of database objects with id, database_name, uuid fields
     * @throws URISyntaxException if the Superset URL is invalid
     * @throws IOException        if there was an error communicating with the Superset server
     */
    public JsonArray listDatabases() throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListDatabasesRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        JsonElement respBody = JsonParser.parseString(resp.getBody());
        return respBody.getAsJsonObject().getAsJsonArray("result");
    }

    /**
     * Gets a single database connection by ID including its uuid.
     *
     * @param databaseId the ID of the database connection
     * @return a JsonObject with database details
     * @throws URISyntaxException if the Superset URL is invalid
     * @throws IOException        if there was an error communicating with the Superset server
     */
    public JsonElement getDatabaseById(int databaseId) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getDatabaseByIdRequest(databaseId, host, port, authToken);
        ApiResponse resp = executeRequest(request);
        JsonElement respBody = JsonParser.parseString(resp.getBody());
        return respBody.getAsJsonObject().get("result");
    }

    /**
     * Exports a dashboard with the specified ID and saves it to the given file.
     *
     * @param dashboardId the ID of the dashboard to export
     * @param destination the file to save the exported dashboard to
     * @return the downloaded file
     * @throws URISyntaxException      if there is an error in the URI syntax
     * @throws ClientProtocolException if there is an error in the HTTP protocol
     * @throws IOException             if there is an I/O error while sending or
     *                                 receiving the HTTP request/response
     */
    public File exportDashboard(int dashboardId, File destination) throws URISyntaxException, IOException {
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
    public void importDashboard(File dashboardFile, JsonElement password, boolean override) throws URISyntaxException, IOException {
        csrf();
        HttpUriRequest request = SupersetApiRequestBuilder.getImportDashboardRequest(host, port, authToken, csrfToken, dashboardFile, override, password, sessionCookie);
        executeRequest(request);
    }

    /**
     * Updates dashboards published flag (publish/un-publish dashboard).
     *
     * @return an updated dashboard.
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

    public SupersetDashboardResponse dashboards(JsonArray filters) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListDashboardsRequestFiltered(host, port, authToken, filters);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        return getObjectMapper().readValue(bytes, SupersetDashboardResponse.class);
    }

    public SupersetDashboardResponse dashboards(JsonArray columns, JsonArray filters) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getListDashboardsRequestFiltered(host, port, authToken, columns, filters);
        ApiResponse resp = executeRequest(request);
        byte[] bytes = resp.getBody().getBytes(StandardCharsets.UTF_8);
        return getObjectMapper().readValue(bytes, SupersetDashboardResponse.class);
    }

    public JsonArray getDashboardCharts(int dashboardId) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getDashboardChartsRequest(host, port, authToken, dashboardId);
        ApiResponse resp = executeRequest(request);
        JsonObject obj = new Gson().fromJson(resp.getBody(), JsonObject.class);
        return obj.has("result") && !obj.get("result").isJsonNull() ? obj.getAsJsonArray("result") : new JsonArray();
    }

    public List<Integer> getChartDashboardIds(int chartId) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getChartByIdRequest(host, port, authToken, chartId);
        ApiResponse resp = executeRequest(request);
        JsonObject obj = new Gson().fromJson(resp.getBody(), JsonObject.class);
        List<Integer> dashboardIds = new ArrayList<>();
        if (obj.has("result") && !obj.get("result").isJsonNull()) {
            JsonObject result = obj.getAsJsonObject("result");
            if (result.has("dashboards") && !result.get("dashboards").isJsonNull()) {
                for (JsonElement dashboardEl : result.getAsJsonArray("dashboards")) {
                    JsonObject dashboardObj = dashboardEl.getAsJsonObject();
                    if (dashboardObj.has("id") && !dashboardObj.get("id").isJsonNull()) {
                        dashboardIds.add(dashboardObj.get("id").getAsInt());
                    }
                }
            }
        }
        return dashboardIds;
    }

    public JsonArray getChartsForDatasource(int datasourceId) throws URISyntaxException, IOException {        JsonArray filters = new JsonArray();

        JsonObject f1 = new JsonObject();
        f1.addProperty("col", "datasource_id");
        f1.addProperty("opr", "eq");
        f1.addProperty("value", datasourceId);
        filters.add(f1);

        JsonObject f2 = new JsonObject();
        f2.addProperty("col", "datasource_type");
        f2.addProperty("opr", "eq");
        f2.addProperty("value", "table");
        filters.add(f2);

        HttpUriRequest request = SupersetApiRequestBuilder.getListChartsRequestFiltered(host, port, authToken, filters);
        ApiResponse resp = executeRequest(request);
        JsonObject obj = new Gson().fromJson(resp.getBody(), JsonObject.class);
        return obj.has("result") && !obj.get("result").isJsonNull() ? obj.getAsJsonArray("result") : new JsonArray();
    }

    public void deleteCharts(List<Integer> chartIds) throws URISyntaxException, IOException {
        csrf();
        HttpUriRequest request = SupersetApiRequestBuilder.getDeleteChartsRequest(host, port, authToken, chartIds, csrfToken, sessionCookie);
        ApiResponse resp = executeRequest(request);
        log.debug("deleteCharts response: {}", resp.getBody());
    }

    /**
     * Finds datasets that share the given physical identity (schema + table name). Superset enforces a
     * unique constraint on {@code (database_id, schema, table_name)}, so callers can use the returned rows
     * to detect an incoming dataset that would collide with an existing one carrying a different uuid.
     * <p>
     * The {@code uuid} and {@code database.id} are not exposed by the dataset "show" endpoint in this
     * Superset distribution, but the list endpoint returns them when requested explicitly via
     * {@code columns}.
     *
     * @return a JsonArray of dataset objects with {@code id}, {@code uuid}, {@code schema},
     * {@code table_name} and nested {@code database.id}
     */
    public JsonArray getDatasetsBySchemaAndTable(String schema, String tableName) throws URISyntaxException, IOException {
        JsonArray columns = new JsonArray();
        columns.add("id");
        columns.add("uuid");
        columns.add("schema");
        columns.add("table_name");
        columns.add("database.id");

        JsonArray filters = new JsonArray();
        JsonObject tableFilter = new JsonObject();
        tableFilter.addProperty("col", "table_name");
        tableFilter.addProperty("opr", "eq");
        tableFilter.addProperty("value", tableName);
        filters.add(tableFilter);
        if (schema != null) {
            JsonObject schemaFilter = new JsonObject();
            schemaFilter.addProperty("col", "schema");
            schemaFilter.addProperty("opr", "eq");
            schemaFilter.addProperty("value", schema);
            filters.add(schemaFilter);
        }

        HttpUriRequest request = SupersetApiRequestBuilder.getListDatasetsRequestFiltered(host, port, authToken, columns, filters);
        ApiResponse resp = executeRequest(request);
        JsonObject obj = new Gson().fromJson(resp.getBody(), JsonObject.class);
        return obj.has("result") && !obj.get("result").isJsonNull() ? obj.getAsJsonArray("result") : new JsonArray();
    }

    public void deleteDataset(int datasetId) throws URISyntaxException, IOException {
        csrf();
        HttpUriRequest request = SupersetApiRequestBuilder.getDeleteDatasetRequest(host, port, authToken, datasetId, csrfToken, sessionCookie);
        ApiResponse resp = executeRequest(request);
        log.debug("deleteDataset response: {}", resp.getBody());
    }

    /**
     * Reads the UUID of a chart. Neither the dashboard charts endpoint nor the chart "show"
     * endpoint expose the UUID in this Superset distribution, so it is resolved through the
     * list endpoint which returns it when requested explicitly via {@code columns}.
     *
     * @return the chart UUID, or {@code null} if it could not be determined
     */
    public String getChartUuid(int chartId) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getChartUuidByIdRequest(host, port, authToken, chartId);
        ApiResponse resp = executeRequest(request);
        return extractFirstResultUuid(resp.getBody());
    }

    /**
     * Reads the UUID of a dataset through the list endpoint (the "show" endpoint does not
     * expose the UUID in this Superset distribution).
     *
     * @return the dataset UUID, or {@code null} if it could not be determined
     */
    public String getDatasetUuid(int datasetId) throws URISyntaxException, IOException {
        HttpUriRequest request = SupersetApiRequestBuilder.getDatasetUuidByIdRequest(host, port, authToken, datasetId);
        ApiResponse resp = executeRequest(request);
        return extractFirstResultUuid(resp.getBody());
    }

    private String extractFirstResultUuid(String body) {
        JsonObject obj = new Gson().fromJson(body, JsonObject.class);
        if (obj != null && obj.has("result") && obj.get("result").isJsonArray()) {
            JsonArray result = obj.getAsJsonArray("result");
            if (!result.isEmpty() && result.get(0).isJsonObject()) {
                JsonObject first = result.get(0).getAsJsonObject();
                if (first.has("uuid") && !first.get("uuid").isJsonNull()) {
                    return first.get("uuid").getAsString();
                }
            }
        }
        return null;
    }

    /**
     * Fetches the stored state of a dashboard permalink.
     *
     * @return the permalink value ({@code dashboardId} + {@code state}), or {@code null} if the
     * permalink (or its referenced dashboard) no longer exists.
     */
    public JsonObject getDashboardPermalinkState(String key) throws URISyntaxException, IOException {
        return getJsonObjectOrNullOnNotFound(SupersetApiRequestBuilder.getDashboardPermalinkRequest(host, port, authToken, key));
    }

    /**
     * Fetches the stored state of an explore (chart) permalink.
     *
     * @return the permalink value, or {@code null} if the permalink no longer exists.
     */
    public JsonObject getExplorePermalinkState(String key) throws URISyntaxException, IOException {
        return getJsonObjectOrNullOnNotFound(SupersetApiRequestBuilder.getExplorePermalinkRequest(host, port, authToken, key));
    }

    /**
     * @return {@code true} if a chart with the given id still exists in Superset.
     */
    public boolean chartExists(int chartId) throws URISyntaxException, IOException {
        return existsOrNotFound(SupersetApiRequestBuilder.getChartByIdRequest(host, port, authToken, chartId));
    }

    /**
     * @return {@code true} if a dashboard with the given id or slug still exists in Superset.
     */
    public boolean dashboardExists(String idOrSlug) throws URISyntaxException, IOException {
        return existsOrNotFound(SupersetApiRequestBuilder.getDashboardByIdOrSlugRequest(host, port, authToken, idOrSlug));
    }

    /**
     * Reads the layout component ids (e.g. {@code CHART-xxxx}, {@code TAB-xxxx}) of a dashboard from
     * its {@code position_json}. Permalink anchors reference these component ids, so they can be used
     * to determine whether a permalink still points to a chart that is part of the dashboard.
     *
     * @return the set of component ids, or {@link Optional#empty()} if it could not be determined.
     */
    public Optional<Set<String>> getDashboardComponentIds(String idOrSlug) throws URISyntaxException, IOException {
        JsonObject body = getJsonObjectOrNullOnNotFound(SupersetApiRequestBuilder.getDashboardByIdOrSlugRequest(host, port, authToken, idOrSlug));
        if (body == null || !body.has("result") || !body.get("result").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject result = body.getAsJsonObject("result");
        if (!result.has("position_json") || result.get("position_json").isJsonNull()) {
            return Optional.empty();
        }
        try {
            JsonElement positionElement = JsonParser.parseString(result.get("position_json").getAsString());
            if (!positionElement.isJsonObject()) {
                return Optional.empty();
            }
            Set<String> componentIds = new HashSet<>(positionElement.getAsJsonObject().keySet());
            return Optional.of(componentIds);
        } catch (RuntimeException e) {
            log.warn("Could not parse position_json for dashboard {}", idOrSlug, e);
            return Optional.empty();
        }
    }

    /**
     * Reads the chart ids ({@code meta.chartId}) of all chart components in a dashboard's
     * {@code position_json}. Permalink chart anchors (e.g. {@code CHART-explore-156-1}) reference a
     * chart by its id, so this set can be used to determine whether a permalink still points to a
     * chart that is part of the dashboard.
     *
     * @return the set of chart ids, or {@link Optional#empty()} if it could not be determined.
     */
    public Optional<Set<Integer>> getDashboardChartIds(String idOrSlug) throws URISyntaxException, IOException {
        JsonObject body = getJsonObjectOrNullOnNotFound(SupersetApiRequestBuilder.getDashboardByIdOrSlugRequest(host, port, authToken, idOrSlug));
        if (body == null || !body.has("result") || !body.get("result").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject result = body.getAsJsonObject("result");
        if (!result.has("position_json") || result.get("position_json").isJsonNull()) {
            return Optional.empty();
        }
        try {
            JsonElement positionElement = JsonParser.parseString(result.get("position_json").getAsString());
            if (!positionElement.isJsonObject()) {
                return Optional.empty();
            }
            Set<Integer> chartIds = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : positionElement.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject component = entry.getValue().getAsJsonObject();
                if (component.has("meta") && component.get("meta").isJsonObject()) {
                    JsonObject meta = component.getAsJsonObject("meta");
                    if (meta.has("chartId") && !meta.get("chartId").isJsonNull()) {
                        chartIds.add(meta.get("chartId").getAsInt());
                    }
                }
            }
            return Optional.of(chartIds);
        } catch (RuntimeException e) {
            log.warn("Could not parse position_json chart ids for dashboard {}", idOrSlug, e);
            return Optional.empty();
        }
    }

    private JsonObject getJsonObjectOrNullOnNotFound(HttpUriRequest request) throws URISyntaxException, IOException {
        try {
            ApiResponse resp = executeRequest(request);
            JsonElement parsed = JsonParser.parseString(resp.getBody());
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (UnexpectedResponseException e) {
            if (e.getCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    private boolean existsOrNotFound(HttpUriRequest request) throws URISyntaxException, IOException {
        try {
            executeRequest(request);
            return true;
        } catch (UnexpectedResponseException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw e;
        }
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

    private static final int MAX_RETRIES_ON_RATE_LIMIT = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 1100; // just over 1 second to let the rate-limit window reset

    private ApiResponse executeRequest(HttpUriRequest request) throws IOException {
        return executeRequestWithRetry(request, 0);
    }

    private ApiResponse executeRequestWithRetry(HttpUriRequest request, int attempt) throws IOException {
        try (CloseableHttpResponse response = client.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String bodyAsString = null;
            if (entity != null) {
                bodyAsString = EntityUtils.toString(entity);
            }
            if (code == 429) {
                if (attempt < MAX_RETRIES_ON_RATE_LIMIT) {
                    long delayMs = INITIAL_RETRY_DELAY_MS * (1L << attempt); // exponential backoff: 1.1s, 2.2s, 4.4s, ...
                    log.warn("Rate limited (429) on {}. Retrying in {} ms (attempt {}/{})", request.getURI(), delayMs, attempt + 1, MAX_RETRIES_ON_RATE_LIMIT);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while waiting for rate-limit retry", e);
                    }
                    return executeRequestWithRetry(request, attempt + 1);
                }
                throw new UnexpectedResponseException(request.getURI().toString(), code, bodyAsString);
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
