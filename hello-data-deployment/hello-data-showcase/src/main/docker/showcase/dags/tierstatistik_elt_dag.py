import datetime
import os
import pandas as pd
import pendulum
import pytz
import sys
from airflow.decorators import dag, task
from airflow.operators.bash import BashOperator
# https://airflow.apache.org/docs/apache-airflow-providers-postgres/stable/_api/airflow/providers/postgres/hooks/postgres/index.html#airflow.providers.postgres.hooks.postgres.PostgresHook
from airflow.providers.postgres.hooks.postgres import PostgresHook
from datetime import timedelta


def is_daylight_savings_time(dt, timeZone):
    """ Checks if the datetime dt in timezone timeZone is in daylight savings time """
    aware_dt = timeZone.localize(dt)
    return aware_dt.dst() != timedelta(0, 0)


SQL_SCHEMA_NAME = "lzn."
cron_expression = '0 3 * * *' if is_daylight_savings_time(datetime.datetime.now(), pytz.timezone("Europe/Zurich")) else '0 4 * * *'

# the key below should be overwritten
sys.path.append("/opt/airflow/dags/" + "DD_KEY" + "/git/*/")


@dag(
    dag_id="hd_showcase_" + "DD_KEY",
    description="Showcase DAG",
    schedule_interval=cron_expression,
    start_date=pendulum.datetime(2021, 1, 1, tz="UTC"),
    catchup=False,
    dagrun_timeout=datetime.timedelta(minutes=60),
    default_args={
        'depends_on_past': False,
        'retries': 0,
        'retry_delay': datetime.timedelta(minutes=5),
    },
)
def hdShowCase():
    @task.virtualenv(
        use_dill=True,
        system_site_packages=False,
        requirements=["rdfpandas"],
    )
    def data_download():

        def snake_case(s: str):
            """
            Converts a string to snake_case

            :param str s: The string to convert to snake case
            :return: Input string converted to snake case
            :rtype: str

            Example input:  "get2HTTPResponse123Code_Style _  Manace- LIKE"
            Example output: "get2_http_response123_code_style_manace_like"
            """
            pattern = re.compile('((?<=[a-z0-9])[A-Z]|(?!^)(?<!_)[A-Z](?=[a-z]))')
            a = re.sub("[^A-Za-z0-9_]", "", s)
            return pattern.sub(r'_\1', a).lower()

        def df_breed_melt(df_data):
            df_data_new = df_data.melt(
                id_vars=["Year", "Month"],
                var_name='breed',
                value_name="n_animals"
            )
            return df_data_new

        import sys
        sys.path.append("/opt/airflow/dags/" + "DD_KEY" + "/git/*/")
        from rdfpandas.graph import to_dataframe
        import rdflib
        import pandas as pd
        import os
        import re
        g = rdflib.Graph()
        g.parse('https://tierstatistik.identitas.ch/tierstatistik.rdf', format='xml')
        df = to_dataframe(g)

        data_path = "/opt/airflow/dags/" + "DD_KEY" + "/files/"

        for index in df.index:
            if "csv" in index:
                file_name_csv = index
                df_data = pd.read_csv(file_name_csv, sep=';', index_col=None, skiprows=1)

                if "breeds" in index:
                    df_data = df_breed_melt(df_data)

                df_data.rename(columns=lambda x: snake_case(x), inplace=True)
                df_data.to_csv(data_path + os.path.basename(file_name_csv).split('/')[-1], index=False)

    @task
    def create_tables():
        postgres_hook = PostgresHook(postgres_conn_id="CONNECTION_ID")
        conn = postgres_hook.get_conn()
        cur = conn.cursor()

        data_path = "/opt/airflow/dags/" + "DD_KEY" + "/files/"

        for file in os.listdir(data_path):
            file_path_csv = os.path.join(data_path, file)
            df_data = pd.read_csv(file_path_csv)
            sql_table_name = SQL_SCHEMA_NAME + os.path.basename(file_path_csv).split('/')[-1].split('.')[0].replace('-', '_').lower()
            sql_column_name_type = ""

            for column in df_data.columns:
                if sql_column_name_type == "":
                    sql_column_name_type += f""""{column}" TEXT"""
                else:
                    sql_column_name_type += f""", "{column}" TEXT"""

            cur.execute(f"""DROP TABLE IF EXISTS {sql_table_name};""")
            cur.execute(f"""CREATE TABLE IF NOT EXISTS {sql_table_name} ( {sql_column_name_type} );""")
            conn.commit()

    @task
    def insert_data():
        postgres_hook = PostgresHook(postgres_conn_id="CONNECTION_ID")
        conn = postgres_hook.get_conn()
        cur = conn.cursor()

        data_path = "/opt/airflow/dags/" + "DD_KEY" + "/files/"

        for file in os.listdir(data_path):
            file_path_csv = os.path.join(data_path, file)
            sql_table_name = SQL_SCHEMA_NAME + os.path.basename(file_path_csv).split('/')[-1].split('.')[0].replace('-', '_').lower()
            with open(file_path_csv, "r") as file:
                cur.copy_expert(
                    f"""COPY {sql_table_name} FROM STDIN WITH CSV HEADER DELIMITER AS ',' QUOTE '\"'""",
                    file,
                )
            conn.commit()

    dbt_run = BashOperator(
        task_id='dbt_run',
        bash_command='cd /opt/airflow/dags/' + "DD_KEY" + '/dbt && dbt deps && dbt run'
    )

    dbt_docs = BashOperator(
        task_id='dbt_docs',
        bash_command='cd /opt/airflow/dags/' + "DD_KEY" + '/dbt && dbt docs generate --target-path /opt/airflow/dags/' + "DD_KEY" + '/dbt-docs'
    )

    @task
    def dbt_docs_serve():
        import json

        dbt_path = "/opt/airflow/dags/" + "DD_KEY" + "/dbt-docs/"
        search_str = 'o=[i("manifest","manifest.json"+t),i("catalog","catalog.json"+t)]'

        with open(os.path.join(dbt_path, 'index.html'), 'r', encoding="utf8") as f:
            content_index = f.read()

        with open(os.path.join(dbt_path, 'manifest.json'), 'r', encoding="utf8") as f:
            json_manifest = json.loads(f.read())

        with open(os.path.join(dbt_path, 'catalog.json'), 'r', encoding="utf8") as f:
            json_catalog = json.loads(f.read())

        with open(os.path.join(dbt_path, 'index2.html'), 'w', encoding="utf8") as f:
            new_str = "o=[{label: 'manifest', data: " + json.dumps(json_manifest) + "},{label: 'catalog', data: " + json.dumps(json_catalog) + "}]"
            new_content = content_index.replace(search_str, new_str)
            f.write(new_content)

        os.rename(os.path.join(dbt_path, 'index.html'), os.path.join(dbt_path, 'index.html_'))
        os.rename(os.path.join(dbt_path, 'index2.html'), os.path.join(dbt_path, 'index.html'))

    data_download = data_download()
    create_tables = create_tables()
    insert_data = insert_data()
    dbt_docs_serve = dbt_docs_serve()

    data_download >> create_tables >> insert_data >> dbt_run >> dbt_docs >> dbt_docs_serve


dag = hdShowCase()
