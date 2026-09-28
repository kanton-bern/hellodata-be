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
import ch.bedag.dap.hellodata.portalcommon.userscache.data.DashboardUsersResultDto;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.SubsystemUsersResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.SimpleKey;

import java.util.List;

import static ch.bedag.dap.hellodata.portalcommon.userscache.UsersCacheNames.SUBSYSTEM_USERS_CACHE;
import static ch.bedag.dap.hellodata.portalcommon.userscache.UsersCacheNames.USERS_WITH_DASHBOARD_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class UsersCacheRefreshServiceTest {

    private final List<SubsystemUsersResultDto> subsystemUsers = List.of(new SubsystemUsersResultDto("airflow", List.of()));
    private final List<DashboardUsersResultDto> dashboardUsers = List.of(new DashboardUsersResultDto("Data Domain", "superset", List.of()));

    private UsersCacheBuilder usersCacheBuilder;
    private CacheManager cacheManager;
    private UsersCacheRefreshService usersCacheRefreshService;

    @BeforeEach
    void setUp() {
        usersCacheBuilder = mock(UsersCacheBuilder.class);
        when(usersCacheBuilder.buildSubsystemUsers()).thenReturn(subsystemUsers);
        when(usersCacheBuilder.buildDashboardUsers()).thenReturn(dashboardUsers);
        cacheManager = new ConcurrentMapCacheManager(SUBSYSTEM_USERS_CACHE, USERS_WITH_DASHBOARD_CACHE);
        usersCacheRefreshService = new UsersCacheRefreshService(usersCacheBuilder, cacheManager);
    }

    @Test
    void refresh_supersetUsers_refreshesBothCaches() {
        usersCacheRefreshService.refresh(ModuleType.SUPERSET);

        assertEquals(subsystemUsers, cacheManager.getCache(SUBSYSTEM_USERS_CACHE).get(SimpleKey.EMPTY).get());
        assertEquals(dashboardUsers, cacheManager.getCache(USERS_WITH_DASHBOARD_CACHE).get(SimpleKey.EMPTY).get());
    }

    @Test
    void refresh_otherUsers_refreshesSubsystemUsersCacheOnly() {
        usersCacheRefreshService.refresh(ModuleType.AIRFLOW);

        assertEquals(subsystemUsers, cacheManager.getCache(SUBSYSTEM_USERS_CACHE).get(SimpleKey.EMPTY).get());
        assertNull(cacheManager.getCache(USERS_WITH_DASHBOARD_CACHE).get(SimpleKey.EMPTY));
        verify(usersCacheBuilder, never()).buildDashboardUsers();
    }

    @Test
    void refresh_afterFailure_acceptsNextRequest() {
        when(usersCacheBuilder.buildSubsystemUsers()).thenThrow(new IllegalStateException("db down")).thenReturn(subsystemUsers);

        try {
            usersCacheRefreshService.refresh(ModuleType.AIRFLOW);
        } catch (IllegalStateException ignored) {
            // expected
        }
        usersCacheRefreshService.refresh(ModuleType.AIRFLOW);

        assertEquals(subsystemUsers, cacheManager.getCache(SUBSYSTEM_USERS_CACHE).get(SimpleKey.EMPTY).get());
    }
}
