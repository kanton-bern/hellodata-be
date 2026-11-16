# The DAG of the default data domain the e2e specs expect in Airflow 3, like the showcase DAG hello_world_<key> the
# showcase copies into Airflow 2. compose-airflow3.yaml mounts it into the data domain folder of the DAG folder.
from datetime import datetime

from airflow.providers.standard.operators.empty import EmptyOperator
from airflow.sdk import DAG

with DAG('hello_world_Default_Data_Domain', description='e2e showcase DAG', schedule=None,
         start_date=datetime(2024, 1, 1), catchup=False, tags=['e2e']):
    EmptyOperator(task_id='hello')
