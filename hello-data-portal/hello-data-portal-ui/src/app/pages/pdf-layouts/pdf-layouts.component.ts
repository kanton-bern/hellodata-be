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

import {Component, computed, DestroyRef, inject, OnInit, signal} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {DatePipe, LowerCasePipe} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {Router} from "@angular/router";
import {Store} from "@ngrx/store";
import {TranslocoPipe, TranslocoService} from "@jsverse/transloco";
import {ConfirmationService, PrimeTemplate} from "primeng/api";
import {TableModule} from "primeng/table";
import {Button} from "primeng/button";
import {Ripple} from "primeng/ripple";
import {Tooltip} from "primeng/tooltip";
import {InputText} from "primeng/inputtext";
import {IconField} from "primeng/iconfield";
import {InputIcon} from "primeng/inputicon";
import {Card} from "primeng/card";
import {ConfirmDialog} from "primeng/confirmdialog";
import {ICON_REGISTRY} from "../../shared/icons";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {showError, showSuccess} from "../../store/app/app.action";
import {selectAllAvailableDataDomains, selectSelectedDataDomain} from "../../store/my-dashboards/my-dashboards.selector";
import {PdfExportService} from "../../store/pdf-export/pdf-export.service";
import {PdfLayoutSummary} from "../../store/pdf-export/pdf-export.model";

/** A table row: the layout plus the display name of its data domain. */
type LayoutRow = PdfLayoutSummary & {dataDomainName: string};

/**
 * Management of saved PDF layouts: list, create, edit and delete. Layouts are shared with everyone
 * who may access their dashboard; only the creator may change or delete one (until a dedicated
 * permission exists). The PDF export page only picks these layouts.
 */
@Component({
  selector: 'app-pdf-layouts',
  standalone: true,
  imports: [FormsModule, TableModule, PrimeTemplate, Button, Ripple, Tooltip, InputText, IconField, InputIcon, Card,
    ConfirmDialog, DatePipe, LowerCasePipe, TranslocoPipe],
  templateUrl: './pdf-layouts.component.html',
})
export class PdfLayoutsComponent implements OnInit {
  protected readonly icons = ICON_REGISTRY;

  private pdfExport = inject(PdfExportService);
  private store = inject(Store);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private transloco = inject(TranslocoService);
  private confirmationService = inject(ConfirmationService);

  private layouts = signal<PdfLayoutSummary[]>([]);
  private dataDomainNames = signal<Map<string, string>>(new Map());
  /** Key of the selected data domain, or null for "All Data Domains". */
  private selectedContextKey = signal<string | null>(null);
  loading = signal(false);
  filterValue = '';

  /** Layouts of the selected data domain, with their data domain name for display. */
  rows = computed<LayoutRow[]>(() => {
    const key = this.selectedContextKey();
    const names = this.dataDomainNames();
    return this.layouts()
      .filter(l => key === null || l.contextKey === key)
      .map(l => ({...l, dataDomainName: names.get(l.contextKey) ?? l.contextKey}));
  });

  ngOnInit(): void {
    this.store.dispatch(createBreadcrumbs({breadcrumbs: [{label: '@PDF layouts'}]}));
    this.store.select(selectSelectedDataDomain).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(dd => this.selectedContextKey.set(dd === null || dd.id === '' ? null : dd.key));
    this.store.select(selectAllAvailableDataDomains).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(domains => this.dataDomainNames.set(new Map(domains.map(d => [d.key, d.name]))));
    this.loadLayouts();
  }

  createLayout(): void {
    this.router.navigate(['pdf-layouts', 'create']);
  }

  editLayout(layout: PdfLayoutSummary): void {
    if (layout.editable) {
      this.router.navigate(['pdf-layouts', 'edit', layout.id]);
    }
  }

  /** Open the PDF export page with this layout selected. */
  useLayout(layout: PdfLayoutSummary): void {
    this.router.navigate(['pdf-builder'], {queryParams: {layout: layout.id}});
  }

  deleteLayout(layout: PdfLayoutSummary): void {
    if (!layout.editable) {
      return;
    }
    this.confirmationService.confirm({
      key: 'deletePdfLayout',
      header: this.transloco.translate('@Delete layout'),
      message: this.transloco.translate('@Delete the saved layout {{name}}?', {name: layout.name}),
      icon: this.icons.DIALOG_WARNING.class,
      closeOnEscape: false,
      accept: () => this.pdfExport.deleteLayout(layout.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          this.store.dispatch(showSuccess({message: '@Layout deleted', interpolateParams: {name: layout.name}}));
          this.loadLayouts();
        },
        error: err => this.store.dispatch(showError({error: err})),
      }),
    });
  }

  private loadLayouts(): void {
    this.loading.set(true);
    this.pdfExport.getLayouts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: layouts => {
        this.layouts.set(layouts);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.store.dispatch(showError({error: err}));
      },
    });
  }
}
