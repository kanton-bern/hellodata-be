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
package ch.bedag.dap.hellodata.portal.dashboard_comment.repository;

import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardCommentRepository extends JpaRepository<DashboardCommentEntity, String> {

    /**
     * Find all comments for a specific dashboard in a context
     */
    List<DashboardCommentEntity> findByContextKeyAndDashboardIdOrderByCreatedDateAsc(String contextKey, Integer dashboardId);

    /**
     * Find comment by ID with history eagerly loaded
     */
    @Query("SELECT c FROM DashboardCommentEntity c LEFT JOIN FETCH c.history WHERE c.id = :id")
    Optional<DashboardCommentEntity> findByIdWithHistory(@Param("id") String id);

    /**
     * Find comment by ID with history eagerly loaded for update operations.
     * Note: Manual version check is performed in service layer for optimistic locking
     * since @Lock(LockModeType.OPTIMISTIC) cannot be used with JOIN FETCH on non-versioned child entities.
     */
    @Query("SELECT c FROM DashboardCommentEntity c LEFT JOIN FETCH c.history WHERE c.id = :id")
    Optional<DashboardCommentEntity> findByIdWithHistoryForUpdate(@Param("id") String id);

    /**
     * Find all non-deleted comments for a dashboard
     */
    List<DashboardCommentEntity> findByContextKeyAndDashboardIdAndDeletedFalseOrderByCreatedDateAsc(String contextKey, Integer dashboardId);

    /**
     * Delete all comments for a specific dashboard
     */
    void deleteByContextKeyAndDashboardId(String contextKey, Integer dashboardId);
}

