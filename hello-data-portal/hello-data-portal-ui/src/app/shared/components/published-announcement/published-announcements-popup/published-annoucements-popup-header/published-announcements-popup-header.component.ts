import { Component, inject } from '@angular/core';
import { CheckboxChangeEvent, Checkbox } from "primeng/checkbox";
import {HideAllCurrentPublishedAnnouncementsService} from "../../hide-all-current-published-announcements.service";
import { TranslocoPipe } from '@jsverse/transloco';

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
    imports: [Checkbox, TranslocoPipe]
})
export class PublishedAnnouncementsPopupHeaderComponent {
  private hideAllCurrentPublishedAnnouncementsService = inject(HideAllCurrentPublishedAnnouncementsService);


  onChange($event: CheckboxChangeEvent) {
    this.hideAllCurrentPublishedAnnouncementsService.hide = $event.checked;
  }
}
