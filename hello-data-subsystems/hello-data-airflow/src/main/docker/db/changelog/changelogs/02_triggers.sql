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
-- DROP FUNCTION remove_dag_view_menu_and_perms;
CREATE OR REPLACE FUNCTION remove_dag_view_menu_and_perms() RETURNS TRIGGER AS
$$
DECLARE
    _view_menu_id INTEGER;

BEGIN
    SELECT id INTO STRICT _view_menu_id FROM ab_view_menu WHERE name = 'DAG:' || old.dag_id;
    DELETE FROM ab_permission_view_role WHERE permission_view_id in (SELECT id FROM ab_permission_view WHERE view_menu_id = _view_menu_id);
    DELETE FROM ab_permission_view WHERE view_menu_id = _view_menu_id;
    DELETE FROM ab_view_menu WHERE id = _view_menu_id;

    RETURN old;
END;
$$
    LANGUAGE plpgsql;

--
-- After delete trigger on dag to remove ab_permission_view_role, ab_permission_view and ab_view_menu for a specific DAG.
--
DROP TRIGGER IF EXISTS remove_dag_view_menu_and_perms on dag;
CREATE TRIGGER remove_dag_view_menu_and_perms
    AFTER DELETE ON dag
    FOR EACH ROW
EXECUTE FUNCTION remove_dag_view_menu_and_perms();


--
-- Function to add a data_domain role in ab_role if not there yet.
-- Role gets the name of the data domain key with the prefix of DD_
-- Example DD_DATA_DOMAIN_ONE
--
-- DROP FUNCTION create_data_domain_role;
CREATE OR REPLACE FUNCTION create_data_domain_role() RETURNS TRIGGER AS
$$
DECLARE
    dag_permission VARCHAR(10);
    role_name VARCHAR(255);
    role_id_count INTEGER;

BEGIN
    -- Check if this is a DAG-related view menu
    SELECT substring(new.name from 1 for 4) INTO dag_permission;
    
    IF dag_permission = 'DAG:' THEN
        SELECT 'DD_' || substring(fileloc from '^/opt/airflow/dags/([a-zA-Z0-9_]*)') INTO role_name FROM dag WHERE dag.dag_id = substring(new.name from 5);
        
        -- Only proceed if we found a matching DAG
        IF role_name IS NOT NULL THEN
            SELECT count(ab_role.id) INTO role_id_count from ab_role where ab_role.name = role_name;

            IF role_id_count = 0 THEN
                INSERT INTO ab_role (name) VALUES(role_name);
            END IF;
        END IF;
    END IF;

    RETURN new;
END;
$$
    LANGUAGE plpgsql;

--
-- After insert trigger on ab_view_menu to create data_domain role if not there yet.
--
DROP TRIGGER IF EXISTS create_data_domain_role on ab_view_menu;
CREATE TRIGGER create_data_domain_role
    AFTER INSERT ON ab_view_menu
    FOR EACH ROW
EXECUTE FUNCTION create_data_domain_role();


--
-- Function to add dag permission to data_domain role on ab_permission_view_role if not there yet.
--
-- DROP FUNCTION add_permission_to_data_domain_role;
CREATE OR REPLACE FUNCTION add_permission_to_data_domain_role() RETURNS TRIGGER AS
$$
DECLARE
    dag_permission VARCHAR(10);
    dag_name VARCHAR(255);
    role_name VARCHAR(255);
    _role_id INTEGER;
    ab_permission_view_role_id_count INTEGER;

BEGIN

    SELECT substring(ab_view_menu.name from 1 for 4) INTO dag_permission FROM ab_view_menu WHERE ab_view_menu.id = new.view_menu_id;

    IF dag_permission is not null and dag_permission = 'DAG:' THEN
        SELECT substring(ab_view_menu.name from 5) INTO dag_name FROM ab_view_menu WHERE ab_view_menu.id = new.view_menu_id;
        
        -- Check if DAG exists before attempting to create role
        SELECT 'DD_' || substring(fileloc from '^/opt/airflow/dags/([a-zA-Z0-9_]*)') INTO role_name FROM dag WHERE dag.dag_id = dag_name;
        
        -- Only proceed if we found a matching DAG
        IF role_name IS NOT NULL THEN
            SELECT ab_role.id INTO _role_id FROM ab_role where name = role_name;
            
            -- Only proceed if we found a matching role
            IF _role_id IS NOT NULL THEN
                SELECT count(ab_permission_view_role.id) INTO ab_permission_view_role_id_count FROM ab_permission_view_role WHERE ab_permission_view_role.role_id = _role_id AND ab_permission_view_role.permission_view_id = new.id;

                IF ab_permission_view_role_id_count = 0 THEN
                    INSERT INTO ab_permission_view_role (permission_view_id, role_id) VALUES(new.id, _role_id);
                END IF;
            END IF;
        END IF;
    END IF;

    RETURN new;
END;
$$
    LANGUAGE plpgsql;

--
-- After insert trigger on ab_permission to create data_domain role if not there yet.
--
DROP TRIGGER IF EXISTS add_permission_to_data_domain_role on ab_permission_view;
CREATE TRIGGER add_permission_to_data_domain_role
    AFTER INSERT ON ab_permission_view
    FOR EACH ROW
EXECUTE FUNCTION add_permission_to_data_domain_role();