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
package ch.bedag.dap.hellodata.sidecars.cloudbeaver.service;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.Privilege;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.Role;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.PrivilegeRepository;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.RoleRepository;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.utils.Sets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Log4j2
@Service
public class SecurityService {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;

    @Transactional
    public void createDataDomainRoles(Set<String> contextKeys) {
        Privilege readOnDwhPrivilege = this.createPrivilegeIfNotFound(Privilege.READ_DWH_PRIVILEGE);
        Privilege readOnLznPrivilege = this.createPrivilegeIfNotFound(Privilege.READ_DM_PRIVILEGE);

        contextKeys.stream().filter(contextKey -> !contextKey.equalsIgnoreCase(Role.ADMIN_ROLE_KEY)).forEach(contextKey -> {
            // create role for full DWH access
            createCbRole(contextKey, readOnDwhPrivilege);
            // create role for _LZN tables access
            createCbRole(contextKey, readOnLznPrivilege);
        });

        // create an admin user role
        createRoleIfNotFound(Role.ADMIN_ROLE_KEY, Role.ADMIN_ROLE_NAME, Sets.newHashSet(readOnDwhPrivilege, readOnLznPrivilege));
    }

    private void createCbRole(String contextKey, Privilege privilege) {
        String roleName = SlugifyUtil.slugify(contextKey, "").toUpperCase() + "_" + privilege.getName().toUpperCase();
        String roleKey = contextKey + "_" + privilege.getName().toUpperCase();
        createRoleIfNotFound(roleKey, roleName, Sets.newHashSet(privilege));
    }

    private void createRoleIfNotFound(String key, String name, Collection<Privilege> privileges) {
        Role role = roleRepository.findByKeyIgnoreCase(key);
        if (role == null) {
            log.info("Create role {} with key {}", name, key);
            role = new Role(key, name, true);
            role.setPrivileges(privileges);
            roleRepository.save(role);
        } else {
            log.info("Update role {} with key {}", name, key);
            role.setName(name);
            role.setEnabled(true);
            role.getPrivileges().clear();
            role.getPrivileges().addAll(privileges);
            roleRepository.save(role);
        }
    }

    private Privilege createPrivilegeIfNotFound(String name) {
        Privilege privilege = privilegeRepository.findByName(name);
        if (privilege == null) {
            privilege = new Privilege(name);
            privilegeRepository.save(privilege);
        }
        return privilege;
    }
}
