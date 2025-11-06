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

import {inject, Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot} from '@angular/router';
import {asyncScheduler, Observable, scheduled} from 'rxjs';
import {filter, map, switchMap, take} from 'rxjs/operators';
import {Store} from '@ngrx/store';
import {AppState} from "../../store/app/app.state";
import {selectCurrentUserPermissions, selectCurrentUserPermissionsLoaded} from "../../store/auth/auth.selector";

@Injectable({
  providedIn: 'root'
})
export class PermissionsGuard {
  private store = inject<Store<AppState>>(Store);
  private router = inject(Router);


  canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | boolean {
    return this.store.select(selectCurrentUserPermissions).pipe(
      switchMap((currentUserPermissions) => {
        if (!currentUserPermissions || currentUserPermissions.length === 0) {
          // Permissions not yet loaded, return observable that will wait for them to be loaded
          return this.store.select(selectCurrentUserPermissionsLoaded).pipe(
            filter((loaded) => loaded),
            take(1),
            switchMap(() => this.permissionsLoadedCheckPermissions(next, state))
          );
        } else {
          // Permissions already loaded, check them immediately
          return scheduled([this.checkPermissions(next, state, currentUserPermissions)], asyncScheduler);
        }
      })
    );
  }

  private permissionsLoadedCheckPermissions(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.store.select(selectCurrentUserPermissions).pipe(
      map((currentUserPermissions) => {
        return this.checkPermissions(next, state, currentUserPermissions);
      })
    )
  }

  private checkPermissions(next: ActivatedRouteSnapshot, state: RouterStateSnapshot, currentUserPermissions: string[]): boolean {
    const requiredPermissions = next.data['requiredPermissions'] as string[];
    const hasPermissionToView = requiredPermissions.every((requiredPermission) =>
      currentUserPermissions.includes(requiredPermission)
    );
    if (hasPermissionToView) {
      return true;
    } else {
      console.debug('[PermissionsGuard] Forbidden - Current user permissions:', currentUserPermissions);
      console.debug('[PermissionsGuard] Forbidden - Current route:', state.url);
      console.debug('[PermissionsGuard] Forbidden - Required permissions for route:', requiredPermissions);
      void this.router.navigate(['/forbidden']);
      return false;
    }
  }
}
