package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.FolderRetention;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ModelApiResponse;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.RetentionCheck;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.RetentionCheckNotification;

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
public class DataRetentionApi {
    private ApiClient apiClient;

    public DataRetentionApi() {
        this(new ApiClient());
    }

    @Autowired
    public DataRetentionApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Get retention checks
     * Returns the active retention checks
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;RetentionCheck&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUsersRetentionChecksRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<RetentionCheck> localVarReturnType = new ParameterizedTypeReference<RetentionCheck>() {};
        return apiClient.invokeAPI("/retention/users/checks", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get retention checks
     * Returns the active retention checks
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;RetentionCheck&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<RetentionCheck> getUsersRetentionChecks() throws WebClientResponseException {
        ParameterizedTypeReference<RetentionCheck> localVarReturnType = new ParameterizedTypeReference<RetentionCheck>() {};
        return getUsersRetentionChecksRequestCreation().bodyToFlux(localVarReturnType);
    }

    /**
     * Get retention checks
     * Returns the active retention checks
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;List&lt;RetentionCheck&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<RetentionCheck>>> getUsersRetentionChecksWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<RetentionCheck> localVarReturnType = new ParameterizedTypeReference<RetentionCheck>() {};
        return getUsersRetentionChecksRequestCreation().toEntityList(localVarReturnType);
    }

    /**
     * Get retention checks
     * Returns the active retention checks
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUsersRetentionChecksWithResponseSpec() throws WebClientResponseException {
        return getUsersRetentionChecksRequestCreation();
    }
    /**
     * Start a retention check
     * Starts a new retention check for the given user. If a retention check for this user is already active a 409 status code is returned
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param folderRetention Defines virtual paths to check and their retention time in hours
     * @param notifications specify how to notify results
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec startUserRetentionCheckRequestCreation(String username, List<FolderRetention> folderRetention, List<RetentionCheckNotification> notifications) throws WebClientResponseException {
        Object postBody = folderRetention;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling startUserRetentionCheck", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'folderRetention' is set
        if (folderRetention == null) {
            throw new WebClientResponseException("Missing the required parameter 'folderRetention' when calling startUserRetentionCheck", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("username", username);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "notifications", notifications));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/retention/users/{username}/check", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Start a retention check
     * Starts a new retention check for the given user. If a retention check for this user is already active a 409 status code is returned
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param folderRetention Defines virtual paths to check and their retention time in hours
     * @param notifications specify how to notify results
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> startUserRetentionCheck(String username, List<FolderRetention> folderRetention, List<RetentionCheckNotification> notifications) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return startUserRetentionCheckRequestCreation(username, folderRetention, notifications).bodyToMono(localVarReturnType);
    }

    /**
     * Start a retention check
     * Starts a new retention check for the given user. If a retention check for this user is already active a 409 status code is returned
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param folderRetention Defines virtual paths to check and their retention time in hours
     * @param notifications specify how to notify results
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> startUserRetentionCheckWithHttpInfo(String username, List<FolderRetention> folderRetention, List<RetentionCheckNotification> notifications) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return startUserRetentionCheckRequestCreation(username, folderRetention, notifications).toEntity(localVarReturnType);
    }

    /**
     * Start a retention check
     * Starts a new retention check for the given user. If a retention check for this user is already active a 409 status code is returned
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param folderRetention Defines virtual paths to check and their retention time in hours
     * @param notifications specify how to notify results
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec startUserRetentionCheckWithResponseSpec(String username, List<FolderRetention> folderRetention, List<RetentionCheckNotification> notifications) throws WebClientResponseException {
        return startUserRetentionCheckRequestCreation(username, folderRetention, notifications);
    }
}
