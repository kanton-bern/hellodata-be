package ch.bedag.dap.hellodata.portal.cache.service;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.portal.lock.service.AdvisoryLockService;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoUsersService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_METAINFO_USERS_CACHE;

@Log4j2
@Service
@AllArgsConstructor
public class CacheUpdateService {

    private static final long LOCK_ID = 6432543124L;

    private final MetaInfoUsersService metaInfoUsersService;
    private final AdvisoryLockService advisoryLockService;

    @PostConstruct
    public void releaseStaleLocksOnStartup() {
        advisoryLockService.releaseStaleLock(LOCK_ID);
        log.info("[CACHE] Released stale advisory lock at startup.");
    }

    @SneakyThrows
    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = UPDATE_METAINFO_USERS_CACHE)
    public void updateMetainfoUsersCache(UserResource userResource) {
        if (Boolean.TRUE.equals(advisoryLockService.acquireLock(LOCK_ID))) {
            try {
                updateUsersWithRolesCache();
                if (userResource.getModuleType() == ModuleType.SUPERSET) {
                    updateUsersWithDashboardsCache();
                }
            } finally {
                advisoryLockService.releaseStaleLock(LOCK_ID);
            }
        } else {
            log.info("[CACHE] Another instance is already synchronizing metainfo users cache.");
        }
    }

    private void updateUsersWithDashboardsCache() {
        log.info("[CACHE] Updating subsystem users with dashboard permissions cache");
        LocalDateTime startTime = LocalDateTime.now();
        metaInfoUsersService.getAllUsersWithRolesForDashboardsRefreshCache();
        Duration between = Duration.between(startTime, LocalDateTime.now());
        log.info("[CACHE] Updating subsystem users with dashboard permissions completed. It took {}", DurationFormatUtils.formatDurationHMS(between.toMillis()));
    }

    private void updateUsersWithRolesCache() {
        log.info("[CACHE] Updating subsystem users aggregation cache");
        LocalDateTime startTime = LocalDateTime.now();
        metaInfoUsersService.getAllUsersWithRolesRefreshCache();
        Duration between = Duration.between(startTime, LocalDateTime.now());
        log.info("[CACHE] Updating subsystem users aggregation completed. It took {}", DurationFormatUtils.formatDurationHMS(between.toMillis()));
    }
}
