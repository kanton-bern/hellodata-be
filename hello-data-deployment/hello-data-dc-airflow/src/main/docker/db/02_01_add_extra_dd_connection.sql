insert into connection (id, conn_id, conn_type, description, host, schema, login, password, port, is_encrypted, is_extra_encrypted, extra)
VALUES (
           nextval('public.connection_id_seq'),
           'postgres_db_extra_dd',
           'postgres',
           '',
           'postgres',
           'hellodata_product_development_extra_data_domain_dwh',
           'postgres',
           'postgres',
           5432,
           false,
           false,
           '{}'
       ) on conflict do nothing ;
