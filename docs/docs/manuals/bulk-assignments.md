# Bulk Assignments

## Overview

The **Bulk Assignments** feature allows administrators to assign data domain roles, dashboards, and dashboard groups to
multiple users at once. Instead of editing each user individually through the user management page, administrators can
use a guided wizard to apply role and resource assignments in bulk across one or more data domains.

!!! info "Required permissions"
Only users with the **USER_MANAGEMENT** permission (typically HELLODATA_ADMIN or BUSINESS_DOMAIN_ADMIN) can access and
use the Bulk Assignments feature.

## Accessing Bulk Assignments

Navigate to **User Management** and click the **Bulk Assignments** button. This opens the Bulk Assignments wizard — a
four-step guided process.

## Wizard Steps

### Step 1: Select Users

The first step presents a paginated grid of all eligible users.

- **Search**: Use the search field to filter users by first name, last name, or email.
- **Filter by role**: Use the dropdown to filter users by their current data domain role (e.g., show only users with
  `DATA_DOMAIN_VIEWER`).
- **Select all**: Use the checkbox to select/deselect all visible (filtered) users at once.
- **Tooltips**: Hover over a user card to see their current data domain role assignments.

!!! info "Excluded users"
Users with **HELLODATA_ADMIN** or **BUSINESS_DOMAIN_ADMIN** business domain roles are automatically excluded from the
list, as their permissions are managed at a higher level.

Click **Next** to proceed once at least one user is selected.

### Step 2: Select Data Domains

Choose one or more data domains where the role assignments should apply.

- All available data domains are listed.
- Use **Select all** to quickly select every domain.
- Each selected domain will be configured independently in the next step.

Click **Next** to proceed once at least one domain is selected.

### Step 3: Assign Roles & Resources

For each selected data domain, configure the following:

#### Role Selection

Choose one of the available data domain roles:

| Role                              | Description                                          |
|-----------------------------------|------------------------------------------------------|
| `DATA_DOMAIN_ADMIN`               | Full access to all resources in the data domain      |
| `DATA_DOMAIN_EDITOR`              | Read/write access to dashboards, SQL lab, data marts |
| `DATA_DOMAIN_VIEWER`              | Read access to specifically assigned dashboards only |
| `DATA_DOMAIN_BUSINESS_SPECIALIST` | Read access to specifically assigned dashboards only |
| `NONE`                            | Removes the user's access to the data domain         |

For a detailed description of each role's permissions, see [Roles & Authorization](role-authorization-concept.md).

#### Dashboard & Dashboard Group Selection

When the selected role is **DATA_DOMAIN_VIEWER** or **DATA_DOMAIN_BUSINESS_SPECIALIST**, additional options appear:

- **Dashboards**: Select individual dashboards the users should have access to.
- **Dashboard Groups**: Select dashboard groups to assign. A dashboard group bundles multiple dashboards together for
  easier management.

!!! warning "Dashboard visibility"
Viewers and Business Specialists can only see dashboards that are explicitly assigned to them. If no dashboards are
selected, the users will have the role but see no dashboards.

Click **Next** to proceed to the summary.

### Step 4: Summary & Review

The summary step presents a carousel view of all planned assignments, one data domain at a time.

#### Review Process

1. The selected users are displayed at the top.
2. Navigate through each data domain using the left/right arrows to review:
    - The **data domain** name
    - The **role** being assigned
    - The **dashboards** and **dashboard groups** (if applicable)
3. A **progress bar** tracks how many domains you have reviewed.
4. The **Apply** button becomes enabled only after all domains have been reviewed.

!!! info "Why review is required"
Bulk assignments can affect many users at once. The mandatory review ensures administrators verify all changes before
they are applied.

#### Applying Changes

Click **Apply** to execute the bulk assignment. A confirmation dialog will appear. After confirming:

- Each selected user is processed individually.
- The result page shows:
    - **Users updated**: Successfully processed users.
    - **Users skipped**: Users whose existing assignments already match the requested configuration (no changes needed).
    - **Users failed**: Users where an error occurred during processing. Error details are shown in an expandable list.

From the result page, you can:

- **Back to user management**: Return to the user management page.
- **Start over**: Begin a new bulk assignment from scratch.

## How It Works

### Role Assignment Logic

For each user and each selected data domain:

1. The **new role** replaces the user's existing role in that specific data domain.
2. Roles in **unselected data domains** are preserved — they are not changed.
3. The user's **business domain role** is never modified by bulk assignments.

### Dashboard & Group Assignment Logic

- Dashboard and group selections **replace** existing assignments for the affected data domains.
- If a role does not support dashboard selection (e.g., `DATA_DOMAIN_ADMIN`, `DATA_DOMAIN_EDITOR`), any previously
  assigned dashboards are cleared for that domain.
- Dashboard group membership is validated server-side: only users with `DATA_DOMAIN_VIEWER` or
  `DATA_DOMAIN_BUSINESS_SPECIALIST` roles can be added to dashboard groups.

### Skip Detection

Users are automatically **skipped** (not updated) when:

- Their current role in every selected domain already matches the requested role, **and**
- Their dashboard group memberships already match the requested configuration.

This avoids unnecessary updates and reduces processing time.

### Error Handling

- If processing fails for one user, the wizard continues with the remaining users.
- Failed users are reported with error details on the result page.
- Common failure causes: user was deleted between selection and application, or a downstream service (e.g., Keycloak) is
  temporarily unavailable.

## Example Scenarios

### Scenario 1: Onboard a team of viewers

A new team of 10 users needs read access to dashboards in the "Finance" data domain.

1. Select the 10 users in Step 1.
2. Select "Finance" in Step 2.
3. Assign `DATA_DOMAIN_VIEWER` and select the relevant dashboards in Step 3.
4. Review and apply in Step 4.

### Scenario 2: Promote editors across multiple domains

5 users need to be upgraded from `DATA_DOMAIN_VIEWER` to `DATA_DOMAIN_EDITOR` in both "Finance" and "HR" domains.

1. Select the 5 users (use the role filter to find current viewers).
2. Select both "Finance" and "HR" domains.
3. Assign `DATA_DOMAIN_EDITOR` to each domain (no dashboard selection needed for editors).
4. Review each domain in the carousel and apply.

### Scenario 3: Revoke access

Remove data domain access for users who have left a project.

1. Select the users.
2. Select the relevant data domains.
3. Assign `NONE` to each domain.
4. Review and apply. Their dashboards and group memberships in those domains will be cleared.

## Technical Notes

- The bulk assignment endpoint is `POST /api/users/bulk-assignments`.
- All user context roles (including the full list of available users with their roles) can be retrieved via
  `GET /api/users/context-roles`.
- Changes are applied transactionally per user — a failure for one user does not roll back changes for others.
- After applying, the portal publishes NATS events to synchronize external tools (Superset, Airflow, etc.) with the
  updated role assignments.

