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
package ch.bedag.dap.hellodata.commons.security;

import lombok.EqualsAndHashCode;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
public class HellodataAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID userId; //keycloak and DB id are the same
    // Keycloak subject (JWT sub). Equals userId once the portal user exists; kept separately so
    // it is still available for a not-yet-provisioned user, whose userId is null. This lets
    // first-login provisioning be triggered downstream instead of inside the auth converter.
    private final UUID keycloakUserId;
    private final String email;
    private final String firstname;
    private final String lastName;
    private final boolean superuser;

    private final Set<String> permissions;

    public HellodataAuthenticationToken(UUID userId, String firstname, String lastName, String email, UUID keycloakUserId, boolean superuser, Set<String> permissions) {
        super(permissions.stream().map(SimpleGrantedAuthority::new).toList());
        this.userId = userId;
        this.keycloakUserId = keycloakUserId;
        this.email = email;
        this.firstname = firstname;
        this.lastName = lastName;
        this.superuser = superuser;
        this.permissions = permissions;
        setAuthenticated(true);
    }

    // Backward-compatible constructor: for an existing (provisioned) user the Keycloak subject and
    // the DB id are the same, so default keycloakUserId to userId.
    public HellodataAuthenticationToken(UUID userId, String firstname, String lastName, String email, boolean superuser, Set<String> permissions) {
        this(userId, firstname, lastName, email, userId, superuser, permissions);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return email;
    }

    public boolean isSuperuser() {
        return superuser;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getKeycloakUserId() {
        return keycloakUserId;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastName() {
        return lastName;
    }
}
