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
package ch.bedag.dap.hellodata.portal.user_audit.repository;

import ch.bedag.dap.hellodata.portal.user_audit.entity.UserAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLogEntity, UUID> {

    Page<UserAuditLogEntity> findAllByTargetUserIdOrderByOccurredAtDesc(UUID targetUserId, Pageable pageable);

    /**
     * The entries of a user that touch one business / data domain: the entry's own context key, or any of its changes.
     */
    @Query(value = """
            SELECT l.* FROM user_audit_log l
            WHERE l.target_user_id = :targetUserId
              AND (l.context_key = :contextKey
                   OR EXISTS (SELECT 1 FROM jsonb_array_elements(l.changes) c WHERE c ->> 'contextKey' = :contextKey))
            ORDER BY l.occurred_at DESC
            """,
            countQuery = """
                    SELECT count(*) FROM user_audit_log l
                    WHERE l.target_user_id = :targetUserId
                      AND (l.context_key = :contextKey
                           OR EXISTS (SELECT 1 FROM jsonb_array_elements(l.changes) c WHERE c ->> 'contextKey' = :contextKey))
                    """,
            nativeQuery = true)
    Page<UserAuditLogEntity> findAllByTargetUserIdAndContextKey(@Param("targetUserId") UUID targetUserId,
                                                                @Param("contextKey") String contextKey,
                                                                Pageable pageable);
}
