{{
dbt_utils.unpivot(
    relation=ref("fact_cattle_pyr_wide"),
    cast_to="numeric",
    exclude=["time_id", "population"],
    field_name="age",
    value_name="n_animals",
    )
}}