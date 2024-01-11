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

-- public.logs definition

-- Drop table

-- DROP TABLE public.logs;

-- public.ab_user definition

-- Drop table

-- DROP TABLE public.ab_user;

CREATE TABLE public.ab_user (
                                id int4 NOT NULL,
                                first_name varchar(64) NOT NULL,
                                last_name varchar(64) NOT NULL,
                                username varchar(64) NOT NULL,
                                "password" varchar(256) NULL,
                                active bool NULL,
                                email varchar(64) NOT NULL,
                                last_login timestamp NULL,
                                login_count int4 NULL,
                                fail_login_count int4 NULL,
                                created_on timestamp NULL,
                                changed_on timestamp NULL,
                                created_by_fk int4 NULL,
                                changed_by_fk int4 NULL,
                                CONSTRAINT ab_user_email_key UNIQUE (email),
                                CONSTRAINT ab_user_pkey PRIMARY KEY (id),
                                CONSTRAINT ab_user_username_key UNIQUE (username),
                                CONSTRAINT ab_user_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id),
                                CONSTRAINT ab_user_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id)
);


CREATE TABLE public.logs (
                             id serial4 NOT NULL,
                             "action" varchar(512) NULL,
                             user_id int4 NULL,
                             "json" text NULL,
                             dttm timestamp NULL,
                             dashboard_id int4 NULL,
                             slice_id int4 NULL,
                             duration_ms int4 NULL,
                             referrer varchar(1024) NULL,
                             CONSTRAINT logs_pkey PRIMARY KEY (id)
);
-- CREATE INDEX ix_logs_user_id_dttm ON public.logs USING btree (user_id, dttm);


-- public.logs foreign keys

-- ALTER TABLE public.logs ADD CONSTRAINT logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);