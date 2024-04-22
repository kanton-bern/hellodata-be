from airflow import DAG
from airflow.operators.dummy_operator import DummyOperator
from airflow.operators.python_operator import PythonOperator
from datetime import datetime


def raise_exception():
    raise AirflowFailException("Ooh nohhh!")


dag = DAG('error_dag_' + "DD_KEY", description='Intentional Error', schedule_interval=None, start_date=datetime(2017, 3, 20), catchup=False)

dummy_operator = DummyOperator(task_id='dummy_task', retries=3, dag=dag)

exception_operator = PythonOperator(task_id='error_task', python_callable=raise_exception, dag=dag)

dummy_operator >> exception_operator
