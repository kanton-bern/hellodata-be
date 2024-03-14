select
    year_month,
    year,
    month,
    species,
    breed,
    n_animals

from {{ ref('rdv_breeds_long') }}

where year_month = (select max(year_month) from {{ ref('rdv_breeds_long') }})