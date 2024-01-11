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

--
-- Migration scripts to initially create dashboard roles and all that is needed around it.
--


--
-- Clean up dashboard_roles table remove possible already existing dashboard roles we have assigned
--
DELETE FROM
    dashboard_roles
WHERE
    dashboard_roles.role_id IN (SELECT ab_role.id from ab_role WHERE ab_role.name LIKE 'D_%');


--
-- Clean up ab_user_role table remove possible already existing dashboard roles we have assigned
--
DELETE FROM
    ab_user_role
WHERE
    ab_user_role.role_id IN (SELECT ab_role.id from ab_role WHERE ab_role.name LIKE 'D_%');


--
-- Clean up ab_role table remove possible already existing dashboard roles we have created
--
DELETE FROM
    ab_role
WHERE
    name LIKE 'D_%';


--
-- Clean up ab_permission_view_role table remove all current permissions on datasource access from roles as this will be handled with RBAC.
--
DELETE FROM
    ab_permission_view_role
WHERE
    permission_view_id in
    (
        SELECT
            ab_permission_view.id
        FROM
            ab_permission_view
        WHERE
            permission_id IN (SELECT ab_permission.id from ab_permission WHERE ab_permission.name = 'datasource_access')
    );


--
-- Create a new dashboard role for every existing dashboard
--
INSERT INTO
    ab_role (id, name)
SELECT
    nextval('ab_role_id_seq'),
    create_dashboard_role_name(dashboard_title, dashboards.id)
FROM
    dashboards;


--
-- Assign the dashboard_role to the corresponding dashboard for RBAC
--
INSERT INTO
    dashboard_roles (id, role_id, dashboard_id)
SELECT
    nextval('dashboard_roles_id_seq'),
    ab_role.id,
    substring(ab_role.name from 'D_.*_([0-9]+)')::INTEGER
FROM
    ab_role
WHERE
    ab_role.name LIKE 'D_%';


--
-- Assign the dashboard_role to the users that are owners of dashboard but not the creator
--
INSERT INTO
    ab_user_role (id, user_id, role_id)
SELECT
    nextval('ab_user_role_id_seq'),
    ab_user.id,
    dashboard_roles.role_id
FROM
    dashboards
    JOIN dashboard_user ON dashboards.id = dashboard_user.dashboard_id
    JOIN ab_user ON dashboard_user.user_id = ab_user.id
    JOIN dashboard_roles ON dashboards.id = dashboard_roles.dashboard_id
WHERE
    ab_user.id <> COALESCE(dashboards.created_by_fk, -1);


--
-- Clean up dashboard_user table remove all current owners on a dashboard
--
TRUNCATE dashboard_user;


--
-- Set the dashboard creator as owner of the dashboard.
--
INSERT INTO
    dashboard_user (id, user_id, dashboard_id)
SELECT
    nextval('dashboard_user_id_seq'),
    created_by_fk,
    dashboards.id
FROM
    dashboards
WHERE
    created_by_fk IS NOT null;