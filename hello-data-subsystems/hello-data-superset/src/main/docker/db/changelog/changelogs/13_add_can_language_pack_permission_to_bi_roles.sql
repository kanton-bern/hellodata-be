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
-- Let the BI roles load the Superset UI translations (language pack).
--
-- Since Superset 5 the frontend no longer gets the language pack in the bootstrap data,
-- it fetches it from /superset/language_pack/<lang>/. That endpoint is @has_access, so it
-- needs 'can_language_pack on Superset'. Admin, Alpha and Gamma have it, the HelloDATA BI
-- roles do not: the request answers 403 and the UI falls back to English, even when the
-- portal opens the dashboard with _l_=de. Read-only, it only returns translation strings.
--
insert into ab_permission_view_role (id, permission_view_id, role_id)
select nextval('ab_permission_view_role_id_seq'), pv.id, r.id
from ab_permission_view pv
    join ab_permission p on pv.permission_id = p.id
    join ab_view_menu vm on pv.view_menu_id = vm.id
    cross join ab_role r
where r."name" in ('BI_VIEWER', 'BI_EDITOR', 'BI_ADMIN')
    and p."name" = 'can_language_pack'
    and vm."name" = 'Superset'
    and not exists (
        select 1 from ab_permission_view_role pvr
        where pvr.permission_view_id = pv.id
            and pvr.role_id = r.id
    )
on conflict do nothing;
