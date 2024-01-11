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


  create view "postgres"."common_data_stg"."raw_date__dbt_tmp" as (
    with day_series as (
    SELECT date_trunc('day', dd):: date as date
    FROM generate_series( '1900-01-01'::date, '2200-12-31'::date, '1 day'::interval) dd
)
select	date,
          TO_CHAR(date, 'yyyymmdd')::INT														AS yyyymmdd,
          TO_CHAR(date, 'yyyyq')::INT															AS yyyyq,
          TO_CHAR(date, 'yyyymm')::INT														AS yyyymm,
          TO_CHAR(date, 'yyyyww')::INT														AS yyyyww,
          EXTRACT('EPOCH' FROM date)															AS epoch,
          EXTRACT('YEAR' FROM date)															AS year,
          EXTRACT('QUARTER' FROM date)														AS quarter,
          EXTRACT('MONTH' FROM date)															AS month,
          EXTRACT('WEEK' FROM date)															AS week,
          EXTRACT('DOY' from date)															AS day_of_year,
          date - DATE_TRUNC('QUARTER', date)::DATE + 1										AS day_of_quarter,
          EXTRACT('DAY' FROM date)															AS day_of_month,
          EXTRACT('DOW' FROM date) + 1														AS day_of_week,
          EXTRACT('ISODOW' FROM date)                                        					AS day_of_week_iso,
          CASE	WHEN EXTRACT(ISODOW FROM date) IN (6, 7)
                      THEN TRUE
                  ELSE FALSE
              END																					AS day_is_weekend,
          TO_CHAR(date, 'TMMonth')															AS month_name_en,
          TO_CHAR(date, 'Mon')																AS month_name_short3_en,
          TO_CHAR(date, 'TMDay')																AS day_name_en,
          LEFT(TO_CHAR(date, 'TMDay'),3)														AS day_name_short3_en,
          LEFT(TO_CHAR(date, 'TMDay'),2)														AS day_name_short2_en,
          TO_CHAR(date, 'fmDDth')                                          					AS day_suffix_en,
          CASE	WHEN EXTRACT(MONTH FROM date) =  1 THEN 'Januar'
                  WHEN EXTRACT(MONTH FROM date) =  2 THEN 'Februar'
                  WHEN EXTRACT(MONTH FROM date) =  3 THEN 'März'
                  WHEN EXTRACT(MONTH FROM date) =  4 THEN 'April'
                  WHEN EXTRACT(MONTH FROM date) =  5 THEN 'Mai'
                  WHEN EXTRACT(MONTH FROM date) =  6 THEN 'Juni'
                  WHEN EXTRACT(MONTH FROM date) =  7 THEN 'Juli'
                  WHEN EXTRACT(MONTH FROM date) =  8 THEN 'August'
                  WHEN EXTRACT(MONTH FROM date) =  9 THEN 'September'
                  WHEN EXTRACT(MONTH FROM date) = 10 THEN 'Oktober'
                  WHEN EXTRACT(MONTH FROM date) = 11 THEN 'November'
                  WHEN EXTRACT(MONTH FROM date) = 12 THEN 'Dezember'
              END																					AS month_name_de,
          case	WHEN EXTRACT(MONTH FROM date) =  1 THEN 'Jan'
                  WHEN EXTRACT(MONTH FROM date) =  2 THEN 'Feb'
                  WHEN EXTRACT(MONTH FROM date) =  3 THEN 'Mär'
                  WHEN EXTRACT(MONTH FROM date) =  4 THEN 'Apr'
                  WHEN EXTRACT(MONTH FROM date) =  5 THEN 'Mai'
                  WHEN EXTRACT(MONTH FROM date) =  6 THEN 'Jun'
                  WHEN EXTRACT(MONTH FROM date) =  7 THEN 'Jul'
                  WHEN EXTRACT(MONTH FROM date) =  8 THEN 'Aug'
                  WHEN EXTRACT(MONTH FROM date) =  9 THEN 'Sep'
                  WHEN EXTRACT(MONTH FROM date) = 10 THEN 'Okt'
                  WHEN EXTRACT(MONTH FROM date) = 11 THEN 'Nov'
                  WHEN EXTRACT(MONTH FROM date) = 12 THEN 'Dez'
              END																					AS month_name_short3_de,
          CASE	WHEN EXTRACT(ISODOW FROM date) = 1 THEN 'Montag'
                  WHEN EXTRACT(ISODOW FROM date) = 2 THEN 'Dienstag'
                  WHEN EXTRACT(ISODOW FROM date) = 3 THEN 'Mittwoch'
                  WHEN EXTRACT(ISODOW FROM date) = 4 THEN 'Donnerstag'
                  WHEN EXTRACT(ISODOW FROM date) = 5 THEN 'Freitag'
                  WHEN EXTRACT(ISODOW FROM date) = 6 THEN 'Samstag'
                  WHEN EXTRACT(ISODOW FROM date) = 7 THEN 'Sonntag'
              END																					AS day_name_de,
          CASE	WHEN EXTRACT(ISODOW FROM date) = 1 THEN 'Mon'
                  WHEN EXTRACT(ISODOW FROM date) = 2 THEN 'Die'
                  WHEN EXTRACT(ISODOW FROM date) = 3 THEN 'Mit'
                  WHEN EXTRACT(ISODOW FROM date) = 4 THEN 'Don'
                  WHEN EXTRACT(ISODOW FROM date) = 5 THEN 'Fre'
                  WHEN EXTRACT(ISODOW FROM date) = 6 THEN 'Sam'
                  WHEN EXTRACT(ISODOW FROM date) = 7 THEN 'Son'
              END																					AS day_name_short3_de,
          CASE	WHEN EXTRACT(ISODOW FROM date) = 1 THEN 'Mo'
                  WHEN EXTRACT(ISODOW FROM date) = 2 THEN 'Di'
                  WHEN EXTRACT(ISODOW FROM date) = 3 THEN 'Mi'
                  WHEN EXTRACT(ISODOW FROM date) = 4 THEN 'Do'
                  WHEN EXTRACT(ISODOW FROM date) = 5 THEN 'Fr'
                  WHEN EXTRACT(ISODOW FROM date) = 6 THEN 'Sa'
                  WHEN EXTRACT(ISODOW FROM date) = 7 THEN 'So'
              END																					AS day_name_short2_de,
          date + (1 - EXTRACT(ISODOW FROM date))::INT											AS first_day_of_week,
          date + (7 - EXTRACT(ISODOW FROM date))::INT											AS last_day_of_week,
          date + (1 - EXTRACT(DAY FROM date))::INT											AS first_day_of_month,
          (DATE_TRUNC('MONTH', date) + INTERVAL '1 MONTH - 1 day')::DATE						AS last_day_of_month,
          DATE_TRUNC('QUARTER', date)::DATE 													AS first_day_of_quarter,
          (DATE_TRUNC('QUARTER', date) + INTERVAL '3 MONTH - 1 day')::DATE 					AS last_day_of_quarter,
          TO_DATE(EXTRACT(YEAR FROM date) || '-01-01', 'YYYY-MM-DD')       					AS first_day_of_year,
          TO_DATE(EXTRACT(YEAR FROM date) || '-12-31', 'YYYY-MM-DD')       					AS last_day_of_year,
          EXTRACT('DAY' FROM (DATE_TRUNC('MONTH', date) + INTERVAL '1 MONTH - 1 day')::DATE)	AS n_days_in_month,
          CASE	WHEN (EXTRACT(YEAR FROM date)::int % 4 = 0 AND EXTRACT(YEAR FROM date)::int % 100 != 0)
              OR EXTRACT(YEAR FROM date)::int % 400 = 0 THEN TRUE
                  ELSE FALSE
              END                                                           						AS year_is_leap_year
from 	day_series
  );