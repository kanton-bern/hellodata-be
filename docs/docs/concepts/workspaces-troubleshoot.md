# Troubleshooting

## Kubernetes

If you haven't turned on Kubernetes, you'll get an error similar to this:
`urllib3.exceptions.MaxRetryError: HTTPSConnectionPool(host='kubernetes.docker.internal', port=6443): Max retries exceeded with url: /api/v1/namespaces/default/pods?labelSelector=dag_id%3Drun_boiler_example%2Ckubernetes_pod_operator%3DTrue%2Cpod-label-test%3Dlabel-name-test%2Crun_id%3Dmanual__2024-01-29T095915.2491840000-f3be8d87f%2Ctask_id%3Drun_duckdb_query%2Calready_checked%21%3DTrue%2C%21airflow-worker (Caused by NewConnectionError('<urllib3.connection.HTTPSConnection object at 0xffff82c2ab10>: Failed to establish a new connection: [Errno 111] Connection refused'))`


Full log:
```sh
[2024-01-29, 09:48:49 UTC] {pod.py:1017} ERROR - 'NoneType' object has no attribute 'metadata'
Traceback (most recent call last):
  File "/usr/local/lib/python3.11/site-packages/urllib3/connection.py", line 174, in _new_conn
    conn = connection.create_connection(
           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/usr/local/lib/python3.11/site-packages/urllib3/util/connection.py", line 95, in create_connection
    raise err
  File "/usr/local/lib/python3.11/site-packages/urllib3/util/connection.py", line 85, in create_connection
    sock.connect(sa)
ConnectionRefusedError: [Errno 111] Connection refused
During handling of the above exception, another exception occurred:
Traceback (most recent call last):
  File "/usr/local/lib/python3.11/site-packages/urllib3/connectionpool.py", line 714, in urlopen
    httplib_response = self._make_request(
                       ^^^^^^^^^^^^^^^^^^^
  File "/usr/local/lib/python3.11/site-packages/urllib3/connectionpool.py", line 403, in _make_request
    self._validate_conn(conn)
  File "/usr/local/lib/python3.11/site-packages/urllib3/connectionpool.py", line 1053, in _validate_conn
    conn.connect()
  File "/usr/local/lib/python3.11/site-packages/urllib3/connection.py", line 363, in connect
    self.sock = conn = self._new_conn()
                       ^^^^^^^^^^^^^^^^
  File "/usr/local/lib/python3.11/site-packages/urllib3/connection.py", line 186, in _new_conn
    raise NewConnectionError(
urllib3.exceptions.NewConnectionError: <urllib3.connection.HTTPSConnection object at 0xffff82db3650>: Failed to establish a new connection: [Errno 111] Connection refused
During handling of the above exception, another exception occurred:
Traceback (most recent call last):
  File "/usr/local/lib/python3.11/site-packages/airflow/providers/cncf/kubernetes/operators/pod.py", line 583, in execute_sync
    self.pod = self.get_or_create_pod(  # must set `self.pod` for `on_kill`
               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/usr/local/lib/python3.11/site-packages/airflow/providers/cncf/kubernetes/operators/pod.py", line 545, in get_or_create_pod
    pod = self.find_pod(self.namespace or pod_request_obj.metadata.namespace, context=context)

....


airflow.exceptions.AirflowException: Pod airflow-running-dagster-workspace-jdkqug7h returned a failure.
remote_pod: None
[2024-01-29, 09:48:49 UTC] {taskinstance.py:1398} INFO - Marking task as UP_FOR_RETRY. dag_id=run_boiler_example, task_id=run_duckdb_query, execution_date=20210501T000000, start_date=20240129T094849, end_date=20240129T094849
[2024-01-29, 09:48:49 UTC] {standard_task_runner.py:104} ERROR - Failed to execute job 3 for task run_duckdb_query (Pod airflow-running-dagster-workspace-jdkqug7h returned a failure.
remote_pod: None; 225)
[2024-01-29, 09:48:49 UTC] {local_task_job_runner.py:228} INFO - Task exited with return code 1
[2024-01-29, 09:48:49 UTC] {taskinstance.py:2776} INFO - 0 downstream tasks scheduled from follow-on schedule check
```


## Docker image not build locally or missing

If your name or image is not available locally (check `docker image ls`), you'll get an error on Airflow like this:

```sh
[2024-01-29, 10:10:14 UTC] {pod.py:961} INFO - Building pod airflow-running-dagster-workspace-64ngbudj with labels: {'dag_id': 'run_boiler_example', 'task_id': 'run_duckdb_query', 'run_id': 'manual__2024-01-29T101013.7029880000-328a76b5e', 'kubernetes_pod_operator': 'True', 'try_number': '1'}
[2024-01-29, 10:10:14 UTC] {pod.py:538} INFO - Found matching pod airflow-running-dagster-workspace-64ngbudj with labels {'airflow_kpo_in_cluster': 'False', 'airflow_version': '2.7.1-astro.1', 'dag_id': 'run_boiler_example', 'kubernetes_pod_operator': 'True', 'pod-label-test': 'label-name-test', 'run_id': 'manual__2024-01-29T101013.7029880000-328a76b5e', 'task_id': 'run_duckdb_query', 'try_number': '1'}
[2024-01-29, 10:10:14 UTC] {pod.py:539} INFO - `try_number` of task_instance: 1
[2024-01-29, 10:10:14 UTC] {pod.py:540} INFO - `try_number` of pod: 1
[2024-01-29, 10:10:14 UTC] {pod_manager.py:348} WARNING - Pod not yet started: airflow-running-dagster-workspace-64ngbudj
[2024-01-29, 10:10:15 UTC] {pod_manager.py:348} WARNING - Pod not yet started: airflow-running-dagster-workspace-64ngbudj
[2024-01-29, 10:10:16 UTC] {pod_manager.py:348} WARNING - Pod not yet started: airflow-running-dagster-workspace-64ngbudj
[2024-01-29, 10:10:17 UTC] {pod_manager.py:348} WARNING - Pod not yet started: airflow-running-dagster-workspace-64ngbudj
[2024-01-29, 10:10:18 UTC] {pod_manager.py:348} WARNING - Pod not yet started: airflow-running-dagster-workspace-64ngbudj
[2024-01-29, 10:12:15 UTC] {pod.py:823} INFO - Deleting pod: airflow-running-dagster-workspace-64ngbudj
[2024-01-29, 10:12:15 UTC] {taskinstance.py:1935} ERROR - Task failed with exception
Traceback (most recent call last):
  File "/usr/local/lib/python3.11/site-packages/airflow/providers/cncf/kubernetes/operators/pod.py", line 594, in execute_sync
    self.await_pod_start(pod=self.pod)
  File "/usr/local/lib/python3.11/site-packages/airflow/providers/cncf/kubernetes/operators/pod.py", line 556, in await_pod_start
    self.pod_manager.await_pod_start(pod=pod, startup_timeout=self.startup_timeout_seconds)
  File "/usr/local/lib/python3.11/site-packages/airflow/providers/cncf/kubernetes/utils/pod_manager.py", line 354, in await_pod_start
    raise PodLaunchFailedException(msg)
airflow.providers.cncf.kubernetes.utils.pod_manager.PodLaunchFailedException: Pod took longer than 120 seconds to start. Check the pod events in kubernetes to determine why.
During handling of the above exception, another exception occurred:
Traceback (most recent call last):
  File "/usr/local/lib/python3.11/site-packages/airflow/providers/cncf/kubernetes/operators/pod.py", line 578, in execute
    return self.execute_sync(context)
           ^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/usr/local/lib/python3.11/site-packages/airflow/providers/cncf/kubernetes/operators/pod.py", line 617, in execute_sync
    self.cleanup(
  File "/usr/local/lib/python3.11/site-packages/airflow/providers/cncf/kubernetes/operators/pod.py", line 746, in cleanup
    raise AirflowException(
airflow.exceptions.AirflowException: Pod airflow-running-dagster-workspace-64ngbudj returned a failure.


...

[2024-01-29, 10:12:15 UTC] {local_task_job_runner.py:228} INFO - Task exited with return code 1
[2024-01-29, 10:12:15 UTC] {taskinstance.py:2776} INFO - 0 downstream tasks scheduled from follow-on schedule check
```


If you open a kubernetes Monitoring tool such as [Lens](https://k8slens.dev/) or [k9s](https://k9scli.io/), you'll also see the pod struggling to pull the image:

![](../images/workspaces-error-pull-image.png)


Another cause, in case you haven't created the local PersistentVolume, you'd see something like "my-pvc" does not exist. Then you'd need to [create](https://kanton-bern.github.io/hellodata-be/concepts/workspaces/#volumes-pvc) the pvc first.
