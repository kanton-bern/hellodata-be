# Dashboard Comments

## Overview

The Dashboard Comments feature allows users to create and manage comments directly within dashboards in the HelloDATA
Portal. Comments support a full publishing workflow with draft and published states, versioning, and role-based access
control.

## Architecture

### Components

#### Backend (Spring Boot)

- **DashboardCommentController** - REST API controller (`/dashboards/{contextKey}/{dashboardId}/comments`)
- **DashboardCommentService** - Business logic and permission validation
- **DashboardCommentPermissionService** - Permission management and synchronization
- **DashboardCommentRepository** - JPA repository for comment database operations
- **DashboardCommentPermissionRepository** - JPA repository for permission database operations
- **DashboardCommentEntity** - JPA entity for comment metadata
- **DashboardCommentVersionEntity** - JPA entity for versioned content
- **DashboardCommentPermissionEntity** - JPA entity for user permissions per data domain

#### Frontend (Angular)

- **CommentsFeed** - Main comments panel component
- **CommentEntryComponent** - Individual comment display with actions
- **DomainDashboardCommentsComponent** - Aggregated view of all comments for a domain
- **DashboardCommentUtilsService** - Shared utilities for comment operations
- **NgRx Store** - State management for comments

### Data Model

The data model consists of three main entities:

1. **DashboardCommentEntry** - Immutable comment metadata
2. **DashboardCommentVersion** - Versioned content with history
3. **DashboardCommentPermission** - User permissions per data domain

**Key Design Points:**

- **`pointerUrl` and `tags`** are stored per-version and derived from the active version when displaying
- **Permissions are independent** of comment data and stored in a separate table
- **Hierarchical permission model** (REVIEW ⊃ WRITE ⊃ READ) is enforced at application layer

```
┌─────────────────────────────────────────────────────────────┐
│                    DashboardCommentEntry                    │
├─────────────────────────────────────────────────────────────┤
│ id: string (UUID)                                           │
│ dashboardId: number                                         │
│ dashboardUrl: string                                        │
│ contextKey: string                                          │
│ author: string                                              │
│ authorEmail: string                                         │
│ createdDate: number (timestamp)                             │
│ deleted: boolean                                            │
│ deletedDate?: number                                        │
│ deletedBy?: string                                          │
│ activeVersion: number                                       │
│ hasActiveDraft?: boolean                                    │
│ entityVersion: number (for optimistic locking)              │
│ importedFromId?: string (original ID when imported)         │
│ history: DashboardCommentVersion[]                          │
│ pointerUrl?: string (derived from active version)           │
│ tags?: string[] (derived from active version)               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  DashboardCommentVersion                    │
├─────────────────────────────────────────────────────────────┤
│ version: number                                             │
│ text: string                                                │
│ status: DashboardCommentStatus                              │
│         (DRAFT | READY_FOR_REVIEW | PUBLISHED | DECLINED    │
│          | DELETED)                                         │
│ editedDate: number                                          │
│ editedBy: string                                            │
│ publishedDate?: number                                      │
│ publishedBy?: string                                        │
│ declineReason?: string                                      │
│ deleted: boolean                                            │
│ pointerUrl?: string (specific page/chart link for version)  │
│ tags?: string[] (tags snapshot for this version)            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│               DashboardCommentPermission                    │
├─────────────────────────────────────────────────────────────┤
│ id: string (UUID)                                           │
│ userId: string (UUID)                                       │
│ contextKey: string                                          │
│ readComments: boolean                                       │
│ writeComments: boolean                                      │
│ reviewComments: boolean                                     │
│ createdDate?: number (timestamp)                            │
│ createdBy?: string                                          │
│ modifiedDate?: number (timestamp)                           │
│ modifiedBy?: string                                         │
└─────────────────────────────────────────────────────────────┘
```

**Relationships:**

- `DashboardCommentEntry` → `DashboardCommentVersion`: One-to-Many (via `history` array)
- `DashboardCommentPermission` → `User`: Many-to-One (via `userId`)
- `DashboardCommentPermission` → `Context`: Many-to-One (via `contextKey`)
- **Unique constraint**: One permission record per `(userId, contextKey)` pair

> **Note:** The top-level `pointerUrl` and `tags` fields on `DashboardCommentEntry` are **derived** from the active
> version in history. They are not stored separately on the comment entity. This ensures that when admins switch
> versions (e.g., `restoreVersion`), the displayed `pointerUrl` and `tags` always match the active version's data.

> **Permission Independence:** The `DashboardCommentPermission` entity is completely independent from comment data,
> allowing administrators to manage commenting permissions separately from comment content. Permissions are
> automatically
> created during user synchronization and can be manually adjusted per user per data domain.

## User Roles and Permissions

### Permission System

The dashboard commenting system uses a **three-level permission model** that is independent of data domain roles:

| Permission Level | Description                          | What it includes                                                        |
|------------------|--------------------------------------|-------------------------------------------------------------------------|
| **READ**         | View published comments only         | Basic read-only access                                                  |
| **WRITE**        | Create and manage own comments       | READ + create/edit own comments + view own drafts + delete own comments |
| **REVIEW**       | Full comment moderation capabilities | WRITE + view/manage all drafts + publish/decline + delete               |

**Key Points:**

- Permissions are stored per user per data domain in `dashboard_comment_permission` table
- **Independent of data domain roles** - a user's commenting permissions can differ from their data access level
- **Hierarchical** - REVIEW includes WRITE capabilities, WRITE includes READ capabilities
- **Flexible** - administrators can assign custom permission combinations for fine-grained control

### Permission Matrix

The table below shows what each permission level allows:

| Action                        | READ | WRITE | REVIEW |
|-------------------------------|------|-------|--------|
| **View published comments**   | ✔    | ✔     | ✔      |
| **View own drafts/declined**  |      | ✔     | ✔      |
| **View all drafts/declined**  |      |       | ✔      |
| **Create comment**            |      | ✔     | ✔      |
| **Edit own comment**          |      | ✔     | ✔      |
| **Edit any comment**          |      |       | ✔      |
| **Delete own version/entire** |      | ✔     | ✔      |
| **Delete any version/entire** |      |       | ✔      |
| **Send for review (own)**     |      | ✔     | ✔      |
| **Publish comment**           |      |       | ✔      |
| **Decline comment**           |      |       | ✔      |
| **View metadata & versions**  |      |       | ✔      |
| **Restore version (own)***    |      | ✔     | ✔      |

**Legend:**

- ✔ = Action allowed
- (empty) = Action not allowed
- \* = Cannot restore version when comment is in READY_FOR_REVIEW status

> **Note for Administrators:** HELLODATA_ADMIN, BUSINESS_DOMAIN_ADMIN, and DATA_DOMAIN_ADMIN roles act as templates for
> default permissions. Their permissions are synced to the `dashboard_comment_permission` table, which is the primary
> source of truth for all authorization checks.

> **Note:** Dashboard-level access (Superset RBAC) is also validated for restricted roles like `DATA_DOMAIN_VIEWER`.
> Even if they have READ permission for comments in the domain, they can only see comments for dashboards they
> can access in Superset.

### Automatic Permission Assignment

When users are synchronized or new domains are created, default permissions are automatically assigned based on portal
roles:

| Portal Role               | Default Comment Permissions | Scope                  |
|---------------------------|-----------------------------|------------------------|
| **HELLODATA_ADMIN**       | READ + WRITE + REVIEW       | All data domains       |
| **BUSINESS_DOMAIN_ADMIN** | READ + WRITE + REVIEW       | All data domains       |
| **DATA_DOMAIN_ADMIN**     | READ + WRITE + REVIEW       | Specific data domain   |
| **Regular Users**         | READ only                   | All accessible domains |
| **NONE role**             | No access                   | Specific data domain   |

> **Important:** These are default assignments. Administrators can manually adjust individual user permissions through
> the User Management UI or database to grant custom access levels.

### Permission Validation

#### Backend (DashboardCommentService)

The backend validates permissions solely by checking the `dashboard_comment_permission` table:

```java
// Check user's comment permissions for a context
private void checkHasAccessToComment(String contextKey) {
    DashboardCommentPermissionEntity perm = getCommentPermissionForCurrentUser(contextKey);
    if (perm == null || !perm.isWriteComments()) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No write permission for comments");
    }
}
```

#### Frontend (NgRx Selectors)

Frontend selectors check permissions from the NgRx store state:

```typescript
// Check if user can publish comments (requires REVIEW permission)
export const canPublishComment = createSelector(
    selectCurrentDashboardContextKey,
    selectCurrentUserCommentPermissions,
    selectIsSuperuser,
    selectIsBusinessDomainAdmin,
    (state: AppState) => state.auth,
    (contextKey, permissions, isSuperuser, isBusinessDomainAdmin, authState) => {
        // Admins always have full access
        if (isSuperuser || isBusinessDomainAdmin) {
            return true;
        }

        // Check if user is data domain admin for this context
        const isDataDomainAdmin = authState.contextRoles.some(role =>
            role.context.contextKey === contextKey &&
            role.role.name === DATA_DOMAIN_ADMIN_ROLE
        );

        if (isDataDomainAdmin) {
            return true;
        }

        // Check REVIEW permission from permission table
        return permissions?.reviewComments || false;
    }
);

// Check if user can create/edit comments (requires WRITE permission)
export const canEditComment = createSelector(
    selectCurrentDashboardContextKey,
    selectCurrentUserCommentPermissions,
    selectCurrentUserEmail,
    (contextKey, permissions, currentUserEmail) => (comment) => {
        // User can edit their own comments with WRITE permission
        if (comment.authorEmail === currentUserEmail && permissions?.writeComments) {
            return true;
        }

        // Or edit any comment with REVIEW permission
        return permissions?.reviewComments || false;
    }
);
```

## Comment Lifecycle

### States

```
  ┌──────────┐   send for review   ┌──────────────────┐    publish     ┌───────────┐
  │  DRAFT   │ ──────────────────► │ READY_FOR_REVIEW │ ─────────────► │ PUBLISHED │
  └──────────┘                     └──────────────────┘                └───────────┘
       ▲                                   │                                 │
       │              decline              │                                 │
       └───────────────────────────────────┘                                 │
                                                                             │
       ▲                                                                     │
       │                          edit published                             │
       └─────────────────────────────────────────────────────────────────────┘
```

### Workflow

1. **Create Comment** - User creates a comment (status: DRAFT, version: 1)
2. **Edit Draft** - Author or admin can edit the draft text
3. **Send for Review** - Author sends the draft for moderation (status: READY_FOR_REVIEW)
4. **Decline** - Reviewer declines the draft with a reason (status: DECLINED)
5. **Publish** - Reviewer publishes the comment (status: PUBLISHED)
6. **Edit Published** - Creates a new DRAFT version while keeping the published version active
7. **Delete** - Soft deletes current version; restores last published if available. If `deleteEntire` is true, soft
   deletes the entire comment entity.

### Versioning

Each edit to a published comment creates a new version:

```
Version 1 (PUBLISHED) ─► Version 2 (DRAFT) ─► Version 2 (PUBLISHED) ─► Version 3 (DRAFT)
                                                       │
                                               (activeVersion = 3)
```

- **activeVersion** - Points to the currently active version
- **hasActiveDraft** - True when a draft version exists
- Admins can restore any non-deleted PUBLISHED version

## API Endpoints

| Method   | Endpoint                                                                              | Description                          |
|----------|---------------------------------------------------------------------------------------|--------------------------------------|
| `GET`    | `/dashboards/{contextKey}/{dashboardId}/comments`                                     | Get all visible comments             |
| `POST`   | `/dashboards/{contextKey}/{dashboardId}/comments`                                     | Create new comment                   |
| `PUT`    | `/dashboards/{contextKey}/{dashboardId}/comments/{commentId}`                         | Update draft comment                 |
| `DELETE` | `/dashboards/{contextKey}/{dashboardId}/comments/{commentId}`                         | Delete comment version or entire     |
| `POST`   | `/dashboards/{contextKey}/{dashboardId}/comments/{commentId}/send-for-review`         | Send draft for review                |
| `POST`   | `/dashboards/{contextKey}/{dashboardId}/comments/{commentId}/decline`                 | Decline draft with reason            |
| `POST`   | `/dashboards/{contextKey}/{dashboardId}/comments/{commentId}/publish`                 | Publish comment                      |
| `POST`   | `/dashboards/{contextKey}/{dashboardId}/comments/{commentId}/clone`                   | Clone for edit (creates new version) |
| `POST`   | `/dashboards/{contextKey}/{dashboardId}/comments/{commentId}/restore/{versionNumber}` | Restore specific version             |
| `GET`    | `/dashboards/{contextKey}/{dashboardId}/comments/tags`                                | Get all tags for dashboard           |
| `GET`    | `/dashboards/{contextKey}/{dashboardId}/comments/export`                              | Export comments to JSON              |
| `POST`   | `/dashboards/{contextKey}/{dashboardId}/comments/import`                              | Import comments from JSON            |

### Request/Response Examples

#### Create Comment

```json
// POST /dashboards/demo/5/comments
// Request:
{
  "text": "This dashboard shows sales metrics",
  "dashboardUrl": "https://superset.example.com/superset/dashboard/5/",
  "pointerUrl": "https://superset.example.com/superset/dashboard/5/?tab=sales"
}

// Response:
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "dashboardId": 5,
  "contextKey": "demo",
  "author": "John Doe",
  "authorEmail": "john.doe@example.com",
  "createdDate": 1705827600000,
  "activeVersion": 1,
  "entityVersion": 0,
  "history": [
    {
      "version": 1,
      "text": "This dashboard shows sales metrics",
      "status": "DRAFT",
      "editedDate": 1705827600000,
      "editedBy": "John Doe",
      "deleted": false
    }
  ]
}
```

#### Update Comment

```json
// PUT /dashboards/demo/5/comments/550e8400-e29b-41d4-a716-446655440000
// Request:
{
  "text": "Updated comment text",
  "entityVersion": 0
}
```

## Optimistic Locking

To prevent concurrent edit conflicts, the system uses optimistic locking via `entityVersion`:

1. Client sends `entityVersion` with update request
2. Backend verifies `entityVersion` matches current value
3. If mismatch, returns `409 Conflict` error
4. Client receives notification and refreshes comments

```java
private void checkEntityVersion(DashboardCommentEntity comment, Integer providedVersion, String userEmail) {
    if (providedVersion != null && !providedVersion.equals(comment.getEntityVersion())) {
        log.warn("Optimistic lock conflict for comment {}, current: {}, provided: {}",
                comment.getId(), comment.getEntityVersion(), providedVersion);
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Comment was modified by another user. Please refresh and try again.");
    }
}
```

## Visibility Rules

### For Regular Users (READ)

- See all PUBLISHED comments
- If current activeVersion is a DRAFT, READY_FOR_REVIEW or DECLINED by someone else → show last PUBLISHED version
  instead
- If no PUBLISHED version exists → comment is hidden

### For Authors (WRITE)

- See all rules for Regular Users
- See own DRAFT, READY_FOR_REVIEW, and DECLINED versions
- Author can see their own declined version to read the decline reason

### For Reviewers (REVIEW)

- See all comments (DRAFT, READY_FOR_REVIEW, PUBLISHED, DECLINED)
- See complete version history
- Can switch between versions for any comment

### Dashboard Access Restriction

Users with certain roles (e.g., `DATA_DOMAIN_VIEWER`, `BUSINESS_SPECIALIST`) are restricted to seeing comments only on
dashboards they have explicit access to via Superset RBAC. This is enforced by the backend by matching user roles
against the required roles for each dashboard.

## Consolidated Deletion Logic

The deletion process has been simplified into a single "Delete" action:

1. **Delete Version (Default)**: By default, the delete action soft-deletes the current active version (sets status to
   `DELETED`).
    - If a previous `PUBLISHED` version exists, the comment is restored to that version.
    - If no published version exists, the entire comment is soft-deleted.
2. **Delete Entire Comment**: If the user explicitly selects "Delete entire comment", the entire comment entity is
   soft-deleted regardless of version history.

Authors can delete their own DRAFT or DECLINED versions. Reviewers can delete any version or entire comments.

## Features

### Pointer URL

Comments can include an optional `pointerUrl` that links to a specific dashboard page, tab, or chart:

- Validated to ensure it points to the same Superset instance
- Clicking the link loads that specific view in the iframe

### Tags

Comments can be tagged with short labels for better organization and filtering:

- **Scope**: Tags are scoped per dashboard - each dashboard has its own set of tags
- **Length**: Maximum 10 characters per tag
- **Normalization**: Tags are automatically trimmed, lowercased, and deduplicated
- **Multiple tags**: A comment can have multiple tags assigned
- **Autocomplete**: When adding tags, existing tags from the dashboard are suggested
- **Filtering**: Comments can be filtered by tag in the comments panel
- **Permissions**: Adding/editing tags follows the same permission rules as editing comments
- **History tracking**: Each version stores a snapshot of tags - changes to tags are visible in version history

#### Tag Display

- Tags are displayed at the bottom of each comment entry
- Each tag shows with a tag icon (`fa-solid fa-tag`) followed by the tag name
- Tags have a distinctive blue chip styling for easy identification
- In version history, tags for each version are shown, allowing comparison of tag changes

#### Tag API

```
GET /dashboards/{contextKey}/{dashboardId}/comments/tags
```

Returns all unique tags used in comments for a specific dashboard.

- Useful for referencing specific parts of complex dashboards

### Filtering

Domain comments view supports filtering by:

- Year
- Quarter (based on comment creation date)
- Tag (if any comments have tags assigned)
- Text search (searches comment text content)

### Auto-refresh

Comments are automatically refreshed every 30 seconds when viewing a dashboard.

## Database Schema

### Main Tables

```sql
CREATE TABLE dashboard_comment
(
    id               VARCHAR(36) PRIMARY KEY,
    dashboard_id     INTEGER      NOT NULL,
    dashboard_url    VARCHAR(2048),
    context_key      VARCHAR(255) NOT NULL,
    author           VARCHAR(255),
    author_email     VARCHAR(255),
    created_date     BIGINT,
    deleted          BOOLEAN DEFAULT FALSE,
    deleted_date     BIGINT,
    deleted_by       VARCHAR(255),
    active_version   INTEGER,
    has_active_draft BOOLEAN DEFAULT FALSE,
    entity_version   INTEGER DEFAULT 0,
    imported_from_id VARCHAR(36) -- Original ID when imported from another dashboard
);

CREATE TABLE dashboard_comment_version
(
    id             BIGSERIAL PRIMARY KEY,
    comment_id     VARCHAR(36) REFERENCES dashboard_comment (id),
    version        INTEGER NOT NULL,
    text           TEXT,
    status         VARCHAR(20),
    edited_date    BIGINT,
    edited_by      VARCHAR(255),
    published_date BIGINT,
    published_by   VARCHAR(255),
    deleted        BOOLEAN DEFAULT FALSE,
    tags           TEXT,         -- Comma-separated tags snapshot for this version
    pointer_url    VARCHAR(2000) -- Optional link to specific page/chart for this version
);

CREATE TABLE dashboard_comment_permission
(
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL,
    context_key     VARCHAR(100) NOT NULL,
    read_comments   BOOLEAN      NOT NULL DEFAULT false,
    write_comments  BOOLEAN      NOT NULL DEFAULT false,
    review_comments BOOLEAN      NOT NULL DEFAULT false,
    created_date    TIMESTAMP,
    created_by      VARCHAR(255),
    modified_date   TIMESTAMP,
    modified_by     VARCHAR(255),
    CONSTRAINT uq_dashboard_comment_permission_user_context UNIQUE (user_id, context_key),
    CONSTRAINT fk_dashboard_comment_permission_user FOREIGN KEY (user_id)
        REFERENCES user_ (id) ON DELETE CASCADE,
    CONSTRAINT fk_dashboard_comment_permission_context FOREIGN KEY (context_key)
        REFERENCES context (context_key) ON DELETE CASCADE
);

-- Indexes for comment lookups
CREATE INDEX idx_dashboard_comment_context_dashboard
    ON dashboard_comment (context_key, dashboard_id);

-- Index for import duplicate detection
CREATE INDEX idx_comment_imported_from
    ON dashboard_comment (imported_from_id, context_key, dashboard_id);

-- Indexes for permission lookups
CREATE INDEX idx_comment_permission_user
    ON dashboard_comment_permission (user_id);

CREATE INDEX idx_comment_permission_context
    ON dashboard_comment_permission (context_key);
```

### Table Descriptions

#### dashboard_comment

Main comment entity storing metadata and reference to active version. Uses optimistic locking via `entity_version`.

#### dashboard_comment_version

Stores complete version history including text, status, tags, and pointer URLs. Each version is immutable once created.

#### dashboard_comment_permission

Stores per-user per-data-domain commenting permissions. Independent of comment data, allowing flexible permission
management.

> **Note:** `pointer_url` and `tags` are stored **per version** in `dashboard_comment_version`, not on the main
> `dashboard_comment` table. This design ensures each version snapshot captures the complete state (text, tags,
> pointerUrl) at that point in time. The application derives the current `pointerUrl` and `tags` from the active
> version when building DTOs.

### Permission Table Details

The `dashboard_comment_permission` table provides fine-grained control over commenting capabilities:

- **Unique constraint** on `(user_id, context_key)` ensures one permission record per user per domain
- **Cascade delete** on user and context ensures automatic cleanup
- **Boolean flags** represent the three permission levels (read, write, review)
- **Hierarchical permissions** are enforced by the application layer (review → write → read)
- **Audit fields** track when and by whom permissions were created/modified
- **Independent storage** allows permission management without affecting comment data

## Permission Management and Synchronization

### Automatic Permission Initialization

#### Initial Setup via Liquibase Migration

When the system is first deployed or upgraded, a Liquibase migration (`55_initialize_dashboard_comment_permissions.sql`)
automatically creates default permissions for all existing users:

**Migration Logic:**

1. **Admins (HELLODATA_ADMIN, BUSINESS_DOMAIN_ADMIN)** → Full access (READ + WRITE + REVIEW) in all domains
2. **Data Domain Admins (DATA_DOMAIN_ADMIN)** → Full access (READ + WRITE + REVIEW) in their specific domain
3. **Regular users** → Read-only access (READ) in all domains

The migration uses SQL JOINs with `user_portal_role` and `user_context_role` tables to determine user roles and is *
*idempotent** - running it multiple times will not create duplicates.

#### User Synchronization (syncAllUsers)

The `syncAllUsers()` method in `UserService` automatically maintains comment permissions when users are synchronized:

```java
// In UserService.syncAllUsers()
dashboardCommentPermissionService.syncDefaultPermissionsForUser(userEntity);
```

**When it runs:**

- During scheduled user synchronization
- When new users are created
- When new data domains are added

**What it does:**

- Checks if user has admin roles (HELLODATA_ADMIN or BUSINESS_DOMAIN_ADMIN)
- Creates missing permissions for any data domains where user doesn't have permissions yet
- **Never modifies existing permissions** - only creates new ones for new domains
- Logs the number of permissions created for each user

**Permission Assignment:**

```java
if(isAdmin){
        // Admins get full access
        permission.

setReadComments(true);
    permission.

setWriteComments(true);
    permission.

setReviewComments(true);
}else{
        // Regular users get read-only
        permission.

setReadComments(true);
    permission.

setWriteComments(false);
    permission.

setReviewComments(false);
}
```

### Use Cases for Automatic Synchronization

#### 1. New Data Domain Added

```
Scenario: Administrator creates a new data domain "finance"
Result:   syncAllUsers() runs automatically
          - All admins receive READ+WRITE+REVIEW for "finance"
          - All regular users receive READ for "finance"
```

#### 2. New User Created

```
Scenario: New user "john@example.com" is added to the system
Result:   syncAllUsers() processes the new user
          - Permissions created for all existing data domains
          - Permission level based on user's portal role
```

#### 3. User Promoted to Admin

```
Scenario: Regular user is promoted to BUSINESS_DOMAIN_ADMIN
Result:   syncAllUsers() does NOT automatically upgrade existing permissions
Action:   Administrator must manually update permissions via database
          OR re-run migration to reset all permissions
```

### Best Practices

1. **New Domains**: Always run user sync after creating new data domains to ensure all users get appropriate permissions
2. **Role Changes**: When promoting users to admin roles, consider manually updating their comment permissions if
   immediate access is needed
3. **Permission Auditing**: Use `created_by` and `modified_by` fields to track permission changes
4. **Regular Sync**: Schedule regular `syncAllUsers()` execution to catch any missing permissions

### Troubleshooting

**Problem:** User cannot see comment actions despite having correct domain role

**Solution:**

1. Check `dashboard_comment_permission` table for user's permissions
2. Verify `context_key` matches the current dashboard's domain
3. Run `syncAllUsers()` to create missing permissions
4. Check application logs for permission validation errors

**SQL Query to Check Permissions:**

```sql
SELECT u.email,
       dcp.context_key,
       dcp.read_comments,
       dcp.write_comments,
       dcp.review_comments
FROM dashboard_comment_permission dcp
         JOIN user_ u ON dcp.user_id = u.id
WHERE u.email = 'user@example.com';
```

## Error Handling

| HTTP Status     | Scenario                                      |
|-----------------|-----------------------------------------------|
| `403 Forbidden` | User lacks permission for the action          |
| `404 Not Found` | Comment not found                             |
| `409 Conflict`  | Optimistic locking conflict (concurrent edit) |

## Import/Export

Comments can be exported and imported between dashboards for migration, backup, or copying comments across environments.

### Export

- **Format**: JSON file
- **Scope**: Exports all non-deleted comments with complete version history (both DRAFT and PUBLISHED)
- **Contents**: Comment ID, text, author, creation date, status, tags, activeVersion, and full version history
- **Permission**: Only admins (superuser, business_domain_admin, data_domain_admin) can export
- **File naming**: `comments_{contextKey}_{dashboardTitle}_{date}.json` (title is sanitized for filesystem)

#### Export Format

```json
{
  "exportVersion": "1.0",
  "contextKey": "demo",
  "dashboardId": 5,
  "dashboardTitle": "Sales Dashboard",
  "exportDate": 1705827600000,
  "comments": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "text": "Current active version text",
      "author": "John Doe",
      "authorEmail": "john.doe@example.com",
      "createdDate": 1705827600000,
      "status": "PUBLISHED",
      "activeVersion": 2,
      "tags": [
        "sales",
        "kpi"
      ],
      "history": [
        {
          "version": 1,
          "text": "First version text",
          "status": "PUBLISHED",
          "editedDate": 1705827600000,
          "editedBy": "John Doe",
          "publishedDate": 1705828000000,
          "publishedBy": "Admin User",
          "tags": [
            "sales"
          ]
        },
        {
          "version": 2,
          "text": "Current active version text",
          "status": "PUBLISHED",
          "editedDate": 1705830000000,
          "editedBy": "Jane Smith",
          "publishedDate": 1705831000000,
          "publishedBy": "Admin User",
          "tags": [
            "sales",
            "kpi"
          ]
        }
      ]
    }
  ]
}
```

### Import

- **Permission**: Only admins (superuser, business_domain_admin, data_domain_admin) can import
- **Status preservation**: Comments are imported with their original status (DRAFT or PUBLISHED) preserved
- **History preservation**: Full version history is imported, allowing admins to review previous versions
- **Pointer URL**: Preserved per-version from export file (stored on each version in history)
- **Dashboard URL**: Automatically set to the target dashboard URL (not copied from source)
- **Tags**: Preserved per-version from export file
- **Author**: Preserved from export if available, otherwise uses importing user

#### Import Scenarios

##### 1. First Import (Same Dashboard - Re-import)

When importing to the same dashboard where comments were originally exported:

- Comments with matching IDs are **updated** with imported data
- New comments (no matching ID) are created
- Prevents duplicate comments on re-import

##### 2. Import from Different Dashboard

When importing to a different dashboard:

- New comments are created with new IDs
- The `importedFromId` field tracks the original comment ID
- On subsequent imports of the same file, comments are **updated** (not duplicated) based on `importedFromId`

##### 3. Cross-Environment Import

When importing from another environment (e.g., DEV → PROD):

- Comments are created with new IDs
- Original author information is preserved
- `importedFromId` enables re-import without duplicates

#### Import Result

```json
{
  "imported": 5,
  "updated": 2,
  "skipped": 1,
  "message": "Imported 5 new, updated 2 existing, skipped 1"
}
```

- **imported**: Number of new comments created
- **updated**: Number of existing comments updated
- **skipped**: Number of invalid items (e.g., empty text)

#### Import Validation

- File must be valid JSON
- Comments without text are skipped
- Invalid tags are normalized or skipped
- Dashboard URL is replaced with target dashboard URL

### Import/Export Use Cases

1. **Backup**: Export comments before major changes
2. **Migration**: Move comments when restructuring dashboards
3. **Template**: Create standard comments that can be imported to multiple dashboards
4. **Cross-environment**: Copy comments from development to production environment
5. **Disaster recovery**: Restore comments from backup after data loss

### Import Tracking (importedFromId)

The `importedFromId` field in the database tracks the original comment ID from import. This enables:

- Detection of previously imported comments to prevent duplicates
- Traceability of comment origins
- Safe re-imports that update existing comments instead of creating duplicates

## Mobile Support

The comments panel adapts to mobile view:

- Comments panel opens as a drawer from the top
- Simplified UI for touch interaction
- Full functionality preserved

