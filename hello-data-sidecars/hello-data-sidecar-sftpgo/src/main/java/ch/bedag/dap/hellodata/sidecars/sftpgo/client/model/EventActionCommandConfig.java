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
 * EventActionCommandConfig
 */
@JsonPropertyOrder({
  EventActionCommandConfig.JSON_PROPERTY_CMD,
  EventActionCommandConfig.JSON_PROPERTY_ARGS,
  EventActionCommandConfig.JSON_PROPERTY_TIMEOUT,
  EventActionCommandConfig.JSON_PROPERTY_ENV_VARS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class EventActionCommandConfig {
  public static final String JSON_PROPERTY_CMD = "cmd";
  private String cmd;

  public static final String JSON_PROPERTY_ARGS = "args";
  private List<String> args = new ArrayList<>();

  public static final String JSON_PROPERTY_TIMEOUT = "timeout";
  private Integer timeout;

  public static final String JSON_PROPERTY_ENV_VARS = "env_vars";
  private List<KeyValue> envVars = new ArrayList<>();

  public EventActionCommandConfig() {
  }

  public EventActionCommandConfig cmd(String cmd) {
    
    this.cmd = cmd;
    return this;
  }

  /**
   * absolute path to the command to execute
   * @return cmd
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CMD)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getCmd() {
    return cmd;
  }


  @JsonProperty(JSON_PROPERTY_CMD)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCmd(String cmd) {
    this.cmd = cmd;
  }

  public EventActionCommandConfig args(List<String> args) {
    
    this.args = args;
    return this;
  }

  public EventActionCommandConfig addArgsItem(String argsItem) {
    if (this.args == null) {
      this.args = new ArrayList<>();
    }
    this.args.add(argsItem);
    return this;
  }

  /**
   * command line arguments
   * @return args
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ARGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getArgs() {
    return args;
  }


  @JsonProperty(JSON_PROPERTY_ARGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setArgs(List<String> args) {
    this.args = args;
  }

  public EventActionCommandConfig timeout(Integer timeout) {
    
    this.timeout = timeout;
    return this;
  }

  /**
   * Get timeout
   * minimum: 1
   * maximum: 120
   * @return timeout
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TIMEOUT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getTimeout() {
    return timeout;
  }


  @JsonProperty(JSON_PROPERTY_TIMEOUT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public EventActionCommandConfig envVars(List<KeyValue> envVars) {
    
    this.envVars = envVars;
    return this;
  }

  public EventActionCommandConfig addEnvVarsItem(KeyValue envVarsItem) {
    if (this.envVars == null) {
      this.envVars = new ArrayList<>();
    }
    this.envVars.add(envVarsItem);
    return this;
  }

  /**
   * Get envVars
   * @return envVars
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENV_VARS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<KeyValue> getEnvVars() {
    return envVars;
  }


  @JsonProperty(JSON_PROPERTY_ENV_VARS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnvVars(List<KeyValue> envVars) {
    this.envVars = envVars;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventActionCommandConfig eventActionCommandConfig = (EventActionCommandConfig) o;
    return Objects.equals(this.cmd, eventActionCommandConfig.cmd) &&
        Objects.equals(this.args, eventActionCommandConfig.args) &&
        Objects.equals(this.timeout, eventActionCommandConfig.timeout) &&
        Objects.equals(this.envVars, eventActionCommandConfig.envVars);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cmd, args, timeout, envVars);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EventActionCommandConfig {\n");
    sb.append("    cmd: ").append(toIndentedString(cmd)).append("\n");
    sb.append("    args: ").append(toIndentedString(args)).append("\n");
    sb.append("    timeout: ").append(toIndentedString(timeout)).append("\n");
    sb.append("    envVars: ").append(toIndentedString(envVars)).append("\n");
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
