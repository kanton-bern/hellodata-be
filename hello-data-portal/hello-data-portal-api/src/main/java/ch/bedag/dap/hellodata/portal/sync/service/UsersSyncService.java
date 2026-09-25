package ch.bedag.dap.hellodata.portal.sync.service;

import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncLockEntity;
import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus;
import ch.bedag.dap.hellodata.portal.sync.repository.UserSyncLockRepository;
import ch.bedag.dap.hellodata.portal.user.event.SyncAllUsersEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.COMPLETED;
import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.RERUN_REQUESTED;
import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.RUNNING;
import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.STARTED;

/**
 * Makes sure only one users synchronization runs at a time across all portal instances.
 * The status moves STARTED -> RUNNING -> COMPLETED, requests coming in while running are merged into one follow-up run.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class UsersSyncService {

    private static final long STALE_RUN_MINUTES = 30;
    private final ApplicationEventPublisher eventPublisher;
    private final UserSyncLockRepository userSyncLockRepository;

    /**
     * Releases the lock when the instance running the synchronization died before finishing it
     */
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    public void resetStatusIfOld() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime olderThan = now.minusMinutes(STALE_RUN_MINUTES);
        int reset = userSyncLockRepository.changeStatusIfOlderThan(RUNNING, COMPLETED, olderThan, now)
                + userSyncLockRepository.changeStatusIfOlderThan(RERUN_REQUESTED, STARTED, olderThan, now);
        if (reset > 0) {
            log.warn("[syncAllUsers] Synchronization did not finish within {} minutes, releasing the lock", STALE_RUN_MINUTES);
        }
    }

    /**
     * Picks up a requested synchronization, the synchronization itself runs asynchronously and calls {@link #finishSynchronization()} when done
     */
    @Transactional
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void synchronizeUsers() {
        if (userSyncLockRepository.changeStatus(STARTED, RUNNING, LocalDateTime.now()) == 1) {
            log.info("[syncAllUsers] Synchronize users started");
            // delivered after this transaction commits, so the RUNNING status is visible to all instances
            eventPublisher.publishEvent(new SyncAllUsersEvent());
        } else {
            log.debug("[syncAllUsers] No synchronization requested or another one is running.");
        }
    }

    @Transactional
    public UserSyncStatus startSynchronization() {
        LocalDateTime now = LocalDateTime.now();
        if (userSyncLockRepository.changeStatus(COMPLETED, STARTED, now) == 0) {
            userSyncLockRepository.changeStatus(RUNNING, RERUN_REQUESTED, now);
        }
        return getUserSyncLockEntity().getStatus();
    }

    @Transactional
    public void finishSynchronization() {
        LocalDateTime now = LocalDateTime.now();
        if (userSyncLockRepository.changeStatus(RUNNING, COMPLETED, now) == 0
                && userSyncLockRepository.changeStatus(RERUN_REQUESTED, STARTED, now) == 1) {
            log.info("[syncAllUsers] Another synchronization was requested while running, it starts shortly");
        }
    }

    @Transactional(readOnly = true)
    public UserSyncStatus getSyncStatus() {
        UserSyncLockEntity userSyncLockEntity = getUserSyncLockEntity();
        return userSyncLockEntity.getStatus();
    }

    private UserSyncLockEntity getUserSyncLockEntity() {
        List<UserSyncLockEntity> all = userSyncLockRepository.findAll();
        if (all.size() > 1) {
            log.warn("[syncAllUsers] Too many synchronization locks: {}", all.size());
        }
        return all.get(0);
    }

}
