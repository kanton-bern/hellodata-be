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
package ch.bedag.dap.hellodata.portal.initialize.service;

import ch.bedag.dap.hellodata.portal.dashboard_comment.config.DashboardCommentSyncProperties;
import ch.bedag.dap.hellodata.portal.dashboard_comment.service.DashboardCommentDwhSyncService;
import ch.bedag.dap.hellodata.portal.initialize.event.InitializationCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * One-time backfill of all published dashboard comments to the datadomain DWH via NATS.
 * Listens to {@link InitializationCompletedEvent} and uses {@link MigrationService}
 * to ensure the backfill runs exactly once.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class DashboardCommentSyncInitializer implements ApplicationListener<InitializationCompletedEvent> {

    private static final String MIGRATION_KEY = "sync_comments_to_dwh_v1";

    private final DashboardCommentSyncProperties syncProperties;
    private final MigrationService migrationService;
    private final DashboardCommentDwhSyncService dwhSyncService;

    @Override
    public void onApplicationEvent(InitializationCompletedEvent event) {
        if (!syncProperties.isDwhSyncEnabled()) {
            log.info("Dashboard comments DWH sync is disabled, skipping initial backfill");
            return;
        }
        if (migrationService.isMigrationCompleted(MIGRATION_KEY)) {
            log.info("Dashboard comments DWH initial backfill already completed, skipping");
            return;
        }
        try {
            log.info("Starting initial backfill of dashboard comments to DWH");
            dwhSyncService.publishAllComments();
            migrationService.recordMigrationSuccess(MIGRATION_KEY, "Initial sync of all published comments to DWH via NATS");
        } catch (Exception e) {
            log.error("Failed initial backfill of dashboard comments to DWH", e);
            migrationService.recordMigrationFailure(MIGRATION_KEY, "Initial comments sync", e.getMessage());
        }
    }
}
