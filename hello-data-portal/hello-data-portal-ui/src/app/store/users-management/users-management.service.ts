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

import { Injectable, inject } from "@angular/core";
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs";
import {
  AdUser,
  CommentPermissions,
  ContextDashboardsForUser,
  CreateUserForm,
  CreateUserResponse,
  DashboardForUser,
  DashboardResponse,
  DashboardUsersResultDto,
  SubsystemUsersResultDto,
  User
} from "./users-management.model";
import {ContextResponse} from "./context-role.model";
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class UsersManagementService {
  protected httpClient = inject(HttpClient);

  baseUsersUrl = `${environment.portalApi}/users`;
  baseUsersSyncUrl = `${environment.portalApi}/user-sync`;
  baseMetainfoUrl = `${environment.portalApi}/metainfo`;

  getUsers(page: number, size: number, sort: string, search: string): Observable<{
    content: User[],
    totalElements: number,
    totalPages: number
  }> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort)
      .set('search', search || '');

    return this.httpClient.get<{
      content: User[],
      totalElements: number,
      totalPages: number
    }>(`${this.baseUsersUrl}`, {params});
  }

  public getSubsystemUsers(): Observable<SubsystemUsersResultDto[]> {
    return this.httpClient.get<SubsystemUsersResultDto[]>(`${this.baseMetainfoUrl}/resources/subsystem-users`);
  }

  public clearSubsystemUsersCache(): Observable<SubsystemUsersResultDto[]> {
    return this.httpClient.get<SubsystemUsersResultDto[]>(`${this.baseMetainfoUrl}/resources/subsystem-users/clear-cache`);
  }

  public getAllUsersWithRolesForDashboards(): Observable<DashboardUsersResultDto[]> {
    return this.httpClient.get<DashboardUsersResultDto[]>(`${this.baseMetainfoUrl}/resources/users-dashboards-overview`);
  }

  public clearAllUsersWithRolesForDashboardsCache(): Observable<DashboardUsersResultDto[]> {
    return this.httpClient.get<DashboardUsersResultDto[]>(`${this.baseMetainfoUrl}/resources/users-dashboards-overview/clear-cache`);
  }

  public getSyncStatus(): Observable<string> {
    return this.httpClient.get<string>(`${this.baseUsersSyncUrl}/status`);
  }

  public getUserById(userId: string): Observable<User> {
    return this.httpClient.get<User>(`${this.baseUsersUrl}/${userId}`);
  }

  public deleteUser(userRepresentation: User): Observable<void> {
    return this.httpClient.delete<void>(`${this.baseUsersUrl}/${userRepresentation.id}`);
  }

  public enableUser(userRepresentation: User): Observable<User> {
    return this.httpClient.patch<User>(`${this.baseUsersUrl}/${userRepresentation.id}/enable`, {});
  }

  public disableUser(userRepresentation: User): Observable<User> {
    return this.httpClient.patch<User>(`${this.baseUsersUrl}/${userRepresentation.id}/disable`, {});
  }

  public createUser(createUserForm: CreateUserForm): Observable<CreateUserResponse> {
    return this.httpClient.post<CreateUserResponse>(`${this.baseUsersUrl}`, createUserForm);
  }

  public syncUsers(): Observable<string> {
    return this.httpClient.get<string>(`${this.baseUsersSyncUrl}/start`);
  }

  public getCurrentAuthData(): Observable<any> {
    return this.httpClient.get<User>(`${this.baseUsersUrl}/current/profile`);
  }

  public getCurrentContextRoles(): Observable<any> {
    return this.httpClient.get<any[]>(`${this.baseUsersUrl}/current/context-roles`);
  }

  public getDashboardsWithMarkedUser(userId: string): Observable<DashboardResponse> {
    return this.httpClient.get<DashboardResponse>(`${this.baseUsersUrl}/${userId}/dashboards`);
  }

  public editDashboardRoleForUser(userId: string, data: DashboardForUser): Observable<DashboardResponse> {
    return this.httpClient.patch<DashboardResponse>(`${this.baseUsersUrl}/${userId}/dashboards`, data);
  }

  public editSelectedLanguageForUser(userId: string, lang: string): Observable<any> {
    return this.httpClient.patch<any>(`${this.baseUsersUrl}/${userId}/set-selected-lang/${lang}`, lang);
  }

  public getAvailableContexts(): Observable<ContextResponse> {
    return this.httpClient.get<ContextResponse>(`${this.baseUsersUrl}/contexts`);
  }

  public getUserContextRoles(userId: string): Observable<any> {
    return this.httpClient.get<any>(`${this.baseUsersUrl}/${userId}/context-roles`);
  }

  public updateUserRoles(data: any, contextDashboardsForUser: ContextDashboardsForUser[], commentPermissions?: Map<string, CommentPermissions>): Observable<any> {
    const selectedDashboardsForUser = new Map<string, DashboardForUser[]>();
    contextDashboardsForUser.forEach(contextDashboardForUser => {
      selectedDashboardsForUser.set(contextDashboardForUser.contextKey, contextDashboardForUser.dashboards);
    })

    const commentPermissionsList = commentPermissions
      ? Array.from(commentPermissions.entries()).map(([contextKey, perms]) => ({
        contextKey,
        readComments: perms.readComments,
        writeComments: perms.writeComments,
        reviewComments: perms.reviewComments
      }))
      : undefined;

    return this.httpClient.patch<any>(`${this.baseUsersUrl}/${data.userId}/context-roles`, {
      businessDomainRole: data.businessDomainRole,
      dataDomainRoles: data.dataDomainRoles,
      selectedDashboardsForUser: this.convertMapToJson(selectedDashboardsForUser),
      commentPermissions: commentPermissionsList
    });
  }

  public getCommentPermissions(userId: string): Observable<{contextKey: string, readComments: boolean, writeComments: boolean, reviewComments: boolean}[]> {
    return this.httpClient.get<{contextKey: string, readComments: boolean, writeComments: boolean, reviewComments: boolean}[]>(`${this.baseUsersUrl}/${userId}/comment-permissions`);
  }

  public getCurrentUserCommentPermissions(): Observable<{contextKey: string, readComments: boolean, writeComments: boolean, reviewComments: boolean}[]> {
    return this.httpClient.get<{contextKey: string, readComments: boolean, writeComments: boolean, reviewComments: boolean}[]>(`${this.baseUsersUrl}/current/comment-permissions`);
  }

  public searchUserByEmail(email: string | undefined): Observable<AdUser[]> {
    return this.httpClient.get<any>(`${this.baseUsersUrl}/search/${email}`);
  }

  public getAdminEmails(): Observable<string[]> {
    return this.httpClient.get<string[]>(`${this.baseUsersUrl}/admin-emails`);
  }

  private convertMapToJson(map: Map<any, any>): any {
    const converted: any = {};

    map.forEach((value, key) => {
      converted[key] = value;
    });

    return converted;
  }
}
