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
 * OSFsConfig
 */
@JsonPropertyOrder({
  OSFsConfig.JSON_PROPERTY_READ_BUFFER_SIZE,
  OSFsConfig.JSON_PROPERTY_WRITE_BUFFER_SIZE
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class OSFsConfig {
  public static final String JSON_PROPERTY_READ_BUFFER_SIZE = "read_buffer_size";
  private Integer readBufferSize;

  public static final String JSON_PROPERTY_WRITE_BUFFER_SIZE = "write_buffer_size";
  private Integer writeBufferSize;

  public OSFsConfig() {
  }

  public OSFsConfig readBufferSize(Integer readBufferSize) {
    
    this.readBufferSize = readBufferSize;
    return this;
  }

  /**
   * The read buffer size, as MB, to use for downloads. 0 means no buffering, that&#39;s fine in most use cases.
   * minimum: 0
   * maximum: 10
   * @return readBufferSize
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_READ_BUFFER_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getReadBufferSize() {
    return readBufferSize;
  }


  @JsonProperty(JSON_PROPERTY_READ_BUFFER_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setReadBufferSize(Integer readBufferSize) {
    this.readBufferSize = readBufferSize;
  }

  public OSFsConfig writeBufferSize(Integer writeBufferSize) {
    
    this.writeBufferSize = writeBufferSize;
    return this;
  }

  /**
   * The write buffer size, as MB, to use for uploads. 0 means no buffering, that&#39;s fine in most use cases.
   * minimum: 0
   * maximum: 10
   * @return writeBufferSize
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_WRITE_BUFFER_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getWriteBufferSize() {
    return writeBufferSize;
  }


  @JsonProperty(JSON_PROPERTY_WRITE_BUFFER_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setWriteBufferSize(Integer writeBufferSize) {
    this.writeBufferSize = writeBufferSize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OSFsConfig osFsConfig = (OSFsConfig) o;
    return Objects.equals(this.readBufferSize, osFsConfig.readBufferSize) &&
        Objects.equals(this.writeBufferSize, osFsConfig.writeBufferSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(readBufferSize, writeBufferSize);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OSFsConfig {\n");
    sb.append("    readBufferSize: ").append(toIndentedString(readBufferSize)).append("\n");
    sb.append("    writeBufferSize: ").append(toIndentedString(writeBufferSize)).append("\n");
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

