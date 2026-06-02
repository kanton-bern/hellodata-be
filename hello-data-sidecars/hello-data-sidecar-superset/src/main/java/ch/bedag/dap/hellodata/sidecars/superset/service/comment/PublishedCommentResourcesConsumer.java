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
package ch.bedag.dap.hellodata.sidecars.superset.service.comment;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.data.DashboardCommentsPublished;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_DASHBOARD_COMMENTS;

/**
 * Consumes published dashboard comments from NATS and writes them to the datadomain DWH.
 * Only processes messages matching this sidecar's contextKey (data domain).
 */
@Log4j2
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hello-data.dashboard-comments.dwh-sync-enabled", havingValue = "true")
public class PublishedCommentResourcesConsumer {

    private final HelloDataContextConfig helloDataContextConfig;
    private final DwhCommentRepository dwhCommentRepository;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = PUBLISH_DASHBOARD_COMMENTS)
    public void subscribe(DashboardCommentsPublished payload) {
        String myContextKey = helloDataContextConfig.getContext().getKey();

        if (!myContextKey.equalsIgnoreCase(payload.getContextKey())) {
            log.debug("Ignoring dashboard comments for context {} (my context: {})", payload.getContextKey(), myContextKey);
            return;
        }

        log.info("Received {} published comments for dashboard {}/{}", 
                payload.getComments() != null ? payload.getComments().size() : 0,
                payload.getContextKey(), payload.getDashboardId());

        try {
            dwhCommentRepository.replaceCommentsForDashboard(payload);
            log.info("Successfully synced comments for dashboard {}/{} to DWH", payload.getContextKey(), payload.getDashboardId());
        } catch (Exception e) {
            log.error("Failed to sync comments for dashboard {}/{} to DWH: {}", 
                    payload.getContextKey(), payload.getDashboardId(), e.getMessage(), e);
        }
    }
}
