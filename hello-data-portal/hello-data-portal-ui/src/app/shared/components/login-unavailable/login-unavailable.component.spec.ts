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

import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TranslocoTestingModule} from '@jsverse/transloco';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {LoginUnavailableComponent} from './login-unavailable.component';
import {AppInfoService} from '../../services';
import {login} from '../../../store/auth/auth.action';
import {LoginUnavailableInfo} from '../../../store/auth/auth.state';

describe('LoginUnavailableComponent', () => {
  const info: LoginUnavailableInfo = {
    reachability: 'refused',
    loginServiceUrl: 'https://sso.example.com/realms/hd/.well-known/openid-configuration',
    occurredAt: '2026-11-24T15:35:11.094Z'
  };
  let fixture: ComponentFixture<LoginUnavailableComponent>;
  let store: MockStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslocoTestingModule.forRoot({
          langs: {en: {}},
          translocoConfig: {availableLangs: ['en'], defaultLang: 'en'},
          preloadLangs: true
        }),
        LoginUnavailableComponent
      ],
      providers: [
        provideMockStore(),
        {provide: AppInfoService, useValue: {buildInfo: '1.8.0'}}
      ]
    });
    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(LoginUnavailableComponent);
    fixture.componentRef.setInput('info', info);
    fixture.detectChanges();
  });

  it('explains a refused connection', () => {
    expect(fixture.componentInstance.reasonKey()).toBe('@Login service refused');
  });

  it('explains an unreachable login service', () => {
    fixture.componentRef.setInput('info', {...info, reachability: 'unreachable'});

    expect(fixture.componentInstance.reasonKey()).toBe('@Login service unreachable');
  });

  it('links directly to the login service so the user sees the real error page', () => {
    const link: HTMLAnchorElement = fixture.nativeElement.querySelector('a.open-login-service');

    expect(link.href).toBe(info.loginServiceUrl);
    expect(link.target).toBe('_blank');
  });

  it('shows the support information', () => {
    const text = fixture.nativeElement.querySelector('.support-info').textContent;

    expect(text).toContain('login service: ' + info.loginServiceUrl);
    expect(text).toContain('result: refused');
    expect(text).toContain('build: 1.8.0');
  });

  it('retries the login', () => {
    const dispatch = jest.spyOn(store, 'dispatch');

    fixture.componentInstance.retry();

    expect(dispatch).toHaveBeenCalledWith(login());
  });

  it('copies the support information to the clipboard', async () => {
    const writeText = jest.fn((_text: string) => Promise.resolve());
    Object.defineProperty(navigator, 'clipboard', {value: {writeText}, configurable: true});

    await fixture.componentInstance.copySupportInfo();

    expect(writeText).toHaveBeenCalledWith(fixture.componentInstance.supportInfo());
    expect(fixture.componentInstance.copied()).toBe(true);
  });
});
