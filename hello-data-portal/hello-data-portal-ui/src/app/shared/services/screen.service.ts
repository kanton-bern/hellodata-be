import {inject, Injectable} from '@angular/core';
import {BreakpointObserver, Breakpoints} from '@angular/cdk/layout';
import {Observable, Subject} from 'rxjs';
import {map} from 'rxjs/operators';

@Injectable()
export class ScreenService {
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly changedSubject = new Subject<boolean>();

  // Expose as observable for subscribers
  readonly changed$ = this.changedSubject.asObservable();

  constructor() {
    this.breakpointObserver
      .observe([Breakpoints.XSmall, Breakpoints.Small, Breakpoints.Medium, Breakpoints.Large])
      .subscribe(() => this.changedSubject.next(true));
  }

  private isLargeScreen() {
    const isLarge = this.breakpointObserver.isMatched(Breakpoints.Large);
    const isXLarge = this.breakpointObserver.isMatched(Breakpoints.XLarge);
    return isLarge || isXLarge;
  }

  public get sizes(): Record<string, boolean> {
    return {
      'screen-x-small': this.breakpointObserver.isMatched(Breakpoints.XSmall),
      'screen-small': this.breakpointObserver.isMatched(Breakpoints.Small),
      'screen-medium': this.breakpointObserver.isMatched(Breakpoints.Medium),
      'screen-large': this.isLargeScreen(),
    };
  }

  public get isMobile(): Observable<boolean> {
    return this.breakpointObserver
      .observe([
        Breakpoints.Handset,
        Breakpoints.Small,
        Breakpoints.XSmall,
        Breakpoints.HandsetLandscape,
        Breakpoints.HandsetPortrait,
      ])
      .pipe(map(result => result.matches));
  }
}
