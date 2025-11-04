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
import {ReactiveFormsModule} from '@angular/forms';
import {Store} from '@ngrx/store';
import {of} from 'rxjs';
import {AnnouncementEditComponent} from './announcement-edit.component';
import {AppState} from '../../../../store/app/app.state';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {TestModule} from "../../../../test.module";
import {
  deleteEditedAnnouncement,
  showDeleteAnnouncementPopup
} from "../../../../store/announcement/announcement.action";
import {ConfirmationService} from "primeng/api";
import {TranslocoTestingModule} from "@jsverse/transloco";
import {HttpClientTestingModule} from "@angular/common/http/testing";

describe('AnnouncementEditComponent', () => {
  let component: AnnouncementEditComponent;
  let fixture: ComponentFixture<AnnouncementEditComponent>;
  let store: Store<AppState>;

  const mockStore = {
    select: jest.fn(),
    dispatch: jest.fn(),
    pipe: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        AnnouncementEditComponent,
        HttpClientTestingModule,
        TranslocoTestingModule.forRoot({
          langs: {en: {}},
          translocoConfig: {
            availableLangs: ['en'],
            defaultLang: 'en',
          },
          preloadLangs: true,
        }),
      ],
      providers: [
        {provide: Store, useValue: mockStore},
        TestModule,
        ConfirmationService,
      ],
    });

    fixture = TestBed.createComponent(AnnouncementEditComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(Store);

    mockStore.select.mockReturnValue(of({}));
  });

  it('should create the announcementEditComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch showDeleteAnnouncementPopup action on openDeletePopup', () => {
    const mockAnnouncement = {id: '1', message: 'Test Message', published: true};

    component.openDeletePopup(mockAnnouncement);

    expect(store.dispatch).toHaveBeenCalledWith(showDeleteAnnouncementPopup({announcement: mockAnnouncement}));
  });

  it('should return deleteEditedAnnouncement action from getDeletionAction', () => {
    const deleteAction = component.getDeletionAction();

    expect(deleteAction).toEqual(deleteEditedAnnouncement());
  });

});
