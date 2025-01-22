package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.BaseVirtualFolder;
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
public class FoldersApi {
    private ApiClient apiClient;

    public FoldersApi() {
        this(new ApiClient());
    }

    @Autowired
    public FoldersApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Add folder
     * Adds a new folder. A quota scan is required to update the used files/size
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param baseVirtualFolder The baseVirtualFolder parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return BaseVirtualFolder
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addFolderRequestCreation(BaseVirtualFolder baseVirtualFolder, Integer confidentialData) throws WebClientResponseException {
        Object postBody = baseVirtualFolder;
        // verify the required parameter 'baseVirtualFolder' is set
        if (baseVirtualFolder == null) {
            throw new WebClientResponseException("Missing the required parameter 'baseVirtualFolder' when calling addFolder", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<BaseVirtualFolder> localVarReturnType = new ParameterizedTypeReference<BaseVirtualFolder>() {};
        return apiClient.invokeAPI("/folders", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add folder
     * Adds a new folder. A quota scan is required to update the used files/size
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param baseVirtualFolder The baseVirtualFolder parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return BaseVirtualFolder
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<BaseVirtualFolder> addFolder(BaseVirtualFolder baseVirtualFolder, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<BaseVirtualFolder> localVarReturnType = new ParameterizedTypeReference<BaseVirtualFolder>() {};
        return addFolderRequestCreation(baseVirtualFolder, confidentialData).bodyToMono(localVarReturnType);
    }

    /**
     * Add folder
     * Adds a new folder. A quota scan is required to update the used files/size
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param baseVirtualFolder The baseVirtualFolder parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseEntity&lt;BaseVirtualFolder&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<BaseVirtualFolder>> addFolderWithHttpInfo(BaseVirtualFolder baseVirtualFolder, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<BaseVirtualFolder> localVarReturnType = new ParameterizedTypeReference<BaseVirtualFolder>() {};
        return addFolderRequestCreation(baseVirtualFolder, confidentialData).toEntity(localVarReturnType);
    }

    /**
     * Add folder
     * Adds a new folder. A quota scan is required to update the used files/size
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param baseVirtualFolder The baseVirtualFolder parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addFolderWithResponseSpec(BaseVirtualFolder baseVirtualFolder, Integer confidentialData) throws WebClientResponseException {
        return addFolderRequestCreation(baseVirtualFolder, confidentialData);
    }
    /**
     * Delete folder
     * Deletes an existing folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteFolderRequestCreation(String name) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling deleteFolder", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/folders/{name}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete folder
     * Deletes an existing folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> deleteFolder(String name) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteFolderRequestCreation(name).bodyToMono(localVarReturnType);
    }

    /**
     * Delete folder
     * Deletes an existing folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> deleteFolderWithHttpInfo(String name) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteFolderRequestCreation(name).toEntity(localVarReturnType);
    }

    /**
     * Delete folder
     * Deletes an existing folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteFolderWithResponseSpec(String name) throws WebClientResponseException {
        return deleteFolderRequestCreation(name);
    }
    /**
     * Find folders by name
     * Returns the folder with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return BaseVirtualFolder
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFolderByNameRequestCreation(String name, Integer confidentialData) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling getFolderByName", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<BaseVirtualFolder> localVarReturnType = new ParameterizedTypeReference<BaseVirtualFolder>() {};
        return apiClient.invokeAPI("/folders/{name}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Find folders by name
     * Returns the folder with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return BaseVirtualFolder
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<BaseVirtualFolder> getFolderByName(String name, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<BaseVirtualFolder> localVarReturnType = new ParameterizedTypeReference<BaseVirtualFolder>() {};
        return getFolderByNameRequestCreation(name, confidentialData).bodyToMono(localVarReturnType);
    }

    /**
     * Find folders by name
     * Returns the folder with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseEntity&lt;BaseVirtualFolder&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<BaseVirtualFolder>> getFolderByNameWithHttpInfo(String name, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<BaseVirtualFolder> localVarReturnType = new ParameterizedTypeReference<BaseVirtualFolder>() {};
        return getFolderByNameRequestCreation(name, confidentialData).toEntity(localVarReturnType);
    }

    /**
     * Find folders by name
     * Returns the folder with the given name if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFolderByNameWithResponseSpec(String name, Integer confidentialData) throws WebClientResponseException {
        return getFolderByNameRequestCreation(name, confidentialData);
    }
    /**
     * Get folders
     * Returns an array with one or more folders
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering folders by name. Default ASC
     * @return List&lt;BaseVirtualFolder&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFoldersRequestCreation(Integer offset, Integer limit, String order) throws WebClientResponseException {
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

        ParameterizedTypeReference<BaseVirtualFolder> localVarReturnType = new ParameterizedTypeReference<BaseVirtualFolder>() {};
        return apiClient.invokeAPI("/folders", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get folders
     * Returns an array with one or more folders
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering folders by name. Default ASC
     * @return List&lt;BaseVirtualFolder&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<BaseVirtualFolder> getFolders(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<BaseVirtualFolder> localVarReturnType = new ParameterizedTypeReference<BaseVirtualFolder>() {};
        return getFoldersRequestCreation(offset, limit, order).bodyToFlux(localVarReturnType);
    }

    /**
     * Get folders
     * Returns an array with one or more folders
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering folders by name. Default ASC
     * @return ResponseEntity&lt;List&lt;BaseVirtualFolder&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<BaseVirtualFolder>>> getFoldersWithHttpInfo(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<BaseVirtualFolder> localVarReturnType = new ParameterizedTypeReference<BaseVirtualFolder>() {};
        return getFoldersRequestCreation(offset, limit, order).toEntityList(localVarReturnType);
    }

    /**
     * Get folders
     * Returns an array with one or more folders
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering folders by name. Default ASC
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFoldersWithResponseSpec(Integer offset, Integer limit, String order) throws WebClientResponseException {
        return getFoldersRequestCreation(offset, limit, order);
    }
    /**
     * Update folder
     * Updates an existing folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param baseVirtualFolder The baseVirtualFolder parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateFolderRequestCreation(String name, BaseVirtualFolder baseVirtualFolder) throws WebClientResponseException {
        Object postBody = baseVirtualFolder;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling updateFolder", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'baseVirtualFolder' is set
        if (baseVirtualFolder == null) {
            throw new WebClientResponseException("Missing the required parameter 'baseVirtualFolder' when calling updateFolder", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/folders/{name}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update folder
     * Updates an existing folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param baseVirtualFolder The baseVirtualFolder parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> updateFolder(String name, BaseVirtualFolder baseVirtualFolder) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateFolderRequestCreation(name, baseVirtualFolder).bodyToMono(localVarReturnType);
    }

    /**
     * Update folder
     * Updates an existing folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param baseVirtualFolder The baseVirtualFolder parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> updateFolderWithHttpInfo(String name, BaseVirtualFolder baseVirtualFolder) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateFolderRequestCreation(name, baseVirtualFolder).toEntity(localVarReturnType);
    }

    /**
     * Update folder
     * Updates an existing folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param baseVirtualFolder The baseVirtualFolder parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateFolderWithResponseSpec(String name, BaseVirtualFolder baseVirtualFolder) throws WebClientResponseException {
        return updateFolderRequestCreation(name, baseVirtualFolder);
    }
}
