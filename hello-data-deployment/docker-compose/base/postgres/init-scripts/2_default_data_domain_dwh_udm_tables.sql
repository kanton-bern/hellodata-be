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
\c hellodata_product_development_default_data_domain_dwh;
CREATE SCHEMA IF NOT EXISTS udm;
CREATE SCHEMA IF NOT EXISTS tierstatistik_lzn;

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


create table udm.lzn_capitimepoint
(
    id                           varchar(40),
    subclass                     varchar(255),
    modification_counter         numeric(10),
    dbid                         numeric(10),
    capi4_systemid               numeric(10),
    capi4_pid                    numeric(10),
    erstellzeitpunkt             numeric(19),
    benutzer_erstell_fk_id       varchar(40),
    zeitpunktrechtsgueltig       timestamp,
    geschaeftnummerjahr          numeric(10),
    geschaeftnummernr            numeric(10),
    geschaeftnummerindex         numeric(10),
    geschaeftssequenz            timestamp,
    eingangsdatumzeit            timestamp,
    ausgangsdatum                timestamp,
    beschreibung                 varchar(255),
    egbtbid                      varchar(255),
    zustand                      varchar(255),
    erstellamt_fk_id             varchar(40),
    capi4_reihenfolge            numeric(10),
    aufnahmetagebuch             timestamp,
    abschlusstagebuch            timestamp,
    aufnahmehauptbuch            timestamp,
    aufnahmeverifikation         timestamp,
    datumpublikation             timestamp,
    einschreibungandere          char,
    einschreibunganmerkungen     char,
    einschreibungdienstbarkeiten char,
    einschreibungpfandrechte     char,
    einschreibungeigentum        char,
    einschreibungglaeubiger      char,
    einschreibungvormerkungen    char,
    berichtigung                 char,
    vorpruefungrequired          char,
    gemeldet                     char,
    meldungsrelevant             varchar(255),
    meldungsrelevantvorgabe      varchar(255),
    anmeldertext                 varchar(30),
    benutzer_fk_id               varchar(40),
    belegerneuerungjahr          numeric(10),
    belegerneuerungnummer        numeric(10),
    belegerneuerungindex         numeric(10),
    registraturtext              varchar(255),
    mutationsnummer              varchar(255),
    datumabschluss               timestamp,
    erstellungmessurkunde        timestamp,
    bereitgrundbuch              timestamp,
    datumrueckmeldunggb          timestamp,
    handriss                     varchar(15),
    altgbgeschaeft_jahr          numeric(10),
    altgbgeschaeft_nummer        numeric(10),
    altgbgeschaeft_amtnummer     numeric(10),
    geogeschaeft_fk_id           varchar(40),
    grundbuchgeschaeft_fk_id     varchar(40),
    geogeschkopf_fk_id           varchar(40),
    gemeinde_fk_id               varchar(40),
    mutnr_nbident                varchar(12),
    mutnr_nummer                 varchar(12),
    messurkundepdfabgelegt       char,
    vermarkungzurueckgestellt    char,
    ech0134generiertam           timestamp,
    nachforderunghaengig         varchar(1)
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


create table udm.lzn_geschaeftsaktivitaet
(
    id                       varchar(40),
    modification_counter     numeric(10),
    dbid                     numeric(10),
    capi4_systemid           numeric(10),
    capi4_pid                numeric(10),
    erstellzeitpunkt         numeric(19),
    benutzer_erstell_fk_id   varchar(40),
    neuerzustand             varchar(255),
    datumneuerzustand        timestamp,
    bemerkung                varchar(255),
    grundentscheid           varchar(4000),
    datumentscheid           timestamp,
    datumwiedervorlage       timestamp,
    datumrekurs              timestamp,
    referenznummer           varchar(20),
    benutzer_fk_id           varchar(40),
    grundbuchgeschaeft_fk_id varchar(40),
    listindex                numeric(10)
);


create table udm.lzn_organisatorischeeinheit
(
    id                     varchar(40),
    subclass               varchar(60),
    modification_counter   numeric(10),
    dbid                   numeric(10),
    capi4_systemid         numeric(10),
    capi4_pid              numeric(10),
    erstellzeitpunkt       numeric(19),
    benutzer_erstell_fk_id varchar(40),
    emailadresse           varchar(60),
    faxnummer              varchar(15),
    gueltigbis             timestamp,
    namede                 varchar(80),
    namefr                 varchar(80),
    nameit                 varchar(80),
    nameen                 varchar(80),
    ortde                  varchar(30),
    ortfr                  varchar(30),
    ortit                  varchar(30),
    orten                  varchar(30),
    plz                    numeric(5),
    strasse                varchar(30),
    telefonnummer1         varchar(15),
    telefonnummer2         varchar(15),
    externereferenz        varchar(30),
    nummer                 numeric(10),
    terravisteilnehmerid   varchar(255),
    technischeemailadresse varchar(60)
);


create table udm.udm_fact_capi
(
    organisatorischeeinheit_id varchar(40),
    namede                     varchar(80),
    ortde                      varchar(30),
    capitimepoint_id           varchar(40),
    subclass                   varchar(255),
    geschaeftsaktivitaet_id    varchar(40),
    erstellzeitpunkt           numeric(19),
    benutzer_erstell_fk_id     varchar(40),
    neuerzustand               varchar(255),
    datumneuerzustand          date,
    yyyymmdd                   integer,
    woche                      numeric,
    yyyyww                     integer,
    monat                      numeric,
    yyyymm                     integer,
    quartal                    numeric,
    yyyyq                      integer,
    jahr                       numeric,
    tage_im_status             integer,
    tage_in_zustand_kum        bigint
);


create table udm.udm_fact_capi_intervall_rollup_woche
(
    region             varchar(80),
    neuerzustand       varchar(255),
    jahr               numeric,
    woche              numeric,
    yyyymmdd           integer,
    geschaefte_anzahl  bigint,
    tage_im_status_avg numeric
);


create table udm.udm_fact_capi_intervall
(
    organisatorischeeinheit_id varchar(40),
    region                     varchar(80),
    ortde                      varchar(30),
    capitimepoint_id           varchar(40),
    subclass                   varchar(255),
    geschaeftsaktivitaet_id    varchar(40),
    erstellzeitpunkt           numeric(19),
    benutzer_erstell_fk_id     varchar(40),
    neuerzustand               varchar(255),
    datumzeitneuerzustand      timestamp,
    datumneuerzustand          date,
    yyyymmdd                   integer,
    woche                      numeric,
    yyyyww                     integer,
    monat                      numeric,
    yyyymm                     integer,
    quartal                    numeric,
    yyyyq                      integer,
    jahr                       numeric,
    zeit_im_status             interval,
    zeit_in_zustand_kum        interval,
    tage_im_status             numeric,
    tage_im_status_kum         numeric
);


create table udm.udm_fact_capi_intervall_rollup_quartal_monat
(
    region             varchar(80),
    neuerzustand       varchar(255),
    jahr               numeric,
    quartal            numeric,
    monat              numeric,
    yyyymmdd           integer,
    geschaefte_anzahl  bigint,
    tage_im_status_avg numeric,
    tage_im_status_min numeric,
    tage_im_status_max numeric
);


create table udm.udm_fact_capi_eingaenge
(
    region            varchar(80),
    datumneuerzustand date,
    jahr              numeric,
    capitimepoint_id  varchar(40)
);
