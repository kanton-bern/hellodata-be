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
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.IPListMode;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.IPListType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * IPListEntry
 */
@JsonPropertyOrder({
  IPListEntry.JSON_PROPERTY_IPORNET,
  IPListEntry.JSON_PROPERTY_DESCRIPTION,
  IPListEntry.JSON_PROPERTY_TYPE,
  IPListEntry.JSON_PROPERTY_MODE,
  IPListEntry.JSON_PROPERTY_PROTOCOLS,
  IPListEntry.JSON_PROPERTY_CREATED_AT,
  IPListEntry.JSON_PROPERTY_UPDATED_AT
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class IPListEntry {
  public static final String JSON_PROPERTY_IPORNET = "ipornet";
  private String ipornet;

  public static final String JSON_PROPERTY_DESCRIPTION = "description";
  private String description;

  public static final String JSON_PROPERTY_TYPE = "type";
  private IPListType type;

  public static final String JSON_PROPERTY_MODE = "mode";
  private IPListMode mode;

  public static final String JSON_PROPERTY_PROTOCOLS = "protocols";
  private Integer protocols;

  public static final String JSON_PROPERTY_CREATED_AT = "created_at";
  private Long createdAt;

  public static final String JSON_PROPERTY_UPDATED_AT = "updated_at";
  private Long updatedAt;

  public IPListEntry() {
  }

  public IPListEntry ipornet(String ipornet) {
    
    this.ipornet = ipornet;
    return this;
  }

  /**
   * IP address or network in CIDR format, for example &#x60;192.168.1.2/32&#x60;, &#x60;192.168.0.0/24&#x60;, &#x60;2001:db8::/32&#x60;
   * @return ipornet
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_IPORNET)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getIpornet() {
    return ipornet;
  }


  @JsonProperty(JSON_PROPERTY_IPORNET)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIpornet(String ipornet) {
    this.ipornet = ipornet;
  }

  public IPListEntry description(String description) {
    
    this.description = description;
    return this;
  }

  /**
   * optional description
   * @return description
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getDescription() {
    return description;
  }


  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDescription(String description) {
    this.description = description;
  }

  public IPListEntry type(IPListType type) {
    
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public IPListType getType() {
    return type;
  }


  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setType(IPListType type) {
    this.type = type;
  }

  public IPListEntry mode(IPListMode mode) {
    
    this.mode = mode;
    return this;
  }

  /**
   * Get mode
   * @return mode
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MODE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public IPListMode getMode() {
    return mode;
  }


  @JsonProperty(JSON_PROPERTY_MODE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMode(IPListMode mode) {
    this.mode = mode;
  }

  public IPListEntry protocols(Integer protocols) {
    
    this.protocols = protocols;
    return this;
  }

  /**
   * Defines the protocol the entry applies to. &#x60;0&#x60; means all the supported protocols, 1 SSH, 2 FTP, 4 WebDAV, 8 HTTP. Protocols can be combined, for example 3 means SSH and FTP
   * @return protocols
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PROTOCOLS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getProtocols() {
    return protocols;
  }


  @JsonProperty(JSON_PROPERTY_PROTOCOLS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setProtocols(Integer protocols) {
    this.protocols = protocols;
  }

  public IPListEntry createdAt(Long createdAt) {
    
    this.createdAt = createdAt;
    return this;
  }

  /**
   * creation time as unix timestamp in milliseconds
   * @return createdAt
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CREATED_AT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getCreatedAt() {
    return createdAt;
  }


  @JsonProperty(JSON_PROPERTY_CREATED_AT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }

  public IPListEntry updatedAt(Long updatedAt) {
    
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * last update time as unix timestamp in millisecond
   * @return updatedAt
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_UPDATED_AT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getUpdatedAt() {
    return updatedAt;
  }


  @JsonProperty(JSON_PROPERTY_UPDATED_AT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUpdatedAt(Long updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IPListEntry ipListEntry = (IPListEntry) o;
    return Objects.equals(this.ipornet, ipListEntry.ipornet) &&
        Objects.equals(this.description, ipListEntry.description) &&
        Objects.equals(this.type, ipListEntry.type) &&
        Objects.equals(this.mode, ipListEntry.mode) &&
        Objects.equals(this.protocols, ipListEntry.protocols) &&
        Objects.equals(this.createdAt, ipListEntry.createdAt) &&
        Objects.equals(this.updatedAt, ipListEntry.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ipornet, description, type, mode, protocols, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IPListEntry {\n");
    sb.append("    ipornet: ").append(toIndentedString(ipornet)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    mode: ").append(toIndentedString(mode)).append("\n");
    sb.append("    protocols: ").append(toIndentedString(protocols)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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
