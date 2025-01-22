package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.Group;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ModelApiResponse;

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
public class GroupsApi {
    private ApiClient apiClient;

    public GroupsApi() {
        this(new ApiClient());
    }

    @Autowired
    public GroupsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Add group
     * Adds a new group
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param group The group parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return Group
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addGroupRequestCreation(Group group, Integer confidentialData) throws WebClientResponseException {
        Object postBody = group;
        // verify the required parameter 'group' is set
        if (group == null) {
            throw new WebClientResponseException("Missing the required parameter 'group' when calling addGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "confidential_data", confidentialData));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<Group> localVarReturnType = new ParameterizedTypeReference<Group>() {};
        return apiClient.invokeAPI("/groups", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add group
     * Adds a new group
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param group The group parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return Group
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<Group> addGroup(Group group, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<Group> localVarReturnType = new ParameterizedTypeReference<Group>() {};
        return addGroupRequestCreation(group, confidentialData).bodyToMono(localVarReturnType);
    }

    /**
     * Add group
     * Adds a new group
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param group The group parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseEntity&lt;Group&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<Group>> addGroupWithHttpInfo(Group group, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<Group> localVarReturnType = new ParameterizedTypeReference<Group>() {};
        return addGroupRequestCreation(group, confidentialData).toEntity(localVarReturnType);
    }

    /**
     * Add group
     * Adds a new group
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param group The group parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addGroupWithResponseSpec(Group group, Integer confidentialData) throws WebClientResponseException {
        return addGroupRequestCreation(group, confidentialData);
    }
    /**
     * Delete group
     * Deletes an existing group
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteGroupRequestCreation(String name) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling deleteGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

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

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/groups/{name}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete group
     * Deletes an existing group
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> deleteGroup(String name) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteGroupRequestCreation(name).bodyToMono(localVarReturnType);
    }

    /**
     * Delete group
     * Deletes an existing group
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> deleteGroupWithHttpInfo(String name) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteGroupRequestCreation(name).toEntity(localVarReturnType);
    }

    /**
     * Delete group
     * Deletes an existing group
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteGroupWithResponseSpec(String name) throws WebClientResponseException {
        return deleteGroupRequestCreation(name);
    }
    /**
     * Find groups by name
     * Returns the group with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return Group
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getGroupByNameRequestCreation(String name, Integer confidentialData) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling getGroupByName", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "confidential_data", confidentialData));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<Group> localVarReturnType = new ParameterizedTypeReference<Group>() {};
        return apiClient.invokeAPI("/groups/{name}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Find groups by name
     * Returns the group with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return Group
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<Group> getGroupByName(String name, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<Group> localVarReturnType = new ParameterizedTypeReference<Group>() {};
        return getGroupByNameRequestCreation(name, confidentialData).bodyToMono(localVarReturnType);
    }

    /**
     * Find groups by name
     * Returns the group with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseEntity&lt;Group&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<Group>> getGroupByNameWithHttpInfo(String name, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<Group> localVarReturnType = new ParameterizedTypeReference<Group>() {};
        return getGroupByNameRequestCreation(name, confidentialData).toEntity(localVarReturnType);
    }

    /**
     * Find groups by name
     * Returns the group with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getGroupByNameWithResponseSpec(String name, Integer confidentialData) throws WebClientResponseException {
        return getGroupByNameRequestCreation(name, confidentialData);
    }
    /**
     * Get groups
     * Returns an array with one or more groups
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering groups by name. Default ASC
     * @return List&lt;Group&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getGroupsRequestCreation(Integer offset, Integer limit, String order) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "offset", offset));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "limit", limit));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "order", order));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<Group> localVarReturnType = new ParameterizedTypeReference<Group>() {};
        return apiClient.invokeAPI("/groups", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get groups
     * Returns an array with one or more groups
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering groups by name. Default ASC
     * @return List&lt;Group&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<Group> getGroups(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<Group> localVarReturnType = new ParameterizedTypeReference<Group>() {};
        return getGroupsRequestCreation(offset, limit, order).bodyToFlux(localVarReturnType);
    }

    /**
     * Get groups
     * Returns an array with one or more groups
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering groups by name. Default ASC
     * @return ResponseEntity&lt;List&lt;Group&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<Group>>> getGroupsWithHttpInfo(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<Group> localVarReturnType = new ParameterizedTypeReference<Group>() {};
        return getGroupsRequestCreation(offset, limit, order).toEntityList(localVarReturnType);
    }

    /**
     * Get groups
     * Returns an array with one or more groups
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering groups by name. Default ASC
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getGroupsWithResponseSpec(Integer offset, Integer limit, String order) throws WebClientResponseException {
        return getGroupsRequestCreation(offset, limit, order);
    }
    /**
     * Update group
     * Updates an existing group
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @param group The group parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateGroupRequestCreation(String name, Group group) throws WebClientResponseException {
        Object postBody = group;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling updateGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'group' is set
        if (group == null) {
            throw new WebClientResponseException("Missing the required parameter 'group' when calling updateGroup", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

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
        return apiClient.invokeAPI("/groups/{name}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update group
     * Updates an existing group
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @param group The group parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> updateGroup(String name, Group group) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateGroupRequestCreation(name, group).bodyToMono(localVarReturnType);
    }

    /**
     * Update group
     * Updates an existing group
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @param group The group parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> updateGroupWithHttpInfo(String name, Group group) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateGroupRequestCreation(name, group).toEntity(localVarReturnType);
    }

    /**
     * Update group
     * Updates an existing group
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name group name
     * @param group The group parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateGroupWithResponseSpec(String name, Group group) throws WebClientResponseException {
        return updateGroupRequestCreation(name, group);
    }
}
