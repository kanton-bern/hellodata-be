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

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {TranslocoTestingModule} from '@jsverse/transloco';
import {UserEditToolbarComponent} from './user-edit-toolbar.component';

describe('UserEditToolbarComponent', () => {
  let fixture: ComponentFixture<UserEditToolbarComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        UserEditToolbarComponent,
        TranslocoTestingModule.forRoot({
          langs: {en: {}}, translocoConfig: {availableLangs: ['en'], defaultLang: 'en'}, preloadLangs: true
        })
      ]
    });
    fixture = TestBed.createComponent(UserEditToolbarComponent);
    fixture.componentRef.setInput('user', {id: 'u1', email: 'u1@example.com', enabled: true, superuser: false} as any);
  });

  const auditLogsButton = () => fixture.nativeElement.querySelector('#userAuditLogsButton');

  it('should not show the audit logs button by default', () => {
    fixture.detectChanges();
    expect(auditLogsButton()).toBeNull();
  });

  it('should emit when the audit logs button is clicked', () => {
    fixture.componentRef.setInput('showAuditLogs', true);
    fixture.detectChanges();
    const clicked = jest.fn();
    fixture.componentInstance.auditLogsClicked.subscribe(() => clicked());

    auditLogsButton().querySelector('button').click();

    expect(clicked).toHaveBeenCalledTimes(1);
  });
});
