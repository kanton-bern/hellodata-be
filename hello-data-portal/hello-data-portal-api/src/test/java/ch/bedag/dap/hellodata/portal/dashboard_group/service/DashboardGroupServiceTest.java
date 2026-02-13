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

import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupCreateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupDomainUserDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupUpdateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntry;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupUserEntry;
import ch.bedag.dap.hellodata.portal.dashboard_group.repository.DashboardGroupRepository;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.UserContextRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
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

    @Mock
    private UserContextRoleRepository userContextRoleRepository;

    @Test
    void testCreate() {
        // given
        DashboardGroupCreateDto createDto = new DashboardGroupCreateDto();
        createDto.setName("Test Group");
        createDto.setContextKey("ctx1");
        createDto.setEntries(List.of(new DashboardGroupEntry(1, "Dashboard 1")));

        // when
        dashboardGroupService.create(createDto);

        // then
        verify(dashboardGroupRepository, times(1)).save(any(DashboardGroupEntity.class));
    }

    @Test
    void testCreateDuplicateNameInSameContext() {
        // given
        DashboardGroupCreateDto createDto = new DashboardGroupCreateDto();
        createDto.setName("Test Group");
        createDto.setContextKey("ctx1");
        createDto.setEntries(List.of(new DashboardGroupEntry(1, "Dashboard 1")));

        when(dashboardGroupRepository.existsByNameIgnoreCaseAndContextKey("Test Group", "ctx1")).thenReturn(true);

        // when / then
        assertThrows(ResponseStatusException.class, () -> dashboardGroupService.create(createDto));
        verify(dashboardGroupRepository, never()).save(any(DashboardGroupEntity.class));
    }

    @Test
    void testUpdate() {
        // given
        DashboardGroupUpdateDto updateDto = new DashboardGroupUpdateDto();
        updateDto.setId(UUID.randomUUID());
        updateDto.setName("Updated Group");
        updateDto.setContextKey("ctx1");
        updateDto.setEntries(List.of(new DashboardGroupEntry(1, "Dashboard 1")));

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
        existingEntity.setContextKey("ctx1");
        when(dashboardGroupRepository.findById(groupId)).thenReturn(Optional.of(existingEntity));

        // when
        DashboardGroupDto resultDto = dashboardGroupService.getDashboardGroupById(groupId);

        // then
        assertNotNull(resultDto);
        assertEquals(groupId.toString(), resultDto.getId());
        assertEquals("Test Group", resultDto.getName());
        assertEquals("ctx1", resultDto.getContextKey());
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

    @Test
    void testGetEligibleUsersForDomain() {
        // given
        String contextKey = "ctx1";
        UUID userId = UUID.randomUUID();

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail("john@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        RoleEntity role = new RoleEntity();
        role.setName(HdRoleName.DATA_DOMAIN_VIEWER);

        UserContextRoleEntity ucr = new UserContextRoleEntity();
        ucr.setUser(user);
        ucr.setRole(role);
        ucr.setContextKey(contextKey);

        when(userContextRoleRepository.findByContextKeyAndRoleNames(eq(contextKey), anyList()))
                .thenReturn(List.of(ucr));

        // when
        List<DashboardGroupDomainUserDto> result = dashboardGroupService.getEligibleUsersForDomain(contextKey);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("john@example.com", result.get(0).getEmail());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals("Doe", result.get(0).getLastName());
        assertEquals("DATA_DOMAIN_VIEWER", result.get(0).getRoleName());
    }

    @Test
    void testRemoveUserFromDashboardGroupsInDomain_userExistsInGroup() {
        // given
        String contextKey = "ctx1";
        String userId = UUID.randomUUID().toString();

        DashboardGroupUserEntry user1 = new DashboardGroupUserEntry(userId, "user1@example.com", "User", "One", "DATA_DOMAIN_VIEWER");
        DashboardGroupUserEntry user2 = new DashboardGroupUserEntry(UUID.randomUUID().toString(), "user2@example.com", "User", "Two", "DATA_DOMAIN_VIEWER");

        DashboardGroupEntity group = new DashboardGroupEntity();
        group.setId(UUID.randomUUID());
        group.setName("Test Group");
        group.setContextKey(contextKey);
        group.setUsers(new ArrayList<>(List.of(user1, user2)));

        when(dashboardGroupRepository.findByContextKeyAndUserId(contextKey, userId))
                .thenReturn(List.of(group));

        // when
        dashboardGroupService.removeUserFromDashboardGroupsInDomain(userId, contextKey);

        // then
        verify(dashboardGroupRepository).save(group);
        assertEquals(1, group.getUsers().size());
        assertEquals("user2@example.com", group.getUsers().get(0).getEmail());
    }

    @Test
    void testRemoveUserFromDashboardGroupsInDomain_userNotInAnyGroup() {
        // given
        String contextKey = "ctx1";
        String userId = UUID.randomUUID().toString();

        when(dashboardGroupRepository.findByContextKeyAndUserId(contextKey, userId))
                .thenReturn(Collections.emptyList());

        // when
        dashboardGroupService.removeUserFromDashboardGroupsInDomain(userId, contextKey);

        // then
        verify(dashboardGroupRepository, never()).save(any());
    }

    @Test
    void testRemoveUserFromDashboardGroupsInDomain_multipleGroups() {
        // given
        String contextKey = "ctx1";
        String userId = UUID.randomUUID().toString();

        DashboardGroupUserEntry user1 = new DashboardGroupUserEntry(userId, "user1@example.com", "User", "One", "DATA_DOMAIN_VIEWER");
        DashboardGroupUserEntry user2 = new DashboardGroupUserEntry(UUID.randomUUID().toString(), "user2@example.com", "User", "Two", "DATA_DOMAIN_VIEWER");

        DashboardGroupEntity group1 = new DashboardGroupEntity();
        group1.setId(UUID.randomUUID());
        group1.setName("Group 1");
        group1.setContextKey(contextKey);
        group1.setUsers(new ArrayList<>(List.of(user1)));

        DashboardGroupEntity group2 = new DashboardGroupEntity();
        group2.setId(UUID.randomUUID());
        group2.setName("Group 2");
        group2.setContextKey(contextKey);
        group2.setUsers(new ArrayList<>(List.of(user1, user2)));

        when(dashboardGroupRepository.findByContextKeyAndUserId(contextKey, userId))
                .thenReturn(List.of(group1, group2));

        // when
        dashboardGroupService.removeUserFromDashboardGroupsInDomain(userId, contextKey);

        // then
        verify(dashboardGroupRepository, times(2)).save(any());
        assertEquals(0, group1.getUsers().size());
        assertEquals(1, group2.getUsers().size());
    }

    @Test
    void testRemoveUserFromDashboardGroupsInDomain_groupWithNullUsers() {
        // given
        String contextKey = "ctx1";
        String userId = UUID.randomUUID().toString();

        DashboardGroupEntity group = new DashboardGroupEntity();
        group.setId(UUID.randomUUID());
        group.setName("Test Group");
        group.setContextKey(contextKey);
        group.setUsers(null);

        when(dashboardGroupRepository.findByContextKeyAndUserId(contextKey, userId))
                .thenReturn(List.of(group));

        // when
        dashboardGroupService.removeUserFromDashboardGroupsInDomain(userId, contextKey);

        // then
        verify(dashboardGroupRepository, never()).save(any());
    }
}
