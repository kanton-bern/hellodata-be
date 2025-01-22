package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.DirEntry;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ModelApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class PublicSharesApi {
    private ApiClient apiClient;

    public PublicSharesApi() {
        this(new ApiClient());
    }

    @Autowired
    public PublicSharesApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Download a single file
     * Returns the file contents as response body. The share must have exactly one path defined and it must be a directory for this to work
     * <p><b>200</b> - successful operation
     * <p><b>206</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id     the share id
     * @param path   Path to the file to download. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param inline If set, the response will not have the Content-Disposition header set to &#x60;attachment&#x60;
     * @return File
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<File> downloadShareFile(String id, String path, String inline) throws WebClientResponseException {
        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return downloadShareFileRequestCreation(id, path, inline).bodyToMono(localVarReturnType);
    }

    /**
     * Download a single file
     * Returns the file contents as response body. The share must have exactly one path defined and it must be a directory for this to work
     * <p><b>200</b> - successful operation
     * <p><b>206</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id     the share id
     * @param path   Path to the file to download. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param inline If set, the response will not have the Content-Disposition header set to &#x60;attachment&#x60;
     * @return ResponseEntity&lt;File&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<File>> downloadShareFileWithHttpInfo(String id, String path, String inline) throws WebClientResponseException {
        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return downloadShareFileRequestCreation(id, path, inline).toEntity(localVarReturnType);
    }

    /**
     * Download a single file
     * Returns the file contents as response body. The share must have exactly one path defined and it must be a directory for this to work
     * <p><b>200</b> - successful operation
     * <p><b>206</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id     the share id
     * @param path   Path to the file to download. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param inline If set, the response will not have the Content-Disposition header set to &#x60;attachment&#x60;
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec downloadShareFileWithResponseSpec(String id, String path, String inline) throws WebClientResponseException {
        return downloadShareFileRequestCreation(id, path, inline);
    }

    /**
     * Download shared files and folders as a single zip file
     * A zip file, containing the shared files and folders, will be generated on the fly and returned as response body. Only folders and regular files will be included in the zip. The share must be defined with the read scope and the associated user must have list and download permissions
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id       the share id
     * @param compress The compress parameter
     * @return File
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<File> getShare(String id, Boolean compress) throws WebClientResponseException {
        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return getShareRequestCreation(id, compress).bodyToMono(localVarReturnType);
    }

    /**
     * Download shared files and folders as a single zip file
     * A zip file, containing the shared files and folders, will be generated on the fly and returned as response body. Only folders and regular files will be included in the zip. The share must be defined with the read scope and the associated user must have list and download permissions
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id       the share id
     * @param compress The compress parameter
     * @return ResponseEntity&lt;File&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<File>> getShareWithHttpInfo(String id, Boolean compress) throws WebClientResponseException {
        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return getShareRequestCreation(id, compress).toEntity(localVarReturnType);
    }

    /**
     * Download shared files and folders as a single zip file
     * A zip file, containing the shared files and folders, will be generated on the fly and returned as response body. Only folders and regular files will be included in the zip. The share must be defined with the read scope and the associated user must have list and download permissions
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id       the share id
     * @param compress The compress parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getShareWithResponseSpec(String id, Boolean compress) throws WebClientResponseException {
        return getShareRequestCreation(id, compress);
    }

    /**
     * Read directory contents
     * Returns the contents of the specified directory for the specified share. The share must have exactly one path defined and it must be a directory for this to work
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id   the share id
     * @param path Path to the folder to read. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the user&#39;s start directory is assumed. If relative, the user&#39;s start directory is used as the base
     * @return List&lt;DirEntry&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<DirEntry> getShareDirContents(String id, String path) throws WebClientResponseException {
        ParameterizedTypeReference<DirEntry> localVarReturnType = new ParameterizedTypeReference<DirEntry>() {
        };
        return getShareDirContentsRequestCreation(id, path).bodyToFlux(localVarReturnType);
    }

    /**
     * Read directory contents
     * Returns the contents of the specified directory for the specified share. The share must have exactly one path defined and it must be a directory for this to work
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id   the share id
     * @param path Path to the folder to read. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the user&#39;s start directory is assumed. If relative, the user&#39;s start directory is used as the base
     * @return ResponseEntity&lt;List&lt;DirEntry&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<DirEntry>>> getShareDirContentsWithHttpInfo(String id, String path) throws WebClientResponseException {
        ParameterizedTypeReference<DirEntry> localVarReturnType = new ParameterizedTypeReference<DirEntry>() {
        };
        return getShareDirContentsRequestCreation(id, path).toEntityList(localVarReturnType);
    }

    /**
     * Read directory contents
     * Returns the contents of the specified directory for the specified share. The share must have exactly one path defined and it must be a directory for this to work
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id   the share id
     * @param path Path to the folder to read. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the user&#39;s start directory is assumed. If relative, the user&#39;s start directory is used as the base
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getShareDirContentsWithResponseSpec(String id, String path) throws WebClientResponseException {
        return getShareDirContentsRequestCreation(id, path);
    }

    /**
     * Upload a single file to the shared path
     * The share must be defined with the write scope and the associated user must have the upload/overwrite permissions
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id             the share id
     * @param fileName       the name of the new file. It must be path encoded. Sub directories are not accepted
     * @param body           The body parameter
     * @param X_SFTPGO_MTIME File modification time as unix timestamp in milliseconds
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> uploadSingleToShare(String id, String fileName, File body, Integer X_SFTPGO_MTIME) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return uploadSingleToShareRequestCreation(id, fileName, body, X_SFTPGO_MTIME).bodyToMono(localVarReturnType);
    }

    /**
     * Upload a single file to the shared path
     * The share must be defined with the write scope and the associated user must have the upload/overwrite permissions
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id             the share id
     * @param fileName       the name of the new file. It must be path encoded. Sub directories are not accepted
     * @param body           The body parameter
     * @param X_SFTPGO_MTIME File modification time as unix timestamp in milliseconds
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> uploadSingleToShareWithHttpInfo(String id, String fileName, File body, Integer X_SFTPGO_MTIME) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return uploadSingleToShareRequestCreation(id, fileName, body, X_SFTPGO_MTIME).toEntity(localVarReturnType);
    }

    /**
     * Upload a single file to the shared path
     * The share must be defined with the write scope and the associated user must have the upload/overwrite permissions
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id             the share id
     * @param fileName       the name of the new file. It must be path encoded. Sub directories are not accepted
     * @param body           The body parameter
     * @param X_SFTPGO_MTIME File modification time as unix timestamp in milliseconds
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec uploadSingleToShareWithResponseSpec(String id, String fileName, File body, Integer X_SFTPGO_MTIME) throws WebClientResponseException {
        return uploadSingleToShareRequestCreation(id, fileName, body, X_SFTPGO_MTIME);
    }

    /**
     * Upload one or more files to the shared path
     * The share must be defined with the write scope and the associated user must have the upload permission
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id        the share id
     * @param filenames The filenames parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> uploadToShare(String id, Set<File> filenames) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return uploadToShareRequestCreation(id, filenames).bodyToMono(localVarReturnType);
    }

    /**
     * Upload one or more files to the shared path
     * The share must be defined with the write scope and the associated user must have the upload permission
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id        the share id
     * @param filenames The filenames parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> uploadToShareWithHttpInfo(String id, Set<File> filenames) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return uploadToShareRequestCreation(id, filenames).toEntity(localVarReturnType);
    }

    /**
     * Upload one or more files to the shared path
     * The share must be defined with the write scope and the associated user must have the upload permission
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id        the share id
     * @param filenames The filenames parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec uploadToShareWithResponseSpec(String id, Set<File> filenames) throws WebClientResponseException {
        return uploadToShareRequestCreation(id, filenames);
    }

    /**
     * Download a single file
     * Returns the file contents as response body. The share must have exactly one path defined and it must be a directory for this to work
     * <p><b>200</b> - successful operation
     * <p><b>206</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id     the share id
     * @param path   Path to the file to download. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param inline If set, the response will not have the Content-Disposition header set to &#x60;attachment&#x60;
     * @return File
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec downloadShareFileRequestCreation(String id, String path, String inline) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new WebClientResponseException("Missing the required parameter 'id' when calling downloadShareFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling downloadShareFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("id", id);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "path", path));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "inline", inline));

        final String[] localVarAccepts = {
                "*/*", "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"BasicAuth"};

        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return apiClient.invokeAPI("/shares/{id}/files", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Download shared files and folders as a single zip file
     * A zip file, containing the shared files and folders, will be generated on the fly and returned as response body. Only folders and regular files will be included in the zip. The share must be defined with the read scope and the associated user must have list and download permissions
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id       the share id
     * @param compress The compress parameter
     * @return File
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getShareRequestCreation(String id, Boolean compress) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new WebClientResponseException("Missing the required parameter 'id' when calling getShare", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("id", id);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "compress", compress));

        final String[] localVarAccepts = {
                "*/*", "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"BasicAuth"};

        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return apiClient.invokeAPI("/shares/{id}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Read directory contents
     * Returns the contents of the specified directory for the specified share. The share must have exactly one path defined and it must be a directory for this to work
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id   the share id
     * @param path Path to the folder to read. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the user&#39;s start directory is assumed. If relative, the user&#39;s start directory is used as the base
     * @return List&lt;DirEntry&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getShareDirContentsRequestCreation(String id, String path) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new WebClientResponseException("Missing the required parameter 'id' when calling getShareDirContents", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("id", id);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "path", path));

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"BasicAuth"};

        ParameterizedTypeReference<DirEntry> localVarReturnType = new ParameterizedTypeReference<DirEntry>() {
        };
        return apiClient.invokeAPI("/shares/{id}/dirs", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Upload a single file to the shared path
     * The share must be defined with the write scope and the associated user must have the upload/overwrite permissions
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id             the share id
     * @param fileName       the name of the new file. It must be path encoded. Sub directories are not accepted
     * @param body           The body parameter
     * @param X_SFTPGO_MTIME File modification time as unix timestamp in milliseconds
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec uploadSingleToShareRequestCreation(String id, String fileName, File body, Integer X_SFTPGO_MTIME) throws WebClientResponseException {
        Object postBody = body;
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new WebClientResponseException("Missing the required parameter 'id' when calling uploadSingleToShare", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'fileName' is set
        if (fileName == null) {
            throw new WebClientResponseException("Missing the required parameter 'fileName' when calling uploadSingleToShare", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new WebClientResponseException("Missing the required parameter 'body' when calling uploadSingleToShare", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("id", id);
        pathParams.put("fileName", fileName);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();


        if (X_SFTPGO_MTIME != null)
            headerParams.add("X-SFTPGO-MTIME", apiClient.parameterToString(X_SFTPGO_MTIME));
        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
                "application/*", "text/*", "image/*", "audio/*", "video/*"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"BasicAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/shares/{id}/{fileName}", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Upload one or more files to the shared path
     * The share must be defined with the write scope and the associated user must have the upload permission
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id        the share id
     * @param filenames The filenames parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec uploadToShareRequestCreation(String id, Set<File> filenames) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new WebClientResponseException("Missing the required parameter 'id' when calling uploadToShare", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("id", id);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        if (filenames != null)
            formParams.addAll("filenames", filenames.stream().map(FileSystemResource::new).collect(Collectors.toList()));

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
                "multipart/form-data"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"BasicAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/shares/{id}", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }
}
