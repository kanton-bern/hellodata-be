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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * EventActionEmailConfig
 */
@JsonPropertyOrder({
  EventActionEmailConfig.JSON_PROPERTY_RECIPIENTS,
  EventActionEmailConfig.JSON_PROPERTY_BCC,
  EventActionEmailConfig.JSON_PROPERTY_SUBJECT,
  EventActionEmailConfig.JSON_PROPERTY_BODY,
  EventActionEmailConfig.JSON_PROPERTY_CONTENT_TYPE,
  EventActionEmailConfig.JSON_PROPERTY_ATTACHMENTS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-12-10T09:15:17.190691+01:00[Europe/Warsaw]", comments = "Generator version: 7.9.0")
public class EventActionEmailConfig {
  public static final String JSON_PROPERTY_RECIPIENTS = "recipients";
  private List<String> recipients = new ArrayList<>();

  public static final String JSON_PROPERTY_BCC = "bcc";
  private List<String> bcc = new ArrayList<>();

  public static final String JSON_PROPERTY_SUBJECT = "subject";
  private String subject;

  public static final String JSON_PROPERTY_BODY = "body";
  private String body;

  /**
   * Content type:   * &#x60;0&#x60; text/plain   * &#x60;1&#x60; text/html 
   */
  public enum ContentTypeEnum {
    NUMBER_0(0),
    
    NUMBER_1(1);

    private Integer value;

    ContentTypeEnum(Integer value) {
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
    public static ContentTypeEnum fromValue(Integer value) {
      for (ContentTypeEnum b : ContentTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_CONTENT_TYPE = "content_type";
  private ContentTypeEnum contentType;

  public static final String JSON_PROPERTY_ATTACHMENTS = "attachments";
  private List<String> attachments = new ArrayList<>();

  public EventActionEmailConfig() {
  }

  public EventActionEmailConfig recipients(List<String> recipients) {
    
    this.recipients = recipients;
    return this;
  }

  public EventActionEmailConfig addRecipientsItem(String recipientsItem) {
    if (this.recipients == null) {
      this.recipients = new ArrayList<>();
    }
    this.recipients.add(recipientsItem);
    return this;
  }

  /**
   * Get recipients
   * @return recipients
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_RECIPIENTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getRecipients() {
    return recipients;
  }


  @JsonProperty(JSON_PROPERTY_RECIPIENTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setRecipients(List<String> recipients) {
    this.recipients = recipients;
  }

  public EventActionEmailConfig bcc(List<String> bcc) {
    
    this.bcc = bcc;
    return this;
  }

  public EventActionEmailConfig addBccItem(String bccItem) {
    if (this.bcc == null) {
      this.bcc = new ArrayList<>();
    }
    this.bcc.add(bccItem);
    return this;
  }

  /**
   * Get bcc
   * @return bcc
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_BCC)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getBcc() {
    return bcc;
  }


  @JsonProperty(JSON_PROPERTY_BCC)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setBcc(List<String> bcc) {
    this.bcc = bcc;
  }

  public EventActionEmailConfig subject(String subject) {
    
    this.subject = subject;
    return this;
  }

  /**
   * Get subject
   * @return subject
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SUBJECT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getSubject() {
    return subject;
  }


  @JsonProperty(JSON_PROPERTY_SUBJECT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSubject(String subject) {
    this.subject = subject;
  }

  public EventActionEmailConfig body(String body) {
    
    this.body = body;
    return this;
  }

  /**
   * Get body
   * @return body
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_BODY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getBody() {
    return body;
  }


  @JsonProperty(JSON_PROPERTY_BODY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setBody(String body) {
    this.body = body;
  }

  public EventActionEmailConfig contentType(ContentTypeEnum contentType) {
    
    this.contentType = contentType;
    return this;
  }

  /**
   * Content type:   * &#x60;0&#x60; text/plain   * &#x60;1&#x60; text/html 
   * @return contentType
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CONTENT_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public ContentTypeEnum getContentType() {
    return contentType;
  }


  @JsonProperty(JSON_PROPERTY_CONTENT_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setContentType(ContentTypeEnum contentType) {
    this.contentType = contentType;
  }

  public EventActionEmailConfig attachments(List<String> attachments) {
    
    this.attachments = attachments;
    return this;
  }

  public EventActionEmailConfig addAttachmentsItem(String attachmentsItem) {
    if (this.attachments == null) {
      this.attachments = new ArrayList<>();
    }
    this.attachments.add(attachmentsItem);
    return this;
  }

  /**
   * list of file paths to attach. The total size is limited to 10 MB
   * @return attachments
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ATTACHMENTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getAttachments() {
    return attachments;
  }


  @JsonProperty(JSON_PROPERTY_ATTACHMENTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAttachments(List<String> attachments) {
    this.attachments = attachments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventActionEmailConfig eventActionEmailConfig = (EventActionEmailConfig) o;
    return Objects.equals(this.recipients, eventActionEmailConfig.recipients) &&
        Objects.equals(this.bcc, eventActionEmailConfig.bcc) &&
        Objects.equals(this.subject, eventActionEmailConfig.subject) &&
        Objects.equals(this.body, eventActionEmailConfig.body) &&
        Objects.equals(this.contentType, eventActionEmailConfig.contentType) &&
        Objects.equals(this.attachments, eventActionEmailConfig.attachments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recipients, bcc, subject, body, contentType, attachments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EventActionEmailConfig {\n");
    sb.append("    recipients: ").append(toIndentedString(recipients)).append("\n");
    sb.append("    bcc: ").append(toIndentedString(bcc)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
    sb.append("    attachments: ").append(toIndentedString(attachments)).append("\n");
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
