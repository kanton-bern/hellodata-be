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
import {Action, Store} from '@ngrx/store';
import {of} from 'rxjs';
import {AnnouncementsManagementComponent} from './announcements-management.component';
import {AppState} from '../../../store/app/app.state';
import {naviElements} from '../../../app-navi-elements';
import {Announcement} from '../../../store/announcement/announcement.model';
import {beforeEach, describe, expect, it, jest} from "@jest/globals";
import {TranslocoTestingModule} from "@jsverse/transloco";
import {
  deleteAnnouncement,
  loadAllAnnouncements,
  openAnnouncementEdition,
  showDeleteAnnouncementPopup
} from "../../../store/announcement/announcement.action";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {ConfirmationService} from "primeng/api";
import {HttpClientTestingModule} from "@angular/common/http/testing";

describe('AnnouncementsManagementComponent', () => {
  let component: AnnouncementsManagementComponent;
  let fixture: ComponentFixture<AnnouncementsManagementComponent>;
  let store: Store<AppState>;

  const mockStore = {
    select: jest.fn(),
    dispatch: jest.fn(),
    pipe: jest.fn(),
  };

  const mockAnnouncement: Announcement = {
    id: '1',
    published: true,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        AnnouncementsManagementComponent,
        HttpClientTestingModule,
        TranslocoTestingModule.forRoot({
          langs: {en: {}},
          translocoConfig: {
            availableLangs: ['en'],
            defaultLang: 'en',
          },
          preloadLangs: true,
        }),],
      providers: [{provide: Store, useValue: mockStore}, ConfirmationService],
    });

    fixture = TestBed.createComponent(AnnouncementsManagementComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(Store);

    mockStore.pipe.mockReturnValue(of([mockAnnouncement])); // Mock the pipe method to return an Observable with mock data
    mockStore.select.mockReturnValue(of([mockAnnouncement])); // Mock the select method to return an Observable with mock data

    fixture.detectChanges();
  });

  it('should create the announcementsManagementComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch loadAllAnnouncements and createBreadcrumbs actions on initialization', () => {
    expect(mockStore.dispatch).toHaveBeenCalledWith(loadAllAnnouncements());
    expect(mockStore.dispatch).toHaveBeenCalledWith(
      createBreadcrumbs({
        breadcrumbs: [
          {
            label: naviElements.announcementsManagement.label,
            routerLink: naviElements.announcementsManagement.path,
          },
        ]
      })
    );
  });

  it('should dispatch openAnnouncementEdition when createAnnouncement is called', () => {
    component.createAnnouncement();
    expect(mockStore.dispatch).toHaveBeenCalledWith(openAnnouncementEdition({announcement: {}}));
  });

  it('should dispatch openAnnouncementEdition with data when editAnnouncement is called', () => {
    const mockData: Announcement = {
      id: '2',
      published: false,
    };
    component.editAnnouncement(mockData);
    expect(mockStore.dispatch).toHaveBeenCalledWith(openAnnouncementEdition({announcement: mockData}));
  });

  it('should dispatch showDeleteAnnouncementPopup with data when showAnnouncementDeletionPopup is called', () => {
    component.showAnnouncementDeletionPopup(mockAnnouncement);
    expect(mockStore.dispatch).toHaveBeenCalledWith(showDeleteAnnouncementPopup({announcement: mockAnnouncement}));
  });

  it('should return a deleteAnnouncement action from getDeletionAction', () => {
    const action: Action = component.getDeletionAction();
    expect(action).toEqual(deleteAnnouncement());
  });

});
