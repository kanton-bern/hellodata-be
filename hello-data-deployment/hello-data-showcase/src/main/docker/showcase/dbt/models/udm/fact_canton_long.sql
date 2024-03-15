with canton_unpivot as ({{
    dbt_utils.unpivot(
        relation=ref("fact_canton_wide"),
        cast_to="numeric",
        exclude=["time_id", "species"],
        field_name="canton",
        value_name="n_animals",
        )
}})
select  time_id,
		species,
		upper(replace(canton, '_', '-')) as canton,
		n_animals
from    canton_unpivot