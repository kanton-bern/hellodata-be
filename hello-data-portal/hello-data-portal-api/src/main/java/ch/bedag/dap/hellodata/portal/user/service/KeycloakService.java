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
package ch.bedag.dap.hellodata.portal.user.service;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
@CacheConfig(cacheNames = "keycloak_users")
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloak;
    @Value("${hello-data.auth-server.realm}")
    private String realmName;
    @Value("${hello-data.auth-server.admin-client-id}")
    private String adminClientId;
    @Value("${hello-data.auth-server.admin-client-id}")
    private String adminClientSecret;
    @Value("${hello-data.auth-server.client-id}")
    private String clientId;
    @Value("${hello-data.auth-server.realm}")
    private String realm;
    @Value("${hello-data.auth-server.url}")
    private String authServerUrl;

    public String createUser(UserRepresentation user) {
        try (Response response = keycloak.realm(realmName).users().create(user)) {
            return getCreatedId(response);
        }
    }

    public UserResource getUserResourceById(String userId) {
        return keycloak.realm(realmName).users().get(userId);
    }

    public UserRepresentation getUserRepresentationById(String userId) {
        UserResource userResource = getUserResourceById(userId); // Reuse cached UserResource
        if (userResource != null) {
            return userResource.toRepresentation();
        }
        return null;
    }

    public UserRepresentation getUserRepresentationByEmail(String email) {
        List<UserRepresentation> userRepresentations = keycloak.realm(realmName).users().searchByEmail(email, true);
        if (CollectionUtils.isEmpty(userRepresentations)) {
            return null;
        }
        return userRepresentations.get(0);
    }

    /**
     * Idempotently create a realm role. Airflow 3 authenticates via Keycloak (FAB OAuth) and maps
     * Keycloak realm roles to FAB roles at login, so the per-domain {@code DD_<domain>} and
     * {@code airflow_*} roles the api-server expects must exist as realm roles. Requires the admin
     * client to hold the {@code manage-realm} (realm-management) capability.
     */
    public void createRealmRoleIfMissing(String roleName) {
        RolesResource rolesResource = keycloak.realm(realmName).roles();
        try {
            rolesResource.get(roleName).toRepresentation();
        } catch (NotFoundException e) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(roleName);
            role.setDescription("Managed by the HelloDATA portal for Airflow 3 access");
            rolesResource.create(role);
            log.info("Created Keycloak realm role '{}'", roleName);
        }
    }

    public Set<String> getRealmRoleNamesOfUser(String userId) {
        return keycloak.realm(realmName).users().get(userId).roles().realmLevel().listAll()
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }

    public void addRealmRolesToUser(String userId, Collection<String> roleNames) {
        if (CollectionUtils.isEmpty(roleNames)) {
            return;
        }
        keycloak.realm(realmName).users().get(userId).roles().realmLevel().add(toRoleRepresentations(roleNames));
    }

    public void removeRealmRolesFromUser(String userId, Collection<String> roleNames) {
        if (CollectionUtils.isEmpty(roleNames)) {
            return;
        }
        keycloak.realm(realmName).users().get(userId).roles().realmLevel().remove(toRoleRepresentations(roleNames));
    }

    private List<RoleRepresentation> toRoleRepresentations(Collection<String> roleNames) {
        RolesResource rolesResource = keycloak.realm(realmName).roles();
        return roleNames.stream().map(name -> rolesResource.get(name).toRepresentation()).toList();
    }

    private String getCreatedId(Response response) {
        try (response) {
            HttpStatusCode status = HttpStatusCode.valueOf(response.getStatus());
            if (!status.is2xxSuccessful()) {
                throw new ResponseStatusException(status);
            }
            URI uri = response.getLocation();
            String path = uri.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        }
    }
}