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

import {Injectable} from "@angular/core";
import {TranslateService} from "./translate.service";
import {MessageService} from "primeng/api";
import {TitleCasePipe} from "@angular/common";

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor(private translateService: TranslateService, private messageService: MessageService,
              private titlecasePipe:TitleCasePipe) {
  }

  public success(message: string, interpolateParams?: Record<string, unknown>): void {
    console.debug('Success message:', message);
    const translated = this.translateService.translate(message, interpolateParams);
    this.showNotification(translated, 'success');
  }

  public info(message: string, interpolateParams?: Record<string, unknown>): void {
    console.debug('Info message:', message);
    const translated = this.translateService.translate(message, interpolateParams);
    this.showNotification(translated, 'info');
  }

  public warn(message: string, interpolateParams?: Record<string, unknown>): void {
    console.warn(message);
    const translated = this.translateService.translate(message, interpolateParams);
    this.showNotification(translated, 'warn');
  }

  public error(message: string, interpolateParams?: Record<string, unknown>): void {
    const translated = message.startsWith('@') ? this.translateService.translate(message, interpolateParams) : message;
    this.showNotification(translated, 'error');
  }

  private showNotification(message: string, type: string): void {
    this.messageService.add({severity: type, summary: this.titlecasePipe.transform(type), detail: message});
  }

}
