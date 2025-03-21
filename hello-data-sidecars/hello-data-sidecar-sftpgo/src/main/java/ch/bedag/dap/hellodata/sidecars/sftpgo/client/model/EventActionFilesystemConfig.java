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
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.EventActionFsCompress;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.FilesystemActionTypes;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.KeyValue;
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
 * EventActionFilesystemConfig
 */
@JsonPropertyOrder({
  EventActionFilesystemConfig.JSON_PROPERTY_TYPE,
  EventActionFilesystemConfig.JSON_PROPERTY_RENAMES,
  EventActionFilesystemConfig.JSON_PROPERTY_MKDIRS,
  EventActionFilesystemConfig.JSON_PROPERTY_DELETES,
  EventActionFilesystemConfig.JSON_PROPERTY_EXIST,
  EventActionFilesystemConfig.JSON_PROPERTY_COPY,
  EventActionFilesystemConfig.JSON_PROPERTY_COMPRESS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class EventActionFilesystemConfig {
  public static final String JSON_PROPERTY_TYPE = "type";
  private FilesystemActionTypes type;

  public static final String JSON_PROPERTY_RENAMES = "renames";
  private List<KeyValue> renames = new ArrayList<>();

  public static final String JSON_PROPERTY_MKDIRS = "mkdirs";
  private List<String> mkdirs = new ArrayList<>();

  public static final String JSON_PROPERTY_DELETES = "deletes";
  private List<String> deletes = new ArrayList<>();

  public static final String JSON_PROPERTY_EXIST = "exist";
  private List<String> exist = new ArrayList<>();

  public static final String JSON_PROPERTY_COPY = "copy";
  private List<KeyValue> copy = new ArrayList<>();

  public static final String JSON_PROPERTY_COMPRESS = "compress";
  private EventActionFsCompress compress;

  public EventActionFilesystemConfig() {
  }

  public EventActionFilesystemConfig type(FilesystemActionTypes type) {
    
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

  public FilesystemActionTypes getType() {
    return type;
  }


  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setType(FilesystemActionTypes type) {
    this.type = type;
  }

  public EventActionFilesystemConfig renames(List<KeyValue> renames) {
    
    this.renames = renames;
    return this;
  }

  public EventActionFilesystemConfig addRenamesItem(KeyValue renamesItem) {
    if (this.renames == null) {
      this.renames = new ArrayList<>();
    }
    this.renames.add(renamesItem);
    return this;
  }

  /**
   * Get renames
   * @return renames
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_RENAMES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<KeyValue> getRenames() {
    return renames;
  }


  @JsonProperty(JSON_PROPERTY_RENAMES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setRenames(List<KeyValue> renames) {
    this.renames = renames;
  }

  public EventActionFilesystemConfig mkdirs(List<String> mkdirs) {
    
    this.mkdirs = mkdirs;
    return this;
  }

  public EventActionFilesystemConfig addMkdirsItem(String mkdirsItem) {
    if (this.mkdirs == null) {
      this.mkdirs = new ArrayList<>();
    }
    this.mkdirs.add(mkdirsItem);
    return this;
  }

  /**
   * Get mkdirs
   * @return mkdirs
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MKDIRS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getMkdirs() {
    return mkdirs;
  }


  @JsonProperty(JSON_PROPERTY_MKDIRS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMkdirs(List<String> mkdirs) {
    this.mkdirs = mkdirs;
  }

  public EventActionFilesystemConfig deletes(List<String> deletes) {
    
    this.deletes = deletes;
    return this;
  }

  public EventActionFilesystemConfig addDeletesItem(String deletesItem) {
    if (this.deletes == null) {
      this.deletes = new ArrayList<>();
    }
    this.deletes.add(deletesItem);
    return this;
  }

  /**
   * Get deletes
   * @return deletes
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DELETES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getDeletes() {
    return deletes;
  }


  @JsonProperty(JSON_PROPERTY_DELETES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDeletes(List<String> deletes) {
    this.deletes = deletes;
  }

  public EventActionFilesystemConfig exist(List<String> exist) {
    
    this.exist = exist;
    return this;
  }

  public EventActionFilesystemConfig addExistItem(String existItem) {
    if (this.exist == null) {
      this.exist = new ArrayList<>();
    }
    this.exist.add(existItem);
    return this;
  }

  /**
   * Get exist
   * @return exist
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_EXIST)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getExist() {
    return exist;
  }


  @JsonProperty(JSON_PROPERTY_EXIST)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setExist(List<String> exist) {
    this.exist = exist;
  }

  public EventActionFilesystemConfig copy(List<KeyValue> copy) {
    
    this.copy = copy;
    return this;
  }

  public EventActionFilesystemConfig addCopyItem(KeyValue copyItem) {
    if (this.copy == null) {
      this.copy = new ArrayList<>();
    }
    this.copy.add(copyItem);
    return this;
  }

  /**
   * Get copy
   * @return copy
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_COPY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<KeyValue> getCopy() {
    return copy;
  }


  @JsonProperty(JSON_PROPERTY_COPY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCopy(List<KeyValue> copy) {
    this.copy = copy;
  }

  public EventActionFilesystemConfig compress(EventActionFsCompress compress) {
    
    this.compress = compress;
    return this;
  }

  /**
   * Get compress
   * @return compress
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_COMPRESS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public EventActionFsCompress getCompress() {
    return compress;
  }


  @JsonProperty(JSON_PROPERTY_COMPRESS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCompress(EventActionFsCompress compress) {
    this.compress = compress;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventActionFilesystemConfig eventActionFilesystemConfig = (EventActionFilesystemConfig) o;
    return Objects.equals(this.type, eventActionFilesystemConfig.type) &&
        Objects.equals(this.renames, eventActionFilesystemConfig.renames) &&
        Objects.equals(this.mkdirs, eventActionFilesystemConfig.mkdirs) &&
        Objects.equals(this.deletes, eventActionFilesystemConfig.deletes) &&
        Objects.equals(this.exist, eventActionFilesystemConfig.exist) &&
        Objects.equals(this.copy, eventActionFilesystemConfig.copy) &&
        Objects.equals(this.compress, eventActionFilesystemConfig.compress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, renames, mkdirs, deletes, exist, copy, compress);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EventActionFilesystemConfig {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    renames: ").append(toIndentedString(renames)).append("\n");
    sb.append("    mkdirs: ").append(toIndentedString(mkdirs)).append("\n");
    sb.append("    deletes: ").append(toIndentedString(deletes)).append("\n");
    sb.append("    exist: ").append(toIndentedString(exist)).append("\n");
    sb.append("    copy: ").append(toIndentedString(copy)).append("\n");
    sb.append("    compress: ").append(toIndentedString(compress)).append("\n");
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

