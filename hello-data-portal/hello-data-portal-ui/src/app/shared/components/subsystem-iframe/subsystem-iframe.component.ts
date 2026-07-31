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
import {catchError, distinctUntilChanged, map, Observable, of, skip, Subscription, switchMap} from "rxjs";
import {environment} from "../../../../environments/environment";
import {SafePipe} from '../../pipes/safe.pipe';
import {TranslocoService} from '@jsverse/transloco';

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
  // Visually crop N px off the LEFT of the embedded page by widening the iframe and shifting it
  // left inside the overflow-hidden wrapper. Used to hide the Airflow 3 native left sidebar in
  // the portal iframe (the iframe is cross-origin, so its DOM can't be touched directly).
  readonly cropLeftPx = input(0);
  // Force an OIDC refresh-token grant before the first load so the auth.access_token cookie carries
  // the user's CURRENT Keycloak claims/roles. Needed for the Airflow 3 embed: it re-syncs FAB roles
  // from that cookie on every open (logout-first), but a not-yet-renewed cookie still holds the old
  // roles, so a role change wouldn't show until the portal's next silent renew without this.
  readonly forceTokenRefreshBeforeLoad = input(false);
  // Optional one-shot step to run BEFORE the token refresh + first load (e.g. the Airflow 3 embed
  // synchronously reconciles the user's Keycloak roles so the subsequent force-refresh already
  // carries the latest roles). Errors are swallowed so the iframe still loads.
  readonly beforeLoad = input<Observable<unknown> | null>(null);
  // Reload the iframe when the portal language changes so the embedded subsystem re-reads it. Used
  // by the Airflow 3 embed: its index.html bootstrap maps the hd_lang cookie -> i18next locale only
  // at load time, so a running iframe must be re-navigated to pick up a new language.
  readonly reloadOnLanguageChange = input(false);
  readonly iframeSetup = output<boolean>();
  frameUrl: string | undefined;
  readonly iframe = viewChild.required<ElementRef<HTMLIFrameElement>>('iframe');
  accessTokenSub!: Subscription;
  private langSub?: Subscription;
  private readonly authService = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  ngOnInit(): void {
    console.debug('on init', this.url(), this.delay());

    // Reload the iframe on portal language change (Airflow 3 only) so it re-reads the hd_lang cookie.
    // distinctUntilChanged is essential: the portal re-applies the selected language on every profile
    // refresh (~30s) via setActiveLang, and Transloco's langChanges$ emits even when the value is
    // unchanged — without it the iframe would reload every ~30s. skip(1) ignores the initial emission;
    // TranslateService.setActiveLang writes the cookie synchronously before this fires.
    if (this.reloadOnLanguageChange()) {
      this.langSub = this.transloco.langChanges$
        .pipe(distinctUntilChanged(), skip(1))
        .subscribe(() => this.reloadIframe());
    }

    this.load();
  }

  // Initial load through the full prepare chain: optional beforeLoad step (e.g. the Airflow 3 role
  // reconcile) -> optional forced token refresh (so the auth.access_token cookie carries the user's
  // CURRENT roles) -> read the current access token -> navigate the iframe. This is where role
  // propagation happens (on open); language-change reloads use the lighter reloadIframe(). Each step
  // is best-effort and, crucially, a beforeLoad failure must NOT skip the refresh (its own
  // catchError keeps the chain going).
  private load(): void {
    if (this.accessTokenSub) {
      this.accessTokenSub.unsubscribe();
    }
    const prepare$ = (this.beforeLoad() ?? of(null)).pipe(catchError(() => of(null)));
    // Use the access token straight from the forceRefreshSession RESPONSE (the just-minted one).
    // Re-reading getAccessToken() after the refresh raced the lib's storage update and often
    // returned the PRE-refresh token, so the cookie carried stale roles. null => fall back below.
    const refresh$: Observable<string | null> = this.forceTokenRefreshBeforeLoad()
      ? this.authService.forceRefreshSession().pipe(
          map((r: any) => (r && r.accessToken) ? r.accessToken as string : null),
          catchError(err => {
            console.error('Airflow embed: forceRefreshSession failed, using existing token', err);
            return of(null);
          })
        )
      : of(null);

    this.accessTokenSub = prepare$.pipe(
      switchMap(() => refresh$),
      switchMap((refreshed: string | null) => refreshed ? of(refreshed) : this.authService.accessToken)
    ).subscribe({
      next: value => {
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

  // Lightweight reload for a portal language change: drop the iframe (@if frameUrl) and re-add it
  // next tick with the SAME url so it re-navigates and re-reads the hd_lang cookie. Deliberately
  // does NOT go through load()/forceRefreshSession: a language change doesn't change roles, and the
  // oidc refresh resolves outside Angular's zone, which left the re-created iframe blank until a
  // change-detection tick (e.g. opening devtools). The re-auth still re-syncs roles from the current
  // cookie, which the open-time force-refresh already made current.
  private reloadIframe() {
    const current = this.frameUrl;
    if (!current) {
      return;
    }
    this.frameUrl = undefined;
    setTimeout(() => {
      this.frameUrl = current;
      setTimeout(() => this.setupIframeLoadListener(), 100);
    });
  }

  ngOnDestroy() {
    if (this.accessTokenSub) {
      this.accessTokenSub.unsubscribe();
    }
    if (this.langSub) {
      this.langSub.unsubscribe();
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


