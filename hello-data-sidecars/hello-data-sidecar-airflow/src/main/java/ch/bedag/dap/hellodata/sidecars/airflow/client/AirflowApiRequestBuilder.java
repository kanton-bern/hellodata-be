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

import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUser;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserRolesUpdate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class AirflowApiRequestBuilder {

    private static final String USERS_API_ENDPOINT = "/api/v1/users";
    private static final String UPDATE_USER_API_ENDPOINT = USERS_API_ENDPOINT + "/%s";
    private static final String DELETE_USER_API_ENDPOINT = USERS_API_ENDPOINT + "/%s";
    private static final String GET_USER_API_ENDPOINT = USERS_API_ENDPOINT + "/%s";
    private static final String ROLES_API_ENDPOINT = "/api/v1/roles";
    private static final String PERMISSIONS_API_ENDPOINT = "/api/v1/permissions";
    private static final String DAGS_API_ENDPOINT = "/api/v1/dags";
    private static final String DAG_RUNS_API_ENDPOINT = DAGS_API_ENDPOINT + "/%s/dagRuns";

    public static HttpUriRequest getListUsersRequest(String host, int port, String username, String password, int offset, int limit) throws URISyntaxException {
        return getHttpUriRequestWithPagination(host, port, username, password, USERS_API_ENDPOINT, offset, limit);
    }

    private static HttpUriRequest getHttpUriRequestWithPagination(String host, int port, String username, String password, String endpoint, int offset, int limit) throws
            URISyntaxException {
        Pair<String, String> offsetParam = Pair.of("offset", "" + offset);
        Pair<String, String> limitParam = Pair.of("limit", "" + limit);
        Pair<String, String> orderByParam = Pair.of("order_by", "email");

        URI apiUri = buildUri(host, port, endpoint, List.of(offsetParam, limitParam, orderByParam));

        return RequestBuilder.get() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader(username, password)) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .build();
    }

    public static HttpUriRequest getUserRequest(String host, int port, String username, String password, String airflowUsername) throws URISyntaxException, IOException {
        URI apiUri = buildUri(host, port, String.format(GET_USER_API_ENDPOINT, airflowUsername), null);
        return RequestBuilder.get() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader(username, password)) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .build();
    }

    public static HttpUriRequest getCreateUserRequest(String host, int port, String username, String password, AirflowUser airflowUser) throws URISyntaxException, IOException {
        URI apiUri = buildUri(host, port, USERS_API_ENDPOINT, null);
        String json = getObjectMapper().writeValueAsString(airflowUser);
        return RequestBuilder.post() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader(username, password)) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .setEntity(new StringEntity(json, ContentType.APPLICATION_JSON)) //
                .build();
    }

    public static HttpUriRequest getDeleteUserRequest(String host, int port, String username, String password, String userNameToUpdate) throws URISyntaxException, IOException {
        URI apiUri = buildUri(host, port, String.format(DELETE_USER_API_ENDPOINT, userNameToUpdate), Collections.emptyList());
        return RequestBuilder.delete() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader(username, password)) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .build();
    }

    public static HttpUriRequest getCreateRoleRequest(String host, int port, String username, String password, AirflowRole airflowRole) throws URISyntaxException, IOException {
        URI apiUri = buildUri(host, port, ROLES_API_ENDPOINT, null);
        String json = getObjectMapper().writeValueAsString(airflowRole);
        return RequestBuilder.post() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader(username, password)) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .setEntity(new StringEntity(json, ContentType.APPLICATION_JSON)) //
                .build();
    }

    public static HttpUriRequest getUpdateUserRequest(String host, int port, String username, String password, AirflowUserRolesUpdate airflowUserRolesUpdate,
                                                      String userNameToUpdate) throws URISyntaxException, JsonProcessingException {
        URI apiUri = buildUri(host, port, String.format(UPDATE_USER_API_ENDPOINT, userNameToUpdate), List.of(Pair.of("update_mask", "roles")));
        String json = getObjectMapper().writeValueAsString(airflowUserRolesUpdate);
        return RequestBuilder.patch() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader(username, password)) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .setEntity(new StringEntity(json, ContentType.APPLICATION_JSON)) //
                .build();
    }

    public static HttpUriRequest getListRolesRequest(String host, int port, String username, String password) throws URISyntaxException {
        return getHttpUriRequestWithBasicParams(host, port, username, password, ROLES_API_ENDPOINT);
    }

    public static HttpUriRequest getListPermissionsRequest(String host, int port, String username, String password) throws URISyntaxException {
        return getHttpUriRequestWithBasicParams(host, port, username, password, PERMISSIONS_API_ENDPOINT);
    }

    public static HttpUriRequest getDagsRequest(String host, int port, String username, String password) throws URISyntaxException {
        return getHttpUriRequestWithBasicParams(host, port, username, password, DAGS_API_ENDPOINT);
    }

    public static HttpUriRequest getDagRunsRequest(String host, int port, String username, String password, String dagId, String orderBy, String limit) throws URISyntaxException {
        URI apiUri = buildUri(host, port, String.format(DAG_RUNS_API_ENDPOINT, dagId), List.of(Pair.of("order_by", orderBy), Pair.of("limit", limit)));
        return RequestBuilder.get() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader(username, password)) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .build();
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    private static HttpUriRequest getHttpUriRequestWithBasicParams(String host, int port, String username, String password, String endpoint) throws URISyntaxException {
        URI apiUri = buildUri(host, port, endpoint, Collections.EMPTY_LIST);

        return RequestBuilder.get() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader(username, password)) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .build();
    }

    private static String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
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
