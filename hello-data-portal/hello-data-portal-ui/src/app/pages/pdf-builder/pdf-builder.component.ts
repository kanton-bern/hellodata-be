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

import {Component, DestroyRef, OnInit, computed, inject, signal} from "@angular/core";
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

/** A gridster cell that also carries what it renders. */
type Cell = GridsterItemConfig & PaletteItem;

const STORAGE_KEY = 'pdf-builder-layout';

/** A PDF page is PAGE_ROWS grid rows tall; the builder page-breaks and clamps on this. */
const PAGE_ROWS = 4;
/** Must match the gridster options below (fixedRowHeight + margin). */
const ROW_HEIGHT = 60;
const GRID_MARGIN = 8;
const ROW_PITCH = ROW_HEIGHT + GRID_MARGIN;

@Component({
  selector: 'app-pdf-builder',
  standalone: true,
  imports: [FormsModule, Gridster, GridsterItem, Select, Button, Ripple, TranslocoPipe],
  templateUrl: './pdf-builder.component.html',
  styleUrl: './pdf-builder.component.scss',
})
export class PdfBuilderComponent implements OnInit {
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
  /** Number of PDF pages the canvas shows. The grid is a fixed pageCount * PAGE_ROWS rows tall, so
   *  there are always empty cells to drop into on every page; grown via "Add page" (or automatically
   *  when a cell ends up on a lower page). Persisted with the layout. */
  pageCount = signal(1);

  /** Picker options: label is the dashboard title, suffixed with the data-domain name only when all
   *  domains are shown (so dashboards from different domains stay distinguishable). */
  dashboardOptions = computed(() => this.dashboards().map(d => ({
    label: this.allDomainsSelected() && d.contextName ? `${d.dashboardTitle} (${d.contextName})` : d.dashboardTitle,
    value: d,
  })));

  /** Top offsets (px) of page-boundary lines, one per interior page break. */
  pageDividers = computed(() =>
    Array.from({length: Math.max(0, this.pageCount() - 1)}, (_, i) => (i + 1) * PAGE_ROWS * ROW_PITCH + GRID_MARGIN / 2));

  /** Number of pages the current cells actually occupy (at least 1). */
  contentPages = computed(() => {
    const rowsUsed = this.cells().reduce((m, c) => Math.max(m, c.y + c.rows), 0);
    return Math.max(1, Math.ceil(rowsUsed / PAGE_ROWS));
  });

  /** The last page can only be removed when it is empty (no cell reaches into it). */
  canRemovePage = computed(() => this.pageCount() > this.contentPages());

  exporting = signal(false);
  editorOpen = signal(false);
  editingText = signal('');
  /** The editable cell being edited, or null when creating a new block. */
  private editingCell: Cell | null = null;

  /** The palette entry currently being dragged (set on dragstart). */
  private dragPayload: PaletteItem | null = null;

  options: GridsterConfig = {
    gridType: GridType.VerticalFixed,
    displayGrid: DisplayGrid.Always,
    minCols: 4,
    maxCols: 4,
    // Fixed height (pageCount * PAGE_ROWS), kept in sync by syncGrid(); a fixed grid means every page
    // always has empty droppable cells, so no drag-to-expand hack is needed to reach lower pages.
    minRows: PAGE_ROWS,
    maxRows: PAGE_ROWS,
    fixedRowHeight: 60,
    margin: 8,
    // Never rearrange the user's settled layout: moving/resizing a chart must not push others aside.
    pushItems: false,
    swap: false,
    pushResizeItems: false,
    // Validate the empty-cell drop against the ACTUAL 2x2 footprint we place (gridster otherwise
    // validates a 1x1, so a drop near a page boundary passes as 1x1 but the placed 2x2 straddles the
    // page / grid bottom and gets auto-relocated to page 1). This also shows a 2x2 drop preview.
    defaultItemCols: 2,
    defaultItemRows: 2,
    // A drop that doesn't fit where released should do nothing (the 2x2 preview shows valid spots),
    // rather than silently jumping the chart to the first free slot on another page.
    disableAutoPositionOnConflict: true,
    draggable: {enabled: true, ignoreContentClass: 'cell-body'},
    resizable: {enabled: true},
    enableEmptyCellDrop: true,
    // A page is 4x4 cells; a chart may not straddle a 4-row page boundary.
    itemValidateCallback: (item: GridsterItemConfig) =>
      Math.floor(item.y / PAGE_ROWS) === Math.floor((item.y + item.rows - 1) / PAGE_ROWS),
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
        this.cells.set([]);
        this.pageCount.set(1);
        this.syncGrid();
        this.persist();
      }
    });
  }

  onTemplateChange(id: string): void {
    this.selectedTemplate.set(id);
    this.persist();
  }

  onDashboardChange(dashboard: SupersetDashboardWithMetadata): void {
    // Switching to a different dashboard must clear the canvas: its cells reference the previous
    // dashboard's charts, but the export takes its title from the newly selected dashboard, so a
    // stale chart would be exported under the wrong dashboard's name. Only clear on a real change —
    // not when the restore flow re-selects the saved dashboard to reattach its saved cells.
    const previous = this.selectedDashboard();
    if (previous && (previous.instanceName !== dashboard.instanceName || previous.id !== dashboard.id)) {
      this.cells.set([]);
      this.pageCount.set(1);
      this.syncGrid();
    }
    this.selectedDashboard.set(dashboard);
    this.pdfExport.getCharts(dashboard.instanceName, dashboard.id).subscribe(c => this.charts.set(c));
    this.pdfExport.getMarkdownBlocks(dashboard.instanceName, dashboard.id).subscribe(m => this.markdownBlocks.set(m));
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
    const cell: Cell = {...this.dragPayload, x: pos.x, y: pos.y, cols: 2, rows: 2};
    this.cells.update(cs => [...cs, cell]);
    this.dragPayload = null;
    this.syncGrid();
    this.persist();
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
      this.cells.update(cs => [...cs, {type: 'markdown', markdown: text, readonly: false, x: 0, y: 0, cols: 4, rows: 1}]);
    }
    this.closeEditor();
    this.syncGrid();
    this.persist();
  }

  closeEditor(): void {
    this.editorOpen.set(false);
    this.editingCell = null;
  }

  removeCell(cell: Cell): void {
    this.cells.update(cs => cs.filter(c => c !== cell));
    this.syncGrid();
    this.persist();
  }

  onCellChange(): void {
    // A move/resize may drop a cell onto a lower page; grow the grid to keep it in view.
    this.syncGrid();
    this.persist();
  }

  /** Append an empty page so charts can be dropped there without dragging one down to expand. */
  addPage(): void {
    this.pageCount.update(p => p + 1);
    this.syncGrid();
    this.persist();
  }

  /** Remove the last page (only allowed when it is empty). */
  removePage(): void {
    if (!this.canRemovePage()) {
      return;
    }
    this.pageCount.update(p => p - 1);
    this.syncGrid();
    this.persist();
  }

  /** Keep the fixed grid height in sync with pageCount, never smaller than the pages the cells span,
   *  and push the new bounds to gridster at runtime. */
  private syncGrid(): void {
    if (this.pageCount() < this.contentPages()) {
      this.pageCount.set(this.contentPages());
    }
    const rows = this.pageCount() * PAGE_ROWS;
    if (this.options.minRows !== rows || this.options.maxRows !== rows) {
      this.options.minRows = rows;
      this.options.maxRows = rows;
      this.options['api']?.optionsChanged?.();
    }
  }

  clear(): void {
    this.cells.set([]);
    this.pageCount.set(1);
    this.syncGrid();
    this.persist();
  }

  export(): void {
    const dashboard = this.selectedDashboard();
    if (dashboard == null || this.cells().length === 0) {
      return;
    }
    const items: PdfLayoutItem[] = this.cells().map(c => ({
      type: c.type,
      chartId: c.chartId,
      markdown: c.markdown,
      name: c.name,
      x: c.x,
      y: c.y,
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
        cells: Cell[];
      };
      if (state.cells) {
        this.cells.set(state.cells);
      }
      if (state.template) {
        this.selectedTemplate.set(state.template);
      }
      if (state.pageCount && state.pageCount > 0) {
        this.pageCount.set(state.pageCount);
      }
      // Size the grid to the restored pages/cells (bumps pageCount up if cells span further).
      this.syncGrid();
      if (state.instanceName != null && state.dashboardId != null) {
        // Re-select once the (data-domain-filtered) dashboard list arrives from the store.
        this.pendingRestore = {instanceName: state.instanceName, dashboardId: state.dashboardId};
      }
    } catch {
      // ignore corrupt storage
    }
  }
}
