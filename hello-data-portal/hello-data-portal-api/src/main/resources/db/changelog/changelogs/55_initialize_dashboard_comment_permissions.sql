--
-- Copyright © 2024, Kanton Bern
-- All rights reserved.
--
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are met:
--     * Redistributions of source code must retain the above copyright
--       notice, this list of conditions and the following disclaimer.
--     * Redistributions in binary form must reproduce the above copyright
--       notice, this list of conditions and the following disclaimer in the
--       documentation and/or other materials provided with the distribution.
--     * Neither the name of the <organization> nor the
--       names of its contributors may be used to endorse or promote products
--       derived from this software without specific prior written permission.
--
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
-- ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
-- WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
-- DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
-- DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
-- (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
-- LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
-- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
-- (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
-- SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
--

-- liquibase formatted sql

-- changeset hellodata:55.1-initialize-dashboard-comment-permissions-for-admins
-- comment: Initialize dashboard comment permissions for existing users - admins get full access (read, write, review)
INSERT INTO dashboard_comment_permission (id, user_id, context_key, read_comments, write_comments, review_comments,
                                          created_date, created_by)
SELECT uuid_generate_v4() AS id,
       u.id               AS user_id,
       c.context_key,
       true               AS read_comments,
       true               AS write_comments,
       true               AS review_comments,
       CURRENT_TIMESTAMP  AS created_date,
       'system-migration' AS created_by -- NOSONAR
FROM user_ u -- NOSONAR
         CROSS JOIN context c
         INNER JOIN user_portal_role upr ON u.id = upr.user_id
         INNER JOIN portal_role pr ON upr.portal_role_id = pr.id
         LEFT JOIN dashboard_comment_permission dcp ON dcp.user_id = u.id AND dcp.context_key = c.context_key
WHERE
  -- Users with HELLODATA_ADMIN or BUSINESS_DOMAIN_ADMIN role
    pr.name IN ('HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN')
  -- Don't create duplicates if permission already exists
  AND dcp.id IS NULL;

-- changeset hellodata:55.2-initialize-dashboard-comment-permissions-for-data-domain-admins
-- comment: Initialize dashboard comment permissions for data domain admins - get full access in their domains
INSERT INTO dashboard_comment_permission (id, user_id, context_key, read_comments, write_comments, review_comments,
                                          created_date, created_by)
SELECT uuid_generate_v4() AS id,
       ucr.user_id,
       ucr.context_key,
       true               AS read_comments,
       true               AS write_comments,
       true               AS review_comments,
       CURRENT_TIMESTAMP  AS created_date,
       'system-migration' AS created_by -- NOSONAR
FROM user_context_role ucr
         INNER JOIN role r ON ucr.role_id = r.id
         LEFT JOIN dashboard_comment_permission dcp ON dcp.user_id = ucr.user_id AND dcp.context_key = ucr.context_key
WHERE r.name = 'DATA_DOMAIN_ADMIN'
  -- Don't create duplicates
  AND dcp.id IS NULL;

-- changeset hellodata:55.3-initialize-dashboard-comment-permissions-for-regular-users
-- comment: Initialize dashboard comment permissions for regular users (NONE role) - get read-only access
INSERT INTO dashboard_comment_permission (id, user_id, context_key, read_comments, write_comments, review_comments,
                                          created_date, created_by)
SELECT uuid_generate_v4() AS id,
       u.id               AS user_id,
       c.context_key,
       true               AS read_comments,
       false              AS write_comments,
       false              AS review_comments,
       CURRENT_TIMESTAMP  AS created_date,
       'system-migration' AS created_by -- NOSONAR
FROM user_ u -- NOSONAR
         CROSS JOIN context c
         LEFT JOIN dashboard_comment_permission dcp ON dcp.user_id = u.id AND dcp.context_key = c.context_key
         LEFT JOIN user_portal_role upr ON upr.user_id = u.id
         LEFT JOIN portal_role pr
                   ON upr.portal_role_id = pr.id AND pr.name IN ('HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN')
         LEFT JOIN user_context_role ucr ON ucr.user_id = u.id AND ucr.context_key = c.context_key
         LEFT JOIN role r ON ucr.role_id = r.id AND r.name = 'DATA_DOMAIN_ADMIN'
WHERE
  -- Exclude users who already have permissions (from previous changesets)
    dcp.id IS NULL
  -- Exclude users who have HELLODATA_ADMIN or BUSINESS_DOMAIN_ADMIN role
  AND pr.id IS NULL
  -- Exclude users who have DATA_DOMAIN_ADMIN role in this context
  AND r.id IS NULL;

-- rollback DELETE FROM dashboard_comment_permission WHERE created_by = 'system-migration';

