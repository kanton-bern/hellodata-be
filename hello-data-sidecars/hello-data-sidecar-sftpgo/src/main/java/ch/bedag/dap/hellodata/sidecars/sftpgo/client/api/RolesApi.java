package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ModelApiResponse;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.Role;

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
public class RolesApi {
    private ApiClient apiClient;

    public RolesApi() {
        this(new ApiClient());
    }

    @Autowired
    public RolesApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Add role
     * Adds a new role
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param role The role parameter
     * @return Role
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addRoleRequestCreation(Role role) throws WebClientResponseException {
        Object postBody = role;
        // verify the required parameter 'role' is set
        if (role == null) {
            throw new WebClientResponseException("Missing the required parameter 'role' when calling addRole", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
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
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<Role> localVarReturnType = new ParameterizedTypeReference<Role>() {};
        return apiClient.invokeAPI("/roles", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add role
     * Adds a new role
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param role The role parameter
     * @return Role
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<Role> addRole(Role role) throws WebClientResponseException {
        ParameterizedTypeReference<Role> localVarReturnType = new ParameterizedTypeReference<Role>() {};
        return addRoleRequestCreation(role).bodyToMono(localVarReturnType);
    }

    /**
     * Add role
     * Adds a new role
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param role The role parameter
     * @return ResponseEntity&lt;Role&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<Role>> addRoleWithHttpInfo(Role role) throws WebClientResponseException {
        ParameterizedTypeReference<Role> localVarReturnType = new ParameterizedTypeReference<Role>() {};
        return addRoleRequestCreation(role).toEntity(localVarReturnType);
    }

    /**
     * Add role
     * Adds a new role
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param role The role parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addRoleWithResponseSpec(Role role) throws WebClientResponseException {
        return addRoleRequestCreation(role);
    }
    /**
     * Delete role
     * Deletes an existing role
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteRoleRequestCreation(String name) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling deleteRole", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/roles/{name}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete role
     * Deletes an existing role
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> deleteRole(String name) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteRoleRequestCreation(name).bodyToMono(localVarReturnType);
    }

    /**
     * Delete role
     * Deletes an existing role
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> deleteRoleWithHttpInfo(String name) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteRoleRequestCreation(name).toEntity(localVarReturnType);
    }

    /**
     * Delete role
     * Deletes an existing role
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteRoleWithResponseSpec(String name) throws WebClientResponseException {
        return deleteRoleRequestCreation(name);
    }
    /**
     * Find roles by name
     * Returns the role with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @return Role
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getRoleByNameRequestCreation(String name) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling getRoleByName", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<Role> localVarReturnType = new ParameterizedTypeReference<Role>() {};
        return apiClient.invokeAPI("/roles/{name}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Find roles by name
     * Returns the role with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @return Role
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<Role> getRoleByName(String name) throws WebClientResponseException {
        ParameterizedTypeReference<Role> localVarReturnType = new ParameterizedTypeReference<Role>() {};
        return getRoleByNameRequestCreation(name).bodyToMono(localVarReturnType);
    }

    /**
     * Find roles by name
     * Returns the role with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @return ResponseEntity&lt;Role&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<Role>> getRoleByNameWithHttpInfo(String name) throws WebClientResponseException {
        ParameterizedTypeReference<Role> localVarReturnType = new ParameterizedTypeReference<Role>() {};
        return getRoleByNameRequestCreation(name).toEntity(localVarReturnType);
    }

    /**
     * Find roles by name
     * Returns the role with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getRoleByNameWithResponseSpec(String name) throws WebClientResponseException {
        return getRoleByNameRequestCreation(name);
    }
    /**
     * Get roles
     * Returns an array with one or more roles
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering groups by name. Default ASC
     * @return List&lt;Role&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getRolesRequestCreation(Integer offset, Integer limit, String order) throws WebClientResponseException {
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

        ParameterizedTypeReference<Role> localVarReturnType = new ParameterizedTypeReference<Role>() {};
        return apiClient.invokeAPI("/roles", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get roles
     * Returns an array with one or more roles
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering groups by name. Default ASC
     * @return List&lt;Role&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<Role> getRoles(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<Role> localVarReturnType = new ParameterizedTypeReference<Role>() {};
        return getRolesRequestCreation(offset, limit, order).bodyToFlux(localVarReturnType);
    }

    /**
     * Get roles
     * Returns an array with one or more roles
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering groups by name. Default ASC
     * @return ResponseEntity&lt;List&lt;Role&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<Role>>> getRolesWithHttpInfo(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<Role> localVarReturnType = new ParameterizedTypeReference<Role>() {};
        return getRolesRequestCreation(offset, limit, order).toEntityList(localVarReturnType);
    }

    /**
     * Get roles
     * Returns an array with one or more roles
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
    public ResponseSpec getRolesWithResponseSpec(Integer offset, Integer limit, String order) throws WebClientResponseException {
        return getRolesRequestCreation(offset, limit, order);
    }
    /**
     * Update role
     * Updates an existing role
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @param role The role parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateRoleRequestCreation(String name, Role role) throws WebClientResponseException {
        Object postBody = role;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling updateRole", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'role' is set
        if (role == null) {
            throw new WebClientResponseException("Missing the required parameter 'role' when calling updateRole", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/roles/{name}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update role
     * Updates an existing role
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @param role The role parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> updateRole(String name, Role role) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateRoleRequestCreation(name, role).bodyToMono(localVarReturnType);
    }

    /**
     * Update role
     * Updates an existing role
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @param role The role parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> updateRoleWithHttpInfo(String name, Role role) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateRoleRequestCreation(name, role).toEntity(localVarReturnType);
    }

    /**
     * Update role
     * Updates an existing role
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name role name
     * @param role The role parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateRoleWithResponseSpec(String name, Role role) throws WebClientResponseException {
        return updateRoleRequestCreation(name, role);
    }
}
