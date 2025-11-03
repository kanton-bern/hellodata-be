import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {interval} from "rxjs";
import { Injectable, inject } from "@angular/core";
import {prolongCBSession} from "../../store/auth/auth.action";

@Injectable({
  providedIn: 'root'
})
export class CloudbeaverSessionService {
  private store = inject<Store<AppState>>(Store);


  renewSessionInterval$ = interval(60000 * 5);
  private readonly CLOUD_BEAVER_SESSION_COOKIE_NAME = 'cloudbeaver-session-renewal-timer';

  // call to maintain the session
  public createInterval(): void {
    this.createTimerCookie();
    this.renewSessionInterval$
      .subscribe(() => {
        if (this.cookieExists()) {
          this.store.dispatch(prolongCBSession());
        } else {
          console.debug('No cookie found: ' + this.CLOUD_BEAVER_SESSION_COOKIE_NAME);
        }
      });
  }

  public destroyTimerCookie(): void {
    document.cookie = `${this.CLOUD_BEAVER_SESSION_COOKIE_NAME}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/`;
  }

  private createTimerCookie(): void {
    if (!this.cookieExists()) {
      document.cookie = `${this.CLOUD_BEAVER_SESSION_COOKIE_NAME}=active;path=/`;
    }
  }

  private cookieExists(): boolean {
    const cookies = document.cookie.split(';');
    for (const cookie of cookies) {
      if (cookie.trim().startsWith(`${this.CLOUD_BEAVER_SESSION_COOKIE_NAME}=`)) {
        return true;
      }
    }
    return false;
  }
}
