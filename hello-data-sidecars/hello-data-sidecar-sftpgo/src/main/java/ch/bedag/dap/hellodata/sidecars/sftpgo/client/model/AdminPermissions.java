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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Admin permissions:   * &#x60;*&#x60; - super admin permissions are granted   * &#x60;add_users&#x60; - add new users is allowed   * &#x60;edit_users&#x60; - change existing users is allowed   * &#x60;del_users&#x60; - remove users is allowed   * &#x60;view_users&#x60; - list users is allowed   * &#x60;view_conns&#x60; - list active connections is allowed   * &#x60;close_conns&#x60; - close active connections is allowed   * &#x60;view_status&#x60; - view the server status is allowed   * &#x60;manage_folders&#x60; - manage folders is allowed   * &#x60;manage_groups&#x60; - manage groups is allowed   * &#x60;quota_scans&#x60; - view and start quota scans is allowed   * &#x60;manage_defender&#x60; - remove ip from the dynamic blocklist is allowed   * &#x60;view_defender&#x60; - list the dynamic blocklist is allowed   * &#x60;view_events&#x60; - view and search filesystem and provider events is allowed   * &#x60;disable_mfa&#x60; - allow to disable two-factor authentication for users and admins 
 */
public enum AdminPermissions {
  
  STAR("*"),
  
  ADD_USERS("add_users"),
  
  EDIT_USERS("edit_users"),
  
  DEL_USERS("del_users"),
  
  VIEW_USERS("view_users"),
  
  VIEW_CONNS("view_conns"),
  
  CLOSE_CONNS("close_conns"),
  
  VIEW_STATUS("view_status"),
  
  MANAGE_FOLDERS("manage_folders"),
  
  MANAGE_GROUPS("manage_groups"),
  
  QUOTA_SCANS("quota_scans"),
  
  MANAGE_DEFENDER("manage_defender"),
  
  VIEW_DEFENDER("view_defender"),
  
  VIEW_EVENTS("view_events"),
  
  DISABLE_MFA("disable_mfa");

  private String value;

  AdminPermissions(String value) {
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
  public static AdminPermissions fromValue(String value) {
    for (AdminPermissions b : AdminPermissions.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

