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
-- PostgreSQL database dump
--

-- Dumped from database version 14.10 (Debian 14.10-1.pgdg120+1)
-- Dumped by pg_dump version 15.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: superset_default_data_domain; Type: DATABASE; Schema: -; Owner: postgres
--

-- CREATE DATABASE superset_default_data_domain WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = libc LOCALE = 'en_US.utf8';


-- ALTER DATABASE superset_default_data_domain OWNER TO postgres;

\connect superset_default_data_domain

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- CREATE SCHEMA public;


-- ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

-- COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: emaildeliverytype; Type: TYPE; Schema: public; Owner: postgres
--
--
CREATE TYPE public.emaildeliverytype AS ENUM (
    'attachment',
    'inline'
);
--
--
ALTER TYPE public.emaildeliverytype OWNER TO postgres;
--
-- --
-- -- Name: objecttypes; Type: TYPE; Schema: public; Owner: postgres
-- --
--
CREATE TYPE public.objecttypes AS ENUM (
    'query',
    'chart',
    'dashboard',
    'dataset'
);
--
--
ALTER TYPE public.objecttypes OWNER TO postgres;
--
-- --
-- -- Name: sliceemailreportformat; Type: TYPE; Schema: public; Owner: postgres
-- --
--
CREATE TYPE public.sliceemailreportformat AS ENUM (
    'visualization',
    'data'
);
--
--
ALTER TYPE public.sliceemailreportformat OWNER TO postgres;
--
-- --
-- -- Name: tagtypes; Type: TYPE; Schema: public; Owner: postgres
-- --
--
CREATE TYPE public.tagtypes AS ENUM (
    'custom',
    'type',
    'owner',
    'favorited_by'
);


ALTER TYPE public.tagtypes OWNER TO postgres;

--
-- Name: create_dashboard_role_name(text, integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.create_dashboard_role_name(dashboard_title text, dashboard_id integer) RETURNS text
    LANGUAGE sql IMMUTABLE STRICT
    AS $$
SELECT 'D_' || slugify(left("dashboard_title", 50)) || '_' || "dashboard_id";
$$;


ALTER FUNCTION public.create_dashboard_role_name(dashboard_title text, dashboard_id integer) OWNER TO postgres;

--
-- Name: delete_dashboard_role(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.delete_dashboard_role() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    ab_role_id INTEGER;
BEGIN
    SELECT ab_role.id INTO STRICT ab_role_id FROM ab_role WHERE ab_role.name = create_dashboard_role_name(old.dashboard_title, old.id);
    DELETE FROM ab_user_role where ab_user_role.role_id = ab_role_id;
    DELETE FROM ab_role where ab_role.id = ab_role_id;
    RETURN NULL;
END;
$$;


ALTER FUNCTION public.delete_dashboard_role() OWNER TO postgres;

--
-- Name: insert_new_dashboard_role(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.insert_new_dashboard_role() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    new_role_id INTEGER := nextval('ab_role_id_seq');
BEGIN
    INSERT INTO
        ab_role(id, name)
    VALUES(new_role_id, create_dashboard_role_name(new.dashboard_title, new.id));

    INSERT INTO
        dashboard_roles(id, role_id, dashboard_id)
    VALUES(nextval('dashboard_roles_id_seq'), new_role_id, new.id);

    RETURN new;
END;
$$;


ALTER FUNCTION public.insert_new_dashboard_role() OWNER TO postgres;

--
-- Name: insert_owners_on_new_dashboard(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.insert_owners_on_new_dashboard() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN

    INSERT INTO dashboard_user(id, user_id, dashboard_id)

        SELECT
            nextval('dashboard_user_id_seq'),
            ab_user.id,
            new.id
        FROM
            ab_user
            LEFT JOIN ab_user_role ON ab_user.id = ab_user_role.user_id
            LEFT JOIN ab_role ON ab_role.id = ab_user_role.role_id
        WHERE
            ab_role.name = 'BI_EDITOR'
            AND ab_user.id <> new.created_by_fk;

    RETURN new;
END;
$$;


ALTER FUNCTION public.insert_owners_on_new_dashboard() OWNER TO postgres;

--
-- Name: insert_owners_on_new_dataset(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.insert_owners_on_new_dataset() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN

    INSERT INTO sqlatable_user(id, user_id, table_id)

    SELECT
        nextval('sqlatable_user_id_seq'),
        ab_user.id,
        new.id
    FROM
        ab_user
        LEFT JOIN ab_user_role ON ab_user.id = ab_user_role.user_id
        LEFT JOIN ab_role ON ab_role.id = ab_user_role.role_id
    WHERE
        ab_role.name = 'BI_EDITOR'
        AND ab_user.id <> new.created_by_fk;

    RETURN new;
END;
$$;


ALTER FUNCTION public.insert_owners_on_new_dataset() OWNER TO postgres;

--
-- Name: remove_deleted_bi_editor_as_owner(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.remove_deleted_bi_editor_as_owner() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    BI_EDITOR_role_id INTEGER := (SELECT id FROM ab_role WHERE name = 'BI_EDITOR');
BEGIN

    IF old.role_id = BI_EDITOR_role_id THEN
        --- Delete ownership of dashboards
        DELETE FROM dashboard_user WHERE user_id = old.user_id;
        --- Delete ownership of datasets
        DELETE FROM sqlatable_user WHERE user_id = old.user_id;
    END IF;

    RETURN old;
END;
$$;


ALTER FUNCTION public.remove_deleted_bi_editor_as_owner() OWNER TO postgres;

--
-- Name: remove_favstar(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.remove_favstar() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    DELETE FROM favstar WHERE
        user_id = old.user_id
        AND class_name = 'Dashboard'
        AND obj_id = (

            SELECT
                substring(ab_role.name from 'D_.*_([0-9]+)')::INTEGER
            FROM
                ab_user_role
                LEFT JOIN ab_role on ab_user_role.role_id = ab_role.id
            WHERE
                ab_role.name LIKE 'D_%'
                AND user_id = old.user_id
                AND role_id = old.role_id

            );

    RETURN old;
END;
$$;


ALTER FUNCTION public.remove_favstar() OWNER TO postgres;

--
-- Name: remove_favstar_2(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.remove_favstar_2() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN

    IF new.published = FALSE THEN
        DELETE FROM favstar
        WHERE
            obj_id = new.id
            AND class_name = 'Dashboard'
            AND user_id IN (

                SELECT
                    user_id
                FROM
                    ab_user_role
                    LEFT JOIN ab_role ON ab_role.id = ab_user_role.role_id
                WHERE
                    ab_role.name = 'BI_VIEWER'
            );

    END IF;

    RETURN new;
END;
$$;


ALTER FUNCTION public.remove_favstar_2() OWNER TO postgres;

--
-- Name: set_new_bi_editor_as_owner(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.set_new_bi_editor_as_owner() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    BI_EDITOR_role_id INTEGER := (SELECT id FROM ab_role WHERE name = 'BI_EDITOR');
BEGIN

    IF new.role_id = BI_EDITOR_role_id THEN
        --- Handle ownership of dashboards
        DELETE FROM dashboard_user WHERE user_id = new.user_id;
        INSERT INTO dashboard_user
        SELECT nextval('dashboard_user_id_seq'), new.user_id, dashboards.id from dashboards;

        --- Handle ownership of datasets
        DELETE FROM sqlatable_user WHERE user_id = new.user_id;
        INSERT INTO sqlatable_user
        SELECT nextval('sqlatable_user_id_seq'), new.user_id, tables.id from tables;
    END IF;

    RETURN new;
END;
$$;


ALTER FUNCTION public.set_new_bi_editor_as_owner() OWNER TO postgres;

--
-- Name: slugify(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.slugify(value text) RETURNS text
    LANGUAGE sql IMMUTABLE STRICT
    AS $_$
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
$_$;


ALTER FUNCTION public.slugify(value text) OWNER TO postgres;

--
-- Name: update_dashboard_role(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_dashboard_role() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN

    IF old.dashboard_title <> new.dashboard_title THEN
        UPDATE
            ab_role
        SET name = create_dashboard_role_name(new.dashboard_title, new.id)
        WHERE
            substring(ab_role.name from 'D_.*_([0-9]+)')::INTEGER = new.id;
    END IF;

    RETURN new;
END;
$$;


ALTER FUNCTION public.update_dashboard_role() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ab_permission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ab_permission (
    id integer NOT NULL,
    name character varying(100) NOT NULL
);


ALTER TABLE public.ab_permission OWNER TO postgres;

--
-- Name: ab_permission_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ab_permission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ab_permission_id_seq OWNER TO postgres;

--
-- Name: ab_permission_view; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ab_permission_view (
    id integer NOT NULL,
    permission_id integer,
    view_menu_id integer
);


ALTER TABLE public.ab_permission_view OWNER TO postgres;

--
-- Name: ab_permission_view_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ab_permission_view_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ab_permission_view_id_seq OWNER TO postgres;

--
-- Name: ab_permission_view_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ab_permission_view_role (
    id integer NOT NULL,
    permission_view_id integer,
    role_id integer
);


ALTER TABLE public.ab_permission_view_role OWNER TO postgres;

--
-- Name: ab_permission_view_role_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ab_permission_view_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ab_permission_view_role_id_seq OWNER TO postgres;

--
-- Name: ab_register_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ab_register_user (
    id integer NOT NULL,
    first_name character varying(64) NOT NULL,
    last_name character varying(64) NOT NULL,
    username character varying(64) NOT NULL,
    password character varying(256),
    email character varying(64) NOT NULL,
    registration_date timestamp without time zone,
    registration_hash character varying(256)
);


ALTER TABLE public.ab_register_user OWNER TO postgres;

--
-- Name: ab_register_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ab_register_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ab_register_user_id_seq OWNER TO postgres;

--
-- Name: ab_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ab_role (
    id integer NOT NULL,
    name character varying(64) NOT NULL
);


ALTER TABLE public.ab_role OWNER TO postgres;

--
-- Name: ab_role_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ab_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ab_role_id_seq OWNER TO postgres;

--
-- Name: ab_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ab_user (
    id integer NOT NULL,
    first_name character varying(64) NOT NULL,
    last_name character varying(64) NOT NULL,
    username character varying(64) NOT NULL,
    password character varying(256),
    active boolean,
    email character varying(64) NOT NULL,
    last_login timestamp without time zone,
    login_count integer,
    fail_login_count integer,
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.ab_user OWNER TO postgres;

--
-- Name: ab_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ab_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ab_user_id_seq OWNER TO postgres;

--
-- Name: ab_user_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ab_user_role (
    id integer NOT NULL,
    user_id integer,
    role_id integer
);


ALTER TABLE public.ab_user_role OWNER TO postgres;

--
-- Name: ab_user_role_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ab_user_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ab_user_role_id_seq OWNER TO postgres;

--
-- Name: ab_view_menu; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ab_view_menu (
    id integer NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.ab_view_menu OWNER TO postgres;

--
-- Name: ab_view_menu_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ab_view_menu_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ab_view_menu_id_seq OWNER TO postgres;

--
-- Name: access_request; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.access_request (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    datasource_type character varying(200),
    datasource_id integer,
    changed_by_fk integer,
    created_by_fk integer
);


ALTER TABLE public.access_request OWNER TO postgres;

--
-- Name: access_request_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.access_request_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.access_request_id_seq OWNER TO postgres;

--
-- Name: access_request_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.access_request_id_seq OWNED BY public.access_request.id;


--
-- Name: alembic_version; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.alembic_version (
    version_num character varying(32) NOT NULL
);


ALTER TABLE public.alembic_version OWNER TO postgres;

--
-- Name: alert_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.alert_logs (
    id integer NOT NULL,
    scheduled_dttm timestamp without time zone,
    dttm_start timestamp without time zone,
    dttm_end timestamp without time zone,
    alert_id integer,
    state character varying(10)
);


ALTER TABLE public.alert_logs OWNER TO postgres;

--
-- Name: alert_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.alert_logs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.alert_logs_id_seq OWNER TO postgres;

--
-- Name: alert_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.alert_logs_id_seq OWNED BY public.alert_logs.id;


--
-- Name: alert_owner; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.alert_owner (
    id integer NOT NULL,
    user_id integer,
    alert_id integer
);


ALTER TABLE public.alert_owner OWNER TO postgres;

--
-- Name: alert_owner_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.alert_owner_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.alert_owner_id_seq OWNER TO postgres;

--
-- Name: alert_owner_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.alert_owner_id_seq OWNED BY public.alert_owner.id;


--
-- Name: alerts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.alerts (
    id integer NOT NULL,
    label character varying(150) NOT NULL,
    active boolean,
    crontab character varying(50) NOT NULL,
    alert_type character varying(50),
    log_retention integer NOT NULL,
    grace_period integer NOT NULL,
    recipients text,
    slice_id integer,
    dashboard_id integer,
    last_eval_dttm timestamp without time zone,
    last_state character varying(10),
    slack_channel text,
    changed_by_fk integer,
    changed_on timestamp without time zone,
    created_by_fk integer,
    created_on timestamp without time zone,
    validator_config text,
    database_id integer NOT NULL,
    sql text NOT NULL,
    validator_type character varying(100) NOT NULL
);


ALTER TABLE public.alerts OWNER TO postgres;

--
-- Name: alerts_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.alerts_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.alerts_id_seq OWNER TO postgres;

--
-- Name: alerts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.alerts_id_seq OWNED BY public.alerts.id;


--
-- Name: annotation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.annotation (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    start_dttm timestamp without time zone,
    end_dttm timestamp without time zone,
    layer_id integer,
    short_descr character varying(500),
    long_descr text,
    changed_by_fk integer,
    created_by_fk integer,
    json_metadata text
);


ALTER TABLE public.annotation OWNER TO postgres;

--
-- Name: annotation_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.annotation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.annotation_id_seq OWNER TO postgres;

--
-- Name: annotation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.annotation_id_seq OWNED BY public.annotation.id;


--
-- Name: annotation_layer; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.annotation_layer (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    name character varying(250),
    descr text,
    changed_by_fk integer,
    created_by_fk integer
);


ALTER TABLE public.annotation_layer OWNER TO postgres;

--
-- Name: annotation_layer_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.annotation_layer_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.annotation_layer_id_seq OWNER TO postgres;

--
-- Name: annotation_layer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.annotation_layer_id_seq OWNED BY public.annotation_layer.id;


--
-- Name: cache_keys; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.cache_keys (
    id integer NOT NULL,
    cache_key character varying(256) NOT NULL,
    cache_timeout integer,
    datasource_uid character varying(64) NOT NULL,
    created_on timestamp without time zone
);


ALTER TABLE public.cache_keys OWNER TO postgres;

--
-- Name: cache_keys_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.cache_keys_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.cache_keys_id_seq OWNER TO postgres;

--
-- Name: cache_keys_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.cache_keys_id_seq OWNED BY public.cache_keys.id;


--
-- Name: clusters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.clusters (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    cluster_name character varying(250) NOT NULL,
    broker_host character varying(255),
    broker_port integer,
    broker_endpoint character varying(255),
    metadata_last_refreshed timestamp without time zone,
    created_by_fk integer,
    changed_by_fk integer,
    cache_timeout integer,
    verbose_name character varying(250),
    broker_pass bytea,
    broker_user character varying(255),
    uuid uuid
);


ALTER TABLE public.clusters OWNER TO postgres;

--
-- Name: clusters_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.clusters_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.clusters_id_seq OWNER TO postgres;

--
-- Name: clusters_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.clusters_id_seq OWNED BY public.clusters.id;


--
-- Name: columns; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.columns (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    column_name character varying(255) NOT NULL,
    is_active boolean,
    type character varying(32),
    groupby boolean,
    filterable boolean,
    description text,
    created_by_fk integer,
    changed_by_fk integer,
    dimension_spec_json text,
    verbose_name character varying(1024),
    datasource_id integer,
    uuid uuid,
    advanced_data_type character varying(255)
);


ALTER TABLE public.columns OWNER TO postgres;

--
-- Name: columns_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.columns_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.columns_id_seq OWNER TO postgres;

--
-- Name: columns_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.columns_id_seq OWNED BY public.columns.id;


--
-- Name: css_templates; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.css_templates (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    template_name character varying(250),
    css text,
    changed_by_fk integer,
    created_by_fk integer
);


ALTER TABLE public.css_templates OWNER TO postgres;

--
-- Name: css_templates_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.css_templates_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.css_templates_id_seq OWNER TO postgres;

--
-- Name: css_templates_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.css_templates_id_seq OWNED BY public.css_templates.id;


--
-- Name: dashboard_email_schedules; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.dashboard_email_schedules (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    active boolean,
    crontab character varying(50),
    recipients text,
    deliver_as_group boolean,
    delivery_type public.emaildeliverytype,
    dashboard_id integer,
    created_by_fk integer,
    changed_by_fk integer,
    user_id integer,
    slack_channel text,
    uuid uuid
);


ALTER TABLE public.dashboard_email_schedules OWNER TO postgres;

--
-- Name: dashboard_email_schedules_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.dashboard_email_schedules_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dashboard_email_schedules_id_seq OWNER TO postgres;

--
-- Name: dashboard_email_schedules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.dashboard_email_schedules_id_seq OWNED BY public.dashboard_email_schedules.id;


--
-- Name: dashboard_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.dashboard_roles (
    id integer NOT NULL,
    role_id integer NOT NULL,
    dashboard_id integer
);


ALTER TABLE public.dashboard_roles OWNER TO postgres;

--
-- Name: dashboard_roles_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.dashboard_roles_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dashboard_roles_id_seq OWNER TO postgres;

--
-- Name: dashboard_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.dashboard_roles_id_seq OWNED BY public.dashboard_roles.id;


--
-- Name: dashboard_slices; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.dashboard_slices (
    id integer NOT NULL,
    dashboard_id integer,
    slice_id integer
);


ALTER TABLE public.dashboard_slices OWNER TO postgres;

--
-- Name: dashboard_slices_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.dashboard_slices_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dashboard_slices_id_seq OWNER TO postgres;

--
-- Name: dashboard_slices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.dashboard_slices_id_seq OWNED BY public.dashboard_slices.id;


--
-- Name: dashboard_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.dashboard_user (
    id integer NOT NULL,
    user_id integer,
    dashboard_id integer
);


ALTER TABLE public.dashboard_user OWNER TO postgres;

--
-- Name: dashboard_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.dashboard_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dashboard_user_id_seq OWNER TO postgres;

--
-- Name: dashboard_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.dashboard_user_id_seq OWNED BY public.dashboard_user.id;


--
-- Name: dashboards; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.dashboards (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    dashboard_title character varying(500),
    position_json text,
    created_by_fk integer,
    changed_by_fk integer,
    css text,
    description text,
    slug character varying(255),
    json_metadata text,
    published boolean,
    uuid uuid,
    certified_by text,
    certification_details text,
    is_managed_externally boolean DEFAULT false NOT NULL,
    external_url text
);


ALTER TABLE public.dashboards OWNER TO postgres;

--
-- Name: dashboards_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.dashboards_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dashboards_id_seq OWNER TO postgres;

--
-- Name: dashboards_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.dashboards_id_seq OWNED BY public.dashboards.id;


--
-- Name: databasechangelog; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.databasechangelog (
    id character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    filename character varying(255) NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20),
    contexts character varying(255),
    labels character varying(255),
    deployment_id character varying(10)
);


ALTER TABLE public.databasechangelog OWNER TO postgres;

--
-- Name: databasechangeloglock; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255)
);


ALTER TABLE public.databasechangeloglock OWNER TO postgres;

--
-- Name: datasources; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.datasources (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    datasource_name character varying(255) NOT NULL,
    is_featured boolean,
    is_hidden boolean,
    description text,
    default_endpoint text,
    created_by_fk integer,
    changed_by_fk integer,
    "offset" integer,
    cache_timeout integer,
    perm character varying(1000),
    filter_select_enabled boolean,
    params character varying(1000),
    fetch_values_from character varying(100),
    schema_perm character varying(1000),
    cluster_id integer NOT NULL,
    uuid uuid,
    is_managed_externally boolean DEFAULT false NOT NULL,
    external_url text
);


ALTER TABLE public.datasources OWNER TO postgres;

--
-- Name: datasources_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.datasources_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datasources_id_seq OWNER TO postgres;

--
-- Name: datasources_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.datasources_id_seq OWNED BY public.datasources.id;


--
-- Name: dbs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.dbs (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    database_name character varying(250) NOT NULL,
    sqlalchemy_uri character varying(1024) NOT NULL,
    created_by_fk integer,
    changed_by_fk integer,
    password bytea,
    cache_timeout integer,
    extra text,
    select_as_create_table_as boolean,
    allow_ctas boolean,
    expose_in_sqllab boolean,
    force_ctas_schema character varying(250),
    allow_run_async boolean,
    allow_dml boolean,
    verbose_name character varying(250),
    impersonate_user boolean,
    allow_file_upload boolean DEFAULT true NOT NULL,
    encrypted_extra bytea,
    server_cert bytea,
    allow_cvas boolean,
    uuid uuid,
    configuration_method character varying(255) DEFAULT 'sqlalchemy_form'::character varying,
    is_managed_externally boolean DEFAULT false NOT NULL,
    external_url text
);


ALTER TABLE public.dbs OWNER TO postgres;

--
-- Name: dbs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.dbs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dbs_id_seq OWNER TO postgres;

--
-- Name: dbs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.dbs_id_seq OWNED BY public.dbs.id;


--
-- Name: druiddatasource_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.druiddatasource_user (
    id integer NOT NULL,
    user_id integer,
    datasource_id integer
);


ALTER TABLE public.druiddatasource_user OWNER TO postgres;

--
-- Name: druiddatasource_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.druiddatasource_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.druiddatasource_user_id_seq OWNER TO postgres;

--
-- Name: druiddatasource_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.druiddatasource_user_id_seq OWNED BY public.druiddatasource_user.id;


--
-- Name: dynamic_plugin; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.dynamic_plugin (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    name character varying(50) NOT NULL,
    key character varying(50) NOT NULL,
    bundle_url character varying(1000) NOT NULL,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.dynamic_plugin OWNER TO postgres;

--
-- Name: dynamic_plugin_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.dynamic_plugin_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dynamic_plugin_id_seq OWNER TO postgres;

--
-- Name: dynamic_plugin_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.dynamic_plugin_id_seq OWNED BY public.dynamic_plugin.id;


--
-- Name: embedded_dashboards; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.embedded_dashboards (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    allow_domain_list text,
    uuid uuid,
    dashboard_id integer NOT NULL,
    changed_by_fk integer,
    created_by_fk integer
);


ALTER TABLE public.embedded_dashboards OWNER TO postgres;

--
-- Name: favstar; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.favstar (
    id integer NOT NULL,
    user_id integer,
    class_name character varying(50),
    obj_id integer,
    dttm timestamp without time zone
);


ALTER TABLE public.favstar OWNER TO postgres;

--
-- Name: favstar_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.favstar_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.favstar_id_seq OWNER TO postgres;

--
-- Name: favstar_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.favstar_id_seq OWNED BY public.favstar.id;


--
-- Name: filter_sets; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.filter_sets (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    name character varying(500) NOT NULL,
    description text,
    json_metadata text NOT NULL,
    owner_id integer NOT NULL,
    owner_type character varying(255) NOT NULL,
    dashboard_id integer NOT NULL,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.filter_sets OWNER TO postgres;

--
-- Name: filter_sets_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.filter_sets_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.filter_sets_id_seq OWNER TO postgres;

--
-- Name: filter_sets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.filter_sets_id_seq OWNED BY public.filter_sets.id;


--
-- Name: key_value; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.key_value (
    id integer NOT NULL,
    resource character varying(32) NOT NULL,
    value bytea NOT NULL,
    uuid uuid,
    created_on timestamp without time zone,
    created_by_fk integer,
    changed_on timestamp without time zone,
    changed_by_fk integer,
    expires_on timestamp without time zone
);


ALTER TABLE public.key_value OWNER TO postgres;

--
-- Name: key_value_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.key_value_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.key_value_id_seq OWNER TO postgres;

--
-- Name: key_value_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.key_value_id_seq OWNED BY public.key_value.id;


--
-- Name: keyvalue; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.keyvalue (
    id integer NOT NULL,
    value text NOT NULL
);


ALTER TABLE public.keyvalue OWNER TO postgres;

--
-- Name: keyvalue_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.keyvalue_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.keyvalue_id_seq OWNER TO postgres;

--
-- Name: keyvalue_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.keyvalue_id_seq OWNED BY public.keyvalue.id;


--
-- Name: logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.logs (
    id integer NOT NULL,
    action character varying(512),
    user_id integer,
    json text,
    dttm timestamp without time zone,
    dashboard_id integer,
    slice_id integer,
    duration_ms integer,
    referrer character varying(1024)
);


ALTER TABLE public.logs OWNER TO postgres;

--
-- Name: logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.logs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.logs_id_seq OWNER TO postgres;

--
-- Name: logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.logs_id_seq OWNED BY public.logs.id;


--
-- Name: metrics; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.metrics (
    id integer NOT NULL,
    metric_name character varying(255) NOT NULL,
    verbose_name character varying(1024),
    metric_type character varying(32),
    json text NOT NULL,
    description text,
    changed_by_fk integer,
    changed_on timestamp without time zone,
    created_by_fk integer,
    created_on timestamp without time zone,
    d3format character varying(128),
    warning_text text,
    datasource_id integer,
    uuid uuid
);


ALTER TABLE public.metrics OWNER TO postgres;

--
-- Name: metrics_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.metrics_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.metrics_id_seq OWNER TO postgres;

--
-- Name: metrics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.metrics_id_seq OWNED BY public.metrics.id;


--
-- Name: query; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.query (
    id integer NOT NULL,
    client_id character varying(11) NOT NULL,
    database_id integer NOT NULL,
    tmp_table_name character varying(256),
    tab_name character varying(256),
    sql_editor_id character varying(256),
    user_id integer,
    status character varying(16),
    schema character varying(256),
    sql text,
    select_sql text,
    executed_sql text,
    "limit" integer,
    select_as_cta boolean,
    select_as_cta_used boolean,
    progress integer,
    rows integer,
    error_message text,
    start_time numeric(20,6),
    changed_on timestamp without time zone,
    end_time numeric(20,6),
    results_key character varying(64),
    start_running_time numeric(20,6),
    end_result_backend_time numeric(20,6),
    tracking_url text,
    extra_json text,
    tmp_schema_name character varying(256),
    ctas_method character varying(16),
    limiting_factor character varying(255) DEFAULT 'UNKNOWN'::character varying
);


ALTER TABLE public.query OWNER TO postgres;

--
-- Name: query_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.query_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.query_id_seq OWNER TO postgres;

--
-- Name: query_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.query_id_seq OWNED BY public.query.id;


--
-- Name: report_execution_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.report_execution_log (
    id integer NOT NULL,
    scheduled_dttm timestamp without time zone NOT NULL,
    start_dttm timestamp without time zone,
    end_dttm timestamp without time zone,
    value double precision,
    value_row_json text,
    state character varying(50) NOT NULL,
    error_message text,
    report_schedule_id integer NOT NULL,
    uuid uuid
);


ALTER TABLE public.report_execution_log OWNER TO postgres;

--
-- Name: report_execution_log_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.report_execution_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.report_execution_log_id_seq OWNER TO postgres;

--
-- Name: report_execution_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.report_execution_log_id_seq OWNED BY public.report_execution_log.id;


--
-- Name: report_recipient; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.report_recipient (
    id integer NOT NULL,
    type character varying(50) NOT NULL,
    recipient_config_json text,
    report_schedule_id integer NOT NULL,
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.report_recipient OWNER TO postgres;

--
-- Name: report_recipient_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.report_recipient_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.report_recipient_id_seq OWNER TO postgres;

--
-- Name: report_recipient_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.report_recipient_id_seq OWNED BY public.report_recipient.id;


--
-- Name: report_schedule; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.report_schedule (
    id integer NOT NULL,
    type character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    context_markdown text,
    active boolean,
    crontab character varying(1000) NOT NULL,
    sql text,
    chart_id integer,
    dashboard_id integer,
    database_id integer,
    last_eval_dttm timestamp without time zone,
    last_state character varying(50),
    last_value double precision,
    last_value_row_json text,
    validator_type character varying(100),
    validator_config_json text,
    log_retention integer,
    grace_period integer,
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    created_by_fk integer,
    changed_by_fk integer,
    working_timeout integer,
    report_format character varying(50) DEFAULT 'PNG'::character varying,
    creation_method character varying(255) DEFAULT 'alerts_reports'::character varying,
    timezone character varying(100) DEFAULT 'UTC'::character varying NOT NULL,
    extra_json text NOT NULL,
    force_screenshot boolean
);


ALTER TABLE public.report_schedule OWNER TO postgres;

--
-- Name: report_schedule_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.report_schedule_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.report_schedule_id_seq OWNER TO postgres;

--
-- Name: report_schedule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.report_schedule_id_seq OWNED BY public.report_schedule.id;


--
-- Name: report_schedule_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.report_schedule_user (
    id integer NOT NULL,
    user_id integer NOT NULL,
    report_schedule_id integer NOT NULL
);


ALTER TABLE public.report_schedule_user OWNER TO postgres;

--
-- Name: report_schedule_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.report_schedule_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.report_schedule_user_id_seq OWNER TO postgres;

--
-- Name: report_schedule_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.report_schedule_user_id_seq OWNED BY public.report_schedule_user.id;


--
-- Name: rls_filter_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rls_filter_roles (
    id integer NOT NULL,
    role_id integer NOT NULL,
    rls_filter_id integer
);


ALTER TABLE public.rls_filter_roles OWNER TO postgres;

--
-- Name: rls_filter_roles_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.rls_filter_roles_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.rls_filter_roles_id_seq OWNER TO postgres;

--
-- Name: rls_filter_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.rls_filter_roles_id_seq OWNED BY public.rls_filter_roles.id;


--
-- Name: rls_filter_tables; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rls_filter_tables (
    id integer NOT NULL,
    table_id integer,
    rls_filter_id integer
);


ALTER TABLE public.rls_filter_tables OWNER TO postgres;

--
-- Name: rls_filter_tables_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.rls_filter_tables_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.rls_filter_tables_id_seq OWNER TO postgres;

--
-- Name: rls_filter_tables_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.rls_filter_tables_id_seq OWNED BY public.rls_filter_tables.id;


--
-- Name: row_level_security_filters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.row_level_security_filters (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    clause text NOT NULL,
    created_by_fk integer,
    changed_by_fk integer,
    filter_type character varying(255),
    group_key character varying(255),
    name character varying(255) NOT NULL,
    description text
);


ALTER TABLE public.row_level_security_filters OWNER TO postgres;

--
-- Name: row_level_security_filters_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.row_level_security_filters_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.row_level_security_filters_id_seq OWNER TO postgres;

--
-- Name: row_level_security_filters_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.row_level_security_filters_id_seq OWNED BY public.row_level_security_filters.id;


--
-- Name: saved_query; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.saved_query (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    user_id integer,
    db_id integer,
    label character varying(256),
    schema character varying(128),
    sql text,
    description text,
    changed_by_fk integer,
    created_by_fk integer,
    extra_json text,
    last_run timestamp without time zone,
    rows integer,
    uuid uuid,
    template_parameters text
);


ALTER TABLE public.saved_query OWNER TO postgres;

--
-- Name: saved_query_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.saved_query_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.saved_query_id_seq OWNER TO postgres;

--
-- Name: saved_query_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.saved_query_id_seq OWNED BY public.saved_query.id;


--
-- Name: sl_columns; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sl_columns (
    uuid uuid,
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    is_aggregation boolean NOT NULL,
    is_additive boolean NOT NULL,
    is_dimensional boolean NOT NULL,
    is_filterable boolean NOT NULL,
    is_increase_desired boolean NOT NULL,
    is_managed_externally boolean NOT NULL,
    is_partition boolean NOT NULL,
    is_physical boolean NOT NULL,
    is_temporal boolean NOT NULL,
    is_spatial boolean NOT NULL,
    name text,
    type text,
    unit text,
    expression text,
    description text,
    warning_text text,
    external_url text,
    extra_json text,
    created_by_fk integer,
    changed_by_fk integer,
    advanced_data_type text
);


ALTER TABLE public.sl_columns OWNER TO postgres;

--
-- Name: sl_columns_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sl_columns_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sl_columns_id_seq OWNER TO postgres;

--
-- Name: sl_columns_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sl_columns_id_seq OWNED BY public.sl_columns.id;


--
-- Name: sl_dataset_columns; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sl_dataset_columns (
    dataset_id integer NOT NULL,
    column_id integer NOT NULL
);


ALTER TABLE public.sl_dataset_columns OWNER TO postgres;

--
-- Name: sl_dataset_tables; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sl_dataset_tables (
    dataset_id integer NOT NULL,
    table_id integer NOT NULL
);


ALTER TABLE public.sl_dataset_tables OWNER TO postgres;

--
-- Name: sl_dataset_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sl_dataset_users (
    dataset_id integer NOT NULL,
    user_id integer NOT NULL
);


ALTER TABLE public.sl_dataset_users OWNER TO postgres;

--
-- Name: sl_datasets; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sl_datasets (
    uuid uuid,
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    database_id integer NOT NULL,
    is_physical boolean,
    is_managed_externally boolean NOT NULL,
    name text,
    expression text,
    external_url text,
    extra_json text,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.sl_datasets OWNER TO postgres;

--
-- Name: sl_datasets_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sl_datasets_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sl_datasets_id_seq OWNER TO postgres;

--
-- Name: sl_datasets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sl_datasets_id_seq OWNED BY public.sl_datasets.id;


--
-- Name: sl_table_columns; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sl_table_columns (
    table_id integer NOT NULL,
    column_id integer NOT NULL
);


ALTER TABLE public.sl_table_columns OWNER TO postgres;

--
-- Name: sl_tables; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sl_tables (
    uuid uuid,
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    database_id integer NOT NULL,
    is_managed_externally boolean NOT NULL,
    catalog text,
    schema text,
    name text,
    external_url text,
    extra_json text,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.sl_tables OWNER TO postgres;

--
-- Name: sl_tables_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sl_tables_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sl_tables_id_seq OWNER TO postgres;

--
-- Name: sl_tables_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sl_tables_id_seq OWNED BY public.sl_tables.id;


--
-- Name: slice_email_schedules; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.slice_email_schedules (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    active boolean,
    crontab character varying(50),
    recipients text,
    deliver_as_group boolean,
    delivery_type public.emaildeliverytype,
    slice_id integer,
    email_format public.sliceemailreportformat,
    created_by_fk integer,
    changed_by_fk integer,
    user_id integer,
    slack_channel text,
    uuid uuid
);


ALTER TABLE public.slice_email_schedules OWNER TO postgres;

--
-- Name: slice_email_schedules_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.slice_email_schedules_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.slice_email_schedules_id_seq OWNER TO postgres;

--
-- Name: slice_email_schedules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.slice_email_schedules_id_seq OWNED BY public.slice_email_schedules.id;


--
-- Name: slice_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.slice_user (
    id integer NOT NULL,
    user_id integer,
    slice_id integer
);


ALTER TABLE public.slice_user OWNER TO postgres;

--
-- Name: slice_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.slice_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.slice_user_id_seq OWNER TO postgres;

--
-- Name: slice_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.slice_user_id_seq OWNED BY public.slice_user.id;


--
-- Name: slices; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.slices (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    slice_name character varying(250),
    datasource_type character varying(200),
    datasource_name character varying(2000),
    viz_type character varying(250),
    params text,
    created_by_fk integer,
    changed_by_fk integer,
    description text,
    cache_timeout integer,
    perm character varying(2000),
    datasource_id integer,
    schema_perm character varying(1000),
    uuid uuid,
    query_context text,
    last_saved_at timestamp without time zone,
    last_saved_by_fk integer,
    certified_by text,
    certification_details text,
    is_managed_externally boolean DEFAULT false NOT NULL,
    external_url text
);


ALTER TABLE public.slices OWNER TO postgres;

--
-- Name: slices_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.slices_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.slices_id_seq OWNER TO postgres;

--
-- Name: slices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.slices_id_seq OWNED BY public.slices.id;


--
-- Name: sql_metrics; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sql_metrics (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    metric_name character varying(255) NOT NULL,
    verbose_name character varying(1024),
    metric_type character varying(32),
    table_id integer,
    expression text NOT NULL,
    description text,
    created_by_fk integer,
    changed_by_fk integer,
    d3format character varying(128),
    warning_text text,
    extra text,
    uuid uuid
);


ALTER TABLE public.sql_metrics OWNER TO postgres;

--
-- Name: sql_metrics_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sql_metrics_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sql_metrics_id_seq OWNER TO postgres;

--
-- Name: sql_metrics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sql_metrics_id_seq OWNED BY public.sql_metrics.id;


--
-- Name: sql_observations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sql_observations (
    id integer NOT NULL,
    dttm timestamp without time zone,
    alert_id integer,
    value double precision,
    error_msg character varying(500)
);


ALTER TABLE public.sql_observations OWNER TO postgres;

--
-- Name: sql_observations_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sql_observations_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sql_observations_id_seq OWNER TO postgres;

--
-- Name: sql_observations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sql_observations_id_seq OWNED BY public.sql_observations.id;


--
-- Name: sqlatable_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sqlatable_user (
    id integer NOT NULL,
    user_id integer,
    table_id integer
);


ALTER TABLE public.sqlatable_user OWNER TO postgres;

--
-- Name: sqlatable_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sqlatable_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sqlatable_user_id_seq OWNER TO postgres;

--
-- Name: sqlatable_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sqlatable_user_id_seq OWNED BY public.sqlatable_user.id;


--
-- Name: ssh_tunnels; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ssh_tunnels (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    created_by_fk integer,
    changed_by_fk integer,
    extra_json text,
    uuid uuid,
    id integer NOT NULL,
    database_id integer,
    server_address character varying(256),
    server_port integer,
    username bytea,
    password bytea,
    private_key bytea,
    private_key_password bytea
);


ALTER TABLE public.ssh_tunnels OWNER TO postgres;

--
-- Name: ssh_tunnels_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ssh_tunnels_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ssh_tunnels_id_seq OWNER TO postgres;

--
-- Name: ssh_tunnels_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ssh_tunnels_id_seq OWNED BY public.ssh_tunnels.id;


--
-- Name: tab_state; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tab_state (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    extra_json text,
    id integer NOT NULL,
    user_id integer,
    label character varying(256),
    active boolean,
    database_id integer,
    schema character varying(256),
    sql text,
    query_limit integer,
    latest_query_id character varying(11),
    autorun boolean NOT NULL,
    template_params text,
    created_by_fk integer,
    changed_by_fk integer,
    hide_left_bar boolean DEFAULT false NOT NULL,
    saved_query_id integer
);


ALTER TABLE public.tab_state OWNER TO postgres;

--
-- Name: tab_state_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tab_state_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tab_state_id_seq OWNER TO postgres;

--
-- Name: tab_state_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tab_state_id_seq OWNED BY public.tab_state.id;


--
-- Name: table_columns; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.table_columns (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    table_id integer,
    column_name character varying(255) NOT NULL,
    is_dttm boolean,
    is_active boolean,
    type text,
    groupby boolean,
    filterable boolean,
    description text,
    created_by_fk integer,
    changed_by_fk integer,
    expression text,
    verbose_name character varying(1024),
    python_date_format character varying(255),
    uuid uuid,
    extra text,
    advanced_data_type character varying(255)
);


ALTER TABLE public.table_columns OWNER TO postgres;

--
-- Name: table_columns_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.table_columns_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.table_columns_id_seq OWNER TO postgres;

--
-- Name: table_columns_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.table_columns_id_seq OWNED BY public.table_columns.id;


--
-- Name: table_schema; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.table_schema (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    extra_json text,
    id integer NOT NULL,
    tab_state_id integer,
    database_id integer NOT NULL,
    schema character varying(256),
    "table" character varying(256),
    description text,
    expanded boolean,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.table_schema OWNER TO postgres;

--
-- Name: table_schema_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.table_schema_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.table_schema_id_seq OWNER TO postgres;

--
-- Name: table_schema_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.table_schema_id_seq OWNED BY public.table_schema.id;


--
-- Name: tables; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tables (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    table_name character varying(250) NOT NULL,
    main_dttm_col character varying(250),
    default_endpoint text,
    database_id integer NOT NULL,
    created_by_fk integer,
    changed_by_fk integer,
    "offset" integer,
    description text,
    is_featured boolean,
    cache_timeout integer,
    schema character varying(255),
    sql text,
    params text,
    perm character varying(1000),
    filter_select_enabled boolean,
    fetch_values_predicate text,
    is_sqllab_view boolean DEFAULT false,
    template_params text,
    schema_perm character varying(1000),
    extra text,
    uuid uuid,
    is_managed_externally boolean DEFAULT false NOT NULL,
    external_url text
);


ALTER TABLE public.tables OWNER TO postgres;

--
-- Name: tables_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tables_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tables_id_seq OWNER TO postgres;

--
-- Name: tables_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tables_id_seq OWNED BY public.tables.id;


--
-- Name: tag; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tag (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    name character varying(250),
    type public.tagtypes,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.tag OWNER TO postgres;

--
-- Name: tag_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tag_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tag_id_seq OWNER TO postgres;

--
-- Name: tag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tag_id_seq OWNED BY public.tag.id;


--
-- Name: tagged_object; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tagged_object (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    tag_id integer,
    object_id integer,
    object_type public.objecttypes,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.tagged_object OWNER TO postgres;

--
-- Name: tagged_object_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tagged_object_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tagged_object_id_seq OWNER TO postgres;

--
-- Name: tagged_object_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tagged_object_id_seq OWNED BY public.tagged_object.id;


--
-- Name: url; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.url (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    url text,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.url OWNER TO postgres;

--
-- Name: url_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.url_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.url_id_seq OWNER TO postgres;

--
-- Name: url_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.url_id_seq OWNED BY public.url.id;


--
-- Name: user_attribute; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_attribute (
    created_on timestamp without time zone,
    changed_on timestamp without time zone,
    id integer NOT NULL,
    user_id integer,
    welcome_dashboard_id integer,
    created_by_fk integer,
    changed_by_fk integer
);


ALTER TABLE public.user_attribute OWNER TO postgres;

--
-- Name: user_attribute_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_attribute_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_attribute_id_seq OWNER TO postgres;

--
-- Name: user_attribute_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_attribute_id_seq OWNED BY public.user_attribute.id;


--
-- Name: access_request id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.access_request ALTER COLUMN id SET DEFAULT nextval('public.access_request_id_seq'::regclass);


--
-- Name: alert_logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alert_logs ALTER COLUMN id SET DEFAULT nextval('public.alert_logs_id_seq'::regclass);


--
-- Name: alert_owner id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alert_owner ALTER COLUMN id SET DEFAULT nextval('public.alert_owner_id_seq'::regclass);


--
-- Name: alerts id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alerts ALTER COLUMN id SET DEFAULT nextval('public.alerts_id_seq'::regclass);


--
-- Name: annotation id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.annotation ALTER COLUMN id SET DEFAULT nextval('public.annotation_id_seq'::regclass);


--
-- Name: annotation_layer id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.annotation_layer ALTER COLUMN id SET DEFAULT nextval('public.annotation_layer_id_seq'::regclass);


--
-- Name: cache_keys id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cache_keys ALTER COLUMN id SET DEFAULT nextval('public.cache_keys_id_seq'::regclass);


--
-- Name: clusters id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clusters ALTER COLUMN id SET DEFAULT nextval('public.clusters_id_seq'::regclass);


--
-- Name: columns id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.columns ALTER COLUMN id SET DEFAULT nextval('public.columns_id_seq'::regclass);


--
-- Name: css_templates id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.css_templates ALTER COLUMN id SET DEFAULT nextval('public.css_templates_id_seq'::regclass);


--
-- Name: dashboard_email_schedules id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_email_schedules ALTER COLUMN id SET DEFAULT nextval('public.dashboard_email_schedules_id_seq'::regclass);


--
-- Name: dashboard_roles id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_roles ALTER COLUMN id SET DEFAULT nextval('public.dashboard_roles_id_seq'::regclass);


--
-- Name: dashboard_slices id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_slices ALTER COLUMN id SET DEFAULT nextval('public.dashboard_slices_id_seq'::regclass);


--
-- Name: dashboard_user id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_user ALTER COLUMN id SET DEFAULT nextval('public.dashboard_user_id_seq'::regclass);


--
-- Name: dashboards id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboards ALTER COLUMN id SET DEFAULT nextval('public.dashboards_id_seq'::regclass);


--
-- Name: datasources id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datasources ALTER COLUMN id SET DEFAULT nextval('public.datasources_id_seq'::regclass);


--
-- Name: dbs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dbs ALTER COLUMN id SET DEFAULT nextval('public.dbs_id_seq'::regclass);


--
-- Name: druiddatasource_user id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.druiddatasource_user ALTER COLUMN id SET DEFAULT nextval('public.druiddatasource_user_id_seq'::regclass);


--
-- Name: dynamic_plugin id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dynamic_plugin ALTER COLUMN id SET DEFAULT nextval('public.dynamic_plugin_id_seq'::regclass);


--
-- Name: favstar id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.favstar ALTER COLUMN id SET DEFAULT nextval('public.favstar_id_seq'::regclass);


--
-- Name: filter_sets id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.filter_sets ALTER COLUMN id SET DEFAULT nextval('public.filter_sets_id_seq'::regclass);


--
-- Name: key_value id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.key_value ALTER COLUMN id SET DEFAULT nextval('public.key_value_id_seq'::regclass);


--
-- Name: keyvalue id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.keyvalue ALTER COLUMN id SET DEFAULT nextval('public.keyvalue_id_seq'::regclass);


--
-- Name: logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logs ALTER COLUMN id SET DEFAULT nextval('public.logs_id_seq'::regclass);


--
-- Name: metrics id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.metrics ALTER COLUMN id SET DEFAULT nextval('public.metrics_id_seq'::regclass);


--
-- Name: query id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.query ALTER COLUMN id SET DEFAULT nextval('public.query_id_seq'::regclass);


--
-- Name: report_execution_log id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_execution_log ALTER COLUMN id SET DEFAULT nextval('public.report_execution_log_id_seq'::regclass);


--
-- Name: report_recipient id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_recipient ALTER COLUMN id SET DEFAULT nextval('public.report_recipient_id_seq'::regclass);


--
-- Name: report_schedule id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule ALTER COLUMN id SET DEFAULT nextval('public.report_schedule_id_seq'::regclass);


--
-- Name: report_schedule_user id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule_user ALTER COLUMN id SET DEFAULT nextval('public.report_schedule_user_id_seq'::regclass);


--
-- Name: rls_filter_roles id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rls_filter_roles ALTER COLUMN id SET DEFAULT nextval('public.rls_filter_roles_id_seq'::regclass);


--
-- Name: rls_filter_tables id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rls_filter_tables ALTER COLUMN id SET DEFAULT nextval('public.rls_filter_tables_id_seq'::regclass);


--
-- Name: row_level_security_filters id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.row_level_security_filters ALTER COLUMN id SET DEFAULT nextval('public.row_level_security_filters_id_seq'::regclass);


--
-- Name: saved_query id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.saved_query ALTER COLUMN id SET DEFAULT nextval('public.saved_query_id_seq'::regclass);


--
-- Name: sl_columns id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_columns ALTER COLUMN id SET DEFAULT nextval('public.sl_columns_id_seq'::regclass);


--
-- Name: sl_datasets id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_datasets ALTER COLUMN id SET DEFAULT nextval('public.sl_datasets_id_seq'::regclass);


--
-- Name: sl_tables id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_tables ALTER COLUMN id SET DEFAULT nextval('public.sl_tables_id_seq'::regclass);


--
-- Name: slice_email_schedules id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_email_schedules ALTER COLUMN id SET DEFAULT nextval('public.slice_email_schedules_id_seq'::regclass);


--
-- Name: slice_user id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_user ALTER COLUMN id SET DEFAULT nextval('public.slice_user_id_seq'::regclass);


--
-- Name: slices id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slices ALTER COLUMN id SET DEFAULT nextval('public.slices_id_seq'::regclass);


--
-- Name: sql_metrics id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sql_metrics ALTER COLUMN id SET DEFAULT nextval('public.sql_metrics_id_seq'::regclass);


--
-- Name: sql_observations id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sql_observations ALTER COLUMN id SET DEFAULT nextval('public.sql_observations_id_seq'::regclass);


--
-- Name: sqlatable_user id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sqlatable_user ALTER COLUMN id SET DEFAULT nextval('public.sqlatable_user_id_seq'::regclass);


--
-- Name: ssh_tunnels id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ssh_tunnels ALTER COLUMN id SET DEFAULT nextval('public.ssh_tunnels_id_seq'::regclass);


--
-- Name: tab_state id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tab_state ALTER COLUMN id SET DEFAULT nextval('public.tab_state_id_seq'::regclass);


--
-- Name: table_columns id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_columns ALTER COLUMN id SET DEFAULT nextval('public.table_columns_id_seq'::regclass);


--
-- Name: table_schema id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_schema ALTER COLUMN id SET DEFAULT nextval('public.table_schema_id_seq'::regclass);


--
-- Name: tables id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tables ALTER COLUMN id SET DEFAULT nextval('public.tables_id_seq'::regclass);


--
-- Name: tag id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tag ALTER COLUMN id SET DEFAULT nextval('public.tag_id_seq'::regclass);


--
-- Name: tagged_object id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tagged_object ALTER COLUMN id SET DEFAULT nextval('public.tagged_object_id_seq'::regclass);


--
-- Name: url id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.url ALTER COLUMN id SET DEFAULT nextval('public.url_id_seq'::regclass);


--
-- Name: user_attribute id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_attribute ALTER COLUMN id SET DEFAULT nextval('public.user_attribute_id_seq'::regclass);


--
-- Data for Name: ab_permission; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ab_permission (id, name) FROM stdin;
1	can_read
2	can_write
3	can_this_form_post
4	can_this_form_get
5	can_edit
6	can_add
7	can_userinfo
8	can_list
9	can_delete
10	can_show
11	userinfoedit
12	copyrole
13	can_get
14	can_info
15	can_add_role_permissions
16	can_put
17	can_post
18	can_list_role_permissions
19	can_invalidate
20	can_export
21	can_set_embedded
22	can_get_embedded
23	can_delete_embedded
24	can_get_or_create_dataset
25	can_duplicate
26	can_get_column_values
27	can_import_
28	can_export_csv
29	can_get_results
30	can_execute_sql_query
31	can_download
32	muldelete
33	can_query
34	can_time_range
35	can_query_form_data
36	can_save
37	can_external_metadata
38	can_external_metadata_by_name
39	can_samples
40	can_get_value
41	can_store
42	can_my_queries
43	can_log
44	can_save_dash
45	can_annotation_json
46	can_sqllab_table_viz
47	can_fave_dashboards
48	can_created_dashboards
49	can_request_access
50	can_csv
51	can_dashboard
52	can_recent_activity
53	can_fave_dashboards_by_username
54	can_filter
55	can_user_slices
56	can_created_slices
57	can_extra_table_metadata
58	can_dashboard_permalink
59	can_add_slices
60	can_slice
61	can_explore
62	can_estimate_query_cost
63	can_fetch_datasource_metadata
64	can_sql_json
65	can_sqllab_viz
66	can_testconn
67	can_copy_dash
68	can_import_dashboards
69	can_queries
70	can_validate_sql_json
71	can_datasources
72	can_slice_json
73	can_sqllab_history
74	can_sqllab
75	can_tables
76	can_available_domains
77	can_fave_slices
78	can_favstar
79	can_search_queries
80	can_override_role_permissions
81	can_warm_up_cache
82	can_stop_query
83	can_explore_json
84	can_profile
85	can_results
86	can_schemas_access_for_file_upload
87	can_approve
88	can_expanded
89	can_delete_query
90	can_activate
91	can_migrate_query
92	can_tagged_objects
93	can_suggestions
94	can_grant_guest_token
95	menu_access
96	all_datasource_access
97	all_database_access
98	all_query_access
99	can_share_dashboard
100	can_share_chart
101	database_access
102	datasource_access
103	schema_access
\.


--
-- Data for Name: ab_permission_view; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ab_permission_view (id, permission_id, view_menu_id) FROM stdin;
1	1	1
2	2	1
3	1	2
4	2	2
5	1	3
6	2	3
7	1	4
8	2	4
9	1	5
10	2	5
11	1	6
12	2	6
13	1	7
14	2	7
15	1	8
16	2	8
17	1	9
18	2	9
19	1	10
20	3	16
21	4	16
22	3	17
23	4	17
24	3	18
25	4	18
26	5	20
27	6	20
28	7	20
29	8	20
30	9	20
31	10	20
32	11	20
33	5	21
34	6	21
35	8	21
36	9	21
37	10	21
38	12	21
39	8	22
40	10	22
41	9	22
42	13	23
43	14	23
44	13	24
45	14	24
46	15	24
47	16	24
48	17	24
49	9	24
50	18	24
51	13	25
52	14	25
53	16	25
54	17	25
55	9	25
56	13	26
57	14	26
58	16	26
59	17	26
60	9	26
61	13	27
62	14	27
63	16	27
64	17	27
65	9	27
66	13	28
67	10	29
68	13	30
69	8	31
70	1	32
71	1	33
72	19	34
73	20	4
74	2	36
75	1	36
76	2	37
77	1	37
78	21	8
79	20	8
80	22	8
81	23	8
82	20	9
83	24	6
84	25	6
85	20	6
86	26	38
87	1	39
88	1	40
89	2	41
90	1	41
91	2	42
92	1	42
93	8	43
94	5	43
95	6	43
96	9	43
97	20	44
98	27	44
99	20	1
100	28	45
101	29	45
102	30	45
103	2	46
104	5	46
105	6	46
106	8	46
107	9	46
108	10	46
109	31	46
110	5	47
111	6	47
112	8	47
113	9	47
114	10	47
115	31	47
116	32	47
117	33	48
118	34	48
119	35	48
120	3	49
121	4	49
122	3	50
123	4	50
124	3	51
125	4	51
126	36	38
127	13	38
128	37	38
129	38	38
130	39	38
131	40	53
132	41	53
133	42	55
134	43	56
135	44	56
136	45	56
137	46	56
138	47	56
139	48	56
140	49	56
141	50	56
142	51	56
143	52	56
144	53	56
145	54	56
146	55	56
147	56	56
148	57	56
149	58	56
150	59	56
151	60	56
152	61	56
153	62	56
154	63	56
155	64	56
156	65	56
157	66	56
158	67	56
159	68	56
160	69	56
161	70	56
162	71	56
163	72	56
164	73	56
165	74	56
166	75	56
167	76	56
168	77	56
169	78	56
170	79	56
171	80	56
172	81	56
173	82	56
174	83	56
175	84	56
176	85	56
177	86	56
178	87	56
179	88	57
180	17	57
181	9	57
182	89	58
183	13	58
184	90	58
185	16	58
186	17	58
187	9	58
188	91	58
189	13	59
190	92	59
191	93	59
192	17	59
193	9	59
194	52	7
195	1	60
196	94	60
197	5	61
198	6	61
199	8	61
200	9	61
201	10	61
202	32	61
203	95	62
204	95	63
205	95	64
206	95	65
207	95	66
208	95	67
209	95	68
210	95	69
211	95	70
212	95	71
213	95	72
214	95	73
215	95	74
216	95	75
217	95	76
218	95	77
219	95	78
220	95	79
221	95	80
222	95	81
223	95	82
224	95	83
225	95	84
226	96	85
227	97	86
228	98	87
229	99	56
230	100	56
231	101	88
232	102	89
233	103	90
234	102	91
235	102	92
236	102	93
237	102	94
238	102	95
\.


--
-- Data for Name: ab_permission_view_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ab_permission_view_role (id, permission_view_id, role_id) FROM stdin;
1	20	1
2	21	1
3	22	1
4	23	1
5	24	1
6	25	1
7	26	1
8	27	1
9	28	1
10	29	1
11	30	1
12	31	1
13	32	1
14	33	1
15	34	1
16	35	1
17	36	1
18	37	1
19	38	1
20	39	1
21	40	1
22	41	1
23	42	1
24	43	1
25	44	1
26	45	1
27	46	1
28	47	1
29	48	1
30	49	1
31	50	1
32	51	1
33	52	1
34	53	1
35	54	1
36	55	1
37	56	1
38	57	1
39	58	1
40	59	1
41	60	1
42	61	1
43	62	1
44	63	1
45	64	1
46	65	1
47	66	1
48	67	1
49	68	1
50	9	1
51	10	1
52	69	1
53	70	1
54	71	1
55	72	1
56	73	1
57	7	1
58	8	1
59	3	1
60	4	1
61	74	1
62	75	1
63	76	1
64	77	1
65	78	1
66	79	1
67	80	1
68	81	1
69	15	1
70	16	1
71	82	1
72	17	1
73	18	1
74	83	1
75	84	1
76	85	1
77	11	1
78	12	1
79	86	1
80	87	1
81	88	1
82	89	1
83	90	1
84	91	1
85	92	1
86	93	1
87	94	1
88	95	1
89	96	1
90	97	1
91	98	1
92	19	1
93	5	1
94	6	1
95	99	1
96	1	1
97	2	1
98	100	1
99	101	1
100	102	1
101	103	1
102	104	1
103	105	1
104	106	1
105	107	1
106	108	1
107	109	1
108	110	1
109	111	1
110	112	1
111	113	1
112	114	1
113	115	1
114	116	1
115	117	1
116	118	1
117	119	1
118	120	1
119	121	1
120	122	1
121	123	1
122	124	1
123	125	1
124	126	1
125	127	1
126	128	1
127	129	1
128	130	1
129	131	1
130	132	1
131	133	1
132	134	1
133	135	1
134	136	1
135	137	1
136	138	1
137	139	1
138	140	1
139	141	1
140	142	1
141	143	1
142	144	1
143	145	1
144	146	1
145	147	1
146	148	1
147	149	1
148	150	1
149	151	1
150	152	1
151	153	1
152	154	1
153	155	1
154	156	1
155	157	1
156	158	1
157	159	1
158	160	1
159	161	1
160	162	1
161	163	1
162	164	1
163	165	1
164	166	1
165	167	1
166	168	1
167	169	1
168	170	1
169	171	1
170	172	1
171	173	1
172	174	1
173	175	1
174	176	1
175	177	1
176	178	1
177	179	1
178	180	1
179	181	1
180	182	1
181	183	1
182	184	1
183	185	1
184	186	1
185	187	1
186	188	1
187	189	1
188	190	1
189	191	1
190	192	1
191	193	1
192	194	1
193	13	1
194	14	1
195	195	1
196	196	1
197	197	1
198	198	1
199	199	1
200	200	1
201	201	1
202	202	1
203	203	1
204	204	1
205	205	1
206	206	1
207	207	1
208	208	1
209	209	1
210	210	1
211	211	1
212	212	1
213	213	1
214	214	1
215	215	1
216	216	1
217	217	1
218	218	1
219	219	1
220	220	1
221	221	1
222	222	1
223	223	1
224	224	1
225	225	1
226	226	1
227	227	1
228	228	1
229	229	1
230	230	1
231	3	3
232	4	3
233	5	3
234	6	3
235	7	3
236	8	3
237	9	3
238	10	3
239	11	3
240	12	3
241	15	3
242	16	3
243	17	3
244	22	3
245	23	3
246	28	3
247	42	3
248	43	3
249	44	3
250	45	3
251	46	3
252	47	3
253	48	3
254	49	3
255	50	3
256	51	3
257	52	3
258	53	3
259	54	3
260	55	3
261	56	3
262	57	3
263	58	3
264	59	3
265	60	3
266	61	3
267	62	3
268	63	3
269	64	3
270	65	3
271	66	3
272	67	3
273	68	3
274	69	3
275	70	3
276	71	3
277	72	3
278	73	3
279	74	3
280	75	3
281	76	3
282	77	3
283	79	3
284	80	3
285	81	3
286	83	3
287	84	3
288	85	3
289	86	3
290	87	3
291	88	3
292	89	3
293	90	3
294	91	3
295	92	3
296	93	3
297	94	3
298	95	3
299	96	3
300	97	3
301	98	3
302	106	3
303	108	3
304	117	3
305	118	3
306	119	3
307	120	3
308	121	3
309	122	3
310	123	3
311	124	3
312	125	3
313	126	3
314	127	3
315	128	3
316	129	3
317	130	3
318	131	3
319	132	3
320	134	3
321	135	3
322	136	3
323	138	3
324	139	3
325	140	3
326	141	3
327	142	3
328	143	3
329	144	3
330	145	3
331	146	3
332	147	3
333	148	3
334	149	3
335	150	3
336	151	3
337	152	3
338	153	3
339	154	3
340	157	3
341	158	3
342	159	3
343	160	3
344	161	3
345	162	3
346	163	3
347	166	3
348	167	3
349	168	3
350	169	3
351	174	3
352	175	3
353	176	3
354	177	3
355	179	3
356	180	3
357	181	3
358	189	3
359	190	3
360	191	3
361	192	3
362	193	3
363	194	3
364	195	3
365	206	3
366	209	3
367	210	3
368	211	3
369	212	3
370	213	3
371	214	3
372	215	3
373	216	3
374	217	3
375	218	3
376	219	3
377	220	3
378	221	3
379	226	3
380	227	3
381	229	3
382	230	3
383	3	4
384	7	4
385	8	4
386	9	4
387	11	4
388	15	4
389	16	4
390	17	4
391	22	4
392	23	4
393	28	4
394	42	4
395	43	4
396	44	4
397	45	4
398	46	4
399	47	4
400	48	4
401	49	4
402	50	4
403	51	4
404	52	4
405	53	4
406	54	4
407	55	4
408	56	4
409	57	4
410	58	4
411	59	4
412	60	4
413	61	4
414	62	4
415	63	4
416	64	4
417	65	4
418	66	4
419	67	4
420	68	4
421	69	4
422	70	4
423	71	4
424	72	4
425	73	4
426	74	4
427	75	4
428	76	4
429	77	4
430	79	4
431	80	4
432	81	4
433	87	4
434	88	4
435	89	4
436	90	4
437	91	4
438	92	4
439	93	4
440	94	4
441	95	4
442	96	4
443	106	4
444	108	4
445	117	4
446	118	4
447	119	4
448	127	4
449	128	4
450	129	4
451	131	4
452	132	4
453	134	4
454	135	4
455	136	4
456	138	4
457	139	4
458	140	4
459	141	4
460	142	4
461	143	4
462	144	4
463	145	4
464	146	4
465	147	4
466	148	4
467	149	4
468	150	4
469	151	4
470	152	4
471	153	4
472	154	4
473	157	4
474	158	4
475	159	4
476	160	4
477	161	4
478	162	4
479	163	4
480	166	4
481	167	4
482	168	4
483	169	4
484	174	4
485	175	4
486	176	4
487	177	4
488	189	4
489	190	4
490	191	4
491	192	4
492	193	4
493	194	4
494	195	4
495	206	4
496	209	4
497	210	4
498	211	4
499	212	4
500	213	4
501	214	4
502	215	4
503	217	4
504	219	4
505	229	4
506	230	4
507	171	5
508	178	5
509	1	6
510	2	6
511	17	6
512	19	6
513	99	6
514	100	6
515	101	6
516	102	6
517	133	6
518	137	6
519	141	6
520	155	6
521	156	6
522	164	6
523	165	6
524	170	6
525	173	6
526	182	6
527	183	6
528	184	6
529	185	6
530	186	6
531	187	6
532	188	6
533	222	6
534	223	6
535	224	6
536	225	6
537	209	8
538	70	8
539	220	8
540	227	8
541	226	8
542	9	8
543	10	8
544	221	8
545	117	8
546	119	8
547	118	8
548	69	8
549	71	8
550	72	8
551	73	8
552	7	8
553	8	8
554	214	8
555	125	8
556	124	8
557	218	8
558	3	8
559	4	8
560	121	8
561	120	8
562	81	8
563	79	8
564	80	8
565	15	8
566	16	8
567	75	8
568	74	8
569	77	8
570	76	8
571	213	8
572	211	8
573	17	8
574	212	8
575	84	8
576	85	8
577	83	8
578	11	8
579	12	8
580	215	8
581	128	8
582	129	8
583	127	8
584	86	8
585	130	8
586	126	8
587	106	8
588	108	8
589	87	8
590	123	8
591	122	8
592	88	8
593	90	8
594	89	8
595	92	8
596	91	8
597	95	8
598	96	8
599	94	8
600	93	8
601	210	8
602	219	8
603	97	8
604	98	8
605	131	8
606	132	8
607	194	8
608	216	8
609	68	8
610	66	8
611	217	8
612	5	8
613	6	8
614	23	8
615	22	8
616	195	8
617	150	8
618	136	8
619	167	8
620	158	8
621	139	8
622	147	8
623	141	8
624	142	8
625	149	8
626	162	8
627	153	8
628	152	8
629	174	8
630	148	8
631	138	8
632	144	8
633	168	8
634	169	8
635	154	8
636	145	8
637	159	8
638	134	8
639	175	8
640	160	8
641	143	8
642	140	8
643	176	8
644	135	8
645	177	8
646	230	8
647	229	8
648	151	8
649	163	8
650	166	8
651	157	8
652	146	8
653	161	8
654	67	8
655	181	8
656	179	8
657	180	8
658	193	8
659	189	8
660	192	8
661	191	8
662	190	8
663	\N	8
664	\N	8
665	19	8
666	225	8
667	224	8
668	99	8
669	1	8
670	2	8
671	223	8
672	222	8
673	102	8
674	100	8
675	101	8
676	133	8
677	165	8
678	156	8
679	164	8
680	184	8
681	187	8
682	182	8
683	183	8
684	188	8
685	186	8
686	185	8
687	142	9
688	174	9
689	138	9
690	141	9
691	169	9
692	175	9
693	7	9
694	3	9
695	15	9
696	17	9
697	11	9
698	194	9
699	\N	9
700	213	9
701	226	9
702	75	9
703	74	9
704	134	9
\.


--
-- Data for Name: ab_register_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ab_register_user (id, first_name, last_name, username, password, email, registration_date, registration_hash) FROM stdin;
\.


--
-- Data for Name: ab_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ab_role (id, name) FROM stdin;
1	Admin
2	Public
3	Alpha
4	Gamma
5	granter
6	sql_lab
7	BI_ADMIN
8	BI_EDITOR
9	BI_VIEWER
10	D_hd-showcase-tierstatistik_1
\.


--
-- Data for Name: ab_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ab_user (id, first_name, last_name, username, password, active, email, last_login, login_count, fail_login_count, created_on, changed_on, created_by_fk, changed_by_fk) FROM stdin;
2	admin	admin	admin	pbkdf2:sha256:260000$pjqaxHs4TrW8pt21$cfb72cd557a69b106dbe71f6bd890b566b5c8dd59ba1ac86a3b1cc4a59aaa7a8	t	admin@hellodata.ch	\N	\N	\N	2023-12-20 15:07:17.345805	2023-12-20 15:07:17.345813	1	1
1	Superset	Admin	local-admin	pbkdf2:sha256:260000$RbhgQYU5jhwwXK5b$43d8f4375c3d06bd87b2b6ccf4bce5923eab06a2676e941ba84edba6d275d1ed	t	admin@superset.com	2023-12-20 15:12:09.876985	42	0	2023-12-20 15:06:29.03476	2023-12-20 15:06:29.034768	\N	\N
\.


--
-- Data for Name: ab_user_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ab_user_role (id, user_id, role_id) FROM stdin;
1	1	1
2	2	2
\.


--
-- Data for Name: ab_view_menu; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ab_view_menu (id, name) FROM stdin;
1	SavedQuery
2	CssTemplate
3	ReportSchedule
4	Chart
5	Annotation
6	Dataset
7	Log
8	Dashboard
9	Database
10	Query
11	SupersetIndexView
12	UtilView
13	LocaleView
14	SecurityApi
15	RegisterUserOAuthView
16	ResetPasswordView
17	ResetMyPasswordView
18	UserInfoEditView
19	HdAuthOAuthView
20	UserOAuthModelView
21	RoleModelView
22	RegisterUserModelView
23	Permission
24	Role
25	User
26	ViewMenu
27	PermissionViewMenu
28	OpenApi
29	SwaggerView
30	MenuApi
31	AsyncEventsRestApi
32	AdvancedDataType
33	AvailableDomains
34	CacheRestApi
35	CurrentUserRestApi
36	DashboardFilterStateRestApi
37	DashboardPermalinkRestApi
38	Datasource
39	EmbeddedDashboard
40	Explore
41	ExploreFormDataRestApi
42	ExplorePermalinkRestApi
43	FilterSets
44	ImportExportRestApi
45	SQLLab
46	DynamicPlugin
47	RowLevelSecurityFiltersModelView
48	Api
49	CsvToDatabaseView
50	ExcelToDatabaseView
51	ColumnarToDatabaseView
52	EmbeddedView
53	KV
54	R
55	SqlLab
56	Superset
57	TableSchemaView
58	TabStateView
59	TagView
60	SecurityRestApi
61	AccessRequestsModelView
62	Security
63	List Users
64	List Roles
65	User's Statistics
66	Row Level Security
67	Action Log
68	Access requests
69	Home
70	Data
71	Databases
72	Dashboards
73	Charts
74	Datasets
75	Manage
76	Plugins
77	CSS Templates
78	Import Dashboards
79	Alerts & Report
80	Annotation Layers
81	SQL Lab
82	SQL Editor
83	Saved Queries
84	Query Search
85	all_datasource_access
86	all_database_access
87	all_query_access
88	[showcase_dd01_bieditor].(id:1)
89	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)
90	[showcase_dd01_bieditor].[udm]
91	[showcase_dd01_bieditor].[anzahl_tiere_art_rasse_datum](id:2)
92	[showcase_dd01_bieditor].[anzahl_tiere_kanton_tierart](id:3)
93	[showcase_dd01_bieditor].[fact_cattle_beefiness_fattissue](id:4)
94	[showcase_dd01_bieditor].[fact_cattle_popvariations](id:5)
95	[showcase_dd01_bieditor].[fact_cattle_pyr_long](id:6)
\.


--
-- Data for Name: access_request; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.access_request (created_on, changed_on, id, datasource_type, datasource_id, changed_by_fk, created_by_fk) FROM stdin;
\.


--
-- Data for Name: alembic_version; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.alembic_version (version_num) FROM stdin;
f3c2d8ec8595
\.


--
-- Data for Name: alert_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.alert_logs (id, scheduled_dttm, dttm_start, dttm_end, alert_id, state) FROM stdin;
\.


--
-- Data for Name: alert_owner; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.alert_owner (id, user_id, alert_id) FROM stdin;
\.


--
-- Data for Name: alerts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.alerts (id, label, active, crontab, alert_type, log_retention, grace_period, recipients, slice_id, dashboard_id, last_eval_dttm, last_state, slack_channel, changed_by_fk, changed_on, created_by_fk, created_on, validator_config, database_id, sql, validator_type) FROM stdin;
\.


--
-- Data for Name: annotation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.annotation (created_on, changed_on, id, start_dttm, end_dttm, layer_id, short_descr, long_descr, changed_by_fk, created_by_fk, json_metadata) FROM stdin;
\.


--
-- Data for Name: annotation_layer; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.annotation_layer (created_on, changed_on, id, name, descr, changed_by_fk, created_by_fk) FROM stdin;
\.


--
-- Data for Name: cache_keys; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.cache_keys (id, cache_key, cache_timeout, datasource_uid, created_on) FROM stdin;
\.


--
-- Data for Name: clusters; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.clusters (created_on, changed_on, id, cluster_name, broker_host, broker_port, broker_endpoint, metadata_last_refreshed, created_by_fk, changed_by_fk, cache_timeout, verbose_name, broker_pass, broker_user, uuid) FROM stdin;
\.


--
-- Data for Name: columns; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.columns (created_on, changed_on, id, column_name, is_active, type, groupby, filterable, description, created_by_fk, changed_by_fk, dimension_spec_json, verbose_name, datasource_id, uuid, advanced_data_type) FROM stdin;
\.


--
-- Data for Name: css_templates; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.css_templates (created_on, changed_on, id, template_name, css, changed_by_fk, created_by_fk) FROM stdin;
\.


--
-- Data for Name: dashboard_email_schedules; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.dashboard_email_schedules (created_on, changed_on, id, active, crontab, recipients, deliver_as_group, delivery_type, dashboard_id, created_by_fk, changed_by_fk, user_id, slack_channel, uuid) FROM stdin;
\.


--
-- Data for Name: dashboard_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.dashboard_roles (id, role_id, dashboard_id) FROM stdin;
1	10	1
\.


--
-- Data for Name: dashboard_slices; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.dashboard_slices (id, dashboard_id, slice_id) FROM stdin;
1	1	7
2	1	16
3	1	9
4	1	8
5	1	14
6	1	13
7	1	3
8	1	11
9	1	15
10	1	4
11	1	2
12	1	1
13	1	12
14	1	6
15	1	5
16	1	10
\.


--
-- Data for Name: dashboard_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.dashboard_user (id, user_id, dashboard_id) FROM stdin;
\.


--
-- Data for Name: dashboards; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.dashboards (created_on, changed_on, id, dashboard_title, position_json, created_by_fk, changed_by_fk, css, description, slug, json_metadata, published, uuid, certified_by, certification_details, is_managed_externally, external_url) FROM stdin;
2023-12-20 15:07:29.322409	2023-12-20 15:07:29.322415	1	HD-Showcase Tierstatistik	{"CHART-explore-568-1": {"children": [], "id": "CHART-explore-568-1", "meta": {"chartId": 16, "height": 72, "sliceName": "Zeitverlauf Anzahl Tiere nach Tierart (separiert)", "sliceNameOverride": "Entwicklungsdynamik", "uuid": "f88aa526-6e9f-4ffa-a4d7-697aa16e013c", "width": 4}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-ay1wCtPZAL", "ROW-P53J2YYoFg"], "type": "CHART"}, "CHART-explore-572-1": {"children": [], "id": "CHART-explore-572-1", "meta": {"chartId": 8, "height": 58, "sliceName": "Anzahl Tiere Geburten, Schlachtungen, Tode", "uuid": "44bd043f-63b3-4943-93e4-7f6312ec9bfe", "width": 12}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K", "ROW-OjN0xyNyi"], "type": "CHART"}, "CHART-explore-573-1": {"children": [], "id": "CHART-explore-573-1", "meta": {"chartId": 14, "height": 50, "sliceName": "Verteilung geschlachteter Rinder nach Fleischigkeitsklasse CHTAX", "uuid": "ed1a5228-482c-4e4b-ade5-84236f01788b", "width": 12}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K", "ROW-n67EYW0FkZ"], "type": "CHART"}, "CHART-explore-574-1": {"children": [], "id": "CHART-explore-574-1", "meta": {"chartId": 13, "height": 50, "sliceName": "Verteilung geschlachteter Rinder nach Fettgewebeklasse CHTAX", "uuid": "e71c4e69-26b2-4f39-a8fc-9e80f3f7a612", "width": 12}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K", "ROW-3Q8qPpP6mu"], "type": "CHART"}, "CHART-explore-578-1": {"children": [], "id": "CHART-explore-578-1", "meta": {"chartId": 7, "height": 63, "sliceName": "Aktueller Bestand Tierart & Rasse", "sliceNameOverride": "Aktuelle Anzahl Tiere nach Art & Rasse", "uuid": "762cf818-1a6f-4f3d-bd93-4c2db1839a8f", "width": 5}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-5JEUk64NcR"], "type": "CHART"}, "CHART-explore-579-1": {"children": [], "id": "CHART-explore-579-1", "meta": {"chartId": 4, "height": 20, "sliceName": "Aktuelle Anzahl Rinder", "sliceNameOverride": "Rinder", "uuid": "4d814a31-d3c7-494d-9831-4a64518c07cb", "width": 2}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-HHA_OJp6SJ"], "type": "CHART"}, "CHART-explore-580-1": {"children": [], "id": "CHART-explore-580-1", "meta": {"chartId": 1, "height": 20, "sliceName": "Aktuelle Anzahl Equiden", "sliceNameOverride": "Equiden", "uuid": "51b2600b-9fc1-4724-b69d-c623cabdd0cd", "width": 2}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-HHA_OJp6SJ"], "type": "CHART"}, "CHART-explore-581-1": {"children": [], "id": "CHART-explore-581-1", "meta": {"chartId": 6, "height": 20, "sliceName": "Aktuelle Anzahl Ziegen", "sliceNameOverride": "Ziegen", "uuid": "839371f3-bea5-430a-a698-0c7d1dd8c956", "width": 2}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-HHA_OJp6SJ"], "type": "CHART"}, "CHART-explore-582-1": {"children": [], "id": "CHART-explore-582-1", "meta": {"chartId": 5, "height": 20, "sliceName": "Aktuelle Anzahl Schafe", "sliceNameOverride": "Schafe", "uuid": "901bab0c-3f65-49cf-b310-c0478d86a2e1", "width": 2}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-HHA_OJp6SJ"], "type": "CHART"}, "CHART-explore-583-1": {"children": [], "id": "CHART-explore-583-1", "meta": {"chartId": 2, "height": 20, "sliceName": "Aktuelle Anzahl Hunde", "sliceNameOverride": "Hunde", "uuid": "9650b94c-e0ab-45cd-aeaa-5144673c0eb4", "width": 2}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-HHA_OJp6SJ"], "type": "CHART"}, "CHART-explore-584-1": {"children": [], "id": "CHART-explore-584-1", "meta": {"chartId": 3, "height": 20, "sliceName": "Aktuelle Anzahl Katzen", "sliceNameOverride": "Katzen", "uuid": "bdc48c2c-c86f-4f6c-b4a1-4533d90a7d84", "width": 2}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-HHA_OJp6SJ"], "type": "CHART"}, "CHART-explore-585-1": {"children": [], "id": "CHART-explore-585-1", "meta": {"chartId": 9, "height": 12, "sliceName": "Datenstand", "uuid": "a7d70451-ea8e-427d-8337-cbd8ecd093cd", "width": 2}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-3aAtDNNY4K"], "type": "CHART"}, "CHART-explore-586-1": {"children": [], "id": "CHART-explore-586-1", "meta": {"chartId": 15, "height": 63, "sliceName": "Vollst\\u00e4ndige Liste Aktuelle Anzahl nach Tierart und Rasse", "sliceNameOverride": "Vollst\\u00e4ndige Liste aktuelle Anzahl Tiere nach Art & Rasse", "uuid": "bc7e2593-ae99-405c-b724-1475356883d6", "width": 7}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-5JEUk64NcR"], "type": "CHART"}, "CHART-explore-587-1": {"children": [], "id": "CHART-explore-587-1", "meta": {"chartId": 12, "height": 111, "sliceName": "Verteilung auf Kantone Karte", "uuid": "330b764a-fd2f-4df2-93d9-5230fb205c7f", "width": 6}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-JWz9tR4JV", "ROW-_iTXCo687O"], "type": "CHART"}, "CHART-explore-588-1": {"children": [], "id": "CHART-explore-588-1", "meta": {"chartId": 11, "height": 111, "sliceName": "Verteilung auf Kantone", "uuid": "c13322e0-4a71-4da4-bbe8-2608f59ee053", "width": 6}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-JWz9tR4JV", "ROW-_iTXCo687O"], "type": "CHART"}, "CHART-explore-589-1": {"children": [], "id": "CHART-explore-589-1", "meta": {"chartId": 10, "height": 72, "sliceName": "Entwicklung Tierarten", "uuid": "f728f3f7-38de-4bfc-b093-a6c7f5173938", "width": 8}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-ay1wCtPZAL", "ROW-P53J2YYoFg"], "type": "CHART"}, "DASHBOARD_VERSION_KEY": "v2", "DIVIDER-5N6lHPEP4P": {"children": [], "id": "DIVIDER-5N6lHPEP4P", "meta": {}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz"], "type": "DIVIDER"}, "DIVIDER-CfX9B-YosH": {"children": [], "id": "DIVIDER-CfX9B-YosH", "meta": {}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K"], "type": "DIVIDER"}, "DIVIDER-jAtZwYj3-a": {"children": [], "id": "DIVIDER-jAtZwYj3-a", "meta": {}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-JWz9tR4JV"], "type": "DIVIDER"}, "DIVIDER-lnHwYbAPYe": {"children": [], "id": "DIVIDER-lnHwYbAPYe", "meta": {}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-ay1wCtPZAL"], "type": "DIVIDER"}, "GRID_ID": {"children": [], "id": "GRID_ID", "parents": ["ROOT_ID"], "type": "GRID"}, "HEADER_ID": {"id": "HEADER_ID", "meta": {"text": "HD-Showcase Tierstatistik"}, "type": "HEADER"}, "MARKDOWN-80qLIM2btt": {"children": [], "id": "MARKDOWN-80qLIM2btt", "meta": {"code": "Datenquelle: <a href=\\"https://tierstatistik.identitas.ch/de/index.html\\" target=\\"_blank\\">Tierstatistik Identitas</a>", "height": 8, "width": 12}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-JWz9tR4JV", "ROW-H1Hp4l_VQt"], "type": "MARKDOWN"}, "MARKDOWN-8xnYXJZ6ey": {"children": [], "id": "MARKDOWN-8xnYXJZ6ey", "meta": {"code": "Datenquelle: <a href=\\"https://tierstatistik.identitas.ch/de/index.html\\" target=\\"_blank\\">Tierstatistik Identitas</a>", "height": 8, "width": 12}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-Z-4d2_roQI"], "type": "MARKDOWN"}, "MARKDOWN-AdGdbzYnR4": {"children": [], "id": "MARKDOWN-AdGdbzYnR4", "meta": {"code": "### Fleischigkeit CHTAX\\n![schleichigkeit](https://www.viegut.ch/assets/Uploads/_resampled/ResizedImageWzYwMCw0MDBd/Viegut-CH-Tax.jpg)<br>\\nBildquelle: [Viegut AG](https://www.viegut.ch/de/marktinfo/chtax-tabellen/)", "height": 63, "width": 6}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K", "ROW-lilLsO1wZa"], "type": "MARKDOWN"}, "MARKDOWN-GmukTaodeK": {"children": [], "id": "MARKDOWN-GmukTaodeK", "meta": {"code": "# Entwicklung Bestand", "height": 12, "width": 12}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-ay1wCtPZAL", "ROW-dvQ98p5AH7"], "type": "MARKDOWN"}, "MARKDOWN-P9KyTuB-py": {"children": [], "id": "MARKDOWN-P9KyTuB-py", "meta": {"code": "Datenquelle: <a href=\\"https://tierstatistik.identitas.ch/de/index.html\\" target=\\"_blank\\">Tierstatistik Identitas</a>", "height": 8, "width": 12}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-ay1wCtPZAL", "ROW-gbH1rPUJsV"], "type": "MARKDOWN"}, "MARKDOWN-Yx_prUwa7s": {"children": [], "id": "MARKDOWN-Yx_prUwa7s", "meta": {"code": "Datenquelle: <a href=\\"https://tierstatistik.identitas.ch/de/index.html\\" target=\\"_blank\\">Tierstatistik Identitas</a>", "height": 8, "width": 12}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K", "ROW-e6Tv32nrGH"], "type": "MARKDOWN"}, "MARKDOWN-nlfat-aCqF": {"children": [], "id": "MARKDOWN-nlfat-aCqF", "meta": {"code": "# Aktueller Bestand", "height": 12, "width": 10}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz", "ROW-3aAtDNNY4K"], "type": "MARKDOWN"}, "MARKDOWN-qoOL49O53S": {"children": [], "id": "MARKDOWN-qoOL49O53S", "meta": {"code": "### Fettgewebeklasse CHTAX\\r\\n<table>\\r\\n  <tr>\\r\\n    <th>Fettgewebeklasse&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</th>\\r\\n    <th>Beschreibung</th>\\r\\n  </tr>\\r\\n  <tr>\\r\\n    <td>ungedeckt<br>&nbsp;</td>\\r\\n    <td>keine Fettabdeckung<br>Griffe nicht ausgebildet</td>\\r\\n  </tr>\\r\\n  <tr>\\r\\n    <td>teilweise gedeckt<br>&nbsp;<br>&nbsp;</td>\\r\\n    <td>unbedeutende bis leichte Fettabdeckung<br>Muskulatur teilweise sichtbar<br>einzelne Griffe leicht sp\\u00fcrbar ausgebildet</td>\\r\\n  </tr>\\r\\n  <tr>\\r\\n    <td>gleichm\\u00e4ssig gedeckt<br>&nbsp;<br>&nbsp;</td>\\r\\n    <td>leichte gleichm\\u00e4ssige Fettabdeckung<br>Muskulatur generell abgedeckt<br>alle Griffe ausgebildet, kernig</td>\\r\\n  </tr>\\r\\n  <tr>\\r\\n    <td>stark gedeckt<br>&nbsp;<br>&nbsp;</td>\\r\\n    <td>betont ausgepr\\u00e4gte Fettabdeckung<br>auf einzelnen Partien \\u00fcberm\\u00e4ssig<br>einzelne Griffe stark ausgebildet</td>\\r\\n  </tr>\\r\\n  <tr>\\r\\n    <td>\\u00fcberfett<br>&nbsp;<br>&nbsp;</td>\\r\\n    <td>Fettabdeckung generell \\u00fcberm\\u00e4ssig<br>wulstartige Fettgebilde<br>einzelne Griffe zu stark ausgebildet</td>\\r\\n  </tr>\\r\\n</table>\\r\\n", "height": 63, "width": 6}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K", "ROW-lilLsO1wZa"], "type": "MARKDOWN"}, "MARKDOWN-wbAps4aZAW": {"children": [], "id": "MARKDOWN-wbAps4aZAW", "meta": {"code": "# Rindfleisch-Qualit\\u00e4t", "height": 12, "width": 12}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K", "ROW-SrWfrFTxWB"], "type": "MARKDOWN"}, "MARKDOWN-wpPKT5xHdF": {"children": [], "id": "MARKDOWN-wpPKT5xHdF", "meta": {"code": "# Geografische Verteilung", "height": 12, "width": 12}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-JWz9tR4JV", "ROW-NEKNZt1jUQ"], "type": "MARKDOWN"}, "ROOT_ID": {"children": ["TABS-J86_3a2S3f"], "id": "ROOT_ID", "type": "ROOT"}, "ROW-3Q8qPpP6mu": {"children": ["CHART-explore-574-1"], "id": "ROW-3Q8qPpP6mu", "meta": {"background": "BACKGROUND_TRANSPARENT"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K"], "type": "ROW"}, "ROW-3aAtDNNY4K": {"children": ["MARKDOWN-nlfat-aCqF", "CHART-explore-585-1"], "id": "ROW-3aAtDNNY4K", "meta": {"background": "BACKGROUND_WHITE"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz"], "type": "ROW"}, "ROW-5JEUk64NcR": {"children": ["CHART-explore-578-1", "CHART-explore-586-1"], "id": "ROW-5JEUk64NcR", "meta": {"background": "BACKGROUND_WHITE"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz"], "type": "ROW"}, "ROW-H1Hp4l_VQt": {"children": ["MARKDOWN-80qLIM2btt"], "id": "ROW-H1Hp4l_VQt", "meta": {"background": "BACKGROUND_WHITE"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-JWz9tR4JV"], "type": "ROW"}, "ROW-HHA_OJp6SJ": {"children": ["CHART-explore-579-1", "CHART-explore-580-1", "CHART-explore-581-1", "CHART-explore-582-1", "CHART-explore-583-1", "CHART-explore-584-1"], "id": "ROW-HHA_OJp6SJ", "meta": {"background": "BACKGROUND_WHITE"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz"], "type": "ROW"}, "ROW-NEKNZt1jUQ": {"children": ["MARKDOWN-wpPKT5xHdF"], "id": "ROW-NEKNZt1jUQ", "meta": {"background": "BACKGROUND_TRANSPARENT"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-JWz9tR4JV"], "type": "ROW"}, "ROW-OjN0xyNyi": {"children": ["CHART-explore-572-1"], "id": "ROW-OjN0xyNyi", "meta": {"background": "BACKGROUND_TRANSPARENT"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K"], "type": "ROW"}, "ROW-P53J2YYoFg": {"children": ["CHART-explore-589-1", "CHART-explore-568-1"], "id": "ROW-P53J2YYoFg", "meta": {"background": "BACKGROUND_WHITE"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-ay1wCtPZAL"], "type": "ROW"}, "ROW-SrWfrFTxWB": {"children": ["MARKDOWN-wbAps4aZAW"], "id": "ROW-SrWfrFTxWB", "meta": {"background": "BACKGROUND_TRANSPARENT"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K"], "type": "ROW"}, "ROW-Z-4d2_roQI": {"children": ["MARKDOWN-8xnYXJZ6ey"], "id": "ROW-Z-4d2_roQI", "meta": {"background": "BACKGROUND_WHITE"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-_EA1MHvkz"], "type": "ROW"}, "ROW-_iTXCo687O": {"children": ["CHART-explore-587-1", "CHART-explore-588-1"], "id": "ROW-_iTXCo687O", "meta": {"background": "BACKGROUND_WHITE"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-JWz9tR4JV"], "type": "ROW"}, "ROW-dvQ98p5AH7": {"children": ["MARKDOWN-GmukTaodeK"], "id": "ROW-dvQ98p5AH7", "meta": {"background": "BACKGROUND_TRANSPARENT"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-ay1wCtPZAL"], "type": "ROW"}, "ROW-e6Tv32nrGH": {"children": ["MARKDOWN-Yx_prUwa7s"], "id": "ROW-e6Tv32nrGH", "meta": {"background": "BACKGROUND_TRANSPARENT"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K"], "type": "ROW"}, "ROW-gbH1rPUJsV": {"children": ["MARKDOWN-P9KyTuB-py"], "id": "ROW-gbH1rPUJsV", "meta": {"background": "BACKGROUND_TRANSPARENT"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-ay1wCtPZAL"], "type": "ROW"}, "ROW-lilLsO1wZa": {"children": ["MARKDOWN-AdGdbzYnR4", "MARKDOWN-qoOL49O53S"], "id": "ROW-lilLsO1wZa", "meta": {"background": "BACKGROUND_WHITE"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K"], "type": "ROW"}, "ROW-n67EYW0FkZ": {"children": ["CHART-explore-573-1"], "id": "ROW-n67EYW0FkZ", "meta": {"background": "BACKGROUND_TRANSPARENT"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f", "TAB-olQcPH77K"], "type": "ROW"}, "TAB-JWz9tR4JV": {"children": ["ROW-NEKNZt1jUQ", "ROW-_iTXCo687O", "DIVIDER-jAtZwYj3-a", "ROW-H1Hp4l_VQt"], "id": "TAB-JWz9tR4JV", "meta": {"defaultText": "Tab title", "placeholder": "Tab title", "text": "Geografische Verteilung"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f"], "type": "TAB"}, "TAB-_EA1MHvkz": {"children": ["ROW-3aAtDNNY4K", "ROW-HHA_OJp6SJ", "ROW-5JEUk64NcR", "DIVIDER-5N6lHPEP4P", "ROW-Z-4d2_roQI"], "id": "TAB-_EA1MHvkz", "meta": {"defaultText": "Tab title", "placeholder": "Tab title", "text": "Aktueller Bestand"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f"], "type": "TAB"}, "TAB-ay1wCtPZAL": {"children": ["ROW-dvQ98p5AH7", "ROW-P53J2YYoFg", "DIVIDER-lnHwYbAPYe", "ROW-gbH1rPUJsV"], "id": "TAB-ay1wCtPZAL", "meta": {"defaultText": "Tab title", "placeholder": "Tab title", "text": "Entwicklung Bestand"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f"], "type": "TAB"}, "TAB-olQcPH77K": {"children": ["ROW-SrWfrFTxWB", "ROW-OjN0xyNyi", "ROW-n67EYW0FkZ", "ROW-3Q8qPpP6mu", "ROW-lilLsO1wZa", "DIVIDER-CfX9B-YosH", "ROW-e6Tv32nrGH"], "id": "TAB-olQcPH77K", "meta": {"defaultText": "Tab title", "placeholder": "Tab title", "text": "Rindfleisch-Qualit\\u00e4t"}, "parents": ["ROOT_ID", "TABS-J86_3a2S3f"], "type": "TAB"}, "TABS-J86_3a2S3f": {"children": ["TAB-_EA1MHvkz", "TAB-JWz9tR4JV", "TAB-ay1wCtPZAL", "TAB-olQcPH77K"], "id": "TABS-J86_3a2S3f", "meta": {}, "parents": ["ROOT_ID"], "type": "TABS"}}	\N	\N	.open > div:nth-child(3) {\r\n  position: absolute;\r\n  bottom: 2vh;\r\n  background: none !important;\r\n}\r\n \r\n.open:nth-child(2) > div:nth-child(2) {\r\n  height: calc(95vh - 80px) !important;\r\n}	\N	tierstatistik	{"show_native_filters": true, "color_scheme": "supersetColors", "refresh_frequency": 0, "expanded_slices": {}, "label_colors": {"======== Tierarten ========": "#000000", "Rinder": "#1fa8c9", "Equiden": "#444e7d", "Ziegen": "#5bc089", "Schafe": "#ff7e45", "Hunde": "#676766", "Katzen": "#e04254"}, "timed_refresh_immune_slices": [], "cross_filters_enabled": false, "default_filters": "{}", "chart_configuration": {}, "native_filter_configuration": [{"id": "NATIVE_FILTER-nPY0EF0Y_", "controlValues": {"sortAscending": true, "enableEmptyFilter": false, "defaultToFirstItem": false, "multiSelect": false, "searchAllOptions": false, "inverseSelection": false}, "name": "Jahr", "filterType": "filter_select", "targets": [{"column": {"name": "year"}, "datasetId": 2}], "defaultDataMask": {"extraFormData": {}, "filterState": {}, "ownState": {}}, "cascadeParentIds": [], "scope": {"rootPath": ["TAB-olQcPH77K", "TAB-ay1wCtPZAL"], "excluded": []}, "type": "NATIVE_FILTER", "description": "", "chartsInScope": [2, 3, 4, 5, 16], "tabsInScope": ["TAB-ay1wCtPZAL", "TAB-olQcPH77K"]}, {"id": "NATIVE_FILTER-52bWG3lld", "controlValues": {"enableEmptyFilter": true, "defaultToFirstItem": false, "multiSelect": false, "searchAllOptions": false, "inverseSelection": false}, "name": "Jahr", "filterType": "filter_select", "targets": [{"column": {"name": "year"}, "datasetId": 3}], "defaultDataMask": {"extraFormData": {"filters": [{"col": "year", "op": "IN", "val": [2023]}]}, "filterState": {"validateMessage": false, "validateStatus": false, "label": "2023", "value": [2023]}, "__cache": {"validateMessage": false, "validateStatus": false, "label": "2023", "value": [2023]}}, "cascadeParentIds": [], "scope": {"rootPath": ["TAB-JWz9tR4JV"], "excluded": []}, "type": "NATIVE_FILTER", "description": "", "chartsInScope": [587, 588, 589], "tabsInScope": ["TAB-JWz9tR4JV", "TAB-ay1wCtPZAL"]}, {"id": "NATIVE_FILTER-y_HJ2e2A-", "controlValues": {"sortAscending": false, "enableEmptyFilter": true, "defaultToFirstItem": true, "multiSelect": false, "searchAllOptions": false, "inverseSelection": false}, "name": "Monat", "filterType": "filter_select", "targets": [{"column": {"name": "month"}, "datasetId": 3}], "defaultDataMask": {"extraFormData": {}, "filterState": {}, "ownState": {}}, "cascadeParentIds": ["NATIVE_FILTER-52bWG3lld"], "scope": {"rootPath": ["TAB-JWz9tR4JV"], "excluded": []}, "type": "NATIVE_FILTER", "description": "", "chartsInScope": [9, 13], "tabsInScope": ["TAB-JWz9tR4JV"], "adhoc_filters": [{"expressionType": "SIMPLE", "subject": "month", "operator": "IS NOT NULL", "operatorId": "IS_NOT_NULL", "comparator": null, "clause": "WHERE", "sqlExpression": null, "isExtra": false, "isNew": false, "datasourceWarning": false, "filterOptionName": "filter_9s8iq3801b_8o00t4s02ms"}], "time_range": "No filter", "requiredFirst": true}, {"id": "NATIVE_FILTER-cDa7hDdb4", "controlValues": {"enableEmptyFilter": false, "defaultToFirstItem": false, "multiSelect": true, "searchAllOptions": false, "inverseSelection": false}, "name": "Tierart", "filterType": "filter_select", "targets": [{"column": {"name": "species"}, "datasetId": 2}], "defaultDataMask": {"extraFormData": {}, "filterState": {}, "ownState": {}}, "cascadeParentIds": [], "scope": {"rootPath": ["TAB-ay1wCtPZAL", "TAB-JWz9tR4JV"], "excluded": []}, "type": "NATIVE_FILTER", "description": "", "chartsInScope": [567, 568, 569, 587], "tabsInScope": ["TAB-JWz9tR4JV", "TAB-ay1wCtPZAL"]}, {"id": "NATIVE_FILTER-JSaeqU-R0", "controlValues": {"enableEmptyFilter": false, "defaultToFirstItem": false, "multiSelect": true, "searchAllOptions": false, "inverseSelection": false}, "name": "Geschlecht", "filterType": "filter_select", "targets": [{"column": {"name": "population"}, "datasetId": 6}], "defaultDataMask": {"extraFormData": {}, "filterState": {}, "ownState": {}}, "cascadeParentIds": [], "scope": {"rootPath": ["TAB-olQcPH77K"], "excluded": [8, 14, 13]}, "type": "NATIVE_FILTER", "description": "", "chartsInScope": [576, 578], "tabsInScope": ["TAB-_EA1MHvkz", "TAB-olQcPH77K"]}, {"id": "NATIVE_FILTER-eN7SEoxMM", "controlValues": {"enableEmptyFilter": false, "defaultToFirstItem": false, "multiSelect": true, "searchAllOptions": false, "inverseSelection": false}, "name": "Kanton", "filterType": "filter_select", "targets": [{"column": {"name": "canton"}, "datasetId": 3}], "defaultDataMask": {"extraFormData": {}, "filterState": {}, "ownState": {}}, "cascadeParentIds": [], "scope": {"rootPath": ["TAB-JWz9tR4JV", "TAB-ay1wCtPZAL"], "excluded": [16, 12]}, "type": "NATIVE_FILTER", "description": "", "chartsInScope": [588], "tabsInScope": ["TAB-JWz9tR4JV"]}], "shared_label_colors": {"root": "#1FA8C9", "Katzen": "#e04254", "European": "#5AC189", "European Shorthair": "#FF7F44", "Crossbred": "#666666", "Maine Coon": "#E04355", "British Shorthair": "#FCC700", "Bengal": "#A868B7", "Norwegian Forest Cat": "#3CCCCB", "European Longhair": "#A38F79", "Persian": "#8FD3E4", "Sacred Birman": "#A1A6BD", "Ragdoll": "#ACE1C4", "Unknown": "#FEC0A1", "Siamese": "#B2B2B2", "Siberian": "#EFA1AA", "Sphynx": "#FDE380", "Rinder": "#1fa8c9", "Braunvieh": "#9EE5E5", "Kreuzung": "#D1C6BC", "Holstein": "#1FA8C9", "Red Holstein": "#454E7C", "Swiss Fleckvieh": "#5AC189", "Limousin": "#FF7F44", "Simmental": "#666666", "Angus": "#E04355", "Original Braunvieh": "#FCC700", "Montb\\u00e9liarde": "#A868B7", "Grauvieh": "#3CCCCB", "Rotfleckvieh": "#A38F79", "Eringer": "#8FD3E4", "Jersey": "#A1A6BD", "Highland Cattle": "#ACE1C4", "Aubrac": "#FEC0A1", "Galloway": "#B2B2B2", "Charolais": "#EFA1AA", "Weissblaue Belgier": "#FDE380", "Dexter": "#D3B3DA", "Normande": "#9EE5E5", "Hinterwaelder": "#D1C6BC", "Piemonteser": "#1FA8C9", "Blonde d'Aquitaine": "#454E7C", "Hunde": "#676766", "Chihuahueno": "#FF7F44", "Labrador Retriever": "#666666", "Rasse unbekannt": "#E04355", "Yorkshire Terrier": "#FCC700", "Jack Russell Terrier": "#A868B7", "Franzosische Bulldogge": "#3CCCCB", "Border Collie": "#A38F79", "Zwergspitz / Pomeranian": "#8FD3E4", "Zwergpudel": "#A1A6BD", "Golden Retriever": "#ACE1C4", "Deutscher Schaferhund": "#FEC0A1", "Australian Shepherd": "#B2B2B2", "Maltese": "#EFA1AA", "Zwerg-Dachshund": "#FDE380", "Berner Sennenhund": "#D3B3DA", "Chien de Berger Belge": "#9EE5E5", "English Cocker Spaniel": "#D1C6BC", "Shih Tzu": "#1FA8C9", "Bolonka Zwetna": "#454E7C", "Pug": "#5AC189", "Bichon Havanais": "#FF7F44", "Lagotto Romagnolo": "#666666", "Siberian Husky": "#E04355", "Beagle": "#FCC700", "Appenzeller Sennenhund": "#A868B7", "Cavalier King Charles Spaniel": "#3CCCCB", "Zwergpinscher / Rehpinscher": "#A38F79", "Staffordshire Bull Terrier": "#8FD3E4", "Deutscher Boxer": "#A1A6BD", "West Highland White Terrier": "#ACE1C4", "Parson Russell Terrier": "#FEC0A1", "Shetland Sheepdog (Sheltie)": "#B2B2B2", "American Staffordshire Terrier": "#EFA1AA", "Shiba Inu": "#FDE380", "Englische Bulldogge": "#D3B3DA", "Schafe": "#ff7e45", "Andere": "#D1C6BC", "Weisses Alpenschaf": "#1FA8C9", "Schwarzbraunes Bergschaf": "#454E7C", "Lacaune": "#5AC189", "Walliser Schwarznasenschaf": "#FF7F44", "Braunkoepfiges Fleischschaf": "#666666", "Engadinerschaf": "#E04355", "Spiegelschaf": "#FCC700", "Suffolk": "#A868B7", "Texel": "#3CCCCB", "Skudde": "#A38F79", "Charollais Suisse": "#8FD3E4", "Heidschnucke": "#A1A6BD", "Ouessant": "#ACE1C4", "Ostfriesisches Milchschaf": "#FEC0A1", "Dorper": "#B2B2B2", "Nolana": "#EFA1AA", "Shropshire": "#FDE380", "Walliser Landschaf": "#D3B3DA", "Equiden": "#444e7d", "Freiberger": "#D1C6BC", "Europ\\u00e4isches Sportpferd Schweiz": "#1FA8C9", "Esel diverse Herkunft": "#454E7C", "Pony diverse Herkunft": "#5AC189", "Shetland Pony": "#FF7F44", "Europ\\u00e4isches Sportpferd Deutschland": "#666666", "Islandpferd": "#E04355", "Warmblut diverse Herkunft": "#FCC700", "Ziegen": "#5bc089", "Gaemsfarbige Gebirgsziege": "#3CCCCB", "Saanenziege": "#A38F79", "Zwergziege": "#8FD3E4", "Buendner Strahlenziege": "#A1A6BD", "Burenziege": "#ACE1C4", "Toggenburgerziege": "#FEC0A1", "Appenzellerziege": "#B2B2B2", "Walliser Schwarzhalsziege": "#EFA1AA", "Nera Verzascaziege": "#FDE380", "Pfauenziege": "#D3B3DA"}, "color_scheme_domain": ["#1FA8C9", "#454E7C", "#5AC189", "#FF7F44", "#666666", "#E04355", "#FCC700", "#A868B7", "#3CCCCB", "#A38F79", "#8FD3E4", "#A1A6BD", "#ACE1C4", "#FEC0A1", "#B2B2B2", "#EFA1AA", "#FDE380", "#D3B3DA", "#9EE5E5", "#D1C6BC"]}	t	cda52e2b-ced4-4525-ba6c-a7c5f3051bc0	\N	\N	f	\N
\.


--
-- Data for Name: databasechangelog; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) FROM stdin;
0000	hellodata	superset-changelog.xml	2023-12-20 15:07:03.30868	1	EXECUTED	8:eb12f182f36b604524e225be9e9144a4	sqlFile path=changelogs/00_enable_extensions.sql		\N	4.21.1	\N	\N	3084822731
0001	hellodata	superset-changelog.xml	2023-12-20 15:07:04.098037	2	EXECUTED	8:0e7bc12a61f5b2e6c480397b286c9630	sqlFile path=changelogs/01_inital_HD_roles_and_permissions.sql		\N	4.21.1	\N	\N	3084822731
0002	hellodata	superset-changelog.xml	2023-12-20 15:07:04.115506	3	EXECUTED	8:73af078d216fba74245a72fb00fc0c28	sqlFile path=changelogs/02_functions.sql		\N	4.21.1	\N	\N	3084822731
0003	hellodata	superset-changelog.xml	2023-12-20 15:07:04.170324	4	EXECUTED	8:4ffb6f15e3092b397afffad29d5e64c6	sqlFile path=changelogs/03_first_time_migration.sql		\N	4.21.1	\N	\N	3084822731
0004	hellodata	superset-changelog.xml	2023-12-20 15:07:04.255122	5	EXECUTED	8:122a94e4618660459c789747f26b8c7e	sqlFile path=changelogs/04_triggers.sql		\N	4.21.1	\N	\N	3084822731
0005	hellodata	superset-changelog.xml	2023-12-20 15:07:04.261913	6	EXECUTED	8:23ab9db0bb9dce1b35a6a186b6fe3e5d	sqlFile path=changelogs/05_delete_duplicate_owners.sql		\N	4.21.1	\N	\N	3084822731
0006	hellodata	superset-changelog.xml	2023-12-20 15:07:04.272775	7	EXECUTED	8:a3d8960572c3b9900b02211f00501075	sqlFile path=changelogs/06_handle_owners_on_datasets.sql		\N	4.21.1	\N	\N	3084822731
\.


--
-- Data for Name: databasechangeloglock; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.databasechangeloglock (id, locked, lockgranted, lockedby) FROM stdin;
1	f	\N	\N
\.


--
-- Data for Name: datasources; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.datasources (created_on, changed_on, id, datasource_name, is_featured, is_hidden, description, default_endpoint, created_by_fk, changed_by_fk, "offset", cache_timeout, perm, filter_select_enabled, params, fetch_values_from, schema_perm, cluster_id, uuid, is_managed_externally, external_url) FROM stdin;
\.


--
-- Data for Name: dbs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.dbs (created_on, changed_on, id, database_name, sqlalchemy_uri, created_by_fk, changed_by_fk, password, cache_timeout, extra, select_as_create_table_as, allow_ctas, expose_in_sqllab, force_ctas_schema, allow_run_async, allow_dml, verbose_name, impersonate_user, allow_file_upload, encrypted_extra, server_cert, allow_cvas, uuid, configuration_method, is_managed_externally, external_url) FROM stdin;
2023-12-20 15:07:28.779933	2023-12-20 15:07:28.77994	1	showcase_dd01_bieditor	postgresql+psycopg2://postgres:postgres@postgres:5432/hellodata_product_development_default_data_domain_dwh	\N	\N	\N	\N	{"allows_virtual_table_explore": true, "schemas_allowed_for_file_upload": ["csv"]}	f	t	t	\N	f	f	\N	f	t	\N	\N	t	d399c363-73cb-4435-a59a-7709e6b615db	sqlalchemy_form	f	\N
\.


--
-- Data for Name: druiddatasource_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.druiddatasource_user (id, user_id, datasource_id) FROM stdin;
\.


--
-- Data for Name: dynamic_plugin; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.dynamic_plugin (created_on, changed_on, id, name, key, bundle_url, created_by_fk, changed_by_fk) FROM stdin;
\.


--
-- Data for Name: embedded_dashboards; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.embedded_dashboards (created_on, changed_on, allow_domain_list, uuid, dashboard_id, changed_by_fk, created_by_fk) FROM stdin;
\.


--
-- Data for Name: favstar; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.favstar (id, user_id, class_name, obj_id, dttm) FROM stdin;
\.


--
-- Data for Name: filter_sets; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.filter_sets (created_on, changed_on, id, name, description, json_metadata, owner_id, owner_type, dashboard_id, created_by_fk, changed_by_fk) FROM stdin;
\.


--
-- Data for Name: key_value; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.key_value (id, resource, value, uuid, created_on, created_by_fk, changed_on, changed_by_fk, expires_on) FROM stdin;
\.


--
-- Data for Name: keyvalue; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.keyvalue (id, value) FROM stdin;
\.


--
-- Data for Name: logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.logs (id, action, user_id, json, dttm, dashboard_id, slice_id, duration_ms, referrer) FROM stdin;
1	DashboardRestApi.get_list	1	{"path": "/api/v1/dashboard/", "q": "{\\"page\\":0,\\"page_size\\":2147483647}", "rison": {"page": 0, "page_size": 2147483647}}	2023-12-20 15:07:34.795319	\N	0	70	\N
2	DashboardRestApi.get_list	1	{"path": "/api/v1/dashboard/", "q": "{\\"page\\":0,\\"page_size\\":2147483647}", "rison": {"page": 0, "page_size": 2147483647}}	2023-12-20 15:08:04.907662	\N	0	27	\N
3	DashboardRestApi.get_list	1	{"path": "/api/v1/dashboard/", "q": "{\\"page\\":0,\\"page_size\\":2147483647}", "rison": {"page": 0, "page_size": 2147483647}}	2023-12-20 15:08:35.023465	\N	0	34	\N
4	DashboardRestApi.get_list	1	{"path": "/api/v1/dashboard/", "q": "{\\"page\\":0,\\"page_size\\":2147483647}", "rison": {"page": 0, "page_size": 2147483647}}	2023-12-20 15:09:05.134642	\N	0	29	\N
5	DashboardRestApi.get_list	1	{"path": "/api/v1/dashboard/", "q": "{\\"page\\":0,\\"page_size\\":2147483647}", "rison": {"page": 0, "page_size": 2147483647}}	2023-12-20 15:09:35.252104	\N	0	29	\N
6	DashboardRestApi.get_list	1	{"path": "/api/v1/dashboard/", "q": "{\\"page\\":0,\\"page_size\\":2147483647}", "rison": {"page": 0, "page_size": 2147483647}}	2023-12-20 15:10:05.357037	\N	0	27	\N
7	DashboardRestApi.get_list	1	{"path": "/api/v1/dashboard/", "q": "{\\"page\\":0,\\"page_size\\":2147483647}", "rison": {"page": 0, "page_size": 2147483647}}	2023-12-20 15:10:35.462243	\N	0	28	\N
8	DashboardRestApi.get_list	1	{"path": "/api/v1/dashboard/", "q": "{\\"page\\":0,\\"page_size\\":2147483647}", "rison": {"page": 0, "page_size": 2147483647}}	2023-12-20 15:11:05.563049	\N	0	26	\N
9	DashboardRestApi.get_list	1	{"path": "/api/v1/dashboard/", "q": "{\\"page\\":0,\\"page_size\\":2147483647}", "rison": {"page": 0, "page_size": 2147483647}}	2023-12-20 15:11:35.677228	\N	0	32	\N
10	DashboardRestApi.get_list	1	{"path": "/api/v1/dashboard/", "q": "{\\"page\\":0,\\"page_size\\":2147483647}", "rison": {"page": 0, "page_size": 2147483647}}	2023-12-20 15:12:05.798315	\N	0	27	\N
\.


--
-- Data for Name: metrics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.metrics (id, metric_name, verbose_name, metric_type, json, description, changed_by_fk, changed_on, created_by_fk, created_on, d3format, warning_text, datasource_id, uuid) FROM stdin;
\.


--
-- Data for Name: query; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.query (id, client_id, database_id, tmp_table_name, tab_name, sql_editor_id, user_id, status, schema, sql, select_sql, executed_sql, "limit", select_as_cta, select_as_cta_used, progress, rows, error_message, start_time, changed_on, end_time, results_key, start_running_time, end_result_backend_time, tracking_url, extra_json, tmp_schema_name, ctas_method, limiting_factor) FROM stdin;
\.


--
-- Data for Name: report_execution_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.report_execution_log (id, scheduled_dttm, start_dttm, end_dttm, value, value_row_json, state, error_message, report_schedule_id, uuid) FROM stdin;
\.


--
-- Data for Name: report_recipient; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.report_recipient (id, type, recipient_config_json, report_schedule_id, created_on, changed_on, created_by_fk, changed_by_fk) FROM stdin;
\.


--
-- Data for Name: report_schedule; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.report_schedule (id, type, name, description, context_markdown, active, crontab, sql, chart_id, dashboard_id, database_id, last_eval_dttm, last_state, last_value, last_value_row_json, validator_type, validator_config_json, log_retention, grace_period, created_on, changed_on, created_by_fk, changed_by_fk, working_timeout, report_format, creation_method, timezone, extra_json, force_screenshot) FROM stdin;
\.


--
-- Data for Name: report_schedule_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.report_schedule_user (id, user_id, report_schedule_id) FROM stdin;
\.


--
-- Data for Name: rls_filter_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.rls_filter_roles (id, role_id, rls_filter_id) FROM stdin;
\.


--
-- Data for Name: rls_filter_tables; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.rls_filter_tables (id, table_id, rls_filter_id) FROM stdin;
\.


--
-- Data for Name: row_level_security_filters; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.row_level_security_filters (created_on, changed_on, id, clause, created_by_fk, changed_by_fk, filter_type, group_key, name, description) FROM stdin;
\.


--
-- Data for Name: saved_query; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.saved_query (created_on, changed_on, id, user_id, db_id, label, schema, sql, description, changed_by_fk, created_by_fk, extra_json, last_run, rows, uuid, template_parameters) FROM stdin;
\.


--
-- Data for Name: sl_columns; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sl_columns (uuid, created_on, changed_on, id, is_aggregation, is_additive, is_dimensional, is_filterable, is_increase_desired, is_managed_externally, is_partition, is_physical, is_temporal, is_spatial, name, type, unit, expression, description, warning_text, external_url, extra_json, created_by_fk, changed_by_fk, advanced_data_type) FROM stdin;
\.


--
-- Data for Name: sl_dataset_columns; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sl_dataset_columns (dataset_id, column_id) FROM stdin;
\.


--
-- Data for Name: sl_dataset_tables; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sl_dataset_tables (dataset_id, table_id) FROM stdin;
\.


--
-- Data for Name: sl_dataset_users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sl_dataset_users (dataset_id, user_id) FROM stdin;
\.


--
-- Data for Name: sl_datasets; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sl_datasets (uuid, created_on, changed_on, id, database_id, is_physical, is_managed_externally, name, expression, external_url, extra_json, created_by_fk, changed_by_fk) FROM stdin;
\.


--
-- Data for Name: sl_table_columns; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sl_table_columns (table_id, column_id) FROM stdin;
\.


--
-- Data for Name: sl_tables; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sl_tables (uuid, created_on, changed_on, id, database_id, is_managed_externally, catalog, schema, name, external_url, extra_json, created_by_fk, changed_by_fk) FROM stdin;
\.


--
-- Data for Name: slice_email_schedules; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.slice_email_schedules (created_on, changed_on, id, active, crontab, recipients, deliver_as_group, delivery_type, slice_id, email_format, created_by_fk, changed_by_fk, user_id, slack_channel, uuid) FROM stdin;
\.


--
-- Data for Name: slice_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.slice_user (id, user_id, slice_id) FROM stdin;
\.


--
-- Data for Name: slices; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.slices (created_on, changed_on, id, slice_name, datasource_type, datasource_name, viz_type, params, created_by_fk, changed_by_fk, description, cache_timeout, perm, datasource_id, schema_perm, uuid, query_context, last_saved_at, last_saved_by_fk, certified_by, certification_details, is_managed_externally, external_url) FROM stdin;
2023-12-20 15:07:29.18349	2023-12-20 15:07:29.183495	1	Aktuelle Anzahl Equiden	table	aktuelle_anzahl_tierart_rasse	big_number_total	{"datasource": "42__table", "viz_type": "big_number_total", "slice_id": 580, "granularity_sqla": "date_actual", "time_range": "No filter", "metric": {"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1017, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": false, "label": "SUM(n_animals)", "optionName": "metric_ijgoktd1u3b_odfww5gc5f", "sqlExpression": null}, "adhoc_filters": [{"clause": "WHERE", "comparator": "Equiden", "datasourceWarning": false, "expressionType": "SIMPLE", "filterOptionName": "filter_lbgt67zk7g_90xlgkbwtxu", "isExtra": false, "isNew": false, "operator": "==", "operatorId": "EQUALS", "sqlExpression": null, "subject": "species"}], "header_font_size": 0.5, "subheader_font_size": 0.15, "y_axis_format": ",d", "time_format": "smart_date", "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)	1	[showcase_dd01_bieditor].[udm]	51b2600b-9fc1-4724-b69d-c623cabdd0cd	{"force": false, "queries": [{"extras": {"where": "", "having": ""}, "columns": [], "filters": [{"op": "==", "col": "species", "val": "Equiden"}], "metrics": [{"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_ijgoktd1u3b_odfww5gc5f", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}], "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "custom_params": {}, "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}}], "form_data": {"force": false, "metric": {"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_ijgoktd1u3b_odfww5gc5f", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}, "slice_id": 580, "viz_type": "big_number_total", "dashboards": [13], "datasource": "42__table", "time_range": "No filter", "result_type": "full", "time_format": "smart_date", "adhoc_filters": [{"isNew": false, "clause": "WHERE", "isExtra": false, "subject": "species", "operator": "==", "comparator": "Equiden", "operatorId": "EQUALS", "sqlExpression": null, "expressionType": "SIMPLE", "filterOptionName": "filter_lbgt67zk7g_90xlgkbwtxu", "datasourceWarning": false}], "result_format": "json", "y_axis_format": ",d", "extra_form_data": {}, "granularity_sqla": "date_actual", "header_font_size": 0.5, "subheader_font_size": 0.15}, "datasource": {"id": "4", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.192433	2023-12-20 15:07:29.192438	2	Aktuelle Anzahl Hunde	table	aktuelle_anzahl_tierart_rasse	big_number_total	{"datasource": "42__table", "viz_type": "big_number_total", "granularity_sqla": "date_actual", "time_range": "No filter", "metric": {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1017, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": false, "label": "SUM(n_animals)", "optionName": "metric_w8n686pyqy_2u2lod9t3bf"}, "adhoc_filters": [{"expressionType": "SIMPLE", "subject": "species", "operator": "==", "operatorId": "EQUALS", "comparator": "Hunde", "clause": "WHERE", "sqlExpression": null, "isExtra": false, "isNew": false, "datasourceWarning": false, "filterOptionName": "filter_xa0wt6xx7gp_shvy0a6nus"}], "header_font_size": 0.5, "subheader_font_size": 0.15, "y_axis_format": ",d", "time_format": "smart_date", "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)	1	[showcase_dd01_bieditor].[udm]	9650b94c-e0ab-45cd-aeaa-5144673c0eb4	{"force": false, "queries": [{"extras": {"where": "", "having": ""}, "columns": [], "filters": [{"op": "==", "col": "species", "val": "Hunde"}], "metrics": [{"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_w8n686pyqy_2u2lod9t3bf", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}], "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "custom_params": {}, "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}}], "form_data": {"force": false, "metric": {"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_w8n686pyqy_2u2lod9t3bf", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}, "viz_type": "big_number_total", "dashboards": [13], "datasource": "42__table", "time_range": "No filter", "result_type": "full", "time_format": "smart_date", "adhoc_filters": [{"isNew": false, "clause": "WHERE", "isExtra": false, "subject": "species", "operator": "==", "comparator": "Hunde", "operatorId": "EQUALS", "sqlExpression": null, "expressionType": "SIMPLE", "filterOptionName": "filter_xa0wt6xx7gp_shvy0a6nus", "datasourceWarning": false}], "result_format": "json", "y_axis_format": ",d", "extra_form_data": {}, "granularity_sqla": "date_actual", "header_font_size": 0.5, "subheader_font_size": 0.15}, "datasource": {"id": "4", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.201243	2023-12-20 15:07:29.201247	3	Aktuelle Anzahl Katzen	table	aktuelle_anzahl_tierart_rasse	big_number_total	{"datasource": "42__table", "viz_type": "big_number_total", "granularity_sqla": "date_actual", "time_range": "No filter", "metric": {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1017, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": false, "label": "SUM(n_animals)", "optionName": "metric_nypgt9zm6a_11ic1o0u4r5h"}, "adhoc_filters": [{"expressionType": "SIMPLE", "subject": "species", "operator": "==", "operatorId": "EQUALS", "comparator": "Katzen", "clause": "WHERE", "sqlExpression": null, "isExtra": false, "isNew": false, "datasourceWarning": false, "filterOptionName": "filter_vhrjy35ko2l_w5gv5ghjb1"}], "header_font_size": 0.5, "subheader_font_size": 0.15, "y_axis_format": ",d", "time_format": "smart_date", "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)	1	[showcase_dd01_bieditor].[udm]	bdc48c2c-c86f-4f6c-b4a1-4533d90a7d84	{"force": false, "queries": [{"extras": {"where": "", "having": ""}, "columns": [], "filters": [{"op": "==", "col": "species", "val": "Katzen"}], "metrics": [{"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_nypgt9zm6a_11ic1o0u4r5h", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}], "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "custom_params": {}, "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}}], "form_data": {"force": false, "metric": {"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_nypgt9zm6a_11ic1o0u4r5h", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}, "viz_type": "big_number_total", "dashboards": [13], "datasource": "42__table", "time_range": "No filter", "result_type": "full", "time_format": "smart_date", "adhoc_filters": [{"isNew": false, "clause": "WHERE", "isExtra": false, "subject": "species", "operator": "==", "comparator": "Katzen", "operatorId": "EQUALS", "sqlExpression": null, "expressionType": "SIMPLE", "filterOptionName": "filter_vhrjy35ko2l_w5gv5ghjb1", "datasourceWarning": false}], "result_format": "json", "y_axis_format": ",d", "extra_form_data": {}, "granularity_sqla": "date_actual", "header_font_size": 0.5, "subheader_font_size": 0.15}, "datasource": {"id": "4", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.209571	2023-12-20 15:07:29.209575	4	Aktuelle Anzahl Rinder	table	aktuelle_anzahl_tierart_rasse	big_number_total	{"datasource": "42__table", "viz_type": "big_number_total", "slice_id": 579, "granularity_sqla": "date_actual", "time_range": "No filter", "metric": {"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1017, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": false, "label": "SUM(n_animals)", "optionName": "metric_sv2rgymn07n_1rvrpts3p1f", "sqlExpression": null}, "adhoc_filters": [{"clause": "WHERE", "comparator": "Rinder", "datasourceWarning": false, "expressionType": "SIMPLE", "filterOptionName": "filter_olc7t94lnwf_p4wbxf409pg", "isExtra": false, "isNew": false, "operator": "==", "operatorId": "EQUALS", "sqlExpression": null, "subject": "species"}], "subheader": "", "header_font_size": 0.5, "subheader_font_size": 0.15, "y_axis_format": ",d", "time_format": "smart_date", "force_timestamp_formatting": false, "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)	1	[showcase_dd01_bieditor].[udm]	4d814a31-d3c7-494d-9831-4a64518c07cb	{"force": false, "queries": [{"extras": {"where": "", "having": ""}, "columns": [], "filters": [{"op": "==", "col": "species", "val": "Rinder"}], "metrics": [{"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_sv2rgymn07n_1rvrpts3p1f", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}], "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "custom_params": {}, "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}}], "form_data": {"force": false, "metric": {"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_sv2rgymn07n_1rvrpts3p1f", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}, "slice_id": 579, "viz_type": "big_number_total", "subheader": "", "dashboards": [13], "datasource": "42__table", "time_range": "No filter", "result_type": "full", "time_format": "smart_date", "adhoc_filters": [{"isNew": false, "clause": "WHERE", "isExtra": false, "subject": "species", "operator": "==", "comparator": "Rinder", "operatorId": "EQUALS", "sqlExpression": null, "expressionType": "SIMPLE", "filterOptionName": "filter_olc7t94lnwf_p4wbxf409pg", "datasourceWarning": false}], "result_format": "json", "y_axis_format": ",d", "extra_form_data": {}, "granularity_sqla": "date_actual", "header_font_size": 0.5, "subheader_font_size": 0.15, "force_timestamp_formatting": false}, "datasource": {"id": "4", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.217859	2023-12-20 15:07:29.217862	5	Aktuelle Anzahl Schafe	table	aktuelle_anzahl_tierart_rasse	big_number_total	{"datasource": "42__table", "viz_type": "big_number_total", "granularity_sqla": "date_actual", "time_range": "No filter", "metric": {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1017, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": false, "label": "SUM(n_animals)", "optionName": "metric_iq7t7j8ub8d_ruj0baqblmj"}, "adhoc_filters": [{"expressionType": "SIMPLE", "subject": "species", "operator": "==", "operatorId": "EQUALS", "comparator": "Schafe", "clause": "WHERE", "sqlExpression": null, "isExtra": false, "isNew": false, "datasourceWarning": false, "filterOptionName": "filter_td90d9rxf3s_55yag92toqe"}], "header_font_size": 0.5, "subheader_font_size": 0.15, "y_axis_format": ",d", "time_format": "smart_date", "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)	1	[showcase_dd01_bieditor].[udm]	901bab0c-3f65-49cf-b310-c0478d86a2e1	{"force": false, "queries": [{"extras": {"where": "", "having": ""}, "columns": [], "filters": [{"op": "==", "col": "species", "val": "Schafe"}], "metrics": [{"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_iq7t7j8ub8d_ruj0baqblmj", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}], "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "custom_params": {}, "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}}], "form_data": {"force": false, "metric": {"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_iq7t7j8ub8d_ruj0baqblmj", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}, "viz_type": "big_number_total", "dashboards": [13], "datasource": "42__table", "time_range": "No filter", "result_type": "full", "time_format": "smart_date", "adhoc_filters": [{"isNew": false, "clause": "WHERE", "isExtra": false, "subject": "species", "operator": "==", "comparator": "Schafe", "operatorId": "EQUALS", "sqlExpression": null, "expressionType": "SIMPLE", "filterOptionName": "filter_td90d9rxf3s_55yag92toqe", "datasourceWarning": false}], "result_format": "json", "y_axis_format": ",d", "extra_form_data": {}, "granularity_sqla": "date_actual", "header_font_size": 0.5, "subheader_font_size": 0.15}, "datasource": {"id": "4", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.226408	2023-12-20 15:07:29.226413	6	Aktuelle Anzahl Ziegen	table	aktuelle_anzahl_tierart_rasse	big_number_total	{"datasource": "42__table", "viz_type": "big_number_total", "granularity_sqla": "date_actual", "time_range": "No filter", "metric": {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1017, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": false, "label": "SUM(n_animals)", "optionName": "metric_5f9dlebj693_plc7dh62svp"}, "adhoc_filters": [{"expressionType": "SIMPLE", "subject": "species", "operator": "==", "operatorId": "EQUALS", "comparator": "Ziegen", "clause": "WHERE", "sqlExpression": null, "isExtra": false, "isNew": false, "datasourceWarning": false, "filterOptionName": "filter_g2bwql8po0w_rnuu9w0phlp"}], "header_font_size": 0.5, "subheader_font_size": 0.15, "y_axis_format": ",d", "time_format": "smart_date", "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)	1	[showcase_dd01_bieditor].[udm]	839371f3-bea5-430a-a698-0c7d1dd8c956	{"force": false, "queries": [{"extras": {"where": "", "having": ""}, "columns": [], "filters": [{"op": "==", "col": "species", "val": "Ziegen"}], "metrics": [{"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_5f9dlebj693_plc7dh62svp", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}], "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "custom_params": {}, "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}}], "form_data": {"force": false, "metric": {"label": "SUM(n_animals)", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_5f9dlebj693_plc7dh62svp", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}, "viz_type": "big_number_total", "dashboards": [13], "datasource": "42__table", "time_range": "No filter", "result_type": "full", "time_format": "smart_date", "adhoc_filters": [{"isNew": false, "clause": "WHERE", "isExtra": false, "subject": "species", "operator": "==", "comparator": "Ziegen", "operatorId": "EQUALS", "sqlExpression": null, "expressionType": "SIMPLE", "filterOptionName": "filter_g2bwql8po0w_rnuu9w0phlp", "datasourceWarning": false}], "result_format": "json", "y_axis_format": ",d", "extra_form_data": {}, "granularity_sqla": "date_actual", "header_font_size": 0.5, "subheader_font_size": 0.15}, "datasource": {"id": "4", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.234893	2023-12-20 15:07:29.234898	7	Aktueller Bestand Tierart & Rasse	table	aktuelle_anzahl_tierart_rasse	sunburst	{"datasource": "42__table", "viz_type": "sunburst", "slice_id": 578, "granularity_sqla": "date_actual", "time_range": "No filter", "groupby": ["species", "breed"], "metric": {"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1017, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": true, "label": "Anzahl Tiere", "optionName": "metric_w8zj0laoolj_l74oe1b6wl", "sqlExpression": null}, "adhoc_filters": [], "row_limit": 1000, "sort_by_metric": true, "color_scheme": "supersetColors", "linear_color_scheme": "superset_seq_1", "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)	1	[showcase_dd01_bieditor].[udm]	762cf818-1a6f-4f3d-bd93-4c2db1839a8f	{"force": false, "queries": [{"extras": {"where": "", "having": ""}, "columns": ["species", "breed"], "filters": [], "metrics": [{"label": "Anzahl Tiere", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_w8zj0laoolj_l74oe1b6wl", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}], "row_limit": 1000, "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "custom_params": {}, "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}}], "form_data": {"force": false, "metric": {"label": "Anzahl Tiere", "column": {"id": 1017, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_w8zj0laoolj_l74oe1b6wl", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, "groupby": ["species", "breed"], "slice_id": 578, "viz_type": "sunburst", "row_limit": 1000, "dashboards": [13], "datasource": "42__table", "time_range": "No filter", "result_type": "full", "color_scheme": "supersetColors", "adhoc_filters": [], "result_format": "json", "sort_by_metric": true, "extra_form_data": {}, "granularity_sqla": "date_actual", "linear_color_scheme": "superset_seq_1"}, "datasource": {"id": "4", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.244407	2023-12-20 15:07:29.244412	8	Anzahl Tiere Geburten, Schlachtungen, Tode	table	fact_cattle_popvariations	echarts_area	{"datasource": "37__table", "viz_type": "echarts_area", "slice_id": 572, "granularity_sqla": "date_actual", "time_grain_sqla": "P1M", "time_range": "No filter", "metrics": [{"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "birth", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 917, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": true, "label": "Geburten", "optionName": "metric_jpjcckxkmah_7ilo7z2tdg", "sqlExpression": null}, {"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "death", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 920, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": true, "label": "Todesf\\u00e4lle", "optionName": "metric_m8sk8cj50c_e7usruqjus", "sqlExpression": null}, {"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "slaugthering", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 919, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": true, "label": "Schlachtungen", "optionName": "metric_k2kw9bbz3bm_u5uuy7svz4", "sqlExpression": null}, {"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "import", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 916, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": true, "label": "Import", "optionName": "metric_c44r08971d_70b57n0lwpo", "sqlExpression": null}, {"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "export", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 922, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": true, "label": "Export", "optionName": "metric_5tz5ijd2shu_i2u5j90da0l", "sqlExpression": null}], "groupby": [], "adhoc_filters": [], "timeseries_limit_metric": [], "order_desc": true, "row_limit": 10000, "truncate_metric": true, "show_empty_columns": true, "rolling_type": null, "comparison_type": "values", "annotation_layers": [], "forecastPeriods": 10, "forecastInterval": 0.8, "x_axis_title_margin": 15, "y_axis_title_margin": 15, "y_axis_title_position": "Left", "color_scheme": "supersetColors", "seriesType": "line", "opacity": 0.2, "only_total": true, "show_extra_controls": false, "markerSize": 6, "zoomable": true, "show_legend": true, "legendType": "scroll", "legendOrientation": "top", "x_axis_time_format": "smart_date", "rich_tooltip": true, "tooltipSortByMetric": true, "tooltipTimeFormat": "%m/%Y", "y_axis_format": ",d", "y_axis_bounds": [null, null], "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[fact_cattle_popvariations](id:5)	5	[showcase_dd01_bieditor].[udm]	44bd043f-63b3-4943-93e4-7f6312ec9bfe	{"force": false, "queries": [{"extras": {"where": "", "having": "", "time_grain_sqla": "P1M"}, "columns": [], "filters": [], "metrics": [{"label": "Geburten", "column": {"id": 917, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "birth", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_jpjcckxkmah_7ilo7z2tdg", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "Todesfälle", "column": {"id": 920, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "death", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_m8sk8cj50c_e7usruqjus", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "Schlachtungen", "column": {"id": 919, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "slaugthering", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_k2kw9bbz3bm_u5uuy7svz4", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "Import", "column": {"id": 916, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "import", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_c44r08971d_70b57n0lwpo", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "Export", "column": {"id": 922, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "export", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_5tz5ijd2shu_i2u5j90da0l", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}], "orderby": [[{"label": "Geburten", "column": {"id": 917, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "birth", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_jpjcckxkmah_7ilo7z2tdg", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, false]], "row_limit": 10000, "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "time_offsets": [], "custom_params": {}, "is_timeseries": true, "series_columns": [], "post_processing": [{"options": {"index": ["__timestamp"], "columns": [], "aggregates": {"Export": {"operator": "mean"}, "Import": {"operator": "mean"}, "Geburten": {"operator": "mean"}, "Todesfälle": {"operator": "mean"}, "Schlachtungen": {"operator": "mean"}}, "drop_missing_columns": false}, "operation": "pivot"}, {"operation": "flatten"}], "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}, "series_limit_metric": []}], "form_data": {"force": false, "groupby": [], "metrics": [{"label": "Geburten", "column": {"id": 917, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "birth", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_jpjcckxkmah_7ilo7z2tdg", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "Todesfälle", "column": {"id": 920, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "death", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_m8sk8cj50c_e7usruqjus", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "Schlachtungen", "column": {"id": 919, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "slaugthering", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_k2kw9bbz3bm_u5uuy7svz4", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "Import", "column": {"id": 916, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "import", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_c44r08971d_70b57n0lwpo", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "Export", "column": {"id": 922, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "export", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_5tz5ijd2shu_i2u5j90da0l", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}], "opacity": 0.2, "slice_id": 572, "viz_type": "echarts_area", "zoomable": true, "row_limit": 10000, "dashboards": [13], "datasource": "37__table", "legendType": "scroll", "markerSize": 6, "only_total": true, "order_desc": true, "seriesType": "line", "time_range": "No filter", "result_type": "full", "show_legend": true, "color_scheme": "supersetColors", "rich_tooltip": true, "rolling_type": null, "adhoc_filters": [], "result_format": "json", "y_axis_bounds": [null, null], "y_axis_format": ",d", "comparison_type": "values", "extra_form_data": {}, "forecastPeriods": 10, "time_grain_sqla": "P1M", "truncate_metric": true, "forecastInterval": 0.8, "granularity_sqla": "date_actual", "annotation_layers": [], "legendOrientation": "top", "tooltipTimeFormat": "%m/%Y", "show_empty_columns": true, "x_axis_time_format": "smart_date", "show_extra_controls": false, "tooltipSortByMetric": true, "x_axis_title_margin": 15, "y_axis_title_margin": 15, "y_axis_title_position": "Left", "timeseries_limit_metric": []}, "datasource": {"id": "6", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.253211	2023-12-20 15:07:29.253214	9	Datenstand	table	aktuelle_anzahl_tierart_rasse	big_number_total	{"datasource": "42__table", "viz_type": "big_number_total", "slice_id": 585, "granularity_sqla": "date_actual", "time_range": "No filter", "metric": {"aggregate": "MIN", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "date_actual", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1010, "is_certified": false, "is_dttm": true, "python_date_format": null, "type": "DATE", "type_generic": 2, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": false, "label": "MIN(date_actual)", "optionName": "metric_ik6cafigov_umwpi8c6d7", "sqlExpression": null}, "adhoc_filters": [], "header_font_size": 0.5, "subheader_font_size": 0.15, "y_axis_format": "%m/%Y", "time_format": "%m/%Y", "force_timestamp_formatting": false, "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)	1	[showcase_dd01_bieditor].[udm]	a7d70451-ea8e-427d-8337-cbd8ecd093cd	{"force": false, "queries": [{"extras": {"where": "", "having": ""}, "columns": [], "filters": [], "metrics": [{"label": "MIN(date_actual)", "column": {"id": 1010, "type": "DATE", "groupby": true, "is_dttm": true, "expression": null, "filterable": true, "column_name": "date_actual", "description": null, "certified_by": null, "is_certified": false, "type_generic": 2, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "MIN", "optionName": "metric_ik6cafigov_umwpi8c6d7", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}], "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "custom_params": {}, "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}}], "form_data": {"force": false, "metric": {"label": "MIN(date_actual)", "column": {"id": 1010, "type": "DATE", "groupby": true, "is_dttm": true, "expression": null, "filterable": true, "column_name": "date_actual", "description": null, "certified_by": null, "is_certified": false, "type_generic": 2, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "MIN", "optionName": "metric_ik6cafigov_umwpi8c6d7", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}, "slice_id": 585, "viz_type": "big_number_total", "dashboards": [13], "datasource": "42__table", "time_range": "No filter", "result_type": "full", "time_format": "%m/%Y", "adhoc_filters": [], "result_format": "json", "y_axis_format": "%m/%Y", "extra_form_data": {}, "granularity_sqla": "date_actual", "header_font_size": 0.5, "subheader_font_size": 0.15, "force_timestamp_formatting": false}, "datasource": {"id": "4", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.261635	2023-12-20 15:07:29.261639	10	Entwicklung Tierarten	table	anzahl_tiere_kanton_tierart	echarts_area	{"datasource": "45__table", "viz_type": "echarts_area", "slice_id": 589, "granularity_sqla": "date_actual", "time_grain_sqla": "P1M", "time_range": "No filter", "metrics": [{"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1043, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": "Anzahl Tiere", "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": false, "label": "SUM(Anzahl Tiere)", "optionName": "metric_kqar7n8t2dj_qn4cudwozwq", "sqlExpression": null}], "groupby": ["species"], "adhoc_filters": [], "order_desc": true, "row_limit": 10000, "truncate_metric": true, "show_empty_columns": true, "comparison_type": "values", "annotation_layers": [], "forecastPeriods": 10, "forecastInterval": 0.8, "x_axis_title_margin": 15, "y_axis_title": "Anzahl Tiere", "y_axis_title_margin": 15, "y_axis_title_position": "Top", "color_scheme": "supersetColors", "seriesType": "line", "opacity": 0.2, "stack": null, "only_total": true, "show_extra_controls": true, "markerSize": 6, "zoomable": true, "show_legend": true, "legendType": "scroll", "legendOrientation": "top", "x_axis_time_format": "smart_date", "rich_tooltip": true, "tooltipTimeFormat": "%m/%Y", "y_axis_format": ",d", "y_axis_bounds": [null, null], "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[anzahl_tiere_kanton_tierart](id:3)	3	[showcase_dd01_bieditor].[udm]	f728f3f7-38de-4bfc-b093-a6c7f5173938	{"force": false, "queries": [{"extras": {"where": "", "having": "", "time_grain_sqla": "P1M"}, "columns": ["species"], "filters": [], "metrics": [{"label": "SUM(Anzahl Tiere)", "column": {"id": 1043, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": "Anzahl Tiere", "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_kqar7n8t2dj_qn4cudwozwq", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}], "orderby": [[{"label": "SUM(Anzahl Tiere)", "column": {"id": 1043, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": "Anzahl Tiere", "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_kqar7n8t2dj_qn4cudwozwq", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}, false]], "row_limit": 10000, "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "time_offsets": [], "custom_params": {}, "is_timeseries": true, "series_columns": ["species"], "post_processing": [{"options": {"index": ["__timestamp"], "columns": ["species"], "aggregates": {"SUM(Anzahl Tiere)": {"operator": "mean"}}, "drop_missing_columns": false}, "operation": "pivot"}, {"options": {"level": 0, "columns": {"SUM(Anzahl Tiere)": null}, "inplace": true}, "operation": "rename"}, {"operation": "flatten"}], "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}}], "form_data": {"force": false, "stack": null, "groupby": ["species"], "metrics": [{"label": "SUM(Anzahl Tiere)", "column": {"id": 1043, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": "Anzahl Tiere", "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_kqar7n8t2dj_qn4cudwozwq", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": false, "datasourceWarning": false}], "opacity": 0.2, "slice_id": 589, "viz_type": "echarts_area", "zoomable": true, "row_limit": 10000, "dashboards": [13], "datasource": "45__table", "legendType": "scroll", "markerSize": 6, "only_total": true, "order_desc": true, "seriesType": "line", "time_range": "No filter", "result_type": "full", "show_legend": true, "color_scheme": "supersetColors", "rich_tooltip": true, "y_axis_title": "Anzahl Tiere", "adhoc_filters": [], "result_format": "json", "y_axis_bounds": [null, null], "y_axis_format": ",d", "comparison_type": "values", "extra_form_data": {}, "forecastPeriods": 10, "time_grain_sqla": "P1M", "truncate_metric": true, "forecastInterval": 0.8, "granularity_sqla": "date_actual", "annotation_layers": [], "legendOrientation": "top", "tooltipTimeFormat": "%m/%Y", "show_empty_columns": true, "x_axis_time_format": "smart_date", "show_extra_controls": true, "x_axis_title_margin": 15, "y_axis_title_margin": 15, "y_axis_title_position": "Top"}, "datasource": {"id": "2", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.270482	2023-12-20 15:07:29.270487	11	Verteilung auf Kantone	table	anzahl_tiere_kanton_tierart	table	{"datasource": "2__table", "viz_type": "table", "slice_id": 13, "granularity_sqla": "date_actual", "time_grain_sqla": "P1D", "time_range": "No filter", "query_mode": "raw", "groupby": ["canton", "species"], "metrics": [{"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1043, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": true, "label": "Anzahl Tiere", "optionName": "metric_ibk18wo6j3p_2y6buq00wkr", "sqlExpression": null}], "all_columns": ["canton", "species", "n_animals"], "percent_metrics": [], "adhoc_filters": [], "order_by_cols": ["[\\"n_animals\\", false]"], "row_limit": 1000, "server_page_length": 10, "order_desc": true, "show_totals": false, "table_timestamp_format": "smart_date", "page_length": 0, "include_search": true, "show_cell_bars": true, "color_pn": true, "column_config": {"n_animals": {"d3NumberFormat": ",d", "d3SmallNumberFormat": ",d"}}, "conditional_formatting": [], "extra_form_data": {}, "dashboards": [1]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[anzahl_tiere_kanton_tierart](id:3)	3	[showcase_dd01_bieditor].[udm]	c13322e0-4a71-4da4-bbe8-2608f59ee053	{"datasource":{"id":2,"type":"table"},"force":false,"queries":[{"time_range":"No filter","granularity":"date_actual","filters":[],"extras":{"time_grain_sqla":"P1D","having":"","where":""},"applied_time_extras":{},"columns":["canton","species","n_animals"],"orderby":[["n_animals",false]],"annotation_layers":[],"row_limit":1000,"series_limit":0,"order_desc":true,"url_params":{},"custom_params":{},"custom_form_data":{},"post_processing":[]}],"form_data":{"datasource":"2__table","viz_type":"table","slice_id":13,"granularity_sqla":"date_actual","time_grain_sqla":"P1D","time_range":"No filter","query_mode":"raw","groupby":["canton","species"],"metrics":[{"aggregate":"SUM","column":{"advanced_data_type":null,"certification_details":null,"certified_by":null,"column_name":"n_animals","description":null,"expression":null,"filterable":true,"groupby":true,"id":1043,"is_certified":false,"is_dttm":false,"python_date_format":null,"type":"DECIMAL","type_generic":0,"verbose_name":null,"warning_markdown":null},"datasourceWarning":false,"expressionType":"SIMPLE","hasCustomLabel":true,"label":"Anzahl Tiere","optionName":"metric_ibk18wo6j3p_2y6buq00wkr","sqlExpression":null}],"all_columns":["canton","species","n_animals"],"percent_metrics":[],"adhoc_filters":[],"order_by_cols":["[\\"n_animals\\", false]"],"row_limit":1000,"server_page_length":10,"include_time":false,"order_desc":true,"show_totals":false,"table_timestamp_format":"smart_date","page_length":0,"include_search":true,"show_cell_bars":true,"color_pn":true,"column_config":{"n_animals":{"d3NumberFormat":",d","d3SmallNumberFormat":",d"}},"conditional_formatting":[],"extra_form_data":{},"dashboards":[1],"force":false,"result_format":"json","result_type":"full"},"result_format":"json","result_type":"full"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.279898	2023-12-20 15:07:29.279902	12	Verteilung auf Kantone Karte	table	anzahl_tiere_kanton_tierart	country_map	{"datasource": "45__table", "viz_type": "country_map", "slice_id": 587, "granularity_sqla": "date_actual", "time_range": "No filter", "select_country": "switzerland", "entity": "canton", "metric": {"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 1043, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": true, "label": "Anzahl Tiere", "optionName": "metric_a5fswm6kfgn_h60al1rhgn", "sqlExpression": null}, "adhoc_filters": [], "number_format": ",d", "linear_color_scheme": "superset_seq_1", "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[anzahl_tiere_kanton_tierart](id:3)	3	[showcase_dd01_bieditor].[udm]	330b764a-fd2f-4df2-93d9-5230fb205c7f	{"force": false, "queries": [{"extras": {"where": "", "having": ""}, "columns": [], "filters": [], "metrics": [{"label": "Anzahl Tiere", "column": {"id": 1043, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_a5fswm6kfgn_h60al1rhgn", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}], "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "custom_params": {}, "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}}], "form_data": {"force": false, "entity": "canton", "metric": {"label": "Anzahl Tiere", "column": {"id": 1043, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "n_animals", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_a5fswm6kfgn_h60al1rhgn", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, "slice_id": 587, "viz_type": "country_map", "dashboards": [13], "datasource": "45__table", "time_range": "No filter", "result_type": "full", "adhoc_filters": [], "number_format": ",d", "result_format": "json", "select_country": "switzerland", "extra_form_data": {}, "granularity_sqla": "date_actual", "linear_color_scheme": "superset_seq_1"}, "datasource": {"id": "2", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.289054	2023-12-20 15:07:29.28906	13	Verteilung geschlachteter Rinder nach Fettgewebeklasse CHTAX	table	fact_cattle_beefiness_fattissue	echarts_timeseries_bar	{"datasource": "38__table", "viz_type": "echarts_timeseries_bar", "slice_id": 574, "granularity_sqla": "date_actual", "time_grain_sqla": "P1M", "time_range": "No filter", "metrics": [{"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "1", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 932, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": true, "label": "1: ungedeckt", "optionName": "metric_4nm20kwhlya_exp4uu0dq3u"}, {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "2", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 933, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": true, "label": "2: teilweise gedeckt", "optionName": "metric_0ioi5v04ut76_27p5z7sl5cy"}, {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "3", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 934, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": true, "label": "3: gleichm\\u00e4ssig gedeckt", "optionName": "metric_qwul4v8exb9_oslt5bfwqv"}, {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "4", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 935, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": true, "label": "4: stark gedeckt", "optionName": "metric_6ixg10x016e_0a01jkv2udyi"}, {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "5", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 936, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": true, "label": "5: \\u00fcberfett", "optionName": "metric_kz1pmbxrz7_t5zs9zzmtms"}], "groupby": [], "contributionMode": null, "adhoc_filters": [], "timeseries_limit_metric": [], "order_desc": true, "row_limit": 10000, "truncate_metric": true, "show_empty_columns": true, "rolling_type": null, "comparison_type": "values", "annotation_layers": [], "forecastPeriods": 10, "forecastInterval": 0.8, "orientation": "vertical", "x_axis_title": "", "x_axis_title_margin": 15, "y_axis_title": "", "y_axis_title_margin": 15, "y_axis_title_position": "Left", "color_scheme": "supersetColors", "stack": "Expand", "only_total": true, "zoomable": true, "show_legend": true, "legendType": "scroll", "legendOrientation": "top", "x_axis_time_format": "smart_date", "xAxisLabelRotation": 0, "y_axis_format": "SMART_NUMBER", "logAxis": false, "minorSplitLine": false, "truncateYAxis": false, "y_axis_bounds": [null, null], "rich_tooltip": true, "tooltipTimeFormat": "%m/%Y", "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[fact_cattle_beefiness_fattissue](id:4)	4	[showcase_dd01_bieditor].[udm]	e71c4e69-26b2-4f39-a8fc-9e80f3f7a612	{"force": false, "queries": [{"extras": {"where": "", "having": "", "time_grain_sqla": "P1M"}, "columns": [], "filters": [], "metrics": [{"label": "1: ungedeckt", "column": {"id": 932, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "1", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_4nm20kwhlya_exp4uu0dq3u", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "2: teilweise gedeckt", "column": {"id": 933, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "2", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_0ioi5v04ut76_27p5z7sl5cy", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "3: gleichmässig gedeckt", "column": {"id": 934, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "3", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_qwul4v8exb9_oslt5bfwqv", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "4: stark gedeckt", "column": {"id": 935, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "4", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_6ixg10x016e_0a01jkv2udyi", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "5: überfett", "column": {"id": 936, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "5", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_kz1pmbxrz7_t5zs9zzmtms", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}], "orderby": [[{"label": "1: ungedeckt", "column": {"id": 932, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "1", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_4nm20kwhlya_exp4uu0dq3u", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, false]], "row_limit": 10000, "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "time_offsets": [], "custom_params": {}, "is_timeseries": true, "series_columns": [], "post_processing": [{"options": {"index": ["__timestamp"], "columns": [], "aggregates": {"1: ungedeckt": {"operator": "mean"}, "5: überfett": {"operator": "mean"}, "4: stark gedeckt": {"operator": "mean"}, "2: teilweise gedeckt": {"operator": "mean"}, "3: gleichmässig gedeckt": {"operator": "mean"}}, "drop_missing_columns": false}, "operation": "pivot"}, {"operation": "flatten"}], "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}, "series_limit_metric": []}], "form_data": {"force": false, "stack": "Expand", "groupby": [], "logAxis": false, "metrics": [{"label": "1: ungedeckt", "column": {"id": 932, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "1", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_4nm20kwhlya_exp4uu0dq3u", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "2: teilweise gedeckt", "column": {"id": 933, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "2", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_0ioi5v04ut76_27p5z7sl5cy", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "3: gleichmässig gedeckt", "column": {"id": 934, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "3", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_qwul4v8exb9_oslt5bfwqv", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "4: stark gedeckt", "column": {"id": 935, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "4", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_6ixg10x016e_0a01jkv2udyi", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "5: überfett", "column": {"id": 936, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "5", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_kz1pmbxrz7_t5zs9zzmtms", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}], "slice_id": 574, "viz_type": "echarts_timeseries_bar", "zoomable": true, "row_limit": 10000, "dashboards": [13], "datasource": "38__table", "legendType": "scroll", "only_total": true, "order_desc": true, "time_range": "No filter", "orientation": "vertical", "result_type": "full", "show_legend": true, "color_scheme": "supersetColors", "rich_tooltip": true, "rolling_type": null, "x_axis_title": "", "y_axis_title": "", "adhoc_filters": [], "result_format": "json", "truncateYAxis": false, "y_axis_bounds": [null, null], "y_axis_format": "SMART_NUMBER", "minorSplitLine": false, "comparison_type": "values", "extra_form_data": {}, "forecastPeriods": 10, "time_grain_sqla": "P1M", "truncate_metric": true, "contributionMode": null, "forecastInterval": 0.8, "granularity_sqla": "date_actual", "annotation_layers": [], "legendOrientation": "top", "tooltipTimeFormat": "%m/%Y", "show_empty_columns": true, "xAxisLabelRotation": 0, "x_axis_time_format": "smart_date", "x_axis_title_margin": 15, "y_axis_title_margin": 15, "y_axis_title_position": "Left", "timeseries_limit_metric": []}, "datasource": {"id": "5", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.298434	2023-12-20 15:07:29.298439	14	Verteilung geschlachteter Rinder nach Fleischigkeitsklasse CHTAX	table	fact_cattle_beefiness_fattissue	echarts_timeseries_bar	{"datasource": "38__table", "viz_type": "echarts_timeseries_bar", "slice_id": 573, "granularity_sqla": "date_actual", "time_grain_sqla": "P1M", "time_range": "No filter", "metrics": [{"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "c", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 931, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": true, "label": "1: sehr vollfleischig", "optionName": "metric_wbv7y6f5j6l_rtkmfkm29eb"}, {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "h", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 927, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": true, "label": "2: vollfleischig", "optionName": "metric_lscvtz4heph_9cwqwtm5eq5"}, {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "t", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 928, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": true, "label": "3: mittelfleischig", "optionName": "metric_6f6479ynatw_tq4ba2v4r9"}, {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "a", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 929, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": true, "label": "4: leerfleischig", "optionName": "metric_t385sqxkxjr_lens78bv93"}, {"expressionType": "SIMPLE", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "x", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 930, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "aggregate": "SUM", "sqlExpression": null, "datasourceWarning": false, "hasCustomLabel": true, "label": "5: sehr leerfleischig", "optionName": "metric_pod7jr68u9_5dxc9vh3x"}], "groupby": [], "contributionMode": "row", "adhoc_filters": [], "timeseries_limit_metric": [], "order_desc": true, "row_limit": 1000, "truncate_metric": true, "show_empty_columns": true, "rolling_type": null, "comparison_type": "values", "annotation_layers": [], "forecastEnabled": false, "forecastPeriods": 10, "forecastInterval": 0.8, "orientation": "vertical", "x_axis_title": "", "x_axis_title_margin": 15, "y_axis_title": "", "y_axis_title_margin": 15, "y_axis_title_position": "Left", "color_scheme": "supersetColors", "stack": true, "only_total": true, "zoomable": true, "show_legend": true, "legendType": "scroll", "legendOrientation": "top", "x_axis_time_format": "smart_date", "xAxisLabelRotation": 0, "y_axis_format": "SMART_NUMBER", "logAxis": false, "minorSplitLine": false, "truncateYAxis": false, "y_axis_bounds": [null, null], "rich_tooltip": true, "tooltipSortByMetric": false, "tooltipTimeFormat": "%m/%Y", "extra_form_data": {}, "dashboards": [13]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[fact_cattle_beefiness_fattissue](id:4)	4	[showcase_dd01_bieditor].[udm]	ed1a5228-482c-4e4b-ade5-84236f01788b	{"force": false, "queries": [{"extras": {"where": "", "having": "", "time_grain_sqla": "P1M"}, "columns": [], "filters": [], "metrics": [{"label": "1: sehr vollfleischig", "column": {"id": 931, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "c", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_wbv7y6f5j6l_rtkmfkm29eb", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "2: vollfleischig", "column": {"id": 927, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "h", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_lscvtz4heph_9cwqwtm5eq5", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "3: mittelfleischig", "column": {"id": 928, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "t", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_6f6479ynatw_tq4ba2v4r9", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "4: leerfleischig", "column": {"id": 929, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "a", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_t385sqxkxjr_lens78bv93", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "5: sehr leerfleischig", "column": {"id": 930, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "x", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_pod7jr68u9_5dxc9vh3x", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}], "orderby": [[{"label": "1: sehr vollfleischig", "column": {"id": 931, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "c", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_wbv7y6f5j6l_rtkmfkm29eb", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, false]], "row_limit": 1000, "order_desc": true, "time_range": "No filter", "url_params": {}, "granularity": "date_actual", "series_limit": 0, "time_offsets": [], "custom_params": {}, "is_timeseries": true, "series_columns": [], "post_processing": [{"options": {"index": ["__timestamp"], "columns": [], "aggregates": {"2: vollfleischig": {"operator": "mean"}, "4: leerfleischig": {"operator": "mean"}, "3: mittelfleischig": {"operator": "mean"}, "1: sehr vollfleischig": {"operator": "mean"}, "5: sehr leerfleischig": {"operator": "mean"}}, "drop_missing_columns": false}, "operation": "pivot"}, {"options": {"orientation": "row"}, "operation": "contribution"}, {"operation": "flatten"}], "custom_form_data": {}, "annotation_layers": [], "applied_time_extras": {}, "series_limit_metric": []}], "form_data": {"force": false, "stack": true, "groupby": [], "logAxis": false, "metrics": [{"label": "1: sehr vollfleischig", "column": {"id": 931, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "c", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_wbv7y6f5j6l_rtkmfkm29eb", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "2: vollfleischig", "column": {"id": 927, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "h", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_lscvtz4heph_9cwqwtm5eq5", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "3: mittelfleischig", "column": {"id": 928, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "t", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_6f6479ynatw_tq4ba2v4r9", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "4: leerfleischig", "column": {"id": 929, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "a", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_t385sqxkxjr_lens78bv93", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}, {"label": "5: sehr leerfleischig", "column": {"id": 930, "type": "DECIMAL", "groupby": true, "is_dttm": false, "expression": null, "filterable": true, "column_name": "x", "description": null, "certified_by": null, "is_certified": false, "type_generic": 0, "verbose_name": null, "warning_markdown": null, "advanced_data_type": null, "python_date_format": null, "certification_details": null}, "aggregate": "SUM", "optionName": "metric_pod7jr68u9_5dxc9vh3x", "sqlExpression": null, "expressionType": "SIMPLE", "hasCustomLabel": true, "datasourceWarning": false}], "slice_id": 573, "viz_type": "echarts_timeseries_bar", "zoomable": true, "row_limit": 1000, "dashboards": [13], "datasource": "38__table", "legendType": "scroll", "only_total": true, "order_desc": true, "time_range": "No filter", "orientation": "vertical", "result_type": "full", "show_legend": true, "color_scheme": "supersetColors", "rich_tooltip": true, "rolling_type": null, "x_axis_title": "", "y_axis_title": "", "adhoc_filters": [], "result_format": "json", "truncateYAxis": false, "y_axis_bounds": [null, null], "y_axis_format": "SMART_NUMBER", "minorSplitLine": false, "comparison_type": "values", "extra_form_data": {}, "forecastEnabled": false, "forecastPeriods": 10, "time_grain_sqla": "P1M", "truncate_metric": true, "contributionMode": "row", "forecastInterval": 0.8, "granularity_sqla": "date_actual", "annotation_layers": [], "legendOrientation": "top", "tooltipTimeFormat": "%m/%Y", "show_empty_columns": true, "xAxisLabelRotation": 0, "x_axis_time_format": "smart_date", "tooltipSortByMetric": false, "x_axis_title_margin": 15, "y_axis_title_margin": 15, "y_axis_title_position": "Left", "timeseries_limit_metric": []}, "datasource": {"id": "5", "type": "table"}, "result_type": "full", "result_format": "json"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.307438	2023-12-20 15:07:29.307443	15	Vollständige Liste Aktuelle Anzahl nach Tierart und Rasse	table	aktuelle_anzahl_tierart_rasse	table	{"datasource": "4__table", "viz_type": "table", "slice_id": 12, "granularity_sqla": "date_actual", "time_grain_sqla": "P1D", "time_range": "No filter", "query_mode": "raw", "groupby": [], "all_columns": ["species", "breed", "n_animals"], "percent_metrics": [], "adhoc_filters": [{"clause": "WHERE", "comparator": null, "datasourceWarning": false, "expressionType": "SIMPLE", "filterOptionName": "filter_ikc2t0cewij_iyd1snog2a", "isExtra": false, "isNew": false, "operator": "IS NOT NULL", "operatorId": "IS_NOT_NULL", "sqlExpression": null, "subject": "n_animals"}], "order_by_cols": ["[\\"n_animals\\", false]"], "row_limit": 1000, "server_page_length": 10, "order_desc": true, "table_timestamp_format": "smart_date", "page_length": 0, "include_search": true, "show_cell_bars": false, "color_pn": false, "column_config": {"n_animals": {"alignPositiveNegative": false, "colorPositiveNegative": false, "d3NumberFormat": ",d", "d3SmallNumberFormat": ",d", "showCellBars": true}}, "conditional_formatting": [{"colorScheme": "#EFA1AA", "column": "n_animals", "operator": "\\u2264 x <", "targetValueLeft": "0", "targetValueRight": "10000"}, {"colorScheme": "#FDE380", "column": "n_animals", "operator": "\\u2264 x <", "targetValueLeft": "10000", "targetValueRight": "100000"}, {"colorScheme": "#ACE1C4", "column": "n_animals", "operator": "\\u2265", "targetValue": 100000}], "extra_form_data": {}, "dashboards": [1]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)	1	[showcase_dd01_bieditor].[udm]	bc7e2593-ae99-405c-b724-1475356883d6	{"datasource":{"id":4,"type":"table"},"force":false,"queries":[{"time_range":"No filter","granularity":"date_actual","filters":[{"col":"n_animals","op":"IS NOT NULL"}],"extras":{"time_grain_sqla":"P1D","having":"","where":""},"applied_time_extras":{},"columns":["species","breed","n_animals"],"orderby":[["n_animals",false]],"annotation_layers":[],"row_limit":1000,"series_limit":0,"order_desc":true,"url_params":{},"custom_params":{},"custom_form_data":{},"post_processing":[]}],"form_data":{"datasource":"4__table","viz_type":"table","slice_id":12,"granularity_sqla":"date_actual","time_grain_sqla":"P1D","time_range":"No filter","query_mode":"raw","groupby":[],"all_columns":["species","breed","n_animals"],"percent_metrics":[],"adhoc_filters":[{"clause":"WHERE","comparator":null,"datasourceWarning":false,"expressionType":"SIMPLE","filterOptionName":"filter_ikc2t0cewij_iyd1snog2a","isExtra":false,"isNew":false,"operator":"IS NOT NULL","operatorId":"IS_NOT_NULL","sqlExpression":null,"subject":"n_animals"}],"order_by_cols":["[\\"n_animals\\", false]"],"row_limit":1000,"server_page_length":10,"include_time":false,"order_desc":true,"table_timestamp_format":"smart_date","page_length":0,"include_search":true,"show_cell_bars":false,"color_pn":false,"column_config":{"n_animals":{"alignPositiveNegative":false,"colorPositiveNegative":false,"d3NumberFormat":",d","d3SmallNumberFormat":",d","showCellBars":true}},"conditional_formatting":[{"colorScheme":"#EFA1AA","column":"n_animals","operator":"≤ x <","targetValueLeft":"0","targetValueRight":"10000"},{"colorScheme":"#FDE380","column":"n_animals","operator":"≤ x <","targetValueLeft":"10000","targetValueRight":"100000"},{"colorScheme":"#ACE1C4","column":"n_animals","operator":"≥","targetValue":100000}],"extra_form_data":{},"dashboards":[1],"force":false,"result_format":"json","result_type":"full"},"result_format":"json","result_type":"full"}	\N	\N	\N	\N	f	\N
2023-12-20 15:07:29.315964	2023-12-20 15:07:29.315968	16	Zeitverlauf Anzahl Tiere nach Tierart (separiert)	table	anzahl_tiere_art_rasse_datum	time_table	{"datasource": "1__table", "viz_type": "time_table", "slice_id": 3, "granularity_sqla": "date_actual", "time_grain_sqla": "P1M", "time_range": "No filter", "metrics": [{"aggregate": "SUM", "column": {"advanced_data_type": null, "certification_details": null, "certified_by": null, "column_name": "n_animals", "description": null, "expression": null, "filterable": true, "groupby": true, "id": 834, "is_certified": false, "is_dttm": false, "python_date_format": null, "type": "DECIMAL", "type_generic": 0, "verbose_name": null, "warning_markdown": null}, "datasourceWarning": false, "expressionType": "SIMPLE", "hasCustomLabel": true, "label": "Anzahl Tiere", "optionName": "metric_v1ez6h8vp8c_rnd9tltdks", "sqlExpression": null}], "adhoc_filters": [], "groupby": ["species"], "limit": 10, "column_collection": [{"bounds": [null, null], "colType": "spark", "comparisonType": "", "d3format": "", "dateFormat": "", "height": "", "key": "fa-fcQbC_", "label": "Anzahl Tiere", "popoverVisible": true, "showYAxis": false, "timeLag": 0, "timeRatio": "", "tooltip": "", "width": "", "yAxisBounds": [null, null]}], "row_limit": 10000, "extra_form_data": {}, "dashboards": [1]}	\N	\N	\N	\N	[showcase_dd01_bieditor].[anzahl_tiere_art_rasse_datum](id:2)	2	[showcase_dd01_bieditor].[udm]	f88aa526-6e9f-4ffa-a4d7-697aa16e013c	{"datasource":{"id":1,"type":"table"},"force":false,"queries":[{"time_range":"No filter","granularity":"date_actual","filters":[],"extras":{"time_grain_sqla":"P1M","having":"","where":""},"applied_time_extras":{},"columns":["species"],"metrics":[{"aggregate":"SUM","column":{"advanced_data_type":null,"certification_details":null,"certified_by":null,"column_name":"n_animals","description":null,"expression":null,"filterable":true,"groupby":true,"id":834,"is_certified":false,"is_dttm":false,"python_date_format":null,"type":"DECIMAL","type_generic":0,"verbose_name":null,"warning_markdown":null},"datasourceWarning":false,"expressionType":"SIMPLE","hasCustomLabel":true,"label":"Anzahl Tiere","optionName":"metric_v1ez6h8vp8c_rnd9tltdks","sqlExpression":null}],"annotation_layers":[],"row_limit":10000,"series_limit":10,"order_desc":true,"url_params":{},"custom_params":{},"custom_form_data":{}}],"form_data":{"datasource":"1__table","viz_type":"time_table","slice_id":3,"granularity_sqla":"date_actual","time_grain_sqla":"P1M","time_range":"No filter","metrics":[{"aggregate":"SUM","column":{"advanced_data_type":null,"certification_details":null,"certified_by":null,"column_name":"n_animals","description":null,"expression":null,"filterable":true,"groupby":true,"id":834,"is_certified":false,"is_dttm":false,"python_date_format":null,"type":"DECIMAL","type_generic":0,"verbose_name":null,"warning_markdown":null},"datasourceWarning":false,"expressionType":"SIMPLE","hasCustomLabel":true,"label":"Anzahl Tiere","optionName":"metric_v1ez6h8vp8c_rnd9tltdks","sqlExpression":null}],"adhoc_filters":[],"groupby":["species"],"limit":10,"column_collection":[{"bounds":[null,null],"colType":"spark","comparisonType":"","d3format":"","dateFormat":"","height":"","key":"fa-fcQbC_","label":"Anzahl Tiere","popoverVisible":true,"showYAxis":false,"timeLag":0,"timeRatio":"","tooltip":"","width":"","yAxisBounds":[null,null]}],"row_limit":10000,"extra_form_data":{},"dashboards":[1],"force":false,"result_format":"json","result_type":"full"},"result_format":"json","result_type":"full"}	\N	\N	\N	\N	f	\N
\.


--
-- Data for Name: sql_metrics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sql_metrics (created_on, changed_on, id, metric_name, verbose_name, metric_type, table_id, expression, description, created_by_fk, changed_by_fk, d3format, warning_text, extra, uuid) FROM stdin;
2023-12-20 15:07:28.816974	2023-12-20 15:07:28.816976	1	count	\N	\N	1	count(*)	\N	\N	\N	\N	\N	{"warning_markdown": ""}	86105840-395d-4e8b-91dc-796abc6bdd65
2023-12-20 15:07:28.965954	2023-12-20 15:07:28.965958	2	count	\N	\N	2	count(*)	\N	\N	\N	\N	\N	{"warning_markdown": ""}	b6a4d5a8-f687-409a-a699-57c5f58a639f
2023-12-20 15:07:29.003449	2023-12-20 15:07:29.003452	3	count	\N	\N	3	count(*)	\N	\N	\N	\N	\N	{"warning_markdown": ""}	14c9fe71-be39-433e-bda5-d20e03ca9ee0
2023-12-20 15:07:29.041321	2023-12-20 15:07:29.041324	4	count	\N	\N	4	count(*)	\N	\N	\N	\N	\N	{"warning_markdown": ""}	bea256b3-d4cc-437f-ba3c-d5bc01210aac
2023-12-20 15:07:29.093505	2023-12-20 15:07:29.093508	5	count	\N	\N	5	count(*)	\N	\N	\N	\N	\N	{"warning_markdown": ""}	d5544fc7-a772-42fe-81f6-33350a390b0a
2023-12-20 15:07:29.139748	2023-12-20 15:07:29.139753	6	count	\N	\N	6	count(*)	\N	\N	\N	\N	\N	{"warning_markdown": ""}	be1b81d9-d2bd-42f3-8693-c1b72e5f4d2e
\.


--
-- Data for Name: sql_observations; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sql_observations (id, dttm, alert_id, value, error_msg) FROM stdin;
\.


--
-- Data for Name: sqlatable_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sqlatable_user (id, user_id, table_id) FROM stdin;
\.


--
-- Data for Name: ssh_tunnels; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ssh_tunnels (created_on, changed_on, created_by_fk, changed_by_fk, extra_json, uuid, id, database_id, server_address, server_port, username, password, private_key, private_key_password) FROM stdin;
\.


--
-- Data for Name: tab_state; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tab_state (created_on, changed_on, extra_json, id, user_id, label, active, database_id, schema, sql, query_limit, latest_query_id, autorun, template_params, created_by_fk, changed_by_fk, hide_left_bar, saved_query_id) FROM stdin;
\.


--
-- Data for Name: table_columns; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.table_columns (created_on, changed_on, id, table_id, column_name, is_dttm, is_active, type, groupby, filterable, description, created_by_fk, changed_by_fk, expression, verbose_name, python_date_format, uuid, extra, advanced_data_type) FROM stdin;
2023-12-20 15:07:28.827097	2023-12-20 15:07:28.8271	1	1	n_animals	f	t	DECIMAL	t	t	\N	\N	\N	\N	Anzahl Tiere	\N	a7a1ef45-3067-43cd-a392-3f6cb0a2c00a	{}	\N
2023-12-20 15:07:28.830028	2023-12-20 15:07:28.830031	2	1	breed	f	t	STRING	t	t	\N	\N	\N	\N	Rasse	\N	fe4e9684-28e8-494b-a565-3f5d486a7424	{}	\N
2023-12-20 15:07:28.832318	2023-12-20 15:07:28.832321	3	1	species	f	t	STRING	t	t	\N	\N	\N	\N	Tierart	\N	5d79e14e-4136-441a-af29-5ab71b3ba281	{}	\N
2023-12-20 15:07:28.834479	2023-12-20 15:07:28.834482	4	1	year	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	5f2de1e1-6b14-4213-9b4d-7d1d7f7539b4	{}	\N
2023-12-20 15:07:28.836698	2023-12-20 15:07:28.8367	5	1	date_actual	t	t	DATE	t	t	\N	\N	\N	\N	\N	\N	872338ce-21bc-4b75-ac71-b30c930910ba	{}	\N
2023-12-20 15:07:28.839709	2023-12-20 15:07:28.839713	6	1	dd_mm_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	7c2f0cb1-cdea-4e2a-ad18-87bdeb20546d	{}	\N
2023-12-20 15:07:28.842627	2023-12-20 15:07:28.842631	7	1	mm_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	cf2bab52-8c70-418d-88eb-693389498bf5	{}	\N
2023-12-20 15:07:28.847383	2023-12-20 15:07:28.847388	8	1	qq_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	b8b05163-7d13-4e8c-9cf3-5b2e3b664927	{}	\N
2023-12-20 15:07:28.968457	2023-12-20 15:07:28.96846	9	2	breed	f	t	STRING	t	t	\N	\N	\N	\N	Rasse	\N	837b316f-7d4c-4b5e-82ae-4ca6759dc3ca	{"warning_markdown": null}	\N
2023-12-20 15:07:28.971068	2023-12-20 15:07:28.971072	10	2	species	f	t	STRING	t	t	\N	\N	\N	\N	Tierart	\N	e40644cd-fa47-4d73-a454-092331ae251e	{"warning_markdown": null}	\N
2023-12-20 15:07:28.973735	2023-12-20 15:07:28.973739	11	2	n_animals	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	97722d94-4254-486e-9aed-3cbbeb934c8e	{"warning_markdown": null}	\N
2023-12-20 15:07:28.976174	2023-12-20 15:07:28.976177	12	2	year	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	d51bfcbf-aa63-4c6f-8557-7ca3b3cc9e24	{"warning_markdown": null}	\N
2023-12-20 15:07:28.978437	2023-12-20 15:07:28.97844	13	2	date_actual	t	t	DATE	t	t	\N	\N	\N	\N	\N	\N	9598a5ab-4985-42c7-ad64-682fb21ae606	{"warning_markdown": null}	\N
2023-12-20 15:07:28.980687	2023-12-20 15:07:28.98069	14	2	dd_mm_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	37aa76ca-a055-4e99-95cb-f6ae9eeafef1	{"warning_markdown": null}	\N
2023-12-20 15:07:28.98282	2023-12-20 15:07:28.982822	15	2	mm_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	3d7dd9f7-5c82-49ab-b1cf-5dc09cc345e1	{"warning_markdown": null}	\N
2023-12-20 15:07:28.984914	2023-12-20 15:07:28.98492	16	2	qq_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	09d23b34-569f-4e72-8e4b-3a372eef7e4b	{"warning_markdown": null}	\N
2023-12-20 15:07:29.006157	2023-12-20 15:07:29.006161	17	3	n_animals	f	t	DECIMAL	t	t	\N	\N	\N	\N	Anzahl Tiere	\N	3c958fab-cb3c-4a34-a1b8-87687d32388c	{}	\N
2023-12-20 15:07:29.008772	2023-12-20 15:07:29.008775	18	3	year	f	t	DECIMAL	t	t	\N	\N	\N	\N	Jahr	\N	2a60c63b-f372-4921-9a8a-c6001b2b6415	{}	\N
2023-12-20 15:07:29.011228	2023-12-20 15:07:29.01123	19	3	month	f	t	INTEGER	t	t	\N	\N	\N	\N	Monat	\N	4b9b5ede-c059-40b3-9569-45580d6a9fbf	{}	\N
2023-12-20 15:07:29.013584	2023-12-20 15:07:29.013587	20	3	canton	f	t	STRING	t	t	\N	\N	\N	\N	Kanton	\N	f9bc968b-601f-4fce-84d9-8657e5b51f6b	{}	\N
2023-12-20 15:07:29.015799	2023-12-20 15:07:29.015802	21	3	species	f	t	STRING	t	t	\N	\N	\N	\N	Tierart	\N	a6f16955-1d18-4b08-9d3d-ef1c1d3d395a	{}	\N
2023-12-20 15:07:29.017974	2023-12-20 15:07:29.017977	22	3	date_actual	t	t	DATE	t	t	\N	\N	\N	\N	\N	\N	a2ef3e42-2834-4d8c-bc0c-658fb7a7399e	{}	\N
2023-12-20 15:07:29.020171	2023-12-20 15:07:29.020173	23	3	mm_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	0158894c-721e-49d5-aa72-e7e514756e10	{}	\N
2023-12-20 15:07:29.022564	2023-12-20 15:07:29.022567	24	3	qq_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	403e3ed7-7bdf-42da-9640-9325df04b192	{}	\N
2023-12-20 15:07:29.043698	2023-12-20 15:07:29.0437	25	4	1	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	c882c711-592b-4628-aa02-4770fb1993cb	{}	\N
2023-12-20 15:07:29.046041	2023-12-20 15:07:29.046044	26	4	2	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	43daf930-0705-485d-8dcf-3bbab592030c	{}	\N
2023-12-20 15:07:29.048371	2023-12-20 15:07:29.048374	27	4	3	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	dc67ddea-d1f4-414f-b7ab-38829dd0ab7a	{}	\N
2023-12-20 15:07:29.050768	2023-12-20 15:07:29.050771	28	4	4	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	99e588e1-178a-4bd4-a672-1e01126de46e	{}	\N
2023-12-20 15:07:29.052999	2023-12-20 15:07:29.053002	29	4	5	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	cd741275-a1bb-4b76-b7cc-0facecbb63f9	{}	\N
2023-12-20 15:07:29.055666	2023-12-20 15:07:29.055669	30	4	year	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	19c88eb6-2b90-4ed5-8d46-90076c26451b	{}	\N
2023-12-20 15:07:29.057976	2023-12-20 15:07:29.057978	31	4	a	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	a2768b4e-4fc7-4c9f-9665-40598331548b	{}	\N
2023-12-20 15:07:29.060289	2023-12-20 15:07:29.060291	32	4	c	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	959c6a54-038e-4997-8a45-a0597b0aac37	{}	\N
2023-12-20 15:07:29.0625	2023-12-20 15:07:29.062503	33	4	h	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	60b0ed7f-ba4d-46cd-96ec-b40bd75163c8	{}	\N
2023-12-20 15:07:29.06482	2023-12-20 15:07:29.064823	34	4	t	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	6c7d77aa-f16c-4de2-8cb6-1a5a190bdad4	{}	\N
2023-12-20 15:07:29.067206	2023-12-20 15:07:29.067209	35	4	x	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	6376a999-3881-4ee8-84cd-738693b58060	{}	\N
2023-12-20 15:07:29.069448	2023-12-20 15:07:29.06945	36	4	date_actual	t	t	DATE	t	t	\N	\N	\N	\N	\N	\N	de53fe95-c408-4200-8023-9014ccd8f979	{}	\N
2023-12-20 15:07:29.072069	2023-12-20 15:07:29.072072	37	4	mm_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	7fb7a74e-5be5-4667-81ec-6486972cc55f	{}	\N
2023-12-20 15:07:29.074538	2023-12-20 15:07:29.07454	38	4	qq_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	084821ea-ab71-49e9-88ca-474f1af77c59	{}	\N
2023-12-20 15:07:29.095689	2023-12-20 15:07:29.095692	39	5	import_swiss_ear_tag	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	53ff649b-6380-4e08-8e24-b4f0bc091875	{}	\N
2023-12-20 15:07:29.097876	2023-12-20 15:07:29.097878	40	5	yard_slaughter	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	3218c633-eb47-42b4-933a-679cfe712094	{}	\N
2023-12-20 15:07:29.099998	2023-12-20 15:07:29.100001	41	5	death	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	a7842a6d-a078-4637-bb1d-0754bacf2069	{}	\N
2023-12-20 15:07:29.102111	2023-12-20 15:07:29.102113	42	5	slaugthering	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	ffa4fd35-2cc2-4300-818d-726f38c0374a	{}	\N
2023-12-20 15:07:29.104189	2023-12-20 15:07:29.104192	43	5	year	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	16e95b8d-8b38-48e1-9fd0-8f2a8d2eb547	{}	\N
2023-12-20 15:07:29.107306	2023-12-20 15:07:29.10731	44	5	birth	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	58583cfc-dd56-4cf5-8af4-8b46079b456a	{}	\N
2023-12-20 15:07:29.109656	2023-12-20 15:07:29.109658	45	5	export	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	517ead79-8877-4d76-b683-a5ee6d0107af	{}	\N
2023-12-20 15:07:29.111852	2023-12-20 15:07:29.111854	46	5	import	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	ac3ad58a-cdd2-41e2-ae23-798cd880df5a	{}	\N
2023-12-20 15:07:29.114083	2023-12-20 15:07:29.114086	47	5	date_actual	t	t	DATE	t	t	\N	\N	\N	\N	\N	\N	635b3d7a-de37-4937-991c-380821ffd294	{}	\N
2023-12-20 15:07:29.116239	2023-12-20 15:07:29.116242	48	5	mm_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	65988ef2-4945-488d-841a-1da3d49eb0c0	{}	\N
2023-12-20 15:07:29.118403	2023-12-20 15:07:29.118406	49	5	qq_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	238e633b-6070-442c-a49f-25fb80d9ee2e	{}	\N
2023-12-20 15:07:29.142512	2023-12-20 15:07:29.142516	50	6	n_animals	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	8e377cc4-0435-4fe9-be12-62c1d6ed7240	{}	\N
2023-12-20 15:07:29.144917	2023-12-20 15:07:29.14492	51	6	year	f	t	DECIMAL	t	t	\N	\N	\N	\N	\N	\N	e604bbb8-2d45-46d1-ba81-a2f84a9db24f	{}	\N
2023-12-20 15:07:29.147397	2023-12-20 15:07:29.1474	52	6	date_actual	t	t	DATE	t	t	\N	\N	\N	\N	\N	\N	fbaba49d-1a41-411c-9777-9a29faa9ba8b	{}	\N
2023-12-20 15:07:29.149619	2023-12-20 15:07:29.149622	53	6	mm_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	21ef41f9-e625-413c-b4d2-e9508a02976f	{}	\N
2023-12-20 15:07:29.151856	2023-12-20 15:07:29.151859	54	6	qq_yyyy	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	24128a23-e0e8-45fa-af66-f4a776721704	{}	\N
2023-12-20 15:07:29.154051	2023-12-20 15:07:29.154054	55	6	age	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	4df0882a-d709-4ffd-9c22-1b712273f432	{}	\N
2023-12-20 15:07:29.15656	2023-12-20 15:07:29.156563	56	6	population	f	t	STRING	t	t	\N	\N	\N	\N	\N	\N	23275887-d39a-4f41-9c3f-866dfc5fc57f	{}	\N
\.


--
-- Data for Name: table_schema; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.table_schema (created_on, changed_on, extra_json, id, tab_state_id, database_id, schema, "table", description, expanded, created_by_fk, changed_by_fk) FROM stdin;
\.


--
-- Data for Name: tables; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tables (created_on, changed_on, id, table_name, main_dttm_col, default_endpoint, database_id, created_by_fk, changed_by_fk, "offset", description, is_featured, cache_timeout, schema, sql, params, perm, filter_select_enabled, fetch_values_predicate, is_sqllab_view, template_params, schema_perm, extra, uuid, is_managed_externally, external_url) FROM stdin;
2023-12-20 15:07:28.794581	2023-12-20 15:07:28.805982	1	aktuelle_anzahl_tierart_rasse	\N	\N	1	\N	\N	0	\N	f	0	udm	with\n\ncurrent_n_animals as (\n    SELECT\n\t\t  time_id,\n\t\t  species,\n\t\t  breed,\n\t\t  n_animals\n    FROM \tudm.fact_breeds_long\n    WHERE time_id = (select max(time_id) from udm.fact_breeds_long)\n      and n_animals is not null\n      and n_animals != 0\n)\n\n\nSELECT\n    dti.date_actual,\n\t\tdti.year,\n    dti.mm_yyyy,\n    dti.dd_mm_yyyy,\n    dti.qq_yyyy,\n\t\tcna.species,\n\t\tcna.breed,\n\t\tcna.n_animals\nFROM      current_n_animals cna\nLEFT JOIN udm.dim_time      dti on cna.time_id = dti.id	\N	[showcase_dd01_bieditor].[aktuelle_anzahl_tierart_rasse](id:1)	f	\N	f	\N	[showcase_dd01_bieditor].[udm]	\N	e11e92ee-55bc-4c6b-9848-2526e01b119f	f	\N
2023-12-20 15:07:28.955266	2023-12-20 15:07:28.962894	2	anzahl_tiere_art_rasse_datum	\N	\N	1	\N	\N	0	\N	f	0	udm	SELECT\r\n    d.date_actual,\r\n\t\td."year",\r\n    d.mm_yyyy,\r\n    d.dd_mm_yyyy,\r\n    d.qq_yyyy,\r\n\t\tf.species,\r\n\t\tf.breed,\r\n\t\tf.n_animals\r\nfrom \tudm.fact_breeds_long \tf\r\njoin \tudm.dim_time\t\t\td\r\non\t\tf.time_id = d.id;	\N	[showcase_dd01_bieditor].[anzahl_tiere_art_rasse_datum](id:2)	f	\N	f	\N	[showcase_dd01_bieditor].[udm]	\N	c94c656a-f38a-47a9-a14d-443cb77852be	f	\N
2023-12-20 15:07:28.994341	2023-12-20 15:07:29.000665	3	anzahl_tiere_kanton_tierart	date_actual	\N	1	\N	\N	0	\N	f	0	udm	with\r\n\r\ncanton_species as (\r\n    SELECT\r\n\t\t  time_id,\r\n\t\t  species,\r\n    \tcanton,\r\n    \tn_animals\r\n    FROM \tudm.fact_canton_long\r\n)\r\n\r\n\r\nSELECT\r\n    d.date_actual,\r\n    d."year",\r\n    d.mm_yyyy,\r\n    EXTRACT('month' from d.date_actual)::INTEGER as month,\r\n    d.qq_yyyy,\r\n    f.species,\r\n    f.canton,\r\n    f.n_animals\r\n    \r\nFROM \t    canton_species f\r\nLEFT JOIN udm.dim_time   d on\t\tf.time_id = d.id\r\n	\N	[showcase_dd01_bieditor].[anzahl_tiere_kanton_tierart](id:3)	f	\N	f	\N	[showcase_dd01_bieditor].[udm]	\N	abec9599-a2f4-4b84-aaa3-fa7d2f0af4a9	f	\N
2023-12-20 15:07:29.031652	2023-12-20 15:07:29.038216	4	fact_cattle_beefiness_fattissue	\N	\N	1	\N	\N	0	\N	f	0	udm	select  d.date_actual,\r\n    \t\td."year",\r\n        d.mm_yyyy,\r\n        d.qq_yyyy,\r\n        f.h,\r\n        f.t,\r\n        f.a,\r\n        f.x,\r\n        f.c,\r\n        f."1",\r\n        f."2",\r\n        f."3",\r\n        f."4",\r\n        f."5"\r\nfrom\tudm.fact_cattle_beefiness_fattissue\tf\r\njoin\tudm.dim_time \t\t\t\t\t\t            d \r\non\t\tf.time_id = d.id	\N	[showcase_dd01_bieditor].[fact_cattle_beefiness_fattissue](id:4)	f	\N	f	\N	[showcase_dd01_bieditor].[udm]	\N	107c297b-16b2-49e2-8933-324ce8b09f36	f	\N
2023-12-20 15:07:29.083332	2023-12-20 15:07:29.090735	5	fact_cattle_popvariations	\N	\N	1	\N	\N	0	\N	f	0	udm	select  d.date_actual,\r\n    \t\td."year",\r\n        d.mm_yyyy,\r\n        d.qq_yyyy,\r\n        f."import",\r\n    \t\tf.birth,\r\n    \t\tf.import_swiss_ear_tag,\r\n    \t\tf.slaugthering,\r\n    \t\tf.death,\r\n    \t\tf.yard_slaughter,\r\n    \t\tf.export\r\nfrom \tudm.fact_cattle_popvariations \tf\r\njoin \tudm.dim_time\t\t\td\r\non\t\tf.time_id = d.id	\N	[showcase_dd01_bieditor].[fact_cattle_popvariations](id:5)	f	\N	f	\N	[showcase_dd01_bieditor].[udm]	\N	b80c7d6a-2798-4005-9a4c-df0483ed6237	f	\N
2023-12-20 15:07:29.128851	2023-12-20 15:07:29.135537	6	fact_cattle_pyr_long	\N	\N	1	\N	\N	0	\N	f	0	udm	select \t \td.date_actual,\r\n    \t\t\td."year",\r\n    \t\t\td.mm_yyyy,\r\n    \t\t\td.qq_yyyy,\r\n    \t\t\tf.population,\r\n    \t\t\tf.age,\r\n    \t\t\tf.n_animals \r\nfrom \t\tudm.fact_cattle_pyr_long \tf\r\njoin\t\tudm.dim_time \t\t\t\t      d \r\non\t\t\tf.time_id = d.id	\N	[showcase_dd01_bieditor].[fact_cattle_pyr_long](id:6)	f	\N	f	\N	[showcase_dd01_bieditor].[udm]	\N	33794a7e-64f7-49f4-acc5-af3364a27c66	f	\N
\.


--
-- Data for Name: tag; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tag (created_on, changed_on, id, name, type, created_by_fk, changed_by_fk) FROM stdin;
\.


--
-- Data for Name: tagged_object; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tagged_object (created_on, changed_on, id, tag_id, object_id, object_type, created_by_fk, changed_by_fk) FROM stdin;
\.


--
-- Data for Name: url; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.url (created_on, changed_on, id, url, created_by_fk, changed_by_fk) FROM stdin;
\.


--
-- Data for Name: user_attribute; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_attribute (created_on, changed_on, id, user_id, welcome_dashboard_id, created_by_fk, changed_by_fk) FROM stdin;
\.


--
-- Name: ab_permission_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ab_permission_id_seq', 103, true);


--
-- Name: ab_permission_view_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ab_permission_view_id_seq', 238, true);


--
-- Name: ab_permission_view_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ab_permission_view_role_id_seq', 704, true);


--
-- Name: ab_register_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ab_register_user_id_seq', 1, false);


--
-- Name: ab_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ab_role_id_seq', 10, true);


--
-- Name: ab_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ab_user_id_seq', 2, true);


--
-- Name: ab_user_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ab_user_role_id_seq', 2, true);


--
-- Name: ab_view_menu_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ab_view_menu_id_seq', 95, true);


--
-- Name: access_request_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.access_request_id_seq', 1, false);


--
-- Name: alert_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.alert_logs_id_seq', 1, false);


--
-- Name: alert_owner_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.alert_owner_id_seq', 1, false);


--
-- Name: alerts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.alerts_id_seq', 1, false);


--
-- Name: annotation_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.annotation_id_seq', 1, false);


--
-- Name: annotation_layer_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.annotation_layer_id_seq', 1, false);


--
-- Name: cache_keys_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.cache_keys_id_seq', 1, false);


--
-- Name: clusters_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.clusters_id_seq', 1, false);


--
-- Name: columns_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.columns_id_seq', 1, false);


--
-- Name: css_templates_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.css_templates_id_seq', 1, false);


--
-- Name: dashboard_email_schedules_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.dashboard_email_schedules_id_seq', 1, false);


--
-- Name: dashboard_roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.dashboard_roles_id_seq', 1, true);


--
-- Name: dashboard_slices_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.dashboard_slices_id_seq', 16, true);


--
-- Name: dashboard_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.dashboard_user_id_seq', 1, false);


--
-- Name: dashboards_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.dashboards_id_seq', 1, true);


--
-- Name: datasources_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.datasources_id_seq', 1, false);


--
-- Name: dbs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.dbs_id_seq', 1, true);


--
-- Name: druiddatasource_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.druiddatasource_user_id_seq', 1, false);


--
-- Name: dynamic_plugin_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.dynamic_plugin_id_seq', 1, false);


--
-- Name: favstar_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.favstar_id_seq', 1, false);


--
-- Name: filter_sets_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.filter_sets_id_seq', 1, false);


--
-- Name: key_value_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.key_value_id_seq', 1, false);


--
-- Name: keyvalue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.keyvalue_id_seq', 1, false);


--
-- Name: logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.logs_id_seq', 10, true);


--
-- Name: metrics_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.metrics_id_seq', 1, false);


--
-- Name: query_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.query_id_seq', 1, false);


--
-- Name: report_execution_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.report_execution_log_id_seq', 1, false);


--
-- Name: report_recipient_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.report_recipient_id_seq', 1, false);


--
-- Name: report_schedule_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.report_schedule_id_seq', 1, false);


--
-- Name: report_schedule_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.report_schedule_user_id_seq', 1, false);


--
-- Name: rls_filter_roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.rls_filter_roles_id_seq', 1, false);


--
-- Name: rls_filter_tables_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.rls_filter_tables_id_seq', 1, false);


--
-- Name: row_level_security_filters_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.row_level_security_filters_id_seq', 1, false);


--
-- Name: saved_query_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.saved_query_id_seq', 1, false);


--
-- Name: sl_columns_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sl_columns_id_seq', 1, false);


--
-- Name: sl_datasets_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sl_datasets_id_seq', 1, false);


--
-- Name: sl_tables_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sl_tables_id_seq', 1, false);


--
-- Name: slice_email_schedules_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.slice_email_schedules_id_seq', 1, false);


--
-- Name: slice_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.slice_user_id_seq', 1, false);


--
-- Name: slices_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.slices_id_seq', 16, true);


--
-- Name: sql_metrics_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sql_metrics_id_seq', 6, true);


--
-- Name: sql_observations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sql_observations_id_seq', 1, false);


--
-- Name: sqlatable_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sqlatable_user_id_seq', 1, false);


--
-- Name: ssh_tunnels_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ssh_tunnels_id_seq', 1, false);


--
-- Name: tab_state_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tab_state_id_seq', 1, false);


--
-- Name: table_columns_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.table_columns_id_seq', 56, true);


--
-- Name: table_schema_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.table_schema_id_seq', 1, false);


--
-- Name: tables_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tables_id_seq', 6, true);


--
-- Name: tag_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tag_id_seq', 1, false);


--
-- Name: tagged_object_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tagged_object_id_seq', 1, false);


--
-- Name: url_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.url_id_seq', 1, false);


--
-- Name: user_attribute_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_attribute_id_seq', 1, false);


--
-- Name: tables _customer_location_uc; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tables
    ADD CONSTRAINT _customer_location_uc UNIQUE (database_id, schema, table_name);


--
-- Name: ab_permission ab_permission_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_permission
    ADD CONSTRAINT ab_permission_name_key UNIQUE (name);


--
-- Name: ab_permission ab_permission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_permission
    ADD CONSTRAINT ab_permission_pkey PRIMARY KEY (id);


--
-- Name: ab_permission_view ab_permission_view_permission_id_view_menu_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_permission_view
    ADD CONSTRAINT ab_permission_view_permission_id_view_menu_id_key UNIQUE (permission_id, view_menu_id);


--
-- Name: ab_permission_view ab_permission_view_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_permission_view
    ADD CONSTRAINT ab_permission_view_pkey PRIMARY KEY (id);


--
-- Name: ab_permission_view_role ab_permission_view_role_permission_view_id_role_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_permission_view_role
    ADD CONSTRAINT ab_permission_view_role_permission_view_id_role_id_key UNIQUE (permission_view_id, role_id);


--
-- Name: ab_permission_view_role ab_permission_view_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_permission_view_role
    ADD CONSTRAINT ab_permission_view_role_pkey PRIMARY KEY (id);


--
-- Name: ab_register_user ab_register_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_register_user
    ADD CONSTRAINT ab_register_user_pkey PRIMARY KEY (id);


--
-- Name: ab_register_user ab_register_user_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_register_user
    ADD CONSTRAINT ab_register_user_username_key UNIQUE (username);


--
-- Name: ab_role ab_role_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_role
    ADD CONSTRAINT ab_role_name_key UNIQUE (name);


--
-- Name: ab_role ab_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_role
    ADD CONSTRAINT ab_role_pkey PRIMARY KEY (id);


--
-- Name: ab_user ab_user_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_user
    ADD CONSTRAINT ab_user_email_key UNIQUE (email);


--
-- Name: ab_user ab_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_user
    ADD CONSTRAINT ab_user_pkey PRIMARY KEY (id);


--
-- Name: ab_user_role ab_user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_user_role
    ADD CONSTRAINT ab_user_role_pkey PRIMARY KEY (id);


--
-- Name: ab_user_role ab_user_role_user_id_role_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_user_role
    ADD CONSTRAINT ab_user_role_user_id_role_id_key UNIQUE (user_id, role_id);


--
-- Name: ab_user ab_user_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_user
    ADD CONSTRAINT ab_user_username_key UNIQUE (username);


--
-- Name: ab_view_menu ab_view_menu_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_view_menu
    ADD CONSTRAINT ab_view_menu_name_key UNIQUE (name);


--
-- Name: ab_view_menu ab_view_menu_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_view_menu
    ADD CONSTRAINT ab_view_menu_pkey PRIMARY KEY (id);


--
-- Name: access_request access_request_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.access_request
    ADD CONSTRAINT access_request_pkey PRIMARY KEY (id);


--
-- Name: alembic_version alembic_version_pkc; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alembic_version
    ADD CONSTRAINT alembic_version_pkc PRIMARY KEY (version_num);


--
-- Name: alert_logs alert_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alert_logs
    ADD CONSTRAINT alert_logs_pkey PRIMARY KEY (id);


--
-- Name: alert_owner alert_owner_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alert_owner
    ADD CONSTRAINT alert_owner_pkey PRIMARY KEY (id);


--
-- Name: alerts alerts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT alerts_pkey PRIMARY KEY (id);


--
-- Name: annotation_layer annotation_layer_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.annotation_layer
    ADD CONSTRAINT annotation_layer_pkey PRIMARY KEY (id);


--
-- Name: annotation annotation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.annotation
    ADD CONSTRAINT annotation_pkey PRIMARY KEY (id);


--
-- Name: cache_keys cache_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cache_keys
    ADD CONSTRAINT cache_keys_pkey PRIMARY KEY (id);


--
-- Name: query client_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.query
    ADD CONSTRAINT client_id UNIQUE (client_id);


--
-- Name: clusters clusters_cluster_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clusters
    ADD CONSTRAINT clusters_cluster_name_key UNIQUE (cluster_name);


--
-- Name: clusters clusters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clusters
    ADD CONSTRAINT clusters_pkey PRIMARY KEY (id);


--
-- Name: clusters clusters_verbose_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clusters
    ADD CONSTRAINT clusters_verbose_name_key UNIQUE (verbose_name);


--
-- Name: columns columns_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.columns
    ADD CONSTRAINT columns_pkey PRIMARY KEY (id);


--
-- Name: css_templates css_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.css_templates
    ADD CONSTRAINT css_templates_pkey PRIMARY KEY (id);


--
-- Name: dashboard_email_schedules dashboard_email_schedules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_email_schedules
    ADD CONSTRAINT dashboard_email_schedules_pkey PRIMARY KEY (id);


--
-- Name: dashboard_roles dashboard_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_roles
    ADD CONSTRAINT dashboard_roles_pkey PRIMARY KEY (id);


--
-- Name: dashboard_slices dashboard_slices_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_slices
    ADD CONSTRAINT dashboard_slices_pkey PRIMARY KEY (id);


--
-- Name: dashboard_user dashboard_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_user
    ADD CONSTRAINT dashboard_user_pkey PRIMARY KEY (id);


--
-- Name: dashboards dashboards_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboards
    ADD CONSTRAINT dashboards_pkey PRIMARY KEY (id);


--
-- Name: databasechangeloglock databasechangeloglock_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.databasechangeloglock
    ADD CONSTRAINT databasechangeloglock_pkey PRIMARY KEY (id);


--
-- Name: datasources datasources_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datasources
    ADD CONSTRAINT datasources_pkey PRIMARY KEY (id);


--
-- Name: dbs dbs_database_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dbs
    ADD CONSTRAINT dbs_database_name_key UNIQUE (database_name);


--
-- Name: dbs dbs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dbs
    ADD CONSTRAINT dbs_pkey PRIMARY KEY (id);


--
-- Name: dbs dbs_verbose_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dbs
    ADD CONSTRAINT dbs_verbose_name_key UNIQUE (verbose_name);


--
-- Name: druiddatasource_user druiddatasource_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.druiddatasource_user
    ADD CONSTRAINT druiddatasource_user_pkey PRIMARY KEY (id);


--
-- Name: dynamic_plugin dynamic_plugin_key_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dynamic_plugin
    ADD CONSTRAINT dynamic_plugin_key_key UNIQUE (key);


--
-- Name: dynamic_plugin dynamic_plugin_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dynamic_plugin
    ADD CONSTRAINT dynamic_plugin_name_key UNIQUE (name);


--
-- Name: dynamic_plugin dynamic_plugin_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dynamic_plugin
    ADD CONSTRAINT dynamic_plugin_pkey PRIMARY KEY (id);


--
-- Name: favstar favstar_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.favstar
    ADD CONSTRAINT favstar_pkey PRIMARY KEY (id);


--
-- Name: filter_sets filter_sets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.filter_sets
    ADD CONSTRAINT filter_sets_pkey PRIMARY KEY (id);


--
-- Name: dashboards idx_unique_slug; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboards
    ADD CONSTRAINT idx_unique_slug UNIQUE (slug);


--
-- Name: key_value key_value_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.key_value
    ADD CONSTRAINT key_value_pkey PRIMARY KEY (id);


--
-- Name: keyvalue keyvalue_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.keyvalue
    ADD CONSTRAINT keyvalue_pkey PRIMARY KEY (id);


--
-- Name: logs logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logs
    ADD CONSTRAINT logs_pkey PRIMARY KEY (id);


--
-- Name: metrics metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.metrics
    ADD CONSTRAINT metrics_pkey PRIMARY KEY (id);


--
-- Name: query query_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.query
    ADD CONSTRAINT query_pkey PRIMARY KEY (id);


--
-- Name: report_execution_log report_execution_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_execution_log
    ADD CONSTRAINT report_execution_log_pkey PRIMARY KEY (id);


--
-- Name: report_recipient report_recipient_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_recipient
    ADD CONSTRAINT report_recipient_pkey PRIMARY KEY (id);


--
-- Name: report_schedule report_schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule
    ADD CONSTRAINT report_schedule_pkey PRIMARY KEY (id);


--
-- Name: report_schedule_user report_schedule_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule_user
    ADD CONSTRAINT report_schedule_user_pkey PRIMARY KEY (id);


--
-- Name: rls_filter_roles rls_filter_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rls_filter_roles
    ADD CONSTRAINT rls_filter_roles_pkey PRIMARY KEY (id);


--
-- Name: rls_filter_tables rls_filter_tables_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rls_filter_tables
    ADD CONSTRAINT rls_filter_tables_pkey PRIMARY KEY (id);


--
-- Name: row_level_security_filters row_level_security_filters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.row_level_security_filters
    ADD CONSTRAINT row_level_security_filters_pkey PRIMARY KEY (id);


--
-- Name: saved_query saved_query_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.saved_query
    ADD CONSTRAINT saved_query_pkey PRIMARY KEY (id);


--
-- Name: sl_columns sl_columns_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_columns
    ADD CONSTRAINT sl_columns_pkey PRIMARY KEY (id);


--
-- Name: sl_columns sl_columns_uuid_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_columns
    ADD CONSTRAINT sl_columns_uuid_key UNIQUE (uuid);


--
-- Name: sl_dataset_columns sl_dataset_columns_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_dataset_columns
    ADD CONSTRAINT sl_dataset_columns_pkey PRIMARY KEY (dataset_id, column_id);


--
-- Name: sl_dataset_tables sl_dataset_tables_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_dataset_tables
    ADD CONSTRAINT sl_dataset_tables_pkey PRIMARY KEY (dataset_id, table_id);


--
-- Name: sl_dataset_users sl_dataset_users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_dataset_users
    ADD CONSTRAINT sl_dataset_users_pkey PRIMARY KEY (dataset_id, user_id);


--
-- Name: sl_datasets sl_datasets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_datasets
    ADD CONSTRAINT sl_datasets_pkey PRIMARY KEY (id);


--
-- Name: sl_datasets sl_datasets_uuid_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_datasets
    ADD CONSTRAINT sl_datasets_uuid_key UNIQUE (uuid);


--
-- Name: sl_table_columns sl_table_columns_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_table_columns
    ADD CONSTRAINT sl_table_columns_pkey PRIMARY KEY (table_id, column_id);


--
-- Name: sl_tables sl_tables_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_tables
    ADD CONSTRAINT sl_tables_pkey PRIMARY KEY (id);


--
-- Name: sl_tables sl_tables_uuid_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_tables
    ADD CONSTRAINT sl_tables_uuid_key UNIQUE (uuid);


--
-- Name: slice_email_schedules slice_email_schedules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_email_schedules
    ADD CONSTRAINT slice_email_schedules_pkey PRIMARY KEY (id);


--
-- Name: slice_user slice_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_user
    ADD CONSTRAINT slice_user_pkey PRIMARY KEY (id);


--
-- Name: slices slices_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slices
    ADD CONSTRAINT slices_pkey PRIMARY KEY (id);


--
-- Name: sql_metrics sql_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sql_metrics
    ADD CONSTRAINT sql_metrics_pkey PRIMARY KEY (id);


--
-- Name: sql_observations sql_observations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sql_observations
    ADD CONSTRAINT sql_observations_pkey PRIMARY KEY (id);


--
-- Name: sqlatable_user sqlatable_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sqlatable_user
    ADD CONSTRAINT sqlatable_user_pkey PRIMARY KEY (id);


--
-- Name: ssh_tunnels ssh_tunnels_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ssh_tunnels
    ADD CONSTRAINT ssh_tunnels_pkey PRIMARY KEY (id);


--
-- Name: tab_state tab_state_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tab_state
    ADD CONSTRAINT tab_state_pkey PRIMARY KEY (id);


--
-- Name: table_columns table_columns_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_columns
    ADD CONSTRAINT table_columns_pkey PRIMARY KEY (id);


--
-- Name: table_schema table_schema_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_schema
    ADD CONSTRAINT table_schema_pkey PRIMARY KEY (id);


--
-- Name: tables tables_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tables
    ADD CONSTRAINT tables_pkey PRIMARY KEY (id);


--
-- Name: tag tag_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tag
    ADD CONSTRAINT tag_name_key UNIQUE (name);


--
-- Name: tag tag_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tag
    ADD CONSTRAINT tag_pkey PRIMARY KEY (id);


--
-- Name: tagged_object tagged_object_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tagged_object
    ADD CONSTRAINT tagged_object_pkey PRIMARY KEY (id);


--
-- Name: clusters uq_clusters_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clusters
    ADD CONSTRAINT uq_clusters_uuid UNIQUE (uuid);


--
-- Name: columns uq_columns_column_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.columns
    ADD CONSTRAINT uq_columns_column_name UNIQUE (column_name, datasource_id);


--
-- Name: columns uq_columns_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.columns
    ADD CONSTRAINT uq_columns_uuid UNIQUE (uuid);


--
-- Name: dashboard_email_schedules uq_dashboard_email_schedules_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_email_schedules
    ADD CONSTRAINT uq_dashboard_email_schedules_uuid UNIQUE (uuid);


--
-- Name: dashboard_slices uq_dashboard_slice; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_slices
    ADD CONSTRAINT uq_dashboard_slice UNIQUE (dashboard_id, slice_id);


--
-- Name: dashboards uq_dashboards_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboards
    ADD CONSTRAINT uq_dashboards_uuid UNIQUE (uuid);


--
-- Name: datasources uq_datasources_cluster_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datasources
    ADD CONSTRAINT uq_datasources_cluster_id UNIQUE (cluster_id, datasource_name);


--
-- Name: datasources uq_datasources_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datasources
    ADD CONSTRAINT uq_datasources_uuid UNIQUE (uuid);


--
-- Name: dbs uq_dbs_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dbs
    ADD CONSTRAINT uq_dbs_uuid UNIQUE (uuid);


--
-- Name: metrics uq_metrics_metric_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.metrics
    ADD CONSTRAINT uq_metrics_metric_name UNIQUE (metric_name, datasource_id);


--
-- Name: metrics uq_metrics_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.metrics
    ADD CONSTRAINT uq_metrics_uuid UNIQUE (uuid);


--
-- Name: report_schedule uq_report_schedule_name_type; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule
    ADD CONSTRAINT uq_report_schedule_name_type UNIQUE (name, type);


--
-- Name: row_level_security_filters uq_rls_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.row_level_security_filters
    ADD CONSTRAINT uq_rls_name UNIQUE (name);


--
-- Name: saved_query uq_saved_query_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.saved_query
    ADD CONSTRAINT uq_saved_query_uuid UNIQUE (uuid);


--
-- Name: slice_email_schedules uq_slice_email_schedules_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_email_schedules
    ADD CONSTRAINT uq_slice_email_schedules_uuid UNIQUE (uuid);


--
-- Name: slices uq_slices_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slices
    ADD CONSTRAINT uq_slices_uuid UNIQUE (uuid);


--
-- Name: sql_metrics uq_sql_metrics_metric_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sql_metrics
    ADD CONSTRAINT uq_sql_metrics_metric_name UNIQUE (metric_name, table_id);


--
-- Name: sql_metrics uq_sql_metrics_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sql_metrics
    ADD CONSTRAINT uq_sql_metrics_uuid UNIQUE (uuid);


--
-- Name: table_columns uq_table_columns_column_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_columns
    ADD CONSTRAINT uq_table_columns_column_name UNIQUE (column_name, table_id);


--
-- Name: table_columns uq_table_columns_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_columns
    ADD CONSTRAINT uq_table_columns_uuid UNIQUE (uuid);


--
-- Name: tables uq_tables_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tables
    ADD CONSTRAINT uq_tables_uuid UNIQUE (uuid);


--
-- Name: url url_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.url
    ADD CONSTRAINT url_pkey PRIMARY KEY (id);


--
-- Name: user_attribute user_attribute_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT user_attribute_pkey PRIMARY KEY (id);


--
-- Name: ix_alerts_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_alerts_active ON public.alerts USING btree (active);


--
-- Name: ix_cache_keys_datasource_uid; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_cache_keys_datasource_uid ON public.cache_keys USING btree (datasource_uid);


--
-- Name: ix_creation_method; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_creation_method ON public.report_schedule USING btree (creation_method);


--
-- Name: ix_dashboard_email_schedules_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_dashboard_email_schedules_active ON public.dashboard_email_schedules USING btree (active);


--
-- Name: ix_key_value_expires_on; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_key_value_expires_on ON public.key_value USING btree (expires_on);


--
-- Name: ix_key_value_uuid; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX ix_key_value_uuid ON public.key_value USING btree (uuid);


--
-- Name: ix_logs_user_id_dttm; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_logs_user_id_dttm ON public.logs USING btree (user_id, dttm);


--
-- Name: ix_query_results_key; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_query_results_key ON public.query USING btree (results_key);


--
-- Name: ix_report_schedule_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_report_schedule_active ON public.report_schedule USING btree (active);


--
-- Name: ix_row_level_security_filters_filter_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_row_level_security_filters_filter_type ON public.row_level_security_filters USING btree (filter_type);


--
-- Name: ix_slice_email_schedules_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_slice_email_schedules_active ON public.slice_email_schedules USING btree (active);


--
-- Name: ix_sql_observations_dttm; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_sql_observations_dttm ON public.sql_observations USING btree (dttm);


--
-- Name: ix_ssh_tunnels_database_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX ix_ssh_tunnels_database_id ON public.ssh_tunnels USING btree (database_id);


--
-- Name: ix_ssh_tunnels_uuid; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX ix_ssh_tunnels_uuid ON public.ssh_tunnels USING btree (uuid);


--
-- Name: ix_tab_state_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX ix_tab_state_id ON public.tab_state USING btree (id);


--
-- Name: ix_table_schema_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX ix_table_schema_id ON public.table_schema USING btree (id);


--
-- Name: ix_tagged_object_object_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_tagged_object_object_id ON public.tagged_object USING btree (object_id);


--
-- Name: ti_dag_state; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ti_dag_state ON public.annotation USING btree (layer_id, start_dttm, end_dttm);


--
-- Name: ti_user_id_changed_on; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ti_user_id_changed_on ON public.query USING btree (user_id, changed_on);


--
-- Name: dashboards delete_dashboard_role; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER delete_dashboard_role AFTER DELETE ON public.dashboards FOR EACH ROW EXECUTE FUNCTION public.delete_dashboard_role();


--
-- Name: dashboards insert_new_dashboard_role; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER insert_new_dashboard_role AFTER INSERT ON public.dashboards FOR EACH ROW EXECUTE FUNCTION public.insert_new_dashboard_role();


--
-- Name: dashboards insert_owners_on_new_dashboard; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER insert_owners_on_new_dashboard AFTER INSERT ON public.dashboards FOR EACH ROW EXECUTE FUNCTION public.insert_owners_on_new_dashboard();


--
-- Name: tables insert_owners_on_new_dataset; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER insert_owners_on_new_dataset AFTER INSERT ON public.tables FOR EACH ROW EXECUTE FUNCTION public.insert_owners_on_new_dataset();


--
-- Name: ab_user_role remove_deleted_bi_editor_as_owner; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER remove_deleted_bi_editor_as_owner AFTER DELETE ON public.ab_user_role FOR EACH ROW EXECUTE FUNCTION public.remove_deleted_bi_editor_as_owner();


--
-- Name: ab_user_role remove_favstar; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER remove_favstar BEFORE DELETE ON public.ab_user_role FOR EACH ROW EXECUTE FUNCTION public.remove_favstar();


--
-- Name: dashboards remove_favstar_2; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER remove_favstar_2 AFTER UPDATE ON public.dashboards FOR EACH ROW EXECUTE FUNCTION public.remove_favstar_2();


--
-- Name: ab_user_role set_new_bi_editor_as_owner; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER set_new_bi_editor_as_owner AFTER INSERT ON public.ab_user_role FOR EACH ROW EXECUTE FUNCTION public.set_new_bi_editor_as_owner();


--
-- Name: dashboards update_dashboard_role; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_dashboard_role AFTER UPDATE ON public.dashboards FOR EACH ROW EXECUTE FUNCTION public.update_dashboard_role();


--
-- Name: ab_permission_view ab_permission_view_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_permission_view
    ADD CONSTRAINT ab_permission_view_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.ab_permission(id);


--
-- Name: ab_permission_view_role ab_permission_view_role_permission_view_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_permission_view_role
    ADD CONSTRAINT ab_permission_view_role_permission_view_id_fkey FOREIGN KEY (permission_view_id) REFERENCES public.ab_permission_view(id);


--
-- Name: ab_permission_view_role ab_permission_view_role_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_permission_view_role
    ADD CONSTRAINT ab_permission_view_role_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.ab_role(id);


--
-- Name: ab_permission_view ab_permission_view_view_menu_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_permission_view
    ADD CONSTRAINT ab_permission_view_view_menu_id_fkey FOREIGN KEY (view_menu_id) REFERENCES public.ab_view_menu(id);


--
-- Name: ab_user ab_user_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_user
    ADD CONSTRAINT ab_user_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: ab_user ab_user_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_user
    ADD CONSTRAINT ab_user_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: ab_user_role ab_user_role_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_user_role
    ADD CONSTRAINT ab_user_role_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.ab_role(id);


--
-- Name: ab_user_role ab_user_role_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ab_user_role
    ADD CONSTRAINT ab_user_role_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: access_request access_request_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.access_request
    ADD CONSTRAINT access_request_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: access_request access_request_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.access_request
    ADD CONSTRAINT access_request_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: alert_logs alert_logs_alert_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alert_logs
    ADD CONSTRAINT alert_logs_alert_id_fkey FOREIGN KEY (alert_id) REFERENCES public.alerts(id);


--
-- Name: alert_owner alert_owner_alert_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alert_owner
    ADD CONSTRAINT alert_owner_alert_id_fkey FOREIGN KEY (alert_id) REFERENCES public.alerts(id);


--
-- Name: alert_owner alert_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alert_owner
    ADD CONSTRAINT alert_owner_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: alerts alerts_dashboard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT alerts_dashboard_id_fkey FOREIGN KEY (dashboard_id) REFERENCES public.dashboards(id);


--
-- Name: alerts alerts_database_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT alerts_database_id_fkey FOREIGN KEY (database_id) REFERENCES public.dbs(id);


--
-- Name: alerts alerts_ibfk_3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT alerts_ibfk_3 FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: alerts alerts_ibfk_4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT alerts_ibfk_4 FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: alerts alerts_slice_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT alerts_slice_id_fkey FOREIGN KEY (slice_id) REFERENCES public.slices(id);


--
-- Name: annotation annotation_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.annotation
    ADD CONSTRAINT annotation_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: annotation annotation_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.annotation
    ADD CONSTRAINT annotation_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: annotation_layer annotation_layer_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.annotation_layer
    ADD CONSTRAINT annotation_layer_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: annotation_layer annotation_layer_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.annotation_layer
    ADD CONSTRAINT annotation_layer_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: annotation annotation_layer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.annotation
    ADD CONSTRAINT annotation_layer_id_fkey FOREIGN KEY (layer_id) REFERENCES public.annotation_layer(id);


--
-- Name: clusters clusters_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clusters
    ADD CONSTRAINT clusters_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: clusters clusters_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clusters
    ADD CONSTRAINT clusters_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: columns columns_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.columns
    ADD CONSTRAINT columns_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: columns columns_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.columns
    ADD CONSTRAINT columns_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: css_templates css_templates_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.css_templates
    ADD CONSTRAINT css_templates_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: css_templates css_templates_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.css_templates
    ADD CONSTRAINT css_templates_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: dashboard_email_schedules dashboard_email_schedules_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_email_schedules
    ADD CONSTRAINT dashboard_email_schedules_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: dashboard_email_schedules dashboard_email_schedules_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_email_schedules
    ADD CONSTRAINT dashboard_email_schedules_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: dashboard_email_schedules dashboard_email_schedules_dashboard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_email_schedules
    ADD CONSTRAINT dashboard_email_schedules_dashboard_id_fkey FOREIGN KEY (dashboard_id) REFERENCES public.dashboards(id);


--
-- Name: dashboard_email_schedules dashboard_email_schedules_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_email_schedules
    ADD CONSTRAINT dashboard_email_schedules_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: dashboard_roles dashboard_roles_dashboard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_roles
    ADD CONSTRAINT dashboard_roles_dashboard_id_fkey FOREIGN KEY (dashboard_id) REFERENCES public.dashboards(id);


--
-- Name: dashboard_roles dashboard_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_roles
    ADD CONSTRAINT dashboard_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.ab_role(id);


--
-- Name: dashboard_slices dashboard_slices_dashboard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_slices
    ADD CONSTRAINT dashboard_slices_dashboard_id_fkey FOREIGN KEY (dashboard_id) REFERENCES public.dashboards(id);


--
-- Name: dashboard_slices dashboard_slices_slice_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_slices
    ADD CONSTRAINT dashboard_slices_slice_id_fkey FOREIGN KEY (slice_id) REFERENCES public.slices(id);


--
-- Name: dashboard_user dashboard_user_dashboard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_user
    ADD CONSTRAINT dashboard_user_dashboard_id_fkey FOREIGN KEY (dashboard_id) REFERENCES public.dashboards(id);


--
-- Name: dashboard_user dashboard_user_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboard_user
    ADD CONSTRAINT dashboard_user_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: dashboards dashboards_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboards
    ADD CONSTRAINT dashboards_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: dashboards dashboards_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dashboards
    ADD CONSTRAINT dashboards_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: datasources datasources_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datasources
    ADD CONSTRAINT datasources_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: dbs dbs_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dbs
    ADD CONSTRAINT dbs_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: dbs dbs_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dbs
    ADD CONSTRAINT dbs_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: druiddatasource_user druiddatasource_user_datasource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.druiddatasource_user
    ADD CONSTRAINT druiddatasource_user_datasource_id_fkey FOREIGN KEY (datasource_id) REFERENCES public.datasources(id);


--
-- Name: druiddatasource_user druiddatasource_user_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.druiddatasource_user
    ADD CONSTRAINT druiddatasource_user_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: dynamic_plugin dynamic_plugin_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dynamic_plugin
    ADD CONSTRAINT dynamic_plugin_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: dynamic_plugin dynamic_plugin_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dynamic_plugin
    ADD CONSTRAINT dynamic_plugin_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: embedded_dashboards embedded_dashboards_dashboard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.embedded_dashboards
    ADD CONSTRAINT embedded_dashboards_dashboard_id_fkey FOREIGN KEY (dashboard_id) REFERENCES public.dashboards(id);


--
-- Name: favstar favstar_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.favstar
    ADD CONSTRAINT favstar_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: filter_sets filter_sets_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.filter_sets
    ADD CONSTRAINT filter_sets_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: filter_sets filter_sets_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.filter_sets
    ADD CONSTRAINT filter_sets_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: filter_sets filter_sets_dashboard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.filter_sets
    ADD CONSTRAINT filter_sets_dashboard_id_fkey FOREIGN KEY (dashboard_id) REFERENCES public.dashboards(id);


--
-- Name: columns fk_columns_datasource_id_datasources; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.columns
    ADD CONSTRAINT fk_columns_datasource_id_datasources FOREIGN KEY (datasource_id) REFERENCES public.datasources(id);


--
-- Name: datasources fk_datasources_cluster_id_clusters; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datasources
    ADD CONSTRAINT fk_datasources_cluster_id_clusters FOREIGN KEY (cluster_id) REFERENCES public.clusters(id);


--
-- Name: metrics fk_metrics_datasource_id_datasources; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.metrics
    ADD CONSTRAINT fk_metrics_datasource_id_datasources FOREIGN KEY (datasource_id) REFERENCES public.datasources(id);


--
-- Name: key_value key_value_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.key_value
    ADD CONSTRAINT key_value_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: key_value key_value_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.key_value
    ADD CONSTRAINT key_value_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: logs logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logs
    ADD CONSTRAINT logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: metrics metrics_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.metrics
    ADD CONSTRAINT metrics_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: metrics metrics_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.metrics
    ADD CONSTRAINT metrics_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: query query_database_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.query
    ADD CONSTRAINT query_database_id_fkey FOREIGN KEY (database_id) REFERENCES public.dbs(id);


--
-- Name: query query_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.query
    ADD CONSTRAINT query_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: report_execution_log report_execution_log_report_schedule_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_execution_log
    ADD CONSTRAINT report_execution_log_report_schedule_id_fkey FOREIGN KEY (report_schedule_id) REFERENCES public.report_schedule(id);


--
-- Name: report_recipient report_recipient_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_recipient
    ADD CONSTRAINT report_recipient_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: report_recipient report_recipient_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_recipient
    ADD CONSTRAINT report_recipient_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: report_recipient report_recipient_report_schedule_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_recipient
    ADD CONSTRAINT report_recipient_report_schedule_id_fkey FOREIGN KEY (report_schedule_id) REFERENCES public.report_schedule(id);


--
-- Name: report_schedule report_schedule_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule
    ADD CONSTRAINT report_schedule_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: report_schedule report_schedule_chart_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule
    ADD CONSTRAINT report_schedule_chart_id_fkey FOREIGN KEY (chart_id) REFERENCES public.slices(id);


--
-- Name: report_schedule report_schedule_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule
    ADD CONSTRAINT report_schedule_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: report_schedule report_schedule_dashboard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule
    ADD CONSTRAINT report_schedule_dashboard_id_fkey FOREIGN KEY (dashboard_id) REFERENCES public.dashboards(id);


--
-- Name: report_schedule report_schedule_database_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule
    ADD CONSTRAINT report_schedule_database_id_fkey FOREIGN KEY (database_id) REFERENCES public.dbs(id);


--
-- Name: report_schedule_user report_schedule_user_report_schedule_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule_user
    ADD CONSTRAINT report_schedule_user_report_schedule_id_fkey FOREIGN KEY (report_schedule_id) REFERENCES public.report_schedule(id);


--
-- Name: report_schedule_user report_schedule_user_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_schedule_user
    ADD CONSTRAINT report_schedule_user_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: rls_filter_roles rls_filter_roles_rls_filter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rls_filter_roles
    ADD CONSTRAINT rls_filter_roles_rls_filter_id_fkey FOREIGN KEY (rls_filter_id) REFERENCES public.row_level_security_filters(id);


--
-- Name: rls_filter_roles rls_filter_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rls_filter_roles
    ADD CONSTRAINT rls_filter_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.ab_role(id);


--
-- Name: rls_filter_tables rls_filter_tables_rls_filter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rls_filter_tables
    ADD CONSTRAINT rls_filter_tables_rls_filter_id_fkey FOREIGN KEY (rls_filter_id) REFERENCES public.row_level_security_filters(id);


--
-- Name: rls_filter_tables rls_filter_tables_table_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rls_filter_tables
    ADD CONSTRAINT rls_filter_tables_table_id_fkey FOREIGN KEY (table_id) REFERENCES public.tables(id);


--
-- Name: row_level_security_filters row_level_security_filters_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.row_level_security_filters
    ADD CONSTRAINT row_level_security_filters_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: row_level_security_filters row_level_security_filters_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.row_level_security_filters
    ADD CONSTRAINT row_level_security_filters_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: saved_query saved_query_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.saved_query
    ADD CONSTRAINT saved_query_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: saved_query saved_query_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.saved_query
    ADD CONSTRAINT saved_query_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: saved_query saved_query_db_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.saved_query
    ADD CONSTRAINT saved_query_db_id_fkey FOREIGN KEY (db_id) REFERENCES public.dbs(id);


--
-- Name: tab_state saved_query_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tab_state
    ADD CONSTRAINT saved_query_id FOREIGN KEY (saved_query_id) REFERENCES public.saved_query(id) ON DELETE SET NULL;


--
-- Name: saved_query saved_query_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.saved_query
    ADD CONSTRAINT saved_query_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: sl_columns sl_columns_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_columns
    ADD CONSTRAINT sl_columns_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: sl_columns sl_columns_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_columns
    ADD CONSTRAINT sl_columns_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: sl_dataset_columns sl_dataset_columns_column_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_dataset_columns
    ADD CONSTRAINT sl_dataset_columns_column_id_fkey FOREIGN KEY (column_id) REFERENCES public.sl_columns(id);


--
-- Name: sl_dataset_columns sl_dataset_columns_dataset_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_dataset_columns
    ADD CONSTRAINT sl_dataset_columns_dataset_id_fkey FOREIGN KEY (dataset_id) REFERENCES public.sl_datasets(id);


--
-- Name: sl_dataset_tables sl_dataset_tables_dataset_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_dataset_tables
    ADD CONSTRAINT sl_dataset_tables_dataset_id_fkey FOREIGN KEY (dataset_id) REFERENCES public.sl_datasets(id);


--
-- Name: sl_dataset_tables sl_dataset_tables_table_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_dataset_tables
    ADD CONSTRAINT sl_dataset_tables_table_id_fkey FOREIGN KEY (table_id) REFERENCES public.sl_tables(id);


--
-- Name: sl_dataset_users sl_dataset_users_dataset_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_dataset_users
    ADD CONSTRAINT sl_dataset_users_dataset_id_fkey FOREIGN KEY (dataset_id) REFERENCES public.sl_datasets(id);


--
-- Name: sl_dataset_users sl_dataset_users_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_dataset_users
    ADD CONSTRAINT sl_dataset_users_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: sl_datasets sl_datasets_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_datasets
    ADD CONSTRAINT sl_datasets_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: sl_datasets sl_datasets_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_datasets
    ADD CONSTRAINT sl_datasets_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: sl_datasets sl_datasets_database_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_datasets
    ADD CONSTRAINT sl_datasets_database_id_fkey FOREIGN KEY (database_id) REFERENCES public.dbs(id);


--
-- Name: sl_table_columns sl_table_columns_column_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_table_columns
    ADD CONSTRAINT sl_table_columns_column_id_fkey FOREIGN KEY (column_id) REFERENCES public.sl_columns(id);


--
-- Name: sl_table_columns sl_table_columns_table_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_table_columns
    ADD CONSTRAINT sl_table_columns_table_id_fkey FOREIGN KEY (table_id) REFERENCES public.sl_tables(id);


--
-- Name: sl_tables sl_tables_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_tables
    ADD CONSTRAINT sl_tables_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: sl_tables sl_tables_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_tables
    ADD CONSTRAINT sl_tables_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: sl_tables sl_tables_database_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sl_tables
    ADD CONSTRAINT sl_tables_database_id_fkey FOREIGN KEY (database_id) REFERENCES public.dbs(id);


--
-- Name: slice_email_schedules slice_email_schedules_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_email_schedules
    ADD CONSTRAINT slice_email_schedules_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: slice_email_schedules slice_email_schedules_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_email_schedules
    ADD CONSTRAINT slice_email_schedules_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: slice_email_schedules slice_email_schedules_slice_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_email_schedules
    ADD CONSTRAINT slice_email_schedules_slice_id_fkey FOREIGN KEY (slice_id) REFERENCES public.slices(id);


--
-- Name: slice_email_schedules slice_email_schedules_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_email_schedules
    ADD CONSTRAINT slice_email_schedules_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: slice_user slice_user_slice_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_user
    ADD CONSTRAINT slice_user_slice_id_fkey FOREIGN KEY (slice_id) REFERENCES public.slices(id);


--
-- Name: slice_user slice_user_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slice_user
    ADD CONSTRAINT slice_user_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: slices slices_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slices
    ADD CONSTRAINT slices_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: slices slices_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slices
    ADD CONSTRAINT slices_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: slices slices_last_saved_by_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.slices
    ADD CONSTRAINT slices_last_saved_by_fk FOREIGN KEY (last_saved_by_fk) REFERENCES public.ab_user(id);


--
-- Name: sql_metrics sql_metrics_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sql_metrics
    ADD CONSTRAINT sql_metrics_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: sql_metrics sql_metrics_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sql_metrics
    ADD CONSTRAINT sql_metrics_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: sql_metrics sql_metrics_table_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sql_metrics
    ADD CONSTRAINT sql_metrics_table_id_fkey FOREIGN KEY (table_id) REFERENCES public.tables(id);


--
-- Name: sql_observations sql_observations_alert_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sql_observations
    ADD CONSTRAINT sql_observations_alert_id_fkey FOREIGN KEY (alert_id) REFERENCES public.alerts(id);


--
-- Name: sqlatable_user sqlatable_user_table_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sqlatable_user
    ADD CONSTRAINT sqlatable_user_table_id_fkey FOREIGN KEY (table_id) REFERENCES public.tables(id);


--
-- Name: sqlatable_user sqlatable_user_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sqlatable_user
    ADD CONSTRAINT sqlatable_user_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: ssh_tunnels ssh_tunnels_database_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ssh_tunnels
    ADD CONSTRAINT ssh_tunnels_database_id_fkey FOREIGN KEY (database_id) REFERENCES public.dbs(id);


--
-- Name: tab_state tab_state_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tab_state
    ADD CONSTRAINT tab_state_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: tab_state tab_state_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tab_state
    ADD CONSTRAINT tab_state_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: tab_state tab_state_database_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tab_state
    ADD CONSTRAINT tab_state_database_id_fkey FOREIGN KEY (database_id) REFERENCES public.dbs(id) ON DELETE CASCADE;


--
-- Name: tab_state tab_state_latest_query_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tab_state
    ADD CONSTRAINT tab_state_latest_query_id_fkey FOREIGN KEY (latest_query_id) REFERENCES public.query(client_id) ON DELETE SET NULL;


--
-- Name: tab_state tab_state_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tab_state
    ADD CONSTRAINT tab_state_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: table_columns table_columns_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_columns
    ADD CONSTRAINT table_columns_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: table_columns table_columns_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_columns
    ADD CONSTRAINT table_columns_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: table_columns table_columns_table_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_columns
    ADD CONSTRAINT table_columns_table_id_fkey FOREIGN KEY (table_id) REFERENCES public.tables(id);


--
-- Name: table_schema table_schema_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_schema
    ADD CONSTRAINT table_schema_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: table_schema table_schema_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_schema
    ADD CONSTRAINT table_schema_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: table_schema table_schema_database_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_schema
    ADD CONSTRAINT table_schema_database_id_fkey FOREIGN KEY (database_id) REFERENCES public.dbs(id) ON DELETE CASCADE;


--
-- Name: table_schema table_schema_tab_state_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.table_schema
    ADD CONSTRAINT table_schema_tab_state_id_fkey FOREIGN KEY (tab_state_id) REFERENCES public.tab_state(id) ON DELETE CASCADE;


--
-- Name: tables tables_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tables
    ADD CONSTRAINT tables_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: tables tables_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tables
    ADD CONSTRAINT tables_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: tables tables_database_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tables
    ADD CONSTRAINT tables_database_id_fkey FOREIGN KEY (database_id) REFERENCES public.dbs(id);


--
-- Name: tag tag_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tag
    ADD CONSTRAINT tag_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: tag tag_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tag
    ADD CONSTRAINT tag_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: tagged_object tagged_object_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tagged_object
    ADD CONSTRAINT tagged_object_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: tagged_object tagged_object_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tagged_object
    ADD CONSTRAINT tagged_object_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: tagged_object tagged_object_tag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tagged_object
    ADD CONSTRAINT tagged_object_tag_id_fkey FOREIGN KEY (tag_id) REFERENCES public.tag(id);


--
-- Name: url url_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.url
    ADD CONSTRAINT url_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: url url_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.url
    ADD CONSTRAINT url_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: user_attribute user_attribute_changed_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT user_attribute_changed_by_fk_fkey FOREIGN KEY (changed_by_fk) REFERENCES public.ab_user(id);


--
-- Name: user_attribute user_attribute_created_by_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT user_attribute_created_by_fk_fkey FOREIGN KEY (created_by_fk) REFERENCES public.ab_user(id);


--
-- Name: user_attribute user_attribute_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT user_attribute_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.ab_user(id);


--
-- Name: user_attribute user_attribute_welcome_dashboard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT user_attribute_welcome_dashboard_id_fkey FOREIGN KEY (welcome_dashboard_id) REFERENCES public.dashboards(id);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

