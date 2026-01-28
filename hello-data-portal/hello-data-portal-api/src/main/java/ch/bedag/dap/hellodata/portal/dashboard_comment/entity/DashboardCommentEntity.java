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
package ch.bedag.dap.hellodata.portal.dashboard_comment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dashboard_comment", indexes = {
        @Index(name = "idx_comment_dashboard_context", columnList = "dashboard_id, context_key"),
        @Index(name = "idx_comment_author_email", columnList = "author_email"),
        @Index(name = "idx_comment_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardCommentEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "dashboard_id", nullable = false)
    private Integer dashboardId;

    @Column(name = "dashboard_url", nullable = false, length = 2000)
    private String dashboardUrl;

    @Column(name = "context_key", nullable = false, length = 100)
    private String contextKey;

    @Column(name = "author", nullable = false, length = 200)
    private String author;

    @Column(name = "author_email", nullable = false, length = 200)
    private String authorEmail;

    @Column(name = "created_date", nullable = false)
    private Long createdDate;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_date")
    private Long deletedDate;

    @Column(name = "deleted_by", length = 200)
    private String deletedBy;

    @Column(name = "active_version", nullable = false)
    @Builder.Default
    private Integer activeVersion = 1;

    @Column(name = "has_active_draft", nullable = false)
    @Builder.Default
    private boolean hasActiveDraft = false;

    @Column(name = "entity_version", nullable = false)
    @Builder.Default
    private Long entityVersion = 0L;

    /**
     * Original comment ID from which this comment was imported.
     * Used to track imports from other dashboards and prevent duplicates on re-import.
     */
    @Column(name = "imported_from_id", length = 36)
    private String importedFromId;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("version ASC")
    @Builder.Default
    private List<DashboardCommentVersionEntity> history = new ArrayList<>();

    public void addVersion(DashboardCommentVersionEntity version) {
        history.add(version);
        version.setComment(this);
    }

    public void removeVersion(DashboardCommentVersionEntity version) {
        history.remove(version);
        version.setComment(null);
    }
}



