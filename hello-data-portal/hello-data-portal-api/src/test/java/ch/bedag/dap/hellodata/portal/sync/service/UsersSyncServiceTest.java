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
import ch.bedag.dap.hellodata.portal.sync.repository.UserSyncLockRepository;
import ch.bedag.dap.hellodata.portal.user.event.SyncAllUsersEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.COMPLETED;
import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.RERUN_REQUESTED;
import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.RUNNING;
import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.STARTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsersSyncServiceTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final UserSyncLockRepository repository = mock(UserSyncLockRepository.class);
    private final UsersSyncService service = new UsersSyncService(eventPublisher, repository);

    @Test
    void startsRequestedSynchronization() {
        when(repository.changeStatus(eq(STARTED), eq(RUNNING), any())).thenReturn(1);

        service.synchronizeUsers();

        verify(eventPublisher).publishEvent(any(SyncAllUsersEvent.class));
    }

    @Test
    void doesNotStartWhenNothingRequestedOrAlreadyRunning() {
        when(repository.changeStatus(eq(STARTED), eq(RUNNING), any())).thenReturn(0);

        service.synchronizeUsers();

        verify(eventPublisher, never()).publishEvent(any(SyncAllUsersEvent.class));
    }

    @Test
    void requestWhileRunningAsksForRerun() {
        when(repository.changeStatus(eq(COMPLETED), eq(STARTED), any())).thenReturn(0);
        when(repository.findAll()).thenReturn(List.of(new UserSyncLockEntity()));

        service.startSynchronization();

        verify(repository).changeStatus(eq(RUNNING), eq(RERUN_REQUESTED), any());
    }

    @Test
    void requestWhenIdleDoesNotTouchRunningStatus() {
        when(repository.changeStatus(eq(COMPLETED), eq(STARTED), any())).thenReturn(1);
        when(repository.findAll()).thenReturn(List.of(new UserSyncLockEntity()));

        service.startSynchronization();

        verify(repository, never()).changeStatus(eq(RUNNING), eq(RERUN_REQUESTED), any());
    }

    @Test
    void finishCompletesOrQueuesRerun() {
        when(repository.changeStatus(eq(RUNNING), eq(COMPLETED), any())).thenReturn(0);

        service.finishSynchronization();

        verify(repository).changeStatus(eq(RERUN_REQUESTED), eq(STARTED), any());
    }

    @Test
    void finishWithoutRerunOnlyCompletes() {
        when(repository.changeStatus(eq(RUNNING), eq(COMPLETED), any())).thenReturn(1);

        service.finishSynchronization();

        verify(repository, never()).changeStatus(eq(RERUN_REQUESTED), eq(STARTED), any());
    }
}
