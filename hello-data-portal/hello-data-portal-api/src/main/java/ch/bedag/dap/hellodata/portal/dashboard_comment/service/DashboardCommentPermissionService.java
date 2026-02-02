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

import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentPermissionDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentPermissionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardCommentPermissionService {

    private final DashboardCommentPermissionRepository repository;

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
