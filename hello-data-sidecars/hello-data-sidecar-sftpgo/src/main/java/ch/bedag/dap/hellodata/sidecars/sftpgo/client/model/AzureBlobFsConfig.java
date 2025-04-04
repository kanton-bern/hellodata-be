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
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.Secret;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Azure Blob Storage configuration details
 */
@JsonPropertyOrder({
  AzureBlobFsConfig.JSON_PROPERTY_CONTAINER,
  AzureBlobFsConfig.JSON_PROPERTY_ACCOUNT_NAME,
  AzureBlobFsConfig.JSON_PROPERTY_ACCOUNT_KEY,
  AzureBlobFsConfig.JSON_PROPERTY_SAS_URL,
  AzureBlobFsConfig.JSON_PROPERTY_ENDPOINT,
  AzureBlobFsConfig.JSON_PROPERTY_UPLOAD_PART_SIZE,
  AzureBlobFsConfig.JSON_PROPERTY_UPLOAD_CONCURRENCY,
  AzureBlobFsConfig.JSON_PROPERTY_DOWNLOAD_PART_SIZE,
  AzureBlobFsConfig.JSON_PROPERTY_DOWNLOAD_CONCURRENCY,
  AzureBlobFsConfig.JSON_PROPERTY_ACCESS_TIER,
  AzureBlobFsConfig.JSON_PROPERTY_KEY_PREFIX,
  AzureBlobFsConfig.JSON_PROPERTY_USE_EMULATOR
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class AzureBlobFsConfig {
  public static final String JSON_PROPERTY_CONTAINER = "container";
  private String container;

  public static final String JSON_PROPERTY_ACCOUNT_NAME = "account_name";
  private String accountName;

  public static final String JSON_PROPERTY_ACCOUNT_KEY = "account_key";
  private Secret accountKey;

  public static final String JSON_PROPERTY_SAS_URL = "sas_url";
  private Secret sasUrl;

  public static final String JSON_PROPERTY_ENDPOINT = "endpoint";
  private String endpoint;

  public static final String JSON_PROPERTY_UPLOAD_PART_SIZE = "upload_part_size";
  private Integer uploadPartSize;

  public static final String JSON_PROPERTY_UPLOAD_CONCURRENCY = "upload_concurrency";
  private Integer uploadConcurrency;

  public static final String JSON_PROPERTY_DOWNLOAD_PART_SIZE = "download_part_size";
  private Integer downloadPartSize;

  public static final String JSON_PROPERTY_DOWNLOAD_CONCURRENCY = "download_concurrency";
  private Integer downloadConcurrency;

  /**
   * Gets or Sets accessTier
   */
  public enum AccessTierEnum {
    EMPTY(""),
    
    ARCHIVE("Archive"),
    
    HOT("Hot"),
    
    COOL("Cool");

    private String value;

    AccessTierEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static AccessTierEnum fromValue(String value) {
      for (AccessTierEnum b : AccessTierEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_ACCESS_TIER = "access_tier";
  private AccessTierEnum accessTier;

  public static final String JSON_PROPERTY_KEY_PREFIX = "key_prefix";
  private String keyPrefix;

  public static final String JSON_PROPERTY_USE_EMULATOR = "use_emulator";
  private Boolean useEmulator;

  public AzureBlobFsConfig() {
  }

  public AzureBlobFsConfig container(String container) {
    
    this.container = container;
    return this;
  }

  /**
   * Get container
   * @return container
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CONTAINER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getContainer() {
    return container;
  }


  @JsonProperty(JSON_PROPERTY_CONTAINER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setContainer(String container) {
    this.container = container;
  }

  public AzureBlobFsConfig accountName(String accountName) {
    
    this.accountName = accountName;
    return this;
  }

  /**
   * Storage Account Name, leave blank to use SAS URL
   * @return accountName
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ACCOUNT_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getAccountName() {
    return accountName;
  }


  @JsonProperty(JSON_PROPERTY_ACCOUNT_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public AzureBlobFsConfig accountKey(Secret accountKey) {
    
    this.accountKey = accountKey;
    return this;
  }

  /**
   * Get accountKey
   * @return accountKey
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ACCOUNT_KEY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Secret getAccountKey() {
    return accountKey;
  }


  @JsonProperty(JSON_PROPERTY_ACCOUNT_KEY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAccountKey(Secret accountKey) {
    this.accountKey = accountKey;
  }

  public AzureBlobFsConfig sasUrl(Secret sasUrl) {
    
    this.sasUrl = sasUrl;
    return this;
  }

  /**
   * Get sasUrl
   * @return sasUrl
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SAS_URL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Secret getSasUrl() {
    return sasUrl;
  }


  @JsonProperty(JSON_PROPERTY_SAS_URL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSasUrl(Secret sasUrl) {
    this.sasUrl = sasUrl;
  }

  public AzureBlobFsConfig endpoint(String endpoint) {
    
    this.endpoint = endpoint;
    return this;
  }

  /**
   * optional endpoint. Default is \&quot;blob.core.windows.net\&quot;. If you use the emulator the endpoint must include the protocol, for example \&quot;http://127.0.0.1:10000\&quot;
   * @return endpoint
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENDPOINT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getEndpoint() {
    return endpoint;
  }


  @JsonProperty(JSON_PROPERTY_ENDPOINT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public AzureBlobFsConfig uploadPartSize(Integer uploadPartSize) {
    
    this.uploadPartSize = uploadPartSize;
    return this;
  }

  /**
   * the buffer size (in MB) to use for multipart uploads. If this value is set to zero, the default value (5MB) will be used.
   * @return uploadPartSize
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_UPLOAD_PART_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getUploadPartSize() {
    return uploadPartSize;
  }


  @JsonProperty(JSON_PROPERTY_UPLOAD_PART_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUploadPartSize(Integer uploadPartSize) {
    this.uploadPartSize = uploadPartSize;
  }

  public AzureBlobFsConfig uploadConcurrency(Integer uploadConcurrency) {
    
    this.uploadConcurrency = uploadConcurrency;
    return this;
  }

  /**
   * the number of parts to upload in parallel. If this value is set to zero, the default value (5) will be used
   * @return uploadConcurrency
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_UPLOAD_CONCURRENCY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getUploadConcurrency() {
    return uploadConcurrency;
  }


  @JsonProperty(JSON_PROPERTY_UPLOAD_CONCURRENCY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUploadConcurrency(Integer uploadConcurrency) {
    this.uploadConcurrency = uploadConcurrency;
  }

  public AzureBlobFsConfig downloadPartSize(Integer downloadPartSize) {
    
    this.downloadPartSize = downloadPartSize;
    return this;
  }

  /**
   * the buffer size (in MB) to use for multipart downloads. If this value is set to zero, the default value (5MB) will be used.
   * @return downloadPartSize
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DOWNLOAD_PART_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getDownloadPartSize() {
    return downloadPartSize;
  }


  @JsonProperty(JSON_PROPERTY_DOWNLOAD_PART_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDownloadPartSize(Integer downloadPartSize) {
    this.downloadPartSize = downloadPartSize;
  }

  public AzureBlobFsConfig downloadConcurrency(Integer downloadConcurrency) {
    
    this.downloadConcurrency = downloadConcurrency;
    return this;
  }

  /**
   * the number of parts to download in parallel. If this value is set to zero, the default value (5) will be used
   * @return downloadConcurrency
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DOWNLOAD_CONCURRENCY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getDownloadConcurrency() {
    return downloadConcurrency;
  }


  @JsonProperty(JSON_PROPERTY_DOWNLOAD_CONCURRENCY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDownloadConcurrency(Integer downloadConcurrency) {
    this.downloadConcurrency = downloadConcurrency;
  }

  public AzureBlobFsConfig accessTier(AccessTierEnum accessTier) {
    
    this.accessTier = accessTier;
    return this;
  }

  /**
   * Get accessTier
   * @return accessTier
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ACCESS_TIER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public AccessTierEnum getAccessTier() {
    return accessTier;
  }


  @JsonProperty(JSON_PROPERTY_ACCESS_TIER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAccessTier(AccessTierEnum accessTier) {
    this.accessTier = accessTier;
  }

  public AzureBlobFsConfig keyPrefix(String keyPrefix) {
    
    this.keyPrefix = keyPrefix;
    return this;
  }

  /**
   * key_prefix is similar to a chroot directory for a local filesystem. If specified the user will only see contents that starts with this prefix and so you can restrict access to a specific virtual folder. The prefix, if not empty, must not start with \&quot;/\&quot; and must end with \&quot;/\&quot;. If empty the whole container contents will be available
   * @return keyPrefix
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_KEY_PREFIX)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getKeyPrefix() {
    return keyPrefix;
  }


  @JsonProperty(JSON_PROPERTY_KEY_PREFIX)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setKeyPrefix(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public AzureBlobFsConfig useEmulator(Boolean useEmulator) {
    
    this.useEmulator = useEmulator;
    return this;
  }

  /**
   * Get useEmulator
   * @return useEmulator
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_USE_EMULATOR)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getUseEmulator() {
    return useEmulator;
  }


  @JsonProperty(JSON_PROPERTY_USE_EMULATOR)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUseEmulator(Boolean useEmulator) {
    this.useEmulator = useEmulator;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AzureBlobFsConfig azureBlobFsConfig = (AzureBlobFsConfig) o;
    return Objects.equals(this.container, azureBlobFsConfig.container) &&
        Objects.equals(this.accountName, azureBlobFsConfig.accountName) &&
        Objects.equals(this.accountKey, azureBlobFsConfig.accountKey) &&
        Objects.equals(this.sasUrl, azureBlobFsConfig.sasUrl) &&
        Objects.equals(this.endpoint, azureBlobFsConfig.endpoint) &&
        Objects.equals(this.uploadPartSize, azureBlobFsConfig.uploadPartSize) &&
        Objects.equals(this.uploadConcurrency, azureBlobFsConfig.uploadConcurrency) &&
        Objects.equals(this.downloadPartSize, azureBlobFsConfig.downloadPartSize) &&
        Objects.equals(this.downloadConcurrency, azureBlobFsConfig.downloadConcurrency) &&
        Objects.equals(this.accessTier, azureBlobFsConfig.accessTier) &&
        Objects.equals(this.keyPrefix, azureBlobFsConfig.keyPrefix) &&
        Objects.equals(this.useEmulator, azureBlobFsConfig.useEmulator);
  }

  @Override
  public int hashCode() {
    return Objects.hash(container, accountName, accountKey, sasUrl, endpoint, uploadPartSize, uploadConcurrency, downloadPartSize, downloadConcurrency, accessTier, keyPrefix, useEmulator);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AzureBlobFsConfig {\n");
    sb.append("    container: ").append(toIndentedString(container)).append("\n");
    sb.append("    accountName: ").append(toIndentedString(accountName)).append("\n");
    sb.append("    accountKey: ").append(toIndentedString(accountKey)).append("\n");
    sb.append("    sasUrl: ").append(toIndentedString(sasUrl)).append("\n");
    sb.append("    endpoint: ").append(toIndentedString(endpoint)).append("\n");
    sb.append("    uploadPartSize: ").append(toIndentedString(uploadPartSize)).append("\n");
    sb.append("    uploadConcurrency: ").append(toIndentedString(uploadConcurrency)).append("\n");
    sb.append("    downloadPartSize: ").append(toIndentedString(downloadPartSize)).append("\n");
    sb.append("    downloadConcurrency: ").append(toIndentedString(downloadConcurrency)).append("\n");
    sb.append("    accessTier: ").append(toIndentedString(accessTier)).append("\n");
    sb.append("    keyPrefix: ").append(toIndentedString(keyPrefix)).append("\n");
    sb.append("    useEmulator: ").append(toIndentedString(useEmulator)).append("\n");
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

