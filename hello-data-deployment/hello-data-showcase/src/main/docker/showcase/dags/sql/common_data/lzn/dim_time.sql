DROP TABLE if exists lzn.dim_time;

create table lzn.dim_time
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

CREATE INDEX dim_time_date_actual_idx
    ON lzn.dim_time(date_actual);

COMMIT;

INSERT INTO lzn.dim_time
SELECT TO_CHAR(datum, 'yyyymmdd')::INT                                   AS id,
       datum                                                             AS date_actual,
       EXTRACT(EPOCH FROM datum)                                         AS epoch,
       TO_CHAR(datum, 'fmDDth')                                          AS day_suffix_en,
       CASE
           WHEN EXTRACT(ISODOW FROM datum) = 1 THEN 'Montag'
           WHEN EXTRACT(ISODOW FROM datum) = 2 THEN 'Dienstag'
           WHEN EXTRACT(ISODOW FROM datum) = 3 THEN 'Mittwoch'
           WHEN EXTRACT(ISODOW FROM datum) = 4 THEN 'Donnerstag'
           WHEN EXTRACT(ISODOW FROM datum) = 5 THEN 'Freitag'
           WHEN EXTRACT(ISODOW FROM datum) = 6 THEN 'Samstag'
           WHEN EXTRACT(ISODOW FROM datum) = 7 THEN 'Sonntag'
           END                                                           AS day_name_de,
       CASE
           WHEN EXTRACT(ISODOW FROM datum) = 1 THEN 'Mon'
           WHEN EXTRACT(ISODOW FROM datum) = 2 THEN 'Die'
           WHEN EXTRACT(ISODOW FROM datum) = 3 THEN 'Mit'
           WHEN EXTRACT(ISODOW FROM datum) = 4 THEN 'Don'
           WHEN EXTRACT(ISODOW FROM datum) = 5 THEN 'Fre'
           WHEN EXTRACT(ISODOW FROM datum) = 6 THEN 'Sam'
           WHEN EXTRACT(ISODOW FROM datum) = 7 THEN 'Son'
           END                                                           AS day_name_short3_de,
       CASE
           WHEN EXTRACT(ISODOW FROM datum) = 1 THEN 'Mo'
           WHEN EXTRACT(ISODOW FROM datum) = 2 THEN 'Di'
           WHEN EXTRACT(ISODOW FROM datum) = 3 THEN 'Mi'
           WHEN EXTRACT(ISODOW FROM datum) = 4 THEN 'Do'
           WHEN EXTRACT(ISODOW FROM datum) = 5 THEN 'Fr'
           WHEN EXTRACT(ISODOW FROM datum) = 6 THEN 'Sa'
           WHEN EXTRACT(ISODOW FROM datum) = 7 THEN 'So'
           END                                                           AS day_name_short2_de,
       TO_CHAR(datum, 'TMDay')                                           AS day_name_en,
       LEFT(TO_CHAR(datum, 'TMDay'), 3)                                  AS day_name_short3_en,
       LEFT(TO_CHAR(datum, 'TMDay'), 2)                                  AS day_name_short2_en,
       EXTRACT(ISODOW FROM datum)                                        AS day_of_week,
       EXTRACT(DAY FROM datum)                                           AS day_of_month,
       datum - DATE_TRUNC('quarter', datum)::DATE + 1                    AS day_of_quarter,
       EXTRACT(DOY FROM datum)                                           AS day_of_year,
       CASE
           WHEN EXTRACT(ISODOW FROM datum) IN (6, 7) THEN TRUE
           ELSE FALSE
           END                                                           AS day_is_weekend,
       TO_CHAR(datum, 'W')::INT                                          AS week_of_month,
       EXTRACT(WEEK FROM datum)                                          AS week_of_year,
       EXTRACT(ISOYEAR FROM datum)
           || TO_CHAR(datum, '"-W"IW-')
           || EXTRACT(ISODOW FROM datum)                                 AS week_of_year_iso,
       EXTRACT(MONTH FROM datum)                                         AS month_of_year,
       CASE
           WHEN EXTRACT(MONTH FROM datum) = 1 THEN 'Januar'
           WHEN EXTRACT(MONTH FROM datum) = 2 THEN 'Februar'
           WHEN EXTRACT(MONTH FROM datum) = 3 THEN 'März'
           WHEN EXTRACT(MONTH FROM datum) = 4 THEN 'April'
           WHEN EXTRACT(MONTH FROM datum) = 5 THEN 'Mai'
           WHEN EXTRACT(MONTH FROM datum) = 6 THEN 'Juni'
           WHEN EXTRACT(MONTH FROM datum) = 7 THEN 'Juli'
           WHEN EXTRACT(MONTH FROM datum) = 8 THEN 'August'
           WHEN EXTRACT(MONTH FROM datum) = 9 THEN 'September'
           WHEN EXTRACT(MONTH FROM datum) = 10 THEN 'Oktober'
           WHEN EXTRACT(MONTH FROM datum) = 11 THEN 'November'
           WHEN EXTRACT(MONTH FROM datum) = 12 THEN 'Dezember'
           END                                                           AS month_name_de,
       CASE
           WHEN EXTRACT(MONTH FROM datum) = 1 THEN 'Jan'
           WHEN EXTRACT(MONTH FROM datum) = 2 THEN 'Feb'
           WHEN EXTRACT(MONTH FROM datum) = 3 THEN 'Mär'
           WHEN EXTRACT(MONTH FROM datum) = 4 THEN 'Apr'
           WHEN EXTRACT(MONTH FROM datum) = 5 THEN 'Mai'
           WHEN EXTRACT(MONTH FROM datum) = 6 THEN 'Jun'
           WHEN EXTRACT(MONTH FROM datum) = 7 THEN 'Jul'
           WHEN EXTRACT(MONTH FROM datum) = 8 THEN 'Aug'
           WHEN EXTRACT(MONTH FROM datum) = 9 THEN 'Sep'
           WHEN EXTRACT(MONTH FROM datum) = 10 THEN 'Okt'
           WHEN EXTRACT(MONTH FROM datum) = 11 THEN 'Nov'
           WHEN EXTRACT(MONTH FROM datum) = 12 THEN 'Dez'
           END                                                           AS month_name_short3_de,
       TO_CHAR(datum, 'TMMonth')                                         AS month_name_en,
       TO_CHAR(datum, 'Mon')                                             AS month_name_short3_en,
       EXTRACT(QUARTER FROM datum)                                       AS quarter,
       EXTRACT(YEAR FROM datum)                                          AS year,
       CASE
           WHEN (EXTRACT(YEAR FROM datum)::int % 4 = 0 AND EXTRACT(YEAR FROM datum)::int % 100 != 0)
               OR EXTRACT(YEAR FROM datum)::int % 400 = 0 THEN TRUE
           ELSE FALSE
           END                                                           AS year_is_leap_year,
       datum + (1 - EXTRACT(ISODOW FROM datum))::INT                     AS first_day_of_week,
       datum + (7 - EXTRACT(ISODOW FROM datum))::INT                     AS last_day_of_week,
       datum + (1 - EXTRACT(DAY FROM datum))::INT                        AS first_day_of_month,
       (DATE_TRUNC('MONTH', datum) + INTERVAL '1 MONTH - 1 day')::DATE   AS last_day_of_month,
       DATE_TRUNC('quarter', datum)::DATE                                AS first_day_of_quarter,
       (DATE_TRUNC('quarter', datum) + INTERVAL '3 MONTH - 1 day')::DATE AS last_day_of_quarter,
       TO_DATE(EXTRACT(YEAR FROM datum) || '-01-01', 'YYYY-MM-DD')       AS first_day_of_year,
       TO_DATE(EXTRACT(YEAR FROM datum) || '-12-31', 'YYYY-MM-DD')       AS last_day_of_year,
       EXTRACT('DAY' FROM (DATE_TRUNC('MONTH', datum) + INTERVAL '1 MONTH - 1 day')::DATE)
                                                                         AS n_days_in_month,
       TO_CHAR(datum, 'mm-yyyy')                                         AS mm_yyyy,
       TO_CHAR(datum, 'dd-mm-yyyy')                                      AS dd_mm_yyyy,
       'Q' || TO_CHAR(datum, 'q-yyyy')                                   AS qq_yyyy

FROM ( -- [1900-01-01 - 2199-12-31]
         SELECT '2010-01-01'::DATE + SEQUENCE.DAY AS datum
         FROM GENERATE_SERIES(0, 109572) AS SEQUENCE (DAY)
         GROUP BY SEQUENCE.DAY) DQ

ORDER BY 1;