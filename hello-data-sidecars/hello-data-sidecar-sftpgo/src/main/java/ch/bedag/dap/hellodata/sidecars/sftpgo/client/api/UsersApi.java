package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.AdminResetPasswordRequest;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ModelApiResponse;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.User;

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
public class UsersApi {
    private ApiClient apiClient;

    public UsersApi() {
        this(new ApiClient());
    }

    @Autowired
    public UsersApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Add user
     * Adds a new user.Recovery codes and TOTP configuration cannot be set using this API: each user must use the specific APIs
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param user The user parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the hash of the password and the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return User
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addUserRequestCreation(User user, Integer confidentialData) throws WebClientResponseException {
        Object postBody = user;
        // verify the required parameter 'user' is set
        if (user == null) {
            throw new WebClientResponseException("Missing the required parameter 'user' when calling addUser", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<User> localVarReturnType = new ParameterizedTypeReference<User>() {};
        return apiClient.invokeAPI("/users", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add user
     * Adds a new user.Recovery codes and TOTP configuration cannot be set using this API: each user must use the specific APIs
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param user The user parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the hash of the password and the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return User
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<User> addUser(User user, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<User> localVarReturnType = new ParameterizedTypeReference<User>() {};
        return addUserRequestCreation(user, confidentialData).bodyToMono(localVarReturnType);
    }

    /**
     * Add user
     * Adds a new user.Recovery codes and TOTP configuration cannot be set using this API: each user must use the specific APIs
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param user The user parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the hash of the password and the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseEntity&lt;User&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<User>> addUserWithHttpInfo(User user, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<User> localVarReturnType = new ParameterizedTypeReference<User>() {};
        return addUserRequestCreation(user, confidentialData).toEntity(localVarReturnType);
    }

    /**
     * Add user
     * Adds a new user.Recovery codes and TOTP configuration cannot be set using this API: each user must use the specific APIs
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param user The user parameter
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the hash of the password and the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addUserWithResponseSpec(User user, Integer confidentialData) throws WebClientResponseException {
        return addUserRequestCreation(user, confidentialData);
    }
    /**
     * Delete user
     * Deletes an existing user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteUserRequestCreation(String username) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling deleteUser", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/users/{username}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete user
     * Deletes an existing user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> deleteUser(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteUserRequestCreation(username).bodyToMono(localVarReturnType);
    }

    /**
     * Delete user
     * Deletes an existing user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> deleteUserWithHttpInfo(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteUserRequestCreation(username).toEntity(localVarReturnType);
    }

    /**
     * Delete user
     * Deletes an existing user
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteUserWithResponseSpec(String username) throws WebClientResponseException {
        return deleteUserRequestCreation(username);
    }
    /**
     * Disable second factor authentication
     * Disables second factor authentication for the given user. This API must be used if the user loses access to their second factor auth device and has no recovery codes
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec disableUser2faRequestCreation(String username) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling disableUser2fa", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/users/{username}/2fa/disable", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Disable second factor authentication
     * Disables second factor authentication for the given user. This API must be used if the user loses access to their second factor auth device and has no recovery codes
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> disableUser2fa(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return disableUser2faRequestCreation(username).bodyToMono(localVarReturnType);
    }

    /**
     * Disable second factor authentication
     * Disables second factor authentication for the given user. This API must be used if the user loses access to their second factor auth device and has no recovery codes
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> disableUser2faWithHttpInfo(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return disableUser2faRequestCreation(username).toEntity(localVarReturnType);
    }

    /**
     * Disable second factor authentication
     * Disables second factor authentication for the given user. This API must be used if the user loses access to their second factor auth device and has no recovery codes
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec disableUser2faWithResponseSpec(String username) throws WebClientResponseException {
        return disableUser2faRequestCreation(username);
    }
    /**
     * Find users by username
     * Returns the user with the given username if it exists. For security reasons the hashed password is omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the hash of the password and the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return User
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUserByUsernameRequestCreation(String username, Integer confidentialData) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling getUserByUsername", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("username", username);

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

        ParameterizedTypeReference<User> localVarReturnType = new ParameterizedTypeReference<User>() {};
        return apiClient.invokeAPI("/users/{username}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Find users by username
     * Returns the user with the given username if it exists. For security reasons the hashed password is omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the hash of the password and the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return User
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<User> getUserByUsername(String username, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<User> localVarReturnType = new ParameterizedTypeReference<User>() {};
        return getUserByUsernameRequestCreation(username, confidentialData).bodyToMono(localVarReturnType);
    }

    /**
     * Find users by username
     * Returns the user with the given username if it exists. For security reasons the hashed password is omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the hash of the password and the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseEntity&lt;User&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<User>> getUserByUsernameWithHttpInfo(String username, Integer confidentialData) throws WebClientResponseException {
        ParameterizedTypeReference<User> localVarReturnType = new ParameterizedTypeReference<User>() {};
        return getUserByUsernameRequestCreation(username, confidentialData).toEntity(localVarReturnType);
    }

    /**
     * Find users by username
     * Returns the user with the given username if it exists. For security reasons the hashed password is omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param confidentialData If set to 1 confidential data will not be hidden. This means that the response will contain the hash of the password and the key and additional data for secrets. If a master key is not set or an external KMS is used, the data returned are enough to get the secrets in cleartext. Ignored if the * permission is not granted.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUserByUsernameWithResponseSpec(String username, Integer confidentialData) throws WebClientResponseException {
        return getUserByUsernameRequestCreation(username, confidentialData);
    }
    /**
     * Get users
     * Returns an array with one or more users. For security reasons hashed passwords are omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering users by username. Default ASC
     * @return List&lt;User&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUsersRequestCreation(Integer offset, Integer limit, String order) throws WebClientResponseException {
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

        ParameterizedTypeReference<User> localVarReturnType = new ParameterizedTypeReference<User>() {};
        return apiClient.invokeAPI("/users", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get users
     * Returns an array with one or more users. For security reasons hashed passwords are omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering users by username. Default ASC
     * @return List&lt;User&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<User> getUsers(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<User> localVarReturnType = new ParameterizedTypeReference<User>() {};
        return getUsersRequestCreation(offset, limit, order).bodyToFlux(localVarReturnType);
    }

    /**
     * Get users
     * Returns an array with one or more users. For security reasons hashed passwords are omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering users by username. Default ASC
     * @return ResponseEntity&lt;List&lt;User&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<User>>> getUsersWithHttpInfo(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<User> localVarReturnType = new ParameterizedTypeReference<User>() {};
        return getUsersRequestCreation(offset, limit, order).toEntityList(localVarReturnType);
    }

    /**
     * Get users
     * Returns an array with one or more users. For security reasons hashed passwords are omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering users by username. Default ASC
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUsersWithResponseSpec(Integer offset, Integer limit, String order) throws WebClientResponseException {
        return getUsersRequestCreation(offset, limit, order);
    }
    /**
     * Update user
     * Updates an existing user and optionally disconnects it, if connected, to apply the new settings. The current password will be preserved if the password field is omitted in the request body. Recovery codes and TOTP configuration cannot be set/updated using this API: each user must use the specific APIs
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param user The user parameter
     * @param disconnect Disconnect:   * &#x60;0&#x60; The user will not be disconnected and it will continue to use the old configuration until connected. This is the default   * &#x60;1&#x60; The user will be disconnected after a successful update. It must login again and so it will be forced to use the new configuration 
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateUserRequestCreation(String username, User user, Integer disconnect) throws WebClientResponseException {
        Object postBody = user;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling updateUser", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'user' is set
        if (user == null) {
            throw new WebClientResponseException("Missing the required parameter 'user' when calling updateUser", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("username", username);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "disconnect", disconnect));
        
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
        return apiClient.invokeAPI("/users/{username}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update user
     * Updates an existing user and optionally disconnects it, if connected, to apply the new settings. The current password will be preserved if the password field is omitted in the request body. Recovery codes and TOTP configuration cannot be set/updated using this API: each user must use the specific APIs
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param user The user parameter
     * @param disconnect Disconnect:   * &#x60;0&#x60; The user will not be disconnected and it will continue to use the old configuration until connected. This is the default   * &#x60;1&#x60; The user will be disconnected after a successful update. It must login again and so it will be forced to use the new configuration 
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> updateUser(String username, User user, Integer disconnect) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateUserRequestCreation(username, user, disconnect).bodyToMono(localVarReturnType);
    }

    /**
     * Update user
     * Updates an existing user and optionally disconnects it, if connected, to apply the new settings. The current password will be preserved if the password field is omitted in the request body. Recovery codes and TOTP configuration cannot be set/updated using this API: each user must use the specific APIs
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param user The user parameter
     * @param disconnect Disconnect:   * &#x60;0&#x60; The user will not be disconnected and it will continue to use the old configuration until connected. This is the default   * &#x60;1&#x60; The user will be disconnected after a successful update. It must login again and so it will be forced to use the new configuration 
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> updateUserWithHttpInfo(String username, User user, Integer disconnect) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateUserRequestCreation(username, user, disconnect).toEntity(localVarReturnType);
    }

    /**
     * Update user
     * Updates an existing user and optionally disconnects it, if connected, to apply the new settings. The current password will be preserved if the password field is omitted in the request body. Recovery codes and TOTP configuration cannot be set/updated using this API: each user must use the specific APIs
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param user The user parameter
     * @param disconnect Disconnect:   * &#x60;0&#x60; The user will not be disconnected and it will continue to use the old configuration until connected. This is the default   * &#x60;1&#x60; The user will be disconnected after a successful update. It must login again and so it will be forced to use the new configuration 
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateUserWithResponseSpec(String username, User user, Integer disconnect) throws WebClientResponseException {
        return updateUserRequestCreation(username, user, disconnect);
    }
    /**
     * Send a password reset code by email
     * You must configure an SMTP server, the account must have a valid email address and must not have the \&quot;reset-password-disabled\&quot; restriction, in which case SFTPGo will send a code via email to reset the password. If the specified user does not exist, the request will be silently ignored (a success response will be returned) to avoid disclosing existing users
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec userForgotPasswordRequestCreation(String username) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling userForgotPassword", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/users/{username}/forgot-password", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Send a password reset code by email
     * You must configure an SMTP server, the account must have a valid email address and must not have the \&quot;reset-password-disabled\&quot; restriction, in which case SFTPGo will send a code via email to reset the password. If the specified user does not exist, the request will be silently ignored (a success response will be returned) to avoid disclosing existing users
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> userForgotPassword(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return userForgotPasswordRequestCreation(username).bodyToMono(localVarReturnType);
    }

    /**
     * Send a password reset code by email
     * You must configure an SMTP server, the account must have a valid email address and must not have the \&quot;reset-password-disabled\&quot; restriction, in which case SFTPGo will send a code via email to reset the password. If the specified user does not exist, the request will be silently ignored (a success response will be returned) to avoid disclosing existing users
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> userForgotPasswordWithHttpInfo(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return userForgotPasswordRequestCreation(username).toEntity(localVarReturnType);
    }

    /**
     * Send a password reset code by email
     * You must configure an SMTP server, the account must have a valid email address and must not have the \&quot;reset-password-disabled\&quot; restriction, in which case SFTPGo will send a code via email to reset the password. If the specified user does not exist, the request will be silently ignored (a success response will be returned) to avoid disclosing existing users
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec userForgotPasswordWithResponseSpec(String username) throws WebClientResponseException {
        return userForgotPasswordRequestCreation(username);
    }
    /**
     * Reset the password
     * Set a new password using the code received via email
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param adminResetPasswordRequest The adminResetPasswordRequest parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec userResetPasswordRequestCreation(String username, AdminResetPasswordRequest adminResetPasswordRequest) throws WebClientResponseException {
        Object postBody = adminResetPasswordRequest;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling userResetPassword", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'adminResetPasswordRequest' is set
        if (adminResetPasswordRequest == null) {
            throw new WebClientResponseException("Missing the required parameter 'adminResetPasswordRequest' when calling userResetPassword", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/users/{username}/reset-password", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Reset the password
     * Set a new password using the code received via email
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param adminResetPasswordRequest The adminResetPasswordRequest parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> userResetPassword(String username, AdminResetPasswordRequest adminResetPasswordRequest) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return userResetPasswordRequestCreation(username, adminResetPasswordRequest).bodyToMono(localVarReturnType);
    }

    /**
     * Reset the password
     * Set a new password using the code received via email
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param adminResetPasswordRequest The adminResetPasswordRequest parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> userResetPasswordWithHttpInfo(String username, AdminResetPasswordRequest adminResetPasswordRequest) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return userResetPasswordRequestCreation(username, adminResetPasswordRequest).toEntity(localVarReturnType);
    }

    /**
     * Reset the password
     * Set a new password using the code received via email
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the username
     * @param adminResetPasswordRequest The adminResetPasswordRequest parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec userResetPasswordWithResponseSpec(String username, AdminResetPasswordRequest adminResetPasswordRequest) throws WebClientResponseException {
        return userResetPasswordRequestCreation(username, adminResetPasswordRequest);
    }
}
