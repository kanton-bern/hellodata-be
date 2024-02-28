select
    year_month,
    year,
    month,
    species,
    sum(n_animals) as n_animals

from {{ ref('rdv_breeds_long') }}

group by 1,2,3,4