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

import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import {map, Observable, Subscription} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectEditedExternalDashboard} from "../../../../store/external-dashboards/external-dashboards.selector";
import { FormBuilder, FormGroup, Validators, FormsModule, ReactiveFormsModule } from "@angular/forms";
import {
  ExternalDashboard,
  ExternalDashboardMetadata
} from "../../../../store/external-dashboards/external-dashboards.model";
import {selectAvailableDataDomainItems} from "../../../../store/my-dashboards/my-dashboards.selector";
import { ConfirmationService, PrimeTemplate } from "primeng/api";
import {TranslateService} from "../../../../shared/services/translate.service";
import {clearUnsavedChanges, markUnsavedChanges} from "../../../../store/unsaved-changes/unsaved-changes.actions";
import {naviElements} from "../../../../app-navi-elements";
import {BaseComponent} from "../../../../shared/components/base/base.component";
import {navigate} from "../../../../store/app/app.action";
import {createBreadcrumbs} from "../../../../store/breadcrumb/breadcrumb.action";
import {
  createExternalDashboard,
  deleteExternalDashboard,
  updateExternalDashboard
} from "../../../../store/external-dashboards/external-dasboards.action";
import { AsyncPipe } from '@angular/common';
import { Select } from 'primeng/select';
import { Toolbar } from 'primeng/toolbar';
import { Button, ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
    selector: 'app-external-dashboard-edit',
    templateUrl: './external-dashboard-edit.component.html',
    styleUrls: ['./external-dashboard-edit.component.scss'],
    imports: [FormsModule, ReactiveFormsModule, Select, Toolbar, Button, Tooltip, ConfirmDialog, PrimeTemplate, ButtonDirective, AsyncPipe, TranslocoPipe]
})
export class ExternalDashboardEditComponent extends BaseComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private store = inject<Store<AppState>>(Store);
  private confirmationService = inject(ConfirmationService);
  private translateService = inject(TranslateService);

  editedExternalDashboard$: Observable<any>;
  availableDataDomains$: Observable<any>;
  externalDashboardForm!: FormGroup;
  formValueChangedSub!: Subscription;

  constructor() {

    super();
    this.availableDataDomains$ = this.store.select(selectAvailableDataDomainItems);

    this.editedExternalDashboard$ = this.store.select(selectEditedExternalDashboard).pipe(
      map(externalDashboard => {
        let externalDashboardForEdition = externalDashboard;
        if (externalDashboard === null) {
          externalDashboardForEdition = {
            url: '',
            title: '',
            datasource: '',
            scheduled: '',
            responsibility: '',
            dataAnalyst: '',
            department: '',
            businessProcess: '',
            id: '',
            contextKey: '',
            contextName: ''
          };
          this.store.dispatch(createBreadcrumbs({
            breadcrumbs: [
              {
                label: naviElements.externalDashboards.label,
                routerLink: naviElements.externalDashboards.path
              },
              {
                label: naviElements.externalDashboardCreate.label,
              }
            ]
          }));
        } else {
          this.store.dispatch(createBreadcrumbs({
            breadcrumbs: [
              {
                label: naviElements.externalDashboards.label,
                routerLink: naviElements.externalDashboards.path
              },
              {
                label: externalDashboardForEdition?.title
              }
            ]
          }));
        }
        const urlRegex = /^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w.-]+)+[\w\-._~:/?#[\]@!$&'()*+,;=%.]+$/;
        this.externalDashboardForm = this.fb.group({
          title: [externalDashboardForEdition?.title, Validators.compose([Validators.required.bind(this), Validators.minLength(3), Validators.maxLength(255), Validators.pattern(/[\p{L}\p{N}].*/u)])],
          url: [externalDashboardForEdition?.url, Validators.compose([Validators.required.bind(this), Validators.minLength(4), Validators.pattern(urlRegex)])],
          dataDomain: [externalDashboardForEdition?.contextKey, Validators.compose([Validators.required.bind(this)])],
          businessProcess: [externalDashboardForEdition?.businessProcess],
          department: [externalDashboardForEdition?.department],
          responsibility: [externalDashboardForEdition?.responsibility],
          dataAnalyst: [externalDashboardForEdition?.dataAnalyst],
          scheduled: [externalDashboardForEdition?.scheduled],
          datasource: [externalDashboardForEdition?.datasource],
          id: [externalDashboardForEdition?.id],
        });
        this.unsubFormValueChanges();
        this.formValueChangedSub = this.externalDashboardForm.valueChanges.subscribe(() => {
          this.onChange(externalDashboard);
        });
        return externalDashboardForEdition;
      })
    );
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  cancel() {
    this.store.dispatch(navigate({url: 'external-dashboards'}));
  }

  saveChanges(editedExternalDashboard: any) {
    if (this.externalDashboardForm.valid) {
      const action = this.prepareActionWithData((editedExternalDashboard));
      if (action) {
        this.store.dispatch(action);
      }
    }
  }

  prepareActionWithData(editedExternalDashboard: any) {
    if (editedExternalDashboard === null || editedExternalDashboard.id === '') {
      const formData = this.externalDashboardForm.getRawValue() as any;
      const dashboard: ExternalDashboardMetadata = {
        contextKey: formData.dataDomain,
        responsibility: formData.responsibility,
        datasource: formData.datasource,
        dataAnalyst: formData.dataAnalyst,
        department: formData.department,
        businessProcess: formData.businessProcess,
        scheduled: formData.scheduled,
        url: formData.url,
        title: formData.title,
        contextName: ''
      };
      return createExternalDashboard({dashboard});
    } else {
      const formData = this.externalDashboardForm.getRawValue() as any;
      const dashboard: ExternalDashboard = {
        id: editedExternalDashboard.id,
        contextKey: formData.dataDomain,
        responsibility: formData.responsibility,
        datasource: formData.datasource,
        dataAnalyst: formData.dataAnalyst,
        department: formData.department,
        businessProcess: formData.businessProcess,
        scheduled: formData.scheduled,
        url: formData.url,
        title: formData.title,
        contextName: ''
      };
      return updateExternalDashboard({dashboard});
    }
  }

  deleteExternalDashboard(externalDashboard: ExternalDashboard) {
    this.confirmationService.confirm({
      message: this.translateService.translate('@Delete external dashboard question'),
      header: 'Confirm',
      icon: 'fas fa-triangle-exclamation',
      accept: () => {
        this.store.dispatch(clearUnsavedChanges());
        this.store.dispatch(deleteExternalDashboard({dashboard: externalDashboard}));
      }
    });
  }

  ngOnDestroy(): void {
    this.unsubFormValueChanges();
  }

  private onChange(externalDashboard: any) {
    const action = this.prepareActionWithData(externalDashboard);
    if (action) {
      this.store.dispatch(markUnsavedChanges({
        action,
        stayOnPage: externalDashboard === null || externalDashboard.id === undefined
      }));
    }
  }

  private unsubFormValueChanges() {
    if (this.formValueChangedSub) {
      this.formValueChangedSub.unsubscribe();
    }
  }
}
