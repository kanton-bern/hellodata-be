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
-- HELLODATA-4160 - the triggers of 02_triggers.sql matched the data domain of a DAG only below /opt/airflow/dags, so
-- the data domain roles got no permissions on DAGs in other dags folders (e.g. /storage/airflow/dags in docker
-- compose). Grant them the DAGs Airflow already knows, the triggers handle the DAGs added later.

-- the view menu "DAG:<dag_id>" of every DAG
INSERT INTO ab_view_menu (name)
SELECT 'DAG:' || d.dag_id
FROM dag d
ON CONFLICT (name) DO NOTHING;

-- the role "DD_<data_domain>" of every data domain with DAGs
INSERT INTO ab_role (name)
SELECT DISTINCT 'DD_' || substring(d.fileloc from '/dags/([a-zA-Z0-9_]+)/')
FROM dag d
WHERE substring(d.fileloc from '/dags/([a-zA-Z0-9_]+)/') IS NOT NULL
ON CONFLICT (name) DO NOTHING;

-- can_delete, can_read and can_edit on every DAG
INSERT INTO ab_permission_view (permission_id, view_menu_id)
SELECT p.id, vm.id
FROM dag d
         JOIN ab_view_menu vm ON vm.name = 'DAG:' || d.dag_id
         CROSS JOIN ab_permission p
WHERE p.name IN ('can_delete', 'can_read', 'can_edit')
ON CONFLICT (permission_id, view_menu_id) DO NOTHING;

-- those permissions for the role of the DAG's data domain
INSERT INTO ab_permission_view_role (permission_view_id, role_id)
SELECT pv.id, r.id
FROM dag d
         JOIN ab_view_menu vm ON vm.name = 'DAG:' || d.dag_id
         JOIN ab_permission_view pv ON pv.view_menu_id = vm.id
         JOIN ab_permission p ON p.id = pv.permission_id
         JOIN ab_role r ON r.name = 'DD_' || substring(d.fileloc from '/dags/([a-zA-Z0-9_]+)/')
WHERE p.name IN ('can_delete', 'can_read', 'can_edit')
ON CONFLICT (permission_view_id, role_id) DO NOTHING;
