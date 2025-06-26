from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.email import EmailOperator
from airflow.models import DagModel, DagRun, TaskInstance, DagTag
#from airflow.utils.dates import days_ago
from airflow.settings import Session
import json
import os
import pendulum
from datetime import datetime
from sqlalchemy import or_

STATE_FILE = os.getenv('MONITORING_DAG_STATE_FILE', '/opt/airflow/dag_state_cache.json')
NOTIFY_EMAIL = os.getenv('MONITORING_DAG_NOTIFY_EMAIL', 'moiraine@tarvalon.org,rand.althor@aielwaste.net').split(',')
AIRFLOW_LINK = os.getenv('MONITORING_DAG_AIRFLOW_LINK', 'your administrator has forgotten to set the MONITORING_DAG_AIRFLOW_LINK env variable')

THIS_DAG_ID = 'monitoring_dag'
INSTANCE_NAME = os.getenv('MONITORING_DAG_INSTANCE_NAME', 'HelloDATA')
THIS_DAG_RUNTIME_SCHEDULE = os.getenv('MONITORING_DAG_RUNTIME_SCHEDULE', '0 5 * * *')


def load_previous_state():
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, 'r') as f:
            return json.load(f)
    return {}

def save_current_state(state):
    with open(STATE_FILE, 'w') as f:
        json.dump(state, f)

# Detect changes in DAG states, new DAGs, deleted DAGs, and 'monitored'-Tag using a json file with the last state of the DAGs.
def detect_changes(**context):
    session = Session()
    dags = session.query(DagModel).filter(DagModel.is_active==True).all()
    current_state = {dag.dag_id: [dag.is_paused,'monitored' in dag.tags] for dag in dags}
    for dag in dags:
        for tag in dag.tags:
            if tag.name == 'monitored':
                current_state[dag.dag_id][1] = True
            else:
                current_state[dag.dag_id][1] = False
    #session.close()

    previous_state = load_previous_state()
    
    # 1. Pause state changes
    state_changes = []
    for dag_id, values in current_state.items():
        is_paused = values[0]
        if dag_id in previous_state and previous_state[dag_id][0] != is_paused:
            state_changes.append((dag_id, is_paused))

    # 2. 'monitored'-tag state changes
    tag_changes = []
    for dag_id, values in current_state.items():
        is_monitored = values[1]
        if dag_id in previous_state and previous_state[dag_id][1] != is_monitored:
            tag_changes.append((dag_id, is_monitored))        

    # 3. New DAGs
    new_dags = [dag_id for dag_id in current_state if dag_id not in previous_state]

    # 4. Deleted DAGs
    deleted_dags = [dag_id for dag_id in previous_state if dag_id not in current_state]

    # 5. Last run info per DAG
    last_run_info = {}
    i = 0
    last_check_time = session.query(DagRun).filter(DagRun.dag_id==THIS_DAG_ID,DagRun.state=='success').order_by(DagRun.execution_date.desc()).first()
    for dag_id in current_state.keys():
        last_run = []
        if last_check_time:
            last_run = (
                session.query(DagRun)
                .filter(DagRun.dag_id == dag_id, or_(DagRun.end_date > last_check_time.execution_date, DagRun.end_date.is_(None)))
                .order_by(DagRun.execution_date.desc())
                #.first()
            ).all()

        for run in last_run:

            task_instance = (
                session.query(TaskInstance)
                .filter(TaskInstance.dag_id == run.dag_id)
                .filter(TaskInstance.execution_date == run.execution_date)           
            )
            retries_nr = 0
            id_list = []
            if task_instance:
                for task in task_instance:
                    retries_nr += task.try_number-1
                    if task.task_id not in id_list:
                        id_list.append(task.task_id)
            formatted_execution_date = pendulum.instance(run.execution_date).in_timezone('Europe/Zurich').strftime('%d.%m.%Y %H:%M') if run.execution_date else None
            last_run_info[i] = {
                'dag_id': dag_id,
                'execution_date': str(formatted_execution_date) if run.execution_date else 'Never',
                'state': run.state,
                'number_tries': max(retries_nr - len(id_list),0)
            }
            i += 1

    # 6. Specific DAGs with tag 'monitored'
    j = 0
    monitored_fail_detected = False
    soll_run_info = {}
    for dag_id in current_state.keys():
        soll_run = (
            session.query(DagTag)
            .filter(DagTag.dag_id == dag_id, DagTag.name == 'monitored')
            .first()
        )

        if soll_run:

            last_run = (
                session.query(DagRun)
                .filter(DagRun.dag_id == soll_run.dag_id)#, DagRun.end_date > last_check_time.execution_date if last_check_time else True)
                .order_by(DagRun.execution_date.desc())
                .first()
            )

            if last_run:
                task_instance = (
                    session.query(TaskInstance)
                    .filter(TaskInstance.dag_id == last_run.dag_id)
                    .filter(TaskInstance.execution_date == last_run.execution_date)            
                )
                retries_nr = 0
                id_list = []
                if task_instance:
                    for task in task_instance:
                        retries_nr += task.try_number-1
                        if task.task_id not in id_list:
                            id_list.append(task.task_id)
                formatted_execution_date = pendulum.instance(last_run.execution_date).in_timezone('Europe/Zurich').strftime('%d.%m.%Y %H:%M') if last_run.execution_date else None
                soll_run_info[j] = {
                    'dag_id': last_run.dag_id,
                    'execution_date': str(formatted_execution_date) if last_run.execution_date else 'Never',
                    'state': last_run.state,
                    'number_tries': max(retries_nr - len(id_list),0),
                    'run_after_last_check': last_run.end_date > last_check_time.execution_date if (last_check_time and last_run.end_date) else True
                }

                if last_run.state != 'success':
                    monitored_fail_detected = True
                if not last_run.end_date:
                    monitored_fail_detected = True    
                elif last_run.end_date < last_check_time.execution_date if last_check_time else True:
                    monitored_fail_detected = True
                j += 1

    
    
    session.close()

    # Push results to XCom
    context['ti'].xcom_push(key='dag_state_changes', value=state_changes)
    context['ti'].xcom_push(key='dag_tag_changes', value=tag_changes)
    context['ti'].xcom_push(key='new_dags', value=new_dags)
    context['ti'].xcom_push(key='deleted_dags', value=deleted_dags)
    context['ti'].xcom_push(key='last_run_info', value=last_run_info)
    context['ti'].xcom_push(key='soll_run_info', value=soll_run_info)
    context['ti'].xcom_push(key='monitored_fail_detected', value=monitored_fail_detected)

    # Save new state
    save_current_state(current_state)


# Write an email with all the changes detected in Airflow since the last check.
def notify_if_any_changes(**context):
    ti = context['ti']
    state_changes = ti.xcom_pull(key='dag_state_changes', task_ids='check_dag_states')
    tag_changes = ti.xcom_pull(key='dag_tag_changes', task_ids='check_dag_states')
    new_dags = ti.xcom_pull(key='new_dags', task_ids='check_dag_states')
    deleted_dags = ti.xcom_pull(key='deleted_dags', task_ids='check_dag_states')
    last_run_info = ti.xcom_pull(key='last_run_info', task_ids='check_dag_states')
    soll_run_info = ti.xcom_pull(key='soll_run_info', task_ids='check_dag_states')


    msg_lines = []
    msg_lines.append("<b>Hi!</b>")

# Table with the last run info of monitored DAGs    
    msg_lines.append("<br><br><h2><u>Monitored DAGs</u></h2>")

    if not soll_run_info:  
        msg_lines.append("You have no monitored DAGs.")
    else:
        success_dags = []
        failed_dags = []
        queued_dags = []
        running_dags = []
        other_dags = []
        for dag_id, info in sorted(soll_run_info.items()):
            if info['state'] == 'success' and info['run_after_last_check']:
                success_dags.append(info)
            elif info['state'] == 'failed' and info['run_after_last_check']:
                failed_dags.append(info)
            elif info['state'] == 'running':
                running_dags.append(info)    
            elif info['state'] == 'queued':
                queued_dags.append(info)    
            else:
                other_dags.append(info)    


        msg_lines.append("<table width='80%' border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse;'>" \
        "<tr>" \
        "<th style='text-align: center; vertical-align: middle; border: 1px solid #444;'>DAG ID</th>" \
        "<th style='text-align: center; vertical-align: middle; border: 1px solid #444;'>Status Last Run</th>" \
        "<th style='text-align: center; vertical-align: middle; border: 1px solid #444;'>Execution Time</th>" \
        "<th style='text-align: center; vertical-align: middle; border: 1px solid #444;'>Total Number of Task Retries</th>" \
        "</tr>")
        for info in failed_dags:
            msg_lines.append("<tr>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'><a href={AIRFLOW_LINK}dags/{info['dag_id']}/grid>{info['dag_id']}</a></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444; color: red;'><b>{info['state']}</b></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['execution_date']}</td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['number_tries']}</td>" \
            "</tr>")
        for info in other_dags:    
            msg_lines.append("<tr>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'><a href={AIRFLOW_LINK}dags/{info['dag_id']}/grid>{info['dag_id']}</a></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444; color: darkorange;'>did not run since last check</td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['execution_date']}</td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['number_tries']}</td>" \
            "</tr>")
        for info in queued_dags:
            msg_lines.append("<tr>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'><a href={AIRFLOW_LINK}dags/{info['dag_id']}/grid>{info['dag_id']}</a></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444; color: brown;'><b>{info['state']}</b></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['execution_date']}</td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['number_tries']}</td>" \
            "</tr>") 
        for info in running_dags:
            msg_lines.append("<tr>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'><a href={AIRFLOW_LINK}dags/{info['dag_id']}/grid>{info['dag_id']}</a></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444; color: lightgreen;'><b>{info['state']}</b></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['execution_date']}</td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['number_tries']}</td>" \
            "</tr>") 
        for info in success_dags:
            msg_lines.append("<tr>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'><a href={AIRFLOW_LINK}dags/{info['dag_id']}/grid>{info['dag_id']}</a></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444; color: green;'><b>{info['state']}</b></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['execution_date']}</td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['number_tries']}</td>" \
            "</tr>")          
        msg_lines.append("</table>")


# Shows all the changes in the DAGs, like new DAGs, deleted DAGs, state changes and tag changes. 
    msg_lines.append("<br><br><h2><u>Changes DAGs</u></h2>")

    # Create lists for each type of change
    msg_lines.append("<br><br><b>Pause/Unpause Changes:</b>")
    if not state_changes:
        msg_lines.append("<br>There are no changes in pause/unpause state.<br>")
    else:
        msg_lines.append("<br><span style='color: darkorange;'>The following DAGs have changed their pause/unpause state:</span>")    
    if state_changes:
        msg_lines.append("<ul>")
        for dag_id, is_paused in state_changes:
            msg_lines.append(f"<li>DAG <b>{dag_id}</b> is now <b>{'Paused' if is_paused else 'Unpaused'}</b>.</li>")
        msg_lines.append("</ul>")

    msg_lines.append("<br><b>New DAGs:</b>")
    if not new_dags:
        msg_lines.append("<br>There are no new DAGs.<br>")
    if new_dags:
        msg_lines.append("<br><span style='color: darkorange;'>The following DAGs were newly added:</span>")
        msg_lines.append("<ul>")
        for dag_id in new_dags:
            msg_lines.append(f"<li>DAG <b>{dag_id}</b></li>")
        msg_lines.append("</ul>")

    msg_lines.append("<br><b>Deleted DAGs:</b>")
    if not deleted_dags:
        msg_lines.append("<br>There are no newly deleted DAGs.<br>")
    if deleted_dags:
        msg_lines.append("<br><span style='color: darkorange;'>The following DAGs are now deleted:</span>")
        msg_lines.append("<ul>")
        for dag_id in deleted_dags:
            msg_lines.append(f"<li>DAG <b>{dag_id}</b></li>")
        msg_lines.append("</ul>")

    msg_lines.append("<br><b>Newly monitored:</b>")
    if not tag_changes:
        msg_lines.append("<br>There are no changes in the 'monitored'-tag.<br>") 
    if tag_changes:
        if all(not info[1] for info in tag_changes):
            msg_lines.append("<br>There are no changes in the 'monitored'-tag.<br>")
        else:    
            msg_lines.append("<br><span style='color: darkorange;'>The following DAGs are now monitored:</span>")
            msg_lines.append("<ul>")
            for info in tag_changes:
                if info[1]:  # If the tag is now 'monitored'
                    msg_lines.append(f"<br>DAG <b>{info[0]}</b> is now monitored.")
            msg_lines.append("</ul>")
        
    msg_lines.append("<br><b>Newly unmonitored:</b>")
    if not tag_changes:
        msg_lines.append("<br>There are no changes in the 'monitored'-tag.<br>")  
    if tag_changes:   
        if all(info[1] for info in tag_changes):
            msg_lines.append("<br>There are no changes in the 'monitored'-tag.<br>")
        else:
            msg_lines.append("<br><span style='color: darkorange;'>The following DAGs are no longer monitored:</span>")
            msg_lines.append("<ul>")       
            for info in tag_changes:
                if not info[1]:
                    msg_lines.append(f"<br>DAG <b>{info[0]}</b> is no longer monitored.") 
            msg_lines.append("</ul>")           


# Table with the last run info of all DAGs
    msg_lines.append("<br><br><h2><u>General Overview</u></h2>")

    # Create a table for the last run info
    msg_lines.append("<br><br><b>DAG-Run Summary:</b>")
    msg_lines.append("<br>Here is how the DAG-runs since the last Mail went.")
    msg_lines.append("<table width='80%' border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse;'>" \
    "<tr>" \
    "<th style='text-align: center; vertical-align: middle; border: 1px solid #444;'>DAG ID</th>" \
    "<th style='text-align: center; vertical-align: middle; border: 1px solid #444;'>Status</th>" \
    "<th style='text-align: center; vertical-align: middle; border: 1px solid #444;'>Execution Time</th>" \
    "<th style='text-align: center; vertical-align: middle; border: 1px solid #444;'>Total Number of Task Retries</th>" \
    "</tr>")
    for dag_id, info in sorted(last_run_info.items()):
        if info['state'] == 'success':
            msg_lines.append("<tr>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'><a href={AIRFLOW_LINK}dags/{info['dag_id']}/grid>{info['dag_id']}</a></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444; color: green;'><b>{info['state']}</b></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['execution_date']}</td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['number_tries']}</td>" \
            "</tr>")
        elif info['state'] == 'failed':    
            msg_lines.append("<tr>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'><a href={AIRFLOW_LINK}dags/{info['dag_id']}/grid>{info['dag_id']}</a></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444; color: red;'><b>{info['state']}</b></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['execution_date']}</td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['number_tries']}</td>" \
            "</tr>")
        elif info['state'] == 'queued':
            msg_lines.append("<tr>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'><a href={AIRFLOW_LINK}dags/{info['dag_id']}/grid>{info['dag_id']}</a></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444; color: brown;'><b>{info['state']}</b></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['execution_date']}</td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['number_tries']}</td>" \
            "</tr>") 
        elif info['state'] == 'running':
            msg_lines.append("<tr>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'><a href={AIRFLOW_LINK}dags/{info['dag_id']}/grid>{info['dag_id']}</a></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444; color: lightgreen;'><b>{info['state']}</b></td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['execution_date']}</td>" \
            f"<td style='text-align: center; vertical-align: middle; border: 1px solid #444;'>{info['number_tries']}</td>" \
            "</tr>")       
    msg_lines.append("</table>")

# Information Text about this DAG and the Controling in general
    msg_lines.append("<br><br><h2><u>Documentation</u></h2>")
    msg_lines.append("<br>This is an automated E-Mail. It should be sent to you every morning. If this Mail ever doesn't show in your inbox, \
        something went wrong and there are probably several failed DAGs that need your attention. Otherwise this Mail will show you all the changes made in Airflow since the last time. \
        It also shows you what DAGs ran in the meantime and if those DAGs ended successfully.")
    msg_lines.append("<br>If you ever want to check the DAGs manually, you just need to run the DAG <i>{THIS_DAG_ID}</i> in Airflow. This DAG will check the state of all DAGs and send you a Mail with all the changes.")
    msg_lines.append("<br>In the section <b>Monitored DAGs</b> you can see all the DAGs that are monitored by this DAG. \
        If a DAG is monitored, it will be checked every time this DAG runs. If the DAG fails, you will get a notification Mail with the information about the failure.")
    msg_lines.append("<br>If you want to monitor a DAG, please add the tag <i>monitored</i> to said DAG in Airflow. If you want to stop monitoring a DAG, just remove the tag.")
    msg_lines.append("<br>In the section <b>Changes DAGs</b> you will be notified about all the changes detected in Airflow since the last check. This Mail should tell you which DAGs were newly created \
        and if some DAGs were deleted. It also shows if some DAGs were newly paused/unpaused or if tags “monitored” are modified.")
    msg_lines.append("<br>In the section <b>General Overview</b> you will see a summary of all the DAG-runs since the last check. \
        If you find anything suspicious, please check the DAGs in Airflow and add corrections if necessary.")
    msg_lines.append("<br><br>Have a nice day!<br><br>")



    html_content = "<html><body>" + "".join(msg_lines) + "</body></html>"

    email = EmailOperator(
        task_id='send_notification_email',
        to=NOTIFY_EMAIL,
        subject=f"{INSTANCE_NAME} monitoring, {pendulum.now().strftime('%d.%m.%Y %H:%M')} - Airflow Overnight Report",
        html_content=html_content,
    )
    email.execute(context=context)

with DAG(
    THIS_DAG_ID,
    schedule_interval=THIS_DAG_RUNTIME_SCHEDULE,
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=['monitoring'],
) as dag:

    check_dag_states = PythonOperator(
        task_id='check_dag_states',
        python_callable=detect_changes,
        provide_context=True,
    )

    notify = PythonOperator(
        task_id='notify_if_changes',
        python_callable=notify_if_any_changes,
        provide_context=True,
    )

    check_dag_states >> notify



