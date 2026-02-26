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

import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../store/app/app.state";
import {checkAuth} from "../store/auth/auth.action";
import {selectIsAuthenticated} from "../store/auth/auth.selector";
import {Router} from "@angular/router";
import {filter, Subject, takeUntil, timer} from "rxjs";

@Component({
  selector: 'app-callback',
  standalone: true,
  templateUrl: './callback.component.html',
  styleUrls: ['./callback.component.css']
})
export class CallbackComponent implements OnInit, OnDestroy {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  isLoading = true;
  hasError = false;

  private static readonly MAX_RETRIES = 3;
  private static readonly RETRY_COUNT_KEY = 'hd_callback_retry_count';
  private static readonly AUTH_TIMEOUT_MS = 30000;

  ngOnInit(): void {
    this.store.dispatch(checkAuth());

    // When auth succeeds, navigate to /home and clear retry counter
    this.store.select(selectIsAuthenticated).pipe(
      filter(isAuth => isAuth),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.isLoading = false;
      sessionStorage.removeItem(CallbackComponent.RETRY_COUNT_KEY);
      this.router.navigate(['/home']);
    });

    // Safety timeout — if auth doesn't complete, retry with a limit
    timer(CallbackComponent.AUTH_TIMEOUT_MS).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (this.isLoading) {
        console.warn('[Callback] Auth timeout after', CallbackComponent.AUTH_TIMEOUT_MS, 'ms');
        this.retryOrFail();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private retryOrFail(): void {
    const retryCount = Number(sessionStorage.getItem(CallbackComponent.RETRY_COUNT_KEY) || '0');
    if (retryCount >= CallbackComponent.MAX_RETRIES) {
      console.error('[Callback] Max retries reached, navigating to error page');
      sessionStorage.removeItem(CallbackComponent.RETRY_COUNT_KEY);
      this.isLoading = false;
      this.hasError = true;
      this.router.navigate(['/unauthorized']);
      return;
    }
    sessionStorage.setItem(CallbackComponent.RETRY_COUNT_KEY, String(retryCount + 1));
    console.warn('[Callback] Retry', retryCount + 1, 'of', CallbackComponent.MAX_RETRIES);
    // Strip stale OIDC query params (code/state) but preserve the path (including any reverse proxy prefix like /app)
    window.location.href = window.location.origin + window.location.pathname;
  }

}
