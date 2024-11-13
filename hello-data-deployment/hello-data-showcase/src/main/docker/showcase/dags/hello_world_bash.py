from airflow import DAG
from airflow.operators.bash_operator import BashOperator
from airflow.operators.dummy_operator import DummyOperator
from datetime import datetime

dag_name = 'hello_world_bash_' + "DD_KEY"

default_args = {
    'description': 'Hello world example with bash usage',
    'start_date': datetime(2024, 11, 12),
    'schedule_interval': None,
    'catchup': False
}

with DAG(dag_name, default_args=default_args) as dag:
    t1 = DummyOperator(task_id="start")

    t2 = BashOperator(task_id='hello_world',
                      bash_command='echo "Hi!!"')

    t3 = DummyOperator(task_id="end")

    t1 >> t2 >> t3
