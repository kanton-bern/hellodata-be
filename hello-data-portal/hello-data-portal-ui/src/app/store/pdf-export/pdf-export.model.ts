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

/** A chart available for the builder palette. */
export interface PdfChartRef {
  id: number;
  name: string;
}

/** A selectable PDF page template. Fixed set (portrait/landscape) — mirrors the backend ReportTemplate. */
export interface PdfTemplateRef {
  id: string;
  name: string;
}

/** One positioned grid cell sent to the backend for the custom export. */
export interface PdfLayoutItem {
  type: 'chart' | 'markdown';
  chartId?: number;
  markdown?: string;
  /** Sanitized again server-side before rendering; takes precedence over `markdown`. */
  html?: string;
  name?: string;
  x: number;
  y: number;
  cols: number;
  rows: number;
}

/** POST body for the custom PDF export. instanceName + dashboardId drive routing + the authZ gate. */
export interface PdfLayoutRequest {
  instanceName: string;
  dashboardId: number;
  title: string;
  template: string;
  items: PdfLayoutItem[];
  /** Re-render every chart even if a cached screenshot exists (the "fresh data" export option). */
  force?: boolean;
}

/** The two fixed page templates — no backend round-trip needed. */
export const PDF_TEMPLATES: PdfTemplateRef[] = [
  {id: 'portrait', name: 'Portrait'},
  {id: 'landscape', name: 'Landscape'},
];

/** One cell of a saved layout, exactly as the builder keeps it: `y` is local to `page`. */
export interface PdfSavedLayoutItem {
  type: 'chart' | 'markdown';
  chartId?: number;
  name?: string;
  markdown?: string;
  html?: string;
  readonly?: boolean;
  page: number;
  x: number;
  y: number;
  cols: number;
  rows: number;
}

/** A saved layout as listed in the management table and the export page picker. Shared with
 *  everyone who may access its dashboard; only `editable` ones (own, for now) can be changed. */
export interface PdfLayoutSummary {
  id: string;
  name: string;
  contextKey: string;
  instanceName: string;
  dashboardId: number;
  dashboardTitle?: string;
  template: string;
  createdDate?: number;
  modifiedDate?: number;
  createdBy?: string;
  editable: boolean;
}

/** A complete saved layout. `removedCharts` names charts that no longer exist on the dashboard and
 *  were dropped by the backend while loading. */
export interface PdfSavedLayout extends PdfLayoutSummary {
  pageCount: number;
  gridCols: number;
  gridRows: number;
  items: PdfSavedLayoutItem[];
  removedCharts: string[];
}

/** POST/PUT body for saving a layout; data domain and dashboard title are resolved server-side. */
export interface PdfLayoutSaveRequest {
  name: string;
  instanceName: string;
  dashboardId: number;
  template: string;
  pageCount: number;
  items: PdfSavedLayoutItem[];
}
