package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.FolderQuotaScan;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ModelApiResponse;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.QuotaScan;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.QuotaUsage;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.TransferQuotaUsage;

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
public class QuotaApi {
    private ApiClient apiClient;

    public QuotaApi() {
        this(new ApiClient());
    }

    @Autowired
    public QuotaApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Update folder quota usage limits
     * Sets the current used quota limits for the given folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param quotaUsage If used_quota_size and used_quota_files are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec folderQuotaUpdateUsageRequestCreation(String name, QuotaUsage quotaUsage, String mode) throws WebClientResponseException {
        Object postBody = quotaUsage;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling folderQuotaUpdateUsage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'quotaUsage' is set
        if (quotaUsage == null) {
            throw new WebClientResponseException("Missing the required parameter 'quotaUsage' when calling folderQuotaUpdateUsage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "mode", mode));
        
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
        return apiClient.invokeAPI("/quotas/folders/{name}/usage", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update folder quota usage limits
     * Sets the current used quota limits for the given folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param quotaUsage If used_quota_size and used_quota_files are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> folderQuotaUpdateUsage(String name, QuotaUsage quotaUsage, String mode) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return folderQuotaUpdateUsageRequestCreation(name, quotaUsage, mode).bodyToMono(localVarReturnType);
    }

    /**
     * Update folder quota usage limits
     * Sets the current used quota limits for the given folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param quotaUsage If used_quota_size and used_quota_files are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> folderQuotaUpdateUsageWithHttpInfo(String name, QuotaUsage quotaUsage, String mode) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return folderQuotaUpdateUsageRequestCreation(name, quotaUsage, mode).toEntity(localVarReturnType);
    }

    /**
     * Update folder quota usage limits
     * Sets the current used quota limits for the given folder
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @param quotaUsage If used_quota_size and used_quota_files are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec folderQuotaUpdateUsageWithResponseSpec(String name, QuotaUsage quotaUsage, String mode) throws WebClientResponseException {
        return folderQuotaUpdateUsageRequestCreation(name, quotaUsage, mode);
    }
    /**
     * Get active folder quota scans
     * Returns the active folder quota scans
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;FolderQuotaScan&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFoldersQuotaScansRequestCreation() throws WebClientResponseException {
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

        ParameterizedTypeReference<FolderQuotaScan> localVarReturnType = new ParameterizedTypeReference<FolderQuotaScan>() {};
        return apiClient.invokeAPI("/quotas/folders/scans", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get active folder quota scans
     * Returns the active folder quota scans
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;FolderQuotaScan&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<FolderQuotaScan> getFoldersQuotaScans() throws WebClientResponseException {
        ParameterizedTypeReference<FolderQuotaScan> localVarReturnType = new ParameterizedTypeReference<FolderQuotaScan>() {};
        return getFoldersQuotaScansRequestCreation().bodyToFlux(localVarReturnType);
    }

    /**
     * Get active folder quota scans
     * Returns the active folder quota scans
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;List&lt;FolderQuotaScan&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<FolderQuotaScan>>> getFoldersQuotaScansWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<FolderQuotaScan> localVarReturnType = new ParameterizedTypeReference<FolderQuotaScan>() {};
        return getFoldersQuotaScansRequestCreation().toEntityList(localVarReturnType);
    }

    /**
     * Get active folder quota scans
     * Returns the active folder quota scans
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFoldersQuotaScansWithResponseSpec() throws WebClientResponseException {
        return getFoldersQuotaScansRequestCreation();
    }
    /**
     * Get active user quota scans
     * Returns the active user quota scans
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;QuotaScan&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUsersQuotaScansRequestCreation() throws WebClientResponseException {
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

        ParameterizedTypeReference<QuotaScan> localVarReturnType = new ParameterizedTypeReference<QuotaScan>() {};
        return apiClient.invokeAPI("/quotas/users/scans", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get active user quota scans
     * Returns the active user quota scans
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;QuotaScan&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<QuotaScan> getUsersQuotaScans() throws WebClientResponseException {
        ParameterizedTypeReference<QuotaScan> localVarReturnType = new ParameterizedTypeReference<QuotaScan>() {};
        return getUsersQuotaScansRequestCreation().bodyToFlux(localVarReturnType);
    }

    /**
     * Get active user quota scans
     * Returns the active user quota scans
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;List&lt;QuotaScan&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<QuotaScan>>> getUsersQuotaScansWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<QuotaScan> localVarReturnType = new ParameterizedTypeReference<QuotaScan>() {};
        return getUsersQuotaScansRequestCreation().toEntityList(localVarReturnType);
    }

    /**
     * Get active user quota scans
     * Returns the active user quota scans
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUsersQuotaScansWithResponseSpec() throws WebClientResponseException {
        return getUsersQuotaScansRequestCreation();
    }
    /**
     * Start a folder quota scan
     * Starts a new quota scan for the given folder. A quota scan update the number of files and their total size for the specified folder
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec startFolderQuotaScanRequestCreation(String name) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling startFolderQuotaScan", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/quotas/folders/{name}/scan", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Start a folder quota scan
     * Starts a new quota scan for the given folder. A quota scan update the number of files and their total size for the specified folder
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> startFolderQuotaScan(String name) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return startFolderQuotaScanRequestCreation(name).bodyToMono(localVarReturnType);
    }

    /**
     * Start a folder quota scan
     * Starts a new quota scan for the given folder. A quota scan update the number of files and their total size for the specified folder
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> startFolderQuotaScanWithHttpInfo(String name) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return startFolderQuotaScanRequestCreation(name).toEntity(localVarReturnType);
    }

    /**
     * Start a folder quota scan
     * Starts a new quota scan for the given folder. A quota scan update the number of files and their total size for the specified folder
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param name folder name
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec startFolderQuotaScanWithResponseSpec(String name) throws WebClientResponseException {
        return startFolderQuotaScanRequestCreation(name);
    }
    /**
     * Start a user quota scan
     * Starts a new quota scan for the given user. A quota scan updates the number of files and their total size for the specified user and the virtual folders, if any, included in his quota
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec startUserQuotaScanRequestCreation(String username) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling startUserQuotaScan", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("username", username);

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
        return apiClient.invokeAPI("/quotas/users/{username}/scan", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Start a user quota scan
     * Starts a new quota scan for the given user. A quota scan updates the number of files and their total size for the specified user and the virtual folders, if any, included in his quota
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> startUserQuotaScan(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return startUserQuotaScanRequestCreation(username).bodyToMono(localVarReturnType);
    }

    /**
     * Start a user quota scan
     * Starts a new quota scan for the given user. A quota scan updates the number of files and their total size for the specified user and the virtual folders, if any, included in his quota
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> startUserQuotaScanWithHttpInfo(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return startUserQuotaScanRequestCreation(username).toEntity(localVarReturnType);
    }

    /**
     * Start a user quota scan
     * Starts a new quota scan for the given user. A quota scan updates the number of files and their total size for the specified user and the virtual folders, if any, included in his quota
     * <p><b>202</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec startUserQuotaScanWithResponseSpec(String username) throws WebClientResponseException {
        return startUserQuotaScanRequestCreation(username);
    }
    /**
     * Update disk quota usage limits
     * Sets the current used quota limits for the given user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param quotaUsage If used_quota_size and used_quota_files are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec userQuotaUpdateUsageRequestCreation(String username, QuotaUsage quotaUsage, String mode) throws WebClientResponseException {
        Object postBody = quotaUsage;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling userQuotaUpdateUsage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'quotaUsage' is set
        if (quotaUsage == null) {
            throw new WebClientResponseException("Missing the required parameter 'quotaUsage' when calling userQuotaUpdateUsage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("username", username);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "mode", mode));
        
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
        return apiClient.invokeAPI("/quotas/users/{username}/usage", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update disk quota usage limits
     * Sets the current used quota limits for the given user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param quotaUsage If used_quota_size and used_quota_files are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> userQuotaUpdateUsage(String username, QuotaUsage quotaUsage, String mode) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return userQuotaUpdateUsageRequestCreation(username, quotaUsage, mode).bodyToMono(localVarReturnType);
    }

    /**
     * Update disk quota usage limits
     * Sets the current used quota limits for the given user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param quotaUsage If used_quota_size and used_quota_files are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> userQuotaUpdateUsageWithHttpInfo(String username, QuotaUsage quotaUsage, String mode) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return userQuotaUpdateUsageRequestCreation(username, quotaUsage, mode).toEntity(localVarReturnType);
    }

    /**
     * Update disk quota usage limits
     * Sets the current used quota limits for the given user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param quotaUsage If used_quota_size and used_quota_files are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec userQuotaUpdateUsageWithResponseSpec(String username, QuotaUsage quotaUsage, String mode) throws WebClientResponseException {
        return userQuotaUpdateUsageRequestCreation(username, quotaUsage, mode);
    }
    /**
     * Update transfer quota usage limits
     * Sets the current used transfer quota limits for the given user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param transferQuotaUsage If used_upload_data_transfer and used_download_data_transfer are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec userTransferQuotaUpdateUsageRequestCreation(String username, TransferQuotaUsage transferQuotaUsage, String mode) throws WebClientResponseException {
        Object postBody = transferQuotaUsage;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling userTransferQuotaUpdateUsage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'transferQuotaUsage' is set
        if (transferQuotaUsage == null) {
            throw new WebClientResponseException("Missing the required parameter 'transferQuotaUsage' when calling userTransferQuotaUpdateUsage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("username", username);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "mode", mode));
        
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
        return apiClient.invokeAPI("/quotas/users/{username}/transfer-usage", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update transfer quota usage limits
     * Sets the current used transfer quota limits for the given user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param transferQuotaUsage If used_upload_data_transfer and used_download_data_transfer are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> userTransferQuotaUpdateUsage(String username, TransferQuotaUsage transferQuotaUsage, String mode) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return userTransferQuotaUpdateUsageRequestCreation(username, transferQuotaUsage, mode).bodyToMono(localVarReturnType);
    }

    /**
     * Update transfer quota usage limits
     * Sets the current used transfer quota limits for the given user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param transferQuotaUsage If used_upload_data_transfer and used_download_data_transfer are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> userTransferQuotaUpdateUsageWithHttpInfo(String username, TransferQuotaUsage transferQuotaUsage, String mode) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return userTransferQuotaUpdateUsageRequestCreation(username, transferQuotaUsage, mode).toEntity(localVarReturnType);
    }

    /**
     * Update transfer quota usage limits
     * Sets the current used transfer quota limits for the given user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>409</b> - Conflict
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param transferQuotaUsage If used_upload_data_transfer and used_download_data_transfer are missing they will default to 0, this means that if mode is \&quot;add\&quot; the current value, for the missing field, will remain unchanged, if mode is \&quot;reset\&quot; the missing field is set to 0
     * @param mode the update mode specifies if the given quota usage values should be added or replace the current ones
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec userTransferQuotaUpdateUsageWithResponseSpec(String username, TransferQuotaUsage transferQuotaUsage, String mode) throws WebClientResponseException {
        return userTransferQuotaUpdateUsageRequestCreation(username, transferQuotaUsage, mode);
    }
}
