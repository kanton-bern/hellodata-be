with

    base_union as (
        select
            year,
            month,
            'cattle' as species,
            ag, ai, ar, be, bl, bs, fr, ge, gl, gr, ju, lu, ne, nw, ow, sg, sh, so, sz, tg, ti, ur, vd, vs, zg, zh--, "??" as unbekannt

        from {{ source('lzn', 'cattle_canton') }}

        union

        select
            year,
            month,
            'equids' as species,
            ag, ai, ar, be, bl, bs, fr, ge, gl, gr, ju, lu, ne, nw, ow, sg, sh, so, sz, tg, ti, ur, vd, vs, zg, zh--, "??" as unbekannt

        from {{ source('lzn', 'equids_canton') }}

        union

        select
            year,
            month,
            'goats'  as species,
            ag, ai, ar, be, bl, bs, fr, ge, gl, gr, ju, lu, ne, nw, ow, sg, sh, so, sz, tg, ti, ur, vd, vs, zg, zh--, "??" as unbekannt

        from {{ source('lzn', 'goats_canton') }}

        union

        select
            year,
            month,
            'sheep'  as species,
            ag, ai, ar, be, bl, bs, fr, ge, gl, gr, ju, lu, ne, nw, ow, sg, sh, so, sz, tg, ti, ur, vd, vs, zg, zh--, "??" as unbekannt

        from {{ source('lzn', 'sheep_canton') }}

        union

        select
            year,
            month,
            'dogs'   as species,
            ag, ai, ar, be, bl, bs, fr, ge, gl, gr, ju, lu, ne, nw, ow, sg, sh, so, sz, tg, ti, ur, vd, vs, zg, zh--, "??" as unbekannt

        from {{ source('lzn', 'dogs_canton') }}

        union

        select
            year,
            month,
            'cats'   as species,
            ag, ai, ar, be, bl, bs, fr, ge, gl, gr, ju, lu, ne, nw, ow, sg, sh, so, sz, tg, ti, ur, vd, vs, zg, zh--, "??" as unbekannt

        from {{ source('lzn', 'cats_canton') }}
    )
	
select *
from base_union
