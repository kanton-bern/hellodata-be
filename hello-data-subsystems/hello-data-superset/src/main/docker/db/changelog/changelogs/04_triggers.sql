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
-- Additional Superset metadata business logic.
--
-- Main invariants:
--   * Every dashboard has exactly one generated dashboard role.
--   * Generated dashboard roles follow create_dashboard_role_name(...), including
--     the dashboard ID as the trailing numeric part.
--   * Every BI_EDITOR owns all dashboards, charts and datasets.
--   * Removing BI_EDITOR removes all ownership for that user.
--
-- This script is repeatable.
--


--
-- Create the generated role for a newly inserted dashboard and link it through
-- dashboard_roles. The role name is the canonical dashboard-role identifier.
--
CREATE OR REPLACE FUNCTION insert_new_dashboard_role() RETURNS TRIGGER AS
$$
DECLARE
    new_role_id INTEGER := nextval('ab_role_id_seq');
BEGIN
    INSERT INTO ab_role(id, name)
    VALUES (new_role_id, create_dashboard_role_name(new.dashboard_title, new.id));

    INSERT INTO dashboard_roles(id, role_id, dashboard_id)
    VALUES (nextval('dashboard_roles_id_seq'), new_role_id, new.id);

    RETURN new;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS insert_new_dashboard_role ON dashboards;
CREATE TRIGGER insert_new_dashboard_role
    AFTER INSERT ON dashboards
    FOR EACH ROW
EXECUTE FUNCTION insert_new_dashboard_role();


--
-- Make all BI_EDITOR users owners of a newly created dashboard.
-- The creator is excluded because Superset manages the creator ownership itself.
-- IS DISTINCT FROM keeps all BI_EDITORs when created_by_fk is NULL (e.g. CLI imports).
-- NOT EXISTS protects against ownership rows that already exist.
--
CREATE OR REPLACE FUNCTION insert_owners_on_new_dashboard() RETURNS TRIGGER AS
$$
BEGIN
    INSERT INTO dashboard_user(id, user_id, dashboard_id)
    SELECT
        nextval('dashboard_user_id_seq'),
        u.id,
        new.id
    FROM ab_user u
        JOIN ab_user_role ur ON u.id = ur.user_id
        JOIN ab_role r ON r.id = ur.role_id
    WHERE
        r.name = 'BI_EDITOR'
        AND u.id IS DISTINCT FROM new.created_by_fk
        AND NOT EXISTS (
            SELECT 1
            FROM dashboard_user du
            WHERE du.user_id = u.id
              AND du.dashboard_id = new.id
        );

    RETURN new;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS insert_owners_on_new_dashboard ON dashboards;
CREATE TRIGGER insert_owners_on_new_dashboard
    AFTER INSERT ON dashboards
    FOR EACH ROW
EXECUTE FUNCTION insert_owners_on_new_dashboard();


--
-- Keep the generated dashboard role name synchronized with the dashboard title.
-- The trailing dashboard ID in the role name is an intentional invariant and is
-- used to identify the corresponding generated role.
--
CREATE OR REPLACE FUNCTION update_dashboard_role() RETURNS TRIGGER AS
$$
BEGIN
    IF old.dashboard_title <> new.dashboard_title THEN
        UPDATE ab_role
        SET name = create_dashboard_role_name(new.dashboard_title, new.id)
        WHERE substring(ab_role.name from 'D_.*_([0-9]+)')::INTEGER = new.id;
    END IF;

    RETURN new;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_dashboard_role ON dashboards;
CREATE TRIGGER update_dashboard_role
    AFTER UPDATE ON dashboards
    FOR EACH ROW
EXECUTE FUNCTION update_dashboard_role();


--
-- Delete the generated dashboard role when its dashboard is deleted.
-- INTO STRICT intentionally enforces that exactly one generated role exists.
-- Any missing or duplicate role is treated as inconsistent metadata.
--
CREATE OR REPLACE FUNCTION delete_dashboard_role() RETURNS TRIGGER AS
$$
DECLARE
    ab_role_id INTEGER;
BEGIN
    SELECT ab_role.id
    INTO STRICT ab_role_id
    FROM ab_role
    WHERE ab_role.name = create_dashboard_role_name(old.dashboard_title, old.id);

    DELETE FROM ab_user_role
    WHERE ab_user_role.role_id = ab_role_id;

    DELETE FROM ab_role
    WHERE ab_role.id = ab_role_id;

    RETURN NULL;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS delete_dashboard_role ON dashboards;
CREATE TRIGGER delete_dashboard_role
    AFTER DELETE ON dashboards
    FOR EACH ROW
EXECUTE FUNCTION delete_dashboard_role();


--
-- When BI_EDITOR is granted, normalize the user's ownership: the user becomes
-- owner of every existing dashboard, chart and dataset.
-- Existing ownership rows are deliberately removed first.
--
CREATE OR REPLACE FUNCTION set_new_bi_editor_as_owner() RETURNS TRIGGER AS
$$
DECLARE
    BI_EDITOR_role_id INTEGER := (SELECT id FROM ab_role WHERE name = 'BI_EDITOR');
BEGIN
    IF new.role_id = BI_EDITOR_role_id THEN
        -- Dashboards
        DELETE FROM dashboard_user WHERE user_id = new.user_id;
        INSERT INTO dashboard_user
        SELECT nextval('dashboard_user_id_seq'), new.user_id, dashboards.id
        FROM dashboards;

        -- Charts
        DELETE FROM slice_user WHERE user_id = new.user_id;
        INSERT INTO slice_user
        SELECT nextval('slice_user_id_seq'), new.user_id, slices.id
        FROM slices;

        -- Datasets
        DELETE FROM sqlatable_user WHERE user_id = new.user_id;
        INSERT INTO sqlatable_user
        SELECT nextval('sqlatable_user_id_seq'), new.user_id, tables.id
        FROM tables;
    END IF;

    RETURN new;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_new_bi_editor_as_owner ON ab_user_role;
CREATE TRIGGER set_new_bi_editor_as_owner
    AFTER INSERT ON ab_user_role
    FOR EACH ROW
EXECUTE FUNCTION set_new_bi_editor_as_owner();


--
-- When a generated dashboard role is removed from a user, remove that user's
-- favorite for the corresponding dashboard. The dashboard ID is intentionally
-- derived from the generated role-name convention.
--
CREATE OR REPLACE FUNCTION remove_favstar() RETURNS TRIGGER AS
$$
BEGIN
    DELETE FROM favstar
    WHERE
        user_id = old.user_id
        AND class_name = 'Dashboard'
        AND obj_id = (
            SELECT substring(ab_role.name from 'D_.*_([0-9]+)')::INTEGER
            FROM ab_user_role
                LEFT JOIN ab_role ON ab_user_role.role_id = ab_role.id
            WHERE
                ab_role.name LIKE 'D_%'
                AND user_id = old.user_id
                AND role_id = old.role_id
        );

    RETURN old;
END;
$$
LANGUAGE plpgsql;

-- BEFORE DELETE is required because remove_favstar() reads the role assignment
-- that is about to be deleted.
DROP TRIGGER IF EXISTS remove_favstar ON ab_user_role;
CREATE TRIGGER remove_favstar
    BEFORE DELETE ON ab_user_role
    FOR EACH ROW
EXECUTE FUNCTION remove_favstar();


--
-- Remove BI_VIEWER favorites when a dashboard transitions to draft.
-- The trigger condition ensures this runs only when published becomes FALSE,
-- not on every update to an already unpublished dashboard.
--
CREATE OR REPLACE FUNCTION remove_favstar_2() RETURNS TRIGGER AS
$$
BEGIN
    DELETE FROM favstar
    WHERE
        obj_id = new.id
        AND class_name = 'Dashboard'
        AND user_id IN (
            SELECT ur.user_id
            FROM ab_user_role ur
                JOIN ab_role r ON r.id = ur.role_id
            WHERE r.name = 'BI_VIEWER'
        );

    RETURN new;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS remove_favstar_2 ON dashboards;
CREATE TRIGGER remove_favstar_2
    AFTER UPDATE OF published ON dashboards
    FOR EACH ROW
    WHEN (old.published IS DISTINCT FROM FALSE AND new.published = FALSE)
EXECUTE FUNCTION remove_favstar_2();


--
-- When BI_EDITOR is revoked, remove all dashboard, chart and dataset ownership
-- from the user. This is intentional: users without BI_EDITOR must own nothing.
--
CREATE OR REPLACE FUNCTION remove_deleted_bi_editor_as_owner() RETURNS TRIGGER AS
$$
DECLARE
    BI_EDITOR_role_id INTEGER := (SELECT id FROM ab_role WHERE name = 'BI_EDITOR');
BEGIN
    IF old.role_id = BI_EDITOR_role_id THEN
        DELETE FROM dashboard_user WHERE user_id = old.user_id;
        DELETE FROM slice_user WHERE user_id = old.user_id;
        DELETE FROM sqlatable_user WHERE user_id = old.user_id;
    END IF;

    RETURN old;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS remove_deleted_bi_editor_as_owner ON ab_user_role;
CREATE TRIGGER remove_deleted_bi_editor_as_owner
    AFTER DELETE ON ab_user_role
    FOR EACH ROW
EXECUTE FUNCTION remove_deleted_bi_editor_as_owner();


--
-- Make all BI_EDITOR users owners of a newly created dataset.
-- The creator is excluded because Superset manages the creator ownership itself.
-- IS DISTINCT FROM keeps all BI_EDITORs when created_by_fk is NULL (e.g. CLI imports).
-- NOT EXISTS protects against ownership rows that already exist.
--
CREATE OR REPLACE FUNCTION insert_owners_on_new_dataset() RETURNS TRIGGER AS
$$
BEGIN
    INSERT INTO sqlatable_user(id, user_id, table_id)
    SELECT
        nextval('sqlatable_user_id_seq'),
        u.id,
        new.id
    FROM ab_user u
        JOIN ab_user_role ur ON u.id = ur.user_id
        JOIN ab_role r ON r.id = ur.role_id
    WHERE
        r.name = 'BI_EDITOR'
        AND u.id IS DISTINCT FROM new.created_by_fk
        AND NOT EXISTS (
            SELECT 1
            FROM sqlatable_user su
            WHERE su.user_id = u.id
              AND su.table_id = new.id
        );

    RETURN new;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS insert_owners_on_new_dataset ON tables;
CREATE TRIGGER insert_owners_on_new_dataset
    AFTER INSERT ON tables
    FOR EACH ROW
EXECUTE FUNCTION insert_owners_on_new_dataset();


--
-- Make all BI_EDITOR users owners of a newly created chart.
-- The creator is excluded because Superset manages the creator ownership itself.
-- IS DISTINCT FROM keeps all BI_EDITORs when created_by_fk is NULL (e.g. CLI imports).
-- NOT EXISTS protects against ownership rows that already exist.
--
CREATE OR REPLACE FUNCTION insert_owners_on_new_slice() RETURNS TRIGGER AS
$$
BEGIN
    INSERT INTO slice_user(id, user_id, slice_id)
    SELECT
        nextval('slice_user_id_seq'),
        u.id,
        new.id
    FROM ab_user u
        JOIN ab_user_role ur ON u.id = ur.user_id
        JOIN ab_role r ON r.id = ur.role_id
    WHERE
        r.name = 'BI_EDITOR'
        AND u.id IS DISTINCT FROM new.created_by_fk
        AND NOT EXISTS (
            SELECT 1
            FROM slice_user su
            WHERE su.user_id = u.id
              AND su.slice_id = new.id
        );

    RETURN new;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS insert_owners_on_new_slice ON slices;
CREATE TRIGGER insert_owners_on_new_slice
    AFTER INSERT ON slices
    FOR EACH ROW
EXECUTE FUNCTION insert_owners_on_new_slice();
