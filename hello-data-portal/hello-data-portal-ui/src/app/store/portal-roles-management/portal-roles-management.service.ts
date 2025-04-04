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

import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {PortalRole, PortalRoleCreate, PortalRoleUpdate} from "./portal-roles-management.model";
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class PortalRolesManagementService {

  baseUrl = `${environment.portalApi}/portal-roles`;

  constructor(protected httpClient: HttpClient) {
  }

  public getPortalRoles(): Observable<PortalRole[]> {
    return this.httpClient.get<PortalRole[]>(`${this.baseUrl}`);
  }

  public getPortalRoleById(id: string): Observable<PortalRole> {
    return this.httpClient.get<PortalRole>(`${this.baseUrl}/${id}`);
  }

  public deletePortalRoleById(id: string): Observable<any> {
    return this.httpClient.delete<void>(`${this.baseUrl}/${id}`);
  }

  public createPortalRole(createRole: PortalRoleCreate): Observable<any> {
    return this.httpClient.post<any>(`${this.baseUrl}`, createRole);
  }

  public updatePortalRole(updateRole: PortalRoleUpdate): Observable<any> {
    return this.httpClient.patch<any>(`${this.baseUrl}`, updateRole);
  }
}


