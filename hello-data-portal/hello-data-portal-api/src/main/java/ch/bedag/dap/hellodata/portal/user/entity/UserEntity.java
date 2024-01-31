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
package ch.bedag.dap.hellodata.portal.user.entity;

import ch.badag.dap.hellodata.commons.basemodel.BaseEntity;
import ch.bedag.dap.hellodata.portal.role.entity.SystemDefaultPortalRoleName;
import ch.bedag.dap.hellodata.portal.role.entity.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portal.role.entity.UserPortalRoleEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity(name = "user_")
public class UserEntity extends BaseEntity {
    private String authId;
    @NotBlank
    @Length(max = 255)
    private String email;
    private LocalDateTime lastAccess;
    //how many invitation emails has been sent
    private int invitationsCount;
    private boolean creationEmailSent;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<UserContextRoleEntity> contextRoles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<UserPortalRoleEntity> portalRoles;

    public Boolean getSuperuser() {
        if (this.getPortalRoles() == null) {
            return false;
        }
        return this.getPortalRoles()
                   .stream()
                   .anyMatch(userPortalRoleEntity -> Objects.equals(userPortalRoleEntity.getRole().getName(), SystemDefaultPortalRoleName.HELLODATA_ADMIN.name()));
    }

    public List<String> getPermissionsFromAllRoles() {
        return CollectionUtils.emptyIfNull(this.getPortalRoles())
                              .stream()
                              .flatMap(userPortalRoleEntity -> userPortalRoleEntity.getRole().getPermissions().getPortalPermissions().stream())
                              .toList();
    }
}
