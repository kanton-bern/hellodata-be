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
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.BandwidthLimit;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.HooksFilter;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.LoginMethods;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.MFAProtocols;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.PatternsFilter;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.RecoveryCode;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.SupportedProtocols;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.TimePeriod;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.UserTOTPConfig;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.UserType;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.WebClientOptions;
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
 * UserFilters
 */
@JsonPropertyOrder({
  UserFilters.JSON_PROPERTY_ALLOWED_IP,
  UserFilters.JSON_PROPERTY_DENIED_IP,
  UserFilters.JSON_PROPERTY_DENIED_LOGIN_METHODS,
  UserFilters.JSON_PROPERTY_DENIED_PROTOCOLS,
  UserFilters.JSON_PROPERTY_FILE_PATTERNS,
  UserFilters.JSON_PROPERTY_MAX_UPLOAD_FILE_SIZE,
  UserFilters.JSON_PROPERTY_TLS_USERNAME,
  UserFilters.JSON_PROPERTY_TLS_CERTS,
  UserFilters.JSON_PROPERTY_HOOKS,
  UserFilters.JSON_PROPERTY_DISABLE_FS_CHECKS,
  UserFilters.JSON_PROPERTY_WEB_CLIENT,
  UserFilters.JSON_PROPERTY_ALLOW_API_KEY_AUTH,
  UserFilters.JSON_PROPERTY_USER_TYPE,
  UserFilters.JSON_PROPERTY_BANDWIDTH_LIMITS,
  UserFilters.JSON_PROPERTY_EXTERNAL_AUTH_CACHE_TIME,
  UserFilters.JSON_PROPERTY_START_DIRECTORY,
  UserFilters.JSON_PROPERTY_TWO_FACTOR_PROTOCOLS,
  UserFilters.JSON_PROPERTY_FTP_SECURITY,
  UserFilters.JSON_PROPERTY_IS_ANONYMOUS,
  UserFilters.JSON_PROPERTY_DEFAULT_SHARES_EXPIRATION,
  UserFilters.JSON_PROPERTY_MAX_SHARES_EXPIRATION,
  UserFilters.JSON_PROPERTY_PASSWORD_EXPIRATION,
  UserFilters.JSON_PROPERTY_PASSWORD_STRENGTH,
  UserFilters.JSON_PROPERTY_ACCESS_TIME,
  UserFilters.JSON_PROPERTY_REQUIRE_PASSWORD_CHANGE,
  UserFilters.JSON_PROPERTY_TOTP_CONFIG,
  UserFilters.JSON_PROPERTY_RECOVERY_CODES
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class UserFilters {
  public static final String JSON_PROPERTY_ALLOWED_IP = "allowed_ip";
  private List<String> allowedIp = new ArrayList<>();

  public static final String JSON_PROPERTY_DENIED_IP = "denied_ip";
  private List<String> deniedIp = new ArrayList<>();

  public static final String JSON_PROPERTY_DENIED_LOGIN_METHODS = "denied_login_methods";
  private List<LoginMethods> deniedLoginMethods = new ArrayList<>();

  public static final String JSON_PROPERTY_DENIED_PROTOCOLS = "denied_protocols";
  private List<SupportedProtocols> deniedProtocols = new ArrayList<>();

  public static final String JSON_PROPERTY_FILE_PATTERNS = "file_patterns";
  private List<PatternsFilter> filePatterns = new ArrayList<>();

  public static final String JSON_PROPERTY_MAX_UPLOAD_FILE_SIZE = "max_upload_file_size";
  private Long maxUploadFileSize;

  public static final String JSON_PROPERTY_TLS_USERNAME = "tls_username";
  private String tlsUsername;

  public static final String JSON_PROPERTY_TLS_CERTS = "tls_certs";
  private List<String> tlsCerts = new ArrayList<>();

  public static final String JSON_PROPERTY_HOOKS = "hooks";
  private HooksFilter hooks;

  public static final String JSON_PROPERTY_DISABLE_FS_CHECKS = "disable_fs_checks";
  private Boolean disableFsChecks;

  public static final String JSON_PROPERTY_WEB_CLIENT = "web_client";
  private List<WebClientOptions> webClient = new ArrayList<>();

  public static final String JSON_PROPERTY_ALLOW_API_KEY_AUTH = "allow_api_key_auth";
  private Boolean allowApiKeyAuth;

  public static final String JSON_PROPERTY_USER_TYPE = "user_type";
  private UserType userType;

  public static final String JSON_PROPERTY_BANDWIDTH_LIMITS = "bandwidth_limits";
  private List<BandwidthLimit> bandwidthLimits = new ArrayList<>();

  public static final String JSON_PROPERTY_EXTERNAL_AUTH_CACHE_TIME = "external_auth_cache_time";
  private Integer externalAuthCacheTime;

  public static final String JSON_PROPERTY_START_DIRECTORY = "start_directory";
  private String startDirectory;

  public static final String JSON_PROPERTY_TWO_FACTOR_PROTOCOLS = "two_factor_protocols";
  private List<MFAProtocols> twoFactorProtocols = new ArrayList<>();

  /**
   * Set to &#x60;1&#x60; to require TLS for both data and control connection. his setting is useful if you want to allow both encrypted and plain text FTP sessions globally and then you want to require encrypted sessions on a per-user basis. It has no effect if TLS is already required for all users in the configuration file.
   */
  public enum FtpSecurityEnum {
    NUMBER_0(0),
    
    NUMBER_1(1);

    private Integer value;

    FtpSecurityEnum(Integer value) {
      this.value = value;
    }

    @JsonValue
    public Integer getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static FtpSecurityEnum fromValue(Integer value) {
      for (FtpSecurityEnum b : FtpSecurityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_FTP_SECURITY = "ftp_security";
  private FtpSecurityEnum ftpSecurity;

  public static final String JSON_PROPERTY_IS_ANONYMOUS = "is_anonymous";
  private Boolean isAnonymous;

  public static final String JSON_PROPERTY_DEFAULT_SHARES_EXPIRATION = "default_shares_expiration";
  private Integer defaultSharesExpiration;

  public static final String JSON_PROPERTY_MAX_SHARES_EXPIRATION = "max_shares_expiration";
  private Integer maxSharesExpiration;

  public static final String JSON_PROPERTY_PASSWORD_EXPIRATION = "password_expiration";
  private Integer passwordExpiration;

  public static final String JSON_PROPERTY_PASSWORD_STRENGTH = "password_strength";
  private Integer passwordStrength;

  public static final String JSON_PROPERTY_ACCESS_TIME = "access_time";
  private List<TimePeriod> accessTime = new ArrayList<>();

  public static final String JSON_PROPERTY_REQUIRE_PASSWORD_CHANGE = "require_password_change";
  private Boolean requirePasswordChange;

  public static final String JSON_PROPERTY_TOTP_CONFIG = "totp_config";
  private UserTOTPConfig totpConfig;

  public static final String JSON_PROPERTY_RECOVERY_CODES = "recovery_codes";
  private List<RecoveryCode> recoveryCodes = new ArrayList<>();

  public UserFilters() {
  }

  public UserFilters allowedIp(List<String> allowedIp) {
    
    this.allowedIp = allowedIp;
    return this;
  }

  public UserFilters addAllowedIpItem(String allowedIpItem) {
    if (this.allowedIp == null) {
      this.allowedIp = new ArrayList<>();
    }
    this.allowedIp.add(allowedIpItem);
    return this;
  }

  /**
   * only clients connecting from these IP/Mask are allowed. IP/Mask must be in CIDR notation as defined in RFC 4632 and RFC 4291, for example \&quot;192.0.2.0/24\&quot; or \&quot;2001:db8::/32\&quot;
   * @return allowedIp
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ALLOWED_IP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getAllowedIp() {
    return allowedIp;
  }


  @JsonProperty(JSON_PROPERTY_ALLOWED_IP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAllowedIp(List<String> allowedIp) {
    this.allowedIp = allowedIp;
  }

  public UserFilters deniedIp(List<String> deniedIp) {
    
    this.deniedIp = deniedIp;
    return this;
  }

  public UserFilters addDeniedIpItem(String deniedIpItem) {
    if (this.deniedIp == null) {
      this.deniedIp = new ArrayList<>();
    }
    this.deniedIp.add(deniedIpItem);
    return this;
  }

  /**
   * clients connecting from these IP/Mask are not allowed. Denied rules are evaluated before allowed ones
   * @return deniedIp
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DENIED_IP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getDeniedIp() {
    return deniedIp;
  }


  @JsonProperty(JSON_PROPERTY_DENIED_IP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDeniedIp(List<String> deniedIp) {
    this.deniedIp = deniedIp;
  }

  public UserFilters deniedLoginMethods(List<LoginMethods> deniedLoginMethods) {
    
    this.deniedLoginMethods = deniedLoginMethods;
    return this;
  }

  public UserFilters addDeniedLoginMethodsItem(LoginMethods deniedLoginMethodsItem) {
    if (this.deniedLoginMethods == null) {
      this.deniedLoginMethods = new ArrayList<>();
    }
    this.deniedLoginMethods.add(deniedLoginMethodsItem);
    return this;
  }

  /**
   * if null or empty any available login method is allowed
   * @return deniedLoginMethods
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DENIED_LOGIN_METHODS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<LoginMethods> getDeniedLoginMethods() {
    return deniedLoginMethods;
  }


  @JsonProperty(JSON_PROPERTY_DENIED_LOGIN_METHODS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDeniedLoginMethods(List<LoginMethods> deniedLoginMethods) {
    this.deniedLoginMethods = deniedLoginMethods;
  }

  public UserFilters deniedProtocols(List<SupportedProtocols> deniedProtocols) {
    
    this.deniedProtocols = deniedProtocols;
    return this;
  }

  public UserFilters addDeniedProtocolsItem(SupportedProtocols deniedProtocolsItem) {
    if (this.deniedProtocols == null) {
      this.deniedProtocols = new ArrayList<>();
    }
    this.deniedProtocols.add(deniedProtocolsItem);
    return this;
  }

  /**
   * if null or empty any available protocol is allowed
   * @return deniedProtocols
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DENIED_PROTOCOLS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<SupportedProtocols> getDeniedProtocols() {
    return deniedProtocols;
  }


  @JsonProperty(JSON_PROPERTY_DENIED_PROTOCOLS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDeniedProtocols(List<SupportedProtocols> deniedProtocols) {
    this.deniedProtocols = deniedProtocols;
  }

  public UserFilters filePatterns(List<PatternsFilter> filePatterns) {
    
    this.filePatterns = filePatterns;
    return this;
  }

  public UserFilters addFilePatternsItem(PatternsFilter filePatternsItem) {
    if (this.filePatterns == null) {
      this.filePatterns = new ArrayList<>();
    }
    this.filePatterns.add(filePatternsItem);
    return this;
  }

  /**
   * filters based on shell like file patterns. These restrictions do not apply to files listing for performance reasons, so a denied file cannot be downloaded/overwritten/renamed but it will still be in the list of files. Please note that these restrictions can be easily bypassed
   * @return filePatterns
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FILE_PATTERNS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<PatternsFilter> getFilePatterns() {
    return filePatterns;
  }


  @JsonProperty(JSON_PROPERTY_FILE_PATTERNS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFilePatterns(List<PatternsFilter> filePatterns) {
    this.filePatterns = filePatterns;
  }

  public UserFilters maxUploadFileSize(Long maxUploadFileSize) {
    
    this.maxUploadFileSize = maxUploadFileSize;
    return this;
  }

  /**
   * maximum allowed size, as bytes, for a single file upload. The upload will be aborted if/when the size of the file being sent exceeds this limit. 0 means unlimited. This restriction does not apply for SSH system commands such as &#x60;git&#x60; and &#x60;rsync&#x60;
   * @return maxUploadFileSize
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MAX_UPLOAD_FILE_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getMaxUploadFileSize() {
    return maxUploadFileSize;
  }


  @JsonProperty(JSON_PROPERTY_MAX_UPLOAD_FILE_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMaxUploadFileSize(Long maxUploadFileSize) {
    this.maxUploadFileSize = maxUploadFileSize;
  }

  public UserFilters tlsUsername(String tlsUsername) {
    
    this.tlsUsername = tlsUsername;
    return this;
  }

  /**
   * defines the TLS certificate field to use as username. For FTP clients it must match the name provided using the \&quot;USER\&quot; command. For WebDAV, if no username is provided, the CN will be used as username. For WebDAV clients it must match the implicit or provided username. Ignored if mutual TLS is disabled. Currently the only supported value is &#x60;CommonName&#x60;
   * @return tlsUsername
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TLS_USERNAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getTlsUsername() {
    return tlsUsername;
  }


  @JsonProperty(JSON_PROPERTY_TLS_USERNAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTlsUsername(String tlsUsername) {
    this.tlsUsername = tlsUsername;
  }

  public UserFilters tlsCerts(List<String> tlsCerts) {
    
    this.tlsCerts = tlsCerts;
    return this;
  }

  public UserFilters addTlsCertsItem(String tlsCertsItem) {
    if (this.tlsCerts == null) {
      this.tlsCerts = new ArrayList<>();
    }
    this.tlsCerts.add(tlsCertsItem);
    return this;
  }

  /**
   * Get tlsCerts
   * @return tlsCerts
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TLS_CERTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getTlsCerts() {
    return tlsCerts;
  }


  @JsonProperty(JSON_PROPERTY_TLS_CERTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTlsCerts(List<String> tlsCerts) {
    this.tlsCerts = tlsCerts;
  }

  public UserFilters hooks(HooksFilter hooks) {
    
    this.hooks = hooks;
    return this;
  }

  /**
   * Get hooks
   * @return hooks
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_HOOKS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public HooksFilter getHooks() {
    return hooks;
  }


  @JsonProperty(JSON_PROPERTY_HOOKS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setHooks(HooksFilter hooks) {
    this.hooks = hooks;
  }

  public UserFilters disableFsChecks(Boolean disableFsChecks) {
    
    this.disableFsChecks = disableFsChecks;
    return this;
  }

  /**
   * Disable checks for existence and automatic creation of home directory and virtual folders. SFTPGo requires that the user&#39;s home directory, virtual folder root, and intermediate paths to virtual folders exist to work properly. If you already know that the required directories exist, disabling these checks will speed up login. You could, for example, disable these checks after the first login
   * @return disableFsChecks
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DISABLE_FS_CHECKS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getDisableFsChecks() {
    return disableFsChecks;
  }


  @JsonProperty(JSON_PROPERTY_DISABLE_FS_CHECKS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDisableFsChecks(Boolean disableFsChecks) {
    this.disableFsChecks = disableFsChecks;
  }

  public UserFilters webClient(List<WebClientOptions> webClient) {
    
    this.webClient = webClient;
    return this;
  }

  public UserFilters addWebClientItem(WebClientOptions webClientItem) {
    if (this.webClient == null) {
      this.webClient = new ArrayList<>();
    }
    this.webClient.add(webClientItem);
    return this;
  }

  /**
   * WebClient/user REST API related configuration options
   * @return webClient
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_WEB_CLIENT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<WebClientOptions> getWebClient() {
    return webClient;
  }


  @JsonProperty(JSON_PROPERTY_WEB_CLIENT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setWebClient(List<WebClientOptions> webClient) {
    this.webClient = webClient;
  }

  public UserFilters allowApiKeyAuth(Boolean allowApiKeyAuth) {
    
    this.allowApiKeyAuth = allowApiKeyAuth;
    return this;
  }

  /**
   * API key authentication allows to impersonate this user with an API key
   * @return allowApiKeyAuth
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ALLOW_API_KEY_AUTH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getAllowApiKeyAuth() {
    return allowApiKeyAuth;
  }


  @JsonProperty(JSON_PROPERTY_ALLOW_API_KEY_AUTH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAllowApiKeyAuth(Boolean allowApiKeyAuth) {
    this.allowApiKeyAuth = allowApiKeyAuth;
  }

  public UserFilters userType(UserType userType) {
    
    this.userType = userType;
    return this;
  }

  /**
   * Get userType
   * @return userType
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_USER_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public UserType getUserType() {
    return userType;
  }


  @JsonProperty(JSON_PROPERTY_USER_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUserType(UserType userType) {
    this.userType = userType;
  }

  public UserFilters bandwidthLimits(List<BandwidthLimit> bandwidthLimits) {
    
    this.bandwidthLimits = bandwidthLimits;
    return this;
  }

  public UserFilters addBandwidthLimitsItem(BandwidthLimit bandwidthLimitsItem) {
    if (this.bandwidthLimits == null) {
      this.bandwidthLimits = new ArrayList<>();
    }
    this.bandwidthLimits.add(bandwidthLimitsItem);
    return this;
  }

  /**
   * Get bandwidthLimits
   * @return bandwidthLimits
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_BANDWIDTH_LIMITS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<BandwidthLimit> getBandwidthLimits() {
    return bandwidthLimits;
  }


  @JsonProperty(JSON_PROPERTY_BANDWIDTH_LIMITS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setBandwidthLimits(List<BandwidthLimit> bandwidthLimits) {
    this.bandwidthLimits = bandwidthLimits;
  }

  public UserFilters externalAuthCacheTime(Integer externalAuthCacheTime) {
    
    this.externalAuthCacheTime = externalAuthCacheTime;
    return this;
  }

  /**
   * Defines the cache time, in seconds, for users authenticated using an external auth hook. 0 means no cache
   * @return externalAuthCacheTime
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_EXTERNAL_AUTH_CACHE_TIME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getExternalAuthCacheTime() {
    return externalAuthCacheTime;
  }


  @JsonProperty(JSON_PROPERTY_EXTERNAL_AUTH_CACHE_TIME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setExternalAuthCacheTime(Integer externalAuthCacheTime) {
    this.externalAuthCacheTime = externalAuthCacheTime;
  }

  public UserFilters startDirectory(String startDirectory) {
    
    this.startDirectory = startDirectory;
    return this;
  }

  /**
   * Specifies an alternate starting directory. If not set, the default is \&quot;/\&quot;. This option is supported for SFTP/SCP, FTP and HTTP (WebClient/REST API) protocols. Relative paths will use this directory as base.
   * @return startDirectory
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_START_DIRECTORY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getStartDirectory() {
    return startDirectory;
  }


  @JsonProperty(JSON_PROPERTY_START_DIRECTORY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStartDirectory(String startDirectory) {
    this.startDirectory = startDirectory;
  }

  public UserFilters twoFactorProtocols(List<MFAProtocols> twoFactorProtocols) {
    
    this.twoFactorProtocols = twoFactorProtocols;
    return this;
  }

  public UserFilters addTwoFactorProtocolsItem(MFAProtocols twoFactorProtocolsItem) {
    if (this.twoFactorProtocols == null) {
      this.twoFactorProtocols = new ArrayList<>();
    }
    this.twoFactorProtocols.add(twoFactorProtocolsItem);
    return this;
  }

  /**
   * Defines protocols that require two factor authentication
   * @return twoFactorProtocols
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TWO_FACTOR_PROTOCOLS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<MFAProtocols> getTwoFactorProtocols() {
    return twoFactorProtocols;
  }


  @JsonProperty(JSON_PROPERTY_TWO_FACTOR_PROTOCOLS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTwoFactorProtocols(List<MFAProtocols> twoFactorProtocols) {
    this.twoFactorProtocols = twoFactorProtocols;
  }

  public UserFilters ftpSecurity(FtpSecurityEnum ftpSecurity) {
    
    this.ftpSecurity = ftpSecurity;
    return this;
  }

  /**
   * Set to &#x60;1&#x60; to require TLS for both data and control connection. his setting is useful if you want to allow both encrypted and plain text FTP sessions globally and then you want to require encrypted sessions on a per-user basis. It has no effect if TLS is already required for all users in the configuration file.
   * @return ftpSecurity
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FTP_SECURITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public FtpSecurityEnum getFtpSecurity() {
    return ftpSecurity;
  }


  @JsonProperty(JSON_PROPERTY_FTP_SECURITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFtpSecurity(FtpSecurityEnum ftpSecurity) {
    this.ftpSecurity = ftpSecurity;
  }

  public UserFilters isAnonymous(Boolean isAnonymous) {
    
    this.isAnonymous = isAnonymous;
    return this;
  }

  /**
   * If enabled the user can login with any password or no password at all. Anonymous users are supported for FTP and WebDAV protocols and permissions will be automatically set to \&quot;list\&quot; and \&quot;download\&quot; (read only)
   * @return isAnonymous
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_IS_ANONYMOUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getIsAnonymous() {
    return isAnonymous;
  }


  @JsonProperty(JSON_PROPERTY_IS_ANONYMOUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIsAnonymous(Boolean isAnonymous) {
    this.isAnonymous = isAnonymous;
  }

  public UserFilters defaultSharesExpiration(Integer defaultSharesExpiration) {
    
    this.defaultSharesExpiration = defaultSharesExpiration;
    return this;
  }

  /**
   * Defines the default expiration for newly created shares as number of days. 0 means no expiration
   * @return defaultSharesExpiration
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DEFAULT_SHARES_EXPIRATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getDefaultSharesExpiration() {
    return defaultSharesExpiration;
  }


  @JsonProperty(JSON_PROPERTY_DEFAULT_SHARES_EXPIRATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDefaultSharesExpiration(Integer defaultSharesExpiration) {
    this.defaultSharesExpiration = defaultSharesExpiration;
  }

  public UserFilters maxSharesExpiration(Integer maxSharesExpiration) {
    
    this.maxSharesExpiration = maxSharesExpiration;
    return this;
  }

  /**
   * Defines the maximum allowed expiration, as a number of days, when a user creates or updates a share. 0 means no expiration
   * @return maxSharesExpiration
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MAX_SHARES_EXPIRATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getMaxSharesExpiration() {
    return maxSharesExpiration;
  }


  @JsonProperty(JSON_PROPERTY_MAX_SHARES_EXPIRATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMaxSharesExpiration(Integer maxSharesExpiration) {
    this.maxSharesExpiration = maxSharesExpiration;
  }

  public UserFilters passwordExpiration(Integer passwordExpiration) {
    
    this.passwordExpiration = passwordExpiration;
    return this;
  }

  /**
   * The password expires after the defined number of days. 0 means no expiration
   * @return passwordExpiration
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PASSWORD_EXPIRATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getPasswordExpiration() {
    return passwordExpiration;
  }


  @JsonProperty(JSON_PROPERTY_PASSWORD_EXPIRATION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPasswordExpiration(Integer passwordExpiration) {
    this.passwordExpiration = passwordExpiration;
  }

  public UserFilters passwordStrength(Integer passwordStrength) {
    
    this.passwordStrength = passwordStrength;
    return this;
  }

  /**
   * Defines the minimum password strength. 0 means disabled, any password will be accepted. Values in the 50-70 range are suggested for common use cases
   * @return passwordStrength
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PASSWORD_STRENGTH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getPasswordStrength() {
    return passwordStrength;
  }


  @JsonProperty(JSON_PROPERTY_PASSWORD_STRENGTH)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPasswordStrength(Integer passwordStrength) {
    this.passwordStrength = passwordStrength;
  }

  public UserFilters accessTime(List<TimePeriod> accessTime) {
    
    this.accessTime = accessTime;
    return this;
  }

  public UserFilters addAccessTimeItem(TimePeriod accessTimeItem) {
    if (this.accessTime == null) {
      this.accessTime = new ArrayList<>();
    }
    this.accessTime.add(accessTimeItem);
    return this;
  }

  /**
   * Get accessTime
   * @return accessTime
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ACCESS_TIME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<TimePeriod> getAccessTime() {
    return accessTime;
  }


  @JsonProperty(JSON_PROPERTY_ACCESS_TIME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAccessTime(List<TimePeriod> accessTime) {
    this.accessTime = accessTime;
  }

  public UserFilters requirePasswordChange(Boolean requirePasswordChange) {
    
    this.requirePasswordChange = requirePasswordChange;
    return this;
  }

  /**
   * User must change password from WebClient/REST API at next login
   * @return requirePasswordChange
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_REQUIRE_PASSWORD_CHANGE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getRequirePasswordChange() {
    return requirePasswordChange;
  }


  @JsonProperty(JSON_PROPERTY_REQUIRE_PASSWORD_CHANGE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setRequirePasswordChange(Boolean requirePasswordChange) {
    this.requirePasswordChange = requirePasswordChange;
  }

  public UserFilters totpConfig(UserTOTPConfig totpConfig) {
    
    this.totpConfig = totpConfig;
    return this;
  }

  /**
   * Get totpConfig
   * @return totpConfig
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TOTP_CONFIG)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public UserTOTPConfig getTotpConfig() {
    return totpConfig;
  }


  @JsonProperty(JSON_PROPERTY_TOTP_CONFIG)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTotpConfig(UserTOTPConfig totpConfig) {
    this.totpConfig = totpConfig;
  }

  public UserFilters recoveryCodes(List<RecoveryCode> recoveryCodes) {
    
    this.recoveryCodes = recoveryCodes;
    return this;
  }

  public UserFilters addRecoveryCodesItem(RecoveryCode recoveryCodesItem) {
    if (this.recoveryCodes == null) {
      this.recoveryCodes = new ArrayList<>();
    }
    this.recoveryCodes.add(recoveryCodesItem);
    return this;
  }

  /**
   * Get recoveryCodes
   * @return recoveryCodes
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_RECOVERY_CODES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<RecoveryCode> getRecoveryCodes() {
    return recoveryCodes;
  }


  @JsonProperty(JSON_PROPERTY_RECOVERY_CODES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setRecoveryCodes(List<RecoveryCode> recoveryCodes) {
    this.recoveryCodes = recoveryCodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserFilters userFilters = (UserFilters) o;
    return Objects.equals(this.allowedIp, userFilters.allowedIp) &&
        Objects.equals(this.deniedIp, userFilters.deniedIp) &&
        Objects.equals(this.deniedLoginMethods, userFilters.deniedLoginMethods) &&
        Objects.equals(this.deniedProtocols, userFilters.deniedProtocols) &&
        Objects.equals(this.filePatterns, userFilters.filePatterns) &&
        Objects.equals(this.maxUploadFileSize, userFilters.maxUploadFileSize) &&
        Objects.equals(this.tlsUsername, userFilters.tlsUsername) &&
        Objects.equals(this.tlsCerts, userFilters.tlsCerts) &&
        Objects.equals(this.hooks, userFilters.hooks) &&
        Objects.equals(this.disableFsChecks, userFilters.disableFsChecks) &&
        Objects.equals(this.webClient, userFilters.webClient) &&
        Objects.equals(this.allowApiKeyAuth, userFilters.allowApiKeyAuth) &&
        Objects.equals(this.userType, userFilters.userType) &&
        Objects.equals(this.bandwidthLimits, userFilters.bandwidthLimits) &&
        Objects.equals(this.externalAuthCacheTime, userFilters.externalAuthCacheTime) &&
        Objects.equals(this.startDirectory, userFilters.startDirectory) &&
        Objects.equals(this.twoFactorProtocols, userFilters.twoFactorProtocols) &&
        Objects.equals(this.ftpSecurity, userFilters.ftpSecurity) &&
        Objects.equals(this.isAnonymous, userFilters.isAnonymous) &&
        Objects.equals(this.defaultSharesExpiration, userFilters.defaultSharesExpiration) &&
        Objects.equals(this.maxSharesExpiration, userFilters.maxSharesExpiration) &&
        Objects.equals(this.passwordExpiration, userFilters.passwordExpiration) &&
        Objects.equals(this.passwordStrength, userFilters.passwordStrength) &&
        Objects.equals(this.accessTime, userFilters.accessTime) &&
        Objects.equals(this.requirePasswordChange, userFilters.requirePasswordChange) &&
        Objects.equals(this.totpConfig, userFilters.totpConfig) &&
        Objects.equals(this.recoveryCodes, userFilters.recoveryCodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowedIp, deniedIp, deniedLoginMethods, deniedProtocols, filePatterns, maxUploadFileSize, tlsUsername, tlsCerts, hooks, disableFsChecks, webClient, allowApiKeyAuth, userType, bandwidthLimits, externalAuthCacheTime, startDirectory, twoFactorProtocols, ftpSecurity, isAnonymous, defaultSharesExpiration, maxSharesExpiration, passwordExpiration, passwordStrength, accessTime, requirePasswordChange, totpConfig, recoveryCodes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserFilters {\n");
    sb.append("    allowedIp: ").append(toIndentedString(allowedIp)).append("\n");
    sb.append("    deniedIp: ").append(toIndentedString(deniedIp)).append("\n");
    sb.append("    deniedLoginMethods: ").append(toIndentedString(deniedLoginMethods)).append("\n");
    sb.append("    deniedProtocols: ").append(toIndentedString(deniedProtocols)).append("\n");
    sb.append("    filePatterns: ").append(toIndentedString(filePatterns)).append("\n");
    sb.append("    maxUploadFileSize: ").append(toIndentedString(maxUploadFileSize)).append("\n");
    sb.append("    tlsUsername: ").append(toIndentedString(tlsUsername)).append("\n");
    sb.append("    tlsCerts: ").append(toIndentedString(tlsCerts)).append("\n");
    sb.append("    hooks: ").append(toIndentedString(hooks)).append("\n");
    sb.append("    disableFsChecks: ").append(toIndentedString(disableFsChecks)).append("\n");
    sb.append("    webClient: ").append(toIndentedString(webClient)).append("\n");
    sb.append("    allowApiKeyAuth: ").append(toIndentedString(allowApiKeyAuth)).append("\n");
    sb.append("    userType: ").append(toIndentedString(userType)).append("\n");
    sb.append("    bandwidthLimits: ").append(toIndentedString(bandwidthLimits)).append("\n");
    sb.append("    externalAuthCacheTime: ").append(toIndentedString(externalAuthCacheTime)).append("\n");
    sb.append("    startDirectory: ").append(toIndentedString(startDirectory)).append("\n");
    sb.append("    twoFactorProtocols: ").append(toIndentedString(twoFactorProtocols)).append("\n");
    sb.append("    ftpSecurity: ").append(toIndentedString(ftpSecurity)).append("\n");
    sb.append("    isAnonymous: ").append(toIndentedString(isAnonymous)).append("\n");
    sb.append("    defaultSharesExpiration: ").append(toIndentedString(defaultSharesExpiration)).append("\n");
    sb.append("    maxSharesExpiration: ").append(toIndentedString(maxSharesExpiration)).append("\n");
    sb.append("    passwordExpiration: ").append(toIndentedString(passwordExpiration)).append("\n");
    sb.append("    passwordStrength: ").append(toIndentedString(passwordStrength)).append("\n");
    sb.append("    accessTime: ").append(toIndentedString(accessTime)).append("\n");
    sb.append("    requirePasswordChange: ").append(toIndentedString(requirePasswordChange)).append("\n");
    sb.append("    totpConfig: ").append(toIndentedString(totpConfig)).append("\n");
    sb.append("    recoveryCodes: ").append(toIndentedString(recoveryCodes)).append("\n");
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
