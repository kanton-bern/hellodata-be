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

import {Component, Input} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {Router} from '@angular/router';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {TranslocoTestingModule} from '@jsverse/transloco';
import {UserAuditLogsComponent} from './user-audit-logs.component';
import {UserAuditHistoryComponent} from '../user-edit/user-audit-history/user-audit-history.component';
import {selectAllDataDomains, selectEditedUser} from '../../../../store/users-management/users-management.selector';
import {loadAvailableContexts, loadUserById} from '../../../../store/users-management/users-management.action';
import {createBreadcrumbs} from '../../../../store/breadcrumb/breadcrumb.action';

@Component({selector: 'app-user-audit-history', template: ''})
class UserAuditHistoryStubComponent {
  @Input() userId!: string;
  @Input() contextKey: string | null = null;
}

describe('UserAuditLogsComponent', () => {
  let fixture: ComponentFixture<UserAuditLogsComponent>;
  let component: UserAuditLogsComponent;
  let store: MockStore;
  const router = {navigate: jest.fn()};
  const user = {id: 'u1', email: 'u1@example.com'} as any;

  beforeEach(() => {
    router.navigate.mockClear();
    TestBed.configureTestingModule({
      imports: [
        UserAuditLogsComponent,
        TranslocoTestingModule.forRoot({
          langs: {en: {'@Audit logs': 'Audit logs', '@Back': 'Back'}},
          translocoConfig: {availableLangs: ['en'], defaultLang: 'en'},
          preloadLangs: true
        })
      ],
      providers: [
        provideMockStore({
          selectors: [
            {selector: selectEditedUser, value: user},
            {selector: selectAllDataDomains, value: [{contextKey: 'dd1', name: 'Data Domain 1'}]}
          ]
        }),
        {provide: Router, useValue: router}
      ]
    });
    TestBed.overrideComponent(UserAuditLogsComponent, {
      remove: {imports: [UserAuditHistoryComponent]},
      add: {imports: [UserAuditHistoryStubComponent]}
    });
    store = TestBed.inject(MockStore);
    jest.spyOn(store, 'dispatch');
    fixture = TestBed.createComponent(UserAuditLogsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load the user and the data domains', () => {
    expect(store.dispatch).toHaveBeenCalledWith(loadUserById());
    expect(store.dispatch).toHaveBeenCalledWith(loadAvailableContexts());
  });

  it('should add Audit logs after the user e-mail in the breadcrumb', () => {
    expect(store.dispatch).toHaveBeenCalledWith(createBreadcrumbs({
      breadcrumbs: [
        {label: '@User management', routerLink: 'user-management'},
        {label: 'u1@example.com', routerLink: 'user-management/edit/u1'},
        {label: '@Audit logs'}
      ]
    }));
  });

  it('should pass the selected data domain to the table', () => {
    const table = () => fixture.debugElement.children[0].query(el => el.componentInstance instanceof UserAuditHistoryStubComponent)
      .componentInstance as UserAuditHistoryStubComponent;
    expect(table().userId).toBe('u1');
    expect(table().contextKey).toBeNull();

    component.selectedContextKey = 'dd1';
    fixture.detectChanges();
    expect(table().contextKey).toBe('dd1');
  });

  it('should go back to the user edit page', () => {
    component.back(user);
    expect(router.navigate).toHaveBeenCalledWith(['user-management', 'edit', 'u1']);
  });
});
