import {Injectable} from '@angular/core';

@Injectable({providedIn: 'root'})
export class WindowManagementService {
  private openedWindows: Window[] = [];

  openWindow(url: string, target = '_blank', features?: string) {
    const win = window.open(url, target, features);
    if (win) {
      this.openedWindows.push(win);
    }
  }

  closeAllWindows(): void {
    this.openedWindows.forEach(win => {
      try {
        if (win && !win.closed) {
          win.close();
        }
      } catch (e) {
        console.warn('Could not close window', e);
      }
    });
    this.openedWindows = []; // clear references
  }
}
