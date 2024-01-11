# Roles and authorization concept


## Platform Authentication Authorization

Authentication and authorizations within the various logical contexts or domains of the HelloDATA system are handled as follows.   
Authentication is handled via the OAuth 2 standard. In the case of the Canton of Bern, this is done via the central KeyCloak server. Authorizations to the various elements within a subject or Data Domain are handled via authorization within the HelloDATA portal.  
To keep administration simple, a role concept is applied. Instead of defining the authorizations for each user, roles receive the authorizations and the users are then assigned to the roles. The roles available in the portal have fixed defined permissions.

### Business Domain

In order for a user to gain access to a Business Domain, the user must be authenticated for the Business Domain.  
Users without authentication who try to access a Business Domain will receive an error message.  
The following two logical roles are available within a Business Domain:

- HELLODATA_ADMIN
- BUSINESS_DOMAIN_ADMIN

#### HELLODATA_ADMIN

- Can act fully in the system.

#### BUSINESS_DOMAIN_ADMIN

- Can manage users and assign roles (except HELLODATA_ADMIN).
- Can manage dashboard metadata.
- Can manage announcements.
- Can manage the FAQ.
- Can manage the external documentation links.

BUSINESS_DOMAIN_ADMIN is automatically DATA_DOMAIN_ADMIN in all Data Domains within the Business Domain (see Data Domain Context).

### Data Domain

A Data Domain encapsulates all data elements and tools that are of interest for a specific issue.  
HalloDATA supports 1 - n Data Domains within a Business Domain.

The resources to be protected within a Data Domain are:

- Schema of the Data Domain.
- Data mart tables of the Data Domain.
- The entire DWH environment of the Data Domain.
- Data lineage documents of the DBT projects of the Data Domain.
- Dashboards, charts, datasets within the superset instance of a Data Domain.
- Airflow DAGs of the Data Domain.

The following three logical roles are available within a Data Domain:

- DATA_DOMAIN_VIEWER    
- DATA_DOMAIN_EDITOR
- DATA_DOMAIN_ADMIN

Depending on the role assigned, users are given different permissions to act in the Data Domain.  
A user who has not been assigned a role in a Data Domain will generally not be granted access to any resources of that Data Domain.

#### DATA_DOMAIN_VIEWER

- The DATA_DOMAIN_VIEWER role is granted potential read access to dashboards of a Data Domain.
- Which dashboards of the Data Domain a DATA_DOMAIN_VIEWER user is allowed to see is administered within the user management of the HelloDATA portal.
- Only assigned dashboards are visible to a DATA_DOMAIN_VIEWER.
- Only dashboards in "Published" status are visible to a DATA_DOMAIN_VIEWER. A DATA_DOMAIN_VIEWER can view all data lineage documents of the Data Domain.
- A DATA_DOMAIN_VIEWER can access the links to external dashboards associated with its Data Domain. It is not checked whether the user has access in the systems outside the HelloDATA system boundary.

#### DATA_DOMAIN_EDITOR

Same as DATA_DOMAIN_VIEWER plus:

- The DATA_DOMAIN_EDITOR role is granted read and write access to the dashboards of a Data Domain. All dashboards are visible and editable for a DATA_DOMAIN_EDITOR. All charts used in the dashboards are visible and editable for a DATA_DOMAIN_EDITOR. All data sets used in the dashboards are visible and editable for a DATA_DOMAIN_EDITOR.
- A DATA_DOMAIN_EDITOR can create new dashboards.
- A DATA_DOMAIN_EDITOR can view the data marts of the Data Domain.
- A DATA_DOMAIN_EDITOR has access to the SQL lab in the superset.

#### DATA_DOMAIN_ADMIN

Same as DATA_DOMAIN_EDITOR plus:

The DATA_DOMAIN_ADMIN role can view the airflow DAGs of the Data Domain.  
A DATA_DOMAIN_ADMIN can view all database objects in the DWH of the Data Domain.

### Extra Data Domain

Beside the standard Data Domains there are also extra Data Domains  
An Extra Data Domain provides additional permissions, functions and database connections such as :

- CSV uploads to the Data Domain.
- Read permissions from one Data Domain to additional other Data Domain(s).
- Database connections to Data Domains of other databases.
- Database connections via AD group permissions.
- etc.

These additional permissions, functions or database connections are a matter of negotiation per extra Data Domain.  
The additional permissions, if any, are then added to the standard roles mentioned above for the extra Data Domain.

  

Row Level Security settings on Superset level can be used to additionally restrict the data that is displayed in a dashboard (e.g. only data of the own domain is displayed).

### System Role to Portal Role Mapping

|   |   |   |   |   |
|---|---|---|---|---|
|**System Role**|**Portal Role**|**Portal Permission**|**Menu / Submenu / Page in Portal**|**Info**|
|HELLODATA_ADMIN|SUPERUSER|ROLE_MANAGEMENT|Administration / Portal Rollenverwaltung||
|||MONITORING|Monitoring||
|||DEVTOOLS|Dev Tools||
|||USER_MANAGEMENT|Administration / Benutzerverwaltung||
|||FAQ_MANAGEMENT|Administration / FAQ Verwaltung||
|||EXTERNAL_DASHBOARDS_MANAGEMENT|Unter External Dashboards|Kann neue Einträge erstellen und verwalten bei Seite External Dashboards|
|||DOCUMENTATION_MANAGEMENT|Administration / Dokumentationsmanagement|   |
|||ANNOUNCEMENT_MANAGEMENT|Administration/ Ankündigungen||
|||DASHBOARDS|Dashboards|Sieht im Menu Liste, dann je einen Link auf alle Data Domains auf die er Zugriff hat mit deren Dashboards auf die er Zugriff hat plus Externe Dashboards|
|||DATA_LINEAGE|Data Lineage|Sieht im Menu je einen Lineage Link für alle Data Domains auf die er Zugriff hat|
|||DATA_MARTS|Data Marts|Sieht im Menu je einen Data Mart Link für alle Data Domains auf die er Zugriff hat|
|||DATA_DWH|Data Eng, / DWH Viewer|Sieht im Menu Data Eng. das Submenu DWH Viewer|
|||DATA_ENG|Data Eng. / Orchestration|Sieht im Menu Data Eng. das Submenu Orchestration|
||||||
|BUSINESS_DOMAIN_ADMIN|BUSINESS_DOMAIN_ADMIN|USER_MANAGEMENT|Administration / Portal Rollenverwaltung||
|||FAQ_MANAGEMENT|Dev Tools||
|||EXTERNAL_DASHBOARDS_MANAGEMENT|Administration / Benutzerverwaltung||
|||DOCUMENTATION_MANAGEMENT|Administration / FAQ Verwaltung||
|||ANNOUNCEMENT_MANAGEMENT|Unter External Dashboards||
|||DASHBOARDS|Administration / Dokumentationsmanagement|Sieht im Menu Liste, dann je einen Link auf alle Data Domains auf die er Zugriff hat mit deren Dashboards auf die er Zugriff hat plus Externe Dashboards|
|||DATA_LINEAGE|Administration/ Ankündigungen|Sieht im Menu je einen Lineage Link für alle Data Domains auf die er Zugriff hat|
|||DATA_MARTS|Data Marts|Sieht im Menu je einen Data Mart Link für alle Data Domains auf die er Zugriff hat|
|||DATA_DWH|Data Eng, / DWH Viewer|Sieht im Menu Data Eng. das Submenu DWH Viewer|
|||DATA_ENG|Data Eng. / Orchestration|Sieht im Menu Data Eng. das Submenu Orchestration|
||||||
|DATA_DOMAIN_ADMIN|DATA_DOMAIN_ADMIN|DASHBOARDS|Dashboards|Sieht im Menu Liste, dann je einen Link auf alle Data Domains auf die er Zugriff hat mit deren Dashboards auf die er Zugriff hat plus Externe Dashboards|
|||DATA_LINEAGE|Data Lineage|Sieht im Menu je einen Lineage Link für alle Data Domains auf die er Zugriff hat|
|||DATA_MARTS|Data Marts|Sieht im Menu je einen Data Mart Link für alle Data Domains auf die er Zugriff hat|
|||DATA_DWH|Data Eng, / DWH Viewer|Sieht im Menu Data Eng. das Submenu DWH Viewer|
|||DATA_ENG|Data Eng. / Orchestration|Sieht im Menu Data Eng. das Submenu Orchestration|
||||||
|DATA_DOMAIN_EDITOR|EDITOR|DASHBOARDS|Dashboards|Sieht im Menu Liste, dann je einen Link auf alle Data Domains auf die er Zugriff hat mit deren Dashboards auf die er Zugriff hat plus Externe Dashboards|
|||DATA_LINEAGE|Data Lineage|Sieht im Menu je einen Lineage Link für alle Data Domains auf die er Zugriff hat|
|||DATA_MARTS|Data Marts|Sieht im Menu je einen Data Mart Link für alle Data Domains auf die er Zugriff hat|
||||||
|DATA_DOMAIN_VIEWER|VIEWER|DASHBOARDS|Dashboards|Sieht im Menu Liste, dann je einen Link auf alle Data Domains auf die er Zugriff hat mit deren Dashboards auf die er Zugriff hat plus Externe Dashboards|
|||DATA_LINEAGE|Data Lineage|Sieht im Menu je einen Lineage Link für alle Data Domains auf die er Zugriff hat|

### System Role to Superset Role Mapping

|   |   |   |
|---|---|---|
|**System Role**|**Superset Role**|**Info**|
|No Data Domain role|Public|User should not get access to Superset functions so he gets a role with no permissions.|
|DATA_DOMAIN_VIEWER|BI_VIEWER plus roles forDashboards he was granted access to i. e. the slugified dashboard names with prefix "D_"|Example: User is "DATA_DOMAIN_VIEWER" in a Data Domain. We grant the user acces to the "Hello World" dashboard. Then user gets the role "BI_VIEWER" plus the role "D_hello_world" in Superset.|
|DATA_DOMAIN_EDITOR|BI_EDITOR|Has access to all Dashboards as he is owner of the dashboards  plus he gets SQL Lab permissions.|
|DATA_DOMAIN_ADMIN|BI_EDITOR plus BI_ADMIN|Has access to all Dashboards as he is owner of the dashboards  plus he gets SQL Lab permissions.|

### System Role to Airflow Role Mapping

|   |   |   |
|---|---|---|
|**System Role**|**Airflow Role**|**Info**|
|HELLO_DATA_ADMIN|Admin|User gets DATA_DOMAIN_ADMIN role for all exisitng Data Domains and thus gets his permissions by that roles.<br><br>User additionally gets the Admin role.|
|BUSINESS_DOMAIN_ADMIN||User gets DATA_DOMAIN_ADMIN role for all exisitng Data Domains and thus gets his permissions by that roles.|
|No Data Domain role|Public|User should not get access to Airflow functions so he gets a role with no permissions.|
|DATA_DOMAIN_VIEWER|Public|User should not get access to Airflow functions so he gets a role with no permissions.|
|DATA_DOMAIN_EDITOR|Public|User should not get access to Airflow functions so he gets a role with no permissions.|
|DATA_DOMAIN_ADMIN|AF_OPERATOR plus role corresponding to his Data Domain Key with prefix "DD_"|Example: User is "DATA_DOMAIN_ADMIN" in a Data Domain with the key "data_domain_one". Then user gets the role "AF_OPERATOR" plus the role "DD_data_domain_one" in Airflow.|

