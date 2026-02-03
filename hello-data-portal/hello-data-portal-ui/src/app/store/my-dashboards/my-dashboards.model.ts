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

export interface SupersetDashboard {
  id: number;
  dashboardTitle: string;
  dashboardUrlPath: string;
  published: boolean;
  roles: Array<SupersetRole>;
  slug?: string;
  status: string;
  thumbnailPath?: string;
  instanceName: string;
  instanceUrl: string;
  contextName: string,
  contextId: string,
  contextKey: string
}

export interface SupersetRole {
  id: number;
  name: string;
}

export interface DataDomain {
  id: string | null,
  name: string,
  key: string
}

export enum DashboardCommentStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED'
}

export interface DashboardCommentVersion {
  version: number;
  text: string;
  status: DashboardCommentStatus;
  editedDate: number;
  editedBy: string;
  editedByEmail?: string; // Email of the person who edited this version
  publishedDate?: number;
  publishedBy?: string;
  publishedByEmail?: string; // Email of the person who published this version
  deleted: boolean; // Soft delete - only non-deleted PUBLISHED versions are shown
  tags?: string[]; // Tags snapshot for this version (for history tracking)
  pointerUrl?: string; // Pointer URL snapshot for this version (for history tracking)
}

export interface DashboardCommentEntry {
  id: string;
  dashboardId: number;
  dashboardUrl: string;
  contextKey: string;
  pointerUrl?: string; // Optional - URL to specific dashboard page/tab/chart the comment refers to
  author: string;
  authorEmail: string;
  createdDate: number;
  deleted: boolean;
  deletedDate?: number;
  deletedBy?: string;
  // Versioning
  activeVersion: number; // Currently active version from history
  hasActiveDraft?: boolean; // True when this comment has an active draft edit
  history: DashboardCommentVersion[]; // All versions of this comment
  entityVersion: number; // incremented on each modification
  tags?: string[]; // Tags associated with this comment
}
