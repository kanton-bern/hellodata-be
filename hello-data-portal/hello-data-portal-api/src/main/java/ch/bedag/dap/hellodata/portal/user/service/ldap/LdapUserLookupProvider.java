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

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.ArrayList;
import java.util.Collections;
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
        log.debug("Looking up users in LDAP with email: {}", email);
        List<AdUserDto> adUsers = adLookup(email);
        log.debug("Found {} users matching adUsers criteria.", adUsers.size());
        return adUsers;
    }

    public List<AdUserDto> adLookup(String email) {
        String encodedEmail = LdapEncoder.filterEncode(email);
        log.debug("Search for email {}", encodedEmail);
        ContainerCriteria query = query().where("objectclass").is("person").and("objectclass").is("user").and("mail").like(encodedEmail + "*");
        return ldapTemplate.search(query,
                (AttributesMapper<AdUserDto>) attrs -> {
                    log.debug("Attributes: {}", attrs);
                    return toAdUserDto(attrs, email);
                });
    }

    private AdUserDto toAdUserDto(Attributes attrs, String email) throws NamingException {
        log.debug("Mapping attributes to AdUserDto: {}", attrs);
        List<String> groups = new ArrayList<>();
        Attribute memberOf = attrs.get("memberOf");
        if (memberOf != null) {
            groups.addAll(Collections.list(memberOf.getAll()).stream()
                    .map(String::valueOf)
                    .map(this::extractCnFromDn)
                    .toList());
        }
        log.debug("Extracted groups for email {}: {}", email, groups);
        var user = new AdUserDto();
        user.setEmail(getFieldOrDefault(attrs, configProperties.getFieldMapping().getEmail()));
        user.setFirstName(getFieldOrDefault(attrs, configProperties.getFieldMapping().getFirstName()));
        user.setLastName(getFieldOrDefault(attrs, configProperties.getFieldMapping().getLastName()));
        user.setOrigin(AdUserOrigin.LDAP);
        return user;
    }

    public String extractCnFromDn(String dn) {
        if (dn == null || dn.isEmpty()) {
            return dn;
        }

        try {
            // LdapName is the standard, RFC 4514-compliant parser.
            LdapName ldapName = new LdapName(dn);

            // iterate through the RDNs (Relative Distinguished Names) in reverse
            // because the most specific RDN (like CN) is often first.
            for (Rdn rdn : ldapName.getRdns()) {
                // check if the attribute type is "CN" (case-insensitive)
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    // return the attribute's value as a string
                    return rdn.getValue().toString();
                }
            }
        } catch (InvalidNameException e) {
            log.error("Failed to parse DN: {}", dn, e);
            return dn;
        }

        // fallback if no CN component was found in a valid DN.
        return dn;
    }

    private String getFieldOrDefault(Attributes attrs, String fieldName) throws NamingException {
        var field = attrs.get(fieldName);
        if (field != null) {
            return (String) field.get();
        }
        return (String) attrs.get("cn").get();
    }
}
