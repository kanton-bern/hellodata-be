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

CREATE TABLE public.cb_auth_token (
                                      token_id varchar(128) NOT NULL,
                                      refresh_token_id varchar(128) NULL,
                                      session_id varchar(64) NOT NULL,
                                      user_id varchar(128) NULL,
                                      auth_role varchar(32) NULL,
                                      expiration_time timestamp NOT NULL,
                                      refresh_token_expiration_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      CONSTRAINT cb_auth_token_pkey PRIMARY KEY (token_id)
);


CREATE TABLE public.cb_auth_attempt (
                                        auth_id varchar(128) NOT NULL,
                                        auth_status varchar(32) NOT NULL,
                                        auth_error text NULL,
                                        app_session_id varchar(64) NOT NULL,
                                        session_id varchar(64) NULL,
                                        session_type varchar(64) NOT NULL,
                                        app_session_state text NOT NULL,
                                        create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        CONSTRAINT cb_auth_attempt_pkey PRIMARY KEY (auth_id)
);

CREATE TABLE public.cb_auth_attempt_info (
                                             auth_id varchar(128) NOT NULL,
                                             auth_provider_id varchar(128) NOT NULL,
                                             auth_provider_configuration_id varchar(128) NULL,
                                             auth_state text NOT NULL,
                                             create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             CONSTRAINT cb_auth_attempt_info_pkey PRIMARY KEY (auth_id,auth_provider_id)
);


CREATE TABLE public.cb_session (
                                   session_id varchar(64) NOT NULL,
                                   app_session_id varchar(64) NULL,
                                   user_id varchar(128) NULL,
                                   create_time timestamp NOT NULL,
                                   last_access_remote_address varchar(128) NULL,
                                   last_access_user_agent varchar(255) NULL,
                                   last_access_time timestamp NOT NULL,
                                   last_access_instance_id bpchar(36) NULL,
                                   session_type varchar(64) NULL,
                                   CONSTRAINT cb_session_pkey PRIMARY KEY (session_id)
);
