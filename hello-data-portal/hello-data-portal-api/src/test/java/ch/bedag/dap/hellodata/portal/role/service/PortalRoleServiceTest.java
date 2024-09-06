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
import ch.bedag.dap.hellodata.portalcommon.role.entity.PortalRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.PortalRoleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
public class PortalRoleServiceTest {

    @Mock
    private PortalRoleRepository portalRoleRepository;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private PortalRoleService portalRoleService;

    @Test
    public void testGetAllRoles() {
        // given
        List<PortalRoleEntity> roleEntities = new ArrayList<>();
        when(portalRoleRepository.findAll()).thenReturn(roleEntities);

        // when
        List<PortalRoleDto> result = portalRoleService.getAllRoles();

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetRoleById() {
        // given
        UUID roleId = UUID.randomUUID();
        PortalRoleEntity roleEntity = new PortalRoleEntity();
        when(portalRoleRepository.findById(roleId)).thenReturn(Optional.of(roleEntity));
        PortalRoleDto expectedDto = new PortalRoleDto(); // Create an expected DTO

        when(modelMapper.map(roleEntity, PortalRoleDto.class)).thenReturn(expectedDto);

        // when
        PortalRoleDto result = portalRoleService.getRoleById(roleId);

        // then
        assertNotNull(result);
        assertEquals(expectedDto, result);
    }

    @Test
    public void testGetRoleById_NotFound() {
        // given
        UUID roleId = UUID.randomUUID();
        when(portalRoleRepository.findById(roleId)).thenReturn(Optional.empty());

        // when then
        assertThrows(ResponseStatusException.class, () -> portalRoleService.getRoleById(roleId));
    }

    @Test
    public void testCreateRole() {
        // given
        PortalRoleCreateDto createDto = new PortalRoleCreateDto();
        createDto.setName("TestRole");

        PortalRoleEntity savedEntity = new PortalRoleEntity();
        savedEntity.setId(UUID.randomUUID());
        when(modelMapper.map(createDto, PortalRoleEntity.class)).thenReturn(savedEntity);

        // when
        portalRoleService.createRole(createDto);

        // then
        verify(portalRoleRepository).save(savedEntity);
    }

    @Test
    public void testUpdateRole() {
        // given
        UUID roleId = UUID.randomUUID();
        PortalRoleUpdateDto updateDto = new PortalRoleUpdateDto();
        updateDto.setId(roleId);
        updateDto.setName("UpdatedRole");

        PortalRoleEntity existingEntity = new PortalRoleEntity();
        existingEntity.setId(roleId);
        existingEntity.setName("OldRole");
        when(portalRoleRepository.findById(roleId)).thenReturn(Optional.of(existingEntity));

        // when
        portalRoleService.updateRole(updateDto);

        // then
        verify(portalRoleRepository).save(existingEntity);
        assertEquals("UpdatedRole", existingEntity.getName());
    }

    @Test
    public void testUpdateRole_NotFound() {
        // given
        UUID roleId = UUID.randomUUID();
        PortalRoleUpdateDto updateDto = new PortalRoleUpdateDto();
        updateDto.setId(roleId);

        when(portalRoleRepository.findById(roleId)).thenReturn(Optional.empty());

        // when
        assertThrows(ResponseStatusException.class, () -> portalRoleService.updateRole(updateDto));
    }

    @Test
    public void testDeleteRole() {
        // given
        UUID roleId = UUID.randomUUID();
        PortalRoleEntity roleEntity = new PortalRoleEntity();
        roleEntity.setId(roleId);
        when(portalRoleRepository.findById(roleId)).thenReturn(Optional.of(roleEntity));

        // when
        portalRoleService.deleteRole(roleId);

        // then
        verify(portalRoleRepository).deleteById(roleId);
    }

    @Test
    public void testDeleteRole_NotFound() {
        // given
        UUID roleId = UUID.randomUUID();
        when(portalRoleRepository.findById(roleId)).thenReturn(Optional.empty());

        // when then
        assertThrows(ResponseStatusException.class, () -> portalRoleService.deleteRole(roleId));
    }
}

