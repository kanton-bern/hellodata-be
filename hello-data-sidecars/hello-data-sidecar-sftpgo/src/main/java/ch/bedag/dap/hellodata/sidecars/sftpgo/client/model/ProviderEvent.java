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
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ProviderEventAction;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ProviderEventObjectType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * ProviderEvent
 */
@JsonPropertyOrder({
  ProviderEvent.JSON_PROPERTY_ID,
  ProviderEvent.JSON_PROPERTY_TIMESTAMP,
  ProviderEvent.JSON_PROPERTY_ACTION,
  ProviderEvent.JSON_PROPERTY_USERNAME,
  ProviderEvent.JSON_PROPERTY_IP,
  ProviderEvent.JSON_PROPERTY_OBJECT_TYPE,
  ProviderEvent.JSON_PROPERTY_OBJECT_NAME,
  ProviderEvent.JSON_PROPERTY_OBJECT_DATA,
  ProviderEvent.JSON_PROPERTY_ROLE,
  ProviderEvent.JSON_PROPERTY_INSTANCE_ID
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class ProviderEvent {
  public static final String JSON_PROPERTY_ID = "id";
  private String id;

  public static final String JSON_PROPERTY_TIMESTAMP = "timestamp";
  private Long timestamp;

  public static final String JSON_PROPERTY_ACTION = "action";
  private ProviderEventAction action;

  public static final String JSON_PROPERTY_USERNAME = "username";
  private String username;

  public static final String JSON_PROPERTY_IP = "ip";
  private String ip;

  public static final String JSON_PROPERTY_OBJECT_TYPE = "object_type";
  private ProviderEventObjectType objectType;

  public static final String JSON_PROPERTY_OBJECT_NAME = "object_name";
  private String objectName;

  public static final String JSON_PROPERTY_OBJECT_DATA = "object_data";
  private byte[] objectData;

  public static final String JSON_PROPERTY_ROLE = "role";
  private String role;

  public static final String JSON_PROPERTY_INSTANCE_ID = "instance_id";
  private String instanceId;

  public ProviderEvent() {
  }

  public ProviderEvent id(String id) {
    
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getId() {
    return id;
  }


  @JsonProperty(JSON_PROPERTY_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setId(String id) {
    this.id = id;
  }

  public ProviderEvent timestamp(Long timestamp) {
    
    this.timestamp = timestamp;
    return this;
  }

  /**
   * unix timestamp in nanoseconds
   * @return timestamp
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TIMESTAMP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getTimestamp() {
    return timestamp;
  }


  @JsonProperty(JSON_PROPERTY_TIMESTAMP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public ProviderEvent action(ProviderEventAction action) {
    
    this.action = action;
    return this;
  }

  /**
   * Get action
   * @return action
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ACTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public ProviderEventAction getAction() {
    return action;
  }


  @JsonProperty(JSON_PROPERTY_ACTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAction(ProviderEventAction action) {
    this.action = action;
  }

  public ProviderEvent username(String username) {
    
    this.username = username;
    return this;
  }

  /**
   * Get username
   * @return username
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_USERNAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getUsername() {
    return username;
  }


  @JsonProperty(JSON_PROPERTY_USERNAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUsername(String username) {
    this.username = username;
  }

  public ProviderEvent ip(String ip) {
    
    this.ip = ip;
    return this;
  }

  /**
   * Get ip
   * @return ip
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_IP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getIp() {
    return ip;
  }


  @JsonProperty(JSON_PROPERTY_IP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIp(String ip) {
    this.ip = ip;
  }

  public ProviderEvent objectType(ProviderEventObjectType objectType) {
    
    this.objectType = objectType;
    return this;
  }

  /**
   * Get objectType
   * @return objectType
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OBJECT_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public ProviderEventObjectType getObjectType() {
    return objectType;
  }


  @JsonProperty(JSON_PROPERTY_OBJECT_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setObjectType(ProviderEventObjectType objectType) {
    this.objectType = objectType;
  }

  public ProviderEvent objectName(String objectName) {
    
    this.objectName = objectName;
    return this;
  }

  /**
   * Get objectName
   * @return objectName
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OBJECT_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getObjectName() {
    return objectName;
  }


  @JsonProperty(JSON_PROPERTY_OBJECT_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }

  public ProviderEvent objectData(byte[] objectData) {
    
    this.objectData = objectData;
    return this;
  }

  /**
   * base64 of the JSON serialized object with sensitive fields removed
   * @return objectData
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OBJECT_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public byte[] getObjectData() {
    return objectData;
  }


  @JsonProperty(JSON_PROPERTY_OBJECT_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setObjectData(byte[] objectData) {
    this.objectData = objectData;
  }

  public ProviderEvent role(String role) {
    
    this.role = role;
    return this;
  }

  /**
   * Get role
   * @return role
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ROLE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getRole() {
    return role;
  }


  @JsonProperty(JSON_PROPERTY_ROLE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setRole(String role) {
    this.role = role;
  }

  public ProviderEvent instanceId(String instanceId) {
    
    this.instanceId = instanceId;
    return this;
  }

  /**
   * Get instanceId
   * @return instanceId
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_INSTANCE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getInstanceId() {
    return instanceId;
  }


  @JsonProperty(JSON_PROPERTY_INSTANCE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProviderEvent providerEvent = (ProviderEvent) o;
    return Objects.equals(this.id, providerEvent.id) &&
        Objects.equals(this.timestamp, providerEvent.timestamp) &&
        Objects.equals(this.action, providerEvent.action) &&
        Objects.equals(this.username, providerEvent.username) &&
        Objects.equals(this.ip, providerEvent.ip) &&
        Objects.equals(this.objectType, providerEvent.objectType) &&
        Objects.equals(this.objectName, providerEvent.objectName) &&
        Arrays.equals(this.objectData, providerEvent.objectData) &&
        Objects.equals(this.role, providerEvent.role) &&
        Objects.equals(this.instanceId, providerEvent.instanceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, timestamp, action, username, ip, objectType, objectName, Arrays.hashCode(objectData), role, instanceId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProviderEvent {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    ip: ").append(toIndentedString(ip)).append("\n");
    sb.append("    objectType: ").append(toIndentedString(objectType)).append("\n");
    sb.append("    objectName: ").append(toIndentedString(objectName)).append("\n");
    sb.append("    objectData: ").append(toIndentedString(objectData)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    instanceId: ").append(toIndentedString(instanceId)).append("\n");
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

