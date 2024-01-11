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

with validation_dataset as (
    with basis as (
        select 1 as attnum, 'date_pk' as attname, 'bytea'as type
        union all
        select 2 as attnum, 'date_id' as attname, 'integer'as type
        union all
        select 3 as attnum, 'date' as attname, 'date'as type
        union all
        select 4 as attnum, 'year' as attname, 'numeric'as type
        union all
        select 5 as attnum, 'quarter' as attname, 'numeric'as type
        union all
        select 6 as attnum, 'month' as attname, 'numeric'as type
        union all
        select 7 as attnum, 'week' as attname, 'numeric'as type
        union all
        select 8 as attnum, 'day_of_quarter' as attname, 'integer'as type
        union all
        select 9 as attnum, 'day_of_month' as attname, 'numeric'as type
        union all
        select 10 as attnum, 'day_of_week' as attname, 'numeric'as type
        union all
        select 11 as attnum, 'day_of_year' as attname, 'numeric'as type
        union all
        select 12 as attnum, 'day_is_weekend' as attname, 'boolean'as type
        union all
        select 13 as attnum, 'day_name' as attname, 'text'as type
        union all
        select 14 as attnum, 'month_name' as attname, 'text'as type
        union all
        select 15 as attnum, 'first_day_of_week' as attname, 'date'as type
        union all
        select 16 as attnum, 'last_day_of_week' as attname, 'date'as type
        union all
        select 17 as attnum, 'first_day_of_month' as attname, 'date'as type
        union all
        select 18 as attnum, 'last_day_of_month' as attname, 'date'as type
        union all
        select 19 as attnum, 'first_day_of_quarter' as attname, 'date'as type
        union all
        select 20 as attnum, 'last_day_of_quarter' as attname, 'date'as type
        union all
        select 21 as attnum, 'first_day_of_year' as attname, 'date'as type
        union all
        select 22 as attnum, 'last_day_of_year' as attname, 'date'as type
        union all
        select 23 as attnum, 'n_days_in_month' as attname, 'numeric'as type
        union all
        select 24 as attnum, 'load_date' as attname, 'timestamp with time zone'as type
        union all
        select 25 as attnum, 'record_source' as attname, 'text'as type
    )
    select 	md5(ROW(b.*)::TEXT)
    from 	basis b
),
     output_table as (
         with basis as (
             SELECT 	attnum,
                       attname,
                       format_type(atttypid, atttypmod) AS type
             from	pg_attribute
             where	attrelid = 'common_dimension_bdv.bdv_sat_date'::regclass
               AND    attnum > 0
               AND    NOT attisdropped
             ORDER  BY attnum
         )
         select 	md5(ROW(b.*)::TEXT)
         from basis b
     )
select			*
from			output_table		ot
                    full outer join	validation_dataset	vd
                                       on				ot.md5 = vd.md5
where 			ot.md5 is null
   or				vd.md5 is null