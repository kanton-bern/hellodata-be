package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.*;
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
public class UserApisApi {
    private ApiClient apiClient;

    public UserApisApi() {
        this(new ApiClient());
    }

    @Autowired
    public UserApisApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Add a share
     * Adds a new share. The share id will be auto-generated
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param share The share parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> addShare(Share share) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return addShareRequestCreation(share).bodyToMono(localVarReturnType);
    }

    /**
     * Add a share
     * Adds a new share. The share id will be auto-generated
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param share The share parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> addShareWithHttpInfo(Share share) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return addShareRequestCreation(share).toEntity(localVarReturnType);
    }

    /**
     * Add a share
     * Adds a new share. The share id will be auto-generated
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param share The share parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addShareWithResponseSpec(Share share) throws WebClientResponseException {
        return addShareRequestCreation(share);
    }

    /**
     * Change user password
     * Changes the password for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param pwdChange The pwdChange parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> changeUserPassword(PwdChange pwdChange) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return changeUserPasswordRequestCreation(pwdChange).bodyToMono(localVarReturnType);
    }

    /**
     * Change user password
     * Changes the password for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param pwdChange The pwdChange parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> changeUserPasswordWithHttpInfo(PwdChange pwdChange) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return changeUserPasswordRequestCreation(pwdChange).toEntity(localVarReturnType);
    }

    /**
     * Change user password
     * Changes the password for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param pwdChange The pwdChange parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec changeUserPasswordWithResponseSpec(PwdChange pwdChange) throws WebClientResponseException {
        return changeUserPasswordRequestCreation(pwdChange);
    }

    /**
     * Create a directory
     * Create a directory for the logged in user
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path         Path to the folder to create. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param mkdirParents Create parent directories if they do not exist?
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> createUserDir(String path, Boolean mkdirParents) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return createUserDirRequestCreation(path, mkdirParents).bodyToMono(localVarReturnType);
    }

    /**
     * Create a directory
     * Create a directory for the logged in user
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path         Path to the folder to create. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param mkdirParents Create parent directories if they do not exist?
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> createUserDirWithHttpInfo(String path, Boolean mkdirParents) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return createUserDirRequestCreation(path, mkdirParents).toEntity(localVarReturnType);
    }

    /**
     * Create a directory
     * Create a directory for the logged in user
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path         Path to the folder to create. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param mkdirParents Create parent directories if they do not exist?
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec createUserDirWithResponseSpec(String path, Boolean mkdirParents) throws WebClientResponseException {
        return createUserDirRequestCreation(path, mkdirParents);
    }

    /**
     * Upload a single file
     * Upload a single file for the logged in user to an existing directory. This API does not use multipart/form-data and so no temporary files are created server side but only a single file can be uploaded as POST body
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path           Full file path. It must be path encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;. The parent directory must exist. If a file with the same name already exists, it will be overwritten
     * @param body           The body parameter
     * @param mkdirParents   Create parent directories if they do not exist?
     * @param X_SFTPGO_MTIME File modification time as unix timestamp in milliseconds
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> createUserFile(String path, File body, Boolean mkdirParents, Integer X_SFTPGO_MTIME) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return createUserFileRequestCreation(path, body, mkdirParents, X_SFTPGO_MTIME).bodyToMono(localVarReturnType);
    }

    /**
     * Upload a single file
     * Upload a single file for the logged in user to an existing directory. This API does not use multipart/form-data and so no temporary files are created server side but only a single file can be uploaded as POST body
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path           Full file path. It must be path encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;. The parent directory must exist. If a file with the same name already exists, it will be overwritten
     * @param body           The body parameter
     * @param mkdirParents   Create parent directories if they do not exist?
     * @param X_SFTPGO_MTIME File modification time as unix timestamp in milliseconds
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> createUserFileWithHttpInfo(String path, File body, Boolean mkdirParents, Integer X_SFTPGO_MTIME) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return createUserFileRequestCreation(path, body, mkdirParents, X_SFTPGO_MTIME).toEntity(localVarReturnType);
    }

    /**
     * Upload a single file
     * Upload a single file for the logged in user to an existing directory. This API does not use multipart/form-data and so no temporary files are created server side but only a single file can be uploaded as POST body
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path           Full file path. It must be path encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;. The parent directory must exist. If a file with the same name already exists, it will be overwritten
     * @param body           The body parameter
     * @param mkdirParents   Create parent directories if they do not exist?
     * @param X_SFTPGO_MTIME File modification time as unix timestamp in milliseconds
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec createUserFileWithResponseSpec(String path, File body, Boolean mkdirParents, Integer X_SFTPGO_MTIME) throws WebClientResponseException {
        return createUserFileRequestCreation(path, body, mkdirParents, X_SFTPGO_MTIME);
    }

    /**
     * Upload files
     * Upload one or more files for the logged in user
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path         Parent directory for the uploaded files. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the root path is assumed. If a file with the same name already exists, it will be overwritten
     * @param mkdirParents Create parent directories if they do not exist?
     * @param filenames    The filenames parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> createUserFiles(String path, Boolean mkdirParents, Set<File> filenames) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return createUserFilesRequestCreation(path, mkdirParents, filenames).bodyToMono(localVarReturnType);
    }

    /**
     * Upload files
     * Upload one or more files for the logged in user
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path         Parent directory for the uploaded files. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the root path is assumed. If a file with the same name already exists, it will be overwritten
     * @param mkdirParents Create parent directories if they do not exist?
     * @param filenames    The filenames parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> createUserFilesWithHttpInfo(String path, Boolean mkdirParents, Set<File> filenames) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return createUserFilesRequestCreation(path, mkdirParents, filenames).toEntity(localVarReturnType);
    }

    /**
     * Upload files
     * Upload one or more files for the logged in user
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path         Parent directory for the uploaded files. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the root path is assumed. If a file with the same name already exists, it will be overwritten
     * @param mkdirParents Create parent directories if they do not exist?
     * @param filenames    The filenames parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec createUserFilesWithResponseSpec(String path, Boolean mkdirParents, Set<File> filenames) throws WebClientResponseException {
        return createUserFilesRequestCreation(path, mkdirParents, filenames);
    }

    /**
     * Delete a directory
     * Delete a directory and any children it contains for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the folder to delete. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> deleteUserDir(String path) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return deleteUserDirRequestCreation(path).bodyToMono(localVarReturnType);
    }

    /**
     * Delete a directory
     * Delete a directory and any children it contains for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the folder to delete. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> deleteUserDirWithHttpInfo(String path) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return deleteUserDirRequestCreation(path).toEntity(localVarReturnType);
    }

    /**
     * Delete a directory
     * Delete a directory and any children it contains for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the folder to delete. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteUserDirWithResponseSpec(String path) throws WebClientResponseException {
        return deleteUserDirRequestCreation(path);
    }

    /**
     * Delete a file
     * Delete a file for the logged in user.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the file to delete. It must be URL encoded
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> deleteUserFile(String path) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return deleteUserFileRequestCreation(path).bodyToMono(localVarReturnType);
    }

    /**
     * Delete a file
     * Delete a file for the logged in user.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the file to delete. It must be URL encoded
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> deleteUserFileWithHttpInfo(String path) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return deleteUserFileRequestCreation(path).toEntity(localVarReturnType);
    }

    /**
     * Delete a file
     * Delete a file for the logged in user.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the file to delete. It must be URL encoded
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteUserFileWithResponseSpec(String path) throws WebClientResponseException {
        return deleteUserFileRequestCreation(path);
    }

    /**
     * Delete share
     * Deletes an existing share belonging to the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id the share id
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> deleteUserShare(String id) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return deleteUserShareRequestCreation(id).bodyToMono(localVarReturnType);
    }

    /**
     * Delete share
     * Deletes an existing share belonging to the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id the share id
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> deleteUserShareWithHttpInfo(String id) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return deleteUserShareRequestCreation(id).toEntity(localVarReturnType);
    }

    /**
     * Delete share
     * Deletes an existing share belonging to the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id the share id
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteUserShareWithResponseSpec(String id) throws WebClientResponseException {
        return deleteUserShareRequestCreation(id);
    }

    /**
     * Download a single file
     * Returns the file contents as response body
     * <p><b>200</b> - successful operation
     * <p><b>206</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file to download. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param inline If set, the response will not have the Content-Disposition header set to &#x60;attachment&#x60;
     * @return File
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<File> downloadUserFile(String path, String inline) throws WebClientResponseException {
        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return downloadUserFileRequestCreation(path, inline).bodyToMono(localVarReturnType);
    }

    /**
     * Download a single file
     * Returns the file contents as response body
     * <p><b>200</b> - successful operation
     * <p><b>206</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file to download. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param inline If set, the response will not have the Content-Disposition header set to &#x60;attachment&#x60;
     * @return ResponseEntity&lt;File&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<File>> downloadUserFileWithHttpInfo(String path, String inline) throws WebClientResponseException {
        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return downloadUserFileRequestCreation(path, inline).toEntity(localVarReturnType);
    }

    /**
     * Download a single file
     * Returns the file contents as response body
     * <p><b>200</b> - successful operation
     * <p><b>206</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file to download. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param inline If set, the response will not have the Content-Disposition header set to &#x60;attachment&#x60;
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec downloadUserFileWithResponseSpec(String path, String inline) throws WebClientResponseException {
        return downloadUserFileRequestCreation(path, inline);
    }

    /**
     * Generate recovery codes
     * Generates new recovery codes for the logged in user. Generating new recovery codes you automatically invalidate old ones
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return List&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<List<String>> generateUserRecoveryCodes() throws WebClientResponseException {
        ParameterizedTypeReference<List<String>> localVarReturnType = new ParameterizedTypeReference<List<String>>() {
        };
        return generateUserRecoveryCodesRequestCreation().bodyToMono(localVarReturnType);
    }

    /**
     * Generate recovery codes
     * Generates new recovery codes for the logged in user. Generating new recovery codes you automatically invalidate old ones
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return ResponseEntity&lt;List&lt;String&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<String>>> generateUserRecoveryCodesWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<List<String>> localVarReturnType = new ParameterizedTypeReference<List<String>>() {
        };
        return generateUserRecoveryCodesRequestCreation().toEntity(localVarReturnType);
    }

    /**
     * Generate recovery codes
     * Generates new recovery codes for the logged in user. Generating new recovery codes you automatically invalidate old ones
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec generateUserRecoveryCodesWithResponseSpec() throws WebClientResponseException {
        return generateUserRecoveryCodesRequestCreation();
    }

    /**
     * Generate a new TOTP secret
     * Generates a new TOTP secret, including the QR code as png, using the specified configuration for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param generateAdminTotpSecretRequest The generateAdminTotpSecretRequest parameter
     * @return GenerateUserTotpSecret200Response
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<GenerateUserTotpSecret200Response> generateUserTotpSecret(GenerateAdminTotpSecretRequest generateAdminTotpSecretRequest) throws WebClientResponseException {
        ParameterizedTypeReference<GenerateUserTotpSecret200Response> localVarReturnType = new ParameterizedTypeReference<GenerateUserTotpSecret200Response>() {
        };
        return generateUserTotpSecretRequestCreation(generateAdminTotpSecretRequest).bodyToMono(localVarReturnType);
    }

    /**
     * Generate a new TOTP secret
     * Generates a new TOTP secret, including the QR code as png, using the specified configuration for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param generateAdminTotpSecretRequest The generateAdminTotpSecretRequest parameter
     * @return ResponseEntity&lt;GenerateUserTotpSecret200Response&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<GenerateUserTotpSecret200Response>> generateUserTotpSecretWithHttpInfo(GenerateAdminTotpSecretRequest generateAdminTotpSecretRequest) throws WebClientResponseException {
        ParameterizedTypeReference<GenerateUserTotpSecret200Response> localVarReturnType = new ParameterizedTypeReference<GenerateUserTotpSecret200Response>() {
        };
        return generateUserTotpSecretRequestCreation(generateAdminTotpSecretRequest).toEntity(localVarReturnType);
    }

    /**
     * Generate a new TOTP secret
     * Generates a new TOTP secret, including the QR code as png, using the specified configuration for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param generateAdminTotpSecretRequest The generateAdminTotpSecretRequest parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec generateUserTotpSecretWithResponseSpec(GenerateAdminTotpSecretRequest generateAdminTotpSecretRequest) throws WebClientResponseException {
        return generateUserTotpSecretRequestCreation(generateAdminTotpSecretRequest);
    }

    /**
     * Read directory contents
     * Returns the contents of the specified directory for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the folder to read. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the user&#39;s start directory is assumed. If relative, the user&#39;s start directory is used as the base
     * @return List&lt;DirEntry&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<DirEntry> getUserDirContents(String path) throws WebClientResponseException {
        ParameterizedTypeReference<DirEntry> localVarReturnType = new ParameterizedTypeReference<DirEntry>() {
        };
        return getUserDirContentsRequestCreation(path).bodyToFlux(localVarReturnType);
    }

    /**
     * Read directory contents
     * Returns the contents of the specified directory for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the folder to read. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the user&#39;s start directory is assumed. If relative, the user&#39;s start directory is used as the base
     * @return ResponseEntity&lt;List&lt;DirEntry&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<DirEntry>>> getUserDirContentsWithHttpInfo(String path) throws WebClientResponseException {
        ParameterizedTypeReference<DirEntry> localVarReturnType = new ParameterizedTypeReference<DirEntry>() {
        };
        return getUserDirContentsRequestCreation(path).toEntityList(localVarReturnType);
    }

    /**
     * Read directory contents
     * Returns the contents of the specified directory for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the folder to read. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the user&#39;s start directory is assumed. If relative, the user&#39;s start directory is used as the base
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUserDirContentsWithResponseSpec(String path) throws WebClientResponseException {
        return getUserDirContentsRequestCreation(path);
    }

    /**
     * Get user profile
     * Returns the profile for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return UserProfile
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<UserProfile> getUserProfile() throws WebClientResponseException {
        ParameterizedTypeReference<UserProfile> localVarReturnType = new ParameterizedTypeReference<UserProfile>() {
        };
        return getUserProfileRequestCreation().bodyToMono(localVarReturnType);
    }

    /**
     * Get user profile
     * Returns the profile for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return ResponseEntity&lt;UserProfile&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<UserProfile>> getUserProfileWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<UserProfile> localVarReturnType = new ParameterizedTypeReference<UserProfile>() {
        };
        return getUserProfileRequestCreation().toEntity(localVarReturnType);
    }

    /**
     * Get user profile
     * Returns the profile for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUserProfileWithResponseSpec() throws WebClientResponseException {
        return getUserProfileRequestCreation();
    }

    /**
     * Get recovery codes
     * Returns the recovery codes for the logged in user. Recovery codes can be used if the user loses access to their second factor auth device. Recovery codes are returned unencrypted
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return List&lt;RecoveryCode&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<RecoveryCode> getUserRecoveryCodes() throws WebClientResponseException {
        ParameterizedTypeReference<RecoveryCode> localVarReturnType = new ParameterizedTypeReference<RecoveryCode>() {
        };
        return getUserRecoveryCodesRequestCreation().bodyToFlux(localVarReturnType);
    }

    /**
     * Get recovery codes
     * Returns the recovery codes for the logged in user. Recovery codes can be used if the user loses access to their second factor auth device. Recovery codes are returned unencrypted
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return ResponseEntity&lt;List&lt;RecoveryCode&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<RecoveryCode>>> getUserRecoveryCodesWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<RecoveryCode> localVarReturnType = new ParameterizedTypeReference<RecoveryCode>() {
        };
        return getUserRecoveryCodesRequestCreation().toEntityList(localVarReturnType);
    }

    /**
     * Get recovery codes
     * Returns the recovery codes for the logged in user. Recovery codes can be used if the user loses access to their second factor auth device. Recovery codes are returned unencrypted
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUserRecoveryCodesWithResponseSpec() throws WebClientResponseException {
        return getUserRecoveryCodesRequestCreation();
    }

    /**
     * Get share by id
     * Returns a share by id for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id the share id
     * @return Share
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<Share> getUserShareById(String id) throws WebClientResponseException {
        ParameterizedTypeReference<Share> localVarReturnType = new ParameterizedTypeReference<Share>() {
        };
        return getUserShareByIdRequestCreation(id).bodyToMono(localVarReturnType);
    }

    /**
     * Get share by id
     * Returns a share by id for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id the share id
     * @return ResponseEntity&lt;Share&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<Share>> getUserShareByIdWithHttpInfo(String id) throws WebClientResponseException {
        ParameterizedTypeReference<Share> localVarReturnType = new ParameterizedTypeReference<Share>() {
        };
        return getUserShareByIdRequestCreation(id).toEntity(localVarReturnType);
    }

    /**
     * Get share by id
     * Returns a share by id for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id the share id
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUserShareByIdWithResponseSpec(String id) throws WebClientResponseException {
        return getUserShareByIdRequestCreation(id);
    }

    /**
     * List user shares
     * Returns the share for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param offset The offset parameter
     * @param limit  The maximum number of items to return. Max value is 500, default is 100
     * @param order  Ordering shares by ID. Default ASC
     * @return List&lt;Share&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<Share> getUserShares(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<Share> localVarReturnType = new ParameterizedTypeReference<Share>() {
        };
        return getUserSharesRequestCreation(offset, limit, order).bodyToFlux(localVarReturnType);
    }

    /**
     * List user shares
     * Returns the share for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param offset The offset parameter
     * @param limit  The maximum number of items to return. Max value is 500, default is 100
     * @param order  Ordering shares by ID. Default ASC
     * @return ResponseEntity&lt;List&lt;Share&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<Share>>> getUserSharesWithHttpInfo(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<Share> localVarReturnType = new ParameterizedTypeReference<Share>() {
        };
        return getUserSharesRequestCreation(offset, limit, order).toEntityList(localVarReturnType);
    }

    /**
     * List user shares
     * Returns the share for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param offset The offset parameter
     * @param limit  The maximum number of items to return. Max value is 500, default is 100
     * @param order  Ordering shares by ID. Default ASC
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUserSharesWithResponseSpec(Integer offset, Integer limit, String order) throws WebClientResponseException {
        return getUserSharesRequestCreation(offset, limit, order);
    }

    /**
     * Get available TOTP configuration
     * Returns the available TOTP configurations for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return List&lt;TOTPConfig&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<TOTPConfig> getUserTotpConfigs() throws WebClientResponseException {
        ParameterizedTypeReference<TOTPConfig> localVarReturnType = new ParameterizedTypeReference<TOTPConfig>() {
        };
        return getUserTotpConfigsRequestCreation().bodyToFlux(localVarReturnType);
    }

    /**
     * Get available TOTP configuration
     * Returns the available TOTP configurations for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return ResponseEntity&lt;List&lt;TOTPConfig&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<TOTPConfig>>> getUserTotpConfigsWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<TOTPConfig> localVarReturnType = new ParameterizedTypeReference<TOTPConfig>() {
        };
        return getUserTotpConfigsRequestCreation().toEntityList(localVarReturnType);
    }

    /**
     * Get available TOTP configuration
     * Returns the available TOTP configurations for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUserTotpConfigsWithResponseSpec() throws WebClientResponseException {
        return getUserTotpConfigsRequestCreation();
    }

    /**
     * Rename a directory. Deprecated, use \&quot;file-actions/move\&quot;
     * Rename a directory for the logged in user. The rename is allowed for empty directory or for non empty local directories, with no virtual folders inside
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the folder to rename. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> renameUserDir(String path, String target) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return renameUserDirRequestCreation(path, target).bodyToMono(localVarReturnType);
    }

    /**
     * Rename a directory. Deprecated, use \&quot;file-actions/move\&quot;
     * Rename a directory for the logged in user. The rename is allowed for empty directory or for non empty local directories, with no virtual folders inside
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the folder to rename. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> renameUserDirWithHttpInfo(String path, String target) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return renameUserDirRequestCreation(path, target).toEntity(localVarReturnType);
    }

    /**
     * Rename a directory. Deprecated, use \&quot;file-actions/move\&quot;
     * Rename a directory for the logged in user. The rename is allowed for empty directory or for non empty local directories, with no virtual folders inside
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the folder to rename. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec renameUserDirWithResponseSpec(String path, String target) throws WebClientResponseException {
        return renameUserDirRequestCreation(path, target);
    }

    /**
     * Rename a file
     * Rename a file for the logged in user. Deprecated, use \&quot;file-actions/move\&quot;
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file to rename. It must be URL encoded
     * @param target New name. It must be URL encoded
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> renameUserFile(String path, String target) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return renameUserFileRequestCreation(path, target).bodyToMono(localVarReturnType);
    }

    /**
     * Rename a file
     * Rename a file for the logged in user. Deprecated, use \&quot;file-actions/move\&quot;
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file to rename. It must be URL encoded
     * @param target New name. It must be URL encoded
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> renameUserFileWithHttpInfo(String path, String target) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return renameUserFileRequestCreation(path, target).toEntity(localVarReturnType);
    }

    /**
     * Rename a file
     * Rename a file for the logged in user. Deprecated, use \&quot;file-actions/move\&quot;
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file to rename. It must be URL encoded
     * @param target New name. It must be URL encoded
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec renameUserFileWithResponseSpec(String path, String target) throws WebClientResponseException {
        return renameUserFileRequestCreation(path, target);
    }

    /**
     * Save a TOTP config
     * Saves the specified TOTP config for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param userTOTPConfig The userTOTPConfig parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> saveUserTotpConfig(UserTOTPConfig userTOTPConfig) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return saveUserTotpConfigRequestCreation(userTOTPConfig).bodyToMono(localVarReturnType);
    }

    /**
     * Save a TOTP config
     * Saves the specified TOTP config for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param userTOTPConfig The userTOTPConfig parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> saveUserTotpConfigWithHttpInfo(UserTOTPConfig userTOTPConfig) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return saveUserTotpConfigRequestCreation(userTOTPConfig).toEntity(localVarReturnType);
    }

    /**
     * Save a TOTP config
     * Saves the specified TOTP config for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param userTOTPConfig The userTOTPConfig parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec saveUserTotpConfigWithResponseSpec(UserTOTPConfig userTOTPConfig) throws WebClientResponseException {
        return saveUserTotpConfigRequestCreation(userTOTPConfig);
    }

    /**
     * Set metadata for a file/directory
     * Set supported metadata attributes for the specified file or directory
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path                    Full file/directory path. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param setpropsUserFileRequest The setpropsUserFileRequest parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> setpropsUserFile(String path, SetpropsUserFileRequest setpropsUserFileRequest) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return setpropsUserFileRequestCreation(path, setpropsUserFileRequest).bodyToMono(localVarReturnType);
    }

    /**
     * Set metadata for a file/directory
     * Set supported metadata attributes for the specified file or directory
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path                    Full file/directory path. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param setpropsUserFileRequest The setpropsUserFileRequest parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> setpropsUserFileWithHttpInfo(String path, SetpropsUserFileRequest setpropsUserFileRequest) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return setpropsUserFileRequestCreation(path, setpropsUserFileRequest).toEntity(localVarReturnType);
    }

    /**
     * Set metadata for a file/directory
     * Set supported metadata attributes for the specified file or directory
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path                    Full file/directory path. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param setpropsUserFileRequest The setpropsUserFileRequest parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec setpropsUserFileWithResponseSpec(String path, SetpropsUserFileRequest setpropsUserFileRequest) throws WebClientResponseException {
        return setpropsUserFileRequestCreation(path, setpropsUserFileRequest);
    }

    /**
     * Download multiple files and folders as a single zip file
     * A zip file, containing the specified files and folders, will be generated on the fly and returned as response body. Only folders and regular files will be included in the zip
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param requestBody The requestBody parameter
     * @return File
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<File> streamzip(List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return streamzipRequestCreation(requestBody).bodyToMono(localVarReturnType);
    }

    /**
     * Download multiple files and folders as a single zip file
     * A zip file, containing the specified files and folders, will be generated on the fly and returned as response body. Only folders and regular files will be included in the zip
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param requestBody The requestBody parameter
     * @return ResponseEntity&lt;File&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<File>> streamzipWithHttpInfo(List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return streamzipRequestCreation(requestBody).toEntity(localVarReturnType);
    }

    /**
     * Download multiple files and folders as a single zip file
     * A zip file, containing the specified files and folders, will be generated on the fly and returned as response body. Only folders and regular files will be included in the zip
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param requestBody The requestBody parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec streamzipWithResponseSpec(List<String> requestBody) throws WebClientResponseException {
        return streamzipRequestCreation(requestBody);
    }

    /**
     * Update user profile
     * Allows to update the profile for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param userProfile The userProfile parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> updateUserProfile(UserProfile userProfile) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return updateUserProfileRequestCreation(userProfile).bodyToMono(localVarReturnType);
    }

    /**
     * Update user profile
     * Allows to update the profile for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param userProfile The userProfile parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> updateUserProfileWithHttpInfo(UserProfile userProfile) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return updateUserProfileRequestCreation(userProfile).toEntity(localVarReturnType);
    }

    /**
     * Update user profile
     * Allows to update the profile for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param userProfile The userProfile parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateUserProfileWithResponseSpec(UserProfile userProfile) throws WebClientResponseException {
        return updateUserProfileRequestCreation(userProfile);
    }

    /**
     * Update share
     * Updates an existing share belonging to the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id    the share id
     * @param share The share parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> updateUserShare(String id, Share share) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return updateUserShareRequestCreation(id, share).bodyToMono(localVarReturnType);
    }

    /**
     * Update share
     * Updates an existing share belonging to the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id    the share id
     * @param share The share parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> updateUserShareWithHttpInfo(String id, Share share) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return updateUserShareRequestCreation(id, share).toEntity(localVarReturnType);
    }

    /**
     * Update share
     * Updates an existing share belonging to the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id    the share id
     * @param share The share parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateUserShareWithResponseSpec(String id, Share share) throws WebClientResponseException {
        return updateUserShareRequestCreation(id, share);
    }

    /**
     * Copy a file or a directory
     *
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file/folder to copy. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> userFileActionsCopyPost(String path, String target) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return userFileActionsCopyPostRequestCreation(path, target).bodyToMono(localVarReturnType);
    }

    /**
     * Copy a file or a directory
     *
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file/folder to copy. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> userFileActionsCopyPostWithHttpInfo(String path, String target) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return userFileActionsCopyPostRequestCreation(path, target).toEntity(localVarReturnType);
    }

    /**
     * Copy a file or a directory
     *
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file/folder to copy. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec userFileActionsCopyPostWithResponseSpec(String path, String target) throws WebClientResponseException {
        return userFileActionsCopyPostRequestCreation(path, target);
    }

    /**
     * Move (rename) a file or a directory
     *
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file/folder to rename. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> userFileActionsMovePost(String path, String target) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return userFileActionsMovePostRequestCreation(path, target).bodyToMono(localVarReturnType);
    }

    /**
     * Move (rename) a file or a directory
     *
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file/folder to rename. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> userFileActionsMovePostWithHttpInfo(String path, String target) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return userFileActionsMovePostRequestCreation(path, target).toEntity(localVarReturnType);
    }

    /**
     * Move (rename) a file or a directory
     *
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file/folder to rename. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec userFileActionsMovePostWithResponseSpec(String path, String target) throws WebClientResponseException {
        return userFileActionsMovePostRequestCreation(path, target);
    }

    /**
     * Validate a one time authentication code
     * Checks if the given authentication code can be validated using the specified secret and config name
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param validateAdminTotpSecretRequest The validateAdminTotpSecretRequest parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> validateUserTotpSecret(ValidateAdminTotpSecretRequest validateAdminTotpSecretRequest) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return validateUserTotpSecretRequestCreation(validateAdminTotpSecretRequest).bodyToMono(localVarReturnType);
    }

    /**
     * Validate a one time authentication code
     * Checks if the given authentication code can be validated using the specified secret and config name
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param validateAdminTotpSecretRequest The validateAdminTotpSecretRequest parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> validateUserTotpSecretWithHttpInfo(ValidateAdminTotpSecretRequest validateAdminTotpSecretRequest) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return validateUserTotpSecretRequestCreation(validateAdminTotpSecretRequest).toEntity(localVarReturnType);
    }

    /**
     * Validate a one time authentication code
     * Checks if the given authentication code can be validated using the specified secret and config name
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param validateAdminTotpSecretRequest The validateAdminTotpSecretRequest parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec validateUserTotpSecretWithResponseSpec(ValidateAdminTotpSecretRequest validateAdminTotpSecretRequest) throws WebClientResponseException {
        return validateUserTotpSecretRequestCreation(validateAdminTotpSecretRequest);
    }

    /**
     * Add a share
     * Adds a new share. The share id will be auto-generated
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param share The share parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addShareRequestCreation(Share share) throws WebClientResponseException {
        Object postBody = share;
        // verify the required parameter 'share' is set
        if (share == null) {
            throw new WebClientResponseException("Missing the required parameter 'share' when calling addShare", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/shares", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Change user password
     * Changes the password for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param pwdChange The pwdChange parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec changeUserPasswordRequestCreation(PwdChange pwdChange) throws WebClientResponseException {
        Object postBody = pwdChange;
        // verify the required parameter 'pwdChange' is set
        if (pwdChange == null) {
            throw new WebClientResponseException("Missing the required parameter 'pwdChange' when calling changeUserPassword", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[]{"BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/changepwd", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Create a directory
     * Create a directory for the logged in user
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path         Path to the folder to create. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param mkdirParents Create parent directories if they do not exist?
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec createUserDirRequestCreation(String path, Boolean mkdirParents) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling createUserDir", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "path", path));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "mkdir_parents", mkdirParents));

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/dirs", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Upload a single file
     * Upload a single file for the logged in user to an existing directory. This API does not use multipart/form-data and so no temporary files are created server side but only a single file can be uploaded as POST body
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path           Full file path. It must be path encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;. The parent directory must exist. If a file with the same name already exists, it will be overwritten
     * @param body           The body parameter
     * @param mkdirParents   Create parent directories if they do not exist?
     * @param X_SFTPGO_MTIME File modification time as unix timestamp in milliseconds
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec createUserFileRequestCreation(String path, File body, Boolean mkdirParents, Integer X_SFTPGO_MTIME) throws WebClientResponseException {
        Object postBody = body;
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling createUserFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new WebClientResponseException("Missing the required parameter 'body' when calling createUserFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "path", path));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "mkdir_parents", mkdirParents));


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

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/files/upload", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Upload files
     * Upload one or more files for the logged in user
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path         Parent directory for the uploaded files. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the root path is assumed. If a file with the same name already exists, it will be overwritten
     * @param mkdirParents Create parent directories if they do not exist?
     * @param filenames    The filenames parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec createUserFilesRequestCreation(String path, Boolean mkdirParents, Set<File> filenames) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "path", path));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "mkdir_parents", mkdirParents));

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

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/files", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete a directory
     * Delete a directory and any children it contains for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the folder to delete. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteUserDirRequestCreation(String path) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling deleteUserDir", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

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

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/dirs", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete a file
     * Delete a file for the logged in user.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the file to delete. It must be URL encoded
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteUserFileRequestCreation(String path) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling deleteUserFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

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

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/files", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete share
     * Deletes an existing share belonging to the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id the share id
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteUserShareRequestCreation(String id) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new WebClientResponseException("Missing the required parameter 'id' when calling deleteUserShare", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("id", id);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/shares/{id}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Download a single file
     * Returns the file contents as response body
     * <p><b>200</b> - successful operation
     * <p><b>206</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file to download. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param inline If set, the response will not have the Content-Disposition header set to &#x60;attachment&#x60;
     * @return File
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec downloadUserFileRequestCreation(String path, String inline) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling downloadUserFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

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

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return apiClient.invokeAPI("/user/files", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Generate recovery codes
     * Generates new recovery codes for the logged in user. Generating new recovery codes you automatically invalidate old ones
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return List&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec generateUserRecoveryCodesRequestCreation() throws WebClientResponseException {
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
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"BearerAuth"};

        ParameterizedTypeReference<List<String>> localVarReturnType = new ParameterizedTypeReference<List<String>>() {
        };
        return apiClient.invokeAPI("/user/2fa/recoverycodes", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Generate a new TOTP secret
     * Generates a new TOTP secret, including the QR code as png, using the specified configuration for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param generateAdminTotpSecretRequest The generateAdminTotpSecretRequest parameter
     * @return GenerateUserTotpSecret200Response
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec generateUserTotpSecretRequestCreation(GenerateAdminTotpSecretRequest generateAdminTotpSecretRequest) throws WebClientResponseException {
        Object postBody = generateAdminTotpSecretRequest;
        // verify the required parameter 'generateAdminTotpSecretRequest' is set
        if (generateAdminTotpSecretRequest == null) {
            throw new WebClientResponseException("Missing the required parameter 'generateAdminTotpSecretRequest' when calling generateUserTotpSecret", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[]{"BearerAuth"};

        ParameterizedTypeReference<GenerateUserTotpSecret200Response> localVarReturnType = new ParameterizedTypeReference<GenerateUserTotpSecret200Response>() {
        };
        return apiClient.invokeAPI("/user/totp/generate", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Read directory contents
     * Returns the contents of the specified directory for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path Path to the folder to read. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;. If empty or missing the user&#39;s start directory is assumed. If relative, the user&#39;s start directory is used as the base
     * @return List&lt;DirEntry&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUserDirContentsRequestCreation(String path) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

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

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<DirEntry> localVarReturnType = new ParameterizedTypeReference<DirEntry>() {
        };
        return apiClient.invokeAPI("/user/dirs", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get user profile
     * Returns the profile for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return UserProfile
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUserProfileRequestCreation() throws WebClientResponseException {
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
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"BearerAuth"};

        ParameterizedTypeReference<UserProfile> localVarReturnType = new ParameterizedTypeReference<UserProfile>() {
        };
        return apiClient.invokeAPI("/user/profile", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get recovery codes
     * Returns the recovery codes for the logged in user. Recovery codes can be used if the user loses access to their second factor auth device. Recovery codes are returned unencrypted
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return List&lt;RecoveryCode&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUserRecoveryCodesRequestCreation() throws WebClientResponseException {
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
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"BearerAuth"};

        ParameterizedTypeReference<RecoveryCode> localVarReturnType = new ParameterizedTypeReference<RecoveryCode>() {
        };
        return apiClient.invokeAPI("/user/2fa/recoverycodes", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get share by id
     * Returns a share by id for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id the share id
     * @return Share
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUserShareByIdRequestCreation(String id) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new WebClientResponseException("Missing the required parameter 'id' when calling getUserShareById", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("id", id);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<Share> localVarReturnType = new ParameterizedTypeReference<Share>() {
        };
        return apiClient.invokeAPI("/user/shares/{id}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List user shares
     * Returns the share for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param offset The offset parameter
     * @param limit  The maximum number of items to return. Max value is 500, default is 100
     * @param order  Ordering shares by ID. Default ASC
     * @return List&lt;Share&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUserSharesRequestCreation(Integer offset, Integer limit, String order) throws WebClientResponseException {
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
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<Share> localVarReturnType = new ParameterizedTypeReference<Share>() {
        };
        return apiClient.invokeAPI("/user/shares", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get available TOTP configuration
     * Returns the available TOTP configurations for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @return List&lt;TOTPConfig&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUserTotpConfigsRequestCreation() throws WebClientResponseException {
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
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"BearerAuth"};

        ParameterizedTypeReference<TOTPConfig> localVarReturnType = new ParameterizedTypeReference<TOTPConfig>() {
        };
        return apiClient.invokeAPI("/user/totp/configs", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Rename a directory. Deprecated, use \&quot;file-actions/move\&quot;
     * Rename a directory for the logged in user. The rename is allowed for empty directory or for non empty local directories, with no virtual folders inside
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the folder to rename. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec renameUserDirRequestCreation(String path, String target) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling renameUserDir", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'target' is set
        if (target == null) {
            throw new WebClientResponseException("Missing the required parameter 'target' when calling renameUserDir", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "path", path));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "target", target));

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/dirs", HttpMethod.PATCH, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Rename a file
     * Rename a file for the logged in user. Deprecated, use \&quot;file-actions/move\&quot;
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file to rename. It must be URL encoded
     * @param target New name. It must be URL encoded
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec renameUserFileRequestCreation(String path, String target) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling renameUserFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'target' is set
        if (target == null) {
            throw new WebClientResponseException("Missing the required parameter 'target' when calling renameUserFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "path", path));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "target", target));

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/files", HttpMethod.PATCH, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Save a TOTP config
     * Saves the specified TOTP config for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param userTOTPConfig The userTOTPConfig parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec saveUserTotpConfigRequestCreation(UserTOTPConfig userTOTPConfig) throws WebClientResponseException {
        Object postBody = userTOTPConfig;
        // verify the required parameter 'userTOTPConfig' is set
        if (userTOTPConfig == null) {
            throw new WebClientResponseException("Missing the required parameter 'userTOTPConfig' when calling saveUserTotpConfig", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[]{"BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/totp/save", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Set metadata for a file/directory
     * Set supported metadata attributes for the specified file or directory
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>413</b> - Request Entity Too Large, max allowed size exceeded
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path                    Full file/directory path. It must be URL encoded, for example the path \&quot;my dir/àdir/file.txt\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir%2Ffile.txt\&quot;
     * @param setpropsUserFileRequest The setpropsUserFileRequest parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec setpropsUserFileRequestCreation(String path, SetpropsUserFileRequest setpropsUserFileRequest) throws WebClientResponseException {
        Object postBody = setpropsUserFileRequest;
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling setpropsUserFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'setpropsUserFileRequest' is set
        if (setpropsUserFileRequest == null) {
            throw new WebClientResponseException("Missing the required parameter 'setpropsUserFileRequest' when calling setpropsUserFile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "path", path));

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
                "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/files/metadata", HttpMethod.PATCH, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Download multiple files and folders as a single zip file
     * A zip file, containing the specified files and folders, will be generated on the fly and returned as response body. Only folders and regular files will be included in the zip
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param requestBody The requestBody parameter
     * @return File
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec streamzipRequestCreation(List<String> requestBody) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling streamzip", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {
                "application/zip", "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
                "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<File> localVarReturnType = new ParameterizedTypeReference<File>() {
        };
        return apiClient.invokeAPI("/user/streamzip", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update user profile
     * Allows to update the profile for the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param userProfile The userProfile parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateUserProfileRequestCreation(UserProfile userProfile) throws WebClientResponseException {
        Object postBody = userProfile;
        // verify the required parameter 'userProfile' is set
        if (userProfile == null) {
            throw new WebClientResponseException("Missing the required parameter 'userProfile' when calling updateUserProfile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[]{"BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/profile", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update share
     * Updates an existing share belonging to the logged in user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param id    the share id
     * @param share The share parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateUserShareRequestCreation(String id, Share share) throws WebClientResponseException {
        Object postBody = share;
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new WebClientResponseException("Missing the required parameter 'id' when calling updateUserShare", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'share' is set
        if (share == null) {
            throw new WebClientResponseException("Missing the required parameter 'share' when calling updateUserShare", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("id", id);

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

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/shares/{id}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Copy a file or a directory
     *
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file/folder to copy. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec userFileActionsCopyPostRequestCreation(String path, String target) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling userFileActionsCopyPost", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'target' is set
        if (target == null) {
            throw new WebClientResponseException("Missing the required parameter 'target' when calling userFileActionsCopyPost", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "path", path));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "target", target));

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/file-actions/copy", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Move (rename) a file or a directory
     *
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param path   Path to the file/folder to rename. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @param target New name. It must be URL encoded, for example the path \&quot;my dir/àdir\&quot; must be sent as \&quot;my%20dir%2F%C3%A0dir\&quot;
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec userFileActionsMovePostRequestCreation(String path, String target) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'path' is set
        if (path == null) {
            throw new WebClientResponseException("Missing the required parameter 'path' when calling userFileActionsMovePost", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'target' is set
        if (target == null) {
            throw new WebClientResponseException("Missing the required parameter 'target' when calling userFileActionsMovePost", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "path", path));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "target", target));

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{"APIKeyAuth", "BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/file-actions/move", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Validate a one time authentication code
     * Checks if the given authentication code can be validated using the specified secret and config name
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     *
     * @param validateAdminTotpSecretRequest The validateAdminTotpSecretRequest parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec validateUserTotpSecretRequestCreation(ValidateAdminTotpSecretRequest validateAdminTotpSecretRequest) throws WebClientResponseException {
        Object postBody = validateAdminTotpSecretRequest;
        // verify the required parameter 'validateAdminTotpSecretRequest' is set
        if (validateAdminTotpSecretRequest == null) {
            throw new WebClientResponseException("Missing the required parameter 'validateAdminTotpSecretRequest' when calling validateUserTotpSecret", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[]{"BearerAuth"};

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {
        };
        return apiClient.invokeAPI("/user/totp/validate", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }
}
