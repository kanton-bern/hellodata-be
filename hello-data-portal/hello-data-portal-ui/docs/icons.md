# HelloData Portal — Icon System

## Overview

The portal uses **Font Awesome 6** as its single icon library. Icons are managed through
a centralized registry at `src/app/shared/icons/icon-registry.ts`.

Workspace/tool logos (Superset, Airflow, dbt, CloudBeaver, JupyterHub, SftpGO) are branded
image assets in `src/assets/workspaces/logos/` and are **not** part of the icon system.

---

## Style Conventions

| Weight | Class Prefix | Usage |
|--------|-------------|-------|
| Regular | `fa-solid` | Navigation & sidebar menu icons |
| Solid | `fa-solid` | Action buttons, emphasis, status indicators |
| Regular | `fa-solid` | Secondary/informational icons |

All icons inherit color via CSS `currentColor` — never hardcode icon colors in class strings.

---

## Usage Policy

### Default: Icon + Label

All interactive elements (buttons, menu items, links) should display **both** an icon and a
text label.

### Exceptions (icon-only allowed)

Only universally recognized icons may appear without a label:

| Icon | Meaning |
|------|---------|
| ✕ (`fa-xmark`) | Close |
| 🔍 (`fa-magnifying-glass`) | Search |
| ☰ (`fa-bars`) | Menu/hamburger |

### Tooltip Requirement

Any icon-only element **must** have a tooltip (`pTooltip` directive) providing the accessible
label text.

---

## Using the Icon Registry

```typescript
import { ACTION_SAVE, NAV_DASHBOARDS } from '../../shared/icons';

// In a menu item:
{ label: 'Save', icon: ACTION_SAVE.class }

// In a template:
// <i [class]="actionSave.class" [pTooltip]="actionSave.label | transloco"></i>
```

### Registry Structure

```typescript
export interface IconDefinition {
  class: string;       // Full CSS class string (e.g., 'fa-solid fa-floppy-disk')
  label: string;       // Translation key for accessible label/tooltip
  category: string;    // 'navigation' | 'action' | 'status' | 'content' | 'dialog'
}
```

---

## Icon Reference

### Navigation Icons

| Key | Class | Purpose |
|-----|-------|---------|
| `NAV_DASHBOARDS` | `fa-solid fa-chart-line` | Dashboards menu |
| `NAV_LINEAGE` | `fa-solid fa-diagram-project` | Lineage menu |
| `NAV_DATA_MARTS` | `fa-solid fa-store` | Data Marts menu |
| `NAV_DATA_ENG` | `fa-solid fa-dice-d6` | Data Engineering menu |
| `NAV_ADMINISTRATION` | `fa-solid fa-gear` | Administration menu |
| `NAV_MONITORING` | `fa-solid fa-list-check` | Monitoring menu |
| `NAV_DEVTOOLS` | `fa-solid fa-screwdriver-wrench` | DevTools menu |
| `NAV_HOME` | `fa-solid fa-house` | Home breadcrumb |
| `NAV_USER_PROFILE` | `fa-solid fa-user` | User profile menu item |
| `NAV_ANNOUNCEMENTS` | `fa-solid fa-bell` | Announcements menu item |
| `NAV_INFO` | `fa-solid fa-circle-info` | Info/summary link |
| `NAV_LOGOUT` | `fa-solid fa-power-off` | Logout action |
| `NAV_EXPAND` | `fa-solid fa-up-right-and-down-left-from-center` | Expand sidebar |
| `NAV_COLLAPSE` | `fa-solid fa-down-left-and-up-right-to-center` | Collapse sidebar |
| `NAV_MENU` | `fa-solid fa-bars` | Mobile menu toggle |
| `NAV_CHEVRON_DOWN` | `fa-solid fa-chevron-down` | Dropdown indicator |
| `NAV_CHEVRON_RIGHT` | `fa-solid fa-chevron-right` | Expand indicator |
| `NAV_CHEVRON_LEFT` | `fa-solid fa-chevron-left` | Back indicator |

### Action Icons

| Key | Class | Purpose |
|-----|-------|---------|
| `ACTION_SAVE` | `fa-solid fa-floppy-disk` | Save |
| `ACTION_DELETE` | `fa-solid fa-trash` | Delete |
| `ACTION_EDIT` | `fa-solid fa-pen-to-square` | Edit |
| `ACTION_ADD` | `fa-solid fa-plus` | Add item |
| `ACTION_CREATE` | `fa-solid fa-circle-plus` | Create new |
| `ACTION_CLOSE` | `fa-solid fa-xmark` | Close |
| `ACTION_CANCEL` | `fa-solid fa-circle-xmark` | Cancel |
| `ACTION_CONFIRM` | `fa-solid fa-check` | Confirm |
| `ACTION_APPROVE` | `fa-solid fa-circle-check` | Approve |
| `ACTION_SEND` | `fa-solid fa-paper-plane` | Send message |
| `ACTION_REFRESH` | `fa-solid fa-arrows-rotate` | Refresh data |
| `ACTION_REDO` | `fa-solid fa-redo` | Redo operation |
| `ACTION_UNDO` | `fa-solid fa-rotate-left` | Undo operation |
| `ACTION_DOWNLOAD` | `fa-solid fa-cloud-arrow-down` | Download |
| `ACTION_UPLOAD` | `fa-solid fa-cloud-arrow-up` | Upload |
| `ACTION_IMPORT` | `fa-solid fa-download` | Import data |
| `ACTION_EXPORT` | `fa-solid fa-upload` | Export data |
| `ACTION_FILE_EXPORT` | `fa-solid fa-file-export` | Export to file |
| `ACTION_SEARCH` | `fa-solid fa-magnifying-glass` | Search |
| `ACTION_FILTER_CLEAR` | `fa-solid fa-filter-circle-xmark` | Clear filters |
| `ACTION_BACK` | `fa-solid fa-arrow-left` | Navigate back |
| `ACTION_FORWARD` | `fa-solid fa-arrow-right` | Navigate forward |
| `ACTION_EXTERNAL_LINK` | `fa-solid fa-up-right-from-square` | Open external |
| `ACTION_PIN` | `fa-solid fa-thumbtack` | Pin item |
| `ACTION_BAN` | `fa-solid fa-ban` | Disable/block |
| `ACTION_LINK` | `fa-solid fa-link` | Copy/show link |

### Status Icons

| Key | Class | Purpose |
|-----|-------|---------|
| `STATUS_SUCCESS` | `fa-solid fa-check` | Success state |
| `STATUS_ERROR` | `fa-solid fa-times` | Error state |
| `STATUS_WARNING` | `fa-solid fa-triangle-exclamation` | Warning state |
| `STATUS_INFO` | `fa-solid fa-circle-info` | Informational |
| `STATUS_PUBLISHED` | `fa-solid fa-circle` | Published/active |
| `STATUS_STOPPED` | `fa-solid fa-circle-stop` | Stopped/inactive |

### Content Icons

| Key | Class | Purpose |
|-----|-------|---------|
| `CONTENT_DASHBOARD` | `fa-solid fa-chart-line` | Dashboard reference |
| `CONTENT_CHART` | `fa-solid fa-chart-pie` | Chart/analytics |
| `CONTENT_DATABASE` | `fa-solid fa-database` | Database/data source |
| `CONTENT_LINEAGE` | `fa-solid fa-diagram-project` | Lineage reference |
| `CONTENT_DATAMART` | `fa-solid fa-store` | Data mart |
| `CONTENT_TAG` | `fa-solid fa-tag` | Single tag |
| `CONTENT_TAGS` | `fa-solid fa-tags` | Multiple tags |
| `CONTENT_USER` | `fa-solid fa-user` | User reference |
| `CONTENT_USERS` | `fa-solid fa-users` | Users/group |
| `CONTENT_BUSINESS_DOMAIN` | `fa-solid fa-business-time` | Business domain |
| `CONTENT_DATA_DOMAIN` | `fa-solid fa-database` | Data domain |
| `CONTENT_GLOBE` | `fa-solid fa-globe` | Global/public |
| `CONTENT_COMMENTS` | `fa-solid fa-comment-dots` | Comments |

### Dialog Icons

| Key | Class | Purpose |
|-----|-------|---------|
| `DIALOG_WARNING` | `fa-solid fa-triangle-exclamation` | Warning dialog |
| `DIALOG_INFO` | `fa-solid fa-circle-info` | Info dialog |
| `DIALOG_CLOSE` | `fa-solid fa-times` | Close dialog |

---

## Adding New Icons

1. Choose the appropriate category and weight (see Style Conventions above)
2. Add the `IconDefinition` to `src/app/shared/icons/icon-registry.ts`
3. Export it from the `ICON_REGISTRY` object
4. Update this documentation
5. Import and use via the registry — never use inline icon class strings

---

## Superset Alignment

Superset uses monochrome SVG icons with `currentColor` inheritance and standardized sizes.
HelloData aligns with this philosophy by:

- Using FA's weight system for visual hierarchy (light/regular/solid)
- Inheriting color via `currentColor` in CSS
- Maintaining consistent sizing through FA's built-in scale
- Using the same semantic categories (navigation, action, status)

The key difference is that Superset wraps SVGs in React components while HelloData uses
Font Awesome's CSS class system via Angular templates. The visual result is equivalent:
monochrome, theme-colored, weight-differentiated icons.
