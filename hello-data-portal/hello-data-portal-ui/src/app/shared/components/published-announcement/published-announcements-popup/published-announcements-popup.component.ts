import {Component, ComponentRef, inject, OnInit, Renderer2, RendererFactory2, ViewContainerRef} from "@angular/core";
import {Observable} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectPublishedAndFilteredAnnouncements} from "../../../../store/announcement/announcement.selector";
import {Announcement} from "../../../../store/announcement/announcement.model";
import {DialogService} from "primeng/dynamicdialog";
import {
  PublishedAnnouncementsPopupHeaderComponent
} from "./published-annoucements-popup-header/published-announcements-popup-header.component";
import {selectDefaultLanguage, selectSelectedLanguage} from "../../../../store/auth/auth.selector";
import {TranslateService} from "../../../services/translate.service";
import {Divider} from "primeng/divider";
import {AsyncPipe, DatePipe} from "@angular/common";
import {Toolbar} from "primeng/toolbar";
import {Editor} from "primeng/editor";
import {FormsModule} from "@angular/forms";
import {SharedModule} from "primeng/api";
import {TranslocoPipe} from "@jsverse/transloco";

@Component({
  providers: [DialogService],
  template: `
    <p-divider/>
    @if ((defaultLanguage$ | async); as defaultLanguage) {
      <div>
        @if ((selectedLanguage$ | async); as selectedLanguage) {
          @if (selectedLanguage.code) {
          <div>
            @for (announcement of publishedAnnouncements$ | async; track announcement) {
              <div id="ghettobox">
                <p-toolbar>
                  <div class="p-toolbar-group-start">
                    <span class="fas fa-circle-info" aria-hidden="true"></span>
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
          </div>
          }
        }
      </div>
    }`,
  imports: [Divider, Toolbar, Editor, FormsModule, SharedModule, AsyncPipe, DatePipe, TranslocoPipe]
})
export class PublishedAnnouncementsPopupComponent implements OnInit {
  publishedAnnouncements$: Observable<Announcement[]>;
  selectedLanguage$: Observable<{ code: string; typeTranslationKey: string }>;
  defaultLanguage$: Observable<string>;

  private readonly store = inject<Store<AppState>>(Store);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly rendererFactory = inject(RendererFactory2);
  private readonly translateService = inject(TranslateService);
  private readonly renderer: Renderer2;
  private headerComponentRef!: ComponentRef<PublishedAnnouncementsPopupHeaderComponent>;

  constructor() {
    this.publishedAnnouncements$ = this.store.select(selectPublishedAndFilteredAnnouncements);
    this.renderer = this.rendererFactory.createRenderer(null, null);
    this.selectedLanguage$ = this.store.select(selectSelectedLanguage);
    this.defaultLanguage$ = this.store.select(selectDefaultLanguage);
  }

  ngOnInit(): void {
    this.headerComponentRef = this.viewContainerRef.createComponent(PublishedAnnouncementsPopupHeaderComponent);
    const titleSpan = document.getElementsByClassName('p-dialog-title')[0];
    titleSpan.setAttribute('style', 'width: 100%');
    this.renderer.appendChild(titleSpan, this.headerComponentRef.location.nativeElement);
  }

  getMessage(announcement: Announcement, selectedLanguage: string, defaultLanguage: string): string | undefined {
    const message = this.findMessage(announcement, selectedLanguage);
    if (!message || message.trim() === '') {
      const fallback = this.findMessage(announcement, defaultLanguage);
      return this.translateService.translate('@Translation not available, fallback to default', {default: defaultLanguage.slice(0, 2)?.toUpperCase()}) + '\n' + (fallback ?? '');
    }
    return message;
  }

  private findMessage(announcement: Announcement, code: string | null | undefined): string | undefined {
    if (!code || !announcement?.messages) return undefined;
    const exact = announcement.messages[code];
    if (exact) return exact;
    const prefix = code.slice(0, 2).toLowerCase();
    const matchedKey = Object.keys(announcement.messages).find(k => k.slice(0, 2).toLowerCase() === prefix);
    return matchedKey ? announcement.messages[matchedKey] : undefined;
  }
}
