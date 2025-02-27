package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.BackupData;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.DumpDataScopes;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.Dumpdata200Response;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ModelApiResponse;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ServicesStatus;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.VersionInfo;

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
public class MaintenanceApi {
    private ApiClient apiClient;

    public MaintenanceApi() {
        this(new ApiClient());
    }

    @Autowired
    public MaintenanceApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Dump data
     * Backups data as data provider independent JSON. The backup can be saved in a local file on the server, to avoid exposing sensitive data over the network, or returned as response body. The output of dumpdata can be used as input for loaddata
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param outputFile Path for the file to write the JSON serialized data to. This path is relative to the configured \&quot;backups_path\&quot;. If this file already exists it will be overwritten. To return the backup as response body set &#x60;output_data&#x60; to true instead.
     * @param outputData output data:   * &#x60;0&#x60; or any other value !&#x3D; 1, the backup will be saved to a file on the server, &#x60;output_file&#x60; is required   * &#x60;1&#x60; the backup will be returned as response body 
     * @param indent indent:   * &#x60;0&#x60; no indentation. This is the default   * &#x60;1&#x60; format the output JSON 
     * @param scopes You can limit the dump contents to the specified scopes. Empty or missing means any supported scope. Scopes must be specified comma separated
     * @return Dumpdata200Response
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec dumpdataRequestCreation(String outputFile, Integer outputData, Integer indent, List<DumpDataScopes> scopes) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "output-file", outputFile));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "output-data", outputData));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "indent", indent));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("csv".toUpperCase(Locale.ROOT)), "scopes", scopes));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<Dumpdata200Response> localVarReturnType = new ParameterizedTypeReference<Dumpdata200Response>() {};
        return apiClient.invokeAPI("/dumpdata", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Dump data
     * Backups data as data provider independent JSON. The backup can be saved in a local file on the server, to avoid exposing sensitive data over the network, or returned as response body. The output of dumpdata can be used as input for loaddata
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param outputFile Path for the file to write the JSON serialized data to. This path is relative to the configured \&quot;backups_path\&quot;. If this file already exists it will be overwritten. To return the backup as response body set &#x60;output_data&#x60; to true instead.
     * @param outputData output data:   * &#x60;0&#x60; or any other value !&#x3D; 1, the backup will be saved to a file on the server, &#x60;output_file&#x60; is required   * &#x60;1&#x60; the backup will be returned as response body 
     * @param indent indent:   * &#x60;0&#x60; no indentation. This is the default   * &#x60;1&#x60; format the output JSON 
     * @param scopes You can limit the dump contents to the specified scopes. Empty or missing means any supported scope. Scopes must be specified comma separated
     * @return Dumpdata200Response
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<Dumpdata200Response> dumpdata(String outputFile, Integer outputData, Integer indent, List<DumpDataScopes> scopes) throws WebClientResponseException {
        ParameterizedTypeReference<Dumpdata200Response> localVarReturnType = new ParameterizedTypeReference<Dumpdata200Response>() {};
        return dumpdataRequestCreation(outputFile, outputData, indent, scopes).bodyToMono(localVarReturnType);
    }

    /**
     * Dump data
     * Backups data as data provider independent JSON. The backup can be saved in a local file on the server, to avoid exposing sensitive data over the network, or returned as response body. The output of dumpdata can be used as input for loaddata
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param outputFile Path for the file to write the JSON serialized data to. This path is relative to the configured \&quot;backups_path\&quot;. If this file already exists it will be overwritten. To return the backup as response body set &#x60;output_data&#x60; to true instead.
     * @param outputData output data:   * &#x60;0&#x60; or any other value !&#x3D; 1, the backup will be saved to a file on the server, &#x60;output_file&#x60; is required   * &#x60;1&#x60; the backup will be returned as response body 
     * @param indent indent:   * &#x60;0&#x60; no indentation. This is the default   * &#x60;1&#x60; format the output JSON 
     * @param scopes You can limit the dump contents to the specified scopes. Empty or missing means any supported scope. Scopes must be specified comma separated
     * @return ResponseEntity&lt;Dumpdata200Response&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<Dumpdata200Response>> dumpdataWithHttpInfo(String outputFile, Integer outputData, Integer indent, List<DumpDataScopes> scopes) throws WebClientResponseException {
        ParameterizedTypeReference<Dumpdata200Response> localVarReturnType = new ParameterizedTypeReference<Dumpdata200Response>() {};
        return dumpdataRequestCreation(outputFile, outputData, indent, scopes).toEntity(localVarReturnType);
    }

    /**
     * Dump data
     * Backups data as data provider independent JSON. The backup can be saved in a local file on the server, to avoid exposing sensitive data over the network, or returned as response body. The output of dumpdata can be used as input for loaddata
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param outputFile Path for the file to write the JSON serialized data to. This path is relative to the configured \&quot;backups_path\&quot;. If this file already exists it will be overwritten. To return the backup as response body set &#x60;output_data&#x60; to true instead.
     * @param outputData output data:   * &#x60;0&#x60; or any other value !&#x3D; 1, the backup will be saved to a file on the server, &#x60;output_file&#x60; is required   * &#x60;1&#x60; the backup will be returned as response body 
     * @param indent indent:   * &#x60;0&#x60; no indentation. This is the default   * &#x60;1&#x60; format the output JSON 
     * @param scopes You can limit the dump contents to the specified scopes. Empty or missing means any supported scope. Scopes must be specified comma separated
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec dumpdataWithResponseSpec(String outputFile, Integer outputData, Integer indent, List<DumpDataScopes> scopes) throws WebClientResponseException {
        return dumpdataRequestCreation(outputFile, outputData, indent, scopes);
    }
    /**
     * Get status
     * Retrieves the status of the active services
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ServicesStatus
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStatusRequestCreation() throws WebClientResponseException {
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

        ParameterizedTypeReference<ServicesStatus> localVarReturnType = new ParameterizedTypeReference<ServicesStatus>() {};
        return apiClient.invokeAPI("/status", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get status
     * Retrieves the status of the active services
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ServicesStatus
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ServicesStatus> getStatus() throws WebClientResponseException {
        ParameterizedTypeReference<ServicesStatus> localVarReturnType = new ParameterizedTypeReference<ServicesStatus>() {};
        return getStatusRequestCreation().bodyToMono(localVarReturnType);
    }

    /**
     * Get status
     * Retrieves the status of the active services
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;ServicesStatus&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ServicesStatus>> getStatusWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<ServicesStatus> localVarReturnType = new ParameterizedTypeReference<ServicesStatus>() {};
        return getStatusRequestCreation().toEntity(localVarReturnType);
    }

    /**
     * Get status
     * Retrieves the status of the active services
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStatusWithResponseSpec() throws WebClientResponseException {
        return getStatusRequestCreation();
    }
    /**
     * Get version details
     * Returns version details such as the version number, build date, commit hash and enabled features
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return VersionInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getVersionRequestCreation() throws WebClientResponseException {
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

        ParameterizedTypeReference<VersionInfo> localVarReturnType = new ParameterizedTypeReference<VersionInfo>() {};
        return apiClient.invokeAPI("/version", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get version details
     * Returns version details such as the version number, build date, commit hash and enabled features
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return VersionInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<VersionInfo> getVersion() throws WebClientResponseException {
        ParameterizedTypeReference<VersionInfo> localVarReturnType = new ParameterizedTypeReference<VersionInfo>() {};
        return getVersionRequestCreation().bodyToMono(localVarReturnType);
    }

    /**
     * Get version details
     * Returns version details such as the version number, build date, commit hash and enabled features
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;VersionInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<VersionInfo>> getVersionWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<VersionInfo> localVarReturnType = new ParameterizedTypeReference<VersionInfo>() {};
        return getVersionRequestCreation().toEntity(localVarReturnType);
    }

    /**
     * Get version details
     * Returns version details such as the version number, build date, commit hash and enabled features
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getVersionWithResponseSpec() throws WebClientResponseException {
        return getVersionRequestCreation();
    }
    /**
     * Load data from path
     * Restores SFTPGo data from a JSON backup file on the server. Objects will be restored one by one and the restore is stopped if a object cannot be added or updated, so it could happen a partial restore
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param inputFile Path for the file to read the JSON serialized data from. This can be an absolute path or a path relative to the configured \&quot;backups_path\&quot;. The max allowed file size is 10MB
     * @param scanQuota Quota scan:   * &#x60;0&#x60; no quota scan is done, the imported users/folders will have used_quota_size and used_quota_files &#x3D; 0 or the existing values if they already exists. This is the default   * &#x60;1&#x60; scan quota   * &#x60;2&#x60; scan quota if the user has quota restrictions required: false 
     * @param mode Mode:   * &#x60;0&#x60; New objects are added, existing ones are updated. This is the default   * &#x60;1&#x60; New objects are added, existing ones are not modified   * &#x60;2&#x60; New objects are added, existing ones are updated and connected users are disconnected and so forced to use the new configuration 
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec loaddataFromFileRequestCreation(String inputFile, Integer scanQuota, Integer mode) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'inputFile' is set
        if (inputFile == null) {
            throw new WebClientResponseException("Missing the required parameter 'inputFile' when calling loaddataFromFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "scan-quota", scanQuota));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "mode", mode));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "input-file", inputFile));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/loaddata", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Load data from path
     * Restores SFTPGo data from a JSON backup file on the server. Objects will be restored one by one and the restore is stopped if a object cannot be added or updated, so it could happen a partial restore
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param inputFile Path for the file to read the JSON serialized data from. This can be an absolute path or a path relative to the configured \&quot;backups_path\&quot;. The max allowed file size is 10MB
     * @param scanQuota Quota scan:   * &#x60;0&#x60; no quota scan is done, the imported users/folders will have used_quota_size and used_quota_files &#x3D; 0 or the existing values if they already exists. This is the default   * &#x60;1&#x60; scan quota   * &#x60;2&#x60; scan quota if the user has quota restrictions required: false 
     * @param mode Mode:   * &#x60;0&#x60; New objects are added, existing ones are updated. This is the default   * &#x60;1&#x60; New objects are added, existing ones are not modified   * &#x60;2&#x60; New objects are added, existing ones are updated and connected users are disconnected and so forced to use the new configuration 
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> loaddataFromFile(String inputFile, Integer scanQuota, Integer mode) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return loaddataFromFileRequestCreation(inputFile, scanQuota, mode).bodyToMono(localVarReturnType);
    }

    /**
     * Load data from path
     * Restores SFTPGo data from a JSON backup file on the server. Objects will be restored one by one and the restore is stopped if a object cannot be added or updated, so it could happen a partial restore
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param inputFile Path for the file to read the JSON serialized data from. This can be an absolute path or a path relative to the configured \&quot;backups_path\&quot;. The max allowed file size is 10MB
     * @param scanQuota Quota scan:   * &#x60;0&#x60; no quota scan is done, the imported users/folders will have used_quota_size and used_quota_files &#x3D; 0 or the existing values if they already exists. This is the default   * &#x60;1&#x60; scan quota   * &#x60;2&#x60; scan quota if the user has quota restrictions required: false 
     * @param mode Mode:   * &#x60;0&#x60; New objects are added, existing ones are updated. This is the default   * &#x60;1&#x60; New objects are added, existing ones are not modified   * &#x60;2&#x60; New objects are added, existing ones are updated and connected users are disconnected and so forced to use the new configuration 
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> loaddataFromFileWithHttpInfo(String inputFile, Integer scanQuota, Integer mode) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return loaddataFromFileRequestCreation(inputFile, scanQuota, mode).toEntity(localVarReturnType);
    }

    /**
     * Load data from path
     * Restores SFTPGo data from a JSON backup file on the server. Objects will be restored one by one and the restore is stopped if a object cannot be added or updated, so it could happen a partial restore
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param inputFile Path for the file to read the JSON serialized data from. This can be an absolute path or a path relative to the configured \&quot;backups_path\&quot;. The max allowed file size is 10MB
     * @param scanQuota Quota scan:   * &#x60;0&#x60; no quota scan is done, the imported users/folders will have used_quota_size and used_quota_files &#x3D; 0 or the existing values if they already exists. This is the default   * &#x60;1&#x60; scan quota   * &#x60;2&#x60; scan quota if the user has quota restrictions required: false 
     * @param mode Mode:   * &#x60;0&#x60; New objects are added, existing ones are updated. This is the default   * &#x60;1&#x60; New objects are added, existing ones are not modified   * &#x60;2&#x60; New objects are added, existing ones are updated and connected users are disconnected and so forced to use the new configuration 
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec loaddataFromFileWithResponseSpec(String inputFile, Integer scanQuota, Integer mode) throws WebClientResponseException {
        return loaddataFromFileRequestCreation(inputFile, scanQuota, mode);
    }
    /**
     * Load data
     * Restores SFTPGo data from a JSON backup. Objects will be restored one by one and the restore is stopped if a object cannot be added or updated, so it could happen a partial restore
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param backupData The backupData parameter
     * @param scanQuota Quota scan:   * &#x60;0&#x60; no quota scan is done, the imported users/folders will have used_quota_size and used_quota_files &#x3D; 0 or the existing values if they already exists. This is the default   * &#x60;1&#x60; scan quota   * &#x60;2&#x60; scan quota if the user has quota restrictions required: false 
     * @param mode Mode:   * &#x60;0&#x60; New objects are added, existing ones are updated. This is the default   * &#x60;1&#x60; New objects are added, existing ones are not modified   * &#x60;2&#x60; New objects are added, existing ones are updated and connected users are disconnected and so forced to use the new configuration 
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec loaddataFromRequestBodyRequestCreation(BackupData backupData, Integer scanQuota, Integer mode) throws WebClientResponseException {
        Object postBody = backupData;
        // verify the required parameter 'backupData' is set
        if (backupData == null) {
            throw new WebClientResponseException("Missing the required parameter 'backupData' when calling loaddataFromRequestBody", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "scan-quota", scanQuota));
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
        return apiClient.invokeAPI("/loaddata", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Load data
     * Restores SFTPGo data from a JSON backup. Objects will be restored one by one and the restore is stopped if a object cannot be added or updated, so it could happen a partial restore
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param backupData The backupData parameter
     * @param scanQuota Quota scan:   * &#x60;0&#x60; no quota scan is done, the imported users/folders will have used_quota_size and used_quota_files &#x3D; 0 or the existing values if they already exists. This is the default   * &#x60;1&#x60; scan quota   * &#x60;2&#x60; scan quota if the user has quota restrictions required: false 
     * @param mode Mode:   * &#x60;0&#x60; New objects are added, existing ones are updated. This is the default   * &#x60;1&#x60; New objects are added, existing ones are not modified   * &#x60;2&#x60; New objects are added, existing ones are updated and connected users are disconnected and so forced to use the new configuration 
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> loaddataFromRequestBody(BackupData backupData, Integer scanQuota, Integer mode) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return loaddataFromRequestBodyRequestCreation(backupData, scanQuota, mode).bodyToMono(localVarReturnType);
    }

    /**
     * Load data
     * Restores SFTPGo data from a JSON backup. Objects will be restored one by one and the restore is stopped if a object cannot be added or updated, so it could happen a partial restore
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param backupData The backupData parameter
     * @param scanQuota Quota scan:   * &#x60;0&#x60; no quota scan is done, the imported users/folders will have used_quota_size and used_quota_files &#x3D; 0 or the existing values if they already exists. This is the default   * &#x60;1&#x60; scan quota   * &#x60;2&#x60; scan quota if the user has quota restrictions required: false 
     * @param mode Mode:   * &#x60;0&#x60; New objects are added, existing ones are updated. This is the default   * &#x60;1&#x60; New objects are added, existing ones are not modified   * &#x60;2&#x60; New objects are added, existing ones are updated and connected users are disconnected and so forced to use the new configuration 
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> loaddataFromRequestBodyWithHttpInfo(BackupData backupData, Integer scanQuota, Integer mode) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return loaddataFromRequestBodyRequestCreation(backupData, scanQuota, mode).toEntity(localVarReturnType);
    }

    /**
     * Load data
     * Restores SFTPGo data from a JSON backup. Objects will be restored one by one and the restore is stopped if a object cannot be added or updated, so it could happen a partial restore
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param backupData The backupData parameter
     * @param scanQuota Quota scan:   * &#x60;0&#x60; no quota scan is done, the imported users/folders will have used_quota_size and used_quota_files &#x3D; 0 or the existing values if they already exists. This is the default   * &#x60;1&#x60; scan quota   * &#x60;2&#x60; scan quota if the user has quota restrictions required: false 
     * @param mode Mode:   * &#x60;0&#x60; New objects are added, existing ones are updated. This is the default   * &#x60;1&#x60; New objects are added, existing ones are not modified   * &#x60;2&#x60; New objects are added, existing ones are updated and connected users are disconnected and so forced to use the new configuration 
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec loaddataFromRequestBodyWithResponseSpec(BackupData backupData, Integer scanQuota, Integer mode) throws WebClientResponseException {
        return loaddataFromRequestBodyRequestCreation(backupData, scanQuota, mode);
    }
}
