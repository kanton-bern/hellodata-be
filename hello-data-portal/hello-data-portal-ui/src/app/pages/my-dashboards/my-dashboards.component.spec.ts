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

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MyDashboardsComponent} from './my-dashboards.component';
import {Store} from '@ngrx/store';
import {of} from 'rxjs';
import {SupersetDashboard} from '../../store/my-dashboards/my-dashboards.model';
import {naviElements} from '../../app-navi-elements';
import {ActivatedRoute} from '@angular/router';
import {MenuService} from '../../store/menu/menu.service';
import {AppState} from '../../store/app/app.state';
import {beforeEach, describe, expect, it, jest} from "@jest/globals";
import {SupersetDashboardWithMetadata} from "../../store/start-page/start-page.model";
import {TranslocoTestingModule} from "@jsverse/transloco";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {updateDashboardMetadata} from "../../store/start-page/start-page.action";

describe('MyDashboardsComponent', () => {
  let component: MyDashboardsComponent;
  let fixture: ComponentFixture<MyDashboardsComponent>;
  let store: Store<AppState>;

  const mockStore = {
    dispatch: jest.fn(),
    pipe: jest.fn(),
    select: jest.fn(),
  };

  const mockActivatedRoute = {
    snapshot: {data: {mockData: 'yourMockData'}},
  };

  const mockMenuService = {
    createDashboardLink: jest.fn(),
  };

  const mockDashboards: SupersetDashboard[] = [];

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MyDashboardsComponent],
      providers: [
        {provide: Store, useValue: mockStore},
        {provide: ActivatedRoute, useValue: mockActivatedRoute},
        {provide: MenuService, useValue: mockMenuService},
      ],
      imports: [
        TranslocoTestingModule
      ],
    });

    fixture = TestBed.createComponent(MyDashboardsComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(Store);

    mockStore.select.mockReturnValue(of(mockDashboards)); // Mock the select method to return an Observable with mock data

    fixture.detectChanges();
  });

  it('should create the MyDashboardsComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch createBreadcrumbs on initialization', () => {
    expect(mockStore.dispatch).toHaveBeenCalledWith(
      createBreadcrumbs({
        breadcrumbs: [
          {
            label: naviElements.myDashboards.label,
            routerLink: naviElements.myDashboards.path,
          },
        ]
      })
    );
  });

  it('should dispatch UpdateDashboardMetadata when updateDashboard is called', () => {
    const mockDashboard: SupersetDashboardWithMetadata = {
      id: 1,
      contextName: 'test',
      contextKey: 'test',
      contextId: '1',
      published: true,
      businessProcess: 'test',
      currentUserViewer: true,
      currentUserAdmin: true,
      currentUserEditor: true,
      dashboardTitle: 'test',
      status: 'test',
      department: 'test',
      responsibility: 'test',
      dataAnalyst: 'test',
      scheduled: 'test',
      datasource: 'test',
      modified: 1,
      instanceUrl: 'test',
      thumbnailPath: 'test',
      dashboardUrlPath: 'test',
      instanceName: 'test',
      roles: [],
      slug: 'test',
    };

    component.updateDashboard(mockDashboard);

    expect(mockStore.dispatch).toHaveBeenCalledWith(updateDashboardMetadata({dashboard: mockDashboard}));
  });

});
