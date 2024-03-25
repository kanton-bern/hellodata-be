import {AfterViewInit, Component, ComponentRef, OnInit, Renderer2, RendererFactory2, ViewContainerRef} from "@angular/core";
import {Observable} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectPublishedAndFilteredAnnouncements} from "../../../../store/announcement/announcement.selector";
import {markAnnouncementAsRead} from "../../../../store/announcement/announcement.action";
import {Announcement} from "../../../../store/announcement/announcement.model";
import {DialogService} from "primeng/dynamicdialog";
import {PublishedAnnouncementsPopupHeader} from "./published-annoucements-popup-header/published-announcements-popup-header";

@Component({
  providers: [DialogService],
  template: `
    <p-divider></p-divider>
    <div *ngFor="let announcement of publishedAnnouncements$ | async" id="ghettobox">
      <p-toolbar>
        <div class="p-toolbar-group-start">
          <i class="fas fa-circle-info"></i>
        </div>
        <div class="p-toolbar-group-center" style="width: 65%">
          <p-editor [(ngModel)]="announcement.message" [disabled]="true" [readonly]="true" [style]="{width: '100%'}">
            <p-header hidden></p-header>
          </p-editor>
        </div>
        <div class="p-toolbar-group-end">
          <div class="published-date" *ngIf="announcement.publishedDate">[{{'@Published date' | transloco}} {{announcement.publishedDate | date: 'dd.MM.yyyy, HH:mm:ss'}}]</div>
          <button (click)="hide(announcement)" [pTooltip]="'@Delete' | transloco" icon="fas fa-circle-xmark" pButton pRipple type="button"></button>
        </div>
      </p-toolbar>
      <p-divider></p-divider>
    </div>`
})
export class PublishedAnnouncementsPopupComponent implements OnInit, AfterViewInit {

  publishedAnnouncements$: Observable<any>;
  private renderer: Renderer2;
  private headerComponentRef!: ComponentRef<PublishedAnnouncementsPopupHeader>;

  constructor(private store: Store<AppState>, private viewContainerRef: ViewContainerRef, private readonly rendererFactory: RendererFactory2) {
    this.publishedAnnouncements$ = this.store.select(selectPublishedAndFilteredAnnouncements);
    this.renderer = this.rendererFactory.createRenderer(null, null);
  }

  ngAfterViewInit(): void {
    this.hide = this.hide.bind(this);
  }

  hide(announcement: Announcement): void {
    this.store.dispatch(markAnnouncementAsRead({announcement}));
  }

  ngOnInit(): void {
    this.headerComponentRef = this.viewContainerRef.createComponent(PublishedAnnouncementsPopupHeader);
    const titleSpan = document.getElementsByClassName('p-dialog-title')[0];
    titleSpan.setAttribute('style', 'width: 100%');
    this.renderer.appendChild(titleSpan, this.headerComponentRef.location.nativeElement)
  }

}
