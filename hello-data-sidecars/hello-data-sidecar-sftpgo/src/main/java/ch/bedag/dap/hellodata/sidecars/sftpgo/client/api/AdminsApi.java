package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.Admin;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.AdminProfile;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.AdminResetPasswordRequest;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.AdminTOTPConfig;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.GenerateAdminTotpSecret200Response;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.GenerateAdminTotpSecretRequest;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ModelApiResponse;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.PwdChange;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.RecoveryCode;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.TOTPConfig;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ValidateAdminTotpSecretRequest;

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
public class AdminsApi {
    private ApiClient apiClient;

    public AdminsApi() {
        this(new ApiClient());
    }

    @Autowired
    public AdminsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Add admin
     * Adds a new admin. Recovery codes and TOTP configuration cannot be set using this API: each admin must use the specific APIs
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param admin The admin parameter
     * @return Admin
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addAdminRequestCreation(Admin admin) throws WebClientResponseException {
        Object postBody = admin;
        // verify the required parameter 'admin' is set
        if (admin == null) {
            throw new WebClientResponseException("Missing the required parameter 'admin' when calling addAdmin", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<Admin> localVarReturnType = new ParameterizedTypeReference<Admin>() {};
        return apiClient.invokeAPI("/admins", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add admin
     * Adds a new admin. Recovery codes and TOTP configuration cannot be set using this API: each admin must use the specific APIs
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param admin The admin parameter
     * @return Admin
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<Admin> addAdmin(Admin admin) throws WebClientResponseException {
        ParameterizedTypeReference<Admin> localVarReturnType = new ParameterizedTypeReference<Admin>() {};
        return addAdminRequestCreation(admin).bodyToMono(localVarReturnType);
    }

    /**
     * Add admin
     * Adds a new admin. Recovery codes and TOTP configuration cannot be set using this API: each admin must use the specific APIs
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param admin The admin parameter
     * @return ResponseEntity&lt;Admin&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<Admin>> addAdminWithHttpInfo(Admin admin) throws WebClientResponseException {
        ParameterizedTypeReference<Admin> localVarReturnType = new ParameterizedTypeReference<Admin>() {};
        return addAdminRequestCreation(admin).toEntity(localVarReturnType);
    }

    /**
     * Add admin
     * Adds a new admin. Recovery codes and TOTP configuration cannot be set using this API: each admin must use the specific APIs
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param admin The admin parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addAdminWithResponseSpec(Admin admin) throws WebClientResponseException {
        return addAdminRequestCreation(admin);
    }
    /**
     * Send a password reset code by email
     * You must set up an SMTP server and the account must have a valid email address, in which case SFTPGo will send a code via email to reset the password. If the specified admin does not exist, the request will be silently ignored (a success response will be returned) to avoid disclosing existing admins
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec adminForgotPasswordRequestCreation(String username) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling adminForgotPassword", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/admins/{username}/forgot-password", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Send a password reset code by email
     * You must set up an SMTP server and the account must have a valid email address, in which case SFTPGo will send a code via email to reset the password. If the specified admin does not exist, the request will be silently ignored (a success response will be returned) to avoid disclosing existing admins
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> adminForgotPassword(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return adminForgotPasswordRequestCreation(username).bodyToMono(localVarReturnType);
    }

    /**
     * Send a password reset code by email
     * You must set up an SMTP server and the account must have a valid email address, in which case SFTPGo will send a code via email to reset the password. If the specified admin does not exist, the request will be silently ignored (a success response will be returned) to avoid disclosing existing admins
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> adminForgotPasswordWithHttpInfo(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return adminForgotPasswordRequestCreation(username).toEntity(localVarReturnType);
    }

    /**
     * Send a password reset code by email
     * You must set up an SMTP server and the account must have a valid email address, in which case SFTPGo will send a code via email to reset the password. If the specified admin does not exist, the request will be silently ignored (a success response will be returned) to avoid disclosing existing admins
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec adminForgotPasswordWithResponseSpec(String username) throws WebClientResponseException {
        return adminForgotPasswordRequestCreation(username);
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
     * @param username the admin username
     * @param adminResetPasswordRequest The adminResetPasswordRequest parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec adminResetPasswordRequestCreation(String username, AdminResetPasswordRequest adminResetPasswordRequest) throws WebClientResponseException {
        Object postBody = adminResetPasswordRequest;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling adminResetPassword", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'adminResetPasswordRequest' is set
        if (adminResetPasswordRequest == null) {
            throw new WebClientResponseException("Missing the required parameter 'adminResetPasswordRequest' when calling adminResetPassword", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/admins/{username}/reset-password", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
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
     * @param username the admin username
     * @param adminResetPasswordRequest The adminResetPasswordRequest parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> adminResetPassword(String username, AdminResetPasswordRequest adminResetPasswordRequest) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return adminResetPasswordRequestCreation(username, adminResetPasswordRequest).bodyToMono(localVarReturnType);
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
     * @param username the admin username
     * @param adminResetPasswordRequest The adminResetPasswordRequest parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> adminResetPasswordWithHttpInfo(String username, AdminResetPasswordRequest adminResetPasswordRequest) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return adminResetPasswordRequestCreation(username, adminResetPasswordRequest).toEntity(localVarReturnType);
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
     * @param username the admin username
     * @param adminResetPasswordRequest The adminResetPasswordRequest parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec adminResetPasswordWithResponseSpec(String username, AdminResetPasswordRequest adminResetPasswordRequest) throws WebClientResponseException {
        return adminResetPasswordRequestCreation(username, adminResetPasswordRequest);
    }
    /**
     * Change admin password
     * Changes the password for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param pwdChange The pwdChange parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec changeAdminPasswordRequestCreation(PwdChange pwdChange) throws WebClientResponseException {
        Object postBody = pwdChange;
        // verify the required parameter 'pwdChange' is set
        if (pwdChange == null) {
            throw new WebClientResponseException("Missing the required parameter 'pwdChange' when calling changeAdminPassword", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[] { "BearerAuth" };

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/admin/changepwd", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Change admin password
     * Changes the password for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param pwdChange The pwdChange parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> changeAdminPassword(PwdChange pwdChange) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return changeAdminPasswordRequestCreation(pwdChange).bodyToMono(localVarReturnType);
    }

    /**
     * Change admin password
     * Changes the password for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param pwdChange The pwdChange parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> changeAdminPasswordWithHttpInfo(PwdChange pwdChange) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return changeAdminPasswordRequestCreation(pwdChange).toEntity(localVarReturnType);
    }

    /**
     * Change admin password
     * Changes the password for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param pwdChange The pwdChange parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec changeAdminPasswordWithResponseSpec(PwdChange pwdChange) throws WebClientResponseException {
        return changeAdminPasswordRequestCreation(pwdChange);
    }
    /**
     * Delete admin
     * Deletes an existing admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteAdminRequestCreation(String username) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling deleteAdmin", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/admins/{username}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete admin
     * Deletes an existing admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> deleteAdmin(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteAdminRequestCreation(username).bodyToMono(localVarReturnType);
    }

    /**
     * Delete admin
     * Deletes an existing admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> deleteAdminWithHttpInfo(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteAdminRequestCreation(username).toEntity(localVarReturnType);
    }

    /**
     * Delete admin
     * Deletes an existing admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteAdminWithResponseSpec(String username) throws WebClientResponseException {
        return deleteAdminRequestCreation(username);
    }
    /**
     * Disable second factor authentication
     * Disables second factor authentication for the given admin. This API must be used if the admin loses access to their second factor auth device and has no recovery codes
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec disableAdmin2faRequestCreation(String username) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling disableAdmin2fa", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/admins/{username}/2fa/disable", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Disable second factor authentication
     * Disables second factor authentication for the given admin. This API must be used if the admin loses access to their second factor auth device and has no recovery codes
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> disableAdmin2fa(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return disableAdmin2faRequestCreation(username).bodyToMono(localVarReturnType);
    }

    /**
     * Disable second factor authentication
     * Disables second factor authentication for the given admin. This API must be used if the admin loses access to their second factor auth device and has no recovery codes
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> disableAdmin2faWithHttpInfo(String username) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return disableAdmin2faRequestCreation(username).toEntity(localVarReturnType);
    }

    /**
     * Disable second factor authentication
     * Disables second factor authentication for the given admin. This API must be used if the admin loses access to their second factor auth device and has no recovery codes
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec disableAdmin2faWithResponseSpec(String username) throws WebClientResponseException {
        return disableAdmin2faRequestCreation(username);
    }
    /**
     * Generate recovery codes
     * Generates new recovery codes for the logged in admin. Generating new recovery codes you automatically invalidate old ones
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec generateAdminRecoveryCodesRequestCreation() throws WebClientResponseException {
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

        String[] localVarAuthNames = new String[] { "BearerAuth" };

        ParameterizedTypeReference<List<String>> localVarReturnType = new ParameterizedTypeReference<List<String>>() {};
        return apiClient.invokeAPI("/admin/2fa/recoverycodes", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Generate recovery codes
     * Generates new recovery codes for the logged in admin. Generating new recovery codes you automatically invalidate old ones
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<List<String>> generateAdminRecoveryCodes() throws WebClientResponseException {
        ParameterizedTypeReference<List<String>> localVarReturnType = new ParameterizedTypeReference<List<String>>() {};
        return generateAdminRecoveryCodesRequestCreation().bodyToMono(localVarReturnType);
    }

    /**
     * Generate recovery codes
     * Generates new recovery codes for the logged in admin. Generating new recovery codes you automatically invalidate old ones
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;List&lt;String&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<String>>> generateAdminRecoveryCodesWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<List<String>> localVarReturnType = new ParameterizedTypeReference<List<String>>() {};
        return generateAdminRecoveryCodesRequestCreation().toEntity(localVarReturnType);
    }

    /**
     * Generate recovery codes
     * Generates new recovery codes for the logged in admin. Generating new recovery codes you automatically invalidate old ones
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec generateAdminRecoveryCodesWithResponseSpec() throws WebClientResponseException {
        return generateAdminRecoveryCodesRequestCreation();
    }
    /**
     * Generate a new TOTP secret
     * Generates a new TOTP secret, including the QR code as png, using the specified configuration for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param generateAdminTotpSecretRequest The generateAdminTotpSecretRequest parameter
     * @return GenerateAdminTotpSecret200Response
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec generateAdminTotpSecretRequestCreation(GenerateAdminTotpSecretRequest generateAdminTotpSecretRequest) throws WebClientResponseException {
        Object postBody = generateAdminTotpSecretRequest;
        // verify the required parameter 'generateAdminTotpSecretRequest' is set
        if (generateAdminTotpSecretRequest == null) {
            throw new WebClientResponseException("Missing the required parameter 'generateAdminTotpSecretRequest' when calling generateAdminTotpSecret", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[] { "BearerAuth" };

        ParameterizedTypeReference<GenerateAdminTotpSecret200Response> localVarReturnType = new ParameterizedTypeReference<GenerateAdminTotpSecret200Response>() {};
        return apiClient.invokeAPI("/admin/totp/generate", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Generate a new TOTP secret
     * Generates a new TOTP secret, including the QR code as png, using the specified configuration for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param generateAdminTotpSecretRequest The generateAdminTotpSecretRequest parameter
     * @return GenerateAdminTotpSecret200Response
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<GenerateAdminTotpSecret200Response> generateAdminTotpSecret(GenerateAdminTotpSecretRequest generateAdminTotpSecretRequest) throws WebClientResponseException {
        ParameterizedTypeReference<GenerateAdminTotpSecret200Response> localVarReturnType = new ParameterizedTypeReference<GenerateAdminTotpSecret200Response>() {};
        return generateAdminTotpSecretRequestCreation(generateAdminTotpSecretRequest).bodyToMono(localVarReturnType);
    }

    /**
     * Generate a new TOTP secret
     * Generates a new TOTP secret, including the QR code as png, using the specified configuration for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param generateAdminTotpSecretRequest The generateAdminTotpSecretRequest parameter
     * @return ResponseEntity&lt;GenerateAdminTotpSecret200Response&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<GenerateAdminTotpSecret200Response>> generateAdminTotpSecretWithHttpInfo(GenerateAdminTotpSecretRequest generateAdminTotpSecretRequest) throws WebClientResponseException {
        ParameterizedTypeReference<GenerateAdminTotpSecret200Response> localVarReturnType = new ParameterizedTypeReference<GenerateAdminTotpSecret200Response>() {};
        return generateAdminTotpSecretRequestCreation(generateAdminTotpSecretRequest).toEntity(localVarReturnType);
    }

    /**
     * Generate a new TOTP secret
     * Generates a new TOTP secret, including the QR code as png, using the specified configuration for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param generateAdminTotpSecretRequest The generateAdminTotpSecretRequest parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec generateAdminTotpSecretWithResponseSpec(GenerateAdminTotpSecretRequest generateAdminTotpSecretRequest) throws WebClientResponseException {
        return generateAdminTotpSecretRequestCreation(generateAdminTotpSecretRequest);
    }
    /**
     * Find admins by username
     * Returns the admin with the given username, if it exists. For security reasons the hashed password is omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return Admin
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAdminByUsernameRequestCreation(String username) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling getAdminByUsername", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<Admin> localVarReturnType = new ParameterizedTypeReference<Admin>() {};
        return apiClient.invokeAPI("/admins/{username}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Find admins by username
     * Returns the admin with the given username, if it exists. For security reasons the hashed password is omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return Admin
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<Admin> getAdminByUsername(String username) throws WebClientResponseException {
        ParameterizedTypeReference<Admin> localVarReturnType = new ParameterizedTypeReference<Admin>() {};
        return getAdminByUsernameRequestCreation(username).bodyToMono(localVarReturnType);
    }

    /**
     * Find admins by username
     * Returns the admin with the given username, if it exists. For security reasons the hashed password is omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ResponseEntity&lt;Admin&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<Admin>> getAdminByUsernameWithHttpInfo(String username) throws WebClientResponseException {
        ParameterizedTypeReference<Admin> localVarReturnType = new ParameterizedTypeReference<Admin>() {};
        return getAdminByUsernameRequestCreation(username).toEntity(localVarReturnType);
    }

    /**
     * Find admins by username
     * Returns the admin with the given username, if it exists. For security reasons the hashed password is omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAdminByUsernameWithResponseSpec(String username) throws WebClientResponseException {
        return getAdminByUsernameRequestCreation(username);
    }
    /**
     * Get admin profile
     * Returns the profile for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return AdminProfile
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAdminProfileRequestCreation() throws WebClientResponseException {
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

        String[] localVarAuthNames = new String[] { "BearerAuth" };

        ParameterizedTypeReference<AdminProfile> localVarReturnType = new ParameterizedTypeReference<AdminProfile>() {};
        return apiClient.invokeAPI("/admin/profile", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get admin profile
     * Returns the profile for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return AdminProfile
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<AdminProfile> getAdminProfile() throws WebClientResponseException {
        ParameterizedTypeReference<AdminProfile> localVarReturnType = new ParameterizedTypeReference<AdminProfile>() {};
        return getAdminProfileRequestCreation().bodyToMono(localVarReturnType);
    }

    /**
     * Get admin profile
     * Returns the profile for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;AdminProfile&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<AdminProfile>> getAdminProfileWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<AdminProfile> localVarReturnType = new ParameterizedTypeReference<AdminProfile>() {};
        return getAdminProfileRequestCreation().toEntity(localVarReturnType);
    }

    /**
     * Get admin profile
     * Returns the profile for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAdminProfileWithResponseSpec() throws WebClientResponseException {
        return getAdminProfileRequestCreation();
    }
    /**
     * Get recovery codes
     * Returns the recovery codes for the logged in admin. Recovery codes can be used if the admin loses access to their second factor auth device. Recovery codes are returned unencrypted
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;RecoveryCode&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAdminRecoveryCodesRequestCreation() throws WebClientResponseException {
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

        String[] localVarAuthNames = new String[] { "BearerAuth" };

        ParameterizedTypeReference<RecoveryCode> localVarReturnType = new ParameterizedTypeReference<RecoveryCode>() {};
        return apiClient.invokeAPI("/admin/2fa/recoverycodes", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get recovery codes
     * Returns the recovery codes for the logged in admin. Recovery codes can be used if the admin loses access to their second factor auth device. Recovery codes are returned unencrypted
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;RecoveryCode&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<RecoveryCode> getAdminRecoveryCodes() throws WebClientResponseException {
        ParameterizedTypeReference<RecoveryCode> localVarReturnType = new ParameterizedTypeReference<RecoveryCode>() {};
        return getAdminRecoveryCodesRequestCreation().bodyToFlux(localVarReturnType);
    }

    /**
     * Get recovery codes
     * Returns the recovery codes for the logged in admin. Recovery codes can be used if the admin loses access to their second factor auth device. Recovery codes are returned unencrypted
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;List&lt;RecoveryCode&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<RecoveryCode>>> getAdminRecoveryCodesWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<RecoveryCode> localVarReturnType = new ParameterizedTypeReference<RecoveryCode>() {};
        return getAdminRecoveryCodesRequestCreation().toEntityList(localVarReturnType);
    }

    /**
     * Get recovery codes
     * Returns the recovery codes for the logged in admin. Recovery codes can be used if the admin loses access to their second factor auth device. Recovery codes are returned unencrypted
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAdminRecoveryCodesWithResponseSpec() throws WebClientResponseException {
        return getAdminRecoveryCodesRequestCreation();
    }
    /**
     * Get available TOTP configuration
     * Returns the available TOTP configurations for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;TOTPConfig&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAdminTotpConfigsRequestCreation() throws WebClientResponseException {
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

        String[] localVarAuthNames = new String[] { "BearerAuth" };

        ParameterizedTypeReference<TOTPConfig> localVarReturnType = new ParameterizedTypeReference<TOTPConfig>() {};
        return apiClient.invokeAPI("/admin/totp/configs", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get available TOTP configuration
     * Returns the available TOTP configurations for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return List&lt;TOTPConfig&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<TOTPConfig> getAdminTotpConfigs() throws WebClientResponseException {
        ParameterizedTypeReference<TOTPConfig> localVarReturnType = new ParameterizedTypeReference<TOTPConfig>() {};
        return getAdminTotpConfigsRequestCreation().bodyToFlux(localVarReturnType);
    }

    /**
     * Get available TOTP configuration
     * Returns the available TOTP configurations for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;List&lt;TOTPConfig&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<TOTPConfig>>> getAdminTotpConfigsWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<TOTPConfig> localVarReturnType = new ParameterizedTypeReference<TOTPConfig>() {};
        return getAdminTotpConfigsRequestCreation().toEntityList(localVarReturnType);
    }

    /**
     * Get available TOTP configuration
     * Returns the available TOTP configurations for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAdminTotpConfigsWithResponseSpec() throws WebClientResponseException {
        return getAdminTotpConfigsRequestCreation();
    }
    /**
     * Get admins
     * Returns an array with one or more admins. For security reasons hashed passwords are omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering admins by username. Default ASC
     * @return List&lt;Admin&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAdminsRequestCreation(Integer offset, Integer limit, String order) throws WebClientResponseException {
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

        ParameterizedTypeReference<Admin> localVarReturnType = new ParameterizedTypeReference<Admin>() {};
        return apiClient.invokeAPI("/admins", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get admins
     * Returns an array with one or more admins. For security reasons hashed passwords are omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering admins by username. Default ASC
     * @return List&lt;Admin&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<Admin> getAdmins(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<Admin> localVarReturnType = new ParameterizedTypeReference<Admin>() {};
        return getAdminsRequestCreation(offset, limit, order).bodyToFlux(localVarReturnType);
    }

    /**
     * Get admins
     * Returns an array with one or more admins. For security reasons hashed passwords are omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering admins by username. Default ASC
     * @return ResponseEntity&lt;List&lt;Admin&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<Admin>>> getAdminsWithHttpInfo(Integer offset, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<Admin> localVarReturnType = new ParameterizedTypeReference<Admin>() {};
        return getAdminsRequestCreation(offset, limit, order).toEntityList(localVarReturnType);
    }

    /**
     * Get admins
     * Returns an array with one or more admins. For security reasons hashed passwords are omitted in the response
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param offset The offset parameter
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering admins by username. Default ASC
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAdminsWithResponseSpec(Integer offset, Integer limit, String order) throws WebClientResponseException {
        return getAdminsRequestCreation(offset, limit, order);
    }
    /**
     * Save a TOTP config
     * Saves the specified TOTP config for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param adminTOTPConfig The adminTOTPConfig parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec saveAdminTotpConfigRequestCreation(AdminTOTPConfig adminTOTPConfig) throws WebClientResponseException {
        Object postBody = adminTOTPConfig;
        // verify the required parameter 'adminTOTPConfig' is set
        if (adminTOTPConfig == null) {
            throw new WebClientResponseException("Missing the required parameter 'adminTOTPConfig' when calling saveAdminTotpConfig", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[] { "BearerAuth" };

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/admin/totp/save", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Save a TOTP config
     * Saves the specified TOTP config for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param adminTOTPConfig The adminTOTPConfig parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> saveAdminTotpConfig(AdminTOTPConfig adminTOTPConfig) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return saveAdminTotpConfigRequestCreation(adminTOTPConfig).bodyToMono(localVarReturnType);
    }

    /**
     * Save a TOTP config
     * Saves the specified TOTP config for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param adminTOTPConfig The adminTOTPConfig parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> saveAdminTotpConfigWithHttpInfo(AdminTOTPConfig adminTOTPConfig) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return saveAdminTotpConfigRequestCreation(adminTOTPConfig).toEntity(localVarReturnType);
    }

    /**
     * Save a TOTP config
     * Saves the specified TOTP config for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param adminTOTPConfig The adminTOTPConfig parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec saveAdminTotpConfigWithResponseSpec(AdminTOTPConfig adminTOTPConfig) throws WebClientResponseException {
        return saveAdminTotpConfigRequestCreation(adminTOTPConfig);
    }
    /**
     * Update admin
     * Updates an existing admin. Recovery codes and TOTP configuration cannot be set/updated using this API: each admin must use the specific APIs. You are not allowed to update the admin impersonated using an API key
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @param admin The admin parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateAdminRequestCreation(String username, Admin admin) throws WebClientResponseException {
        Object postBody = admin;
        // verify the required parameter 'username' is set
        if (username == null) {
            throw new WebClientResponseException("Missing the required parameter 'username' when calling updateAdmin", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'admin' is set
        if (admin == null) {
            throw new WebClientResponseException("Missing the required parameter 'admin' when calling updateAdmin", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/admins/{username}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update admin
     * Updates an existing admin. Recovery codes and TOTP configuration cannot be set/updated using this API: each admin must use the specific APIs. You are not allowed to update the admin impersonated using an API key
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @param admin The admin parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> updateAdmin(String username, Admin admin) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateAdminRequestCreation(username, admin).bodyToMono(localVarReturnType);
    }

    /**
     * Update admin
     * Updates an existing admin. Recovery codes and TOTP configuration cannot be set/updated using this API: each admin must use the specific APIs. You are not allowed to update the admin impersonated using an API key
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @param admin The admin parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> updateAdminWithHttpInfo(String username, Admin admin) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateAdminRequestCreation(username, admin).toEntity(localVarReturnType);
    }

    /**
     * Update admin
     * Updates an existing admin. Recovery codes and TOTP configuration cannot be set/updated using this API: each admin must use the specific APIs. You are not allowed to update the admin impersonated using an API key
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param username the admin username
     * @param admin The admin parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateAdminWithResponseSpec(String username, Admin admin) throws WebClientResponseException {
        return updateAdminRequestCreation(username, admin);
    }
    /**
     * Update admin profile
     * Allows to update the profile for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param adminProfile The adminProfile parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateAdminProfileRequestCreation(AdminProfile adminProfile) throws WebClientResponseException {
        Object postBody = adminProfile;
        // verify the required parameter 'adminProfile' is set
        if (adminProfile == null) {
            throw new WebClientResponseException("Missing the required parameter 'adminProfile' when calling updateAdminProfile", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[] { "BearerAuth" };

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/admin/profile", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update admin profile
     * Allows to update the profile for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param adminProfile The adminProfile parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> updateAdminProfile(AdminProfile adminProfile) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateAdminProfileRequestCreation(adminProfile).bodyToMono(localVarReturnType);
    }

    /**
     * Update admin profile
     * Allows to update the profile for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param adminProfile The adminProfile parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> updateAdminProfileWithHttpInfo(AdminProfile adminProfile) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateAdminProfileRequestCreation(adminProfile).toEntity(localVarReturnType);
    }

    /**
     * Update admin profile
     * Allows to update the profile for the logged in admin
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param adminProfile The adminProfile parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateAdminProfileWithResponseSpec(AdminProfile adminProfile) throws WebClientResponseException {
        return updateAdminProfileRequestCreation(adminProfile);
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
     * @param validateAdminTotpSecretRequest The validateAdminTotpSecretRequest parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec validateAdminTotpSecretRequestCreation(ValidateAdminTotpSecretRequest validateAdminTotpSecretRequest) throws WebClientResponseException {
        Object postBody = validateAdminTotpSecretRequest;
        // verify the required parameter 'validateAdminTotpSecretRequest' is set
        if (validateAdminTotpSecretRequest == null) {
            throw new WebClientResponseException("Missing the required parameter 'validateAdminTotpSecretRequest' when calling validateAdminTotpSecret", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        String[] localVarAuthNames = new String[] { "BearerAuth" };

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/admin/totp/validate", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
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
     * @param validateAdminTotpSecretRequest The validateAdminTotpSecretRequest parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> validateAdminTotpSecret(ValidateAdminTotpSecretRequest validateAdminTotpSecretRequest) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return validateAdminTotpSecretRequestCreation(validateAdminTotpSecretRequest).bodyToMono(localVarReturnType);
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
     * @param validateAdminTotpSecretRequest The validateAdminTotpSecretRequest parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> validateAdminTotpSecretWithHttpInfo(ValidateAdminTotpSecretRequest validateAdminTotpSecretRequest) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return validateAdminTotpSecretRequestCreation(validateAdminTotpSecretRequest).toEntity(localVarReturnType);
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
     * @param validateAdminTotpSecretRequest The validateAdminTotpSecretRequest parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec validateAdminTotpSecretWithResponseSpec(ValidateAdminTotpSecretRequest validateAdminTotpSecretRequest) throws WebClientResponseException {
        return validateAdminTotpSecretRequestCreation(validateAdminTotpSecretRequest);
    }
}
