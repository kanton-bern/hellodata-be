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
package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.portal.user.entity.UserSelectedDashboardEntity;
import ch.bedag.dap.hellodata.portal.user.repository.UserSelectedDashboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserSelectedDashboardService {

    private final UserSelectedDashboardRepository repository;

    public record DashboardSelection(int dashboardId, String dashboardTitle, String instanceName) {}

    @Transactional(readOnly = true)
    public Set<Integer> getSelectedDashboardIds(UUID userId, String contextKey) {
        return repository.findAllByUserIdAndContextKey(userId, contextKey)
                .stream()
                .map(UserSelectedDashboardEntity::getDashboardId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<UserSelectedDashboardEntity> getSelectedDashboards(UUID userId, String contextKey) {
        return repository.findAllByUserIdAndContextKey(userId, contextKey);
    }

    @Transactional
    public void saveSelectedDashboards(UUID userId, String contextKey, List<DashboardSelection> selections) {
        repository.deleteAllByUserIdAndContextKey(userId, contextKey);
        for (DashboardSelection selection : selections) {
            UserSelectedDashboardEntity entity = new UserSelectedDashboardEntity();
            entity.setUserId(userId);
            entity.setDashboardId(selection.dashboardId());
            entity.setDashboardTitle(selection.dashboardTitle());
            entity.setContextKey(contextKey);
            entity.setInstanceName(selection.instanceName());
            repository.save(entity);
        }
        log.info("Saved {} selected dashboards for user {} in context {}", selections.size(), userId, contextKey);
    }

    @Transactional
    public void removeAllForUser(UUID userId) {
        repository.deleteAllByUserId(userId);
        log.info("Removed all selected dashboards for user {}", userId);
    }

    @Transactional
    public void removeAllForUserInContext(UUID userId, String contextKey) {
        repository.deleteAllByUserIdAndContextKey(userId, contextKey);
        log.info("Removed selected dashboards for user {} in context {}", userId, contextKey);
    }

    @Transactional
    public void cleanupStaleDashboards(UUID userId, String contextKey, Set<Integer> validDashboardIds) {
        List<UserSelectedDashboardEntity> existing = repository.findAllByUserIdAndContextKey(userId, contextKey);
        List<UserSelectedDashboardEntity> stale = existing.stream()
                .filter(e -> !validDashboardIds.contains(e.getDashboardId()))
                .toList();
        if (!stale.isEmpty()) {
            repository.deleteAll(stale);
            log.info("Cleaned up {} stale dashboard selections for user {} in context {}", stale.size(), userId, contextKey);
        }
    }

    @Transactional(readOnly = true)
    public boolean isEmpty() {
        return repository.count() == 0;
    }
}
