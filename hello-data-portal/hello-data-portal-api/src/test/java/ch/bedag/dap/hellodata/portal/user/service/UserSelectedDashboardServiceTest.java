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
package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.portal.user.entity.UserSelectedDashboardEntity;
import ch.bedag.dap.hellodata.portal.user.repository.UserSelectedDashboardRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@ExtendWith(MockitoExtension.class)
class UserSelectedDashboardServiceTest {

    @InjectMocks
    private UserSelectedDashboardService userSelectedDashboardService;

    @Mock
    private UserSelectedDashboardRepository repository;

    @Test
    void testGetSelectedDashboardIds() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "test-context";

        UserSelectedDashboardEntity entity1 = new UserSelectedDashboardEntity();
        entity1.setDashboardId(1);
        UserSelectedDashboardEntity entity2 = new UserSelectedDashboardEntity();
        entity2.setDashboardId(2);

        when(repository.findAllByUserIdAndContextKey(userId, contextKey))
                .thenReturn(List.of(entity1, entity2));

        // when
        Set<Integer> result = userSelectedDashboardService.getSelectedDashboardIds(userId, contextKey);

        // then
        assertEquals(2, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
    }

    @Test
    void testGetSelectedDashboardIdsEmpty() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "test-context";

        when(repository.findAllByUserIdAndContextKey(userId, contextKey))
                .thenReturn(List.of());

        // when
        Set<Integer> result = userSelectedDashboardService.getSelectedDashboardIds(userId, contextKey);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void testSaveSelectedDashboards() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "test-context";
        List<UserSelectedDashboardService.DashboardSelection> selections = List.of(
                new UserSelectedDashboardService.DashboardSelection(1, "Dashboard 1", "superset-instance"),
                new UserSelectedDashboardService.DashboardSelection(2, "Dashboard 2", "superset-instance")
        );

        // when
        userSelectedDashboardService.saveSelectedDashboards(userId, contextKey, selections);

        // then
        verify(repository).deleteAllByUserIdAndContextKey(userId, contextKey);
        verify(repository, times(2)).save(any(UserSelectedDashboardEntity.class));
    }

    @Test
    void testSaveSelectedDashboardsCreatesCorrectEntities() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "test-context";
        List<UserSelectedDashboardService.DashboardSelection> selections = List.of(
                new UserSelectedDashboardService.DashboardSelection(1, "Dashboard 1", "superset-instance")
        );

        ArgumentCaptor<UserSelectedDashboardEntity> captor = ArgumentCaptor.forClass(UserSelectedDashboardEntity.class);

        // when
        userSelectedDashboardService.saveSelectedDashboards(userId, contextKey, selections);

        // then
        verify(repository).save(captor.capture());
        UserSelectedDashboardEntity saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(1, saved.getDashboardId());
        assertEquals("Dashboard 1", saved.getDashboardTitle());
        assertEquals(contextKey, saved.getContextKey());
        assertEquals("superset-instance", saved.getInstanceName());
    }

    @Test
    void testRemoveAllForUser() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        userSelectedDashboardService.removeAllForUser(userId);

        // then
        verify(repository).deleteAllByUserId(userId);
    }

    @Test
    void testRemoveAllForUserInContext() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "test-context";

        // when
        userSelectedDashboardService.removeAllForUserInContext(userId, contextKey);

        // then
        verify(repository).deleteAllByUserIdAndContextKey(userId, contextKey);
    }

    @Test
    void testCleanupStaleDashboards() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "test-context";
        Set<Integer> validDashboardIds = Set.of(1, 2);

        UserSelectedDashboardEntity valid1 = new UserSelectedDashboardEntity();
        valid1.setId(UUID.randomUUID());
        valid1.setDashboardId(1);

        UserSelectedDashboardEntity valid2 = new UserSelectedDashboardEntity();
        valid2.setId(UUID.randomUUID());
        valid2.setDashboardId(2);

        UserSelectedDashboardEntity stale = new UserSelectedDashboardEntity();
        stale.setId(UUID.randomUUID());
        stale.setDashboardId(999); // not in validDashboardIds

        when(repository.findAllByUserIdAndContextKey(userId, contextKey))
                .thenReturn(List.of(valid1, valid2, stale));

        // when
        userSelectedDashboardService.cleanupStaleDashboards(userId, contextKey, validDashboardIds);

        // then
        ArgumentCaptor<List<UserSelectedDashboardEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).deleteAll(captor.capture());
        List<UserSelectedDashboardEntity> deleted = captor.getValue();
        assertEquals(1, deleted.size());
        assertEquals(999, deleted.get(0).getDashboardId());
    }

    @Test
    void testCleanupStaleDashboardsNoStaleEntries() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "test-context";
        Set<Integer> validDashboardIds = Set.of(1, 2);

        UserSelectedDashboardEntity valid1 = new UserSelectedDashboardEntity();
        valid1.setDashboardId(1);
        UserSelectedDashboardEntity valid2 = new UserSelectedDashboardEntity();
        valid2.setDashboardId(2);

        when(repository.findAllByUserIdAndContextKey(userId, contextKey))
                .thenReturn(List.of(valid1, valid2));

        // when
        userSelectedDashboardService.cleanupStaleDashboards(userId, contextKey, validDashboardIds);

        // then
        verify(repository, never()).deleteAll(anyList());
    }

    @Test
    void testIsEmpty() {
        // given
        when(repository.count()).thenReturn(0L);

        // when
        boolean result = userSelectedDashboardService.isEmpty();

        // then
        assertTrue(result);
    }

    @Test
    void testIsEmptyReturnsFalseWhenNotEmpty() {
        // given
        when(repository.count()).thenReturn(5L);

        // when
        boolean result = userSelectedDashboardService.isEmpty();

        // then
        assertFalse(result);
    }
}
