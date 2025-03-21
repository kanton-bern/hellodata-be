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
 * Transfer
 */
@JsonPropertyOrder({
  Transfer.JSON_PROPERTY_OPERATION_TYPE,
  Transfer.JSON_PROPERTY_PATH,
  Transfer.JSON_PROPERTY_START_TIME,
  Transfer.JSON_PROPERTY_SIZE
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class Transfer {
  /**
   * Operations:   * &#x60;upload&#x60;   * &#x60;download&#x60; 
   */
  public enum OperationTypeEnum {
    UPLOAD("upload"),
    
    DOWNLOAD("download");

    private String value;

    OperationTypeEnum(String value) {
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
    public static OperationTypeEnum fromValue(String value) {
      for (OperationTypeEnum b : OperationTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_OPERATION_TYPE = "operation_type";
  private OperationTypeEnum operationType;

  public static final String JSON_PROPERTY_PATH = "path";
  private String path;

  public static final String JSON_PROPERTY_START_TIME = "start_time";
  private Long startTime;

  public static final String JSON_PROPERTY_SIZE = "size";
  private Long size;

  public Transfer() {
  }

  public Transfer operationType(OperationTypeEnum operationType) {
    
    this.operationType = operationType;
    return this;
  }

  /**
   * Operations:   * &#x60;upload&#x60;   * &#x60;download&#x60; 
   * @return operationType
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OPERATION_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public OperationTypeEnum getOperationType() {
    return operationType;
  }


  @JsonProperty(JSON_PROPERTY_OPERATION_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOperationType(OperationTypeEnum operationType) {
    this.operationType = operationType;
  }

  public Transfer path(String path) {
    
    this.path = path;
    return this;
  }

  /**
   * file path for the upload/download
   * @return path
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PATH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getPath() {
    return path;
  }


  @JsonProperty(JSON_PROPERTY_PATH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPath(String path) {
    this.path = path;
  }

  public Transfer startTime(Long startTime) {
    
    this.startTime = startTime;
    return this;
  }

  /**
   * start time as unix timestamp in milliseconds
   * @return startTime
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_START_TIME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getStartTime() {
    return startTime;
  }


  @JsonProperty(JSON_PROPERTY_START_TIME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Transfer size(Long size) {
    
    this.size = size;
    return this;
  }

  /**
   * bytes transferred
   * @return size
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getSize() {
    return size;
  }


  @JsonProperty(JSON_PROPERTY_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSize(Long size) {
    this.size = size;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Transfer transfer = (Transfer) o;
    return Objects.equals(this.operationType, transfer.operationType) &&
        Objects.equals(this.path, transfer.path) &&
        Objects.equals(this.startTime, transfer.startTime) &&
        Objects.equals(this.size, transfer.size);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operationType, path, startTime, size);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Transfer {\n");
    sb.append("    operationType: ").append(toIndentedString(operationType)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
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

