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
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs";
import {environment} from "../../../environments/environment";
import {PdfChartRef, PdfLayoutRequest} from "./pdf-export.model";

@Injectable({providedIn: 'root'})
export class PdfExportService {
  private httpClient = inject(HttpClient);

  private baseUrl = `${environment.portalApi}/superset/dashboards`;

  /** Charts of one dashboard for the builder palette. */
  public getCharts(instanceName: string, dashboardId: number): Observable<PdfChartRef[]> {
    return this.httpClient.get<PdfChartRef[]>(
      `${this.baseUrl}/${encodeURIComponent(instanceName)}/${dashboardId}/charts`
    );
  }

  /** Existing markdown/text blocks of one dashboard for the builder palette. */
  public getMarkdownBlocks(instanceName: string, dashboardId: number): Observable<string[]> {
    return this.httpClient.get<string[]>(
      `${this.baseUrl}/${encodeURIComponent(instanceName)}/${dashboardId}/markdown`
    );
  }

  /** Render the custom grid layout to a PDF (streamed as application/pdf). */
  public exportCustom(request: PdfLayoutRequest): Observable<Blob> {
    return this.httpClient.post(`${this.baseUrl}/pdf/custom`, request, {responseType: 'blob'});
  }

  /** Single-chart screenshot preview (PNG blob), sized to the tile's span, for the builder grid. */
  public getChartPreview(instanceName: string, dashboardId: number, chartId: number,
                         cols: number, rows: number, template: string): Observable<Blob> {
    const params = new HttpParams().set('cols', cols).set('rows', rows).set('template', template);
    return this.httpClient.get(
      `${this.baseUrl}/${encodeURIComponent(instanceName)}/${dashboardId}/charts/${chartId}/preview`,
      {params, responseType: 'blob'}
    );
  }
}
