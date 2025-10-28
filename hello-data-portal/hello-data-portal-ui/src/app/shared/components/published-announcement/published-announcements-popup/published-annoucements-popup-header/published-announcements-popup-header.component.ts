import {Component} from '@angular/core';
import {CheckboxChangeEvent} from "primeng/checkbox";
import {HideAllCurrentPublishedAnnouncementsService} from "../../hide-all-current-published-announcements.service";

@Component({
  template: `
    <div class="grid">
      <div class="col-10">
        <h3><b>{{ '@Announcements' | transloco }}</b></h3>
      </div>
      <div class="col">
        <p-checkbox [binary]="true" inputId="dont-show-again"
                    (onChange)="onChange($event)"></p-checkbox>
        <label for="dont-show-again" class="ml-2"> {{ '@Do not show again' | transloco } </label>
      </div>
    </div>
  `,
  standalone: false
})
export class PublishedAnnouncementsPopupHeaderComponent {

  constructor(private hideAllCurrentPublishedAnnouncementsService: HideAllCurrentPublishedAnnouncementsService) {
  }

  onChange($event: CheckboxChangeEvent) {
    this.hideAllCurrentPublishedAnnouncementsService.hide = $event.checked;
  }
}
