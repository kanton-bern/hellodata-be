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

--
-- Various functions to handle additional business logic in Superset.
-- This script is repeatable.
--


--
-- Slugify function turns a string into a SLUG
--
-- DROP FUNCTION slugify;
CREATE OR REPLACE FUNCTION slugify("value" TEXT)
    RETURNS TEXT AS
$$
    -- removes accents (diacritic signs) from a given string --
WITH "unaccented" AS (
    SELECT unaccent("value") AS "value"
),
     -- lowercase the string
     "lowercase" AS (
         SELECT lower("value") AS "value"
         FROM "unaccented"
     ),
     -- remove single and double quotes
     "removed_quotes" AS (
         SELECT regexp_replace("value", '[''"]+', '', 'gi') AS "value"
         FROM "lowercase"
     ),
     -- replaces anything that's not a letter, number, or hyphen('-') with a hyphen('-')
     "hyphenated" AS (
         SELECT regexp_replace("value", '[^a-z0-9-]+', '-', 'gi') AS "value"
         FROM "removed_quotes"
     ),
     -- trims hyphens('-') if they exist on the head or tail of the string
     "trimmed" AS (
         SELECT regexp_replace(regexp_replace("value", '\-+$', ''), '^\-', '') AS "value"
         FROM "hyphenated"
     )
SELECT "value" FROM "trimmed";
$$ LANGUAGE SQL STRICT IMMUTABLE;


--
-- Function that creates a dashboard role name from dashboard_title and dashboard id
--
-- DROP FUNCTION create_dashboard_role_name;
CREATE OR REPLACE FUNCTION create_dashboard_role_name("dashboard_title" TEXT, "dashboard_id" INTEGER)
    RETURNS TEXT AS $$
SELECT 'D_' || slugify(left("dashboard_title", 50)) || '_' || "dashboard_id";
$$ LANGUAGE SQL STRICT IMMUTABLE;