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

import {inject, Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {CommentEntry, DataDomain} from "./my-dashboards.model";
import {environment} from "../../../environments/environment";
import {SupersetDashboardWithMetadata} from "../start-page/start-page.model";

export interface CommentCreateRequest {
  dashboardUrl: string;
  pointerUrl?: string;
  text: string;
}

export interface CommentUpdateRequest {
  text: string;
  pointerUrl?: string;
  entityVersion: number;
}

@Injectable({
  providedIn: 'root'
})
export class MyDashboardsService {
  protected httpClient = inject(HttpClient);


  baseUrl = `${environment.portalApi}/superset`;
  commentsBaseUrl = `${environment.portalApi}/dashboards`;

  public getMyDashboards(): Observable<SupersetDashboardWithMetadata[]> {
    return this.httpClient.get<SupersetDashboardWithMetadata[]>(`${this.baseUrl}/my-dashboards`);
  }

  public getAvailableDataDomains(): Observable<DataDomain[]> {
    return this.httpClient.get<DataDomain[]>(`${environment.portalApi}/users/data-domains`);
  }

  // Comments API methods
  public getDashboardComments(contextKey: string, dashboardId: number): Observable<CommentEntry[]> {
    return this.httpClient.get<CommentEntry[]>(`${this.commentsBaseUrl}/${contextKey}/${dashboardId}/comments`);
  }

  public createComment(contextKey: string, dashboardId: number, request: CommentCreateRequest): Observable<CommentEntry> {
    return this.httpClient.post<CommentEntry>(`${this.commentsBaseUrl}/${contextKey}/${dashboardId}/comments`, request);
  }

  public updateComment(contextKey: string, dashboardId: number, commentId: string, request: CommentUpdateRequest): Observable<CommentEntry> {
    return this.httpClient.put<CommentEntry>(`${this.commentsBaseUrl}/${contextKey}/${dashboardId}/comments/${commentId}`, request);
  }

  public deleteComment(contextKey: string, dashboardId: number, commentId: string): Observable<CommentEntry> {
    return this.httpClient.delete<CommentEntry>(`${this.commentsBaseUrl}/${contextKey}/${dashboardId}/comments/${commentId}`);
  }

  public publishComment(contextKey: string, dashboardId: number, commentId: string): Observable<CommentEntry> {
    return this.httpClient.post<CommentEntry>(`${this.commentsBaseUrl}/${contextKey}/${dashboardId}/comments/${commentId}/publish`, {});
  }

  public unpublishComment(contextKey: string, dashboardId: number, commentId: string): Observable<CommentEntry> {
    return this.httpClient.post<CommentEntry>(`${this.commentsBaseUrl}/${contextKey}/${dashboardId}/comments/${commentId}/unpublish`, {});
  }

  public cloneCommentForEdit(contextKey: string, dashboardId: number, commentId: string, request: CommentUpdateRequest): Observable<CommentEntry> {
    return this.httpClient.post<CommentEntry>(`${this.commentsBaseUrl}/${contextKey}/${dashboardId}/comments/${commentId}/clone`, request);
  }

  public restoreVersion(contextKey: string, dashboardId: number, commentId: string, versionNumber: number): Observable<CommentEntry> {
    return this.httpClient.post<CommentEntry>(`${this.commentsBaseUrl}/${contextKey}/${dashboardId}/comments/${commentId}/restore/${versionNumber}`, {});
  }
}
