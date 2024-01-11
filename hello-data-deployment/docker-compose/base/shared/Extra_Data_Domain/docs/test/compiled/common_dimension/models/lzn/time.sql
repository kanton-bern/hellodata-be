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

-- to_char() -> https://www.postgresql.org/docs/8.4/functions-formatting.html
-- extract() -> https://www.postgresqltutorial.com/postgresql-date-functions/postgresql-extract/
with day_series as (
    SELECT date_trunc('day', dd):: date as date
    FROM generate_series( '2021-12-30'::date, '2022-01-10'::date, '1 day'::interval) dd
)
select	TO_CHAR(date, 'yyyymmdd')::INT AS id,
          date,
          extract('YEAR' from date) as year,
          extract('QUARTER' from date) as quarter,
          extract('MONTH' from date) as month,
          extract('WEEK' from date) as week,
          date - DATE_TRUNC('QUARTER', date)::DATE + 1 AS day_of_quarter,
          extract('DAY' from date) as day_of_month,
          extract('ISODOW' FROM date) AS day_of_week,
          extract('DOY' from date) as day_of_year,
          case	WHEN EXTRACT(ISODOW FROM date) IN (6, 7)
                      THEN true
                  ELSE FALSE
              end AS day_is_weekend,
          to_char(date, 'TMDay') as day_name,
          to_char(date, 'TMMonth') as month_name,
          date + (1 - EXTRACT(ISODOW FROM date))::INT                     AS first_day_of_week,
          date + (7 - EXTRACT(ISODOW FROM date))::INT                     AS last_day_of_week,
          date + (1 - EXTRACT(DAY FROM date))::INT                        AS first_day_of_month,
          (DATE_TRUNC('MONTH', date) + INTERVAL '1 MONTH - 1 day')::DATE   AS last_day_of_month,
          DATE_TRUNC('QUARTER', date)::DATE AS first_day_of_quarter,
          (DATE_TRUNC('QUARTER', date) + INTERVAL '3 MONTH - 1 day')::DATE AS last_day_of_quarter,
          TO_DATE(EXTRACT(YEAR FROM date) || '-01-01', 'YYYY-MM-DD')       AS first_day_of_year,
          TO_DATE(EXTRACT(YEAR FROM date) || '-12-31', 'YYYY-MM-DD')       AS last_day_of_year,
          EXTRACT('DAY' FROM (DATE_TRUNC('MONTH', date) + INTERVAL '1 MONTH - 1 day')::DATE)
              AS n_days_in_month
from 	day_series