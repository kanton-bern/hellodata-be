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
package ch.bedag.dap.hellodata.portal.dashboard_comment.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.data.DashboardCommentsPublished;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.data.PublishedComment;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.portal.dashboard_comment.config.DashboardCommentSyncProperties;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentStatus;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentVersionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for publishing dashboard comments to the datadomain DWH via NATS.
 * Publishes a full-replace batch for a (contextKey, dashboardId) pair for idempotent sync.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardCommentDwhSyncService {

    private final DashboardCommentSyncProperties syncProperties;
    private final DashboardCommentRepository commentRepository;
    private final NatsSenderService natsSenderService;
    private final MetaInfoResourceService metaInfoResourceService;

    /**
     * Publishes all currently published (non-deleted) comments for a specific dashboard to NATS.
     * The consumer will do a full-replace in the DWH.
     */
    public void publishCommentsForDashboard(String contextKey, int dashboardId) {
        if (!syncProperties.isDwhSyncEnabled()) {
            return;
        }
        try {
            List<DashboardCommentEntity> comments = commentRepository.findByContextKeyAndDashboardIdOrderByCreatedDateAsc(contextKey, dashboardId);
            Map<Integer, DashboardInfo> infoMap = getDashboardInfoMap(contextKey);

            List<PublishedComment> publishedComments = comments.stream()
                    .filter(c -> !c.isDeleted())
                    .map(c -> toPublishedComment(c, infoMap))
                    .filter(Objects::nonNull)
                    .toList();

            DashboardCommentsPublished payload = DashboardCommentsPublished.builder()
                    .contextKey(contextKey)
                    .dashboardId(dashboardId)
                    .comments(publishedComments)
                    .build();

            natsSenderService.publishMessageToJetStream(HDEvent.PUBLISH_DASHBOARD_COMMENTS, payload);
            log.info("Published {} comments for dashboard {}/{} to DWH sync", publishedComments.size(), contextKey, dashboardId);
        } catch (Exception e) {
            log.error("Failed to publish comments for dashboard {}/{} to DWH sync: {}", contextKey, dashboardId, e.getMessage(), e);
        }
    }

    /**
     * Publishes all published comments for all dashboards in all contexts.
     * Used by the initializer for backfill and by the reconciliation job.
     */
    public void publishAllComments() {
        if (!syncProperties.isDwhSyncEnabled()) {
            return;
        }
        log.info("Starting full comments DWH sync");

        List<DashboardCommentEntity> allComments = commentRepository.findAll();
        Map<String, List<DashboardCommentEntity>> byContext = allComments.stream()
                .filter(c -> !c.isDeleted())
                .collect(Collectors.groupingBy(DashboardCommentEntity::getContextKey));

        int totalPublished = 0;
        for (Map.Entry<String, List<DashboardCommentEntity>> contextEntry : byContext.entrySet()) {
            String contextKey = contextEntry.getKey();
            Map<Integer, DashboardInfo> infoMap = getDashboardInfoMap(contextKey);

            Map<Integer, List<DashboardCommentEntity>> byDashboard = contextEntry.getValue().stream()
                    .collect(Collectors.groupingBy(DashboardCommentEntity::getDashboardId));

            for (Map.Entry<Integer, List<DashboardCommentEntity>> dashboardEntry : byDashboard.entrySet()) {
                int dashboardId = dashboardEntry.getKey();
                List<PublishedComment> publishedComments = dashboardEntry.getValue().stream()
                        .map(c -> toPublishedComment(c, infoMap))
                        .filter(Objects::nonNull)
                        .toList();

                DashboardCommentsPublished payload = DashboardCommentsPublished.builder()
                        .contextKey(contextKey)
                        .dashboardId(dashboardId)
                        .comments(publishedComments)
                        .build();

                try {
                    natsSenderService.publishMessageToJetStream(HDEvent.PUBLISH_DASHBOARD_COMMENTS, payload);
                    totalPublished += publishedComments.size();
                } catch (Exception e) {
                    log.error("Failed to publish comments for dashboard {}/{}: {}", contextKey, dashboardId, e.getMessage(), e);
                }
            }
        }
        log.info("Full comments DWH sync completed. Published {} comments across {} contexts", totalPublished, byContext.size());
    }

    private PublishedComment toPublishedComment(DashboardCommentEntity entity, Map<Integer, DashboardInfo> infoMap) {
        DashboardCommentVersionEntity activeVersion = entity.getHistory().stream()
                .filter(v -> v.getVersion().equals(entity.getActiveVersion()))
                .filter(v -> v.getStatus() == DashboardCommentStatus.PUBLISHED)
                .findFirst()
                .orElse(null);

        if (activeVersion == null) {
            return null;
        }

        DashboardInfo info = infoMap.get(entity.getDashboardId());

        return PublishedComment.builder()
                .commentId(entity.getId())
                .dashboardId(entity.getDashboardId())
                .dashboardTitle(info != null ? info.title() : null)
                .dashboardSlug(info != null ? info.slug() : null)
                .author(entity.getAuthor())
                .authorEmail(entity.getAuthorEmail())
                .createdDate(entity.getCreatedDate())
                .publishedDate(activeVersion.getPublishedDate())
                .text(activeVersion.getText())
                .tags(activeVersion.getTags())
                .pointerUrl(activeVersion.getPointerUrl())
                .build();
    }

    private record DashboardInfo(String title, String slug) {
    }

    private Map<Integer, DashboardInfo> getDashboardInfoMap(String contextKey) {
        Map<Integer, DashboardInfo> infoMap = new HashMap<>();
        try {
            DashboardResource dashboardResource = metaInfoResourceService.findAllByModuleTypeAndKindAndContextKey(
                    ModuleType.SUPERSET,
                    ModuleResourceKind.HELLO_DATA_DASHBOARDS,
                    contextKey,
                    DashboardResource.class
            );
            if (dashboardResource != null && dashboardResource.getData() != null) {
                for (SupersetDashboard dashboard : dashboardResource.getData()) {
                    infoMap.put(dashboard.getId(), new DashboardInfo(dashboard.getDashboardTitle(), dashboard.getSlug()));
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch dashboard info for context {}: {}", contextKey, e.getMessage());
        }
        return infoMap;
    }
}
