# RLS Groups (Row Level Security)

## Overview

Every Superset instance of a Data Domain contains the 15 row level security roles `RLS_01` to `RLS_15`. Superset row level
security filters are bound to these roles, so a user only sees the rows of a dataset that the filters of their RLS roles
allow.

In the portal these roles are called **RLS Groups**. A BI viewer can be assigned to 1 - 15 RLS Groups per Data Domain,
either directly in the user management or through a Dashboard Group. The assignment is optional.

## Assigning users to RLS Groups

In the user management (`Benutzerverwaltung`), every Data Domain card of a user with the role **DATA_DOMAIN_VIEWER** or
**DATA_DOMAIN_BUSINESS_SPECIALIST** shows an **RLS Groups** section next to the dashboard selection. Selecting groups
there assigns the corresponding `RLS_xx` roles to the user in the Superset instance of that Data Domain.

This is available to users with the **`USER_MANAGEMENT`** authority (HelloDATA admins and business domain admins).

When the role of a user in a Data Domain is changed to a role other than viewer or business specialist, the RLS Group
assignments of that Data Domain are removed.

## Assigning Dashboard Groups to RLS Groups

A Dashboard Group can also select RLS Groups (tab **RLS Groups** on the Dashboard Group edit page). All members of the
group receive these RLS roles. The final set of RLS roles of a user is the union of the directly assigned RLS Groups and
the RLS Groups of all Dashboard Groups the user is a member of.

## Naming RLS Groups

By default an RLS Group is named after its role ID (`RLS_01` - `RLS_15`). On the page **Administration > RLS Groups**
a meaningful name can be set per Data Domain (i.e. `RLS_01` -> "Region Bern"). The name is shown in the user management
and on the Dashboard Group edit page. Leaving the name empty restores the default name. Names must be unique within a
Data Domain.

The page requires the **`RLS_ROLES_MANAGEMENT`** authority:

| Role                      | Data Domains that can be managed               |
|---------------------------|------------------------------------------------|
| **HELLODATA_ADMIN**       | all                                            |
| **BUSINESS_DOMAIN_ADMIN** | all                                            |
| **DATA_DOMAIN_ADMIN**     | only the Data Domains the user is an admin of |

## Synchronization with Superset

The portal is the source of truth for the RLS roles. Every user synchronization sends the complete list of RLS roles per
Data Domain to the Superset sidecar, which removes all `RLS_xx` roles of the user and assigns the listed ones. Users with
a role other than viewer or business specialist get no RLS roles.

- The CSV batch import keeps working: `RLS_xx` entries in the `supersetRole` column are stored as the user's RLS Groups.
- On the first start of this version, the RLS roles users already have in Superset (i.e. from a previous CSV import) are
  imported once into the portal, so they are not revoked by the next synchronization.
