package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ModelApiResponse;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.Token;

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
public class TokenApi {
    private ApiClient apiClient;

    public TokenApi() {
        this(new ApiClient());
    }

    @Autowired
    public TokenApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Invalidate a user access token
     * Allows to invalidate a client token before its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec clientLogoutRequestCreation() throws WebClientResponseException {
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

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/user/logout", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Invalidate a user access token
     * Allows to invalidate a client token before its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> clientLogout() throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return clientLogoutRequestCreation().bodyToMono(localVarReturnType);
    }

    /**
     * Invalidate a user access token
     * Allows to invalidate a client token before its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> clientLogoutWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return clientLogoutRequestCreation().toEntity(localVarReturnType);
    }

    /**
     * Invalidate a user access token
     * Allows to invalidate a client token before its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec clientLogoutWithResponseSpec() throws WebClientResponseException {
        return clientLogoutRequestCreation();
    }
    /**
     * Get a new admin access token
     * Returns an access token and its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param X_SFTPGO_OTP If you have 2FA configured for the admin attempting to log in you need to set the authentication code using this header parameter
     * @return Token
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getTokenRequestCreation(String X_SFTPGO_OTP) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();


        if (X_SFTPGO_OTP != null)
        headerParams.add("X-SFTPGO-OTP", apiClient.parameterToString(X_SFTPGO_OTP));
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "BasicAuth" };

        ParameterizedTypeReference<Token> localVarReturnType = new ParameterizedTypeReference<Token>() {};
        return apiClient.invokeAPI("/token", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get a new admin access token
     * Returns an access token and its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param X_SFTPGO_OTP If you have 2FA configured for the admin attempting to log in you need to set the authentication code using this header parameter
     * @return Token
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<Token> getToken(String X_SFTPGO_OTP) throws WebClientResponseException {
        ParameterizedTypeReference<Token> localVarReturnType = new ParameterizedTypeReference<Token>() {};
        return getTokenRequestCreation(X_SFTPGO_OTP).bodyToMono(localVarReturnType);
    }

    /**
     * Get a new admin access token
     * Returns an access token and its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param X_SFTPGO_OTP If you have 2FA configured for the admin attempting to log in you need to set the authentication code using this header parameter
     * @return ResponseEntity&lt;Token&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<Token>> getTokenWithHttpInfo(String X_SFTPGO_OTP) throws WebClientResponseException {
        ParameterizedTypeReference<Token> localVarReturnType = new ParameterizedTypeReference<Token>() {};
        return getTokenRequestCreation(X_SFTPGO_OTP).toEntity(localVarReturnType);
    }

    /**
     * Get a new admin access token
     * Returns an access token and its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param X_SFTPGO_OTP If you have 2FA configured for the admin attempting to log in you need to set the authentication code using this header parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getTokenWithResponseSpec(String X_SFTPGO_OTP) throws WebClientResponseException {
        return getTokenRequestCreation(X_SFTPGO_OTP);
    }
    /**
     * Get a new user access token
     * Returns an access token and its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param X_SFTPGO_OTP If you have 2FA configured, for the HTTP protocol, for the user attempting to log in you need to set the authentication code using this header parameter
     * @return Token
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getUserTokenRequestCreation(String X_SFTPGO_OTP) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();


        if (X_SFTPGO_OTP != null)
        headerParams.add("X-SFTPGO-OTP", apiClient.parameterToString(X_SFTPGO_OTP));
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "BasicAuth" };

        ParameterizedTypeReference<Token> localVarReturnType = new ParameterizedTypeReference<Token>() {};
        return apiClient.invokeAPI("/user/token", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get a new user access token
     * Returns an access token and its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param X_SFTPGO_OTP If you have 2FA configured, for the HTTP protocol, for the user attempting to log in you need to set the authentication code using this header parameter
     * @return Token
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<Token> getUserToken(String X_SFTPGO_OTP) throws WebClientResponseException {
        ParameterizedTypeReference<Token> localVarReturnType = new ParameterizedTypeReference<Token>() {};
        return getUserTokenRequestCreation(X_SFTPGO_OTP).bodyToMono(localVarReturnType);
    }

    /**
     * Get a new user access token
     * Returns an access token and its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param X_SFTPGO_OTP If you have 2FA configured, for the HTTP protocol, for the user attempting to log in you need to set the authentication code using this header parameter
     * @return ResponseEntity&lt;Token&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<Token>> getUserTokenWithHttpInfo(String X_SFTPGO_OTP) throws WebClientResponseException {
        ParameterizedTypeReference<Token> localVarReturnType = new ParameterizedTypeReference<Token>() {};
        return getUserTokenRequestCreation(X_SFTPGO_OTP).toEntity(localVarReturnType);
    }

    /**
     * Get a new user access token
     * Returns an access token and its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param X_SFTPGO_OTP If you have 2FA configured, for the HTTP protocol, for the user attempting to log in you need to set the authentication code using this header parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getUserTokenWithResponseSpec(String X_SFTPGO_OTP) throws WebClientResponseException {
        return getUserTokenRequestCreation(X_SFTPGO_OTP);
    }
    /**
     * Invalidate an admin access token
     * Allows to invalidate an admin token before its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec logoutRequestCreation() throws WebClientResponseException {
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

        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return apiClient.invokeAPI("/logout", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Invalidate an admin access token
     * Allows to invalidate an admin token before its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> logout() throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return logoutRequestCreation().bodyToMono(localVarReturnType);
    }

    /**
     * Invalidate an admin access token
     * Allows to invalidate an admin token before its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> logoutWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return logoutRequestCreation().toEntity(localVarReturnType);
    }

    /**
     * Invalidate an admin access token
     * Allows to invalidate an admin token before its expiration
     * <p><b>200</b> - successful operation
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec logoutWithResponseSpec() throws WebClientResponseException {
        return logoutRequestCreation();
    }
}
