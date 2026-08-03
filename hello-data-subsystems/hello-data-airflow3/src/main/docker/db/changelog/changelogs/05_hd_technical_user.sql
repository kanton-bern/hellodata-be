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

-- Technical service user for the HelloDATA Airflow-3 sidecar's read-only REST polling.
--
-- Airflow 3 dropped Basic auth; the sidecar authenticates by minting a JWT whose `sub` MUST be
-- an existing FAB user id (Airflow rejects the token otherwise). FAB user ids are DB-assigned,
-- so we seed a service user with a FIXED, high id (900000) that is identical on every instance
-- and never collides with real users (which start at 1). The sidecar pins
-- AIRFLOW_API_USER_ID=900000 (its application.yaml default), so DAG / DAG-run monitoring works
-- out of the box on any deployment that runs these migrations.
--
-- Role: Viewer (read-only) — the sidecar only issues GET /api/v2/dags and .../dagRuns; no write
-- access is needed. Password is NULL: this account is JWT-only and cannot log in interactively.
-- Idempotent (guards + fixed ids) so it is safe under Liquibase runOnChange.

INSERT INTO ab_user (id, first_name, last_name, username, email, active, password, login_count, fail_login_count, created_on, changed_on)
SELECT 900000, 'HelloDATA', 'Sidecar', 'hd-sidecar', 'hd-sidecar@hellodata.local', true, NULL, 0, 0, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM ab_user WHERE id = 900000 OR username = 'hd-sidecar'
);

INSERT INTO ab_user_role (id, user_id, role_id)
SELECT 900000, 900000, r.id
FROM ab_role r
WHERE r.name = 'Viewer'
  AND NOT EXISTS (
      SELECT 1 FROM ab_user_role WHERE user_id = 900000 AND role_id = r.id
  );

-- The sidecar also publishes the workspace's Users/Roles/Permissions by reading the FAB
-- auth-manager API (GET /auth/fab/v1/{users,roles,permissions}). Those endpoints require
-- `can read` on the FAB security resources, which the Viewer role does NOT have. Grant a
-- dedicated read-only role (does NOT touch Viewer, so other viewers are unaffected).
INSERT INTO ab_role (id, name)
SELECT 900001, 'hd_sidecar_monitor'
WHERE NOT EXISTS (SELECT 1 FROM ab_role WHERE name = 'hd_sidecar_monitor');

-- Link can_read on Users / Roles / Permissions to the role (only pairs that exist; safe if absent).
INSERT INTO ab_permission_view_role (id, permission_view_id, role_id)
SELECT 900000 + row_number() OVER (ORDER BY pv.id), pv.id, r.id
FROM ab_permission_view pv
JOIN ab_permission p ON p.id = pv.permission_id AND p.name = 'can_read'
JOIN ab_view_menu vm ON vm.id = pv.view_menu_id AND vm.name IN ('Users', 'Roles', 'Permissions')
CROSS JOIN (SELECT id FROM ab_role WHERE name = 'hd_sidecar_monitor') r
WHERE NOT EXISTS (
    SELECT 1 FROM ab_permission_view_role x WHERE x.permission_view_id = pv.id AND x.role_id = r.id
);

-- Assign the read-only monitor role to the sidecar service user (in addition to Viewer).
INSERT INTO ab_user_role (id, user_id, role_id)
SELECT 900001, 900000, r.id
FROM ab_role r
WHERE r.name = 'hd_sidecar_monitor'
  AND NOT EXISTS (
      SELECT 1 FROM ab_user_role WHERE user_id = 900000 AND role_id = r.id
  );
