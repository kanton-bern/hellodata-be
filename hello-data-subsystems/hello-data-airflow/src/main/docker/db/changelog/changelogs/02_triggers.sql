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
-- Function to remove ab_permission_view_role, ab_permission_view and ab_view_menu for a specific DAG
-- Airflow unfortunately does not delete these records.
--

--
-- Overview:
-- This script manages DAG-related security metadata in Flask-AppBuilder tables.
-- It keeps definitions idempotent by dropping legacy objects first and using
-- ON CONFLICT DO NOTHING for inserts.
--


--
-- Delete old triggers and functions if they exist to avoid duplicates and ensure the latest version is used.
--
DROP TRIGGER IF EXISTS create_view_menu_for_dag on dag;
DROP TRIGGER IF EXISTS create_dag_security_entries on dag;
DROP TRIGGER IF EXISTS create_data_domain_role on ab_view_menu;
DROP TRIGGER IF EXISTS a_create_data_domain_role on ab_view_menu;
DROP TRIGGER IF EXISTS add_default_permissions_to_view_menu on ab_view_menu;
DROP TRIGGER IF EXISTS b_add_default_permissions_to_view_menu on ab_view_menu;
DROP TRIGGER IF EXISTS add_permission_to_data_domain_role on ab_permission_view;

DROP FUNCTION IF EXISTS create_view_menu_for_dag();
DROP FUNCTION IF EXISTS create_data_domain_role();
DROP FUNCTION IF EXISTS add_default_permissions_to_view_menu();
DROP FUNCTION IF EXISTS add_permission_to_data_domain_role();

-- DROP FUNCTION create_dag_security_entries;
--
-- create_dag_security_entries()
-- Trigger context: AFTER INSERT ON dag
--
-- Flow per new DAG row:
-- 1) Build view menu key "DAG:<dag_id>" and role key "DD_<data_domain>".
-- 2) Ensure ab_view_menu entry exists.
-- 3) Ensure ab_role entry exists (if derivable from fileloc).
-- 4) Ensure permission views for can_delete/can_read/can_edit exist.
-- 5) Map those permission views to the domain role.
--
-- Result:
-- The DAG becomes visible in FAB security with role mappings in a single
-- trigger execution path.
CREATE OR REPLACE FUNCTION create_dag_security_entries() RETURNS TRIGGER AS
$$
DECLARE
    _view_menu_id INTEGER;
    _role_id INTEGER;
    _view_menu_name VARCHAR(255);
    _role_name VARCHAR(255);
BEGIN
    _view_menu_name := 'DAG:' || new.dag_id;
    _role_name := 'DD_' || substring(new.fileloc from '^/opt/airflow/dags/([a-zA-Z0-9_]*)');

    INSERT INTO ab_view_menu (name)
    VALUES (_view_menu_name)
    ON CONFLICT (name) DO NOTHING;

    SELECT id INTO _view_menu_id
    FROM ab_view_menu
    WHERE name = _view_menu_name;

    IF _view_menu_id IS NULL THEN
        RETURN new;
    END IF;

    IF _role_name IS NOT NULL THEN
        INSERT INTO ab_role (name)
        VALUES (_role_name)
        ON CONFLICT (name) DO NOTHING;

        SELECT id INTO _role_id
        FROM ab_role
        WHERE name = _role_name;
    END IF;

    INSERT INTO ab_permission_view (permission_id, view_menu_id)
    SELECT p.id, _view_menu_id
    FROM ab_permission p
    WHERE p.name IN ('can_delete', 'can_read', 'can_edit')
    ON CONFLICT (permission_id, view_menu_id) DO NOTHING;

    IF _role_id IS NOT NULL THEN
        INSERT INTO ab_permission_view_role (permission_view_id, role_id)
        SELECT pv.id, _role_id
        FROM ab_permission_view pv
        JOIN ab_permission p ON p.id = pv.permission_id
        WHERE pv.view_menu_id = _view_menu_id
          AND p.name IN ('can_delete', 'can_read', 'can_edit')
        ON CONFLICT (permission_view_id, role_id) DO NOTHING;
    END IF;

    RETURN new;
END;
$$
    LANGUAGE plpgsql;

-- Register trigger on dag table to create view menu and permissions for new DAGs.
-- The name of the trigger has to be alphabetically after the add_permission_to_data_domain_role trigger to ensure the correct execution order.
-- This ensures that the role we're trying to map permissions to is already created when the mapping trigger runs.
DROP TRIGGER IF EXISTS create_dag_security_entries on dag;
CREATE TRIGGER create_dag_security_entries
    AFTER INSERT ON dag
    FOR EACH ROW
EXECUTE FUNCTION create_dag_security_entries();

--
-- add_permission_to_data_domain_role()
-- Trigger context: AFTER INSERT ON ab_permission_view
--
-- For a new permission_view row, if it belongs to a DAG view menu (name LIKE
-- 'DAG:%'), this function resolves the matching DAG and domain role and adds
-- the relation in ab_permission_view_role.
--
-- Note:
-- This is a compatibility/helper path. The flattened logic above already
-- creates permission_view_role mappings during dag insert.
CREATE OR REPLACE FUNCTION add_permission_to_data_domain_role() RETURNS TRIGGER AS
$$
BEGIN
    INSERT INTO ab_permission_view_role (permission_view_id, role_id)
    SELECT
        new.id,
        r.id
    FROM ab_view_menu vm
    JOIN dag d
      ON d.dag_id = substring(vm.name from 5)
    JOIN ab_role r
      ON r.name = 'DD_' || substring(d.fileloc from '^/opt/airflow/dags/([a-zA-Z0-9_]*)')
    WHERE vm.id = new.view_menu_id
      AND vm.name LIKE 'DAG:%'
    ON CONFLICT (permission_view_id, role_id) DO NOTHING;

    RETURN new;
END;
$$
    LANGUAGE plpgsql;

--
-- Register trigger to add new permissions to data domain role if a new permission_view is created for a DAG view menu.
--
DROP TRIGGER IF EXISTS add_permission_to_data_domain_role on ab_permission_view;
CREATE TRIGGER add_permission_to_data_domain_role
    AFTER INSERT ON ab_permission_view
    FOR EACH ROW
EXECUTE FUNCTION add_permission_to_data_domain_role();