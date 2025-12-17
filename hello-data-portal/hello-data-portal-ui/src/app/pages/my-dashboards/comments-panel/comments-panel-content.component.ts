import {Component, EventEmitter, Output} from '@angular/core';

@Component({
  selector: 'app-comments-panel-content',
  templateUrl: './comments-panel-content.component.html',
  styleUrls: ['./comments-panel-content.component.scss']
})
export class CommentsPanelContentComponent {
  @Output() toggleComments = new EventEmitter<void>();

}
