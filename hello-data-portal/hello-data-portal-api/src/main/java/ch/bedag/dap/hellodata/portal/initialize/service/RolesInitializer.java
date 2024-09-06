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
package ch.bedag.dap.hellodata.portal.initialize.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.repositories.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.ContextsNotFetchedYetException;
import ch.bedag.dap.hellodata.portal.user.conf.DefaultAdminProperties;
import ch.bedag.dap.hellodata.portalcommon.role.entity.PortalRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.SystemDefaultPortalRoleName;
import ch.bedag.dap.hellodata.portalcommon.role.entity.UserPortalRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.PortalRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.role.repository.RoleRepository;
import ch.bedag.dap.hellodata.portalcommon.role.repository.UserPortalRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.Permissions;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static ch.bedag.dap.hellodata.portal.base.auth.HellodataAuthenticationConverter.WORKSPACES_PERMISSION;

@Log4j2
@Component
@RequiredArgsConstructor
public class RolesInitializer {
    private final UserPortalRoleRepository userPortalRoleRepository;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleService roleService;
    private final PortalRoleRepository portalRoleRepository;
    private final HdContextRepository contextRepository;
    private final DefaultAdminProperties defaultAdminProperties;
    private final HelloDataContextConfig helloDataContextConfig;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initSystemDefaultPortalRoles() {
        for (SystemDefaultPortalRoleName systemDefaultPortalRoleName : SystemDefaultPortalRoleName.values()) {
            Optional<PortalRoleEntity> byName = portalRoleRepository.findByName(systemDefaultPortalRoleName.name());
            PortalRoleEntity portalRoleEntity = byName.orElseGet(PortalRoleEntity::new);
            portalRoleEntity.setSystemDefault(true);
            portalRoleEntity.setDescription("SYSTEM ROLE");
            portalRoleEntity.setName(systemDefaultPortalRoleName.name());
            Permissions permissions = new Permissions();
            permissions.setPortalPermissions(new ArrayList<>(systemDefaultPortalRoleName.getPermissions().stream().map(Enum::name).toList()));
            if (systemDefaultPortalRoleName == SystemDefaultPortalRoleName.HELLODATA_ADMIN) {
                permissions.getPortalPermissions().add(WORKSPACES_PERMISSION);
            }
            portalRoleEntity.setPermissions(permissions);
            portalRoleRepository.save(portalRoleEntity);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initContextRoles() {
        if (contextRepository.count() == 0) {
            throw new ContextsNotFetchedYetException();
        }
        log.info("Checking if context roles are added:");
        List<RoleEntity> all = roleRepository.findAll();
        log.info("\tFound {} roles", all.size());
        for (HdRoleName roleEnum : HdRoleName.values()) {
            Optional<RoleEntity> foundByName = roleRepository.findByName(roleEnum);
            if (foundByName.isPresent()) {
                RoleEntity roleEntity = foundByName.get();
                log.info("\tFound role by name {} and context type {}", roleEntity.getName(), roleEntity.getContextType());
                if (roleEntity.getContextType() != roleEnum.getContextType()) {
                    log.warn("\tWARNING! The context types doesn't match expected {}, actual {}", roleEnum.getContextType(), roleEntity.getContextType());
                } else {
                    log.info("\tThe configuration of role is correct");
                }
            } else {
                log.info("\tRole by name {} not found, creating...", roleEnum.name());
                RoleEntity entity = new RoleEntity();
                entity.setName(roleEnum);
                entity.setContextType(roleEnum.getContextType());
                roleRepository.save(entity);
                log.info("\tAdded role {} for context type {}", entity.getName(), entity.getContextType());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initContextRolesToUsers() {
        List<UserEntity> all = userRepository.findAll();
        for (UserEntity user : all) {
            if (defaultAdminProperties.getEmail().equalsIgnoreCase(user.getEmail()) && user.getContextRoles() == null) {
                roleService.createNoneContextRoles(user);
                roleService.setBusinessDomainRoleForUser(user, HdRoleName.HELLODATA_ADMIN);
                roleService.setAllDataDomainRolesForUser(user, HdRoleName.DATA_DOMAIN_ADMIN);
            }
            if (!defaultAdminProperties.getEmail().equalsIgnoreCase(user.getEmail()) && user.getContextRoles() == null) {
                roleService.createNoneContextRoles(user);
                roleService.setBusinessDomainRoleForUser(user, HdRoleName.NONE);
                roleService.setAllDataDomainRolesForUser(user, HdRoleName.NONE);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initDefaultUserAsSuperuser() {
        Optional<UserEntity> userEntityByEmail = userRepository.findUserEntityByEmailIgnoreCase(defaultAdminProperties.getEmail());
        if (userEntityByEmail.isPresent() && userEntityByEmail.get().getPermissionsFromAllRoles().isEmpty()) {
            UserEntity user = userEntityByEmail.get();
            UserPortalRoleEntity userPortalRoleEntity = new UserPortalRoleEntity();
            userPortalRoleEntity.setUser(user);
            userPortalRoleEntity.setContextType(HdContextType.BUSINESS_DOMAIN);
            userPortalRoleEntity.setContextKey(helloDataContextConfig.getBusinessContext().getKey());
            Optional<PortalRoleEntity> portalRole = portalRoleRepository.findByName(SystemDefaultPortalRoleName.HELLODATA_ADMIN.name());
            if (portalRole.isPresent()) {
                userPortalRoleEntity.setRole(portalRole.get());
                userPortalRoleRepository.save(userPortalRoleEntity);
            }
        }
    }
}
