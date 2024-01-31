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

import ch.bedag.dap.hellodata.portal.role.data.PortalRoleCreateDto;
import ch.bedag.dap.hellodata.portal.role.data.PortalRoleDto;
import ch.bedag.dap.hellodata.portal.role.data.PortalRoleUpdateDto;
import ch.bedag.dap.hellodata.portal.role.entity.PortalRoleEntity;
import ch.bedag.dap.hellodata.portal.role.entity.SystemDefaultPortalRoleName;
import ch.bedag.dap.hellodata.portal.role.repository.PortalRoleRepository;
import ch.bedag.dap.hellodata.portal.user.entity.Permissions;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class PortalRoleService {
    private final PortalRoleRepository portalRoleRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<PortalRoleDto> getAllRoles() {
        return findAllAndOrderBySystemDefaultThenName().stream().map(portalRoleEntity -> modelMapper.map(portalRoleEntity, PortalRoleDto.class)).toList();
    }

    /**
     * First system default ones at the top, then sort by name
     *
     * @return
     */
    private List<PortalRoleEntity> findAllAndOrderBySystemDefaultThenName() {
        List<PortalRoleEntity> portalRoles = portalRoleRepository.findAll();
        portalRoles.sort((role1, role2) -> {
            if (role1.isSystemDefault() != role2.isSystemDefault()) {
                return role1.isSystemDefault() ? -1 : 1;
            } else if (role1.isSystemDefault()) {
                return compareSystemDefaultRoles(role1.getName(), role2.getName());
            } else {
                return role1.getName().compareTo(role2.getName());
            }
        });
        return portalRoles;
    }

    private int compareSystemDefaultRoles(String roleName1, String roleName2) {
        String[] defaultRolesOrder = { SystemDefaultPortalRoleName.HELLODATA_ADMIN.name(), SystemDefaultPortalRoleName.BUSINESS_DOMAIN_ADMIN.name(),
                SystemDefaultPortalRoleName.DATA_DOMAIN_ADMIN.name(), SystemDefaultPortalRoleName.DATA_DOMAIN_EDITOR.name(),
                SystemDefaultPortalRoleName.DATA_DOMAIN_VIEWER.name() };
        int index1 = java.util.Arrays.asList(defaultRolesOrder).indexOf(roleName1);
        int index2 = java.util.Arrays.asList(defaultRolesOrder).indexOf(roleName2);
        return Integer.compare(index1, index2);
    }

    @Transactional(readOnly = true)
    public PortalRoleDto getRoleById(UUID roleId) {
        Optional<PortalRoleEntity> entity = portalRoleRepository.findById(roleId);
        if (entity.isEmpty()) {
            log.error("Role with id {} not found", roleId);//NOSONAR
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return modelMapper.map(entity.get(), PortalRoleDto.class);
    }

    public void createRole(PortalRoleCreateDto portalRoleCreateDto) {
        PortalRoleEntity entity = modelMapper.map(portalRoleCreateDto, PortalRoleEntity.class);
        Permissions permissions = new Permissions();
        permissions.setPortalPermissions(portalRoleCreateDto.getPermissions());
        entity.setPermissions(permissions);
        entity.setSystemDefault(false);
        portalRoleRepository.save(entity);
    }

    public void updateRole(PortalRoleUpdateDto roleUpdateDto) {
        Optional<PortalRoleEntity> entity = portalRoleRepository.findById(roleUpdateDto.getId());
        if (entity.isEmpty()) {
            log.error("Role with id {} not found", roleUpdateDto.getId());//NOSONAR
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        PortalRoleEntity entityToUpdate = entity.get();
        if (entityToUpdate.isSystemDefault()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update a system default role");
        }
        modelMapper.map(roleUpdateDto, entityToUpdate);
        Permissions permissions = new Permissions();
        permissions.setPortalPermissions(roleUpdateDto.getPermissions());
        entityToUpdate.setPermissions(permissions);
        portalRoleRepository.save(entityToUpdate);
    }

    public void deleteRole(UUID id) {
        Optional<PortalRoleEntity> entity = portalRoleRepository.findById(id);
        if (entity.isEmpty()) {
            log.error("Role with id {} not found", id);//NOSONAR
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (entity.get().isSystemDefault()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete a system default role");
        }
        portalRoleRepository.deleteById(id);
    }
}
