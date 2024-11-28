package ch.bedag.dap.hellodata.portal.sync.service;

import ch.bedag.dap.hellodata.portal.lock.service.AdvisoryLockService;
import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncLockEntity;
import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus;
import ch.bedag.dap.hellodata.portal.sync.repository.UserSyncLockRepository;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
@RequiredArgsConstructor
public class UsersSyncService {

    private static final long LOCK_ID = 5432543124L;
    private final UserService userService;
    private final UserSyncLockRepository userSyncLockRepository;
    private final AdvisoryLockService advisoryLockService;

    @PostConstruct
    public void releaseStaleLocksOnStartup() {
        advisoryLockService.releaseStaleLock(LOCK_ID);
        log.info("[syncAllUsers] Released stale advisory lock at startup.");
    }

    @Transactional
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    public void resetStatusIfOld() {
        UserSyncLockEntity userSyncLockEntity = getUserSyncLockEntity();
        if (userSyncLockEntity.getStatus() == UserSyncStatus.STARTED &&
                userSyncLockEntity.getModifiedDate().isBefore(LocalDateTime.now().minusMinutes(15))) {
            userSyncLockEntity.setStatus(UserSyncStatus.COMPLETED);
            userSyncLockRepository.save(userSyncLockEntity);
            advisoryLockService.releaseStaleLock(LOCK_ID);
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void synchronizeUsers() {
        if (Boolean.TRUE.equals(advisoryLockService.acquireLock(LOCK_ID))) {
            UserSyncLockEntity userSyncLockEntity = getUserSyncLockEntity();
            if (userSyncLockEntity.getStatus() == UserSyncStatus.STARTED) {
                LocalDateTime startTime = LocalDateTime.now();
                try {
                    log.info("[syncAllUsers] Synchronize users started");
                    userService.syncAllUsers();
                } finally {
                    userSyncLockEntity.setStatus(UserSyncStatus.COMPLETED);
                    userSyncLockRepository.save(userSyncLockEntity);
                    advisoryLockService.releaseStaleLock(LOCK_ID);
                    Duration between = Duration.between(startTime, LocalDateTime.now());
                    log.info("[syncAllUsers] Synchronize users completed. It took {}", DurationFormatUtils.formatDurationHMS(between.toMillis()));
                }
            }
        } else {
            log.info("[syncAllUsers] Another instance is already synchronizing users.");
        }
    }

    @Transactional
    public UserSyncStatus startSynchronization() {
        UserSyncLockEntity userSyncLockEntity = getUserSyncLockEntity();
        if (userSyncLockEntity.getStatus() == UserSyncStatus.COMPLETED) {
            userSyncLockEntity.setStatus(UserSyncStatus.STARTED);
            userSyncLockRepository.save(userSyncLockEntity);
        }
        return userSyncLockEntity.getStatus();
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
