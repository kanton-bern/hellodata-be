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
package ch.bedag.dap.hellodata.portal.dashboard_group.repository;

import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DashboardGroupRepository extends JpaRepository<DashboardGroupEntity, UUID> {

    Page<DashboardGroupEntity> findAllByContextKey(String contextKey, Pageable pageable);

    @Query(value = "SELECT * FROM dashboard_group dg WHERE dg.context_key = :contextKey AND (" +
            "LOWER(dg.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "EXISTS (SELECT 1 FROM jsonb_array_elements(dg.entries) AS entry " +
            "WHERE LOWER(entry->>'dashboardTitle') LIKE LOWER(CONCAT('%', :search, '%'))))",
            countQuery = "SELECT COUNT(*) FROM dashboard_group dg WHERE dg.context_key = :contextKey AND (" +
                    "LOWER(dg.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "EXISTS (SELECT 1 FROM jsonb_array_elements(dg.entries) AS entry " +
                    "WHERE LOWER(entry->>'dashboardTitle') LIKE LOWER(CONCAT('%', :search, '%'))))",
            nativeQuery = true)
    Page<DashboardGroupEntity> searchByContextKeyAndNameOrDashboardTitle(
            @Param("contextKey") String contextKey, @Param("search") String search, Pageable pageable);

    boolean existsByNameIgnoreCaseAndContextKey(String name, String contextKey);

    boolean existsByNameIgnoreCaseAndContextKeyAndIdNot(String name, String contextKey, UUID id);
}
