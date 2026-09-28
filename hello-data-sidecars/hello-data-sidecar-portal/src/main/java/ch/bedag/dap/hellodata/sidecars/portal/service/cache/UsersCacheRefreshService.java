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
package ch.bedag.dap.hellodata.sidecars.portal.service.cache;

import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.portalcommon.userscache.UsersCacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static ch.bedag.dap.hellodata.portalcommon.userscache.UsersCacheNames.SUBSYSTEM_USERS_CACHE;
import static ch.bedag.dap.hellodata.portalcommon.userscache.UsersCacheNames.USERS_WITH_DASHBOARD_CACHE;

/**
 * Refreshes the users caches read by the portal api. Requests arriving while a refresh is running are merged into one more refresh afterwards,
 * so the caches always end up built from the latest published resources.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class UsersCacheRefreshService {

    private final UsersCacheBuilder usersCacheBuilder;
    private final CacheManager cacheManager;

    private boolean running;
    private boolean subsystemUsersPending;
    private boolean dashboardUsersPending;

    public void refresh(ModuleType moduleType) {
        synchronized (this) {
            subsystemUsersPending = true;
            dashboardUsersPending |= moduleType == ModuleType.SUPERSET;
            if (running) {
                log.debug("[CACHE] Refresh already running, the users caches will be refreshed once more afterwards");
                return;
            }
            running = true;
        }
        try {
            refreshWhilePending();
        } catch (RuntimeException e) {
            synchronized (this) {
                running = false;
            }
            throw e;
        }
    }

    private void refreshWhilePending() {
        while (true) {
            boolean refreshSubsystemUsers;
            boolean refreshDashboardUsers;
            synchronized (this) {
                if (!subsystemUsersPending && !dashboardUsersPending) {
                    // cleared in the same lock as the pending check, so a request arriving now starts a new refresh
                    running = false;
                    return;
                }
                refreshSubsystemUsers = subsystemUsersPending;
                refreshDashboardUsers = dashboardUsersPending;
                subsystemUsersPending = false;
                dashboardUsersPending = false;
            }
            if (refreshSubsystemUsers) {
                put(SUBSYSTEM_USERS_CACHE, usersCacheBuilder::buildSubsystemUsers);
            }
            if (refreshDashboardUsers) {
                put(USERS_WITH_DASHBOARD_CACHE, usersCacheBuilder::buildDashboardUsers);
            }
        }
    }

    private void put(String cacheName, Supplier<List<?>> builder) {
        log.debug("[CACHE] Updating {} cache", cacheName);
        LocalDateTime startTime = LocalDateTime.now();
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("[CACHE] Cache {} not found", cacheName);
            return;
        }
        // same key as @Cacheable on a method without parameters in the portal api
        cache.put(SimpleKey.EMPTY, builder.get());
        Duration between = Duration.between(startTime, LocalDateTime.now());
        log.debug("[CACHE] Updating {} cache completed. It took {}", cacheName, DurationFormatUtils.formatDurationHMS(between.toMillis()));
    }
}
