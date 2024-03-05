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
import {HttpClient, HttpEvent, HttpEventType} from "@angular/common/http";
import {DataDomain} from "./my-dashboards.model";
import {environment} from "../../../environments/environment";
import {SupersetDashboardWithMetadata} from "../start-page/start-page.model";

@Injectable({
  providedIn: 'root'
})
export class MyDashboardsService {

  baseUrl = `${environment.portalApi}/superset`;

  constructor(protected httpClient: HttpClient) {
  }

  public getMyDashboards(): Observable<SupersetDashboardWithMetadata[]> {
    return this.httpClient.get<SupersetDashboardWithMetadata[]>(`${this.baseUrl}/my-dashboards`);
  }

  public getAvailableDataDomains(): Observable<DataDomain[]> {
    return this.httpClient.get<DataDomain[]>(`${environment.portalApi}/users/data-domains`);
  }

  public uploadDashboardsFile(formData: FormData): Observable<any> {
    return this.httpClient.post<any>(`${this.baseUrl}/upload-dashboards`, formData, {
      reportProgress: true, // Enable progress tracking
      observe: 'events' // Return HttpEvent<any> instead of just the response
    });
  }

  private handleUploadEvent(event: HttpEvent<any>): any {
    switch (event.type) {
      case HttpEventType.Sent:
        // The request was sent, start tracking progress here if needed
        break;
      case HttpEventType.UploadProgress:
        const eventTotal = event.total ? event.total : 1;
        const percentDone = Math.round((100 * event.loaded) / eventTotal);
        // Here you can emit or log the progress percentage
        console.log(`Progress: ${percentDone}%`);
        break;
      case HttpEventType.Response:
        // Upload completed, you can handle response here if needed
        break;
    }
  }
}
