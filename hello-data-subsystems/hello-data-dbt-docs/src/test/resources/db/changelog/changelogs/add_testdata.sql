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

-- Users
INSERT INTO dbt_user
(id, created_by, created_date, modified_by, modified_date, email, enabled, first_name, last_name, user_name, superuser)
VALUES('21509de5-df4f-42c7-8d6b-4226026fd1b1', 'SYSTEM', '2023-06-30 11:32:29.283', 'SYSTEM', '2023-06-30 11:32:29.283', 'momi-user@bedag.ch', true, 'momi-user', 'user', 'momi-user', false);


INSERT INTO dbt_user
(id, created_by, created_date, modified_by, modified_date, email, enabled, first_name, last_name, user_name, superuser)
VALUES('8cbf43de-5f5f-4045-83e3-c6f46d606b60', 'SYSTEM', '2023-06-30 11:32:29.283', 'SYSTEM', '2023-06-30 11:32:29.283', 'user-no-roles@bedag.ch', true, 'no-roles', 'user', 'user-no-roles', false);

-- Roles
INSERT INTO "role"
(id, created_by, created_date, modified_by, modified_date, enabled, "key", "name")
VALUES('41d6f151-f25d-4a88-9239-67f33f596d14', 'SYSTEM', '2023-06-30 11:22:29.277', NULL, NULL, true, 'momi', 'ROLE_D_MOMI');
INSERT INTO "role"
(id, created_by, created_date, modified_by, modified_date, enabled, "key", "name")
VALUES('438d291f-9035-48d2-be95-2c4ae38331b5', 'SYSTEM', '2023-06-30 11:22:29.313', NULL, NULL, true, 'kibon', 'ROLE_D_KIBON');

-- User-roles
INSERT INTO users_roles (user_id, role_id) VALUES('21509de5-df4f-42c7-8d6b-4226026fd1b1', '41d6f151-f25d-4a88-9239-67f33f596d14');

-- Privilege
INSERT INTO privilege (id, created_by, created_date, modified_by, modified_date, "name")
VALUES('19cfca7e-5bce-4688-b586-107ad8effb01', 'SYSTEM', '2023-06-30 10:34:27.604', NULL, NULL, 'READ_PRIVILEGE');

INSERT INTO privilege (id, created_by, created_date, modified_by, modified_date, "name")
VALUES('96d9e231-66d3-4e10-8c2d-c997e99bf7a6', 'SYSTEM', '2023-06-30 10:34:27.660', NULL, NULL, 'WRITE_PRIVILEGE');

-- Add Privilege to role
INSERT INTO roles_privileges (role_id, privilege_id) VALUES('41d6f151-f25d-4a88-9239-67f33f596d14', '19cfca7e-5bce-4688-b586-107ad8effb01');
INSERT INTO roles_privileges (role_id, privilege_id) VALUES('438d291f-9035-48d2-be95-2c4ae38331b5', '19cfca7e-5bce-4688-b586-107ad8effb01');
