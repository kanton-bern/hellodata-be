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
package ch.bedag.dap.hellodata.portal.sync.service;

import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncLockEntity;
import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus;
import ch.bedag.dap.hellodata.portal.sync.repository.UserSyncLockRepository;
import ch.bedag.dap.hellodata.portal.user.event.SyncAllUsersEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The cluster wide locking of the scheduled run is done by ShedLock (@SchedulerLock), so only the synchronization logic is tested here
 */
class UsersSyncServiceTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final UserSyncLockRepository userSyncLockRepository = mock(UserSyncLockRepository.class);
    private final UsersSyncService service = new UsersSyncService(eventPublisher, userSyncLockRepository);

    private UserSyncLockEntity lockWithStatus(UserSyncStatus status) {
        UserSyncLockEntity lock = new UserSyncLockEntity();
        lock.setStatus(status);
        when(userSyncLockRepository.findAll()).thenReturn(List.of(lock));
        return lock;
    }

    @Test
    void doesNothingWhenNoSynchronizationWasRequested() {
        UserSyncLockEntity lock = lockWithStatus(UserSyncStatus.COMPLETED);

        service.synchronizeUsers();

        verify(eventPublisher, never()).publishEvent(any());
        assertThat(lock.getStatus()).isEqualTo(UserSyncStatus.COMPLETED);
    }

    @Test
    void synchronizesAndCompletesWhenRequested() {
        UserSyncLockEntity lock = lockWithStatus(UserSyncStatus.STARTED);

        service.synchronizeUsers();

        verify(eventPublisher).publishEvent(any(SyncAllUsersEvent.class));
        assertThat(lock.getStatus()).isEqualTo(UserSyncStatus.COMPLETED);
        verify(userSyncLockRepository).save(lock);
    }
}
