# User Manual

## Goal

This use manual should enable you to use the HelloDATA platform and illustrate the features of the product and how to
use them.

→ More about the Platform and its architecture you can find
on[Architecture & Concepts](../architecture/architecture.md).

## Navigation

### Portal

The entry page of HelloDATA is the [Web Portal](../architecture/data-stack.md#control-pane-portal).

1. Navigation to jump to the different capabilities of HelloDATA
2. Extended status information about
    1. data pipelines, containers, performance and security
    2. documentation and subscriptions
3. User and profile information of logged-in user.4. Overview of your dashboards

![](../images/1068204566.png)

#### Business & Data Domain

As explained in [Domain View](../architecture/architecture.md#domain-view), a key feature is to create business domains
with n-data domains. If you have access to more than one data domain, you can switch between them by clicking the
`drop-down` at the top and switch between them.

![](../images/Pasted%20image%2020231130145958.png)

### Dashboards

The most important navigation button is the dashboard links. If you hover over it, you'll see three options to choose
from.

You can either click the dashboard list in the hover menu (2) to see the list of dashboards with thumbnails, or directly
choose your dashboard (3).

![](../images/1068204575.png)

### Data-Lineage

To see the data lineage (dependencies of your data tables), you have the second menu option. Again, you chose the list
or directly on "data lineage" (2).

Button 2 will bring you to the project site, where you choose your project and load the lineage.
![](../images/1068204578.png)

Once loaded, you see all sources (1) and dbt Projects (2). On the detail page, you can see all the beautiful and helpful
documentation such as:

- table name (3)
- columns and data types (4)
- which table and model this selected object depends on (5)
- the SQL code (6)
    - as a template or complied
- and dependency graph (7)
    - which you can expand to full view (8) after clicking (7)
    - interactive data lineage view (9)

![](../images/1068204586.png)
![](../images/1068204588.png)
![](../images/1068204591.png)

### Data Marts Viewer

This view let's you access the universaal data mart (udm) layer:

![](../images/Pasted%20image%2020231130155512.png)

These are cleaned and modeled data mart tables. Data marts are the tables that have been joined and cleaned from the
source tables. This is effectively the latest layer of HelloDATA BE, which the Dashboards are accessing. Dashboards
should not access any layer before (landing zone, data storage, or data processing).

We use CloudBeaver for this, same as the DWH Viewer later.
![](../images/Pasted%20image%2020231130155752.png)

### Data Engineering

#### DWH Viewer

This is essentially a database access layer where you see all your tables, and you can write SQL queries based on your
access roles with a provided tool ([CloudBeaver](https://github.com/dbeaver/cloudbeaver)).

##### Create new SQL Query

![](../images/Pasted%20image%2020231130154714.png)o

##### Choose Connection and stored queries

You can chose pre-defined connections and query your data warehouse. Also you can store queries that other user can see
and use as well. Run your queries with (1).

![](../images/Pasted%20image%2020231130154943.png)

##### Settings and Powerful features

You can set many settings, such as user status, and many more.

![](../images/Pasted%20image%2020231130154849.png)
Please find all setting and features in the [CloudBeaver Documentation](https://dbeaver.com/docs/cloudbeaver/).

#### Orchestration

The orchestrator is your task manager. You
tell [Airflow](https://wiki.bedag.ch/pages/viewpage.action?pageId=1040683176#HDTechArchitecture&Concepts-TaskOrchestration-Airflow),
our orchestrator, in which order the task will run. This is usually done ahead of time, and in the portal, you can see
the latest runs and their status (successful, failed, etc.).

- You can navigate to DAGs (2) and see all the details (3) with the DAG name, owner, runs, schedules, next run and
  recent.
- You can also dive deeper into Datasets, Security, Admin or similar (4)
- Airflow offers lots of different visualization modes, e.g. the Graph view (6), that allows you to see each step of
  this task.
    - As you can see, you can choose calendar, task duration, Gantt, etc.

![](../images/1068204596.png)
![](../images/1068204607.png)

##### Default DAG: HelloDATA Monitoring

This is a DAG provided by us that gives you a summary of DAG runs. It will send you an email reporting which DAGs
have failed since the monitoring DAG last ran, which have run successfully, which have not run, and which are still running.

![Screenshot of the monitoring DAG Email](../images/monitoring-dag-email.png)

The email contains three sections:
1. **Monitored DAGs** – A table with an overview of DAG runs tagged as `monitored`.
2. **Changes to DAGs** – Lists DAGs that have been paused/unpaused, are new, deleted, newly monitored (added the `monitored` tag), or newly unmonitored.
3. **General Overview** – A table with all DAG runs.

You can modify the behavior of the DAG using environment variables on the Airflow worker:

| Variable Name                    | Default Value                                                                 | Effect                                                                                                                                                                                                                       |
|----------------------------------|------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MONITORING_DAG_STATE_FILE`      | `/opt/airflow/dag_state_cache.json`                                          | Path to a file where the state is saved. On Kubernetes, this could be on a PVC to ensure it persists after a pod restart.                                                             |
| `MONITORING_DAG_NOTIFY_EMAIL`    | `moiraine@tarvalon.org,rand.althor@aielwaste.net`                           | Comma-separated list of email addresses to send the report to. Airflow's mail server settings are used for sending the email.                                                         |
| `MONITORING_DAG_AIRFLOW_LINK`    | `your administrator has forgotten to set the MONITORING_DAG_AIRFLOW_LINK env variable` | Value used to generate direct links to the DAG runs.                                                                                                                                  |
| `MONITORING_DAG_INSTANCE_NAME`   | `HelloDATA`                                                                 | Used to generate the email title: `<MONITORING_DAG_INSTANCE_NAME> monitoring, <date and time> - DAG monitoring report`.                                                               |
| `MONITORING_DAG_RUNTIME_SCHEDULE`| `0 5 * * *`                                                                 | [Cron expression](https://en.wikipedia.org/wiki/Cron) for when to run the DAG.                                                                                                        |

#### Jupyter Notebooks (Jupyter Hub)

If you have one of the roles of `HELLODATA_ADMIN`, `BUSINESS_DOMAIN_ADMIN`, or `DATA_DOMAIN_ADMIN`, you can access
Jupyter Hub and its notebooks with:

![](../images/Pasted%20image%2020240828153754.png)

That opens up Jupyter Hub where you choose the base image you want to start with. E.g. you choose Data Science to do ML
workloads, or R if you solely want to work with R. This could look like this:

![](../images/Pasted%20image%2020240828153902.png)

After you can start creating notebooks with `File -> New -> Notebook`:

![](../images/Pasted%20image%2020240828155058.png)
After you choose the language (e.g. Python for Python notebooks, or R).

After you can start running commands like you do in Jupyter Notebooks.

![JupyterNotebook](https://docs.jupyter.org/en/latest/_images/jupyterlab.png)

See the [official documentation](https://docs.jupyter.org/) for help or functions.

##### Connect to HD Postgres DB

By default, a connection to your own Postgres DB can be made.

The default session time is 24h as of now and can be changed with ENV
`HELLODATA_JUPYTERHUB_TEMP_USER_PASSWORD_VALID_IN_DAYS`.

###### How to connect to the database

This is how to get a db-connection:

```python
from hello_data_scripts import connect # import the function
connection = connect() # use function, it fetches the temp user creds and establishes the connection
```

`connection` can be used to read from postgres.

###### Example

This is a more extensive example of querying the Postgres database. Imagine `SELECT version();` as your custom query or
logic you want to do.

```python
import sys
#import psycopg2 -> this is imported through the below hello_data_scripts import
from hello_data_scripts import connect  

# Get the database connection
connection = connect()

if connection is None:
    print("Failed to connect to the database.")
    sys.exit(1)

try:
    # Create a cursor object
    cursor = connection.cursor()
    
    # Example query to check the connection
    cursor.execute("SELECT version();")
    db_version = cursor.fetchone()
    print(f"Connected to database. PostgreSQL version: {db_version}")
    
except psycopg2.Error as e:
    print(f"An error occurred while performing database operations: {e}")
    
finally:
    # Close the cursor and connection
    cursor.close()
    connection.close()
    print("Database connection closed.")
```

### Administration

Here you manage the portal configurations such as user, roles, announcements, FAQs, and documentation management.

![](../images/1068204613.png)

#### Benutzerverwaltung / User Management

##### Adding user

First type your email and hit enter. Then choose the drop down and click on it.
![](../images/Pasted%20image%2020231130151446.png)

Now type the Name and hit `Berechtigungen setzen` to add the user:
![](../images/Pasted%20image%2020231130151542.png)

You should see something like this:

![](../images/Pasted%20image%2020231130151610.png)

##### Changing Permissions

1. Search the user you want to give or change permission
2. Scroll to the right
3. Click the green edit icon

![](../images/Pasted%20image%2020231130151712.png)

Now choose the `role` you want to give:

![](../images/Pasted%20image%2020231130151757.png)

And or give access to specific data domains:

![](../images/Pasted%20image%2020231130151816.png)

See more in [role-authorization-concept](role-authorization-concept.md).

#### Portal Rollenverwaltung / Portal Role Management

In this portal role management, you can see all the roles that exist.

!!! warning

    Creating new roles are not supported, despite the fact "Rolle erstellen" button exists. All roles are defined and hard coded.

![](../images/Pasted%20image%2020231130152628.png)

##### Creating a new role

See how to create a new role below:
![](../images/Pasted%20image%2020231130152819.png)

#### Ankündigung / Announcement

You can simply create an announcement that goes to all users by `Ankündigung erstellen`:
![](../images/Pasted%20image%2020231130153025.png)

Then you fill in your message. Save it.

![](../images/Pasted%20image%2020231130153054.png)
You'll see a success if everything went well:
![](../images/Pasted%20image%2020231130153156.png)

And this is how it looks to the users — It will appear until the user clicks the cross to close it.
![](../images/Pasted%20image%2020231130153220.png)

#### FAQ

The FAQ works the same as the announcements above. They are shown on the starting dashboard, but you can set the
granularity of a data domain:

![](../images/Pasted%20image%2020231130153427.png)

And this is how it looks:
![](../images/Pasted%20image%2020231130153507.png)

#### Dokumentationsmanagement / Documentation Management

Lastly, you can document the system with documentation management. Here you have one document that you can document
everything in detail, and everyone can write to it. It will appear on the dashboard as well:

![](../images/Pasted%20image%2020231130153801.png)

### Monitoring

We provide two different ways of monitoring:

- Status:- Workspaces

![](../images/1068204614.png)

#### Status

It will show you details information on instances of HelloDATA, how is the situation for the Portal, is the monitoring
running, etc.
![](../images/1068204616.png)

#### Data Domains

In Monitoring your data domains you see each system and the link to the native application. You can easily and quickly
observer permission, roles and users by different subsystems (1). Click the one you want, and you can choose different
levels (2) for each, and see its permissions (3).

![](../images/1068204622.png)

![](../images/1068204620.png)

By clicking on the blue underlined `DBT Docs`, you will be navigated to the native dbt docs. Same is true if you click
on a Airflow or Superset instance.

### DevTools

DevTools are additional tools HelloDATA provides out of the box to e.g. send Mail (Mailbox) or browse files (
FileBrowser).

![](../images/1068204623.png)

#### Mailbox

You can check in Mailbox (we use[MailHog](https://github.com/mailhog/MailHog)) what emails have been sending or what
accounts are updated.|

![](../images/1068204627.png)

#### FileBrowser

Here you can browse all the documentation or code from the git repos as file browser. We
use [SFTPGo](https://github.com/drakkan/sftpgo) here. Please use with care, as some of the folder are system relevant.

!!! note "Log in"

    Make sure you have the login credentials to log in. Your administrator should be able to provide these to you.

![](../images/sftpgo.png)

## More: Know-How

- More help for Superset
    - [Superset Documentation](https://superset.apache.org/docs/intro/)
- More help for dbt:
    - [dbt Documentation](https://docs.getdbt.com/docs/collaborate/documentation)
    - [dbt Developer Hub](https://docs.getdbt.com/)
- More about Airflow
    - [Airflow Documentation](https://airflow.apache.org/docs/)
- More about SFTPGo
    - [SFTPGo Documentation](https://docs.sftpgo.com/2.6/)

Find further important references, know-how, and best practices
on [HelloDATA Know-How](https://confluence.bedag.ch/x/4wHXE).
