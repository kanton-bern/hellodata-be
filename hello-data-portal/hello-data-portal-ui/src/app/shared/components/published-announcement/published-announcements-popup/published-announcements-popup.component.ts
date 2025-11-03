import {
  AfterViewInit,
  Component,
  ComponentRef,
  OnInit,
  Renderer2,
  RendererFactory2,
  ViewContainerRef
} from "@angular/core";
import {Observable} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectPublishedAndFilteredAnnouncements} from "../../../../store/announcement/announcement.selector";
import {markAnnouncementAsRead} from "../../../../store/announcement/announcement.action";
import {Announcement} from "../../../../store/announcement/announcement.model";
import {DialogService} from "primeng/dynamicdialog";
import {
  PublishedAnnouncementsPopupHeaderComponent
} from "./published-annoucements-popup-header/published-announcements-popup-header.component";
import {selectDefaultLanguage, selectSelectedLanguage} from "../../../../store/auth/auth.selector";
import {TranslateService} from "../../../services/translate.service";
import { Divider } from "primeng/divider";
import { NgIf, NgFor, AsyncPipe, DatePipe } from "@angular/common";
import { Toolbar } from "primeng/toolbar";
import { Editor } from "primeng/editor";
import { FormsModule } from "@angular/forms";
import { SharedModule } from "primeng/api";
import { TranslocoPipe } from "@jsverse/transloco";

@Component({
    providers: [DialogService],
    template: `
    <p-divider></p-divider>
    <div *ngIf="(defaultLanguage$ | async) as defaultLanguage">
      <div *ngIf="(selectedLanguage$ | async) as selectedLanguage">
        <div *ngFor="let announcement of publishedAnnouncements$ | async" id="ghettobox">
          <p-toolbar>
            <div class="p-toolbar-group-start">
              <i class="fas fa-circle-info"></i>
            </div>
            <div class="p-toolbar-group-center" style="width: 65%">
              <p-editor [ngModel]="getMessage(announcement, selectedLanguage.code, defaultLanguage)" [disabled]="true"
                        [readonly]="true" class="p-editor-readonly"
                        [style]="{width: '100%'}">
                <p-header hidden></p-header>
              </p-editor>
            </div>
            <div class="p-toolbar-group-end">
              <div class="published-date" *ngIf="announcement.publishedDate">
                [{{ '@Published date' | transloco }} {{ announcement.publishedDate | date: 'dd.MM.yyyy, HH:mm:ss' }}]
              </div>
            </div>
          </p-toolbar>
          <p-divider></p-divider>
        </div>
      </div>
    </div>`,
    imports: [Divider, NgIf, NgFor, Toolbar, Editor, FormsModule, SharedModule, AsyncPipe, DatePipe, TranslocoPipe]
})
export class PublishedAnnouncementsPopupComponent implements OnInit, AfterViewInit {

  publishedAnnouncements$: Observable<any>;
  selectedLanguage$: Observable<any>;
  defaultLanguage$: Observable<any>;
  private renderer: Renderer2;
  private headerComponentRef!: ComponentRef<PublishedAnnouncementsPopupHeaderComponent>;

  constructor(private store: Store<AppState>,
              private viewContainerRef: ViewContainerRef,
              private readonly rendererFactory: RendererFactory2,
              private translateService: TranslateService) {
    this.publishedAnnouncements$ = this.store.select(selectPublishedAndFilteredAnnouncements);
    this.renderer = this.rendererFactory.createRenderer(null, null);
    this.selectedLanguage$ = store.select(selectSelectedLanguage);
    this.defaultLanguage$ = store.select(selectDefaultLanguage);
  }

  ngAfterViewInit(): void {
    this.hide = this.hide.bind(this);
  }

  hide(announcement: Announcement): void {
    this.store.dispatch(markAnnouncementAsRead({announcement}));
  }

  ngOnInit(): void {
    this.headerComponentRef = this.viewContainerRef.createComponent(PublishedAnnouncementsPopupHeaderComponent);
    const titleSpan = document.getElementsByClassName('p-dialog-title')[0];
    titleSpan.setAttribute('style', 'width: 100%');
    this.renderer.appendChild(titleSpan, this.headerComponentRef.location.nativeElement)
  }

  getMessage(announcement: Announcement, selectedLanguage: string, defaultLanguage: any): string | undefined {
    const message = announcement?.messages?.[selectedLanguage];
    if (!message || message.trim() === '') {
      return this.translateService.translate('@Translation not available, fallback to default', {default: defaultLanguage.slice(0, 2)?.toUpperCase()}) + '\n' + announcement?.messages?.[defaultLanguage];
    }
    return message;
  }

}
