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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * QuotaUsage
 */
@JsonPropertyOrder({
  QuotaUsage.JSON_PROPERTY_USED_QUOTA_SIZE,
  QuotaUsage.JSON_PROPERTY_USED_QUOTA_FILES
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class QuotaUsage {
  public static final String JSON_PROPERTY_USED_QUOTA_SIZE = "used_quota_size";
  private Long usedQuotaSize;

  public static final String JSON_PROPERTY_USED_QUOTA_FILES = "used_quota_files";
  private Integer usedQuotaFiles;

  public QuotaUsage() {
  }

  public QuotaUsage usedQuotaSize(Long usedQuotaSize) {
    
    this.usedQuotaSize = usedQuotaSize;
    return this;
  }

  /**
   * Get usedQuotaSize
   * @return usedQuotaSize
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_USED_QUOTA_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getUsedQuotaSize() {
    return usedQuotaSize;
  }


  @JsonProperty(JSON_PROPERTY_USED_QUOTA_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUsedQuotaSize(Long usedQuotaSize) {
    this.usedQuotaSize = usedQuotaSize;
  }

  public QuotaUsage usedQuotaFiles(Integer usedQuotaFiles) {
    
    this.usedQuotaFiles = usedQuotaFiles;
    return this;
  }

  /**
   * Get usedQuotaFiles
   * @return usedQuotaFiles
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_USED_QUOTA_FILES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getUsedQuotaFiles() {
    return usedQuotaFiles;
  }


  @JsonProperty(JSON_PROPERTY_USED_QUOTA_FILES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUsedQuotaFiles(Integer usedQuotaFiles) {
    this.usedQuotaFiles = usedQuotaFiles;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuotaUsage quotaUsage = (QuotaUsage) o;
    return Objects.equals(this.usedQuotaSize, quotaUsage.usedQuotaSize) &&
        Objects.equals(this.usedQuotaFiles, quotaUsage.usedQuotaFiles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(usedQuotaSize, usedQuotaFiles);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuotaUsage {\n");
    sb.append("    usedQuotaSize: ").append(toIndentedString(usedQuotaSize)).append("\n");
    sb.append("    usedQuotaFiles: ").append(toIndentedString(usedQuotaFiles)).append("\n");
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

