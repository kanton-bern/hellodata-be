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
import {ActivatedRoute, Router} from "@angular/router";
import {Select} from "primeng/select";
import {Button} from "primeng/button";
import {Ripple} from "primeng/ripple";
import {Tooltip} from "primeng/tooltip";
import {Editor} from "primeng/editor";
import {InputText} from "primeng/inputtext";
import {ConfirmationService} from "primeng/api";
import {ConfirmDialog} from "primeng/confirmdialog";
import {TranslocoPipe, TranslocoService} from "@jsverse/transloco";
import {Store} from "@ngrx/store";
import {DisplayGrid, Gridster, GridsterConfig, GridsterItem, GridsterItemConfig, GridType} from "angular-gridster2";
import {ICON_REGISTRY} from "../../shared/icons";
import {markdownToHtml} from "../../shared/utils/markdown";
import {isRichTextEmpty, sanitizeRichText} from "../../shared/utils/sanitize-html";
import {disableEditorImageInsert} from "../../shared/utils/editor-utils";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {loadMyDashboards} from "../../store/my-dashboards/my-dashboards.action";
import {selectMyDashboards, selectSelectedDataDomain} from "../../store/my-dashboards/my-dashboards.selector";
import {SupersetDashboardWithMetadata} from "../../store/start-page/start-page.model";
import {NotificationService} from "../../shared/services/notification.service";
import {PdfExportService} from "../../store/pdf-export/pdf-export.service";
import {showError, showSuccess} from "../../store/app/app.action";
import {
  PDF_TEMPLATES,
  PdfChartRef,
  PdfLayoutItem,
  PdfLayoutRequest,
  PdfLayoutSaveRequest,
  PdfLayoutSummary,
  PdfSavedLayout,
  PdfSavedLayoutItem,
  PdfTemplateRef
} from "../../store/pdf-export/pdf-export.model";

/** What a cell renders. Markdown from the dashboard is read-only; text blocks created via
 *  "+ Add text block" are editable rich text (`html`). Older saved blocks may still carry `markdown`
 *  only - they are shown as-is and converted to `html` the first time they are edited. */
type PaletteItem = {
  type: 'chart' | 'markdown';
  chartId?: number;
  name?: string;
  markdown?: string;
  html?: string;
  readonly?: boolean;
};

/** A gridster cell that also carries what it renders and which page it lives on. `x`/`y` are LOCAL
 *  to that page's grid (y in 0..PAGE_ROWS-1); only the current page's cells are shown at a time. */
type Cell = GridsterItemConfig & PaletteItem & {page: number};

const STORAGE_KEY = 'pdf-builder-layout';

/** Each PDF page is a PAGE_ROWS x PAGE_ROWS grid. The export maps a cell's page + local y to the
 *  global row (page * PAGE_ROWS + y) so the backend page-breaks correctly. */
const PAGE_ROWS = 4;

/**
 * The grid layout builder, in one of two modes (route data `mode`):
 * - `export` (PDF export page): pick a saved layout or start empty, adapt it for your own export only
 *   (kept in the browser, never saved to the layout) and export the PDF.
 * - `manage` (PDF layouts section, create/edit): build a layout and save it for everyone with access
 *   to its dashboard.
 */
@Component({
  selector: 'app-pdf-builder',
  standalone: true,
  imports: [FormsModule, Gridster, GridsterItem, Select, Button, Ripple, Tooltip, ConfirmDialog, Editor, InputText, TranslocoPipe],
  templateUrl: './pdf-builder.component.html',
  styleUrl: './pdf-builder.component.scss',
})
export class PdfBuilderComponent implements OnInit, OnDestroy {
  protected readonly icons = ICON_REGISTRY;

  private pdfExport = inject(PdfExportService);
  private store = inject(Store);
  private destroyRef = inject(DestroyRef);
  private notification = inject(NotificationService);
  private transloco = inject(TranslocoService);
  private confirmationService = inject(ConfirmationService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly mode: 'export' | 'manage' = this.route.snapshot.data['mode'] === 'manage' ? 'manage' : 'export';
  /** The layout being edited in manage mode, or null when creating one. */
  readonly editId: string | null = this.mode === 'manage' ? this.route.snapshot.paramMap.get('id') : null;

  /** A dashboard to re-select from localStorage once it appears in the (data-domain-filtered) list. */
  private pendingRestore: {instanceName: string; dashboardId: number} | null = null;
  /** A remembered saved layout to re-fetch from the server once the dashboards are known, so a reload
   *  shows its latest version (e.g. saved from another window) instead of a stale local copy. */
  private pendingLayoutReload: string | null = null;
  /** The canvas as it was when the current layout was loaded/saved; differs from snapshot() when
   *  there are unsaved changes. */
  private savedSnapshot: string | null = null;
  /** Server version (modified or created timestamp) of the loaded layout, to notice saves made in
   *  another window. */
  private loadedVersion: number | null = null;
  private readonly onVisibilityChange = () => this.checkForNewerVersion();

  dashboards = signal<SupersetDashboardWithMetadata[]>([]);
  /** True when "All Data Domains" is selected; then the picker disambiguates titles with the domain. */
  allDomainsSelected = signal(true);
  /** Key of the selected data domain, or null for "All Data Domains" (filters the saved layouts). */
  selectedContextKey = signal<string | null>(null);

  /** The saved layouts the user may see (all data domains; filtered for the picker below). */
  savedLayouts = signal<PdfLayoutSummary[]>([]);
  /** The saved layout the canvas was loaded from. Cleared when the user switches to another dashboard. */
  currentLayout = signal<{id: string; name: string} | null>(null);
  /** Name of the layout being created/edited (manage mode). */
  layoutName = signal('');
  saving = signal(false);
  loadingLayout = signal(false);
  templates = signal<PdfTemplateRef[]>(PDF_TEMPLATES);
  selectedTemplate = signal<string>('portrait');
  charts = signal<PdfChartRef[]>([]);
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

  /** Saved-layout picker options, narrowed to the selected data domain; the dashboard title is
   *  appended so equally named layouts of different dashboards stay distinguishable. */
  savedLayoutOptions = computed(() => {
    const key = this.selectedContextKey();
    return this.savedLayouts()
      .filter(l => key === null || l.contextKey === key)
      .map(l => ({label: l.dashboardTitle ? `${l.name} (${l.dashboardTitle})` : l.name, value: l.id}));
  });

  /** Cells on the currently shown page (what the 4x4 grid renders). */
  visibleCells = computed(() => this.cells().filter(c => c.page === this.currentPage()));

  /** 0-indexed page numbers, for the paginator. */
  pages = computed(() => Array.from({length: this.pageCount()}, (_, i) => i));

  /** A page can be removed whenever it is not the last remaining page; removing a non-empty page
   *  discards its tiles (after a confirm — see removePage). */
  canRemovePage = computed(() => this.pageCount() > 1);

  /** True while at least one chart preview is still rendering. Export is blocked until this clears so
   *  the export runs against a fully warmed cache (no in-flight render to race or re-render). */
  anyPreviewLoading = computed(() => this.previewLoading().size > 0);

  exporting = signal(false);
  /** "Fresh data" export toggle: re-render every chart (force) so the PDF reflects current data,
   *  bypassing the screenshot cache. Off by default = fast, cached. */
  freshData = signal(false);
  editorOpen = signal(false);
  editingText = signal('');
  /** 'edit' when changing an existing block, 'add' when creating one - drives the dialog title. */
  editorMode = signal<'add' | 'edit'>('add');
  /** The editable cell being edited, or null when creating a new block. */
  private editingCell: Cell | null = null;

  /** The Quill instance behind the rich-text editor, used to read clean semantic HTML on save. */
  private quill: any = null;

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
    if (this.mode === 'manage') {
      this.store.dispatch(createBreadcrumbs({breadcrumbs: [
        {label: '@PDF layouts', routerLink: 'pdf-layouts'},
        {label: this.editId ? '@Edit layout' : '@Create layout'},
      ]}));
      this.pendingLayoutReload = this.editId;
    } else {
      this.store.dispatch(createBreadcrumbs({breadcrumbs: [{label: '@PDF export'}]}));
      this.restore();
      // "Use for PDF export" from the layout management opens this page with ?layout=<id>.
      const requested = this.route.snapshot.queryParamMap.get('layout');
      if (requested) {
        this.pendingLayoutReload = requested;
        this.router.navigate([], {relativeTo: this.route, queryParams: {}, replaceUrl: true});
      }
      this.loadSavedLayouts();
      document.addEventListener('visibilitychange', this.onVisibilityChange);
    }
    this.store.dispatch(loadMyDashboards());
    this.store.select(selectSelectedDataDomain).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(dd => {
        const all = dd === null || dd.id === '';
        this.allDomainsSelected.set(all);
        this.selectedContextKey.set(all || !dd ? null : dd.key);
      });
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
      }
      // A layout to (re)load from the server needs the dashboards to resolve its dashboard.
      if (this.pendingLayoutReload && dashboards.length > 0) {
        this.reloadPendingLayout();
        return;
      }
      if (this.pendingRestore) {
        return;
      }
      // If the data domain changed and the selected dashboard is no longer listed, reset the picker
      // and clear the canvas — its cells reference charts of the now-unavailable dashboard.
      const current = this.selectedDashboard();
      if (current && !dashboards.some(d => d.instanceName === current.instanceName && d.id === current.id)) {
        this.selectedDashboard.set(null);
        this.charts.set([]);
        this.resetPages();
        this.detachLayout();
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
      // A saved layout belongs to one dashboard; don't let a later save overwrite it with another one.
      this.detachLayout();
    }
    this.selectedDashboard.set(dashboard);
    this.pdfExport.getCharts(dashboard.instanceName, dashboard.id).subscribe(c => this.charts.set(c));
    this.refreshPreviews();   // load previews for any restored tiles now that the dashboard is known
    this.persist();
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

  /** Fetch a chart tile's preview screenshot once (async, deduplicated by key). With {@code force}
   *  the backend re-renders even if a cached screenshot exists (per-tile refresh). */
  private ensurePreview(cell: Cell, force = false): void {
    const dashboard = this.selectedDashboard();
    if (cell.type !== 'chart' || cell.chartId == null || dashboard == null) {
      return;
    }
    const key = this.previewKey(cell);
    if (this.previewUrls().has(key) || this.previewLoading().has(key)) {
      return;
    }
    this.previewLoading.update(s => new Set(s).add(key));
    this.pdfExport.getChartPreview(dashboard.instanceName, dashboard.id, cell.chartId, cell.cols, cell.rows, this.selectedTemplate(), force)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          this.previewUrls.update(m => new Map(m).set(key, URL.createObjectURL(blob)));
          this.clearLoading(key);
        },
        error: () => this.clearLoading(key),   // leave the fallback icon in place
      });
  }

  /** Per-tile "refresh": drop this tile's cached preview and re-fetch with force so a chart whose
   *  data changed within the cache TTL is re-rendered. No-op while it is already loading. */
  refreshPreview(cell: Cell): void {
    if (cell.type !== 'chart') {
      return;
    }
    const key = this.previewKey(cell);
    if (this.previewLoading().has(key)) {
      return;
    }
    const existing = this.previewUrls().get(key);
    if (existing) {
      URL.revokeObjectURL(existing);
    }
    this.previewUrls.update(m => {
      const next = new Map(m);
      next.delete(key);
      return next;
    });
    this.ensurePreview(cell, true);
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
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    this.clearPreviews();
  }

  /** Open the dialog to create a new editable markdown block. */
  addMarkdown(): void {
    this.editingCell = null;
    this.editorMode.set('add');
    this.editingText.set('');
    this.editorOpen.set(true);
  }

  /** Open the dialog to edit an existing editable markdown cell. */
  editCell(cell: Cell): void {
    if (cell.type !== 'markdown' || cell.readonly) {
      return;
    }
    this.editingCell = cell;
    this.editorMode.set('edit');
    this.editingText.set(cell.html ?? markdownToHtml(cell.markdown));
    this.editorOpen.set(true);
  }

  saveEditor(): void {
    // getSemanticHTML gives real <ul>/<ol> lists instead of Quill's internal <ol data-list> markup.
    const html = this.quill?.getSemanticHTML?.() ?? this.editingText();
    const empty = isRichTextEmpty(html);
    if (this.editingCell) {
      const target = this.editingCell;
      // Drop any legacy markdown: once edited, the block is rich text only.
      const {markdown: _legacy, ...rest} = target;
      const updated: Cell = {...rest, html: empty ? '' : html};
      this.cells.update(cs => cs.map(c => (c === target ? updated : c)));
    } else if (!empty) {
      // Place the new block in the first free slot on the page. Hardcoding (0,0) made a second block
      // land on top of the first, where gridster (pushItems/autoPosition off) can't show it until the
      // first is removed. If the page is full, keep the dialog open so the text isn't lost.
      const spot = this.findMarkdownSpot();
      if (!spot) {
        this.notification.warn('@No free space on this page - add a page or make room first');
        return;
      }
      this.cells.update(cs => [...cs, {type: 'markdown', html, readonly: false, page: this.currentPage(), ...spot}]);
      this.reflowGrid();
    }
    this.closeEditor();
    this.persist();
  }

  /** Where a new full-width text block should go: the first fully-free row (4x1); if no whole row is
   *  free, the first free cell widened to the free run to its right; null when the page is full. */
  private findMarkdownSpot(): {x: number; y: number; cols: number; rows: number} | null {
    const grid = this.occupancy();
    for (let y = 0; y < PAGE_ROWS; y++) {
      if (grid[y].every(occupied => !occupied)) {
        return {x: 0, y, cols: PAGE_ROWS, rows: 1};
      }
    }
    for (let y = 0; y < PAGE_ROWS; y++) {
      for (let x = 0; x < PAGE_ROWS; x++) {
        if (!grid[y][x]) {
          let cols = 1;
          while (x + cols < PAGE_ROWS && !grid[y][x + cols]) {
            cols++;
          }
          return {x, y, cols, rows: 1};
        }
      }
    }
    return null;
  }

  /** Ask gridster to re-run its layout after we add a tile programmatically, so it is positioned (with
   *  its margins) right away instead of only after the next drag/click. Deferred so the new
   *  gridster-item has registered before the reflow runs. */
  private reflowGrid(): void {
    setTimeout(() => this.options['api']?.optionsChanged?.());
  }

  onEditorInit(event: {editor: any}): void {
    this.quill = event.editor;
    disableEditorImageInsert(event.editor);
  }

  /** Rendered preview of a text cell, so the canvas shows the formatting the exported PDF will have. */
  textHtml(cell: Cell): string {
    return cell.html ? sanitizeRichText(cell.html) : markdownToHtml(cell.markdown);
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

  /** Remove the current page (any page but the last one); its tiles are discarded and the pages
   *  after it shift down. A non-empty page asks for confirmation first, so charts aren't lost by
   *  accident. */
  removePage(): void {
    if (!this.canRemovePage()) {
      return;
    }
    const removed = this.currentPage();
    // An empty page carries nothing to lose, so drop it straight away; a non-empty page asks for
    // confirmation first via the app's standard dialog so charts aren't discarded by accident.
    if (this.visibleCells().length === 0) {
      this.doRemovePage(removed);
      return;
    }
    this.confirmationService.confirm({
      key: 'removePdfPage',
      header: this.transloco.translate('@Remove page'),
      message: this.transloco.translate('@Remove this page and its charts?'),
      icon: this.icons.DIALOG_WARNING.class,
      closeOnEscape: false,
      accept: () => this.doRemovePage(removed),
    });
  }

  /** Drop the given page's tiles, then shift every later page down into the gap it leaves. */
  private doRemovePage(removed: number): void {
    this.cells.update(cs => cs
      .filter(c => c.page !== removed)
      .map(c => (c.page > removed ? {...c, page: c.page - 1} : c)));
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

  export(): void {
    const dashboard = this.selectedDashboard();
    // Guard against a re-entrant call while a PDF is already being generated (the button is also
    // disabled during export, but this makes double-submission impossible even programmatically).
    if (dashboard == null || this.cells().length === 0 || this.exporting()) {
      return;
    }
    // Map each cell's page + local y to the global grid row the backend page-breaks on.
    const items: PdfLayoutItem[] = this.cells().map(c => ({
      type: c.type,
      chartId: c.chartId,
      markdown: c.markdown,
      html: c.html,
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
      force: this.freshData(),
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

  /** Refresh the saved-layout picker. Also drops a remembered layout that no longer exists. */
  private loadSavedLayouts(): void {
    this.pdfExport.getLayouts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: layouts => {
        this.savedLayouts.set(layouts);
        const current = this.currentLayout();
        if (current && !layouts.some(l => l.id === current.id)) {
          this.detachLayout();
          this.persist();
        }
      },
      error: err => this.store.dispatch(showError({error: err})),
    });
  }

  /** Load a saved layout onto the canvas. The backend refuses it when its dashboard no longer exists
   *  and drops charts that were removed from the dashboard (reported as a warning here). */
  onSavedLayoutSelect(id: string | null): void {
    if (!id || this.loadingLayout()) {
      return;
    }
    this.loadingLayout.set(true);
    this.pdfExport.getLayout(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: layout => {
        this.loadingLayout.set(false);
        this.applyLayout(layout);
      },
      error: err => {
        this.loadingLayout.set(false);
        this.store.dispatch(showError({error: err}));
      },
    });
  }

  private applyLayout(layout: PdfSavedLayout): void {
    if (this.mode === 'manage' && !layout.editable) {
      this.notification.error('@Only the creator can change this layout');
      this.router.navigate(['pdf-layouts']);
      return;
    }
    const dashboard = this.dashboards().find(d => d.instanceName === layout.instanceName && d.id === layout.dashboardId);
    if (!dashboard) {
      // Accessible for the backend but not in the picker (e.g. another data domain is selected).
      this.notification.error('@Dashboard {{name}} not found.', {name: layout.dashboardTitle ?? layout.dashboardId});
      return;
    }
    this.clearPreviews();
    this.selectedDashboard.set(dashboard);
    this.pdfExport.getCharts(dashboard.instanceName, dashboard.id).subscribe(c => this.charts.set(c));
    this.selectedTemplate.set(layout.template);
    this.cells.set(layout.items.map(item => ({...item} as Cell)));
    const maxCellPage = this.cells().reduce((m, c) => Math.max(m, c.page), 0);
    this.pageCount.set(Math.max(1, layout.pageCount, maxCellPage + 1));
    this.currentPage.set(0);
    this.attachLayout(layout);
    this.layoutName.set(layout.name);
    this.refreshPreviews();
    this.reflowGrid();
    this.persist();
    if (layout.removedCharts?.length) {
      this.notification.warn(layout.removedCharts.length === 1 ? '@Chart {{names}} not found and removed.' : '@Charts {{names}} not found and removed.',
        {names: this.joinNames(layout.removedCharts)});
    }
  }

  /** "A", "A and B", "A, B and C" - with a translated "and". */
  private joinNames(names: string[]): string {
    if (names.length <= 1) {
      return names.join('');
    }
    return `${names.slice(0, -1).join(', ')} ${this.transloco.translate('@and')} ${names[names.length - 1]}`;
  }

  /** Manage mode: create the layout, or update the edited one, then return to the layout list. */
  saveManagedLayout(): void {
    const dashboard = this.selectedDashboard();
    const name = this.layoutName().trim();
    if (this.mode !== 'manage' || dashboard == null || !name || this.saving()) {
      return;
    }
    const request: PdfLayoutSaveRequest = {
      name,
      instanceName: dashboard.instanceName,
      dashboardId: dashboard.id,
      template: this.selectedTemplate(),
      pageCount: this.pageCount(),
      items: this.layoutItems(),
    };
    const save$ = this.editId
      ? this.pdfExport.updateLayout(this.editId, request)
      : this.pdfExport.createLayout(request);
    this.saving.set(true);
    save$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        this.saving.set(false);
        this.attachLayout(saved);
        this.store.dispatch(showSuccess({message: '@Layout saved', interpolateParams: {name: saved.name}}));
        this.router.navigate(['pdf-layouts']);
      },
      error: err => {
        this.saving.set(false);
        this.store.dispatch(showError({error: err}));
      },
    });
  }

  /** Manage mode: back to the layout list, after confirming when there are unsaved changes. */
  cancelManage(): void {
    const dirty = this.editId
      ? this.hasUnsavedChanges() || this.layoutName().trim() !== (this.currentLayout()?.name ?? '')
      : this.cells().length > 0 || this.layoutName().trim() !== '';
    if (!dirty) {
      this.router.navigate(['pdf-layouts']);
      return;
    }
    this.confirmDiscard('@Unsaved changes message', () => this.router.navigate(['pdf-layouts']));
  }

  openLayoutManagement(): void {
    this.router.navigate(['pdf-layouts']);
  }

  /** Export mode: drop the local changes and reload the selected layout as it is saved. */
  resetToLayout(): void {
    const current = this.currentLayout();
    if (!current) {
      return;
    }
    if (!this.hasUnsavedChanges()) {
      this.onSavedLayoutSelect(current.id);
      return;
    }
    this.confirmDiscard('@Reset the layout and discard your changes?', () => this.onSavedLayoutSelect(current.id));
  }

  private confirmDiscard(messageKey: string, accept: () => void): void {
    this.confirmationService.confirm({
      key: 'discardPdfLayout',
      header: this.transloco.translate('@Discard changes'),
      message: this.transloco.translate(messageKey),
      icon: this.icons.DIALOG_WARNING.class,
      closeOnEscape: false,
      accept,
    });
  }

  /** The canvas cells in the shape they are saved in. */
  private layoutItems(): PdfSavedLayoutItem[] {
    return this.cells().map(c => ({
      type: c.type,
      chartId: c.chartId,
      name: c.name,
      markdown: c.markdown,
      html: c.html,
      readonly: c.readonly,
      page: c.page,
      x: c.x,
      y: c.y,
      cols: c.cols,
      rows: c.rows,
    }));
  }

  /** Everything a save would store, for detecting unsaved changes. Gridster moves/resizes cells in
   *  place, so this is computed on demand rather than as a signal. */
  private snapshot(): string {
    return JSON.stringify({template: this.selectedTemplate(), pageCount: this.pageCount(), items: this.layoutItems()});
  }

  /** True when the canvas differs from the loaded/saved layout. */
  private hasUnsavedChanges(): boolean {
    return this.currentLayout() !== null && this.savedSnapshot !== this.snapshot();
  }

  private attachLayout(layout: PdfLayoutSummary): void {
    this.currentLayout.set({id: layout.id, name: layout.name});
    this.savedSnapshot = this.snapshot();
    this.loadedVersion = layout.modifiedDate ?? layout.createdDate ?? null;
  }

  private detachLayout(): void {
    this.currentLayout.set(null);
    this.savedSnapshot = null;
    this.loadedVersion = null;
  }

  private reloadPendingLayout(): void {
    const id = this.pendingLayoutReload;
    this.pendingLayoutReload = null;
    // The loaded layout decides the dashboard; don't re-select a remembered one afterwards.
    this.pendingRestore = null;
    if (id) {
      this.onSavedLayoutSelect(id);
    }
  }

  /** Export mode: when the tab becomes visible again, pick up a newer version of the selected layout
   *  (changed in the layout management): reload it when there are no local changes, otherwise tell
   *  the user that "Reset to layout" gets the new version. */
  private checkForNewerVersion(): void {
    const current = this.currentLayout();
    if (document.visibilityState !== 'visible' || current === null || this.loadingLayout()) {
      return;
    }
    this.pdfExport.getLayouts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: layouts => {
        this.savedLayouts.set(layouts);
        const summary = layouts.find(l => l.id === current.id);
        if (!summary) {
          this.detachLayout();
          this.persist();
          return;
        }
        const version = summary.modifiedDate ?? summary.createdDate ?? null;
        if (version === null || version === this.loadedVersion || this.currentLayout()?.id !== current.id) {
          return;
        }
        if (this.hasUnsavedChanges()) {
          this.loadedVersion = version;   // warn once per newer version
          this.notification.warn('@Layout was updated', {name: summary.name});
        } else {
          this.onSavedLayoutSelect(current.id);
          this.notification.info('@Layout reloaded', {name: summary.name});
        }
      },
      error: () => {
        // background refresh only; the next explicit action reports errors
      },
    });
  }

  /** Remembers the export page canvas in the browser; the manage mode keeps nothing locally. */
  private persist(): void {
    if (this.mode === 'manage') {
      return;
    }
    const dashboard = this.selectedDashboard();
    const state = {
      instanceName: dashboard?.instanceName ?? null,
      dashboardId: dashboard?.id ?? null,
      template: this.selectedTemplate(),
      pageCount: this.pageCount(),
      currentPage: this.currentPage(),
      cells: this.cells(),
      layout: this.currentLayout(),
      savedSnapshot: this.savedSnapshot,
      loadedVersion: this.loadedVersion,
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
        layout?: {id: string; name: string} | null;
        savedSnapshot?: string | null;
        loadedVersion?: number | null;
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
      if (state.layout) {
        this.currentLayout.set(state.layout);
        this.savedSnapshot = state.savedSnapshot ?? null;
        this.loadedVersion = state.loadedVersion ?? null;
        // Without unsaved changes the server copy is authoritative: re-fetch it once the dashboards
        // are known. Unsaved local edits are kept as they are. State stored before change tracking
        // existed has no snapshot and is refreshed as well.
        if (state.savedSnapshot === undefined || !this.hasUnsavedChanges()) {
          this.pendingLayoutReload = state.layout.id;
        }
      }
      if (state.instanceName != null && state.dashboardId != null) {
        // Re-select once the (data-domain-filtered) dashboard list arrives from the store.
        this.pendingRestore = {instanceName: state.instanceName, dashboardId: state.dashboardId};
      }
    } catch {
      // ignore corrupt storage
    }
  }
}
