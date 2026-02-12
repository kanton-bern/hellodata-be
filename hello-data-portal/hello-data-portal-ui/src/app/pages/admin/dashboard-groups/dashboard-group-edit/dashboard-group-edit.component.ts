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

import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Observable, Subscription, tap} from 'rxjs';
import {FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '../../../../store/app/app.state';
import {selectEditedDashboardGroup} from '../../../../store/dashboard-groups/dashboard-groups.selector';
import {
  DashboardGroup,
  DashboardGroupCreateUpdate,
  DashboardGroupEntry
} from '../../../../store/dashboard-groups/dashboard-groups.model';
import {naviElements} from '../../../../app-navi-elements';
import {markUnsavedChanges} from '../../../../store/unsaved-changes/unsaved-changes.actions';
import {BaseComponent} from '../../../../shared/components/base/base.component';
import {
  deleteDashboardGroup,
  saveChangesToDashboardGroup,
  showDeleteDashboardGroupPopup
} from '../../../../store/dashboard-groups/dashboard-groups.action';
import {navigate} from '../../../../store/app/app.action';
import {createBreadcrumbs} from '../../../../store/breadcrumb/breadcrumb.action';
import {AsyncPipe, DatePipe} from '@angular/common';
import {Button} from 'primeng/button';
import {Toolbar} from 'primeng/toolbar';
import {Tooltip} from 'primeng/tooltip';
import {InputText} from 'primeng/inputtext';
import {TranslocoPipe} from '@jsverse/transloco';
import {Ripple} from 'primeng/ripple';
import {
  DeleteDashboardGroupPopupComponent
} from '../delete-dashboard-group-popup/delete-dashboard-group-popup.component';
import {TableModule} from 'primeng/table';

@Component({
  selector: 'app-dashboard-group-edit',
  templateUrl: './dashboard-group-edit.component.html',
  styleUrls: ['./dashboard-group-edit.component.scss'],
  imports: [FormsModule, ReactiveFormsModule, Button, Toolbar, Tooltip, InputText, TableModule,
    DeleteDashboardGroupPopupComponent, AsyncPipe, DatePipe, TranslocoPipe, Ripple]
})
export class DashboardGroupEditComponent extends BaseComponent implements OnInit, OnDestroy {
  editedDashboardGroup$: Observable<any>;
  dashboardGroupForm!: FormGroup;
  formValueChangedSub!: Subscription;
  private readonly store = inject<Store<AppState>>(Store);
  private readonly fb = inject(FormBuilder);

  constructor() {
    super();
    this.editedDashboardGroup$ = this.store.select(selectEditedDashboardGroup);
  }

  get entriesFormArray(): FormArray {
    return this.dashboardGroupForm.get('entries') as FormArray;
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.editedDashboardGroup$ = this.store.select(selectEditedDashboardGroup).pipe(
      tap((dashboardGroup) => {
        if (dashboardGroup) {
          this.initForm(dashboardGroup);
          if (dashboardGroup.id) {
            this.createEditBreadcrumbs();
          } else {
            this.createCreateBreadcrumbs();
          }
        }
      })
    );
  }

  navigateToList() {
    this.store.dispatch(navigate({url: naviElements.dashboardGroups.path}));
  }

  saveDashboardGroup(editedDashboardGroup: DashboardGroup) {
    const formValue = this.dashboardGroupForm.getRawValue();
    const dashboardGroup: DashboardGroupCreateUpdate = {
      id: editedDashboardGroup.id,
      name: formValue.name,
      entries: formValue.entries
    };
    this.store.dispatch(saveChangesToDashboardGroup({dashboardGroup}));
  }

  openDeletePopup(editedDashboardGroup: DashboardGroup) {
    this.store.dispatch(showDeleteDashboardGroupPopup({dashboardGroup: editedDashboardGroup}));
  }

  getDeletionAction() {
    return deleteDashboardGroup();
  }

  addEntry() {
    this.entriesFormArray.push(this.createEntryFormGroup({contextKey: '', dashboardId: 0, dashboardTitle: ''}));
  }

  removeEntry(index: number) {
    this.entriesFormArray.removeAt(index);
  }

  ngOnDestroy(): void {
    this.unsubFormValueChanges();
  }

  private initForm(dashboardGroup: DashboardGroup) {
    const entryFormGroups = (dashboardGroup.entries || []).map(entry => this.createEntryFormGroup(entry));
    this.dashboardGroupForm = this.fb.group({
      name: [dashboardGroup.name || '', [Validators.required, Validators.minLength(3), Validators.maxLength(150), Validators.pattern(/^[\p{L}\p{N}].*/u)]],
      entries: this.fb.array(entryFormGroups)
    });
    this.unsubFormValueChanges();
    this.formValueChangedSub = this.dashboardGroupForm.valueChanges.subscribe(() => {
      this.onChange(dashboardGroup);
    });
  }

  private createEntryFormGroup(entry: DashboardGroupEntry): FormGroup {
    return this.fb.group({
      contextKey: [entry.contextKey, Validators.required],
      dashboardId: [entry.dashboardId, [Validators.required, Validators.min(1)]],
      dashboardTitle: [entry.dashboardTitle, Validators.required]
    });
  }

  private onChange(editedDashboardGroup: DashboardGroup) {
    const formValue = this.dashboardGroupForm.getRawValue();
    const dashboardGroup: DashboardGroupCreateUpdate = {
      id: editedDashboardGroup.id,
      name: formValue.name,
      entries: formValue.entries
    };
    this.store.dispatch(markUnsavedChanges({
      action: saveChangesToDashboardGroup({dashboardGroup}),
      stayOnPage: editedDashboardGroup.id === undefined
    }));
  }

  private createCreateBreadcrumbs() {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.dashboardGroups.label,
          routerLink: naviElements.dashboardGroups.path,
        },
        {
          label: naviElements.dashboardGroupCreate.label,
        }
      ]
    }));
  }

  private createEditBreadcrumbs() {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.dashboardGroups.label,
          routerLink: naviElements.dashboardGroups.path,
        },
        {
          label: naviElements.dashboardGroupEdit.label,
        }
      ]
    }));
  }

  private unsubFormValueChanges() {
    if (this.formValueChangedSub) {
      this.formValueChangedSub.unsubscribe();
    }
  }
}
