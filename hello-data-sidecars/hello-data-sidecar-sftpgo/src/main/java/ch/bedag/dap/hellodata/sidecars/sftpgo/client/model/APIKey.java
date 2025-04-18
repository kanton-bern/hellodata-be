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
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.APIKeyScope;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * APIKey
 */
@JsonPropertyOrder({
  APIKey.JSON_PROPERTY_ID,
  APIKey.JSON_PROPERTY_NAME,
  APIKey.JSON_PROPERTY_KEY,
  APIKey.JSON_PROPERTY_SCOPE,
  APIKey.JSON_PROPERTY_CREATED_AT,
  APIKey.JSON_PROPERTY_UPDATED_AT,
  APIKey.JSON_PROPERTY_LAST_USE_AT,
  APIKey.JSON_PROPERTY_EXPIRES_AT,
  APIKey.JSON_PROPERTY_DESCRIPTION,
  APIKey.JSON_PROPERTY_USER,
  APIKey.JSON_PROPERTY_ADMIN
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class APIKey {
  public static final String JSON_PROPERTY_ID = "id";
  private String id;

  public static final String JSON_PROPERTY_NAME = "name";
  private String name;

  public static final String JSON_PROPERTY_KEY = "key";
  private String key;

  public static final String JSON_PROPERTY_SCOPE = "scope";
  private APIKeyScope scope;

  public static final String JSON_PROPERTY_CREATED_AT = "created_at";
  private Long createdAt;

  public static final String JSON_PROPERTY_UPDATED_AT = "updated_at";
  private Long updatedAt;

  public static final String JSON_PROPERTY_LAST_USE_AT = "last_use_at";
  private Long lastUseAt;

  public static final String JSON_PROPERTY_EXPIRES_AT = "expires_at";
  private Long expiresAt;

  public static final String JSON_PROPERTY_DESCRIPTION = "description";
  private String description;

  public static final String JSON_PROPERTY_USER = "user";
  private String user;

  public static final String JSON_PROPERTY_ADMIN = "admin";
  private String admin;

  public APIKey() {
  }

  public APIKey id(String id) {
    
    this.id = id;
    return this;
  }

  /**
   * unique key identifier
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

  public APIKey name(String name) {
    
    this.name = name;
    return this;
  }

  /**
   * User friendly key name
   * @return name
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getName() {
    return name;
  }


  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setName(String name) {
    this.name = name;
  }

  public APIKey key(String key) {
    
    this.key = key;
    return this;
  }

  /**
   * We store the hash of the key. This is just like a password. For security reasons this field is omitted when you search/get API keys
   * @return key
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_KEY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getKey() {
    return key;
  }


  @JsonProperty(JSON_PROPERTY_KEY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setKey(String key) {
    this.key = key;
  }

  public APIKey scope(APIKeyScope scope) {
    
    this.scope = scope;
    return this;
  }

  /**
   * Get scope
   * @return scope
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SCOPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public APIKeyScope getScope() {
    return scope;
  }


  @JsonProperty(JSON_PROPERTY_SCOPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setScope(APIKeyScope scope) {
    this.scope = scope;
  }

  public APIKey createdAt(Long createdAt) {
    
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

  public APIKey updatedAt(Long updatedAt) {
    
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * last update time as unix timestamp in milliseconds
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

  public APIKey lastUseAt(Long lastUseAt) {
    
    this.lastUseAt = lastUseAt;
    return this;
  }

  /**
   * last use time as unix timestamp in milliseconds. It is saved at most once every 10 minutes
   * @return lastUseAt
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_LAST_USE_AT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getLastUseAt() {
    return lastUseAt;
  }


  @JsonProperty(JSON_PROPERTY_LAST_USE_AT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setLastUseAt(Long lastUseAt) {
    this.lastUseAt = lastUseAt;
  }

  public APIKey expiresAt(Long expiresAt) {
    
    this.expiresAt = expiresAt;
    return this;
  }

  /**
   * expiration time as unix timestamp in milliseconds
   * @return expiresAt
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_EXPIRES_AT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getExpiresAt() {
    return expiresAt;
  }


  @JsonProperty(JSON_PROPERTY_EXPIRES_AT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setExpiresAt(Long expiresAt) {
    this.expiresAt = expiresAt;
  }

  public APIKey description(String description) {
    
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

  public APIKey user(String user) {
    
    this.user = user;
    return this;
  }

  /**
   * username associated with this API key. If empty and the scope is \&quot;user scope\&quot; the key can impersonate any user
   * @return user
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_USER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getUser() {
    return user;
  }


  @JsonProperty(JSON_PROPERTY_USER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUser(String user) {
    this.user = user;
  }

  public APIKey admin(String admin) {
    
    this.admin = admin;
    return this;
  }

  /**
   * admin associated with this API key. If empty and the scope is \&quot;admin scope\&quot; the key can impersonate any admin
   * @return admin
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ADMIN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getAdmin() {
    return admin;
  }


  @JsonProperty(JSON_PROPERTY_ADMIN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAdmin(String admin) {
    this.admin = admin;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    APIKey apIKey = (APIKey) o;
    return Objects.equals(this.id, apIKey.id) &&
        Objects.equals(this.name, apIKey.name) &&
        Objects.equals(this.key, apIKey.key) &&
        Objects.equals(this.scope, apIKey.scope) &&
        Objects.equals(this.createdAt, apIKey.createdAt) &&
        Objects.equals(this.updatedAt, apIKey.updatedAt) &&
        Objects.equals(this.lastUseAt, apIKey.lastUseAt) &&
        Objects.equals(this.expiresAt, apIKey.expiresAt) &&
        Objects.equals(this.description, apIKey.description) &&
        Objects.equals(this.user, apIKey.user) &&
        Objects.equals(this.admin, apIKey.admin);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, key, scope, createdAt, updatedAt, lastUseAt, expiresAt, description, user, admin);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIKey {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    key: ").append("*").append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("    lastUseAt: ").append(toIndentedString(lastUseAt)).append("\n");
    sb.append("    expiresAt: ").append(toIndentedString(expiresAt)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    admin: ").append(toIndentedString(admin)).append("\n");
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

