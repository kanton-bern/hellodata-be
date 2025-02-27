package ch.bedag.dap.hellodata.sidecars.sftpgo.client.api;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.IPListEntry;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.IPListType;
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
public class IpListsApi {
    private ApiClient apiClient;

    public IpListsApi() {
        this(new ApiClient());
    }

    @Autowired
    public IpListsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Add a new IP list entry
     * Add an IP address or a CIDR network to a supported list
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipListEntry The ipListEntry parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addIpListEntryRequestCreation(IPListType type, IPListEntry ipListEntry) throws WebClientResponseException {
        Object postBody = ipListEntry;
        // verify the required parameter 'type' is set
        if (type == null) {
            throw new WebClientResponseException("Missing the required parameter 'type' when calling addIpListEntry", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'ipListEntry' is set
        if (ipListEntry == null) {
            throw new WebClientResponseException("Missing the required parameter 'ipListEntry' when calling addIpListEntry", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("type", type);

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
        return apiClient.invokeAPI("/iplists/{type}", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Add a new IP list entry
     * Add an IP address or a CIDR network to a supported list
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipListEntry The ipListEntry parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> addIpListEntry(IPListType type, IPListEntry ipListEntry) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return addIpListEntryRequestCreation(type, ipListEntry).bodyToMono(localVarReturnType);
    }

    /**
     * Add a new IP list entry
     * Add an IP address or a CIDR network to a supported list
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipListEntry The ipListEntry parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> addIpListEntryWithHttpInfo(IPListType type, IPListEntry ipListEntry) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return addIpListEntryRequestCreation(type, ipListEntry).toEntity(localVarReturnType);
    }

    /**
     * Add a new IP list entry
     * Add an IP address or a CIDR network to a supported list
     * <p><b>201</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipListEntry The ipListEntry parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addIpListEntryWithResponseSpec(IPListType type, IPListEntry ipListEntry) throws WebClientResponseException {
        return addIpListEntryRequestCreation(type, ipListEntry);
    }
    /**
     * Delete IP list entry
     * Deletes an existing IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteIpListEntryRequestCreation(IPListType type, String ipornet) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'type' is set
        if (type == null) {
            throw new WebClientResponseException("Missing the required parameter 'type' when calling deleteIpListEntry", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'ipornet' is set
        if (ipornet == null) {
            throw new WebClientResponseException("Missing the required parameter 'ipornet' when calling deleteIpListEntry", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("type", type);
        pathParams.put("ipornet", ipornet);

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
        return apiClient.invokeAPI("/iplists/{type}/{ipornet}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete IP list entry
     * Deletes an existing IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> deleteIpListEntry(IPListType type, String ipornet) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteIpListEntryRequestCreation(type, ipornet).bodyToMono(localVarReturnType);
    }

    /**
     * Delete IP list entry
     * Deletes an existing IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> deleteIpListEntryWithHttpInfo(IPListType type, String ipornet) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return deleteIpListEntryRequestCreation(type, ipornet).toEntity(localVarReturnType);
    }

    /**
     * Delete IP list entry
     * Deletes an existing IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteIpListEntryWithResponseSpec(IPListType type, String ipornet) throws WebClientResponseException {
        return deleteIpListEntryRequestCreation(type, ipornet);
    }
    /**
     * Find entry by ipornet
     * Returns the entry with the given ipornet if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @return IPListEntry
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getIpListByIpornetRequestCreation(IPListType type, String ipornet) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'type' is set
        if (type == null) {
            throw new WebClientResponseException("Missing the required parameter 'type' when calling getIpListByIpornet", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'ipornet' is set
        if (ipornet == null) {
            throw new WebClientResponseException("Missing the required parameter 'ipornet' when calling getIpListByIpornet", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("type", type);
        pathParams.put("ipornet", ipornet);

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

        ParameterizedTypeReference<IPListEntry> localVarReturnType = new ParameterizedTypeReference<IPListEntry>() {};
        return apiClient.invokeAPI("/iplists/{type}/{ipornet}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Find entry by ipornet
     * Returns the entry with the given ipornet if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @return IPListEntry
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<IPListEntry> getIpListByIpornet(IPListType type, String ipornet) throws WebClientResponseException {
        ParameterizedTypeReference<IPListEntry> localVarReturnType = new ParameterizedTypeReference<IPListEntry>() {};
        return getIpListByIpornetRequestCreation(type, ipornet).bodyToMono(localVarReturnType);
    }

    /**
     * Find entry by ipornet
     * Returns the entry with the given ipornet if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @return ResponseEntity&lt;IPListEntry&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<IPListEntry>> getIpListByIpornetWithHttpInfo(IPListType type, String ipornet) throws WebClientResponseException {
        ParameterizedTypeReference<IPListEntry> localVarReturnType = new ParameterizedTypeReference<IPListEntry>() {};
        return getIpListByIpornetRequestCreation(type, ipornet).toEntity(localVarReturnType);
    }

    /**
     * Find entry by ipornet
     * Returns the entry with the given ipornet if it exists.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getIpListByIpornetWithResponseSpec(IPListType type, String ipornet) throws WebClientResponseException {
        return getIpListByIpornetRequestCreation(type, ipornet);
    }
    /**
     * Get IP list entries
     * Returns an array with one or more IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param filter restrict results to ipornet matching or starting with this filter
     * @param from ipornet to start from
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering entries by ipornet field. Default ASC
     * @return List&lt;IPListEntry&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getIpListEntriesRequestCreation(IPListType type, String filter, String from, Integer limit, String order) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'type' is set
        if (type == null) {
            throw new WebClientResponseException("Missing the required parameter 'type' when calling getIpListEntries", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("type", type);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "filter", filter));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "from", from));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "limit", limit));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "order", order));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "APIKeyAuth", "BearerAuth" };

        ParameterizedTypeReference<IPListEntry> localVarReturnType = new ParameterizedTypeReference<IPListEntry>() {};
        return apiClient.invokeAPI("/iplists/{type}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get IP list entries
     * Returns an array with one or more IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param filter restrict results to ipornet matching or starting with this filter
     * @param from ipornet to start from
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering entries by ipornet field. Default ASC
     * @return List&lt;IPListEntry&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<IPListEntry> getIpListEntries(IPListType type, String filter, String from, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<IPListEntry> localVarReturnType = new ParameterizedTypeReference<IPListEntry>() {};
        return getIpListEntriesRequestCreation(type, filter, from, limit, order).bodyToFlux(localVarReturnType);
    }

    /**
     * Get IP list entries
     * Returns an array with one or more IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param filter restrict results to ipornet matching or starting with this filter
     * @param from ipornet to start from
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering entries by ipornet field. Default ASC
     * @return ResponseEntity&lt;List&lt;IPListEntry&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<IPListEntry>>> getIpListEntriesWithHttpInfo(IPListType type, String filter, String from, Integer limit, String order) throws WebClientResponseException {
        ParameterizedTypeReference<IPListEntry> localVarReturnType = new ParameterizedTypeReference<IPListEntry>() {};
        return getIpListEntriesRequestCreation(type, filter, from, limit, order).toEntityList(localVarReturnType);
    }

    /**
     * Get IP list entries
     * Returns an array with one or more IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param filter restrict results to ipornet matching or starting with this filter
     * @param from ipornet to start from
     * @param limit The maximum number of items to return. Max value is 500, default is 100
     * @param order Ordering entries by ipornet field. Default ASC
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getIpListEntriesWithResponseSpec(IPListType type, String filter, String from, Integer limit, String order) throws WebClientResponseException {
        return getIpListEntriesRequestCreation(type, filter, from, limit, order);
    }
    /**
     * Update IP list entry
     * Updates an existing IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @param ipListEntry The ipListEntry parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateIpListEntryRequestCreation(IPListType type, String ipornet, IPListEntry ipListEntry) throws WebClientResponseException {
        Object postBody = ipListEntry;
        // verify the required parameter 'type' is set
        if (type == null) {
            throw new WebClientResponseException("Missing the required parameter 'type' when calling updateIpListEntry", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'ipornet' is set
        if (ipornet == null) {
            throw new WebClientResponseException("Missing the required parameter 'ipornet' when calling updateIpListEntry", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'ipListEntry' is set
        if (ipListEntry == null) {
            throw new WebClientResponseException("Missing the required parameter 'ipListEntry' when calling updateIpListEntry", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("type", type);
        pathParams.put("ipornet", ipornet);

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
        return apiClient.invokeAPI("/iplists/{type}/{ipornet}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update IP list entry
     * Updates an existing IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @param ipListEntry The ipListEntry parameter
     * @return ModelApiResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ModelApiResponse> updateIpListEntry(IPListType type, String ipornet, IPListEntry ipListEntry) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateIpListEntryRequestCreation(type, ipornet, ipListEntry).bodyToMono(localVarReturnType);
    }

    /**
     * Update IP list entry
     * Updates an existing IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @param ipListEntry The ipListEntry parameter
     * @return ResponseEntity&lt;ModelApiResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ModelApiResponse>> updateIpListEntryWithHttpInfo(IPListType type, String ipornet, IPListEntry ipListEntry) throws WebClientResponseException {
        ParameterizedTypeReference<ModelApiResponse> localVarReturnType = new ParameterizedTypeReference<ModelApiResponse>() {};
        return updateIpListEntryRequestCreation(type, ipornet, ipListEntry).toEntity(localVarReturnType);
    }

    /**
     * Update IP list entry
     * Updates an existing IP list entry
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Bad Request
     * <p><b>401</b> - Unauthorized
     * <p><b>403</b> - Forbidden
     * <p><b>404</b> - Not Found
     * <p><b>500</b> - Internal Server Error
     * <p><b>0</b> - Unexpected Error
     * @param type IP list type
     * @param ipornet The ipornet parameter
     * @param ipListEntry The ipListEntry parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateIpListEntryWithResponseSpec(IPListType type, String ipornet, IPListEntry ipListEntry) throws WebClientResponseException {
        return updateIpListEntryRequestCreation(type, ipornet, ipListEntry);
    }
}
