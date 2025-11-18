import {Component, inject} from '@angular/core';
import {Checkbox, CheckboxChangeEvent} from "primeng/checkbox";
import {HideAllCurrentPublishedAnnouncementsService} from "../../hide-all-current-published-announcements.service";
import {TranslocoPipe} from '@jsverse/transloco';
import {DynamicDialogRef} from "primeng/dynamicdialog";

@Component({
  template: `
    <div class="grid grid-cols-12 gap-4">
      <div class="col-span-10">
        <h3><b>{{ '@Announcements' | transloco }}</b></h3>
      </div>
      <div class="col">
        <div class="flex items-center mt-2">
          <p-checkbox [binary]="true" inputId="dont-show-again" (onChange)="onChange($event)"></p-checkbox>
          <label for="dont-show-again" class="ml-2"
                 style="font-size: 0.9rem">{{ '@Do not show again' | transloco }}</label>
          <a (click)="closeDialog()" style="cursor: pointer;"
             class="self-end layout-topbar-button pl-20">
            <i class="fas fa-times"></i>
          </a>
        </div>
      </div>
    </div>
  `,
  imports: [Checkbox, TranslocoPipe]
})
export class PublishedAnnouncementsPopupHeaderComponent {
  private hideAllCurrentPublishedAnnouncementsService = inject(HideAllCurrentPublishedAnnouncementsService);
  ref = inject(DynamicDialogRef);

  closeDialog() {
    console.debug('Closing popup...');
    this.ref.close();
  }

  onChange($event: CheckboxChangeEvent) {
    this.hideAllCurrentPublishedAnnouncementsService.hide = $event.checked;
  }
}
