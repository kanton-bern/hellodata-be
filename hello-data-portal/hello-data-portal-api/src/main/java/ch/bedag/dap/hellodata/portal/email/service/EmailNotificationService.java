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

import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.portal.email.model.EmailTemplate;
import ch.bedag.dap.hellodata.portal.email.model.EmailTemplateData;
import ch.bedag.dap.hellodata.portal.user.data.UpdateContextRolesForUserDto;
import ch.bedag.dap.hellodata.portal.user.data.UserContextRoleDto;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static ch.bedag.dap.hellodata.portal.email.model.EmailTemplateModelKeys.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmailNotificationService {

    private final EmailSendService emailSendService;
    private final HelloDataContextConfig helloDataContextConfig;

    public void notifyAboutUserCreation(String createdUserFirstName, String createdUserEmail, UpdateContextRolesForUserDto updateContextRolesForUserDto,
                                        List<UserContextRoleDto> adminContextRolesAddedToUser, Locale locale) {
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_ACCOUNT_CREATED);
        fillRolesInformation(updateContextRolesForUserDto, adminContextRolesAddedToUser, emailTemplateData);
        fillCommonParamsAndSend(createdUserFirstName, createdUserEmail, emailTemplateData, locale);
    }

    public void notifyAboutUserActivation(String activatedUserFirstName, String createdUserEmail, Locale locale) {
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_ACTIVATED);
        fillCommonParamsAndSend(activatedUserFirstName, createdUserEmail, emailTemplateData, locale);
    }

    public void notifyAboutUserDeactivation(String deactivatedUserFirstName, String createdUserEmail, Locale locale) {
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_DEACTIVATED);
        fillCommonParamsAndSend(deactivatedUserFirstName, createdUserEmail, emailTemplateData, locale);
    }

    public void notifyAboutUserRoleChanged(String editedUserFirstName, String editedUserEmail, UpdateContextRolesForUserDto updateContextRolesForUserDto,
                                           List<UserContextRoleDto> adminContextRolesAddedToUser, Locale locale) {
        EmailTemplateData emailTemplateData = new EmailTemplateData(EmailTemplate.USER_ROLE_CHANGED);
        fillRolesInformation(updateContextRolesForUserDto, adminContextRolesAddedToUser, emailTemplateData);
        fillCommonParamsAndSend(editedUserFirstName, editedUserEmail, emailTemplateData, locale);
    }

    private void fillCommonParamsAndSend(String editedUserFirstName, String createdUserEmail, EmailTemplateData emailTemplateData, Locale locale) {
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_NAME_PARAM, helloDataContextConfig.getBusinessContext().getName());
        emailTemplateData.getTemplateModel().put(AFFECTED_USER_FIRST_NAME_PARAM, editedUserFirstName);
        emailTemplateData.getTemplateModel().put(FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM, SecurityUtils.getCurrentUserFullName());
        emailTemplateData.setSubjectParams(new Object[]{helloDataContextConfig.getBusinessContext().getName()});
        emailTemplateData.getReceivers().add(createdUserEmail);
        emailTemplateData.setLocale(locale);
        emailSendService.sendEmailFromTemplate(emailTemplateData);
    }

    private void fillRolesInformation(UpdateContextRolesForUserDto updateContextRolesForUserDto, List<UserContextRoleDto> adminContextRolesAddedToUser,
                                      EmailTemplateData emailTemplateData) {
        String businessDomainRoleName = updateContextRolesForUserDto.getBusinessDomainRole().getName();
        emailTemplateData.getTemplateModel().put(BUSINESS_DOMAIN_ROLE_NAME_PARAM, businessDomainRoleName);
        List<UserContextRoleDto> allDataDomainRoles = new ArrayList<>();
        allDataDomainRoles.addAll(updateContextRolesForUserDto.getDataDomainRoles());
        allDataDomainRoles.addAll(filterOffDuplicates(updateContextRolesForUserDto, adminContextRolesAddedToUser));
        emailTemplateData.getTemplateModel().put(DATA_DOMAIN_ROLES_PARAM, allDataDomainRoles);
    }

    /**
     * filter off duplicates by the context key
     */
    @NotNull
    private List<UserContextRoleDto> filterOffDuplicates(UpdateContextRolesForUserDto updateContextRolesForUserDto, List<UserContextRoleDto> adminContextRolesAddedToUser) {
        return adminContextRolesAddedToUser.stream()
                .filter(adminContextRole -> updateContextRolesForUserDto.getDataDomainRoles()
                        .stream()
                        .noneMatch(contextRoleForUser -> contextRoleForUser.getContext()
                                .getContextKey()
                                .equalsIgnoreCase(
                                        adminContextRole.getContext()
                                                .getContextKey())))
                .toList();
    }
}
