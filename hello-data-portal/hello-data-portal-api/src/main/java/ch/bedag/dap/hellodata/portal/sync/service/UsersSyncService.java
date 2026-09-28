package ch.bedag.dap.hellodata.portal.sync.service;

import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncLockEntity;
import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus;
import ch.bedag.dap.hellodata.portal.sync.repository.UserSyncLockRepository;
import ch.bedag.dap.hellodata.portal.user.event.SyncAllUsersEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ApplicationEventPublisher eventPublisher;
    private final UserSyncLockRepository userSyncLockRepository;

    @Transactional
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    @SchedulerLock(name = "resetUsersSyncStatusIfOld", lockAtMostFor = "PT5M")
    public void resetStatusIfOld() {
        UserSyncLockEntity userSyncLockEntity = getUserSyncLockEntity();
        if (userSyncLockEntity.getStatus() == UserSyncStatus.STARTED &&
                userSyncLockEntity.getModifiedDate().isBefore(LocalDateTime.now().minusMinutes(15))) {
            userSyncLockEntity.setStatus(UserSyncStatus.COMPLETED);
            userSyncLockRepository.save(userSyncLockEntity);
        }
    }

    /**
     * Only publishes the event, the synchronization itself runs asynchronously after the commit, so a short lock is enough
     */
    @Transactional
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "synchronizeUsers", lockAtMostFor = "PT5M")
    public void synchronizeUsers() {
        UserSyncLockEntity userSyncLockEntity = getUserSyncLockEntity();
        if (userSyncLockEntity.getStatus() == UserSyncStatus.STARTED) {
            LocalDateTime startTime = LocalDateTime.now();
            try {
                log.info("[syncAllUsers] Synchronize users started");
                eventPublisher.publishEvent(new SyncAllUsersEvent());
            } finally {
                userSyncLockEntity.setStatus(UserSyncStatus.COMPLETED);
                userSyncLockRepository.save(userSyncLockEntity);
                Duration between = Duration.between(startTime, LocalDateTime.now());
                log.info("[syncAllUsers] Synchronize users completed. It took {}", DurationFormatUtils.formatDurationHMS(between.toMillis()));
            }
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
