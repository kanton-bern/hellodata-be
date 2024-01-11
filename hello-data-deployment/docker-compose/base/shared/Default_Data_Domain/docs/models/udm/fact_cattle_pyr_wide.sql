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

select 	(year::int*10000 + month::int * 100 + 1) as time_id,
          population,
          "01_years_old"::numeric as age_0_to_1,
          "12_years_old"::numeric as age_1_to_2,
          "23_years_old"::numeric as age_2_to_3,
          "34_years_old"::numeric as age_3_to_4,
          "45_years_old"::numeric as age_4_to_5,
          "56_years_old"::numeric as age_5_to_6,
          "67_years_old"::numeric as age_6_to_7,
          "78_years_old"::numeric as age_7_to_8,
          "89_years_old"::numeric as age_8_to_9,
          "910_years_old"::numeric as age_9_to_10,
          "1011_years_old"::numeric as age_10_to_11,
          "1112_years_old"::numeric as age_11_to_12,
          "1213_years_old"::numeric as age_12_to_13,
          "1314_years_old"::numeric as age_13_to_14,
          "1415_years_old"::numeric as age_14_to_15,
          "1516_years_old"::numeric as age_15_to_16,
          "1617_years_old"::numeric as age_16_to_17,
          "1718_years_old"::numeric as age_17_to_18,
          "1819_years_old"::numeric as age_18_to_19,
          "1920_years_old"::numeric as age_19_to_20,
          "2025_years_old"::numeric as age_20_to_25
from 	{{ source('tierstatistik_lzn', 'cattle_pyr') }}