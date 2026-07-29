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
-- HELLODATA-2351 - admin user has missing can_create on Users


-- insert missing permission
INSERT INTO ab_permission (id, name)
VALUES (NEXTVAL('ab_permission_id_seq'), 'can_create')
    ON CONFLICT (name) DO NOTHING;


-- enable can_create on Users resource
INSERT INTO public.ab_permission_view (id, permission_id, view_menu_id)
VALUES (
           nextval('ab_permission_view_id_seq'),
           (SELECT id FROM ab_permission WHERE name = 'can_create'),
           (SELECT id FROM ab_view_menu WHERE name = 'Users')
       ) ON CONFLICT DO NOTHING;


-- insert permission view role: can_create on Users for the Admin role
with role_ as (
    select id from ab_role where "name" = 'Admin'
),
     permission_view_ as (
         select pv.id from ab_permission_view pv
                               join ab_permission p on
             pv.permission_id = p.id
                               join ab_view_menu vm on
             pv.view_menu_id = vm.id
         where
             p."name" = 'can_create' and
             vm."name" = 'Users'
     )
insert into ab_permission_view_role
(id, permission_view_id, role_id) values
(nextval('ab_permission_view_role_id_seq'), (select id from permission_view_ limit 1), (select id from role_ limit 1))
on conflict do nothing;