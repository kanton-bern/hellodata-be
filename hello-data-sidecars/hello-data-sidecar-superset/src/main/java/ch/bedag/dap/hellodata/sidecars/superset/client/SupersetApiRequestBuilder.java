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

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetDashboardPublishedFlagUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserActiveUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserRolesUpdate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.springframework.util.CollectionUtils;

@Log4j2
@UtilityClass
public class SupersetApiRequestBuilder {

    private static final String BEARER_TOKEN_VALUE_PREFIX = "Bearer ";
    private static final String LOGIN_API_ENDPOINT = "/api/v1/security/login";
    private static final String CSRF_TOKEN_API_ENDPOINT = "/api/v1/security/csrf_token/";
    private static final String LIST_ROLES_API_ENDPOINT = "/api/v1/security/roles/";
    private static final String LIST_PERMISSIONS_API_ENDPOINT = "/api/v1/security/permissions/";
    private static final String USERS_API_ENDPOINT = "/api/v1/security/users/";
    private static final String LIST_ROLE_PERMISSIONS_API_ENDPOINT = "/api/v1/security/roles/%d/permissions/";
    private static final String LIST_DASHBOARD_API_ENDPOINT = "/api/v1/dashboard/";
    private static final String DASHBOARD_API_ENDPOINT = "/api/v1/dashboard/%d";
    private static final String EXPORT_DASHBOARD_API_ENDPOINT = "/api/v1/dashboard/export/";
    private static final String IMPORT_DASHBOARD_API_ENDPOINT = "/api/v1/dashboard/import/";
    private static final String UPDATE_USER_API_ENDPOINT = USERS_API_ENDPOINT + "%d";
    private static final String DELETE_USER_API_ENDPOINT = USERS_API_ENDPOINT + "%d";

    public static HttpUriRequest getAuthTokenRequest(String host, int port, String username, String password) throws URISyntaxException, UnsupportedEncodingException {
        URI apiUri = buildUri(host, port, LOGIN_API_ENDPOINT, null);

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);
        body.addProperty("provider", "db");
        body.addProperty("refresh", true);
        StringEntity entity = new StringEntity(body.toString());

        return RequestBuilder.post() //
                             .setUri(apiUri) //
                             .setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()) //
                             .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                             .setEntity(entity) //
                             .build();
    }

    public static HttpUriRequest getCsrfTokenRequest(String host, int port, String authToken) throws URISyntaxException {
        URI apiUri = buildUri(host, port, CSRF_TOKEN_API_ENDPOINT, null);

        return RequestBuilder.get() //
                             .setUri(apiUri) //
                             .setHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_VALUE_PREFIX + authToken) //
                             .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                             .build();
    }

    public static HttpUriRequest getCreateUserRequest(String host, int port, String authToken, SubsystemUserUpdate supersetUser) throws URISyntaxException, IOException {
        URI apiUri = buildUri(host, port, USERS_API_ENDPOINT, null);
        String json = getObjectMapper().writeValueAsString(supersetUser);
        return RequestBuilder.post() //
                             .setUri(apiUri) //
                             .setHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_VALUE_PREFIX + authToken) //
                             .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                             .setEntity(new StringEntity(json, ContentType.APPLICATION_JSON)) //
                             .build();
    }

    public static HttpUriRequest getDeleteUserRequest(String host, int port, String authToken, int supersetUserId) throws URISyntaxException, IOException {
        URI apiUri = buildUri(host, port, String.format(DELETE_USER_API_ENDPOINT, supersetUserId), null);
        return RequestBuilder.delete() //
                             .setUri(apiUri) //
                             .setHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_VALUE_PREFIX + authToken) //
                             .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                             .build();
    }

    public static HttpUriRequest getUserByIdRequest(int userId, String host, int port, String authToken) throws URISyntaxException {
        return getHttpUriRequestWithBasicParams(host, port, authToken, null, String.format(UPDATE_USER_API_ENDPOINT, userId));
    }

    public static HttpUriRequest getListUsersRequest(String host, int port, String authToken) throws URISyntaxException {
        return getHttpUriRequestWithBasicParams(host, port, authToken, null, USERS_API_ENDPOINT);
    }

    public static HttpUriRequest getListRolePermissionsRequest(int roleId, String host, int port, String authToken) throws URISyntaxException {
        return getHttpUriRequestWithBasicParams(host, port, authToken, null, String.format(LIST_ROLE_PERMISSIONS_API_ENDPOINT, roleId));
    }

    public static HttpUriRequest getListRolesRequest(String host, int port, String authToken) throws URISyntaxException {
        return getHttpUriRequestWithBasicParams(host, port, authToken, null, LIST_ROLES_API_ENDPOINT);
    }

    public static HttpUriRequest getListPermissionsRequest(String host, int port, String authToken) throws URISyntaxException {
        return getHttpUriRequestWithBasicParams(host, port, authToken, null, LIST_PERMISSIONS_API_ENDPOINT);
    }

    public static HttpUriRequest getDashboardRequest(int dashboardId, String host, int port, String authToken) throws URISyntaxException {
        return getHttpUriRequestWithBasicParams(host, port, authToken, null, String.format(DASHBOARD_API_ENDPOINT, dashboardId));
    }

    public static HttpUriRequest getListDashboardsRequest(String host, int port, String authToken) throws URISyntaxException {
        return getHttpUriRequestWithBasicParams(host, port, authToken, null, LIST_DASHBOARD_API_ENDPOINT);
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    private static HttpUriRequest getHttpUriRequestWithBasicParams(String host, int port, String authToken, JsonArray columns, String listDashboardApiEndpoint) throws
            URISyntaxException {
        JsonObject param = new JsonObject();
        param.addProperty("page", 0);
        param.addProperty("page_size", Integer.MAX_VALUE);
        if (columns != null) {
            param.add("columns", columns);
        }

        URI apiUri = buildUri(host, port, listDashboardApiEndpoint, List.of(Pair.of("q", param.toString())));

        return RequestBuilder.get() //
                             .setUri(apiUri) //
                             .setHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_VALUE_PREFIX + authToken) //
                             .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                             .build();
    }

    public static HttpUriRequest getExportDashboardRequest(String host, int port, String authToken, int dashboardId) throws URISyntaxException {
        JsonArray ids = new JsonArray();
        ids.add(dashboardId);

        URI apiUri = buildUri(host, port, EXPORT_DASHBOARD_API_ENDPOINT, List.of(Pair.of("q", ids.toString())));

        return RequestBuilder.get() //
                             .setUri(apiUri) //
                             .setHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_VALUE_PREFIX + authToken) //
                             .setHeader(HttpHeaders.ACCEPT, ContentType.TEXT_PLAIN.getMimeType()) //
                             .build();
    }

    public static HttpUriRequest getImportDashboardRequest(String host, int port, String authToken, String csrfToken, File compressedDashboardFile, boolean isOverride,
                                                           JsonElement passwords, String sessionCookie) throws URISyntaxException, IOException {
        URI apiUri = buildUri(host, port, IMPORT_DASHBOARD_API_ENDPOINT, null);
        log.debug("create import dashboard request, auth token {}", authToken);
        log.debug("create import dashboard request, csrf token {}", csrfToken);
        log.debug("create import dashboard request, cookie {}", sessionCookie);
        try (FileInputStream fis = new FileInputStream(compressedDashboardFile)) {
            byte[] arr = new byte[(int) compressedDashboardFile.length()];
            int read = fis.read(arr);
            log.debug("{} bytes were read", read);

            ContentType contentType = ContentType.create("multipart/form-data", StandardCharsets.UTF_8);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("formData", arr, ContentType.DEFAULT_BINARY, "dashboard.zip");
            builder.addTextBody("overwrite", String.valueOf(isOverride), contentType);
            builder.addTextBody("passwords", new Gson().toJson(passwords), contentType);

            return RequestBuilder.post() //
                                 .setUri(apiUri) //
                                 .setHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_VALUE_PREFIX + authToken) //
                                 .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                                 .setHeader("X-CSRFToken", csrfToken) //
                                 .setHeader("Cookie", sessionCookie) //
                                 .setEntity(builder.build()) //
                                 .build();
        }
    }

    public static HttpUriRequest getUpdateUserRolesRequest(String host, int port, String authToken, SupersetUserRolesUpdate supersetUserRolesUpdate, int userId) throws
            URISyntaxException, JsonProcessingException {
        String json = getObjectMapper().writeValueAsString(supersetUserRolesUpdate);
        URI apiUri = buildUri(host, port, String.format(UPDATE_USER_API_ENDPOINT, userId), null);
        return createPutRequest(authToken, apiUri, json);
    }

    public static HttpUriRequest getUpdateUserActiveRequest(String host, int port, String authToken, SupersetUserActiveUpdate supersetUserActiveUpdate, int userId) throws
            URISyntaxException, JsonProcessingException {
        String json = getObjectMapper().writeValueAsString(supersetUserActiveUpdate);
        URI apiUri = buildUri(host, port, String.format(UPDATE_USER_API_ENDPOINT, userId), null);
        return createPutRequest(authToken, apiUri, json);
    }

    public static HttpUriRequest getUpdateDashboardPublishedFlagRequest(String host, int port, String authToken,
                                                                        SupersetDashboardPublishedFlagUpdate supersetDashboardPublishedFlagUpdate, int dashboardId) throws
            URISyntaxException, JsonProcessingException {
        String json = getObjectMapper().writeValueAsString(supersetDashboardPublishedFlagUpdate);
        URI apiUri = buildUri(host, port, String.format(DASHBOARD_API_ENDPOINT, dashboardId), null);
        return createPutRequest(authToken, apiUri, json);
    }

    private static HttpUriRequest createPutRequest(String authToken, URI apiUri, String json) {
        return RequestBuilder.put() //
                             .setUri(apiUri) //
                             .setHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_VALUE_PREFIX + authToken) //
                             .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                             .setEntity(new StringEntity(json, ContentType.APPLICATION_JSON)) //
                             .build();
    }

    private static URI buildUri(String host, int port, String endpoint, List<Pair<String, String>> params) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost(host).setPort(port).setPath(endpoint);

        if (!CollectionUtils.isEmpty(params)) {
            params.forEach(p -> {
                builder.addParameter(p.getKey(), p.getValue());
            });
        }

        return builder.build();
    }
}
