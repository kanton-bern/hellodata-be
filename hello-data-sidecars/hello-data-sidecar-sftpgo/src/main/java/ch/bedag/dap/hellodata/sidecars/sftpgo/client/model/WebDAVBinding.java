/*
 * SFTPGo
 * SFTPGo allows you to securely share your files over SFTP and optionally over HTTP/S, FTP/S and WebDAV as well. Several storage backends are supported and they are configurable per-user, so you can serve a local directory for a user and an S3 bucket (or part of it) for another one. SFTPGo also supports virtual folders, a virtual folder can use any of the supported storage backends. So you can have, for example, a user with the S3 backend mapping a Google Cloud Storage bucket (or part of it) on a specified path and an encrypted local filesystem on another one. Virtual folders can be private or shared among multiple users, for shared virtual folders you can define different quota limits for each user. SFTPGo supports groups to simplify the administration of multiple accounts by letting you assign settings once to a group, instead of multiple times to each individual user. The SFTPGo WebClient allows end users to change their credentials, browse and manage their files in the browser and setup two-factor authentication which works with Authy, Google Authenticator and other compatible apps. From the WebClient each authorized user can also create HTTP/S links to externally share files and folders securely, by setting limits to the number of downloads/uploads, protecting the share with a password, limiting access by source IP address, setting an automatic expiration date. 
 *
 * The version of the OpenAPI document: 2.6.4
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package ch.bedag.dap.hellodata.sidecars.sftpgo.client.model;

import java.util.Objects;
import java.util.Arrays;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.TLSVersions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * WebDAVBinding
 */
@JsonPropertyOrder({
  WebDAVBinding.JSON_PROPERTY_ADDRESS,
  WebDAVBinding.JSON_PROPERTY_PORT,
  WebDAVBinding.JSON_PROPERTY_ENABLE_HTTPS,
  WebDAVBinding.JSON_PROPERTY_MIN_TLS_VERSION,
  WebDAVBinding.JSON_PROPERTY_CLIENT_AUTH_TYPE,
  WebDAVBinding.JSON_PROPERTY_TLS_CIPHER_SUITES,
  WebDAVBinding.JSON_PROPERTY_PREFIX,
  WebDAVBinding.JSON_PROPERTY_PROXY_ALLOWED
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class WebDAVBinding {
  public static final String JSON_PROPERTY_ADDRESS = "address";
  private String address;

  public static final String JSON_PROPERTY_PORT = "port";
  private Integer port;

  public static final String JSON_PROPERTY_ENABLE_HTTPS = "enable_https";
  private Boolean enableHttps;

  public static final String JSON_PROPERTY_MIN_TLS_VERSION = "min_tls_version";
  private TLSVersions minTlsVersion;

  public static final String JSON_PROPERTY_CLIENT_AUTH_TYPE = "client_auth_type";
  private Integer clientAuthType;

  public static final String JSON_PROPERTY_TLS_CIPHER_SUITES = "tls_cipher_suites";
  private List<String> tlsCipherSuites = new ArrayList<>();

  public static final String JSON_PROPERTY_PREFIX = "prefix";
  private String prefix;

  public static final String JSON_PROPERTY_PROXY_ALLOWED = "proxy_allowed";
  private List<String> proxyAllowed = new ArrayList<>();

  public WebDAVBinding() {
  }

  public WebDAVBinding address(String address) {
    
    this.address = address;
    return this;
  }

  /**
   * TCP address the server listen on
   * @return address
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ADDRESS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getAddress() {
    return address;
  }


  @JsonProperty(JSON_PROPERTY_ADDRESS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAddress(String address) {
    this.address = address;
  }

  public WebDAVBinding port(Integer port) {
    
    this.port = port;
    return this;
  }

  /**
   * the port used for serving requests
   * @return port
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PORT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getPort() {
    return port;
  }


  @JsonProperty(JSON_PROPERTY_PORT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPort(Integer port) {
    this.port = port;
  }

  public WebDAVBinding enableHttps(Boolean enableHttps) {
    
    this.enableHttps = enableHttps;
    return this;
  }

  /**
   * Get enableHttps
   * @return enableHttps
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENABLE_HTTPS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getEnableHttps() {
    return enableHttps;
  }


  @JsonProperty(JSON_PROPERTY_ENABLE_HTTPS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnableHttps(Boolean enableHttps) {
    this.enableHttps = enableHttps;
  }

  public WebDAVBinding minTlsVersion(TLSVersions minTlsVersion) {
    
    this.minTlsVersion = minTlsVersion;
    return this;
  }

  /**
   * Get minTlsVersion
   * @return minTlsVersion
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MIN_TLS_VERSION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public TLSVersions getMinTlsVersion() {
    return minTlsVersion;
  }


  @JsonProperty(JSON_PROPERTY_MIN_TLS_VERSION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMinTlsVersion(TLSVersions minTlsVersion) {
    this.minTlsVersion = minTlsVersion;
  }

  public WebDAVBinding clientAuthType(Integer clientAuthType) {
    
    this.clientAuthType = clientAuthType;
    return this;
  }

  /**
   * 1 means that client certificate authentication is required in addition to HTTP basic authentication
   * @return clientAuthType
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CLIENT_AUTH_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getClientAuthType() {
    return clientAuthType;
  }


  @JsonProperty(JSON_PROPERTY_CLIENT_AUTH_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setClientAuthType(Integer clientAuthType) {
    this.clientAuthType = clientAuthType;
  }

  public WebDAVBinding tlsCipherSuites(List<String> tlsCipherSuites) {
    
    this.tlsCipherSuites = tlsCipherSuites;
    return this;
  }

  public WebDAVBinding addTlsCipherSuitesItem(String tlsCipherSuitesItem) {
    if (this.tlsCipherSuites == null) {
      this.tlsCipherSuites = new ArrayList<>();
    }
    this.tlsCipherSuites.add(tlsCipherSuitesItem);
    return this;
  }

  /**
   * List of supported cipher suites for TLS version 1.2. If empty  a default list of secure cipher suites is used, with a preference order based on hardware performance
   * @return tlsCipherSuites
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TLS_CIPHER_SUITES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getTlsCipherSuites() {
    return tlsCipherSuites;
  }


  @JsonProperty(JSON_PROPERTY_TLS_CIPHER_SUITES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTlsCipherSuites(List<String> tlsCipherSuites) {
    this.tlsCipherSuites = tlsCipherSuites;
  }

  public WebDAVBinding prefix(String prefix) {
    
    this.prefix = prefix;
    return this;
  }

  /**
   * Prefix for WebDAV resources, if empty WebDAV resources will be available at the &#x60;/&#x60; URI
   * @return prefix
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PREFIX)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getPrefix() {
    return prefix;
  }


  @JsonProperty(JSON_PROPERTY_PREFIX)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public WebDAVBinding proxyAllowed(List<String> proxyAllowed) {
    
    this.proxyAllowed = proxyAllowed;
    return this;
  }

  public WebDAVBinding addProxyAllowedItem(String proxyAllowedItem) {
    if (this.proxyAllowed == null) {
      this.proxyAllowed = new ArrayList<>();
    }
    this.proxyAllowed.add(proxyAllowedItem);
    return this;
  }

  /**
   * List of IP addresses and IP ranges allowed to set proxy headers
   * @return proxyAllowed
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PROXY_ALLOWED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getProxyAllowed() {
    return proxyAllowed;
  }


  @JsonProperty(JSON_PROPERTY_PROXY_ALLOWED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setProxyAllowed(List<String> proxyAllowed) {
    this.proxyAllowed = proxyAllowed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebDAVBinding webDAVBinding = (WebDAVBinding) o;
    return Objects.equals(this.address, webDAVBinding.address) &&
        Objects.equals(this.port, webDAVBinding.port) &&
        Objects.equals(this.enableHttps, webDAVBinding.enableHttps) &&
        Objects.equals(this.minTlsVersion, webDAVBinding.minTlsVersion) &&
        Objects.equals(this.clientAuthType, webDAVBinding.clientAuthType) &&
        Objects.equals(this.tlsCipherSuites, webDAVBinding.tlsCipherSuites) &&
        Objects.equals(this.prefix, webDAVBinding.prefix) &&
        Objects.equals(this.proxyAllowed, webDAVBinding.proxyAllowed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, port, enableHttps, minTlsVersion, clientAuthType, tlsCipherSuites, prefix, proxyAllowed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebDAVBinding {\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
    sb.append("    enableHttps: ").append(toIndentedString(enableHttps)).append("\n");
    sb.append("    minTlsVersion: ").append(toIndentedString(minTlsVersion)).append("\n");
    sb.append("    clientAuthType: ").append(toIndentedString(clientAuthType)).append("\n");
    sb.append("    tlsCipherSuites: ").append(toIndentedString(tlsCipherSuites)).append("\n");
    sb.append("    prefix: ").append(toIndentedString(prefix)).append("\n");
    sb.append("    proxyAllowed: ").append(toIndentedString(proxyAllowed)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

