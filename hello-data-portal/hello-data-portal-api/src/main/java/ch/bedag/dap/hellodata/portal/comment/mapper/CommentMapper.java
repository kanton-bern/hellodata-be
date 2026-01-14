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
package ch.bedag.dap.hellodata.portal.comment.mapper;

import ch.bedag.dap.hellodata.portal.comment.data.CommentDto;
import ch.bedag.dap.hellodata.portal.comment.data.CommentVersionDto;
import ch.bedag.dap.hellodata.portal.comment.entity.CommentEntity;
import ch.bedag.dap.hellodata.portal.comment.entity.CommentVersionEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CommentMapper {

    public CommentDto toDto(CommentEntity entity) {
        if (entity == null) {
            return null;
        }

        return CommentDto.builder()
                .id(entity.getId())
                .dashboardId(entity.getDashboardId())
                .dashboardUrl(entity.getDashboardUrl())
                .contextKey(entity.getContextKey())
                .pointerUrl(entity.getPointerUrl())
                .author(entity.getAuthor())
                .authorEmail(entity.getAuthorEmail())
                .createdDate(entity.getCreatedDate())
                .deleted(entity.isDeleted())
                .deletedDate(entity.getDeletedDate())
                .deletedBy(entity.getDeletedBy())
                .activeVersion(entity.getActiveVersion())
                .hasActiveDraft(entity.isHasActiveDraft())
                .entityVersion(entity.getEntityVersion())
                .history(entity.getHistory().stream()
                        .map(this::toVersionDto)
                        .collect(Collectors.toList()))
                .build();
    }

    public CommentEntity toEntity(CommentDto dto) {
        if (dto == null) {
            return null;
        }

        CommentEntity entity = CommentEntity.builder()
                .id(dto.getId())
                .dashboardId(dto.getDashboardId())
                .dashboardUrl(dto.getDashboardUrl())
                .contextKey(dto.getContextKey())
                .pointerUrl(dto.getPointerUrl())
                .author(dto.getAuthor())
                .authorEmail(dto.getAuthorEmail())
                .createdDate(dto.getCreatedDate())
                .deleted(dto.isDeleted())
                .deletedDate(dto.getDeletedDate())
                .deletedBy(dto.getDeletedBy())
                .activeVersion(dto.getActiveVersion())
                .hasActiveDraft(dto.isHasActiveDraft())
                .entityVersion(dto.getEntityVersion())
                .build();

        if (dto.getHistory() != null) {
            dto.getHistory().forEach(versionDto -> {
                CommentVersionEntity version = toVersionEntity(versionDto);
                entity.addVersion(version);
            });
        }

        return entity;
    }

    public CommentVersionDto toVersionDto(CommentVersionEntity entity) {
        if (entity == null) {
            return null;
        }

        return CommentVersionDto.builder()
                .version(entity.getVersion())
                .text(entity.getText())
                .status(entity.getStatus())
                .editedDate(entity.getEditedDate())
                .editedBy(entity.getEditedBy())
                .publishedDate(entity.getPublishedDate())
                .publishedBy(entity.getPublishedBy())
                .deleted(entity.isDeleted())
                .build();
    }

    public CommentVersionEntity toVersionEntity(CommentVersionDto dto) {
        if (dto == null) {
            return null;
        }

        return CommentVersionEntity.builder()
                .version(dto.getVersion())
                .text(dto.getText())
                .status(dto.getStatus())
                .editedDate(dto.getEditedDate())
                .editedBy(dto.getEditedBy())
                .publishedDate(dto.getPublishedDate())
                .publishedBy(dto.getPublishedBy())
                .deleted(dto.isDeleted())
                .build();
    }
}

