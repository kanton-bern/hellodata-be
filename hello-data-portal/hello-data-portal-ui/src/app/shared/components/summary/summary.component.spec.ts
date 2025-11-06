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
import {SummaryComponent} from './summary.component';
import {Store} from '@ngrx/store';
import {of} from 'rxjs';
import {afterEach, beforeEach, describe, expect, it, jest} from "@jest/globals";
import {TranslocoTestingModule} from "@jsverse/transloco";
import {Pipeline} from "../../../store/summary/summary.model";
import {AppInfoService} from "../../services";
import {HttpClientTestingModule} from "@angular/common/http/testing";

class MockAppInfoService {
  getAppInfo() {
    return {};
  }
}

describe('SummaryComponent', () => {
  let component: SummaryComponent;
  let fixture: ComponentFixture<SummaryComponent>;

  // Mock Store
  const mockStore = {
    pipe: jest.fn(),
    dispatch: jest.fn(),
    select: jest.fn(),
  };

  // Sample data for observables
  const documentation = 'Sample Documentation';
  const currentUserPermissions = ['permission1', 'permission2'];
  const pipelines: Pipeline[] = [
    {
      id: '1',
      description: 'Pipeline 1',
      contextKey: 'contextKey1',
      lastInstance: {state: 'success', startDate: 0, endDate: 1}
    },
    {
      id: '2',
      description: 'Pipeline 2',
      contextKey: 'contextKey2',
      lastInstance: {state: 'failed', startDate: 0, endDate: 1}
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslocoTestingModule,
        SummaryComponent,
        TranslocoTestingModule.forRoot({
          langs: {en: {}},
          translocoConfig: {
            availableLangs: ['en'],
            defaultLang: 'en',
          },
          preloadLangs: true,
        }),
        HttpClientTestingModule
      ],
      providers: [
        {provide: Store, useValue: mockStore},
        {provide: AppInfoService, useClass: MockAppInfoService},
      ],
    });

    fixture = TestBed.createComponent(SummaryComponent);
    component = fixture.componentInstance;

    // Mock store.pipe and store.select calls to return sample data
    jest.spyOn(mockStore, 'select').mockReturnValueOnce(of(documentation));
    jest.spyOn(mockStore, 'select').mockReturnValueOnce(of(currentUserPermissions));
    jest.spyOn(mockStore, 'select').mockReturnValueOnce(of(pipelines));

    fixture.detectChanges();
  });

  it('should create the SummaryComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should set documentation$, currentUserPermissions$, and pipelines$ from store', (done) => {
    component.documentation$.subscribe((documentationResult) => {
      expect(documentationResult).toBe(documentation);

      component.currentUserPermissions$.subscribe((currentUserPermissionsResult) => {
        expect(currentUserPermissionsResult).toEqual(currentUserPermissions);

        component.pipelines$.subscribe((pipelinesResult) => {
          expect(pipelinesResult).toEqual(pipelines);
          done(); // Notify Jest that the asynchronous test is complete
        });
      });
    });
  });


  afterEach(() => {
    // Clean up
    fixture.destroy();
  });
});
