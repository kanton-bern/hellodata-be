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

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Builds requests against the Airflow 3 stable REST API (v2). Airflow 3 removed the FAB
 * user/role/permission endpoints from the core API, so this monitoring sidecar only reads
 * DAGs and DAG runs. Authentication is a Bearer JWT (see {@link AirflowClient}).
 */
@UtilityClass
public class AirflowApiRequestBuilder {

    private static final String DAGS_API_ENDPOINT = "/api/v2/dags";
    private static final String DAG_RUNS_API_ENDPOINT = DAGS_API_ENDPOINT + "/%s/dagRuns";

    public static HttpUriRequest getDagsRequest(String host, int port, String bearerToken) throws URISyntaxException {
        return get(buildUri(host, port, DAGS_API_ENDPOINT, Collections.emptyList()), bearerToken);
    }

    public static HttpUriRequest getDagRunsRequest(String host, int port, String bearerToken, String dagId, String orderBy, String limit)
            throws URISyntaxException {
        URI apiUri = buildUri(host, port, String.format(DAG_RUNS_API_ENDPOINT, dagId),
                List.of(Pair.of("order_by", orderBy), Pair.of("limit", limit)));
        return get(apiUri, bearerToken);
    }

    private static HttpUriRequest get(URI uri, String bearerToken) {
        return RequestBuilder.get()
                .setUri(uri)
                .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType())
                .build();
    }

    private static URI buildUri(String host, int port, String endpoint, List<Pair<String, String>> params) throws URISyntaxException {
        URIBuilder builder = new URIBuilder().setScheme("http").setHost(host).setPort(port).setPath(endpoint);
        if (!CollectionUtils.isEmpty(params)) {
            params.forEach(p -> builder.addParameter(p.getKey(), p.getValue()));
        }
        return builder.build();
    }
}
