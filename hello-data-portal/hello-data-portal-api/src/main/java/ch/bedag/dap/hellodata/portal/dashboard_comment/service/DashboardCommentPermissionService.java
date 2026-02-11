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

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentPermissionDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentPermissionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentPermissionRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardCommentPermissionService {

    private final DashboardCommentPermissionRepository repository;
    private final HdContextRepository hdContextRepository;

    @Transactional
    public void updatePermissions(UUID userId, List<DashboardCommentPermissionDto> permissions) {
        for (DashboardCommentPermissionDto dto : permissions) {
            normalizePermissions(dto);
            Optional<DashboardCommentPermissionEntity> existing = repository.findByUserIdAndContextKey(userId, dto.getContextKey());
            if (existing.isPresent()) {
                DashboardCommentPermissionEntity entity = existing.get();
                entity.setReadComments(dto.isReadComments());
                entity.setWriteComments(dto.isWriteComments());
                entity.setReviewComments(dto.isReviewComments());
                repository.save(entity);
            } else {
                DashboardCommentPermissionEntity entity = new DashboardCommentPermissionEntity();
                entity.setId(UUID.randomUUID());
                entity.setUserId(userId);
                entity.setContextKey(dto.getContextKey());
                entity.setReadComments(dto.isReadComments());
                entity.setWriteComments(dto.isWriteComments());
                entity.setReviewComments(dto.isReviewComments());
                repository.save(entity);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<DashboardCommentPermissionDto> getPermissions(UUID userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Synchronizes default comment permissions for a user across all data domains.
     * Portal admins (HELLODATA_ADMIN, BUSINESS_DOMAIN_ADMIN) get full access (READ+WRITE+REVIEW) in all domains.
     * Data Domain Admins (DATA_DOMAIN_ADMIN) get full access in their specific domain.
     * Other users with any non-NONE role in a domain get read-only access (READ) in that domain.
     * Users with NONE role get no access.
     * Only creates permissions for contexts where they don't already exist.
     *
     * @param user the user entity to sync permissions for
     */
    @Transactional
    public void syncDefaultPermissionsForUser(UserEntity user) {
        // Check if user has portal-wide admin roles
        boolean isPortalAdmin = user.isHelloDataAdmin() || user.isBusinessDomainAdmin();

        // Get all data domain contexts
        List<HdContextEntity> contexts = hdContextRepository.findAll().stream()
                .filter(ctx -> HdContextType.DATA_DOMAIN.equals(ctx.getType()))
                .toList();

        int createdCount = 0;
        for (HdContextEntity context : contexts) {
            // Check if permission already exists for this user and context
            Optional<DashboardCommentPermissionEntity> existing =
                    repository.findByUserIdAndContextKey(user.getId(), context.getContextKey());

            if (existing.isEmpty()) {
                // Create new permission
                DashboardCommentPermissionEntity permission = new DashboardCommentPermissionEntity();
                permission.setId(UUID.randomUUID());
                permission.setUserId(user.getId());
                permission.setContextKey(context.getContextKey());

                if (isPortalAdmin) {
                    // Portal admins get full access to all contexts
                    permission.setReadComments(true);
                    permission.setWriteComments(true);
                    permission.setReviewComments(true);
                } else {
                    // Check user's role in this specific context
                    HdRoleName contextRoleName = user.getContextRoles().stream()
                            .filter(cr -> cr.getContextKey().equals(context.getContextKey()))
                            .map(cr -> cr.getRole().getName())
                            .findFirst()
                            .orElse(HdRoleName.NONE);

                    if (HdRoleName.DATA_DOMAIN_ADMIN.equals(contextRoleName)) {
                        // Data Domain Admin gets full access to their context
                        permission.setReadComments(true);
                        permission.setWriteComments(true);
                        permission.setReviewComments(true);
                    } else if (!HdRoleName.NONE.equals(contextRoleName)) {
                        // Other non-NONE roles get read-only access
                        permission.setReadComments(true);
                        permission.setWriteComments(false);
                        permission.setReviewComments(false);
                    } else {
                        // NONE role gets no access
                        permission.setReadComments(false);
                        permission.setWriteComments(false);
                        permission.setReviewComments(false);
                    }
                }

                repository.save(permission);
                createdCount++;
            }
        }

        if (createdCount > 0) {
            log.info("Created {} default comment permissions for user {} (portal admin: {})",
                    createdCount, user.getEmail(), isPortalAdmin);
        }
    }

    private void normalizePermissions(DashboardCommentPermissionDto dto) {
        if (dto.isReviewComments()) {
            dto.setWriteComments(true);
            dto.setReadComments(true);
        }
        if (dto.isWriteComments()) {
            dto.setReadComments(true);
        }
        if (!dto.isReadComments()) {
            dto.setWriteComments(false);
            dto.setReviewComments(false);
        }
    }

    private DashboardCommentPermissionDto toDto(DashboardCommentPermissionEntity entity) {
        DashboardCommentPermissionDto dto = new DashboardCommentPermissionDto();
        dto.setContextKey(entity.getContextKey());
        dto.setReadComments(entity.isReadComments());
        dto.setWriteComments(entity.isWriteComments());
        dto.setReviewComments(entity.isReviewComments());
        return dto;
    }
}
