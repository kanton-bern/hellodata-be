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
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.DataProviderStatus;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.FTPServiceStatus;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.MFAStatus;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.SSHServiceStatus;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ServicesStatusDefender;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.ServicesStatusRateLimiters;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.WebDAVServiceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * ServicesStatus
 */
@JsonPropertyOrder({
  ServicesStatus.JSON_PROPERTY_SSH,
  ServicesStatus.JSON_PROPERTY_FTP,
  ServicesStatus.JSON_PROPERTY_WEBDAV,
  ServicesStatus.JSON_PROPERTY_DATA_PROVIDER,
  ServicesStatus.JSON_PROPERTY_DEFENDER,
  ServicesStatus.JSON_PROPERTY_MFA,
  ServicesStatus.JSON_PROPERTY_ALLOW_LIST,
  ServicesStatus.JSON_PROPERTY_RATE_LIMITERS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class ServicesStatus {
  public static final String JSON_PROPERTY_SSH = "ssh";
  private SSHServiceStatus ssh;

  public static final String JSON_PROPERTY_FTP = "ftp";
  private FTPServiceStatus ftp;

  public static final String JSON_PROPERTY_WEBDAV = "webdav";
  private WebDAVServiceStatus webdav;

  public static final String JSON_PROPERTY_DATA_PROVIDER = "data_provider";
  private DataProviderStatus dataProvider;

  public static final String JSON_PROPERTY_DEFENDER = "defender";
  private ServicesStatusDefender defender;

  public static final String JSON_PROPERTY_MFA = "mfa";
  private MFAStatus mfa;

  public static final String JSON_PROPERTY_ALLOW_LIST = "allow_list";
  private ServicesStatusDefender allowList;

  public static final String JSON_PROPERTY_RATE_LIMITERS = "rate_limiters";
  private ServicesStatusRateLimiters rateLimiters;

  public ServicesStatus() {
  }

  public ServicesStatus ssh(SSHServiceStatus ssh) {
    
    this.ssh = ssh;
    return this;
  }

  /**
   * Get ssh
   * @return ssh
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SSH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public SSHServiceStatus getSsh() {
    return ssh;
  }


  @JsonProperty(JSON_PROPERTY_SSH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSsh(SSHServiceStatus ssh) {
    this.ssh = ssh;
  }

  public ServicesStatus ftp(FTPServiceStatus ftp) {
    
    this.ftp = ftp;
    return this;
  }

  /**
   * Get ftp
   * @return ftp
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FTP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public FTPServiceStatus getFtp() {
    return ftp;
  }


  @JsonProperty(JSON_PROPERTY_FTP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFtp(FTPServiceStatus ftp) {
    this.ftp = ftp;
  }

  public ServicesStatus webdav(WebDAVServiceStatus webdav) {
    
    this.webdav = webdav;
    return this;
  }

  /**
   * Get webdav
   * @return webdav
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_WEBDAV)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public WebDAVServiceStatus getWebdav() {
    return webdav;
  }


  @JsonProperty(JSON_PROPERTY_WEBDAV)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setWebdav(WebDAVServiceStatus webdav) {
    this.webdav = webdav;
  }

  public ServicesStatus dataProvider(DataProviderStatus dataProvider) {
    
    this.dataProvider = dataProvider;
    return this;
  }

  /**
   * Get dataProvider
   * @return dataProvider
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DATA_PROVIDER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public DataProviderStatus getDataProvider() {
    return dataProvider;
  }


  @JsonProperty(JSON_PROPERTY_DATA_PROVIDER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDataProvider(DataProviderStatus dataProvider) {
    this.dataProvider = dataProvider;
  }

  public ServicesStatus defender(ServicesStatusDefender defender) {
    
    this.defender = defender;
    return this;
  }

  /**
   * Get defender
   * @return defender
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DEFENDER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public ServicesStatusDefender getDefender() {
    return defender;
  }


  @JsonProperty(JSON_PROPERTY_DEFENDER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDefender(ServicesStatusDefender defender) {
    this.defender = defender;
  }

  public ServicesStatus mfa(MFAStatus mfa) {
    
    this.mfa = mfa;
    return this;
  }

  /**
   * Get mfa
   * @return mfa
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MFA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public MFAStatus getMfa() {
    return mfa;
  }


  @JsonProperty(JSON_PROPERTY_MFA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMfa(MFAStatus mfa) {
    this.mfa = mfa;
  }

  public ServicesStatus allowList(ServicesStatusDefender allowList) {
    
    this.allowList = allowList;
    return this;
  }

  /**
   * Get allowList
   * @return allowList
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ALLOW_LIST)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public ServicesStatusDefender getAllowList() {
    return allowList;
  }


  @JsonProperty(JSON_PROPERTY_ALLOW_LIST)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAllowList(ServicesStatusDefender allowList) {
    this.allowList = allowList;
  }

  public ServicesStatus rateLimiters(ServicesStatusRateLimiters rateLimiters) {
    
    this.rateLimiters = rateLimiters;
    return this;
  }

  /**
   * Get rateLimiters
   * @return rateLimiters
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_RATE_LIMITERS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public ServicesStatusRateLimiters getRateLimiters() {
    return rateLimiters;
  }


  @JsonProperty(JSON_PROPERTY_RATE_LIMITERS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setRateLimiters(ServicesStatusRateLimiters rateLimiters) {
    this.rateLimiters = rateLimiters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServicesStatus servicesStatus = (ServicesStatus) o;
    return Objects.equals(this.ssh, servicesStatus.ssh) &&
        Objects.equals(this.ftp, servicesStatus.ftp) &&
        Objects.equals(this.webdav, servicesStatus.webdav) &&
        Objects.equals(this.dataProvider, servicesStatus.dataProvider) &&
        Objects.equals(this.defender, servicesStatus.defender) &&
        Objects.equals(this.mfa, servicesStatus.mfa) &&
        Objects.equals(this.allowList, servicesStatus.allowList) &&
        Objects.equals(this.rateLimiters, servicesStatus.rateLimiters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ssh, ftp, webdav, dataProvider, defender, mfa, allowList, rateLimiters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServicesStatus {\n");
    sb.append("    ssh: ").append(toIndentedString(ssh)).append("\n");
    sb.append("    ftp: ").append(toIndentedString(ftp)).append("\n");
    sb.append("    webdav: ").append(toIndentedString(webdav)).append("\n");
    sb.append("    dataProvider: ").append(toIndentedString(dataProvider)).append("\n");
    sb.append("    defender: ").append(toIndentedString(defender)).append("\n");
    sb.append("    mfa: ").append(toIndentedString(mfa)).append("\n");
    sb.append("    allowList: ").append(toIndentedString(allowList)).append("\n");
    sb.append("    rateLimiters: ").append(toIndentedString(rateLimiters)).append("\n");
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
