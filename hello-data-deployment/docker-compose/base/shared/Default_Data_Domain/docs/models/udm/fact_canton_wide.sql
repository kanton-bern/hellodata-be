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

with union_canton as (
    select 'Katzen' as species, * from {{ source('tierstatistik_lzn', 'cats_canton') }}
    union all
    select 'Rinder' as species, * from {{ source('tierstatistik_lzn', 'cattle_canton') }}
    union all
    select 'Hunde' as species, * from {{ source('tierstatistik_lzn', 'dogs_canton') }}
    union all
    select 'Equiden' as species, * from {{ source('tierstatistik_lzn', 'equids_canton') }}
    union all
    select 'Ziegen' as species, * from {{ source('tierstatistik_lzn', 'goats_canton') }}
    union all
    select 'Schafe' as species, * from {{ source('tierstatistik_lzn', 'sheep_canton') }}
)
select  (year::int*10000 + month::int * 100 + 1) as time_id,
        species,
        ag::numeric as ch_ag,
        ai::numeric as ch_ai,
        ar::numeric as ch_ar,
        be::numeric as ch_be,
        bl::numeric as ch_bl,
        bs::numeric as ch_bs,
        fr::numeric as ch_fr,
        ge::numeric as ch_ge,
        gl::numeric as ch_gl,
        gr::numeric as ch_gr,
        ju::numeric as ch_ju,
        lu::numeric as ch_lu,
        ne::numeric as ch_ne,
        nw::numeric as ch_nw,
        ow::numeric as ch_ow,
        sg::numeric as ch_sg,
        sh::numeric as ch_sh,
        so::numeric as ch_so,
        sz::numeric as ch_sz,
        tg::numeric as ch_tg,
        ti::numeric as ch_ti,
        ur::numeric as ch_ur,
        vd::numeric as ch_vd,
        vs::numeric as ch_vs,
        zg::numeric as ch_zg,
        zh::numeric as ch_zh
from 	union_canton