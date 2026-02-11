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

  errorMessage: string | null = null;
  isLoading = true;
  private readonly AUTH_TIMEOUT_MS = 30000; // 30 seconds timeout

  ngOnInit(): void {
    this.startAuth();

    // Listen for auth errors
    this.actions$.pipe(
      ofType(authError),
      takeUntil(this.destroy$)
    ).subscribe(action => {
      console.error('Callback received auth error:', action.error);
      this.isLoading = false;
      this.errorMessage = action.error?.message || 'Authentication failed. Please try again.';
    });

    // Set a timeout in case auth hangs
    timer(this.AUTH_TIMEOUT_MS).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (this.isLoading && !this.errorMessage) {
        console.warn('Auth timeout - checkAuth did not complete within', this.AUTH_TIMEOUT_MS, 'ms');
        this.isLoading = false;
        this.errorMessage = 'Authentication timed out. Please try again.';
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private startAuth(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.store.dispatch(checkAuth());
  }

  retry(): void {
    this.startAuth();
  }

  goToLogin(): void {
    // Clear any stale state and redirect to login page
    window.location.href = '/';
  }
}
