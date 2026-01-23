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
package ch.bedag.dap.hellodata.portal.dashboard_comment.mapper;

import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentVersionDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentTagEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentVersionEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DashboardCommentMapper {

    public DashboardCommentDto toDto(DashboardCommentEntity entity) {
        if (entity == null) {
            return null;
        }

        return DashboardCommentDto.builder()
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
                .tags(entity.getTags() != null
                        ? entity.getTags().stream()
                        .map(DashboardCommentTagEntity::getTag)
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    public DashboardCommentEntity toEntity(DashboardCommentDto dto) {
        if (dto == null) {
            return null;
        }

        DashboardCommentEntity entity = DashboardCommentEntity.builder()
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
                DashboardCommentVersionEntity version = toVersionEntity(versionDto);
                entity.addVersion(version);
            });
        }

        return entity;
    }

    public DashboardCommentVersionDto toVersionDto(DashboardCommentVersionEntity entity) {
        if (entity == null) {
            return null;
        }

        return DashboardCommentVersionDto.builder()
                .version(entity.getVersion())
                .text(entity.getText())
                .status(entity.getStatus())
                .editedDate(entity.getEditedDate())
                .editedBy(entity.getEditedBy())
                .publishedDate(entity.getPublishedDate())
                .publishedBy(entity.getPublishedBy())
                .deleted(entity.isDeleted())
                .tags(parseTagsFromString(entity.getTags()))
                .build();
    }

    public DashboardCommentVersionEntity toVersionEntity(DashboardCommentVersionDto dto) {
        if (dto == null) {
            return null;
        }

        return DashboardCommentVersionEntity.builder()
                .version(dto.getVersion())
                .text(dto.getText())
                .status(dto.getStatus())
                .editedDate(dto.getEditedDate())
                .editedBy(dto.getEditedBy())
                .publishedDate(dto.getPublishedDate())
                .publishedBy(dto.getPublishedBy())
                .deleted(dto.isDeleted())
                .tags(tagsToString(dto.getTags()))
                .build();
    }

    /**
     * Convert comma-separated tags string to List.
     */
    private List<String> parseTagsFromString(String tagsString) {
        if (tagsString == null || tagsString.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tagsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Convert List of tags to comma-separated string.
     */
    public String tagsToString(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return String.join(",", tags);
    }
}

