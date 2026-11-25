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


import {SimpleChange} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {of, Subject} from 'rxjs';
import {Action} from '@ngrx/store';
import {provideMockActions} from '@ngrx/effects/testing';
import {TranslocoTestingModule} from '@jsverse/transloco';
import {UserAuditHistoryComponent} from './user-audit-history.component';
import {UsersManagementService} from '../../../../../store/users-management/users-management.service';
import {UserAuditLog} from '../../../../../store/users-management/users-management.model';
import {updateUserRolesSuccess} from '../../../../../store/users-management/users-management.action';

describe('UserAuditHistoryComponent', () => {
  let component: UserAuditHistoryComponent;
  let fixture: ComponentFixture<UserAuditHistoryComponent>;
  let actions$: Subject<Action>;

  const entry: UserAuditLog = {
    id: '1',
    occurredAt: '2026-01-02T10:00:00+01:00',
    actorId: null,
    actorEmail: null,
    targetUserId: 'u1',
    targetEmail: 'u1@example.com',
    action: 'ROLES_CHANGED',
    source: 'BATCH',
    contextKey: 'ctx',
    contextName: 'Context',
    changes: [
      {field: 'role', contextKey: 'ctx', contextName: 'Context', before: null, after: 'DATA_DOMAIN_VIEWER'},
      {field: 'dashboards', contextKey: null, contextName: null, before: ['a', 'b'], after: []}
    ]
  };

  const mockService = {
    getUserAuditLog: jest.fn((_userId: string, _page: number, _size: number, _contextKey?: string | null) =>
      of({content: [entry], totalElements: 1, totalPages: 1, number: 0, size: 20}))
  };

  beforeEach(() => {
    mockService.getUserAuditLog.mockClear();
    actions$ = new Subject<Action>();
    TestBed.configureTestingModule({
      providers: [{provide: UsersManagementService, useValue: mockService}, provideMockActions(() => actions$)],
      imports: [
        TranslocoTestingModule.forRoot({
          langs: {
            en: {
              '@History source BATCH': 'Batch',
              '@History field role': 'Role',
              '@History field dashboards': 'Dashboards'
            }
          },
          translocoConfig: {availableLangs: ['en'], defaultLang: 'en'},
          preloadLangs: true
        }),
        UserAuditHistoryComponent
      ]
    });
    fixture = TestBed.createComponent(UserAuditHistoryComponent);
    component = fixture.componentInstance;
    component.userId = 'u1';
  });

  it('should load the first page of the audit log', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(mockService.getUserAuditLog).toHaveBeenCalledWith('u1', 0, 20, null);
    expect(component.entries).toEqual([entry]);
    expect(component.totalRecords).toBe(1);
  });

  it('should request the right page on lazy load', async () => {
    component.load({first: 40, rows: 20});
    await Promise.resolve();
    expect(mockService.getUserAuditLog).toHaveBeenCalledWith('u1', 2, 20, null);
  });

  it('should reload the first page after the user was saved', () => {
    fixture.detectChanges();
    component.first = 40;
    mockService.getUserAuditLog.mockClear();

    actions$.next(updateUserRolesSuccess());

    expect(component.first).toBe(0);
    expect(mockService.getUserAuditLog).toHaveBeenCalledWith('u1', 0, 20, null);
  });

  it('should reload the first page of the selected data domain when the filter changes', () => {
    fixture.detectChanges();
    component.first = 20;
    mockService.getUserAuditLog.mockClear();

    component.contextKey = 'ctx';
    component.ngOnChanges({contextKey: new SimpleChange(null, 'ctx', false)});

    expect(component.first).toBe(0);
    expect(mockService.getUserAuditLog).toHaveBeenCalledWith('u1', 0, 20, 'ctx');
  });

  it('should not load twice for the initial filter value', () => {
    component.ngOnChanges({contextKey: new SimpleChange(undefined, null, true)});
    expect(mockService.getUserAuditLog).not.toHaveBeenCalled();
  });

  it('should hide the paginator when there are no entries', async () => {
    mockService.getUserAuditLog.mockReturnValueOnce(of({content: [], totalElements: 0, totalPages: 0, number: 0, size: 20}));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.p-paginator')).toBeNull();
  });

  it('should show the paginator when there are entries', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.p-paginator')).not.toBeNull();
  });

  it('should use the translated source when there is no actor', () => {
    expect(component.who(entry)).toBe('Batch');
    expect(component.who({...entry, actorEmail: 'a@example.com'})).toBe('a@example.com');
  });

  it('should format changes', () => {
    expect(component.formatChange(entry.changes[0])).toBe('Role [Context]: - -> DATA_DOMAIN_VIEWER');
    expect(component.formatChange(entry.changes[1])).toBe('Dashboards: a, b -> -');
  });
});
