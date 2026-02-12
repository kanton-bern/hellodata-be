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
package ch.bedag.dap.hellodata.portal.dashboard_group.service;

import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupCreateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupUpdateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntry;
import ch.bedag.dap.hellodata.portal.dashboard_group.repository.DashboardGroupRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
class DashboardGroupServiceTest {

    @InjectMocks
    private DashboardGroupService dashboardGroupService;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @Mock
    private DashboardGroupRepository dashboardGroupRepository;

    @Test
    void testCreate() {
        // given
        DashboardGroupCreateDto createDto = new DashboardGroupCreateDto();
        createDto.setName("Test Group");
        createDto.setEntries(List.of(new DashboardGroupEntry("ctx1", 1, "Dashboard 1")));

        // when
        dashboardGroupService.create(createDto);

        // then
        verify(dashboardGroupRepository, times(1)).save(any(DashboardGroupEntity.class));
    }

    @Test
    void testUpdate() {
        // given
        DashboardGroupUpdateDto updateDto = new DashboardGroupUpdateDto();
        updateDto.setId(UUID.randomUUID());
        updateDto.setName("Updated Group");
        updateDto.setEntries(List.of(new DashboardGroupEntry("ctx1", 1, "Dashboard 1")));

        DashboardGroupEntity existingEntity = new DashboardGroupEntity();
        existingEntity.setId(updateDto.getId());
        when(dashboardGroupRepository.findById(updateDto.getId())).thenReturn(Optional.of(existingEntity));

        // when
        dashboardGroupService.update(updateDto);

        // then
        verify(dashboardGroupRepository, times(1)).save(existingEntity);
    }

    @Test
    void testUpdateNotFound() {
        // given
        DashboardGroupUpdateDto updateDto = new DashboardGroupUpdateDto();
        updateDto.setId(UUID.randomUUID());

        when(dashboardGroupRepository.findById(updateDto.getId())).thenReturn(Optional.empty());

        // when
        assertThrows(ResponseStatusException.class, () -> dashboardGroupService.update(updateDto));

        // then
        verify(dashboardGroupRepository, never()).save(any(DashboardGroupEntity.class));
    }

    @Test
    void testDelete() {
        // given
        UUID groupId = UUID.randomUUID();

        // when
        dashboardGroupService.delete(groupId);

        // then
        verify(dashboardGroupRepository, times(1)).deleteById(groupId);
    }

    @Test
    void testGetById() {
        // given
        UUID groupId = UUID.randomUUID();

        DashboardGroupEntity existingEntity = new DashboardGroupEntity();
        existingEntity.setId(groupId);
        existingEntity.setName("Test Group");
        when(dashboardGroupRepository.findById(groupId)).thenReturn(Optional.of(existingEntity));

        // when
        DashboardGroupDto resultDto = dashboardGroupService.getDashboardGroupById(groupId);

        // then
        assertNotNull(resultDto);
        assertEquals(groupId.toString(), resultDto.getId());
        assertEquals("Test Group", resultDto.getName());
    }

    @Test
    void testGetByIdNotFound() {
        // given
        UUID groupId = UUID.randomUUID();

        when(dashboardGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        // when
        assertThrows(ResponseStatusException.class, () -> dashboardGroupService.getDashboardGroupById(groupId));

        // then
        verify(modelMapper, never()).map(any(DashboardGroupEntity.class), eq(DashboardGroupDto.class));
    }
}
