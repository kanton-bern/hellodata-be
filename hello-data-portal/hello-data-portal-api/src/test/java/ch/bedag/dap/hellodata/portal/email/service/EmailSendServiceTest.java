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
    public void toSimpleMailMessage_when_user_deactivated_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_DEACTIVATED);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");
        String expectedHTML = """
                <!doctype html>
                <html xmlns="http://www.w3.org/1999/html">
                  <body>
                    <p>Hallo <span>John Doe</span></p>
                    <p>
                      Der Administrator <span>Darth Vader</span> hat dir deinen HelloDATA Benutzer*in in der Fachdomäne
                      <span>Fancy Business Domain</span> deaktiviert.
                    </p>
                    <p>Link zu Fachdomäne
                      <a href="http://localhost:4200"><span>Fancy Business Domain</span></a>
                    </p>
                    <p>Freundliche Grüsse<br/>HelloDATA</p>
                  </body>
                </html>""";
        Document expectedDoc = Jsoup.parse(expectedHTML);

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.html()).isEqualTo(expectedDoc.html());
    }

    @Test
    public void toSimpleMailMessage_when_user_activated_should_generate_correct_html_content() {
        //given
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_ACTIVATED);
        emailTemplateData.setLocale(Locale.forLanguageTag("de-CH"));
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, "John Doe");
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, "Fancy Business Domain");
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, "Darth Vader");
        String expectedHTML = """
                <!doctype html>
                <html xmlns="http://www.w3.org/1999/html">
                  <body>
                    <p>Hallo <span>John Doe</span></p>
                    <p>
                      Der Administrator <span>Darth Vader</span> hat dir deinen HelloDATA Benutzer*in in der Fachdomäne
                      <span>Fancy Business Domain</span> aktiviert.
                    </p>
                    <p>Link zu Fachdomäne
                      <a href="http://localhost:4200"><span>Fancy Business Domain</span></a>
                    </p>
                    <p>Freundliche Grüsse<br/>HelloDATA</p>
                  </body>
                </html>""";
        Document expectedDoc = Jsoup.parse(expectedHTML);

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.html()).isEqualTo(expectedDoc.html());
    }

    @Test
    public void toSimpleMailMessage_when_user_role_changed_should_generate_correct_html_content() {
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
        String expectedHTML = """
                <!doctype html>
                <html xmlns="http://www.w3.org/1999/html">
                 <head></head>
                 <body>
                  <p>Hallo <span>John Doe</span></p>
                  <p>Der Administrator <span>Darth Vader</span> hat in HelloDATA in der Fachdomäne <span>Fancy Business Domain</span> Deine Rollen angepasst. Du hast folgende Rollen zugewiesen.</p>
                  <ul>
                   <li>Rolle Fachdomäne <span>Fancy Business Domain: HelloDATA Admin</span></li>
                  </ul>
                  <div>
                    <ul>
                      <li>Rolle Datendomäne <span>Data Domain 1: Role1</span></li>
                    </ul>
                  </div>
                  <p>Link zu Fachdomäne <a href="http://localhost:4200"><span>Fancy Business Domain</span></a></p>
                  <p>Freundliche Grüsse<br>HelloDATA</p>
                 </body>
                </html>""";
        Document expectedDoc = Jsoup.parse(expectedHTML);

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.html()).isEqualTo(expectedDoc.html());
    }

    @Test
    public void toSimpleMailMessage_when_user_account_created_should_generate_correct_html_content() {
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
        String expectedHTML = """
                <!doctype html>
                <html xmlns="http://www.w3.org/1999/html">
                 <head></head>
                 <body>
                  <p>Hallo <span>John Doe</span></p>
                  <p>Der Administrator <span>Darth Vader</span> hat dir in HelloDATA in der Fachdomäne <span>Fancy Business Domain</span> einen Benutzer*in mit folgenden Rollen angelegt.</p>
                  <ul>
                   <li>Rolle Fachdomäne <span>Fancy Business Domain: HelloDATA Admin</span></li>
                  </ul>
                  <div>
                    <ul>
                      <li>Rolle Datendomäne <span>Data Domain 1: Role1</span></li>
                    </ul>
                  </div>
                  <p>Link zu Fachdomäne <a href="http://localhost:4200"><span>Fancy Business Domain</span></a></p>
                  <p>Freundliche Grüsse<br>HelloDATA</p>
                 </body>
                </html>""";
        Document expectedDoc = Jsoup.parse(expectedHTML);

        //when
        SimpleMailMessage email = emailSendService.toSimpleMailMessage(emailTemplateData);
        Document actualDoc = Jsoup.parse(Objects.requireNonNull(email.getText()));

        //then
        assertThat(actualDoc.html()).isEqualTo(expectedDoc.html());
    }
}
