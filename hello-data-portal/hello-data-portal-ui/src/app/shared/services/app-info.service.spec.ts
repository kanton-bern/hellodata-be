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

import {TestBed} from '@angular/core/testing';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {OidcSecurityService, UserDataResult} from 'angular-auth-oidc-client';
import {of} from 'rxjs';
import {AppInfoService} from './app-info.service';
import {ActuatorInfo} from './interfaces/actuator-info';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {AuthConfigModule} from "../../auth/auth-config.module";

describe('AppInfoService', () => {
  let appInfoService: AppInfoService;
  let oidcSecurityService: OidcSecurityService;
  let httpMock: HttpTestingController;

  const actuatorInfoMock: ActuatorInfo = {
    git: {
      branch: 'main',
      commit: {
        id: 'abc123',
        time: '2023-10-04T12:34:56Z',
      },
      tags: 'v1.0',
    },
    build: {
      artifact: 'app',
      name: 'My App',
      time: '2023-10-04T12:00:00Z',
      version: '1.0.0',
      group: 'com.example',
    },
  };

  const userDataResult: UserDataResult = {
    userData: {},
    allUserData: [{
      configId: 'configId',
      userData: {
        preferred_username: 'username',
        name: 'name',
        email: 'email',
        roles: ['role1', 'role2']
      }
    }]
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, AuthConfigModule],
      providers: [AppInfoService],
    });
    appInfoService = TestBed.inject(AppInfoService);
    oidcSecurityService = TestBed.inject(OidcSecurityService);

    httpMock = TestBed.inject(HttpTestingController);

    jest.spyOn(oidcSecurityService, 'userData$', 'get').mockReturnValue(of(userDataResult));
  });

  it('should be created', () => {
    expect(appInfoService).toBeTruthy();
  });

  it('should return branch, timestamp, and git hash if tag is not available', () => {
    jest.spyOn(appInfoService['http'], 'get').mockReturnValue(of(actuatorInfoMock));
    appInfoService['http'].get<ActuatorInfo>('/actuator/info').subscribe(() => {
      const expectedInfo = 'main - 2023-10-04 12:34:56 - abc123';
      expect(appInfoService.buildInfo).toBe(expectedInfo);
    });
  });

  it('should return empty string for getTime() if timestamp is not available', () => {
    appInfoService['_timestamp'] = '';
    expect(appInfoService.getTime()).toBe('');
  });

  it('should return the title', () => {
    expect(appInfoService.title).toBe('HelloDATA BE | Portal');
  });

  it('should return the current year', () => {
    const currentYear = new Date().getFullYear();
    expect(appInfoService.currentYear).toBe(currentYear);
  });

  it('should unsubscribe from userData$ when ngOnDestroy is called', () => {
    jest.spyOn(appInfoService.userData$, 'unsubscribe');
    appInfoService.ngOnDestroy();
    expect(appInfoService.userData$.unsubscribe).toHaveBeenCalled();
  });
});
