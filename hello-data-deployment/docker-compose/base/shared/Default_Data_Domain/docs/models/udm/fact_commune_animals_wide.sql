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

with
map_data_cattle as (
	select 		commune,
				count::numeric  as cattle_n_animals,
				count_per_surfacekm2::numeric as cattle_n_animals_per_surfacekm2,
				count_per100_inhabitants::numeric as cattle_n_animals_per100_inhabitants
	from 		{{ source('tierstatistik_lzn', 'cattle_map_commune') }} 
),
map_data_equids as (
	select 		commune,
				count::numeric  as equids_n_animals,
				count_per_surfacekm2::numeric as equids_n_animals_per_surfacekm2,
				count_per100_inhabitants::numeric  as equids_n_animals_per100_inhabitants
	from 		{{ source('tierstatistik_lzn', 'equids_map_commune') }} 
),
map_data_goats as (
	select 		commune,
				count::numeric  as goats_n_animals,
				count_per_surfacekm2::numeric as goats_n_animals_per_surfacekm2,
				count_per100_inhabitants::numeric  as goats_n_animals_per100_inhabitants
	from 		{{ source('tierstatistik_lzn', 'goats_map_commune') }} 
),
map_data_sheep as (
	select 		commune,
				count::numeric  as sheep_n_animals,
				count_per_surfacekm2::numeric as sheep_n_animals_per_surfacekm2,
				count_per100_inhabitants::numeric  as sheep_n_animals_per100_inhabitants
	from 		{{ source('tierstatistik_lzn', 'sheep_map_commune') }} 
),
map_data_dogs as (
	select 		commune,
				count::numeric  as dogs_n_animals,
				count_per_surfacekm2::numeric as dogs_n_animals_per_surfacekm2,
				count_per100_inhabitants::numeric  as dogs_n_animals_per100_inhabitants
	from 		{{ source('tierstatistik_lzn', 'dogs_map_commune') }} 
),
map_data_cats as (
	select 		commune,
				count::numeric  as cats_n_animals,
				count_per_surfacekm2::numeric as cats_n_animals_per_surfacekm2,
				count_per100_inhabitants::numeric  as cats_n_animals_per100_inhabitants
	from 		{{ source('tierstatistik_lzn', 'cats_map_commune') }} 
),
map_json_common as (
	select		canton_id,
				canton_fso_id,
				canton_name_de,
				district_id,
				district_fso_id,
				district_name_de,
				commune_id,
				commune_fso_id,
				commune_name_de,
				geojson_simply
	from		{{ source('tierstatistik_csv', 'admin_units_geoshapes') }}
	where		current_date between valid_from and valid_till
	and			commune_id is not null
),
basis as (
	select 			m_cd.*,
					m_cattle.cattle_n_animals,
					m_cattle.cattle_n_animals_per_surfacekm2,
					m_cattle.cattle_n_animals_per100_inhabitants,
					m_equids.equids_n_animals,
					m_equids.equids_n_animals_per_surfacekm2,
					m_equids.equids_n_animals_per100_inhabitants,
					m_goats.goats_n_animals,
					m_goats.goats_n_animals_per_surfacekm2,
					m_goats.goats_n_animals_per100_inhabitants,
					m_sheep.sheep_n_animals,
					m_sheep.sheep_n_animals_per_surfacekm2,
					m_sheep.sheep_n_animals_per100_inhabitants,
					m_dogs.dogs_n_animals,
					m_dogs.dogs_n_animals_per_surfacekm2,
					m_dogs.dogs_n_animals_per100_inhabitants,
					m_cats.cats_n_animals,
					m_cats.cats_n_animals_per_surfacekm2,
					m_cats.cats_n_animals_per100_inhabitants
	from 			map_json_common								m_cd
	left join		map_data_cattle								m_cattle
	on				m_cd.commune_name_de = m_cattle.commune
	left join		map_data_equids								m_equids
	on				m_cd.commune_name_de = m_equids.commune
	left join		map_data_goats								m_goats
	on				m_cd.commune_name_de = m_goats.commune
	left join		map_data_sheep								m_sheep
	on				m_cd.commune_name_de = m_sheep.commune
	left join		map_data_dogs								m_dogs
	on				m_cd.commune_name_de = m_dogs.commune
	left join		map_data_cats								m_cats
	on				m_cd.commune_name_de = m_cats.commune
	order by 		canton_name_de, district_name_de, commune_name_de
)
select 	*
from 	basis