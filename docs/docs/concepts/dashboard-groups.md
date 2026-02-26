# Dashboard Groups

## Overview

Dashboard Groups allow administrators to organize Superset dashboards into logical collections and assign them to users within a specific Data Domain. Instead of granting access to dashboards individually for each user, administrators can create a group (e.g. "Finance Reports", "HR Analytics"), add the relevant dashboards to it, and then assign users to the group. All members of a group automatically receive viewer access to the dashboards contained within that group.

This mechanism simplifies user management — especially in environments with many dashboards and users — by providing a single point of control for dashboard access.

## Key Concepts

### What is a Dashboard Group?

A Dashboard Group is a named collection that brings together:

- **A set of dashboards** — selected from the dashboards available in a given Data Domain
- **A set of users** — selected from users who have an eligible role in that Data Domain

Each group is scoped to exactly one **Data Domain** (identified by `contextKey`). A Data Domain can have many groups, and each group manages its own list of dashboards and members independently.

### How does it work?

1. An administrator creates a new Dashboard Group within a chosen Data Domain.
2. The administrator selects which dashboards should belong to the group.
3. The administrator selects which users should be members of the group.
4. Once the group is saved, the system automatically synchronizes the dashboard permissions to **Apache Superset** — members of the group receive **viewer** access to all dashboards in the group.

When the group is later modified (dashboards added/removed, users added/removed) or deleted, the system re-synchronizes the affected users so that their Superset permissions always reflect the current state of the groups.

## Access Control

### Who can manage Dashboard Groups?

Dashboard Group management is restricted to users with the **`DASHBOARD_GROUPS_MANAGEMENT`** authority. This is typically granted to administrators responsible for managing data access within the portal.

All operations — listing, creating, editing, deleting groups, as well as viewing eligible users — require this authority.

### Which users can be added to a group?

Only users with one of the following Data Domain roles are eligible for group membership:

| Role                             | Description                                                     |
|----------------------------------|-----------------------------------------------------------------|
| **DATA_DOMAIN_VIEWER**           | Standard viewer with read-only access to domain resources       |
| **DATA_DOMAIN_BUSINESS_SPECIALIST** | Business specialist with access to dashboards in the domain  |

Users with higher roles (e.g. `DATA_DOMAIN_ADMIN`) are not listed as eligible group members because they typically already have broader access to all dashboards.

### What happens when a user's role changes?

If a user's Data Domain role is changed to a role that is **not** `DATA_DOMAIN_VIEWER` or `DATA_DOMAIN_BUSINESS_SPECIALIST`, the user is **automatically removed** from all Dashboard Groups in that Data Domain. This ensures that group memberships stay consistent with the user's actual role.

## Dashboard Access: Direct vs. Group-Based

HelloDATA supports two ways of granting dashboard access to users:

| Method                  | Description                                                                      |
|-------------------------|----------------------------------------------------------------------------------|
| **Direct assignment**   | An administrator assigns individual dashboards directly to a user                |
| **Dashboard Group**     | A user receives dashboard access through membership in one or more groups        |

Both methods coexist. When determining a user's final set of dashboards for Superset synchronization, the system **merges** direct assignments with group-based assignments. If the same dashboard appears in both a direct assignment and a group, it is included only once — the user still gets viewer access.

## Validation Rules

The system enforces the following rules when creating or updating Dashboard Groups:

- **Name is required** and must be between 3 and 150 characters.
- **Name must be unique** within the same Data Domain (case-insensitive). Attempting to create or rename a group to a name that already exists in that Data Domain will result in a conflict error.
- **Name must start** with a letter or a digit.
- **A Data Domain must be selected** — every group is scoped to exactly one Data Domain.

## Synchronization with Superset

Dashboard Groups are tightly integrated with the Superset permission synchronization pipeline:

- **On group creation** — if the group contains both dashboards and users, all member users are synchronized.
- **On group update** — the system detects which users were added or removed, and whether the dashboard list changed. Only affected users are re-synchronized. If the dashboard list changed, all current and former members are synchronized.
- **On group deletion** — all users who were members of the deleted group are re-synchronized so that their excess permissions are revoked.

Synchronization happens **asynchronously** after the database transaction is committed, ensuring data consistency.

## Automatic Cleanup

When dashboards are removed or become unavailable in a Data Domain (e.g. deleted from Superset), the system automatically **cleans up stale entries** from Dashboard Groups. Any dashboard entry referencing a dashboard that no longer exists is removed from all groups in the affected Data Domain. This prevents groups from containing references to non-existent dashboards.

## Batch Import Support

Dashboard Groups can also be assigned to users via the **CSV batch import** mechanism. The CSV file supports an optional `Dashboard Group` column where group names can be specified (comma-separated within the column). During import, group names are resolved to their IDs, and users are assigned to the corresponding groups. If a referenced group name does not exist in the target Data Domain, the import will report an error.

## API Reference

All Dashboard Group operations are exposed through the Portal REST API under the `/dashboard-groups` endpoint. The entire endpoint requires the **`DASHBOARD_GROUPS_MANAGEMENT`** authority.

| Method     | Path                              | Description                                              |
|------------|-----------------------------------|----------------------------------------------------------|
| `GET`      | `/dashboard-groups`               | List groups for a Data Domain (paginated, with search)   |
| `GET`      | `/dashboard-groups/{id}`          | Get a single group by ID                                 |
| `GET`      | `/dashboard-groups/eligible-users`| Get users eligible for group membership in a Data Domain |
| `POST`     | `/dashboard-groups`               | Create a new Dashboard Group                             |
| `PUT`      | `/dashboard-groups`               | Update an existing Dashboard Group                       |
| `DELETE`   | `/dashboard-groups/{id}`          | Delete a Dashboard Group                                 |

**Query parameters for listing:**

- `contextKey` (required) — the Data Domain key
- `page`, `size` — pagination
- `sort` — sorting field
- `search` — free-text search filtering by group name or dashboard title
