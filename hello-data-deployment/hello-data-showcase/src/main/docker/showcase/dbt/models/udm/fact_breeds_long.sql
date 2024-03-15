with
add_cattle_species as (
    select		year::int,
		        month::int,
		        'Rinder' as species,
		        breed,
		        n_animals::numeric
	from 		{{ source('lzn', 'cattle_breeds') }}
),
add_equids_species as (
	select		year::int,
				month::int,
				'Equiden' as species,
				breed,
				n_animals::numeric
	from 		{{ source('lzn', 'equids_breeds') }}
),
add_goats_species as (
	select		year::int,
				month::int,
				'Ziegen' as species,
				breed,
				n_animals::numeric
	from 		{{ source('lzn', 'goats_breeds') }}
),
add_sheep_species as (
	select		year::int,
				month::int,
				'Schafe' as species,
				breed,
				n_animals::numeric
	from 		{{ source('lzn', 'sheep_breeds') }}
),
add_dogs_species as (
	select		year::int,
				month::int,
				'Hunde' as species,
				breed,
				n_animals::numeric
	from 		{{ source('lzn', 'dogs_breeds') }}
),
add_cats_species as (
	select		year::int,
				month::int,
				'Katzen' as species,
				breed,
				n_animals::numeric
	from 		{{ source('lzn', 'cats_breeds') }}
),
union_longs as (
    select * from add_cattle_species union all
    select * from add_equids_species union all
    select * from add_goats_species  union all
    select * from add_sheep_species  union all
    select * from add_dogs_species   union all
    select * from add_cats_species
)
select  (year*10000 + month * 100 + 1) as time_id,
		year,
		month,
		species,
		REPLACE(breed, '_', ' ') as breed,
		n_animals
from 	union_longs