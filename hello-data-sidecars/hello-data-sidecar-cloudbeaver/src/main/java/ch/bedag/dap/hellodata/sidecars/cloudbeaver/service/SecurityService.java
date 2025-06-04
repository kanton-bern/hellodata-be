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
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.cbnative.AuthSubject;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.cbnative.Team;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.AuthSubjectRepository;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.PrivilegeRepository;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.RoleRepository;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.utils.Sets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RequiredArgsConstructor
@Log4j2
@Service
public class SecurityService {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final AuthSubjectRepository authSubjectRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public void createDataDomainRoles(Set<String> contextKeys) {
        Privilege readOnDwhPrivilege = this.createPrivilegeIfNotFound(Privilege.READ_DWH_PRIVILEGE);
        Privilege readOnLznPrivilege = this.createPrivilegeIfNotFound(Privilege.READ_DM_PRIVILEGE);

        contextKeys.stream().filter(contextKey -> !contextKey.equalsIgnoreCase(Role.ADMIN_ROLE_KEY)).forEach(contextKey -> {
            // create role for full DWH access
            createCbRole(contextKey, readOnDwhPrivilege);
            // create role for _LZN tables access
            createCbRole(contextKey, readOnLznPrivilege);

            // create teams + auth_subject_id entries
            String cbAuthSubjectReadDM = contextKey.toUpperCase(Locale.ROOT) + "_" + Privilege.READ_DM_PRIVILEGE;
            createCbAuthSubjectIfNotFound(cbAuthSubjectReadDM);
            createCbTeamIfNotFound(cbAuthSubjectReadDM, false, contextKey);
            String cbAuthSubjectReadDWH = contextKey.toUpperCase(Locale.ROOT) + "_" + Privilege.READ_DWH_PRIVILEGE;
            createCbAuthSubjectIfNotFound(cbAuthSubjectReadDWH);
            createCbTeamIfNotFound(cbAuthSubjectReadDWH, true, contextKey);
        });

        // create an admin user role
        createRoleIfNotFound(Role.ADMIN_ROLE_KEY, Role.ADMIN_ROLE_NAME, Sets.newHashSet(readOnDwhPrivilege, readOnLznPrivilege));
    }

    private void createCbTeamIfNotFound(String teamId, boolean dwh, String contextKey) {
        List<Team> teams = teamRepository.findByTeamId(teamId);
        if (teams.isEmpty()) {
            Team team = new Team();
            team.setTeamId(teamId);
            team.setTeamName(teamId);
            team.setTeamDescription(String.format("User to read on %s in %s", dwh ? "all tables" : "_dm", contextKey));
            team.setCreateTime(LocalDateTime.now());
            teamRepository.save(team);
            log.info("Team with id {} not found, created {}", teamId, team);
        }
    }

    private void createCbAuthSubjectIfNotFound(String cbAuthSubject) {
        List<AuthSubject> authSubjects = authSubjectRepository.findBySubjectId(cbAuthSubject);
        if (authSubjects.isEmpty()) {
            AuthSubject authSubject = new AuthSubject();
            authSubject.setSubjectType("R");
            authSubject.setSubjectId(cbAuthSubject);
            authSubject.setIsSecretStorage("Y");
            authSubjectRepository.saveAndFlush(authSubject);
            log.info("Subject with id {} not found, created", cbAuthSubject);
        }
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
