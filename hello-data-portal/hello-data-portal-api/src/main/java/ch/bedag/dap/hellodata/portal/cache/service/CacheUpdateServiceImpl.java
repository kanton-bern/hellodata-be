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
import java.util.concurrent.CompletableFuture;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_METAINFO_USERS_CACHE;

@Log4j2
@Service
@AllArgsConstructor
public class CacheUpdateServiceImpl {

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
    public CompletableFuture<Void> updateCache(UserResource userResource) {
        if (Boolean.TRUE.equals(advisoryLockService.acquireLock(LOCK_ID))) {
            updateUsersWithRolesCache();
            if (userResource.getModuleType() == ModuleType.SUPERSET) {
                updateUsersWithDashboardsCache();
            }
        } else {
            log.info("[CACHE] Another instance is already synchronizing cache.");
        }
        return null;
    }

    private void updateUsersWithDashboardsCache() {
        log.info("[CACHE] Updating metainfo users with dashboard permissions cache");
        LocalDateTime startTime = LocalDateTime.now();
        metaInfoUsersService.getAllUsersWithRolesForDashboardsInternal();
        Duration between = Duration.between(startTime, LocalDateTime.now());
        log.info("[CACHE] Updating metainfo users with dashboard permissions completed. It took {}", DurationFormatUtils.formatDurationHMS(between.toMillis()));
    }

    private void updateUsersWithRolesCache() {
        log.info("[CACHE] Updating metainfo users aggregation cache");
        LocalDateTime startTime = LocalDateTime.now();
        metaInfoUsersService.getAllUsersWithRolesInternal();
        Duration between = Duration.between(startTime, LocalDateTime.now());
        log.info("[CACHE] Updating metainfo users aggregation completed. It took {}", DurationFormatUtils.formatDurationHMS(between.toMillis()));
    }
}
