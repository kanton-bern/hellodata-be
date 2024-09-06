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
-- Various triggers to automatically handle additional business logic in Superset.
-- This script is repeatable.
--


--
-- Trigger function for inserts on dashboards table that also inserts a corresponding dashboard_role
--
-- DROP FUNCTION insert_new_dashboard_role;
CREATE OR REPLACE FUNCTION insert_new_dashboard_role() RETURNS TRIGGER AS
$$
DECLARE
    new_role_id INTEGER := nextval('ab_role_id_seq');
BEGIN
    INSERT INTO
        ab_role(id, name)
    VALUES(new_role_id, create_dashboard_role_name(new.dashboard_title, new.id));

    INSERT INTO
        dashboard_roles(id, role_id, dashboard_id)
    VALUES(nextval('dashboard_roles_id_seq'), new_role_id, new.id);

    RETURN new;
END;
$$
LANGUAGE plpgsql;


--
-- After insert Trigger on dashboard that creates a dashboard role
--
DROP TRIGGER IF EXISTS insert_new_dashboard_role on dashboards;
CREATE TRIGGER insert_new_dashboard_role
    AFTER INSERT ON dashboards
    FOR EACH ROW
EXECUTE FUNCTION insert_new_dashboard_role();


--
-- Trigger function for inserts on dashboards table that sets all BI_EDITOR users as owner of the dashboard
--
-- DROP FUNCTION insert_owners_on_new_dashboard;
CREATE OR REPLACE FUNCTION insert_owners_on_new_dashboard() RETURNS TRIGGER AS
$$
BEGIN

    INSERT INTO dashboard_user(id, user_id, dashboard_id)

        SELECT
            nextval('dashboard_user_id_seq'),
            ab_user.id,
            new.id
        FROM
            ab_user
            LEFT JOIN ab_user_role ON ab_user.id = ab_user_role.user_id
            LEFT JOIN ab_role ON ab_role.id = ab_user_role.role_id
        WHERE
            ab_role.name = 'BI_EDITOR'
            AND ab_user.id <> new.created_by_fk;

    RETURN new;
END;
$$
LANGUAGE plpgsql;


--
-- After insert Trigger on dashboard that sets all BI_EDITOR users as owner of the dashboard
--
DROP TRIGGER IF EXISTS insert_owners_on_new_dashboard on dashboards;
CREATE TRIGGER insert_owners_on_new_dashboard
    AFTER INSERT ON dashboards
    FOR EACH ROW
EXECUTE FUNCTION insert_owners_on_new_dashboard();


--
-- Function for updates on dashboards table updates the dashboard role name as well if needed
--
-- DROP FUNCTION update_dashboard_role;
CREATE OR REPLACE FUNCTION update_dashboard_role() RETURNS TRIGGER AS
$$
BEGIN

    IF old.dashboard_title <> new.dashboard_title THEN
        UPDATE
            ab_role
        SET name = create_dashboard_role_name(new.dashboard_title, new.id)
        WHERE
            substring(ab_role.name from 'D_.*_([0-9]+)')::INTEGER = new.id;
    END IF;

    RETURN new;
END;
$$
LANGUAGE plpgsql;


--
-- After update trigger on dashboards to update dashboard role name after change of dashboard title
--
DROP TRIGGER IF EXISTS update_dashboard_role on dashboards;
CREATE TRIGGER update_dashboard_role
    AFTER UPDATE ON dashboards
    FOR EACH ROW
EXECUTE FUNCTION update_dashboard_role();


--
-- Function for deletions on dashboards table deletes the corresponding dashboard role and possible assignments of the role to users
--
-- DROP FUNCTION delete_dashboard_role;
CREATE OR REPLACE FUNCTION delete_dashboard_role() RETURNS TRIGGER AS
$$
DECLARE
    ab_role_id INTEGER;
BEGIN
    SELECT ab_role.id INTO STRICT ab_role_id FROM ab_role WHERE ab_role.name = create_dashboard_role_name(old.dashboard_title, old.id);
    DELETE FROM ab_user_role where ab_user_role.role_id = ab_role_id;
    DELETE FROM ab_role where ab_role.id = ab_role_id;
    RETURN NULL;
END;
$$
LANGUAGE plpgsql;


--
-- After delete trigger that handles the deletion of dashboard roles after deleting a dashboard
--
DROP TRIGGER IF EXISTS delete_dashboard_role on dashboards;
CREATE TRIGGER delete_dashboard_role
    AFTER DELETE ON dashboards
    FOR EACH ROW
EXECUTE FUNCTION delete_dashboard_role();


--
-- Trigger function for inserts on ab_user_role table that assigns the user as owner to all dashboards if he is a BI_EDITOR
--
-- DROP FUNCTION set_new_bi_editor_as_owner;
CREATE OR REPLACE FUNCTION set_new_bi_editor_as_owner() RETURNS TRIGGER AS
$$
DECLARE
    BI_EDITOR_role_id INTEGER := (SELECT id FROM ab_role WHERE name = 'BI_EDITOR');
BEGIN

    IF new.role_id = BI_EDITOR_role_id THEN
        --- Handle ownership of dashboards
        DELETE FROM dashboard_user WHERE user_id = new.user_id;
        INSERT INTO dashboard_user
        SELECT nextval('dashboard_user_id_seq'), new.user_id, dashboards.id from dashboards;

        --- Handle ownership of charts
        DELETE FROM slice_user WHERE user_id = new.user_id;
        INSERT INTO slice_user
        SELECT nextval('slice_user_id_seq'), new.user_id, slices.id from slices;

        --- Handle ownership of datasets
        DELETE FROM sqlatable_user WHERE user_id = new.user_id;
        INSERT INTO sqlatable_user
        SELECT nextval('sqlatable_user_id_seq'), new.user_id, tables.id from tables;
    END IF;

    RETURN new;
END;
$$
LANGUAGE plpgsql;


--
-- After insert Trigger on ab_user_role that that assigns the user as owner to all dashboards if he is a BI_EDITOR
--
DROP TRIGGER IF EXISTS set_new_bi_editor_as_owner ON ab_user_role;
CREATE TRIGGER set_new_bi_editor_as_owner
    AFTER INSERT ON ab_user_role
    FOR EACH ROW
EXECUTE FUNCTION set_new_bi_editor_as_owner();


--
-- Trigger function for deletes on ab_user_role table that removes dashboard favorites if user looses dashboard role
--
-- DROP FUNCTION remove_favstar;
CREATE OR REPLACE FUNCTION remove_favstar() RETURNS TRIGGER AS
$$
BEGIN
    DELETE FROM favstar WHERE
        user_id = old.user_id
        AND class_name = 'Dashboard'
        AND obj_id = (

            SELECT
                substring(ab_role.name from 'D_.*_([0-9]+)')::INTEGER
            FROM
                ab_user_role
                LEFT JOIN ab_role on ab_user_role.role_id = ab_role.id
            WHERE
                ab_role.name LIKE 'D_%'
                AND user_id = old.user_id
                AND role_id = old.role_id

            );

    RETURN old;
END;
$$
    LANGUAGE plpgsql;


--
-- Before delete Trigger on ab_user_role that removes dashboard favorites if user looses dashboard role
--
DROP TRIGGER IF EXISTS remove_favstar ON ab_user_role;
CREATE TRIGGER remove_favstar
    BEFORE DELETE ON ab_user_role
    FOR EACH ROW
EXECUTE FUNCTION remove_favstar();


--
-- Trigger function for updates on dashboards table that removes dashboard favorites for BI_VIEWERs if dashboard is set to draft
--
-- DROP FUNCTION remove_favstar_2;
CREATE OR REPLACE FUNCTION remove_favstar_2() RETURNS TRIGGER AS
$$
BEGIN

    IF new.published = FALSE THEN
        DELETE FROM favstar
        WHERE
            obj_id = new.id
            AND class_name = 'Dashboard'
            AND user_id IN (

                SELECT
                    user_id
                FROM
                    ab_user_role
                    LEFT JOIN ab_role ON ab_role.id = ab_user_role.role_id
                WHERE
                    ab_role.name = 'BI_VIEWER'
            );

    END IF;

    RETURN new;
END;
$$
LANGUAGE plpgsql;


--
-- After update Trigger on dashboards that removes dashboard favorites for BI_VIEWERs if dashboard is set to draft
--
DROP TRIGGER IF EXISTS remove_favstar_2 ON dashboards;
CREATE TRIGGER remove_favstar_2
    AFTER UPDATE ON dashboards
    FOR EACH ROW
EXECUTE FUNCTION remove_favstar_2();


--
-- Trigger function for deletes on ab_user_role table that removes the user as owner from all dashboards if he was a BI_EDITOR
--
-- DROP FUNCTION remove_deleted_bi_editor_as_owner;
CREATE OR REPLACE FUNCTION remove_deleted_bi_editor_as_owner() RETURNS TRIGGER AS
$$
DECLARE
    BI_EDITOR_role_id INTEGER := (SELECT id FROM ab_role WHERE name = 'BI_EDITOR');
BEGIN

    IF old.role_id = BI_EDITOR_role_id THEN
        --- Delete ownership of dashboards
        DELETE FROM dashboard_user WHERE user_id = old.user_id;

        --- Delete ownership of charts
        DELETE FROM slice_user WHERE user_id = old.user_id;

        --- Delete ownership of datasets
        DELETE FROM sqlatable_user WHERE user_id = old.user_id;
    END IF;

    RETURN old;
END;
$$
LANGUAGE plpgsql;


--
-- After delete Trigger on ab_user_role that removes the user as owner from all dashboards if he was a BI_EDITOR
--
DROP TRIGGER IF EXISTS remove_deleted_bi_editor_as_owner ON ab_user_role;
CREATE TRIGGER remove_deleted_bi_editor_as_owner
    AFTER DELETE ON ab_user_role
    FOR EACH ROW
EXECUTE FUNCTION remove_deleted_bi_editor_as_owner();


--
-- Trigger function for inserts on tables table that sets all BI_EDITOR users as owner of the corresponding dataset
--
-- DROP FUNCTION insert_owners_on_new_dataset;
CREATE OR REPLACE FUNCTION insert_owners_on_new_dataset() RETURNS TRIGGER AS
$$
BEGIN

    INSERT INTO sqlatable_user(id, user_id, table_id)

    SELECT
        nextval('sqlatable_user_id_seq'),
        ab_user.id,
        new.id
    FROM
        ab_user
        LEFT JOIN ab_user_role ON ab_user.id = ab_user_role.user_id
        LEFT JOIN ab_role ON ab_role.id = ab_user_role.role_id
    WHERE
        ab_role.name = 'BI_EDITOR'
        AND ab_user.id <> new.created_by_fk;

    RETURN new;
END;
$$
    LANGUAGE plpgsql;


--
-- After insert Trigger on tables that sets all BI_EDITOR users as owner of the corresponding dataset
--
DROP TRIGGER IF EXISTS insert_owners_on_new_dataset on tables;
CREATE TRIGGER insert_owners_on_new_dataset
    AFTER INSERT ON tables
    FOR EACH ROW
EXECUTE FUNCTION insert_owners_on_new_dataset();



--
-- Trigger function for inserts on slices table that sets all BI_EDITOR users as owner of the corresponding chart
--
-- DROP FUNCTION insert_owners_on_new_slice;
CREATE OR REPLACE FUNCTION insert_owners_on_new_slice() RETURNS TRIGGER AS
$$
BEGIN

    INSERT INTO slice_user(id, user_id, slice_id)

    SELECT
        nextval('slice_user_id_seq'),
        ab_user.id,
        new.id
    FROM
        ab_user
            LEFT JOIN ab_user_role ON ab_user.id = ab_user_role.user_id
            LEFT JOIN ab_role ON ab_role.id = ab_user_role.role_id
    WHERE
        ab_role.name = 'BI_EDITOR'
        AND ab_user.id <> new.created_by_fk;

    RETURN new;
END;
$$
    LANGUAGE plpgsql;


--
-- After insert Trigger on slices that sets all BI_EDITOR users as owner of the corresponding chart
--
DROP TRIGGER IF EXISTS insert_owners_on_new_dataset on slices;
DROP TRIGGER IF EXISTS insert_owners_on_new_slice on slices;
CREATE TRIGGER insert_owners_on_new_slice
    AFTER INSERT ON slices
    FOR EACH ROW
EXECUTE FUNCTION insert_owners_on_new_slice();