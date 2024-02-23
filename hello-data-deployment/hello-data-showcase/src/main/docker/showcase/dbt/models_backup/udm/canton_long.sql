with

    base_union_unpivot as (
        {{
            dbt_utils.unpivot(relation=ref('canton_base_union'),
                              cast_to='integer',
                              exclude=['year','month','species'],
                              field_name='canton',
                              value_name='n_animals')
        }}
    ),

    base_admin_units as (
        select
            canton_id,
            canton_fso_id,
            canton_name_de,
            canton_name_short_de,
            district_id,
            district_fso_id,
            district_name_de,
            district_name_short_de,
            commune_id,
            commune_fso_id,
            commune_name_de,
            commune_name_short_de

        from {{ source('lzn', 'admin_units_snapshots_wide') }}
    )

select *
from base_union_unpivot

