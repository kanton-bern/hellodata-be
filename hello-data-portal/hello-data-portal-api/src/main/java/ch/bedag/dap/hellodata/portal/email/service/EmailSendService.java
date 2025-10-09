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

import ch.bedag.dap.hellodata.portal.base.config.SystemProperties;
import ch.bedag.dap.hellodata.portal.email.model.EmailTemplateData;
import jakarta.annotation.Nonnull;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
class EmailSendService {
    private static final String HELLODATA_FRONTEND_URL_PARAM_KEY = "link";

    private final JavaMailSender javaMailSender;
    private final TemplateService templateService;
    private final SystemProperties systemProperties;
    private final MessageSource resourceBundleMessageSource;
    @Value("${hello-data.auth-server.redirect-url}")
    private String redirectUrl;

    void sendEmailFromTemplate(EmailTemplateData templateData) {
        if (templateData.getReceivers().isEmpty()) {
            log.error("No receiver defined for given email data :" + templateData);
            return;
        }
        sendEmail(toSimpleMailMessage(templateData));
    }

    private void sendEmail(SimpleMailMessage simpleMailMessage) {
        log.info("Sending email : {}", simpleMailMessage);
        validateEmail(simpleMailMessage);
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = getMimeMessageHelper(simpleMailMessage, message);
            if (helper != null) {
                javaMailSender.send(message);
            }
        } catch (Exception ex) {
            log.error("Exception while sending Email", ex);
        }
    }

    private Map<String, Object> getTemplateModel(EmailTemplateData genericEmailData) {
        Map<String, Object> model = new HashMap<>(genericEmailData.getTemplateModel());
        model.put(HELLODATA_FRONTEND_URL_PARAM_KEY, redirectUrl);
        return model;
    }

    SimpleMailMessage toSimpleMailMessage(EmailTemplateData emailTemplateData) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(systemProperties.getNoReplyEmail());
        email.setTo(emailTemplateData.getReceivers().toArray(new String[0]));
        Locale locale = emailTemplateData.getLocale();
        String message = resourceBundleMessageSource.getMessage(emailTemplateData.getSubject(), emailTemplateData.getSubjectParams(), emailTemplateData.getSubject(), locale != null ? locale : Locale.ROOT);
        email.setSubject(message);
        email.setText(templateService.getContent(emailTemplateData.getTemplate(), getTemplateModel(emailTemplateData), locale));
        return email;
    }

    private MimeMessageHelper getMimeMessageHelper(@Nonnull SimpleMailMessage email, @Nonnull MimeMessage message) throws Exception {
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        if (email.getFrom() != null) {
            helper.setFrom(email.getFrom());
        }
        String[] toArray = email.getTo();
        String[] ccArray = email.getCc();
        String[] bccArray = email.getBcc();
        if (ArrayUtils.isEmpty(toArray) && ArrayUtils.isEmpty(ccArray) && ArrayUtils.isEmpty(bccArray)) {
            return null;
        }
        if (toArray != null) {
            helper.setTo(toArray);
        }
        if (ccArray != null) {
            helper.setCc(ccArray);
        }
        if (bccArray != null) {
            helper.setBcc(bccArray);
        }
        if (StringUtils.isBlank(email.getSubject())) {
            helper.setSubject("");
        } else {
            helper.setSubject(email.getSubject());
        }
        if (email.getText() != null) {
            helper.setText(email.getText(), true);
        }
        return helper;
    }

    private void validateEmail(SimpleMailMessage message) {
        message.setTo(toStringArray(filterInternetAddresses(message.getTo())));
        message.setCc(toStringArray(filterInternetAddresses(message.getCc())));
        message.setBcc(toStringArray(filterInternetAddresses(message.getBcc())));
        verifyRecipientsExist(message);
    }

    private void verifyRecipientsExist(SimpleMailMessage message) {

        if (ArrayUtils.isEmpty(message.getTo()) && ArrayUtils.isEmpty(message.getBcc()) && ArrayUtils.isEmpty(message.getCc())) {
            throw new MailSendException("Unable to send email because no destination found");
        }
    }

    private List<InternetAddress> filterInternetAddresses(String[] addresses) {
        if (addresses == null) {
            return new ArrayList<>();
        }
        List<InternetAddress> addressList = new LinkedList<>();
        for (String address : addresses) {
            try {
                InternetAddress[] parsedAddresses = InternetAddress.parse(address);
                for (InternetAddress parsedAddress : parsedAddresses) {
                    parsedAddress.validate();
                    addressList.add(parsedAddress);
                }
            } catch (AddressException ex) {
                log.error("Exception while parsing Email addresses", ex);
            }
        }
        return addressList;
    }

    private String[] toStringArray(List<InternetAddress> addresses) {
        return addresses.stream().map(InternetAddress::getAddress).toArray(String[]::new);
    }
}
