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

import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dashboard_comment_version", indexes = {
        @Index(name = "idx_dashboard_comment_version_comment_id", columnList = "comment_id"),
        @Index(name = "idx_dashboard_comment_version_status", columnList = "status"),
        @Index(name = "idx_dashboard_comment_version_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardCommentVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private DashboardCommentEntity comment;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DashboardCommentStatus status;

    @Column(name = "edited_date", nullable = false)
    private Long editedDate;

    @Column(name = "edited_by", nullable = false, length = 200)
    private String editedBy;

    @Column(name = "edited_by_email", length = 255)
    private String editedByEmail;

    @Column(name = "published_date")
    private Long publishedDate;

    @Column(name = "published_by", length = 200)
    private String publishedBy;

    @Column(name = "published_by_email", length = 255)
    private String publishedByEmail;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * Tags associated with this version, stored as comma-separated values.
     * Each version stores its own snapshot of tags for history tracking.
     */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    /**
     * Pointer URL snapshot for this version.
     * Stores the specific dashboard element URL that the comment refers to.
     */
    @Column(name = "pointer_url", length = 2000)
    private String pointerUrl;
}

