///
/// Copyright © 2024, Kanton Bern
/// All rights reserved.
///
/// Redistribution and use in source and binary forms, with or without
/// modification, are permitted provided that the following conditions are met:
///     * Redistributions of source code must retain the above copyright
///       notice, this list of conditions and the following disclaimer.
///     * Redistributions in binary form must reproduce the above copyright
///       notice, this list of conditions and the following disclaimer in the
///       documentation and/or other materials provided with the distribution.
///     * Neither the name of the <organization> nor the
///       names of its contributors may be used to endorse or promote products
///       derived from this software without specific prior written permission.
///
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
/// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
/// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
/// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
/// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
/// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
/// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
/// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
/// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
/// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
///

/**
 * Icon Registry for HelloData Portal
 *
 * Style conventions (Font Awesome Free):
 * - fa-solid: All icons (FA Free only includes solid weight for most icons)
 *
 * Visual hierarchy is achieved through icon size and color, not weight,
 * since FA Free does not include fa-light or fa-regular for most icons.
 * Usage policy:
 * - Default: icon + label for all interactive elements
 * - Icon-only exceptions: close (✕), search (🔍), hamburger (☰)
 * - Tooltip required for any icon-only usage
 *
 * Colors inherit via CSS `currentColor` — do not hardcode icon colors.
 */

export interface IconDefinition {
  /** Full CSS class string for rendering the icon */
  class: string;
  /** Accessible label / tooltip text (translation key) */
  label: string;
  /** Category for documentation grouping */
  category: 'navigation' | 'action' | 'status' | 'dialog' | 'content';
}

// ---------------------------------------------------------------------------
// Navigation & Menu Icons
// ---------------------------------------------------------------------------

export const NAV_DASHBOARDS: IconDefinition = {
  class: 'fa-solid fa-chart-line',
  label: '@Dashboards',
  category: 'navigation',
};

export const NAV_LINEAGE: IconDefinition = {
  class: 'fa-solid fa-diagram-project',
  label: '@Lineage',
  category: 'navigation',
};

export const NAV_DATA_MARTS: IconDefinition = {
  class: 'fa-solid fa-store',
  label: '@Data Marts',
  category: 'navigation',
};

export const NAV_DATA_ENG: IconDefinition = {
  class: 'fa-solid fa-dice-d6',
  label: '@Data Eng',
  category: 'navigation',
};

export const NAV_ADMINISTRATION: IconDefinition = {
  class: 'fa-solid fa-gear',
  label: '@Administration',
  category: 'navigation',
};

export const NAV_MONITORING: IconDefinition = {
  class: 'fa-solid fa-list-check',
  label: '@Monitoring',
  category: 'navigation',
};

export const NAV_DEVTOOLS: IconDefinition = {
  class: 'fa-solid fa-screwdriver-wrench',
  label: '@DevTools',
  category: 'navigation',
};

export const NAV_HOME: IconDefinition = {
  class: 'fa-solid fa-house',
  label: '@Home',
  category: 'navigation',
};

export const NAV_USER_PROFILE: IconDefinition = {
  class: 'fa-solid fa-user',
  label: '@Profile',
  category: 'navigation',
};

export const NAV_ANNOUNCEMENTS: IconDefinition = {
  class: 'fa-solid fa-bell',
  label: '@View announcements',
  category: 'navigation',
};

export const NAV_INFO: IconDefinition = {
  class: 'fa-solid fa-circle-info',
  label: '@Info',
  category: 'navigation',
};

export const NAV_LOGOUT: IconDefinition = {
  class: 'fa-solid fa-power-off',
  label: '@Logout',
  category: 'navigation',
};

export const NAV_EXPAND: IconDefinition = {
  class: 'fa-solid fa-up-right-and-down-left-from-center',
  label: '@Expand navigation',
  category: 'navigation',
};

export const NAV_COLLAPSE: IconDefinition = {
  class: 'fa-solid fa-down-left-and-up-right-to-center',
  label: '@Collapse navigation',
  category: 'navigation',
};

export const NAV_MENU: IconDefinition = {
  class: 'fa-solid fa-bars',
  label: '@Menu',
  category: 'navigation',
};

export const NAV_CHEVRON_DOWN: IconDefinition = {
  class: 'fa-solid fa-chevron-down',
  label: '@Expand',
  category: 'navigation',
};

export const NAV_CHEVRON_RIGHT: IconDefinition = {
  class: 'fa-solid fa-chevron-right',
  label: '@Expand',
  category: 'navigation',
};

export const NAV_CHEVRON_LEFT: IconDefinition = {
  class: 'fa-solid fa-chevron-left',
  label: '@Back',
  category: 'navigation',
};

// ---------------------------------------------------------------------------
// Action Icons
// ---------------------------------------------------------------------------

export const ACTION_SAVE: IconDefinition = {
  class: 'fa-solid fa-floppy-disk',
  label: '@Save',
  category: 'action',
};

export const ACTION_DELETE: IconDefinition = {
  class: 'fa-solid fa-trash',
  label: '@Delete',
  category: 'action',
};

export const ACTION_EDIT: IconDefinition = {
  class: 'fa-solid fa-pen-to-square',
  label: '@Edit',
  category: 'action',
};

export const ACTION_ADD: IconDefinition = {
  class: 'fa-solid fa-plus',
  label: '@Add',
  category: 'action',
};

export const ACTION_CREATE: IconDefinition = {
  class: 'fa-solid fa-circle-plus',
  label: '@Create',
  category: 'action',
};

export const ACTION_CLOSE: IconDefinition = {
  class: 'fa-solid fa-xmark',
  label: '@Close',
  category: 'action',
};

export const ACTION_CANCEL: IconDefinition = {
  class: 'fa-solid fa-circle-xmark',
  label: '@Cancel',
  category: 'action',
};

export const ACTION_CONFIRM: IconDefinition = {
  class: 'fa-solid fa-check',
  label: '@Confirm',
  category: 'action',
};

export const ACTION_APPROVE: IconDefinition = {
  class: 'fa-solid fa-circle-check',
  label: '@Approve',
  category: 'action',
};

export const ACTION_SEND: IconDefinition = {
  class: 'fa-solid fa-paper-plane',
  label: '@Send',
  category: 'action',
};

export const ACTION_REFRESH: IconDefinition = {
  class: 'fa-solid fa-arrows-rotate',
  label: '@Refresh',
  category: 'action',
};

export const ACTION_REDO: IconDefinition = {
  class: 'fa-solid fa-redo',
  label: '@Redo',
  category: 'action',
};

export const ACTION_UNDO: IconDefinition = {
  class: 'fa-solid fa-rotate-left',
  label: '@Undo',
  category: 'action',
};

export const ACTION_DOWNLOAD: IconDefinition = {
  class: 'fa-solid fa-cloud-arrow-down',
  label: '@Download',
  category: 'action',
};

export const ACTION_UPLOAD: IconDefinition = {
  class: 'fa-solid fa-cloud-arrow-up',
  label: '@Upload',
  category: 'action',
};

export const ACTION_IMPORT: IconDefinition = {
  class: 'fa-solid fa-download',
  label: '@Import',
  category: 'action',
};

export const ACTION_EXPORT: IconDefinition = {
  class: 'fa-solid fa-upload',
  label: '@Export',
  category: 'action',
};

export const ACTION_FILE_EXPORT: IconDefinition = {
  class: 'fa-solid fa-file-export',
  label: '@Export',
  category: 'action',
};

export const ACTION_SEARCH: IconDefinition = {
  class: 'fa-solid fa-magnifying-glass',
  label: '@Search',
  category: 'action',
};

export const ACTION_FILTER_CLEAR: IconDefinition = {
  class: 'fa-solid fa-filter-circle-xmark',
  label: '@Clear filters',
  category: 'action',
};

export const ACTION_BACK: IconDefinition = {
  class: 'fa-solid fa-arrow-left',
  label: '@Back',
  category: 'action',
};

export const ACTION_FORWARD: IconDefinition = {
  class: 'fa-solid fa-arrow-right',
  label: '@Forward',
  category: 'action',
};

export const ACTION_EXTERNAL_LINK: IconDefinition = {
  class: 'fa-solid fa-up-right-from-square',
  label: '@Open in new tab',
  category: 'action',
};

export const ACTION_PIN: IconDefinition = {
  class: 'fa-solid fa-thumbtack',
  label: '@Pin',
  category: 'action',
};

export const ACTION_BAN: IconDefinition = {
  class: 'fa-solid fa-ban',
  label: '@Disable',
  category: 'action',
};

export const ACTION_LINK: IconDefinition = {
  class: 'fa-solid fa-link',
  label: '@Link',
  category: 'action',
};

// ---------------------------------------------------------------------------
// Status & Info Icons
// ---------------------------------------------------------------------------

export const STATUS_SUCCESS: IconDefinition = {
  class: 'fa-solid fa-check',
  label: '@Success',
  category: 'status',
};

export const STATUS_ERROR: IconDefinition = {
  class: 'fa-solid fa-times',
  label: '@Error',
  category: 'status',
};

export const STATUS_WARNING: IconDefinition = {
  class: 'fa-solid fa-triangle-exclamation',
  label: '@Warning',
  category: 'status',
};

export const STATUS_INFO: IconDefinition = {
  class: 'fa-solid fa-circle-info',
  label: '@Information',
  category: 'status',
};

export const STATUS_PUBLISHED: IconDefinition = {
  class: 'fa-solid fa-circle',
  label: '@Published',
  category: 'status',
};

export const STATUS_STOPPED: IconDefinition = {
  class: 'fa-solid fa-circle-stop',
  label: '@Stopped',
  category: 'status',
};

// ---------------------------------------------------------------------------
// Content / Contextual Icons
// ---------------------------------------------------------------------------

export const CONTENT_DASHBOARD: IconDefinition = {
  class: 'fa-solid fa-chart-line',
  label: '@Dashboard',
  category: 'content',
};

export const CONTENT_CHART: IconDefinition = {
  class: 'fa-solid fa-chart-pie',
  label: '@Chart',
  category: 'content',
};

export const CONTENT_DATABASE: IconDefinition = {
  class: 'fa-solid fa-database',
  label: '@Database',
  category: 'content',
};

export const CONTENT_LINEAGE: IconDefinition = {
  class: 'fa-solid fa-diagram-project',
  label: '@Lineage',
  category: 'content',
};

export const CONTENT_DATAMART: IconDefinition = {
  class: 'fa-solid fa-store',
  label: '@Data Mart',
  category: 'content',
};

export const CONTENT_TAG: IconDefinition = {
  class: 'fa-solid fa-tag',
  label: '@Tag',
  category: 'content',
};

export const CONTENT_TAGS: IconDefinition = {
  class: 'fa-solid fa-tags',
  label: '@Tags',
  category: 'content',
};

export const CONTENT_USER: IconDefinition = {
  class: 'fa-solid fa-user',
  label: '@User',
  category: 'content',
};

export const CONTENT_USERS: IconDefinition = {
  class: 'fa-solid fa-users',
  label: '@Users',
  category: 'content',
};

export const CONTENT_BUSINESS_DOMAIN: IconDefinition = {
  class: 'fa-solid fa-business-time',
  label: '@Business domain',
  category: 'content',
};

export const CONTENT_DATA_DOMAIN: IconDefinition = {
  class: 'fa-solid fa-database',
  label: '@Data domain',
  category: 'content',
};

export const CONTENT_GLOBE: IconDefinition = {
  class: 'fa-solid fa-globe',
  label: '@Global',
  category: 'content',
};

export const CONTENT_COMMENTS: IconDefinition = {
  class: 'fa-solid fa-comment-dots',
  label: '@Comments',
  category: 'content',
};

// ---------------------------------------------------------------------------
// Dialog Icons
// ---------------------------------------------------------------------------

export const DIALOG_WARNING: IconDefinition = {
  class: 'fa-solid fa-triangle-exclamation',
  label: '@Warning',
  category: 'dialog',
};

export const DIALOG_INFO: IconDefinition = {
  class: 'fa-solid fa-circle-info',
  label: '@Information',
  category: 'dialog',
};

export const DIALOG_CLOSE: IconDefinition = {
  class: 'fa-solid fa-times',
  label: '@Close',
  category: 'dialog',
};

// ---------------------------------------------------------------------------
// Utility: All icons as a lookup map
// ---------------------------------------------------------------------------

export const ICON_REGISTRY = {
  // Navigation
  NAV_DASHBOARDS,
  NAV_LINEAGE,
  NAV_DATA_MARTS,
  NAV_DATA_ENG,
  NAV_ADMINISTRATION,
  NAV_MONITORING,
  NAV_DEVTOOLS,
  NAV_HOME,
  NAV_USER_PROFILE,
  NAV_ANNOUNCEMENTS,
  NAV_INFO,
  NAV_LOGOUT,
  NAV_EXPAND,
  NAV_COLLAPSE,
  NAV_MENU,
  NAV_CHEVRON_DOWN,
  NAV_CHEVRON_RIGHT,
  NAV_CHEVRON_LEFT,
  // Actions
  ACTION_SAVE,
  ACTION_DELETE,
  ACTION_EDIT,
  ACTION_ADD,
  ACTION_CREATE,
  ACTION_CLOSE,
  ACTION_CANCEL,
  ACTION_CONFIRM,
  ACTION_APPROVE,
  ACTION_SEND,
  ACTION_REFRESH,
  ACTION_REDO,
  ACTION_UNDO,
  ACTION_DOWNLOAD,
  ACTION_UPLOAD,
  ACTION_IMPORT,
  ACTION_EXPORT,
  ACTION_FILE_EXPORT,
  ACTION_SEARCH,
  ACTION_FILTER_CLEAR,
  ACTION_BACK,
  ACTION_FORWARD,
  ACTION_EXTERNAL_LINK,
  ACTION_PIN,
  ACTION_BAN,
  ACTION_LINK,
  // Status
  STATUS_SUCCESS,
  STATUS_ERROR,
  STATUS_WARNING,
  STATUS_INFO,
  STATUS_PUBLISHED,
  STATUS_STOPPED,
  // Content
  CONTENT_DASHBOARD,
  CONTENT_CHART,
  CONTENT_DATABASE,
  CONTENT_LINEAGE,
  CONTENT_DATAMART,
  CONTENT_TAG,
  CONTENT_TAGS,
  CONTENT_USER,
  CONTENT_USERS,
  CONTENT_BUSINESS_DOMAIN,
  CONTENT_DATA_DOMAIN,
  CONTENT_GLOBE,
  CONTENT_COMMENTS,
  // Dialog
  DIALOG_WARNING,
  DIALOG_INFO,
  DIALOG_CLOSE,
} as const;
