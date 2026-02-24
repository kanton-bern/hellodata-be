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

import {
  Component,
  ElementRef,
  HostListener,
  inject,
  input,
  OnChanges,
  OnDestroy,
  OnInit,
  output,
  SimpleChanges,
  viewChild
} from '@angular/core';
import {NgStyle} from "@angular/common";

import {AuthService} from "../../services";
import {Subscription} from "rxjs";
import {environment} from "../../../../environments/environment";
import {SafePipe} from '../../pipes/safe.pipe';

@Component({
  selector: 'app-subsystem-iframe[url]',
  templateUrl: './subsystem-iframe.component.html',
  styleUrls: ['./subsystem-iframe.component.scss'],
  imports: [NgStyle, SafePipe]
})
export class SubsystemIframeComponent implements OnInit, OnDestroy, OnChanges {
  url = input.required<string>();
  readonly accessTokenInQueryParam = input(false);
  readonly delay = input(0);
  readonly style = input<{
    [p: string]: any;
  } | null>(null);
  readonly switchStyleOverflow = input(true);
  readonly iframeSetup = output<boolean>();
  frameUrl: string | undefined;
  readonly iframe = viewChild.required<ElementRef<HTMLIFrameElement>>('iframe');
  accessTokenSub!: Subscription;
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    console.debug('on init', this.url(), this.delay());


    this.accessTokenSub = this.authService.accessToken.subscribe({
      next: value => {
        console.debug('access token changed', value)
        console.debug("creating an auth cookie for a domain: ." + environment.baseDomain);
        document.cookie = 'auth.access_token=' + value + '; path=/; domain=.' + environment.baseDomain + '; secure;';
        setTimeout(() => {
          this.frameUrl = this.accessTokenInQueryParam() ? this.url() + '?auth.access_token=' + value : this.url();
          this.iframeSetup.emit(true);
          if (this.switchStyleOverflow()) {
            const mainContentDiv = document.getElementById('mainContentDiv');
            if (mainContentDiv) {
              mainContentDiv.style.overflowX = 'hidden';
              // Keep overflowY as scroll to allow scrolling to the bottom of the iframe
            }
          }
          this.clickScrollTopIfExists();

          // Add listener for iframe load event
          setTimeout(() => this.setupIframeLoadListener(), 100);
        }, this.delay())
      }
    });
  }

  @HostListener('window:resize')
  onResize() {
    this.notifyIframeResize();
  }

  ngOnDestroy() {
    if (this.accessTokenSub) {
      this.accessTokenSub.unsubscribe();
    }
    const mainContentDiv = document.getElementById('mainContentDiv');
    if (this.switchStyleOverflow() && mainContentDiv) {
      mainContentDiv.style.overflow = 'scroll';
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ('url' in changes) {
      if (this.accessTokenSub) {
        this.accessTokenSub.unsubscribe();
      }
      this.accessTokenSub = this.authService.accessToken.subscribe({
        next: () => {
          this.frameUrl = this.url();
          this.iframeSetup.emit(true);
        }
      });
    }
  }

  private clickScrollTopIfExists() {
    setTimeout(() => {
      const elementsByClassNameElement = document.getElementsByClassName('p-scrolltop-sticky')[0];
      if (elementsByClassNameElement) {
        (elementsByClassNameElement as HTMLElement).click();
      }
    }, 500);
  }

  private notifyIframeResize() {
    if (this.iframe && this.iframe()) {
      const iframeElement = this.iframe().nativeElement;
      if (iframeElement?.contentWindow) {
        try {
          // Send resize event to iframe
          const targetOrigin = new URL(this.frameUrl || '').origin;
          iframeElement.contentWindow.postMessage({type: 'resize'}, targetOrigin);
        } catch (e) {
          console.debug('Could not send resize message to iframe', e);
        }
      }
    }
  }

  private setupIframeLoadListener() {
    const iframeElement = this.iframe()?.nativeElement;
    if (iframeElement) {
      iframeElement.addEventListener('load', () => {
        setTimeout(() => this.notifyIframeResize(), 500);
      });
    }
  }

}


