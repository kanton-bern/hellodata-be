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
      <div class="col-span-2">
        <div class="flex items-center justify-between w-full">
          <div class="flex items-center">
            <p-checkbox [binary]="true" inputId="dont-show-again" (onChange)="onChange($event)"></p-checkbox>
            <label for="dont-show-again" class="ml-2"
                   style="font-size: 0.9rem">{{ '@Do not show again' | transloco }}</label>
          </div>
          <a (click)="closeDialog()" style="cursor: pointer;"
             class="layout-topbar-button ml-4">
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
