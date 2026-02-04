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

// unsaved-changes.guard.ts
import {inject, Injectable} from '@angular/core';
import {combineLatest, Observable, of} from 'rxjs';
import {Store} from '@ngrx/store';
import {switchMap, take} from 'rxjs/operators';
import {selectHasUnsavedChanges} from "../../store/unsaved-changes/unsaved-changes.selector";
import {ConfirmationService} from "primeng/api";
import {AppState} from "../../store/app/app.state";
import {ActivatedRouteSnapshot, CanDeactivateFn, RouterStateSnapshot} from "@angular/router";
import {clearUnsavedChanges, runSaveAction} from "../../store/unsaved-changes/unsaved-changes.actions";
import {TranslateService} from "../services/translate.service";

@Injectable({
  providedIn: 'root',
})
export class UnsavedChangesGuard {
  private readonly confirmationService = inject(ConfirmationService);
  private readonly store = inject<Store<AppState>>(Store);
  private readonly translateService = inject(TranslateService);

  canDeactivate(
    _component: any,
    _activatedRouteSnapshot: ActivatedRouteSnapshot,
    _currentState: RouterStateSnapshot,
    _nextState: RouterStateSnapshot
  ): Observable<boolean> {
    return combineLatest([
      this.store.select(selectHasUnsavedChanges),
      this.translateService.selectTranslate('@Unsaved changes message')
    ]).pipe(
      take(1),
      switchMap(([hasUnsavedChanges, msg]) => {
        if (hasUnsavedChanges) {
          return this.showConfirmationDialog(msg);
        }
        return of(true); // Allow navigation if no unsaved changes
      })
    )
  }

  private showConfirmationDialog(message: string): Observable<boolean> {
    return new Observable<boolean>(observer => {
      // Schedule the confirmation dialog after the current cycle
      setTimeout(() => {
        this.confirmationService.confirm({
          key: 'unsavedChangesConfirmation',
          message: message,
          icon: 'fas fa-triangle-exclamation',
          accept: () => this.handleAccept(observer),
          reject: () => this.handleReject(observer),
        });
      }, 0);
    });
  }

  private handleAccept(observer: any): void {
    this.store.dispatch(runSaveAction());
    this.store.dispatch(clearUnsavedChanges());
    // Allow navigation to proceed
    observer.next(true);
    observer.complete();
  }

  private handleReject(observer: any): void {
    this.store.dispatch(clearUnsavedChanges());
    // Allow navigation to proceed
    observer.next(true);
    observer.complete();
  }
}

export const unsavedChangesGuard: CanDeactivateFn<any> = (component: any, currentRoute: ActivatedRouteSnapshot, currentState: RouterStateSnapshot, nextState: RouterStateSnapshot): Observable<boolean> => {
  return inject(UnsavedChangesGuard).canDeactivate(component, currentRoute, currentState, nextState);
}
