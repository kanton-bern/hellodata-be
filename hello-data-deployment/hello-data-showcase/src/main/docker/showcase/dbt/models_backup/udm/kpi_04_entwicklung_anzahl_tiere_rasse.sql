select
    year_month,
    year,
    month,
    species,
    breed,
    coalesce(n_animals,0) as n_animals

from {{ ref('rdv_breeds_long') }}