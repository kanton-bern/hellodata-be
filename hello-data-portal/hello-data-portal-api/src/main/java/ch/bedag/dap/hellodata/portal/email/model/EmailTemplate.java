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
package ch.bedag.dap.hellodata.portal.email.model;

public enum EmailTemplate {
    USER_ACCOUNT_CREATED("Notify_user_account_created", "user/notify-user-account-created"),
    USER_ROLE_CHANGED("Notify_user_role_changed", "user/notify-user-role-changed"),
    USER_DEACTIVATED("Notify_user_deactivated", "user/notify-user-deactivated"),
    USER_ACTIVATED("Notify_user_activated", "user/notify-user-activated"),
    COMMENT_STATUS_PUBLISHED("Notify_comment_status_published", "comment/notify-comment-status-published"),
    COMMENT_STATUS_DECLINED("Notify_comment_status_declined", "comment/notify-comment-status-declined"),
    COMMENT_SENT_FOR_REVIEW("Notify_comment_sent_for_review", "comment/notify-comment-sent-for-review"),
    COMMENT_DELETED("Notify_comment_deleted", "comment/notify-comment-deleted"),
    COMMENT_EDITED_BY_REVIEWER("Notify_comment_edited_by_reviewer", "comment/notify-comment-edited-by-reviewer");

    private final String subjectKey;
    private final String templateKey;

    EmailTemplate(String subjectKey, String templateKey) {
        this.subjectKey = subjectKey;
        this.templateKey = templateKey;
    }

    public String getSubjectKey() {
        return subjectKey;
    }

    public String getTemplateKey() {
        return templateKey;
    }
}
