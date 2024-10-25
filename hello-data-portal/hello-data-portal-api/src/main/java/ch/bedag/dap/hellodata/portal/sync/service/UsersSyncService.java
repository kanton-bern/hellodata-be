package ch.bedag.dap.hellodata.portal.sync.service;

import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncLockEntity;
import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus;
import ch.bedag.dap.hellodata.portal.sync.repository.UserSyncLockRepository;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final long LOCK_ID = 5432543124L; // Unique lock ID for synchronization
    private static final String ADVISORY_UNLOCK_QUERY = "SELECT pg_advisory_unlock(" + LOCK_ID + ")";
    private static final String ADVISORY_LOCK_QUERY = "SELECT pg_try_advisory_lock(?)";
    private final UserService userService;
    private final UserSyncLockRepository userSyncLockRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void releaseStaleLocksOnStartup() {
        releaseStaleLock();
        log.info("[syncAllUsers] Released stale advisory lock at startup.");
    }

    @Transactional
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void resetStatusIfOld() {
        UserSyncLockEntity userSyncLockEntity = getUserSyncLockEntity();
        if (userSyncLockEntity.getStatus() == UserSyncStatus.STARTED &&
                userSyncLockEntity.getModifiedDate().isBefore(LocalDateTime.now().minusMinutes(30))) {
            userSyncLockEntity.setStatus(UserSyncStatus.COMPLETED);
            userSyncLockRepository.save(userSyncLockEntity);
            releaseStaleLock();
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void synchronizeUsers() {
        if (Boolean.TRUE.equals(isStaleLockAquired())) {
            UserSyncLockEntity userSyncLockEntity = getUserSyncLockEntity();
            if (userSyncLockEntity.getStatus() == UserSyncStatus.STARTED) {
                LocalDateTime startTime = LocalDateTime.now();
                try {
                    log.info("[syncAllUsers] Synchronize users started");
                    userService.syncAllUsers();
                } finally {
                    userSyncLockEntity.setStatus(UserSyncStatus.COMPLETED);
                    userSyncLockRepository.save(userSyncLockEntity);
                    releaseStaleLock();
                    Duration between = Duration.between(startTime, LocalDateTime.now());
                    log.info("[syncAllUsers] Synchronize users completed. It took {}", DurationFormatUtils.formatDurationHMS(between.toMillis()));
                }
            }
        } else {
            log.info("[syncAllUsers] Another instance is already synchronizing users.");
        }
    }

    @Transactional
    public void startSynchronization() {
        UserSyncLockEntity userSyncLockEntity = getUserSyncLockEntity();
        if (userSyncLockEntity.getStatus() == UserSyncStatus.COMPLETED) {
            userSyncLockEntity.setStatus(UserSyncStatus.STARTED);
            userSyncLockRepository.save(userSyncLockEntity);
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

    private Boolean isStaleLockAquired() {
        Boolean lockAcquired = jdbcTemplate.queryForObject(ADVISORY_LOCK_QUERY, Boolean.class, LOCK_ID);
        return lockAcquired;
    }

    private void releaseStaleLock() {
        jdbcTemplate.execute(ADVISORY_UNLOCK_QUERY);
    }
}
