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

import {Injectable, OnDestroy} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {ActuatorInfo} from "./interfaces/actuator-info";
import {OidcSecurityService} from "angular-auth-oidc-client";
import {Subscription} from 'rxjs';
import {environment} from "../../../environments/environment";

@Injectable()
export class AppInfoService implements OnDestroy {

  readonly userData$: Subscription;
  private baseUrl = `${environment.portalApi}`;
  private _timestamp!: string;
  private _branch!: string;
  private _version!: string;
  private _tag!: string;
  private _gitHash!: string;

  constructor(private http: HttpClient, private oidcSecurityService: OidcSecurityService) {
    this.userData$ = this.oidcSecurityService.userData$.subscribe(u => {
      this.http.get<ActuatorInfo>(this.baseUrl + `/actuator/info`).subscribe(actuatorInfo => {
        this._version = actuatorInfo.build.version;
        this._branch = actuatorInfo.git.branch;
        this._timestamp = actuatorInfo.git.commit.time;
        this._gitHash = actuatorInfo.git.commit.id;
        this._tag = actuatorInfo.git.tags;
      });
    });
  }

  public get buildInfo() {
    if (this._tag) {
      return this._tag
    }
    return `${this._branch} - ${this.getTime()} - ${this._gitHash}`
  }

  public get title() {
    return environment.appTitle ? environment.appTitle : 'HelloDATA | Portal';
  }

  public get currentYear() {
    return new Date().getFullYear();
  }

  ngOnDestroy(): void {
    if (this.userData$) {
      this.userData$.unsubscribe();
    }
  }

  getTime() {
    if (this._timestamp) {
      return this._timestamp.replace('T', ' ').replace('Z', '');
    }
    return '';
  }
}
