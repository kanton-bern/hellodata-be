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
package ch.bedag.dap.hellodata.portal.role.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.RoleRepository;
import ch.bedag.dap.hellodata.portalcommon.role.repository.UserContextRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for roles in contexts
 */
@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;
    private final HelloDataContextConfig helloDataContextConfig;
    private final UserContextRoleRepository userContextRoleRepository;
    private final HdContextRepository contextRepository;

    @Transactional(readOnly = true)
    public List<RoleDto> getAll() {
        return roleRepository.findAll().stream().map(roleEntity -> {
            RoleDto dto = modelMapper.map(roleEntity, RoleDto.class);
            if (roleEntity.getContextType() != null) {
                dto.setContextType(roleEntity.getContextType());
            }
            return dto;
        }).toList();
    }

    @Transactional
    public void updateBusinessRoleForUser(UserEntity userEntity, RoleDto businessDomainRole) {
        updateDomainRoleForUserInternal(userEntity, businessDomainRole, helloDataContextConfig.getBusinessContext().getKey());
    }

    @Transactional
    public void updateDomainRoleForUser(UserEntity userEntity, RoleDto domainRole, String contextKey) {
        updateDomainRoleForUserInternal(userEntity, domainRole, contextKey);
    }

    private void updateDomainRoleForUserInternal(UserEntity userEntity, RoleDto domainRole, String contextKey) {
        long alreadyHasContextRole = CollectionUtils.emptyIfNull(userEntity.getContextRoles())
                .stream()
                .filter(contextRole -> contextRole.getContextKey().equalsIgnoreCase(contextKey) &&
                        contextRole.getRole().getId().equals(domainRole.getId()))
                .count();
        if (alreadyHasContextRole > 0) {
            return;
        }
        Optional<UserContextRoleEntity> contextRoleForEdition =
                CollectionUtils.emptyIfNull(userEntity.getContextRoles()).stream().filter(contextRole -> contextRole.getContextKey().equalsIgnoreCase(contextKey)).findFirst();
        RoleEntity roleEntity = roleRepository.getReferenceById(domainRole.getId());
        if (contextRoleForEdition.isPresent()) {
            UserContextRoleEntity userContextRoleEntity = contextRoleForEdition.get();
            userContextRoleEntity.setRole(roleEntity);
            userContextRoleRepository.saveAndFlush(userContextRoleEntity);
        } else {
            UserContextRoleEntity userContextRoleEntity = new UserContextRoleEntity();
            userContextRoleEntity.setUser(userEntity);
            userContextRoleEntity.setContextKey(contextKey);
            userContextRoleEntity.setRole(roleEntity);
            if (userEntity.getContextRoles() == null) {
                userEntity.setContextRoles(Set.of(userContextRoleEntity));
            } else {
                userEntity.getContextRoles().add(userContextRoleEntity);
            }
        }
    }

    @Transactional
    public void setBusinessDomainRoleForUser(UserEntity userEntity, HdRoleName roleName) {
        log.info("Setting business domain role {} for user {}", roleName, userEntity.getUsername());
        Set<UserContextRoleEntity> contextRoles = userEntity.getContextRoles();
        long alreadyHasTheRole = CollectionUtils.emptyIfNull(contextRoles)
                .stream()
                .filter(contextRole -> contextRole.getContextKey().equalsIgnoreCase(helloDataContextConfig.getBusinessContext().getKey()) &&
                        contextRole.getRole().getName() == roleName)
                .count();
        if (alreadyHasTheRole == 1) {
            return;
        }
        Optional<UserContextRoleEntity> businessDomainRoleExists =
                CollectionUtils.emptyIfNull(contextRoles).stream().filter(contextRole -> contextRole.getRole().getContextType() == HdContextType.BUSINESS_DOMAIN).findFirst();
        log.info("Found business domain role {} for user {} with context roles {}", roleName, userEntity.getUsername(), contextRoles);
        businessDomainRoleExists.ifPresent(contextRoles::remove);
        UserContextRoleEntity userContextRoleEntity = new UserContextRoleEntity();
        userContextRoleEntity.setUser(userEntity);
        userContextRoleEntity.setContextKey(helloDataContextConfig.getBusinessContext().getKey());
        Optional<RoleEntity> role = roleRepository.findByName(roleName);
        role.ifPresent(userContextRoleEntity::setRole);
        userContextRoleRepository.save(userContextRoleEntity);
    }

    @Transactional
    public void setAllDataDomainRolesForUser(UserEntity userEntity, HdRoleName name) {
        Optional<RoleEntity> role = roleRepository.findByName(name);
        if (role.isPresent()) {
            List<UserContextRoleEntity> userContextRoleEntities = CollectionUtils.emptyIfNull(userEntity.getContextRoles()).stream().filter(userContextRoleEntity -> {
                String contextKey = userContextRoleEntity.getContextKey();
                return contextRepository.existsByContextKeyAndType(contextKey, HdContextType.DATA_DOMAIN);
            }).toList();

            List<HdContextEntity> allDataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
            for (HdContextEntity dataDomain : allDataDomains) {
                UserContextRoleEntity userContextRoleEntity =
                        userContextRoleEntities.stream().filter(entity -> entity.getContextKey().equalsIgnoreCase(dataDomain.getContextKey())).findFirst().orElse(null);
                if (userContextRoleEntity == null) {
                    userContextRoleEntity = new UserContextRoleEntity();
                    userContextRoleEntity.setContextKey(dataDomain.getContextKey());
                    userContextRoleEntity.setUser(userEntity);
                    userContextRoleEntity.setRole(role.get());
                } else {
                    userContextRoleEntity.setRole(role.get());
                }
                userContextRoleRepository.save(userContextRoleEntity);
            }
        }
    }

    @Transactional
    public void createNoneContextRoles(UserEntity user) {
        List<HdContextEntity> all = contextRepository.findAll();
        Optional<RoleEntity> noneRole = roleRepository.findByName(HdRoleName.NONE);
        if (noneRole.isPresent()) {
            for (HdContextEntity hdContextEntity : all) {
                UserContextRoleEntity userContextRoleEntity = new UserContextRoleEntity();
                userContextRoleEntity.setUser(user);
                userContextRoleEntity.setContextKey(hdContextEntity.getContextKey());
                userContextRoleEntity.setRole(noneRole.get());
                userContextRoleRepository.save(userContextRoleEntity);
            }
        } else {
            throw new RuntimeException("NONE role not found!"); //NOSONAR
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addContextRoleToUser(UserEntity user, String contextKey, HdRoleName roleName) {
        Optional<RoleEntity> noneRole = roleRepository.findByName(roleName);
        if (noneRole.isPresent()) {
            UserContextRoleEntity userContextRoleEntity = new UserContextRoleEntity();
            userContextRoleEntity.setUser(user);
            userContextRoleEntity.setContextKey(contextKey);
            userContextRoleEntity.setRole(noneRole.get());
            userContextRoleRepository.saveAndFlush(userContextRoleEntity);
        } else {
            throw new RuntimeException("Role not found!"); //NOSONAR
        }
    }

    @Transactional(readOnly = true)
    public List<UserContextRoleEntity> getAllContextRolesForUser(UserEntity userEntity) {
        return userContextRoleRepository.findAllByUser(userEntity);
    }
}
