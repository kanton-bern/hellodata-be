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
import {authError, checkAuth} from "../store/auth/auth.action";
import {Actions, ofType} from "@ngrx/effects";
import {Subject, takeUntil, timer} from "rxjs";

@Component({
  selector: 'app-callback',
  standalone: true,
  templateUrl: './callback.component.html',
  styleUrls: ['./callback.component.css']
})
export class CallbackComponent implements OnInit, OnDestroy {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly actions$ = inject(Actions);
  private readonly destroy$ = new Subject<void>();

  isLoading = true;
  private readonly AUTH_TIMEOUT_MS = 30000; // 30 seconds timeout

  ngOnInit(): void {
    this.startAuth();

    // On auth error or timeout, reload the page to trigger a fresh auth cycle
    this.actions$.pipe(
      ofType(authError),
      takeUntil(this.destroy$)
    ).subscribe(action => {
      console.error('Callback received auth error, reloading page:', action.error);
      window.location.reload();
    });

    timer(this.AUTH_TIMEOUT_MS).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (this.isLoading) {
        console.warn('Auth timeout - checkAuth did not complete within', this.AUTH_TIMEOUT_MS, 'ms, reloading page');
        window.location.reload();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private startAuth(): void {
    this.isLoading = true;
    this.store.dispatch(checkAuth());
  }

}
