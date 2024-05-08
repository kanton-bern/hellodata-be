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

-- Default Data Domain dashboard dwh udm schema
\c hellodata_product_development_extra_data_domain_dwh;
CREATE SCHEMA IF NOT EXISTS udm;
CREATE SCHEMA IF NOT EXISTS lzn;
CREATE SCHEMA IF NOT EXISTS lzn;

create table udm.fact_commune_animals_wide
(
    canton_id                           bigint,
    canton_fso_id                       double precision,
    canton_name_de                      text,
    district_id                         bigint,
    district_fso_id                     double precision,
    district_name_de                    text,
    commune_id                          bigint,
    commune_fso_id                      double precision,
    commune_name_de                     text,
    geojson_simply                      text,
    cattle_n_animals                    numeric,
    cattle_n_animals_per_surfacekm2     numeric,
    cattle_n_animals_per100_inhabitants numeric,
    equids_n_animals                    numeric,
    equids_n_animals_per_surfacekm2     numeric,
    equids_n_animals_per100_inhabitants numeric,
    goats_n_animals                     numeric,
    goats_n_animals_per_surfacekm2      numeric,
    goats_n_animals_per100_inhabitants  numeric,
    sheep_n_animals                     numeric,
    sheep_n_animals_per_surfacekm2      numeric,
    sheep_n_animals_per100_inhabitants  numeric,
    dogs_n_animals                      numeric,
    dogs_n_animals_per_surfacekm2       numeric,
    dogs_n_animals_per100_inhabitants   numeric,
    cats_n_animals                      numeric,
    cats_n_animals_per_surfacekm2       numeric,
    cats_n_animals_per100_inhabitants   numeric
);


create table udm.fact_breeds_long
(
    time_id   integer,
    year      integer,
    month     integer,
    species   text,
    breed     text,
    n_animals numeric
);


create table udm.fact_canton_long
(
    time_id   integer,
    species   text,
    canton    text,
    n_animals numeric
);


create table udm.fact_canton_wide
(
    time_id integer,
    species text,
    ch_ag   numeric,
    ch_ai   numeric,
    ch_ar   numeric,
    ch_be   numeric,
    ch_bl   numeric,
    ch_bs   numeric,
    ch_fr   numeric,
    ch_ge   numeric,
    ch_gl   numeric,
    ch_gr   numeric,
    ch_ju   numeric,
    ch_lu   numeric,
    ch_ne   numeric,
    ch_nw   numeric,
    ch_ow   numeric,
    ch_sg   numeric,
    ch_sh   numeric,
    ch_so   numeric,
    ch_sz   numeric,
    ch_tg   numeric,
    ch_ti   numeric,
    ch_ur   numeric,
    ch_vd   numeric,
    ch_vs   numeric,
    ch_zg   numeric,
    ch_zh   numeric
);


create table udm.fact_cattle_beefiness_fattissue
(
    time_id integer,
    h       numeric,
    t       numeric,
    a       numeric,
    x       numeric,
    c       numeric,
    "1"     numeric,
    "2"     numeric,
    "3"     numeric,
    "4"     numeric,
    "5"     numeric
);


create table udm.fact_cattle_popvariations
(
    time_id              integer,
    import               numeric,
    birth                numeric,
    import_swiss_ear_tag numeric,
    slaugthering         numeric,
    death                numeric,
    yard_slaughter       numeric,
    export               numeric
);


create table udm.fact_cattle_pyr_long
(
    time_id    integer,
    population text,
    age        varchar,
    n_animals  numeric
);


create table udm.fact_cattle_pyr_wide
(
    time_id      integer,
    population   text,
    age_0_to_1   numeric,
    age_1_to_2   numeric,
    age_2_to_3   numeric,
    age_3_to_4   numeric,
    age_4_to_5   numeric,
    age_5_to_6   numeric,
    age_6_to_7   numeric,
    age_7_to_8   numeric,
    age_8_to_9   numeric,
    age_9_to_10  numeric,
    age_10_to_11 numeric,
    age_11_to_12 numeric,
    age_12_to_13 numeric,
    age_13_to_14 numeric,
    age_14_to_15 numeric,
    age_15_to_16 numeric,
    age_16_to_17 numeric,
    age_17_to_18 numeric,
    age_18_to_19 numeric,
    age_19_to_20 numeric,
    age_20_to_25 numeric
);


create table udm.fact_commune_animals_long
(
    canton_id                    bigint,
    canton_fso_id                double precision,
    canton_name_de               text,
    district_id                  bigint,
    district_fso_id              double precision,
    district_name_de             text,
    commune_id                   bigint,
    commune_fso_id               double precision,
    commune_name_de              text,
    geojson_simply               text,
    species                      text,
    n_animals                    numeric,
    n_animals_per_surfacekm2     numeric,
    n_animals_per100_inhabitants numeric
);


create table udm.dim_time
(
    id                   integer,
    date_actual          date,
    epoch                numeric,
    day_suffix_en        text,
    day_name_de          text,
    day_name_short3_de   text,
    day_name_short2_de   text,
    day_name_en          text,
    day_name_short3_en   text,
    day_name_short2_en   text,
    day_of_week          numeric,
    day_of_month         numeric,
    day_of_quarter       integer,
    day_of_year          numeric,
    day_is_weekend       boolean,
    week_of_month        integer,
    week_of_year         numeric,
    week_of_year_iso     text,
    month_of_year        numeric,
    month_name_de        text,
    month_name_short3_de text,
    month_name_en        text,
    month_name_short3_en text,
    quarter              numeric,
    year                 numeric,
    year_is_leap_year    boolean,
    first_day_of_week    date,
    last_day_of_week     date,
    first_day_of_month   date,
    last_day_of_month    date,
    first_day_of_quarter date,
    last_day_of_quarter  date,
    first_day_of_year    date,
    last_day_of_year     date,
    n_days_in_month      numeric,
    mm_yyyy              text,
    dd_mm_yyyy           text,
    qq_yyyy              text
);


create table udm.lzn_dim_zeit
(
    date                 date,
    yyyymmdd             integer,
    yyyyq                integer,
    yyyymm               integer,
    yyyyww               integer,
    epoch                numeric,
    year                 numeric,
    quarter              numeric,
    month                numeric,
    week                 numeric,
    day_of_year          numeric,
    day_of_quarter       integer,
    day_of_month         numeric,
    day_of_week          numeric,
    day_of_week_iso      numeric,
    day_is_weekend       boolean,
    month_name_en        text,
    month_name_short3_en text,
    day_name_en          text,
    day_name_short3_en   text,
    day_name_short2_en   text,
    day_suffix_en        text,
    month_name_de        text,
    month_name_short3_de text,
    day_name_de          text,
    day_name_short3_de   text,
    day_name_short2_de   text,
    first_day_of_week    date,
    last_day_of_week     date,
    first_day_of_month   date,
    last_day_of_month    date,
    first_day_of_quarter date,
    last_day_of_quarter  date,
    first_day_of_year    date,
    last_day_of_year     date,
    n_days_in_month      numeric,
    year_is_leap_year    boolean
);

