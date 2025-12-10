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
package ch.bedag.dap.hellodata.docs.service;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.docs.conf.DefaultAdminProperties;
import ch.bedag.dap.hellodata.docs.entities.Privilege;
import ch.bedag.dap.hellodata.docs.entities.Role;
import ch.bedag.dap.hellodata.docs.entities.User;
import ch.bedag.dap.hellodata.docs.model.ProjectDoc;
import ch.bedag.dap.hellodata.docs.repository.PrivilegeRepository;
import ch.bedag.dap.hellodata.docs.repository.RoleRepository;
import ch.bedag.dap.hellodata.docs.repository.UserRepository;
import ch.bedag.dap.hellodata.docs.security.exception.NotAuthorizedException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Log4j2
@Service
public class SecurityService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final DefaultAdminProperties adminProperties;

    private final ProjectDocService projectDocService;

    public SecurityService(UserRepository userRepository, RoleRepository roleRepository, PrivilegeRepository privilegeRepository, DefaultAdminProperties adminProperties,
                           ProjectDocService projectDocService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
        this.adminProperties = adminProperties;
        this.projectDocService = projectDocService;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<Privilege> getAllPrivileges() {
        return privilegeRepository.findAll();
    }

    @Transactional
    public void createOrUpdateAdminUser() {
        User adminUser = userRepository.findByUserNameOrEmail(adminProperties.getUsername(), adminProperties.getEmail());
        if (adminUser == null) {
            adminUser = new User(adminProperties.getUsername(), adminProperties.getEmail());
        } else {
            adminUser.setEmail(adminProperties.getEmail());
            adminUser.setUserName(adminProperties.getUsername());
        }
        adminUser.setFirstName(adminProperties.getFirstName());
        adminUser.setLastName(adminProperties.getLastName());
        adminUser.setRoles(getAdminRoles());
        adminUser.setSuperuser(true);
        adminUser.setEnabled(true);
        userRepository.save(adminUser);
    }

    @Transactional
    public void createProjectRoles(List<Privilege> defaultPrivileges, Set<String> contextKeys) {
        createProjectRolesInternal(defaultPrivileges, contextKeys);
    }

    private List<Role> createProjectRolesInternal(List<Privilege> defaultPrivileges, Set<String> contextKeys) {
        return contextKeys.stream().filter(contextKey -> !contextKey.equalsIgnoreCase(Role.ADMIN_ROLE_KEY)).map(contextKey -> {
            String roleName = "ROLE_" + SlugifyUtil.slugify(contextKey).toUpperCase();
            return createRoleIfNotFoundInternal(contextKey, roleName, defaultPrivileges);
        }).toList();
    }

    @Transactional
    public void updateRoles(Set<String> contextKeys) {
        List<Role> currentRoles = getAllRolesMinusAdminRole();
        Privilege readPrivilege = createPrivilegeIfNotFoundInternal(Privilege.READ_PRIVILEGE);
        List<Role> newRoles = createProjectRolesInternal(Collections.singletonList(readPrivilege), contextKeys);
        log.info("Found {} current roles.", newRoles.size());
        boolean itemsToRemove = currentRoles.removeAll(newRoles);
        if (itemsToRemove) {
            log.info("Going to disable {} roles: {}", currentRoles.size(), currentRoles);
            for (Role r : currentRoles) {
                if (r.isEnabled()) {
                    r.setEnabled(false);
                    roleRepository.save(r);
                    log.info("Deactivated role with key {}", r.getKey());
                }
            }
            return;
        }
        log.debug("Roles didn't change. Nothing to do.");
    }

    @Transactional
    public void createRoleIfNotFound(String key, String name, Collection<Privilege> privileges) {
        createRoleIfNotFoundInternal(key, name, privileges);
    }

    private Role createRoleIfNotFoundInternal(String key, String name, Collection<Privilege> privileges) {
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
        return role;
    }

    @Transactional
    public Privilege createPrivilegeIfNotFound(String name) {
        return createPrivilegeIfNotFoundInternal(name);
    }

    private Privilege createPrivilegeIfNotFoundInternal(String name) {
        Privilege privilege = privilegeRepository.findByName(name);
        if (privilege == null) {
            privilege = new Privilege(name);
            privilegeRepository.save(privilege);
        }
        return privilege;
    }

    public List<ProjectDoc> getProjectDocsFilteredByUser(User user, List<ProjectDoc> allProjectsDocs) {
        log.debug("User {} requested a list of all project-docs.", user.getUserName());
        Collection<Role> userRoles = user.getRoles();
        return allProjectsDocs.stream()
                .filter(projectDoc -> canAccessProject(userRoles, projectDoc.contextKey()))
                .sorted((a, b) -> a.contextKey().compareToIgnoreCase(b.contextKey()))
                .toList();
    }

    public void validateUserIsAllowedOnProjectDoc(User loggedInUser, String projectName) {
        log.debug("User {} requested info docs on project {}.", loggedInUser.getUserName(), projectName);
        if (!canAccessProject(loggedInUser.getRoles(), projectName)) {
            throw new NotAuthorizedException(projectName);
        }
    }

    public boolean canAccessProject(Collection<Role> userRoles, String dataDomainKey) {
        return userRoles.stream()
                .anyMatch(ur -> ur.getKey().equals(Role.ADMIN_ROLE_KEY) || (ur.isEnabled() && ur.getKey().equalsIgnoreCase(dataDomainKey) &&
                        ur.getPrivileges().stream().anyMatch(p -> p.getName().equals(Privilege.READ_PRIVILEGE))));
    }

    /**
     * Role-Authorization for endpoint 'projects-docs/get-by-path' get already checked in DocsAuthorizationManager::check
     **/
    public void validateIsAllowedToAccessPath(User loggedInUser, String path) {
        List<ProjectDoc> allProjectsDocs = projectDocService.getAllProjectsDocs();
        ProjectDoc requestedProjectDoc = allProjectsDocs.stream().filter(projectDoc -> projectDoc.path().equalsIgnoreCase(path)).findFirst().orElse(null);
        if (requestedProjectDoc == null) {
            throw new RuntimeException("Could not find a project for path " + path); //NOSONAR
        }
        validateUserIsAllowedOnProjectDoc(loggedInUser, requestedProjectDoc.name());
    }

    private List<Role> getAdminRoles() {
        Role adminRole = roleRepository.findByKeyIgnoreCase(Role.ADMIN_ROLE_KEY);
        List<Role> roleList = new ArrayList<>();
        roleList.add(adminRole);
        return roleList;
    }

    private List<Role> getAllRolesMinusAdminRole() {
        List<Role> currentRoles = getAllRoles();
        currentRoles.removeIf(r -> r.getKey().equals(Role.ADMIN_ROLE_KEY));
        return currentRoles;
    }
}
