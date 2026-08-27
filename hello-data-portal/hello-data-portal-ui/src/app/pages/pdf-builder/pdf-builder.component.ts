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

import {Component, OnInit, computed, inject, signal} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {Select} from "primeng/select";
import {Button} from "primeng/button";
import {Ripple} from "primeng/ripple";
import {TranslocoPipe} from "@jsverse/transloco";
import {Store} from "@ngrx/store";
import {DisplayGrid, Gridster, GridsterConfig, GridsterItem, GridsterItemConfig, GridType} from "angular-gridster2";
import {ICON_REGISTRY} from "../../shared/icons";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {MyDashboardsService} from "../../store/my-dashboards/my-dashboards.service";
import {SupersetDashboardWithMetadata} from "../../store/start-page/start-page.model";
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

  private myDashboards = inject(MyDashboardsService);
  private pdfExport = inject(PdfExportService);
  private store = inject(Store);

  dashboards = signal<SupersetDashboardWithMetadata[]>([]);
  templates = signal<PdfTemplateRef[]>(PDF_TEMPLATES);
  selectedTemplate = signal<string>('portrait');
  charts = signal<PdfChartRef[]>([]);
  markdownBlocks = signal<string[]>([]);
  selectedDashboard = signal<SupersetDashboardWithMetadata | null>(null);
  cells = signal<Cell[]>([]);

  /** Top offsets (px) of page-boundary lines, one per interior 4-row page break. */
  pageDividers = computed(() => {
    const rowsUsed = this.cells().reduce((m, c) => Math.max(m, c.y + c.rows), PAGE_ROWS);
    const pages = Math.ceil(rowsUsed / PAGE_ROWS);
    return Array.from({length: Math.max(0, pages - 1)}, (_, i) => (i + 1) * PAGE_ROWS * ROW_PITCH + GRID_MARGIN / 2);
  });

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
    minRows: 4,
    fixedRowHeight: 60,
    margin: 8,
    pushItems: true,
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
    this.myDashboards.getMyDashboards().subscribe(d => this.dashboards.set(d));
    this.restore();
  }

  onTemplateChange(id: string): void {
    this.selectedTemplate.set(id);
    this.persist();
  }

  onDashboardChange(dashboard: SupersetDashboardWithMetadata): void {
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

  onCellChange(): void {
    this.persist();
  }

  clear(): void {
    this.cells.set([]);
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
      error: () => this.exporting.set(false),
    });
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  private persist(): void {
    const dashboard = this.selectedDashboard();
    const state = {
      instanceName: dashboard?.instanceName ?? null,
      dashboardId: dashboard?.id ?? null,
      template: this.selectedTemplate(),
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
        cells: Cell[];
      };
      if (state.cells) {
        this.cells.set(state.cells);
      }
      if (state.template) {
        this.selectedTemplate.set(state.template);
      }
      if (state.instanceName != null && state.dashboardId != null) {
        // Re-select once the dashboard list has loaded, then reload its palette.
        this.myDashboards.getMyDashboards().subscribe(dashboards => {
          this.dashboards.set(dashboards);
          const match = dashboards.find(d => d.instanceName === state.instanceName && d.id === state.dashboardId);
          if (match) {
            this.onDashboardChange(match);
          }
        });
      }
    } catch {
      // ignore corrupt storage
    }
  }
}
