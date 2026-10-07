--
-- Copyright © 2026, Kanton Bern
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
-- BI_NO_ACCESS is the role a user gets when the portal sets NONE for the data domain.
--
-- Superset requires at least one role per user. The Public role cannot be used (it is
-- AUTH_ROLE_PUBLIC, the role of anonymous requests) and BI_VIEWER is too broad, as it
-- holds all_datasource_access and lets the user query every dataset via the API.
-- BI_NO_ACCESS only allows to log in and see an empty dashboard list: no datasource,
-- chart, dataset, database, explore or SQL Lab permissions.
--
-- Runs always and keeps the role's permissions in sync with the list below.
--
insert into ab_role (id, "name")
select nextval('ab_role_id_seq'), 'BI_NO_ACCESS'
where not exists (select 1 from ab_role where "name" = 'BI_NO_ACCESS');

delete from ab_permission_view_role pvr
using ab_role r, ab_permission_view pv, ab_permission p, ab_view_menu vm
where pvr.role_id = r.id
    and r."name" = 'BI_NO_ACCESS'
    and pvr.permission_view_id = pv.id
    and pv.permission_id = p.id
    and pv.view_menu_id = vm.id
    and (p."name", vm."name") not in (
        ('can_profile', 'Superset'),
        ('can_read', 'Dashboard'),
        ('menu_access', 'Dashboards')
    );

insert into ab_permission_view_role (id, permission_view_id, role_id)
select nextval('ab_permission_view_role_id_seq'), pv.id, r.id
from ab_permission_view pv
    join ab_permission p on pv.permission_id = p.id
    join ab_view_menu vm on pv.view_menu_id = vm.id
    cross join ab_role r
where r."name" = 'BI_NO_ACCESS'
    and (p."name", vm."name") in (
        ('can_profile', 'Superset'),
        ('can_read', 'Dashboard'),
        ('menu_access', 'Dashboards')
    )
    and not exists (
        select 1 from ab_permission_view_role pvr
        where pvr.permission_view_id = pv.id and pvr.role_id = r.id
    );
