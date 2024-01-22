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

import ch.bedag.dap.hellodata.commons.metainfomodel.entities.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repositories.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portal.role.entity.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portal.role.repository.RoleRepository;
import ch.bedag.dap.hellodata.portal.role.repository.UserContextRoleRepository;
import ch.bedag.dap.hellodata.portal.user.entity.UserEntity;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserContextRoleRepository userContextRoleRepository;

    @Mock
    private HelloDataContextConfig helloDataContextConfig;

    @Mock
    private HdContextRepository contextRepository;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private RoleService roleService;

    @Test
    public void testGetAll() {
        // given
        List<RoleEntity> roleEntities = Collections.singletonList(new RoleEntity());
        when(roleRepository.findAll()).thenReturn(roleEntities);

        List<RoleDto> result = roleService.getAll();

        // when
        assertEquals(1, result.size());

        // then
        verify(roleRepository, times(1)).findAll();
    }

    @Test
    public void testUpdateBusinessRoleForUser() {
        // given
        UserEntity userEntity = new UserEntity();
        RoleDto roleDto = new RoleDto();
        roleDto.setId(UUID.randomUUID());

        UserContextRoleEntity userContextRoleEntity = setUpContextRoles(userEntity);
        when(userContextRoleRepository.saveAndFlush(any())).thenReturn(userContextRoleEntity);
        setUpBusinessContextConfig();

        // when
        roleService.updateBusinessRoleForUser(userEntity, roleDto);

        // then
        verify(userContextRoleRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void testUpdateDomainRoleForUser_should_add_new_role() {
        // given
        UserEntity userEntity = new UserEntity();
        setUpContextRoles(userEntity);
        RoleDto roleDto = new RoleDto();
        roleDto.setId(UUID.randomUUID());

        // when
        roleService.updateDomainRoleForUser(userEntity, roleDto, "contextKey");

        // then
        verify(userContextRoleRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testSetBusinessDomainRoleForUser() {
        // given
        UserEntity userEntity = new UserEntity();
        setUpContextRoles(userEntity);
        HdRoleName roleName = HdRoleName.NONE;
        setUpBusinessContextConfig();

        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(new RoleEntity()));

        when(userContextRoleRepository.save(any())).thenReturn(new UserContextRoleEntity());

        // when
        roleService.setBusinessDomainRoleForUser(userEntity, roleName);

        // then
        verify(roleRepository, times(1)).findByName(roleName);
        verify(userContextRoleRepository, times(1)).save(any());
    }

    @Test
    public void testSetAllDataDomainRolesForUser() {
        // given
        UserEntity userEntity = new UserEntity();
        HdRoleName roleName = HdRoleName.NONE;

        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(new RoleEntity()));

        List<HdContextEntity> allDataDomains = Collections.singletonList(new HdContextEntity());
        when(contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN))).thenReturn(allDataDomains);

        when(userContextRoleRepository.save(any())).thenReturn(new UserContextRoleEntity());

        // when
        roleService.setAllDataDomainRolesForUser(userEntity, roleName);

        // then
        verify(roleRepository, times(1)).findByName(roleName);

        verify(contextRepository, times(1)).findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));

        verify(userContextRoleRepository, times(1)).save(any());
    }

    @Test
    public void testCreateNoneContextRoles() {
        // given
        UserEntity userEntity = new UserEntity();

        when(roleRepository.findByName(HdRoleName.NONE)).thenReturn(Optional.of(new RoleEntity()));

        List<HdContextEntity> allContexts = Collections.singletonList(new HdContextEntity());
        when(contextRepository.findAll()).thenReturn(allContexts);

        when(userContextRoleRepository.save(any())).thenReturn(new UserContextRoleEntity());

        // when
        roleService.createNoneContextRoles(userEntity);

        // then
        verify(roleRepository, times(1)).findByName(HdRoleName.NONE);

        verify(contextRepository, times(1)).findAll();

        verify(userContextRoleRepository, times(allContexts.size())).save(any());
    }

    @Test
    public void testGetAllContextRolesForUser() {
        // given
        UserEntity userEntity = new UserEntity();

        List<UserContextRoleEntity> userContextRoles = Collections.singletonList(new UserContextRoleEntity());
        when(userContextRoleRepository.findAllByUser(userEntity)).thenReturn(userContextRoles);

        // when
        List<UserContextRoleEntity> result = roleService.getAllContextRolesForUser(userEntity);

        // then
        assertEquals(userContextRoles, result);

        verify(userContextRoleRepository, times(1)).findAllByUser(userEntity);
    }

    @Test
    public void testAddContextRoleToUser() {
        // given
        UserEntity user = new UserEntity();
        String contextKey = "some_context_key";
        HdRoleName roleName = HdRoleName.NONE;

        RoleEntity mockRoleEntity = new RoleEntity();
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(mockRoleEntity));

        // when
        roleService.addContextRoleToUser(user, contextKey, roleName);

        // then
        verify(userContextRoleRepository, times(1)).saveAndFlush(argThat(argument -> {
            return argument.getUser().equals(user) && argument.getContextKey().equals(contextKey) && argument.getRole().equals(mockRoleEntity);
        }));
    }

    @Test
    public void testAddContextRoleToUserRoleNotFound() {
        // given
        UserEntity user = new UserEntity();
        String contextKey = "some_context_key";
        HdRoleName roleName = HdRoleName.NONE;

        when(roleRepository.findByName(roleName)).thenReturn(Optional.empty());

        // when
        try {
            roleService.addContextRoleToUser(user, contextKey, roleName);
            // If the role is not found, an exception should be thrown, so the test should fail if it reaches this point
            fail("Expected RuntimeException, but method executed successfully");
        } catch (RuntimeException e) {
            // then
            assertEquals("Role not found!", e.getMessage());
        }
    }

    private UserContextRoleEntity setUpContextRoles(UserEntity userEntity) {
        UserContextRoleEntity userContextRoleEntity = new UserContextRoleEntity();
        userContextRoleEntity.setUser(userEntity);
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(UUID.randomUUID());
        roleEntity.setName(HdRoleName.DATA_DOMAIN_ADMIN);
        userContextRoleEntity.setRole(roleEntity);
        userContextRoleEntity.setContextKey("key");
        userEntity.setContextRoles(new HashSet<>(Set.of(userContextRoleEntity)));
        return userContextRoleEntity;
    }

    private void setUpBusinessContextConfig() {
        HelloDataContextConfig.BusinessContext businessContext = new HelloDataContextConfig.BusinessContext();
        businessContext.setName("business context");
        businessContext.setKey("key");
        businessContext.setType("business context type");
        when(helloDataContextConfig.getBusinessContext()).thenReturn(businessContext);
    }
}

