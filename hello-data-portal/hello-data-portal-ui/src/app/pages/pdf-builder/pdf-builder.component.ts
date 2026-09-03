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

import {Component, DestroyRef, OnDestroy, OnInit, computed, inject, signal} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {FormsModule} from "@angular/forms";
import {Select} from "primeng/select";
import {Button} from "primeng/button";
import {Ripple} from "primeng/ripple";
import {TranslocoPipe} from "@jsverse/transloco";
import {Store} from "@ngrx/store";
import {DisplayGrid, Gridster, GridsterConfig, GridsterItem, GridsterItemConfig, GridType} from "angular-gridster2";
import {ICON_REGISTRY} from "../../shared/icons";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {loadMyDashboards} from "../../store/my-dashboards/my-dashboards.action";
import {selectMyDashboards, selectSelectedDataDomain} from "../../store/my-dashboards/my-dashboards.selector";
import {SupersetDashboardWithMetadata} from "../../store/start-page/start-page.model";
import {NotificationService} from "../../shared/services/notification.service";
import {PdfExportService} from "../../store/pdf-export/pdf-export.service";
import {PDF_TEMPLATES, PdfChartRef, PdfLayoutItem, PdfLayoutRequest, PdfTemplateRef} from "../../store/pdf-export/pdf-export.model";

/** What a cell renders. Markdown from the dashboard is read-only; markdown created
 *  via "+ Add markdown" is editable. */
type PaletteItem = {
  type: 'chart' | 'markdown';
  chartId?: number;
  name?: string;
  markdown?: string;
  readonly?: boolean;
};

/** A gridster cell that also carries what it renders and which page it lives on. `x`/`y` are LOCAL
 *  to that page's grid (y in 0..PAGE_ROWS-1); only the current page's cells are shown at a time. */
type Cell = GridsterItemConfig & PaletteItem & {page: number};

const STORAGE_KEY = 'pdf-builder-layout';

/** Each PDF page is a PAGE_ROWS x PAGE_ROWS grid. The export maps a cell's page + local y to the
 *  global row (page * PAGE_ROWS + y) so the backend page-breaks correctly. */
const PAGE_ROWS = 4;

@Component({
  selector: 'app-pdf-builder',
  standalone: true,
  imports: [FormsModule, Gridster, GridsterItem, Select, Button, Ripple, TranslocoPipe],
  templateUrl: './pdf-builder.component.html',
  styleUrl: './pdf-builder.component.scss',
})
export class PdfBuilderComponent implements OnInit, OnDestroy {
  protected readonly icons = ICON_REGISTRY;

  private pdfExport = inject(PdfExportService);
  private store = inject(Store);
  private destroyRef = inject(DestroyRef);
  private notification = inject(NotificationService);

  /** A dashboard to re-select from localStorage once it appears in the (data-domain-filtered) list. */
  private pendingRestore: {instanceName: string; dashboardId: number} | null = null;

  dashboards = signal<SupersetDashboardWithMetadata[]>([]);
  /** True when "All Data Domains" is selected; then the picker disambiguates titles with the domain. */
  allDomainsSelected = signal(true);
  templates = signal<PdfTemplateRef[]>(PDF_TEMPLATES);
  selectedTemplate = signal<string>('portrait');
  charts = signal<PdfChartRef[]>([]);
  markdownBlocks = signal<string[]>([]);
  selectedDashboard = signal<SupersetDashboardWithMetadata | null>(null);
  cells = signal<Cell[]>([]);
  /** Total number of PDF pages (>= 1); the paginator adds/removes these. Persisted with the layout. */
  pageCount = signal(1);
  /** 0-indexed page currently shown on the 4x4 canvas. */
  currentPage = signal(0);

  /** Per-tile chart screenshot previews, keyed by chart + size + template (see previewKey). Each is
   *  an object URL for the loaded PNG blob; fetched lazily/async so tiles fill in while you keep
   *  building. `previewLoading` drives the per-tile spinner. */
  private previewUrls = signal<Map<string, string>>(new Map());
  private previewLoading = signal<Set<string>>(new Set());

  /** Picker options: label is the dashboard title, suffixed with the data-domain name only when all
   *  domains are shown (so dashboards from different domains stay distinguishable). */
  dashboardOptions = computed(() => this.dashboards().map(d => ({
    label: this.allDomainsSelected() && d.contextName ? `${d.dashboardTitle} (${d.contextName})` : d.dashboardTitle,
    value: d,
  })));

  /** Cells on the currently shown page (what the 4x4 grid renders). */
  visibleCells = computed(() => this.cells().filter(c => c.page === this.currentPage()));

  /** 0-indexed page numbers, for the paginator. */
  pages = computed(() => Array.from({length: this.pageCount()}, (_, i) => i));

  /** A page can be removed only when it is empty and it is not the last remaining page. */
  canRemovePage = computed(() => this.pageCount() > 1 && this.visibleCells().length === 0);

  exporting = signal(false);
  editorOpen = signal(false);
  editingText = signal('');
  /** The editable cell being edited, or null when creating a new block. */
  private editingCell: Cell | null = null;

  /** The palette entry currently being dragged (set on dragstart). */
  private dragPayload: PaletteItem | null = null;

  options: GridsterConfig = {
    // Fit: exactly PAGE_ROWS x PAGE_ROWS cells that fill the (bounded, see SCSS) page area — one page
    // on screen at a time, no scrolling. VerticalFixed instead fills the container with fixed-height
    // rows (many more than 4), which forced the user to scroll to find the paginator.
    gridType: GridType.Fit,
    displayGrid: DisplayGrid.Always,
    minCols: PAGE_ROWS,
    maxCols: PAGE_ROWS,
    minRows: PAGE_ROWS,
    maxRows: PAGE_ROWS,
    margin: 8,
    // Never rearrange the user's settled layout: moving/resizing a chart must not push others aside.
    pushItems: false,
    swap: false,
    pushResizeItems: false,
    // Gate the drop on a 1x1 footprint: gridster suppresses the drop callback when the placeholder
    // collides (getValidItemFromEvent), so a 2x2 default would swallow drops onto any gap smaller
    // than 2x2 (e.g. the 2x1 slot left by shrinking a chart). With 1x1 the callback fires whenever
    // the released cell itself is free; onDrop then grows the tile to the largest size that fits
    // there (up to 2x2). See fitFootprint().
    defaultItemCols: 1,
    defaultItemRows: 1,
    // A drop that doesn't fit where released should do nothing, rather than jumping to another slot.
    disableAutoPositionOnConflict: true,
    draggable: {enabled: true, ignoreContentClass: 'cell-body'},
    resizable: {enabled: true},
    enableEmptyCellDrop: true,
    emptyCellDropCallback: (event: DragEvent, item: GridsterItemConfig) => this.onDrop(item),
  };

  ngOnInit(): void {
    this.store.dispatch(createBreadcrumbs({breadcrumbs: [{label: '@PDF export'}]}));
    this.restore();
    this.store.dispatch(loadMyDashboards());
    this.store.select(selectSelectedDataDomain).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(dd => this.allDomainsSelected.set(dd === null || dd.id === ''));
    // Reactively track the dashboards of the currently selected data domain (selectMyDashboards
    // returns all domains when "All Data Domains" is selected, otherwise only the chosen one).
    this.store.select(selectMyDashboards).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(dashboards => {
      this.dashboards.set(dashboards);
      if (this.pendingRestore) {
        const match = dashboards.find(d => d.instanceName === this.pendingRestore!.instanceName && d.id === this.pendingRestore!.dashboardId);
        if (match) {
          this.pendingRestore = null;
          this.onDashboardChange(match);
        }
        return;
      }
      // If the data domain changed and the selected dashboard is no longer listed, reset the picker
      // and clear the canvas — its cells reference charts of the now-unavailable dashboard.
      const current = this.selectedDashboard();
      if (current && !dashboards.some(d => d.instanceName === current.instanceName && d.id === current.id)) {
        this.selectedDashboard.set(null);
        this.charts.set([]);
        this.markdownBlocks.set([]);
        this.resetPages();
        this.persist();
      }
    });
  }

  onTemplateChange(id: string): void {
    this.selectedTemplate.set(id);
    // Previews are keyed by template (orientation changes the aspect) -> drop the old ones and refetch.
    this.clearPreviews();
    this.refreshPreviews();
    this.persist();
  }

  onDashboardChange(dashboard: SupersetDashboardWithMetadata): void {
    // Switching to a different dashboard must clear the canvas: its cells reference the previous
    // dashboard's charts, but the export takes its title from the newly selected dashboard, so a
    // stale chart would be exported under the wrong dashboard's name. Only clear on a real change —
    // not when the restore flow re-selects the saved dashboard to reattach its saved cells.
    const previous = this.selectedDashboard();
    if (previous && (previous.instanceName !== dashboard.instanceName || previous.id !== dashboard.id)) {
      this.resetPages();
    }
    this.selectedDashboard.set(dashboard);
    this.pdfExport.getCharts(dashboard.instanceName, dashboard.id).subscribe(c => this.charts.set(c));
    this.pdfExport.getMarkdownBlocks(dashboard.instanceName, dashboard.id).subscribe(m => this.markdownBlocks.set(m));
    this.refreshPreviews();   // load previews for any restored tiles now that the dashboard is known
    this.persist();
  }

  /** Palette label for an existing markdown block: first line, at most 50 chars. */
  mdLabel(code: string): string {
    const firstLine = code.split('\n', 1)[0].trim();
    return firstLine.length > 50 ? firstLine.slice(0, 50) + '…' : firstLine || '(leer)';
  }

  onDragStart(payload: PaletteItem): void {
    this.dragPayload = payload;
  }

  private onDrop(pos: GridsterItemConfig): void {
    if (!this.dragPayload) {
      return;
    }
    // pos.x/pos.y is the (empty) cell the user released on. Grow the tile to the largest footprint
    // that fits there without overlapping other tiles or leaving the 4x4 page.
    const {cols, rows} = this.fitFootprint(pos.x, pos.y);
    const cell: Cell = {...this.dragPayload, page: this.currentPage(), x: pos.x, y: pos.y, cols, rows};
    this.cells.update(cs => [...cs, cell]);
    this.dragPayload = null;
    this.ensurePreview(cell);
    this.persist();
  }

  /** Occupancy grid (PAGE_ROWS x PAGE_ROWS) of the current page's tiles; true = covered. */
  private occupancy(): boolean[][] {
    const grid = Array.from({length: PAGE_ROWS}, () => Array<boolean>(PAGE_ROWS).fill(false));
    this.visibleCells().forEach(c => {
      for (let y = c.y; y < c.y + c.rows && y < PAGE_ROWS; y++) {
        for (let x = c.x; x < c.x + c.cols && x < PAGE_ROWS; x++) {
          grid[y][x] = true;
        }
      }
    });
    return grid;
  }

  /** True when a cols x rows footprint anchored at (x,y) stays in the page and hits no occupied cell. */
  private footprintFits(grid: boolean[][], x: number, y: number, cols: number, rows: number): boolean {
    if (x + cols > PAGE_ROWS || y + rows > PAGE_ROWS) {
      return false;
    }
    for (let yy = y; yy < y + rows; yy++) {
      for (let xx = x; xx < x + cols; xx++) {
        if (grid[yy][xx]) {
          return false;
        }
      }
    }
    return true;
  }

  /** Largest footprint (preferring 2x2, then 2x1, 1x2, else 1x1) that fits anchored at the drop cell. */
  private fitFootprint(x: number, y: number): {cols: number; rows: number} {
    const grid = this.occupancy();
    const candidates = [{cols: 2, rows: 2}, {cols: 2, rows: 1}, {cols: 1, rows: 2}];
    for (const c of candidates) {
      if (this.footprintFits(grid, x, y, c.cols, c.rows)) {
        return c;
      }
    }
    return {cols: 1, rows: 1};   // the released cell itself was free (drop callback wouldn't fire otherwise)
  }

  /** Stable cache key for a chart tile's preview: same chart at the same size + template shares one
   *  screenshot. Includes the template so switching orientation re-renders at the new aspect. */
  previewKey(cell: Cell): string {
    return `${cell.chartId}_${cell.cols}_${cell.rows}_${this.selectedTemplate()}`;
  }

  /** The loaded preview object URL for a chart tile, or undefined while it is still loading/absent. */
  previewUrl(cell: Cell): string | undefined {
    return this.previewUrls().get(this.previewKey(cell));
  }

  /** True while a chart tile's preview screenshot is being fetched (drives the per-tile spinner). */
  previewLoadingFor(cell: Cell): boolean {
    return this.previewLoading().has(this.previewKey(cell));
  }

  /** Fetch a chart tile's preview screenshot once (async, deduplicated by key). */
  private ensurePreview(cell: Cell): void {
    const dashboard = this.selectedDashboard();
    if (cell.type !== 'chart' || cell.chartId == null || dashboard == null) {
      return;
    }
    const key = this.previewKey(cell);
    if (this.previewUrls().has(key) || this.previewLoading().has(key)) {
      return;
    }
    this.previewLoading.update(s => new Set(s).add(key));
    this.pdfExport.getChartPreview(dashboard.instanceName, dashboard.id, cell.chartId, cell.cols, cell.rows, this.selectedTemplate())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          this.previewUrls.update(m => new Map(m).set(key, URL.createObjectURL(blob)));
          this.clearLoading(key);
        },
        error: () => this.clearLoading(key),   // leave the fallback icon in place
      });
  }

  /** Kick off previews for every chart tile that doesn't have one yet (e.g. after restore). */
  private refreshPreviews(): void {
    this.cells().forEach(c => this.ensurePreview(c));
  }

  private clearLoading(key: string): void {
    this.previewLoading.update(s => {
      const next = new Set(s);
      next.delete(key);
      return next;
    });
  }

  /** Revoke all preview object URLs and reset the caches (on dashboard/template change and teardown). */
  private clearPreviews(): void {
    this.previewUrls().forEach(url => URL.revokeObjectURL(url));
    this.previewUrls.set(new Map());
    this.previewLoading.set(new Set());
  }

  ngOnDestroy(): void {
    this.clearPreviews();
  }

  /** Open the dialog to create a new editable markdown block. */
  addMarkdown(): void {
    this.editingCell = null;
    this.editingText.set('');
    this.editorOpen.set(true);
  }

  /** Open the dialog to edit an existing editable markdown cell. */
  editCell(cell: Cell): void {
    if (cell.type !== 'markdown' || cell.readonly) {
      return;
    }
    this.editingCell = cell;
    this.editingText.set(cell.markdown ?? '');
    this.editorOpen.set(true);
  }

  saveEditor(): void {
    const text = this.editingText();
    if (this.editingCell) {
      const target = this.editingCell;
      this.cells.update(cs => cs.map(c => (c === target ? {...c, markdown: text} : c)));
    } else if (text.trim()) {
      this.cells.update(cs => [...cs, {type: 'markdown', markdown: text, readonly: false, page: this.currentPage(), x: 0, y: 0, cols: 4, rows: 1}]);
    }
    this.closeEditor();
    this.persist();
  }

  closeEditor(): void {
    this.editorOpen.set(false);
    this.editingCell = null;
  }

  removeCell(cell: Cell): void {
    this.cells.update(cs => cs.filter(c => c !== cell));
    this.persist();
  }

  onCellChange(cell: Cell): void {
    this.ensurePreview(cell);   // a resize changes the tile size -> fetch a preview at the new aspect
    this.persist();
  }

  /** Switch the canvas to another page. */
  goToPage(page: number): void {
    if (page >= 0 && page < this.pageCount()) {
      this.currentPage.set(page);
    }
  }

  /** Append a new empty page and jump to it. */
  addPage(): void {
    this.pageCount.update(p => p + 1);
    this.currentPage.set(this.pageCount() - 1);
    this.persist();
  }

  /** Remove the current page (only when it is empty and not the last one); pages after it shift down. */
  removePage(): void {
    if (!this.canRemovePage()) {
      return;
    }
    const removed = this.currentPage();
    this.cells.update(cs => cs.map(c => (c.page > removed ? {...c, page: c.page - 1} : c)));
    this.pageCount.update(p => p - 1);
    if (this.currentPage() >= this.pageCount()) {
      this.currentPage.set(this.pageCount() - 1);
    }
    this.persist();
  }

  private resetPages(): void {
    this.cells.set([]);
    this.pageCount.set(1);
    this.currentPage.set(0);
    this.clearPreviews();
  }

  clear(): void {
    this.resetPages();
    this.persist();
  }

  export(): void {
    const dashboard = this.selectedDashboard();
    if (dashboard == null || this.cells().length === 0) {
      return;
    }
    // Map each cell's page + local y to the global grid row the backend page-breaks on.
    const items: PdfLayoutItem[] = this.cells().map(c => ({
      type: c.type,
      chartId: c.chartId,
      markdown: c.markdown,
      name: c.name,
      x: c.x,
      y: c.page * PAGE_ROWS + c.y,
      cols: c.cols,
      rows: c.rows,
    }));
    const request: PdfLayoutRequest = {
      instanceName: dashboard.instanceName,
      dashboardId: dashboard.id,
      title: dashboard.dashboardTitle,
      template: this.selectedTemplate(),
      items,
    };
    this.exporting.set(true);
    this.pdfExport.exportCustom(request).subscribe({
      next: blob => {
        this.download(blob, `dashboard-${dashboard.id}.pdf`);
        this.exporting.set(false);
      },
      error: (err) => {
        this.exporting.set(false);
        console.error('PDF export failed', err);   // full detail (incl. 502/504 body) for debugging
        this.notification.error('@PDF export failed');
      },
    });
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.style.display = 'none';
    // The anchor must be in the DOM for the click to trigger a download in some browsers (Firefox),
    // and the object URL must outlive the click so the browser can finish reading the blob.
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 10000);
  }

  private persist(): void {
    const dashboard = this.selectedDashboard();
    const state = {
      instanceName: dashboard?.instanceName ?? null,
      dashboardId: dashboard?.id ?? null,
      template: this.selectedTemplate(),
      pageCount: this.pageCount(),
      currentPage: this.currentPage(),
      cells: this.cells(),
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }

  private restore(): void {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return;
    }
    try {
      const state = JSON.parse(raw) as {
        instanceName: string | null;
        dashboardId: number | null;
        template?: string | null;
        pageCount?: number | null;
        currentPage?: number | null;
        cells: (Cell & {page?: number})[];
      };
      if (state.cells) {
        // Migrate any pre-paginator layout (global y, no page) to page + local y.
        this.cells.set(state.cells.map(c => c.page === undefined
          ? {...c, page: Math.floor((c.y ?? 0) / PAGE_ROWS), y: (c.y ?? 0) % PAGE_ROWS}
          : c));
      }
      if (state.template) {
        this.selectedTemplate.set(state.template);
      }
      // pageCount is at least 1 and must cover the furthest page any restored cell lives on.
      const maxCellPage = this.cells().reduce((m, c) => Math.max(m, c.page), 0);
      this.pageCount.set(Math.max(1, state.pageCount ?? 1, maxCellPage + 1));
      this.currentPage.set(Math.min(Math.max(0, state.currentPage ?? 0), this.pageCount() - 1));
      if (state.instanceName != null && state.dashboardId != null) {
        // Re-select once the (data-domain-filtered) dashboard list arrives from the store.
        this.pendingRestore = {instanceName: state.instanceName, dashboardId: state.dashboardId};
      }
    } catch {
      // ignore corrupt storage
    }
  }
}
