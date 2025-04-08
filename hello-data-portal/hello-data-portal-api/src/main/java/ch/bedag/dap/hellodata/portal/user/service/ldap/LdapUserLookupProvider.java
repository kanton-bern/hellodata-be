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
package ch.bedag.dap.hellodata.portal.user.service.ldap;


import ch.bedag.dap.hellodata.portal.profiles.LdapUserLookupProfile;
import ch.bedag.dap.hellodata.portal.user.data.AdUserDto;
import ch.bedag.dap.hellodata.portal.user.data.AdUserOrigin;
import ch.bedag.dap.hellodata.portal.user.service.UserLookupProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.ContainerCriteria;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.List;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

@Component
@RequiredArgsConstructor
@Log4j2
@LdapUserLookupProfile
public class LdapUserLookupProvider implements UserLookupProvider {

    private final LdapTemplate ldapTemplate;
    private final LdapConfigProperties configProperties;

    @Override
    public List<AdUserDto> searchUserByEmail(String email) {
        log.info("Looking up users in LDAP with email: {}", email);
        List<AdUserDto> adUsers = adLookup(email);
        log.info("Found {} users matching adUsers criteria.", adUsers.size());
        return adUsers;
    }

    public List<AdUserDto> adLookup(String email) {
        String encodedEmail = LdapEncoder.filterEncode(email);
        log.info("Search for email {}", encodedEmail);
        ContainerCriteria query = query().where("objectclass").is("person").and("mail").like(encodedEmail + "*");
        return ldapTemplate.search(query,
                (AttributesMapper<AdUserDto>) attrs -> {
                    log.debug("Attributes: {}", attrs);
                    return toAdUserDto(attrs);
                });
    }

    private AdUserDto toAdUserDto(Attributes attrs) throws NamingException {
        var user = new AdUserDto();
        user.setEmail(getFieldOrDefault(attrs, configProperties.getFieldMapping().getEmail()));
        user.setFirstName(getFieldOrDefault(attrs, configProperties.getFieldMapping().getFirstName()));
        user.setLastName(getFieldOrDefault(attrs, configProperties.getFieldMapping().getLastName()));
        user.setUsername(getFieldOrDefault(attrs, configProperties.getFieldMapping().getUserPrincipalName()));
        user.setOrigin(AdUserOrigin.LDAP);
        return user;
    }

    private String getFieldOrDefault(Attributes attrs, String fieldName) throws NamingException {
        var field = attrs.get(fieldName);
        if (field != null) {
            return (String) field.get();
        }
        return (String) attrs.get("cn").get();
    }
}
