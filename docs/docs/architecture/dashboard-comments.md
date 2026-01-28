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
- **DashboardCommentRepository** - JPA repository for database operations
- **DashboardCommentEntity** / **DashboardCommentVersionEntity** - JPA entities

#### Frontend (Angular)

- **CommentsFeed** - Main comments panel component
- **CommentEntryComponent** - Individual comment display with actions
- **DomainDashboardCommentsComponent** - Aggregated view of all comments for a domain
- **DashboardCommentUtilsService** - Shared utilities for comment operations
- **NgRx Store** - State management for comments

### Data Model

The data model separates immutable comment metadata from versioned content. **`pointerUrl` and `tags` are stored
per-version** and derived from the active version when displaying the comment.

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
│ status: DashboardCommentStatus (DRAFT | PUBLISHED)          │
│ editedDate: number                                          │
│ editedBy: string                                            │
│ publishedDate?: number                                      │
│ publishedBy?: string                                        │
│ deleted: boolean                                            │
│ pointerUrl?: string (specific page/chart link for version)  │
│ tags?: string[] (tags snapshot for this version)            │
└─────────────────────────────────────────────────────────────┘
```

> **Note:** The top-level `pointerUrl` and `tags` fields on `DashboardCommentEntry` are **derived** from the active
> version in history. They are not stored separately on the comment entity. This ensures that when admins switch
> versions (e.g., `restoreVersion`), the displayed `pointerUrl` and `tags` always match the active version's data.

## User Roles and Permissions

### Role Types

| Role                      | Description                                                            |
|---------------------------|------------------------------------------------------------------------|
| **Superuser**             | HELLODATA ADMIN - has full access to all comments                      |
| **Business Domain Admin** | Admin for all data domains within a business domain                    |
| **Data Domain Admin**     | Admin for a specific data domain (contextKey)                          |
| **Data Domain Editor**    | User with edit access to dashboards within a data domain               |
| **Data Domain Viewer**    | User with view-only access to assigned dashboards within a data domain |

> **Note:** Users with `DATA_DOMAIN_VIEWER` role can only see comments for dashboards that are explicitly assigned to
> them. Dashboard access is validated by the frontend before loading comments.

### Permission Matrix

| Action                       | Data Domain Viewer             | Data Domain Editor | Data Domain Admin | Business Domain Admin | Superuser |
|------------------------------|--------------------------------|--------------------|-------------------|-----------------------|-----------|
| **View published comments**  | Yes (assigned dashboards only) | Yes                | Yes               | Yes                   | Yes       |
| **View own drafts**          | Yes (assigned dashboards only) | Yes                | Yes               | Yes                   | Yes       |
| **View all drafts**          | No                             | No                 | Yes               | Yes                   | Yes       |
| **Create comment**           | Yes (assigned dashboards only) | Yes                | Yes               | Yes                   | Yes       |
| **Edit own comment**         | Yes (assigned dashboards only) | Yes                | Yes               | Yes                   | Yes       |
| **Edit any comment**         | No                             | No                 | Yes               | Yes                   | Yes       |
| **Delete own comment**       | Yes (assigned dashboards only) | Yes                | Yes               | Yes                   | Yes       |
| **Delete any comment**       | No                             | No                 | Yes               | Yes                   | Yes       |
| **Publish comment**          | No                             | No                 | Yes               | Yes                   | Yes       |
| **Unpublish comment**        | No                             | No                 | Yes               | Yes                   | Yes       |
| **View metadata & versions** | No                             | No                 | Yes               | Yes                   | Yes       |
| **Restore version**          | No                             | No                 | Yes               | Yes                   | Yes       |
| **Export comments**          | No                             | No                 | Yes               | Yes                   | Yes       |
| **Import comments**          | No                             | No                 | Yes               | Yes                   | Yes       |

### Permission Validation

#### Backend (DashboardCommentService)

```java
// Check if user is admin for a specific context
private boolean isAdminForContext(String userEmail, String contextKey) {
    return userRepository.findUserEntityByEmailIgnoreCase(userEmail)
            .map(user -> {
                // Business domain admin has access to all contexts
                if (Boolean.TRUE.equals(user.isBusinessDomainAdmin())) {
                    return true;
                }
                // Data domain admin only for specific context
                return user.getContextRoles().stream()
                        .anyMatch(role ->
                                contextKey.equals(role.getContextKey()) &&
                                        HdRoleName.DATA_DOMAIN_ADMIN.equals(role.getRole().getName())
                        );
            })
            .orElse(false);
}
```

#### Frontend (NgRx Selectors)

```typescript
export const canPublishComment = createSelector(
    selectCurrentDashboardContextKey,
    selectIsSuperuser,
    selectIsBusinessDomainAdmin,
    (state: AppState) => state.auth,
    (contextKey, isSuperuser, isBusinessDomainAdmin, authState) => (comment) => {
        // Check if user is data_domain_admin for this specific context
        const isDataDomainAdmin = authState.contextRoles.some(role =>
            role.context.contextKey === contextKey &&
            role.role.name === DATA_DOMAIN_ADMIN_ROLE
        );

        return isSuperuser || isBusinessDomainAdmin || isDataDomainAdmin;
    }
);
```

## Comment Lifecycle

### States

```
  ┌──────────┐     publish      ┌───────────┐
  │  DRAFT   │ ───────────────► │ PUBLISHED │
  └──────────┘                  └───────────┘
       ▲                              │
       │          unpublish           │
       └──────────────────────────────┘
```

### Workflow

1. **Create Comment** - User creates a comment (status: DRAFT, version: 1)
2. **Edit Draft** - Author or admin can edit the draft text
3. **Publish** - Admin publishes the comment (status: PUBLISHED)
4. **Edit Published** - Creates a new DRAFT version while keeping the published version
5. **Publish New Version** - Admin publishes the new version
6. **Unpublish** - Admin can revert published comment to draft
7. **Delete** - Soft deletes current version; restores last published if available

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
| `DELETE` | `/dashboards/{contextKey}/{dashboardId}/comments/{commentId}`                         | Delete comment                       |
| `POST`   | `/dashboards/{contextKey}/{dashboardId}/comments/{commentId}/publish`                 | Publish comment                      |
| `POST`   | `/dashboards/{contextKey}/{dashboardId}/comments/{commentId}/unpublish`               | Unpublish comment                    |
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

### For Regular Users

- See all PUBLISHED comments
- See own DRAFT comments
- If current activeVersion is a DRAFT by someone else → show last PUBLISHED version instead
- If no PUBLISHED version exists → comment is hidden

### For Admins

- See all comments (DRAFT and PUBLISHED)
- See complete version history
- Can switch between versions

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
    tags           TEXT,        -- Comma-separated tags snapshot for this version
    pointer_url    VARCHAR(2000) -- Optional link to specific page/chart for this version
);

CREATE INDEX idx_dashboard_comment_context_dashboard
    ON dashboard_comment (context_key, dashboard_id);

-- Index for import duplicate detection
CREATE INDEX idx_comment_imported_from
    ON dashboard_comment (imported_from_id, context_key, dashboard_id);
```

> **Note:** `pointer_url` and `tags` are stored **per version** in `dashboard_comment_version`, not on the main
> `dashboard_comment` table. This design ensures each version snapshot captures the complete state (text, tags,
> pointerUrl) at that point in time. The application derives the current `pointerUrl` and `tags` from the active
> version when building DTOs.

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

