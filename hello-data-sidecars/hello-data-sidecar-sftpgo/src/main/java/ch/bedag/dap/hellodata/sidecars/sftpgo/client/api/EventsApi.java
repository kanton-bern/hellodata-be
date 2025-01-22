package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.EventProtocols;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.FsEvent;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.FsEventAction;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.FsEventStatus;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.FsProviders;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.LogEvent;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.LogEventType;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ModelApiResponse;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ProviderEvent;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ProviderEventAction;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ProviderEventObjectType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class EventsApi {
    private ApiClient apiClient;

    public EventsApi() {
        this(new ApiClient());
    }

    @Autowired
    public EventsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Get filesystem events
     * Returns an array with one or more filesystem events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param actions the event action must be included among those specified. Empty or missing means omit this filter. Actions must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param sshCmd the event SSH command must be the same as the one specified. Empty or missing means omit this filter
     * @param fsProvider the event filesystem provider must be the same as the one specified. Empty or missing means omit this filter
     * @param bucket the bucket must be the same as the one specified. Empty or missing means omit this filter
     * @param endpoint the endpoint must be the same as the one specified. Empty or missing means omit this filter
     * @param protocols the event protocol must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param statuses the event status must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role User role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return List&lt;FsEvent&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFsEventsRequestCreation(Long startTimestamp, Long endTimestamp, List<FsEventAction> actions, String username, String ip, String sshCmd, FsProviders fsProvider, String bucket, String endpoint, List<EventProtocols> protocols, List<FsEventStatus> statuses, List<String> instanceIds, String fromId, String role, Boolean csvExport, Integer limit, String order) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "start_timestamp", startTimestamp));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "end_timestamp", endTimestamp));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "actions", actions));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "username", username));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "ip", ip));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "ssh_cmd", sshCmd));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "fs_provider", fsProvider));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "bucket", bucket));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "endpoint", endpoint));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "protocols", protocols));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "statuses", statuses));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "instance_ids", instanceIds));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "from_id", fromId));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "role", role));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "csv_export", csvExport));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "limit", limit));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "order", order));
        
        final String[] localVarAccepts = { 
            "application/json", "text/csv"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<FsEvent> localVarReturnType = new ParameterizedTypeReference<FsEvent>() {};
        return apiClient.invokeAPI("/events/fs", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get filesystem events
     * Returns an array with one or more filesystem events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param actions the event action must be included among those specified. Empty or missing means omit this filter. Actions must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param sshCmd the event SSH command must be the same as the one specified. Empty or missing means omit this filter
     * @param fsProvider the event filesystem provider must be the same as the one specified. Empty or missing means omit this filter
     * @param bucket the bucket must be the same as the one specified. Empty or missing means omit this filter
     * @param endpoint the endpoint must be the same as the one specified. Empty or missing means omit this filter
     * @param protocols the event protocol must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param statuses the event status must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role User role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return List&lt;FsEvent&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<FsEvent> getFsEvents(Long startTimestamp, Long endTimestamp, List<FsEventAction> actions, String username, String ip, String sshCmd, FsProviders fsProvider, String bucket, String endpoint, List<EventProtocols> protocols, List<FsEventStatus> statuses, List<String> instanceIds, String fromId, String role, Boolean csvExport, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<FsEvent> localVarReturnType = new ParameterizedTypeReference<FsEvent>() {};
        return getFsEventsRequestCreation(startTimestamp, endTimestamp, actions, username, ip, sshCmd, fsProvider, bucket, endpoint, protocols, statuses, instanceIds, fromId, role, csvExport, limit, order).bodyToFlux(localVarReturnType);
    }

    /**
     * Get filesystem events
     * Returns an array with one or more filesystem events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param actions the event action must be included among those specified. Empty or missing means omit this filter. Actions must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param sshCmd the event SSH command must be the same as the one specified. Empty or missing means omit this filter
     * @param fsProvider the event filesystem provider must be the same as the one specified. Empty or missing means omit this filter
     * @param bucket the bucket must be the same as the one specified. Empty or missing means omit this filter
     * @param endpoint the endpoint must be the same as the one specified. Empty or missing means omit this filter
     * @param protocols the event protocol must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param statuses the event status must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role User role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return ResponseEntity&lt;List&lt;FsEvent&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<FsEvent>>> getFsEventsWithHttpInfo(Long startTimestamp, Long endTimestamp, List<FsEventAction> actions, String username, String ip, String sshCmd, FsProviders fsProvider, String bucket, String endpoint, List<EventProtocols> protocols, List<FsEventStatus> statuses, List<String> instanceIds, String fromId, String role, Boolean csvExport, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<FsEvent> localVarReturnType = new ParameterizedTypeReference<FsEvent>() {};
        return getFsEventsRequestCreation(startTimestamp, endTimestamp, actions, username, ip, sshCmd, fsProvider, bucket, endpoint, protocols, statuses, instanceIds, fromId, role, csvExport, limit, order).toEntityList(localVarReturnType);
    }

    /**
     * Get filesystem events
     * Returns an array with one or more filesystem events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param actions the event action must be included among those specified. Empty or missing means omit this filter. Actions must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param sshCmd the event SSH command must be the same as the one specified. Empty or missing means omit this filter
     * @param fsProvider the event filesystem provider must be the same as the one specified. Empty or missing means omit this filter
     * @param bucket the bucket must be the same as the one specified. Empty or missing means omit this filter
     * @param endpoint the endpoint must be the same as the one specified. Empty or missing means omit this filter
     * @param protocols the event protocol must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param statuses the event status must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role User role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFsEventsWithResponseSpec(Long startTimestamp, Long endTimestamp, List<FsEventAction> actions, String username, String ip, String sshCmd, FsProviders fsProvider, String bucket, String endpoint, List<EventProtocols> protocols, List<FsEventStatus> statuses, List<String> instanceIds, String fromId, String role, Boolean csvExport, Integer limit, String order) throws WebClientResponseException {
        return getFsEventsRequestCreation(startTimestamp, endTimestamp, actions, username, ip, sshCmd, fsProvider, bucket, endpoint, protocols, statuses, instanceIds, fromId, role, csvExport, limit, order);
    }
    /**
     * Get log events
     * Returns an array with one or more log events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param events the log events must be included among those specified. Empty or missing means omit this filter. Events must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param protocols the event protocol must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role User role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return List&lt;LogEvent&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getLogEventsRequestCreation(Long startTimestamp, Long endTimestamp, List<LogEventType> events, String username, String ip, List<EventProtocols> protocols, List<String> instanceIds, String fromId, String role, Boolean csvExport, Integer limit, String order) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "start_timestamp", startTimestamp));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "end_timestamp", endTimestamp));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "events", events));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "username", username));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "ip", ip));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "protocols", protocols));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "instance_ids", instanceIds));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "from_id", fromId));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "role", role));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "csv_export", csvExport));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "limit", limit));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "order", order));
        
        final String[] localVarAccepts = { 
            "application/json", "text/csv"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<LogEvent> localVarReturnType = new ParameterizedTypeReference<LogEvent>() {};
        return apiClient.invokeAPI("/events/log", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get log events
     * Returns an array with one or more log events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param events the log events must be included among those specified. Empty or missing means omit this filter. Events must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param protocols the event protocol must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role User role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return List&lt;LogEvent&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<LogEvent> getLogEvents(Long startTimestamp, Long endTimestamp, List<LogEventType> events, String username, String ip, List<EventProtocols> protocols, List<String> instanceIds, String fromId, String role, Boolean csvExport, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<LogEvent> localVarReturnType = new ParameterizedTypeReference<LogEvent>() {};
        return getLogEventsRequestCreation(startTimestamp, endTimestamp, events, username, ip, protocols, instanceIds, fromId, role, csvExport, limit, order).bodyToFlux(localVarReturnType);
    }

    /**
     * Get log events
     * Returns an array with one or more log events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param events the log events must be included among those specified. Empty or missing means omit this filter. Events must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param protocols the event protocol must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role User role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return ResponseEntity&lt;List&lt;LogEvent&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<LogEvent>>> getLogEventsWithHttpInfo(Long startTimestamp, Long endTimestamp, List<LogEventType> events, String username, String ip, List<EventProtocols> protocols, List<String> instanceIds, String fromId, String role, Boolean csvExport, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<LogEvent> localVarReturnType = new ParameterizedTypeReference<LogEvent>() {};
        return getLogEventsRequestCreation(startTimestamp, endTimestamp, events, username, ip, protocols, instanceIds, fromId, role, csvExport, limit, order).toEntityList(localVarReturnType);
    }

    /**
     * Get log events
     * Returns an array with one or more log events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param events the log events must be included among those specified. Empty or missing means omit this filter. Events must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param protocols the event protocol must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role User role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getLogEventsWithResponseSpec(Long startTimestamp, Long endTimestamp, List<LogEventType> events, String username, String ip, List<EventProtocols> protocols, List<String> instanceIds, String fromId, String role, Boolean csvExport, Integer limit, String order) throws WebClientResponseException {
        return getLogEventsRequestCreation(startTimestamp, endTimestamp, events, username, ip, protocols, instanceIds, fromId, role, csvExport, limit, order);
    }
    /**
     * Get provider events
     * Returns an array with one or more provider events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param actions the event action must be included among those specified. Empty or missing means omit this filter. Actions must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param objectName the event object name must be the same as the one specified. Empty or missing means omit this filter
     * @param objectTypes the event object type must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role Admin role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param omitObjectData If enabled, returned events will not contain the &#x60;object_data&#x60; field
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return List&lt;ProviderEvent&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getProviderEventsRequestCreation(Long startTimestamp, Long endTimestamp, List<ProviderEventAction> actions, String username, String ip, String objectName, List<ProviderEventObjectType> objectTypes, List<String> instanceIds, String fromId, String role, Boolean csvExport, Boolean omitObjectData, Integer limit, String order) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "start_timestamp", startTimestamp));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "end_timestamp", endTimestamp));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "actions", actions));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "username", username));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "ip", ip));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "object_name", objectName));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "object_types", objectTypes));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "instance_ids", instanceIds));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "from_id", fromId));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "role", role));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "csv_export", csvExport));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "omit_object_data", omitObjectData));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "limit", limit));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "order", order));
        
        final String[] localVarAccepts = { 
            "application/json", "text/csv"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<ProviderEvent> localVarReturnType = new ParameterizedTypeReference<ProviderEvent>() {};
        return apiClient.invokeAPI("/events/provider", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get provider events
     * Returns an array with one or more provider events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param actions the event action must be included among those specified. Empty or missing means omit this filter. Actions must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param objectName the event object name must be the same as the one specified. Empty or missing means omit this filter
     * @param objectTypes the event object type must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role Admin role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param omitObjectData If enabled, returned events will not contain the &#x60;object_data&#x60; field
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return List&lt;ProviderEvent&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<ProviderEvent> getProviderEvents(Long startTimestamp, Long endTimestamp, List<ProviderEventAction> actions, String username, String ip, String objectName, List<ProviderEventObjectType> objectTypes, List<String> instanceIds, String fromId, String role, Boolean csvExport, Boolean omitObjectData, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<ProviderEvent> localVarReturnType = new ParameterizedTypeReference<ProviderEvent>() {};
        return getProviderEventsRequestCreation(startTimestamp, endTimestamp, actions, username, ip, objectName, objectTypes, instanceIds, fromId, role, csvExport, omitObjectData, limit, order).bodyToFlux(localVarReturnType);
    }

    /**
     * Get provider events
     * Returns an array with one or more provider events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param actions the event action must be included among those specified. Empty or missing means omit this filter. Actions must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param objectName the event object name must be the same as the one specified. Empty or missing means omit this filter
     * @param objectTypes the event object type must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role Admin role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param omitObjectData If enabled, returned events will not contain the &#x60;object_data&#x60; field
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return ResponseEntity&lt;List&lt;ProviderEvent&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<ProviderEvent>>> getProviderEventsWithHttpInfo(Long startTimestamp, Long endTimestamp, List<ProviderEventAction> actions, String username, String ip, String objectName, List<ProviderEventObjectType> objectTypes, List<String> instanceIds, String fromId, String role, Boolean csvExport, Boolean omitObjectData, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<ProviderEvent> localVarReturnType = new ParameterizedTypeReference<ProviderEvent>() {};
        return getProviderEventsRequestCreation(startTimestamp, endTimestamp, actions, username, ip, objectName, objectTypes, instanceIds, fromId, role, csvExport, omitObjectData, limit, order).toEntityList(localVarReturnType);
    }

    /**
     * Get provider events
     * Returns an array with one or more provider events applying the specified filters. This API is only available if you configure an \&quot;eventsearcher\&quot; plugin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param startTimestamp the event timestamp, unix timestamp in nanoseconds, must be greater than or equal to the specified one. 0 or missing means omit this filter
     * @param endTimestamp the event timestamp, unix timestamp in nanoseconds, must be less than or equal to the specified one. 0 or missing means omit this filter
     * @param actions the event action must be included among those specified. Empty or missing means omit this filter. Actions must be specified comma separated
     * @param username the event username must be the same as the one specified. Empty or missing means omit this filter
     * @param ip the event IP must be the same as the one specified. Empty or missing means omit this filter
     * @param objectName the event object name must be the same as the one specified. Empty or missing means omit this filter
     * @param objectTypes the event object type must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param instanceIds the event instance id must be included among those specified. Empty or missing means omit this filter. Values must be specified comma separated
     * @param fromId the event id to start from. This is useful for cursor based pagination. Empty or missing means omit this filter.
     * @param role Admin role. Empty or missing means omit this filter. Ignored if the admin has a role
     * @param csvExport If enabled, events are exported as a CSV file
     * @param omitObjectData If enabled, returned events will not contain the &#x60;object_data&#x60; field
     * @param limit The maximum number of items to return. Max value is 1000, default is 100
     * @param order Ordering events by timestamp. Default DESC
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getProviderEventsWithResponseSpec(Long startTimestamp, Long endTimestamp, List<ProviderEventAction> actions, String username, String ip, String objectName, List<ProviderEventObjectType> objectTypes, List<String> instanceIds, String fromId, String role, Boolean csvExport, Boolean omitObjectData, Integer limit, String order) throws WebClientResponseException {
        return getProviderEventsRequestCreation(startTimestamp, endTimestamp, actions, username, ip, objectName, objectTypes, instanceIds, fromId, role, csvExport, omitObjectData, limit, order);
    }
}
