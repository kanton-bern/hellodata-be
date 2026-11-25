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
package ch.bedag.dap.hellodata.portal.user_audit.service;

import ch.bedag.dap.hellodata.portal.user_audit.data.UserAuditAction;
import ch.bedag.dap.hellodata.portal.user_audit.data.UserAuditChange;
import ch.bedag.dap.hellodata.portal.user_audit.data.UserAuditSource;
import ch.bedag.dap.hellodata.portal.user_audit.entity.UserAuditLogEntity;
import ch.bedag.dap.hellodata.portal.user_audit.repository.UserAuditLogRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuditServiceTest {

    private final UUID userId = UUID.randomUUID();

    @Mock
    private UserAuditLogRepository userAuditLogRepository;
    @Mock
    private UserStateSnapshotReader snapshotReader;
    @Mock
    private HdContextRepository contextRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private UserAuditService userAuditService;

    private static UserStateSnapshot snapshot(Map<UserStateSnapshot.Key, Object> values) {
        return new UserStateSnapshot("user@example.com", new TreeMap<>(values));
    }

    private static UserStateSnapshot.Key key(String field, String contextKey) {
        return new UserStateSnapshot.Key(field, contextKey);
    }

    private UserAuditLogEntity savedEntity() {
        ArgumentCaptor<UserAuditLogEntity> captor = ArgumentCaptor.forClass(UserAuditLogEntity.class);
        verify(userAuditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void audit_recordsDiffOnce() {
        when(snapshotReader.read(userId)).thenReturn(snapshot(Map.of(key("role", "ctx1"), "NONE")), snapshot(Map.of(key("role", "ctx1"), "DATA_DOMAIN_VIEWER")));

        String result = userAuditService.audit(userId, UserAuditAction.ROLES_CHANGED, UserAuditSource.MANUAL, () -> "result");

        assertEquals("result", result);
        verify(entityManager, times(2)).flush();
        UserAuditLogEntity entity = savedEntity();
        assertEquals(userId, entity.getTargetUserId());
        assertEquals("user@example.com", entity.getTargetEmail());
        assertEquals(UserAuditAction.ROLES_CHANGED, entity.getAction());
        assertEquals(List.of(new UserAuditChange("role", "ctx1", "NONE", "DATA_DOMAIN_VIEWER")), entity.getChanges());
    }

    @Test
    void audit_withoutChanges_recordsNothing() {
        UserStateSnapshot state = snapshot(Map.of(key("enabled", null), true));
        when(snapshotReader.read(userId)).thenReturn(state);

        userAuditService.audit(userId, UserAuditAction.ROLES_CHANGED, UserAuditSource.MANUAL, () -> { });

        verify(userAuditLogRepository, never()).save(any());
    }

    @Test
    void audit_nestedCallForSameUser_isFoldedIntoOutermost() {
        when(snapshotReader.read(userId)).thenReturn(snapshot(Map.of(key("enabled", null), true)), snapshot(Map.of(key("enabled", null), false)));

        userAuditService.audit(userId, UserAuditAction.USER_DISABLED, UserAuditSource.MANUAL, () ->
                userAuditService.audit(userId, UserAuditAction.ROLES_CHANGED, UserAuditSource.MANUAL, () -> { }));

        verify(snapshotReader, times(2)).read(userId);
        assertEquals(UserAuditAction.USER_DISABLED, savedEntity().getAction());
    }

    @Test
    void audit_nestedCallForOtherUser_isAuditedSeparately() {
        UUID otherUser = UUID.randomUUID();
        UserStateSnapshot before = snapshot(Map.of(key("enabled", null), true));
        UserStateSnapshot after = snapshot(Map.of(key("enabled", null), false));
        when(snapshotReader.read(any(UUID.class))).thenReturn(before, before, after, after);

        userAuditService.audit(userId, UserAuditAction.USER_DISABLED, UserAuditSource.MANUAL, () ->
                userAuditService.audit(otherUser, UserAuditAction.USER_DISABLED, UserAuditSource.MANUAL, () -> { }));

        verify(userAuditLogRepository, times(2)).save(any());
    }

    @Test
    void audit_storesPassedSource() {
        when(snapshotReader.read(userId)).thenReturn(snapshot(Map.of()), snapshot(Map.of(key("enabled", null), true)));

        userAuditService.audit(userId, UserAuditAction.USER_CREATED, UserAuditSource.BATCH, () -> { });

        assertEquals(UserAuditSource.BATCH, savedEntity().getSource());
    }

    @Test
    void audit_workThrows_recordsNothingAndAllowsNextAudit() {
        when(snapshotReader.read(userId)).thenReturn(snapshot(Map.of()));

        assertThrows(IllegalStateException.class, () -> userAuditService.audit(userId, UserAuditAction.ROLES_CHANGED, UserAuditSource.MANUAL, () -> {
            throw new IllegalStateException("boom");
        }));
        verify(userAuditLogRepository, never()).save(any());

        // the user is no longer marked as in progress
        userAuditService.audit(userId, UserAuditAction.ROLES_CHANGED, UserAuditSource.MANUAL, () -> { });
        verify(snapshotReader, times(3)).read(userId);
    }

    @Test
    void record_setsContextKeyOnlyWhenAllChangesShareOne() {
        userAuditService.recordChanges(userId, "user@example.com", UserAuditAction.ROLES_CHANGED, UserAuditSource.MANUAL,
                List.of(new UserAuditChange("role", "ctx1", "A", "B"), new UserAuditChange("dashboards", "ctx1", null, List.of("D"))));

        assertEquals("ctx1", savedEntity().getContextKey());
    }

    @Test
    void record_noContextKeyWhenChangesSpanContexts() {
        userAuditService.recordChanges(userId, "user@example.com", UserAuditAction.ROLES_CHANGED, UserAuditSource.MANUAL,
                List.of(new UserAuditChange("role", "ctx1", "A", "B"), new UserAuditChange("role", "ctx2", "A", "B")));

        assertNull(savedEntity().getContextKey());
    }

    @Test
    void record_noContextKeyForUserLevelChange() {
        userAuditService.recordChanges(userId, "user@example.com", UserAuditAction.USER_DISABLED, UserAuditSource.MANUAL, List.of(new UserAuditChange("enabled", null, true, false)));

        assertNull(savedEntity().getContextKey());
    }

    @Test
    void record_emptyChanges_isSkipped() {
        userAuditService.recordChanges(userId, "user@example.com", UserAuditAction.ROLES_CHANGED, UserAuditSource.MANUAL, List.of());

        verify(userAuditLogRepository, never()).save(any());
    }

    @Test
    void recordCreated_storesFullStateAsAfterValues() {
        when(snapshotReader.read(userId)).thenReturn(snapshot(Map.of(key("enabled", null), true)));

        userAuditService.recordCreated(userId, UserAuditSource.MANUAL);

        UserAuditLogEntity entity = savedEntity();
        assertEquals(UserAuditAction.USER_CREATED, entity.getAction());
        assertEquals(List.of(new UserAuditChange("enabled", null, null, true)), entity.getChanges());
    }
}
