with union_canton as (
    select 'Katzen' as species, * from {{ source('lzn', 'cats_canton') }}
    union all
    select 'Rinder' as species, * from {{ source('lzn', 'cattle_canton') }}
    union all
    select 'Hunde' as species, * from {{ source('lzn', 'dogs_canton') }}
    union all
    select 'Equiden' as species, * from {{ source('lzn', 'equids_canton') }}
    union all
    select 'Ziegen' as species, * from {{ source('lzn', 'goats_canton') }}
    union all
    select 'Schafe' as species, * from {{ source('lzn', 'sheep_canton') }}
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