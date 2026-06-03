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
-- Sets permissions for HD roles.
-- For AF_OPERATOR.
-- Is repeatable.
--


--
-- Drop temporary table temp_permission_view_role
--
DROP TABLE IF EXISTS temp_permission_view_role;


--
-- Create temporary table temp_permission_view_role if not exists
--
CREATE TEMPORARY TABLE temp_permission_view_role (
    permission_name VARCHAR(100),
    view_name VARCHAR(255),
    role_name VARCHAR(64),
    CONSTRAINT temp_permission_view_role_unique_row UNIQUE (permission_name, view_name, role_name)
);


--
-- Add default permissions for AF_OPERATOR role in temp_permission_view_role
--
INSERT INTO temp_permission_view_role VALUES ('can_edit','My Password','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','My Password','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_edit','My Profile','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','My Profile','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_create','DAG Runs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','DAG Runs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_edit','DAG Runs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_delete','DAG Runs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','DAG Runs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Browse','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Jobs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Jobs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Audit Logs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Audit Logs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_create','Variables','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Variables','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_edit','Variables','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_delete','Variables','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Variables','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Admin','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_create','Task Instances','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Task Instances','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_edit','Task Instances','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_delete','Task Instances','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Task Instances','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Configurations','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Configurations','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_create','Connections','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Connections','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_edit','Connections','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_delete','Connections','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Connections','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','SLA Misses','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','SLA Misses','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Plugins','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Plugins','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Providers','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_create','Pools','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Pools','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_edit','Pools','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_delete','Pools','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Pools','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','XComs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_delete','XComs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','XComs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','DAG Dependencies','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Datasets','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Documentation','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('menu_access','Docs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Datasets','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','ImportError','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','DAG Code','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','DAG Warnings','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','DAG Dependencies','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Task Logs','AF_OPERATOR_TEMP');
INSERT INTO temp_permission_view_role VALUES ('can_read','Website','AF_OPERATOR_TEMP');


--
-- Delete AF_OPERATOR_TEMP permissions (might happen when script gets stuck)
--
DELETE FROM
    ab_permission_view_role
WHERE
    role_id IN (SELECT ab_role.id FROM ab_role WHERE ab_role.name IN ('AF_OPERATOR_TEMP'));


--
-- Delete AF_OPERATOR_TEMP role (might happen when script gets stuck)
--
DELETE FROM
    ab_role
WHERE
    name IN ('AF_OPERATOR_TEMP');


--
-- Create AF_OPERATOR_TEMP
--
INSERT INTO ab_role VALUES (nextval('ab_role_id_seq'), 'AF_OPERATOR_TEMP');


--
-- Create the initial permissions according to temp_permission_view_role table
--
INSERT INTO
    ab_permission_view_role(id, permission_view_id, role_id)
SELECT
    nextval('ab_permission_view_role_id_seq'),
    (
        SELECT
            ab_permission_view.id
        FROM
            ab_permission_view
                JOIN ab_permission ON ab_permission.id = permission_id
                JOIN ab_view_menu ON ab_view_menu.id = view_menu_id
        WHERE (ab_permission.name = temp_permission_view_role.permission_name AND ab_view_menu.name = temp_permission_view_role.view_name)
    ) ab_permission_view_id,
    (
        SELECT
            ab_role.id
        FROM
            ab_role
        WHERE
                name = temp_permission_view_role.role_name
    ) ab_role_id

FROM
    temp_permission_view_role;


--
-- Update all users that have AF_OPERATOR to the temporary AF_OPERATOR_TEMP role
--
UPDATE ab_user_role SET
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'AF_OPERATOR_TEMP')
WHERE
    role_id = (SELECT ab_role.id FROM ab_role WHERE ab_role.name = 'AF_OPERATOR');


--
-- Delete all permissions associated with AF_OPERATOR
--
DELETE FROM
    ab_permission_view_role
WHERE
    role_id IN (SELECT ab_role.id FROM ab_role WHERE ab_role.name IN ('AF_OPERATOR'));


--
-- Delete the old AF_OPERATOR role
--
DELETE FROM ab_role WHERE name IN ('AF_OPERATOR');


--
-- Update name of temporary AF_OPERATOR_TEMP role to AF_OPERATOR
--
UPDATE ab_role SET name = 'AF_OPERATOR' WHERE name = 'AF_OPERATOR_TEMP';
