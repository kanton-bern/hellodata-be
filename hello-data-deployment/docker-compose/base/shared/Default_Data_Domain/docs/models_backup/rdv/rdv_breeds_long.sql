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

    cattle_unpivot as (
       {{
            dbt_utils.unpivot(relation=ref('rdv_cattle_breeds'),
                              cast_to='numeric',
                              exclude=['year','month'],
                              field_name='breed',
                              value_name='n_animals')
        }}
    ),

    add_cattle_species as (
        select
            year::int,
            month::int,
            'cattle' as species,
            breed,
            n_animals::int
        from cattle_unpivot
    ),

    equids_unpivot as (
       {{
            dbt_utils.unpivot(relation=ref('rdv_equids_breeds'),
                              cast_to='numeric',
                              exclude=['year','month'],
                              field_name='breed',
                              value_name='n_animals')
        }}
    ),

    add_equids_species as (
        select
            year::int,
            month::int,
            'equids' as species,
            breed,
            n_animals::int
        from equids_unpivot
    ),

    goats_unpivot as (
       {{
            dbt_utils.unpivot(relation=ref('rdv_goats_breeds'),
                              cast_to='numeric',
                              exclude=['year','month'],
                              field_name='breed',
                              value_name='n_animals')
        }}
    ),

    add_goats_species as (
        select
            year::int,
            month::int,
            'goats' as species,
            breed,
            n_animals::int
        from goats_unpivot
    ),

    sheep_unpivot as (
       {{
            dbt_utils.unpivot(relation=ref('rdv_sheep_breeds'),
                              cast_to='numeric',
                              exclude=['year','month'],
                              field_name='breed',
                              value_name='n_animals')
        }}
    ),

    add_sheep_species as (
        select
            year::int,
            month::int,
            'sheep' as species,
            breed,
            n_animals::int
        from sheep_unpivot
    ),

    dogs_unpivot as (
       {{
            dbt_utils.unpivot(relation=ref('rdv_dogs_breeds'),
                              cast_to='numeric',
                              exclude=['year','month'],
                              field_name='breed',
                              value_name='n_animals')
        }}
    ),

    add_dogs_species as (
        select
            year::int,
            month::int,
            'dogs' as species,
            breed,
            n_animals::int
        from dogs_unpivot
    ),

    cats_unpivot as (
       {{
            dbt_utils.unpivot(relation=ref('rdv_cats_breeds'),
                              cast_to='numeric',
                              exclude=['year','month'],
                              field_name='breed',
                              value_name='n_animals')
        }}
    ),

    add_cats_species as (
        select
            year::int,
            month::int,
            'cats' as species,
            breed,
            n_animals::int
        from cats_unpivot
    ),

    union_longs as (
        select * from add_cattle_species union
        select * from add_equids_species union
        select * from add_goats_species  union
        select * from add_sheep_species  union
        select * from add_dogs_species   union
        select * from add_cats_species
    ),

    add_year_month as (
        select
            year*100 + month as year_month, *
        from union_longs
    )

select  *
from add_year_month
