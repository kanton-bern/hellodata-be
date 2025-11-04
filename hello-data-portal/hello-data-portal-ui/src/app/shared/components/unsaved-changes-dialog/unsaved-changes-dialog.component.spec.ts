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
import {UnsavedChangesDialogComponent} from './unsaved-changes-dialog.component';
import {Store} from '@ngrx/store';
import {of} from 'rxjs';
import {afterEach, beforeEach, describe, expect, it, jest} from "@jest/globals";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {ConfirmationService} from "primeng/api";

describe('UnsavedChangesDialogComponent', () => {
  let component: UnsavedChangesDialogComponent;
  let fixture: ComponentFixture<UnsavedChangesDialogComponent>;

  // Mock Store
  const mockStore = {
    select: jest.fn(),
  };

  // Sample data for stayOnPage$
  const stayOnPageContainer = {
    value: true,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ConfirmDialogModule, UnsavedChangesDialogComponent],
      providers: [{provide: Store, useValue: mockStore}, ConfirmationService],
    });

    fixture = TestBed.createComponent(UnsavedChangesDialogComponent);
    component = fixture.componentInstance;

    // Mock store.select call to return sample data
    jest.spyOn(mockStore, 'select').mockReturnValueOnce(of(stayOnPageContainer));

    fixture.detectChanges();
  });

  it('should create the UnsavedChangesDialogComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should set stayOnPage$ from store', (done) => {
    component.stayOnPage$.subscribe((stayOnPageResult) => {
      expect(stayOnPageResult).toEqual(stayOnPageContainer);
      done(); // Notify Jest that the asynchronous test is complete
    });
  });

  afterEach(() => {
    // Clean up
    fixture.destroy();
  });
});
