--
-- Copyright © 2025, Kanton Bern
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
-- HELLODATA-4067: allow the portal PDF export to render chart screenshots as a BI_VIEWER.
--
-- The screenshot renderer opens each chart's standalone Explore page
-- (/superset/slice/<id>/ -> /explore/). BI_VIEWER, by design, cannot reach the Explore
-- view, so the headless render is redirected to the login/dashboard-list page, the chart
-- element never appears, and the export times out (only Admin/Editor roles could export).
--
-- These are strictly READ / render permissions. They do NOT grant any write (charts,
-- dashboards and datasets stay read-only), no SQL Lab, no datasource "samples", and no
-- menu access (the Superset navigation is unchanged and viewers cannot browse or discover
-- assets). Row-level security (RLS_*) and datasource access remain fully enforced, so a
-- viewer still only ever sees their own authorized, RLS-filtered rows — the render simply
-- applies the requesting user's own RLS. The only functional change is that the Explore
-- drill-down of a chart the viewer already has access to becomes available (read-only).
--
insert into ab_permission_view_role (id, permission_view_id, role_id)
select nextval('ab_permission_view_role_id_seq'), pv.id, r.id
from ab_permission_view pv
    join ab_permission p on pv.permission_id = p.id
    join ab_view_menu vm on pv.view_menu_id = vm.id
    cross join ab_role r
where r."name" = 'BI_VIEWER'
    and (p."name", vm."name") in (
        ('can_read', 'Explore'),
        ('can_read', 'ExploreFormDataRestApi'),
        ('can_read', 'ExplorePermalinkRestApi'),
        ('can_slice', 'Superset'),
        ('can_slice_json', 'Superset'),
        ('can_explore', 'Superset')
    )
    and not exists (
        select 1 from ab_permission_view_role pvr
        where pvr.permission_view_id = pv.id
            and pvr.role_id = r.id
    )
on conflict do nothing;
