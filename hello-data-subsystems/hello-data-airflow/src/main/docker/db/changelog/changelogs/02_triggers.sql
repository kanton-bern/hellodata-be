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
-- DAG/FAB security synchronization triggers.
--
-- This changelog keeps Flask-AppBuilder security tables in sync with Airflow's
-- dag table by creating, updating, and removing DAG-scoped security metadata.
--

--
-- Overview:
-- This script manages DAG-related security metadata in Flask-AppBuilder tables.
-- It keeps definitions idempotent by dropping legacy objects first and using
-- ON CONFLICT DO NOTHING for inserts.
--
-- Current active trigger flow:
-- 1) AFTER INSERT ON dag      -> create_dag_security_entries
-- 2) AFTER DELETE ON dag      -> remove_dag_security_entries
-- 3) AFTER UPDATE ON dag      -> delete_inactive_dag_entries
--


--
-- Delete old triggers and functions if they exist to avoid duplicates and ensure the latest version is used.
--
DROP TRIGGER IF EXISTS create_view_menu_for_dag on dag;
DROP TRIGGER IF EXISTS create_dag_security_entries on dag;
DROP TRIGGER IF EXISTS remove_dag_security_entries on dag;
DROP TRIGGER IF EXISTS delete_inactive_dag_entries on dag;
DROP TRIGGER IF EXISTS create_data_domain_role on ab_view_menu;
DROP TRIGGER IF EXISTS a_create_data_domain_role on ab_view_menu;
DROP TRIGGER IF EXISTS add_default_permissions_to_view_menu on ab_view_menu;
DROP TRIGGER IF EXISTS b_add_default_permissions_to_view_menu on ab_view_menu;
DROP TRIGGER IF EXISTS add_permission_to_data_domain_role on ab_permission_view;
DROP TRIGGER IF EXISTS remove_dag_view_menu_and_perms ON dag;

DROP FUNCTION IF EXISTS create_view_menu_for_dag();
DROP FUNCTION IF EXISTS create_data_domain_role();
DROP FUNCTION IF EXISTS add_default_permissions_to_view_menu();
DROP FUNCTION IF EXISTS add_permission_to_data_domain_role();
DROP FUNCTION IF EXISTS remove_dag_security_entries();
DROP FUNCTION IF EXISTS delete_inactive_dag_entries();
DROP FUNCTION IF EXISTS remove_dag_view_menu_and_perms();

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

-- Register trigger on dag table to create DAG security metadata.
DROP TRIGGER IF EXISTS create_dag_security_entries on dag;
CREATE TRIGGER create_dag_security_entries
    AFTER INSERT ON dag
    FOR EACH ROW
EXECUTE FUNCTION create_dag_security_entries();

--
-- remove_dag_security_entries()
-- Trigger context: AFTER DELETE ON dag
--
-- Removes security metadata created for one DAG:
-- - permission-role mappings for that DAG view menu
-- - permission views for that DAG view menu
-- - the DAG view menu itself
--
-- Note:
-- This function currently removes DAG view-menu based entries only.
-- Role cleanup is intentionally not performed here.
CREATE OR REPLACE FUNCTION remove_dag_security_entries() RETURNS TRIGGER AS
$$
DECLARE
    _view_menu_id INTEGER;
    _view_menu_name VARCHAR(255);
BEGIN
    _view_menu_name := 'DAG:' || old.dag_id;

    SELECT id INTO _view_menu_id
    FROM ab_view_menu
    WHERE name = _view_menu_name;

    IF _view_menu_id IS NOT NULL THEN
        DELETE FROM ab_permission_view_role
        WHERE permission_view_id IN (
            SELECT id
            FROM ab_permission_view
            WHERE view_menu_id = _view_menu_id
        );

        DELETE FROM ab_permission_view
        WHERE view_menu_id = _view_menu_id;

        DELETE FROM ab_view_menu
        WHERE id = _view_menu_id;
    END IF;

    RETURN old;
END;
$$
    LANGUAGE plpgsql;

-- Register trigger on dag table to remove security metadata for deleted DAGs.
DROP TRIGGER IF EXISTS remove_dag_security_entries on dag;
CREATE TRIGGER remove_dag_security_entries
    AFTER DELETE ON dag
    FOR EACH ROW
EXECUTE FUNCTION remove_dag_security_entries();

--
-- delete_inactive_dag_entries()
-- Trigger context: AFTER UPDATE ON dag
--
-- If a DAG row is updated with is_active = false (or NULL), delete that DAG row.
-- This intentionally triggers the AFTER DELETE cleanup trigger
-- (remove_dag_security_entries) to remove linked FAB security metadata.
CREATE OR REPLACE FUNCTION delete_inactive_dag_entries() RETURNS TRIGGER AS
$$
BEGIN
    IF new.is_active = false or new.is_active is null THEN
        DELETE FROM dag
        WHERE dag_id = new.dag_id;
    END IF;

    RETURN new;
END;
$$
    LANGUAGE plpgsql;

-- Register trigger on dag table to delete rows switched to inactive/null.
DROP TRIGGER IF EXISTS delete_inactive_dag_entries on dag;
CREATE TRIGGER delete_inactive_dag_entries
    AFTER UPDATE ON dag
    FOR EACH ROW
EXECUTE FUNCTION delete_inactive_dag_entries();
