/**
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import {AfterViewInit, Component, ElementRef, inject, ViewChild} from "@angular/core";
import {TranslocoPipe} from "@jsverse/transloco";
import {FormsModule} from "@angular/forms";
import {Button} from "primeng/button";
import {CommentEntryComponent} from "./comment-entry/comment-entry.component";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {addComment} from "../../../store/my-dashboards/my-dashboards.action";
import {
  selectCurrentDashboardContextKey,
  selectCurrentDashboardId,
  selectVisibleComments
} from "../../../store/my-dashboards/my-dashboards.selector";
import {AsyncPipe} from "@angular/common";
import {ConfirmDialog} from "primeng/confirmdialog";
import {PrimeTemplate} from "primeng/api";


@Component({
  selector: 'app-comments-feed',
  templateUrl: './comments-feed.component.html',
  imports: [
    TranslocoPipe,
    FormsModule,
    Button,
    CommentEntryComponent,
    AsyncPipe,
    ConfirmDialog,
    PrimeTemplate
  ],
  styleUrls: ['./comments-feed.component.scss']
})
export class CommentsFeed implements AfterViewInit {
  private readonly store = inject<Store<AppState>>(Store);
  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLElement>;

  comments$ = this.store.select(selectVisibleComments);
  currentDashboardId$ = this.store.select(selectCurrentDashboardId);
  currentDashboardContextKey$ = this.store.select(selectCurrentDashboardContextKey);

  newCommentText = '';

  private currentDashboardId: number | undefined;
  private currentDashboardContextKey: string | undefined;

  constructor() {
    this.currentDashboardId$.subscribe(id => this.currentDashboardId = id);
    this.currentDashboardContextKey$.subscribe(key => this.currentDashboardContextKey = key);
  }

  ngAfterViewInit(): void {
    this.scrollToBottom();
  }


  submitComment(): void {
    const text = this.newCommentText.trim();
    if (!text) return;

    if (this.currentDashboardId && this.currentDashboardContextKey) {
      this.store.dispatch(addComment({
        dashboardId: this.currentDashboardId,
        contextKey: this.currentDashboardContextKey,
        text
      }));
    }

    this.newCommentText = '';

    setTimeout(() => {
      this.scrollToBottom();
    });
  }

  private scrollToBottom(): void {
    const container = this.scrollContainer?.nativeElement;
    if (!container) return;

    container.scrollTo({
      top: container.scrollHeight,
      behavior: 'smooth'
    });
  }
}
