/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.portal.email.service;

import ch.bedag.dap.hellodata.portal.email.model.EmailTemplate;
import ch.bedag.dap.hellodata.portal.email.model.EmailTemplateData;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.user.KeycloakTestContainerTest;
import ch.bedag.dap.hellodata.portal.user.data.ContextDto;
import ch.bedag.dap.hellodata.portal.user.data.UserContextRoleDto;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static ch.bedag.dap.hellodata.portal.email.model.EmailTemplateModelKeys.*;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
class EmailSendServiceTest extends KeycloakTestContainerTest {

    @Autowired
    private EmailSendService emailSendService;

    @Test
    void toSimpleMailMessage_when_user_deactivated_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_DEACTIVATED);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");
        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.select("p").first().text()).isEqualTo("Hallo John Doe");
        assertThat(actualDoc.select("p").get(1).text()).contains("Darth Vader");
        assertThat(actualDoc.select("p").get(1).text()).contains("Fancy Business Domain");
        assertThat(actualDoc.select("p").get(1).text()).contains("deaktiviert");
        assertThat(actualDoc.select("a").attr("href")).isEqualTo("http://localhost:4200");
    }

    @Test
    void toSimpleMailMessage_when_user_deactivated_should_generate_correct_html_content_default_locale_uses_de_CH() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_DEACTIVATED);
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        String businessDomain = "Fancy Business Domain";
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, businessDomain);
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");
        emailTemplateData.setSubjectParams(new Object[]{businessDomain});

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then — default (null) locale falls back to de_CH templates
        assertThat(actualDoc.select("p").first().text()).isEqualTo("Hallo John Doe");
        assertThat(actualDoc.select("p").get(1).text()).contains("Darth Vader");
        assertThat(actualDoc.select("p").get(1).text()).contains("deaktiviert");
        assertThat(email.getSubject()).isEqualTo("Benutzer in HelloDATA Fachdomäne Fancy Business Domain deaktiviert");
    }


    @Test
    void toSimpleMailMessage_when_user_activated_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_ACTIVATED);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.select("p").first().text()).isEqualTo("Hallo John Doe");
        assertThat(actualDoc.select("p").get(1).text()).contains("Darth Vader");
        assertThat(actualDoc.select("p").get(1).text()).contains("Fancy Business Domain");
        assertThat(actualDoc.select("p").get(1).text()).contains("aktiviert");
        assertThat(actualDoc.select("a").attr("href")).isEqualTo("http://localhost:4200");
    }

    @Test
    void toSimpleMailMessage_when_user_role_changed_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_ROLE_CHANGED);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_ROLE_NAME_PARAM, "HelloDATA Admin");
        List<UserContextRoleDto> allDataDomainRoles = new ArrayList<>();
        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
        RoleDto roleDto = new RoleDto();
        roleDto.setName("Role1");
        userContextRoleDto.setRole(roleDto);
        ContextDto contextDto = new ContextDto();
        contextDto.setName("Data Domain 1");
        userContextRoleDto.setContext(contextDto);
        allDataDomainRoles.add(userContextRoleDto);
        emailTemplateData.getTemplateModel().put(DATA_DOMAIN_ROLES_PARAM, allDataDomainRoles);

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.select("p").first().text()).isEqualTo("Hallo John Doe");
        assertThat(actualDoc.select("p").get(1).text()).contains("Darth Vader");
        assertThat(actualDoc.select("p").get(1).text()).contains("Fancy Business Domain");
        assertThat(actualDoc.select("p").get(1).text()).contains("Rollen angepasst");
        assertThat(actualDoc.select("ul li").first().text()).contains("Fancy Business Domain: HelloDATA Admin");
        assertThat(actualDoc.select("div ul li").text()).contains("Data Domain 1: Role1");
        assertThat(actualDoc.select("a").attr("href")).isEqualTo("http://localhost:4200");
    }

    @Test
    void toSimpleMailMessage_when_user_account_created_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_ACCOUNT_CREATED);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_ROLE_NAME_PARAM, "HelloDATA Admin");
        List<UserContextRoleDto> allDataDomainRoles = new ArrayList<>();
        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
        RoleDto roleDto = new RoleDto();
        roleDto.setName("Role1");
        userContextRoleDto.setRole(roleDto);
        ContextDto contextDto = new ContextDto();
        contextDto.setName("Data Domain 1");
        userContextRoleDto.setContext(contextDto);
        allDataDomainRoles.add(userContextRoleDto);

        emailTemplateData.getTemplateModel().put(DATA_DOMAIN_ROLES_PARAM, allDataDomainRoles);

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.select("p").first().text()).isEqualTo("Hallo John Doe");
        assertThat(actualDoc.select("p").get(1).text()).contains("Darth Vader");
        assertThat(actualDoc.select("p").get(1).text()).contains("Fancy Business Domain");
        assertThat(actualDoc.select("p").get(1).text()).contains("Benutzer*in mit folgenden Rollen angelegt");
        assertThat(actualDoc.select("ul li").first().text()).contains("Fancy Business Domain: HelloDATA Admin");
        assertThat(actualDoc.select("div ul li").text()).contains("Data Domain 1: Role1");
        assertThat(actualDoc.select("a").attr("href")).isEqualTo("http://localhost:4200");
    }

    @Test
    void toSimpleMailMessage_when_comment_published_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.COMMENT_STATUS_PUBLISHED);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");
        emailTemplateData.getTemplateModel().put(COMMENT_TEXT_PARAM, "This is my comment");
        emailTemplateData.getTemplateModel().put(DASHBOARD_NAME_PARAM, "Sales Dashboard");
        emailTemplateData.getTemplateModel().put(COMMENT_NEW_STATUS_PARAM, "PUBLISHED");

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.select("p").first().text()).isEqualTo("Hallo John Doe");
        assertThat(actualDoc.select("p").get(1).text()).contains("Darth Vader");
        assertThat(actualDoc.select("p").get(1).text()).contains("Sales Dashboard");
        assertThat(actualDoc.select("p").get(1).text()).contains("Fancy Business Domain");
        assertThat(actualDoc.select("p").get(1).text()).contains("veröffentlicht");
        assertThat(actualDoc.select("blockquote").text()).isEqualTo("This is my comment");
        assertThat(actualDoc.select("a").attr("href")).isEqualTo("http://localhost:4200");
    }

    @Test
    void toSimpleMailMessage_when_comment_declined_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.COMMENT_STATUS_DECLINED);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");
        emailTemplateData.getTemplateModel().put(COMMENT_TEXT_PARAM, "This is my comment");
        emailTemplateData.getTemplateModel().put(DASHBOARD_NAME_PARAM, "Sales Dashboard");
        emailTemplateData.getTemplateModel().put(COMMENT_NEW_STATUS_PARAM, "DECLINED");
        emailTemplateData.getTemplateModel().put(COMMENT_DECLINE_REASON_PARAM, "Not relevant");

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.select("p").first().text()).isEqualTo("Hallo John Doe");
        assertThat(actualDoc.select("p").get(1).text()).contains("Darth Vader");
        assertThat(actualDoc.select("p").get(1).text()).contains("Sales Dashboard");
        assertThat(actualDoc.select("p").get(1).text()).contains("abgelehnt");
        assertThat(actualDoc.select("blockquote").first().text()).isEqualTo("This is my comment");
        assertThat(actualDoc.select("blockquote").get(1).text()).isEqualTo("Not relevant");
        assertThat(actualDoc.select("a").attr("href")).isEqualTo("http://localhost:4200");
    }

    @Test
    void toSimpleMailMessage_when_comment_sent_for_review_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.COMMENT_SENT_FOR_REVIEW);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "Reviewer Hans");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(COMMENT_TEXT_PARAM, "Please review this");
        emailTemplateData.getTemplateModel().put(DASHBOARD_NAME_PARAM, "Sales Dashboard");
        emailTemplateData.getTemplateModel().put(COMMENT_NEW_STATUS_PARAM, "READY_FOR_REVIEW");

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.select("p").first().text()).isEqualTo("Hallo Reviewer Hans");
        assertThat(actualDoc.select("p").get(1).text()).contains("John Doe");
        assertThat(actualDoc.select("p").get(1).text()).contains("Sales Dashboard");
        assertThat(actualDoc.select("p").get(1).text()).contains("Überprüfung");
        assertThat(actualDoc.select("blockquote").text()).isEqualTo("Please review this");
        assertThat(actualDoc.select("a").attr("href")).isEqualTo("http://localhost:4200");
    }

    @Test
    void toSimpleMailMessage_when_comment_deleted_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.COMMENT_DELETED);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");
        emailTemplateData.getTemplateModel().put(COMMENT_TEXT_PARAM, "This was my comment");
        emailTemplateData.getTemplateModel().put(DASHBOARD_NAME_PARAM, "Sales Dashboard");
        emailTemplateData.getTemplateModel().put(COMMENT_NEW_STATUS_PARAM, "DELETED");
        emailTemplateData.getTemplateModel().put(COMMENT_DELETION_REASON_PARAM, "Spam content");

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.select("p").first().text()).isEqualTo("Hallo John Doe");
        assertThat(actualDoc.select("p").get(1).text()).contains("Darth Vader");
        assertThat(actualDoc.select("p").get(1).text()).contains("Sales Dashboard");
        assertThat(actualDoc.select("p").get(1).text()).contains("gelöscht");
        assertThat(actualDoc.select("blockquote").first().text()).isEqualTo("This was my comment");
        assertThat(actualDoc.select("blockquote").get(1).text()).isEqualTo("Spam content");
        assertThat(actualDoc.select("a").attr("href")).isEqualTo("http://localhost:4200");
    }

    @Test
    void toSimpleMailMessage_when_comment_edited_by_reviewer_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.COMMENT_EDITED_BY_REVIEWER);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");
        emailTemplateData.getTemplateModel().put(COMMENT_TEXT_PARAM, "Edited comment text");
        emailTemplateData.getTemplateModel().put(DASHBOARD_NAME_PARAM, "Sales Dashboard");

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.select("p").first().text()).isEqualTo("Hallo John Doe");
        assertThat(actualDoc.select("p").get(1).text()).contains("Darth Vader");
        assertThat(actualDoc.select("p").get(1).text()).contains("Sales Dashboard");
        assertThat(actualDoc.select("p").get(1).text()).contains("Fancy Business Domain");
        assertThat(actualDoc.select("p").get(1).text()).contains("bearbeitet");
        assertThat(actualDoc.select("blockquote").text()).isEqualTo("Edited comment text");
        assertThat(actualDoc.select("a").attr("href")).isEqualTo("http://localhost:4200");
    }
}
