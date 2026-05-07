import {Component, inject, OnDestroy} from "@angular/core";
import {Observable} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectPublishedAndFilteredAnnouncements} from "../../../../store/announcement/announcement.selector";
import {Announcement} from "../../../../store/announcement/announcement.model";
import {selectDefaultLanguage, selectSelectedLanguage} from "../../../../store/auth/auth.selector";
import {TranslateService} from "../../../services/translate.service";
import {Divider} from "primeng/divider";
import {AsyncPipe, DatePipe} from "@angular/common";
import {Toolbar} from "primeng/toolbar";
import {Editor} from "primeng/editor";
import {FormsModule} from "@angular/forms";
import {SharedModule} from "primeng/api";
import {TranslocoPipe} from "@jsverse/transloco";
import {Checkbox} from "primeng/checkbox";
import {DynamicDialogRef} from "primeng/dynamicdialog";

@Component({
  template: `
    <div class="flex items-center justify-end gap-2 px-1 pt-2 pb-3">
      <p-checkbox [binary]="true" inputId="dont-show-again" [(ngModel)]="dontShowAgain"></p-checkbox>
      <label for="dont-show-again" class="cursor-pointer select-none">{{ '@Do not show again' | transloco }}</label>
    </div>
    <p-divider styleClass="mt-0"/>
    @if ((defaultLanguage$ | async); as defaultLanguage) {
      @if ((selectedLanguage$ | async); as selectedLanguage) {
        @for (announcement of publishedAnnouncements$ | async; track announcement) {
          <div>
            <p-toolbar>
              <div class="p-toolbar-group-start">
                <i class="fas fa-circle-info"></i>
              </div>
              <div class="p-toolbar-group-center" style="width: 65%">
                <p-editor [ngModel]="getMessage(announcement, selectedLanguage.code, defaultLanguage)"
                          [disabled]="true"
                          [readonly]="true" class="p-editor-readonly"
                          [style]="{width: '100%'}">
                  <p-header hidden/>
                </p-editor>
              </div>
              <div class="p-toolbar-group-end">
                @if (announcement.publishedDate) {
                  <div class="published-date">
                    {{ '@Published date' | transloco }} {{ announcement.publishedDate | date: 'dd.MM.yyyy, HH:mm:ss' }}
                  </div>
                }
              </div>
            </p-toolbar>
            <p-divider/>
          </div>
        }
      }
    }`,
  imports: [Divider, Toolbar, Editor, FormsModule, SharedModule, AsyncPipe, DatePipe, TranslocoPipe, Checkbox]
})
export class PublishedAnnouncementsPopupComponent implements OnDestroy {
  publishedAnnouncements$: Observable<any>;
  selectedLanguage$: Observable<any>;
  defaultLanguage$: Observable<any>;
  dontShowAgain = false;
  private readonly store = inject<Store<AppState>>(Store);
  private readonly translateService = inject(TranslateService);
  private readonly ref = inject(DynamicDialogRef);

  constructor() {
    this.publishedAnnouncements$ = this.store.select(selectPublishedAndFilteredAnnouncements);
    this.selectedLanguage$ = this.store.select(selectSelectedLanguage);
    this.defaultLanguage$ = this.store.select(selectDefaultLanguage);
  }

  ngOnDestroy(): void {
    this.ref.close({dontShowAgain: this.dontShowAgain});
  }

  getMessage(announcement: Announcement, selectedLanguage: string, defaultLanguage: any): string | undefined {
    const message = announcement?.messages?.[selectedLanguage];
    if (!message || message.trim() === '') {
      return this.translateService.translate('@Translation not available, fallback to default', {default: defaultLanguage.slice(0, 2)?.toUpperCase()}) + '\n' + announcement?.messages?.[defaultLanguage];
    }
    return message;
  }
}
