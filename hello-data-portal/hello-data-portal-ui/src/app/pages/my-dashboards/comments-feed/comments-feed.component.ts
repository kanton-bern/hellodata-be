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

import {AfterViewInit, Component, ElementRef, inject, output, ViewChild} from "@angular/core";
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
  selectCurrentDashboardUrl,
  selectVisibleComments
} from "../../../store/my-dashboards/my-dashboards.selector";
import {AsyncPipe} from "@angular/common";
import {ConfirmDialog} from "primeng/confirmdialog";
import {PrimeTemplate} from "primeng/api";
import {Select} from "primeng/select";
import {Tooltip} from "primeng/tooltip";
import {CommentEntry} from "../../../store/my-dashboards/my-dashboards.model";
import {map, Observable} from "rxjs";

interface FilterOption {
  label: string;
  value: number | null;
}

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
    PrimeTemplate,
    Select,
    Tooltip
  ],
  styleUrls: ['./comments-feed.component.scss']
})
export class CommentsFeed implements AfterViewInit {
  private readonly store = inject<Store<AppState>>(Store);
  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLElement>;

  pointerUrlClick = output<string>();

  comments$ = this.store.select(selectVisibleComments);
  currentDashboardId$ = this.store.select(selectCurrentDashboardId);
  currentDashboardContextKey$ = this.store.select(selectCurrentDashboardContextKey);
  currentDashboardUrl$ = this.store.select(selectCurrentDashboardUrl);

  // Filter options
  yearOptions: FilterOption[] = [];
  quarterOptions: FilterOption[] = [
    {label: 'All', value: null},
    {label: 'Q1', value: 1},
    {label: 'Q2', value: 2},
    {label: 'Q3', value: 3},
    {label: 'Q4', value: 4}
  ];

  selectedYear: number | null = null;
  selectedQuarter: number | null = null;

  filteredComments$: Observable<CommentEntry[]>;

  newCommentText = '';
  pointerUrl = '';
  showPointerUrlInput = false;

  private currentDashboardId: number | undefined;
  private currentDashboardContextKey: string | undefined;
  private currentDashboardUrl: string | undefined;

  constructor() {
    this.currentDashboardId$.subscribe(id => this.currentDashboardId = id);
    this.currentDashboardContextKey$.subscribe(key => this.currentDashboardContextKey = key);
    this.currentDashboardUrl$.subscribe(url => this.currentDashboardUrl = url);

    // Initialize year options
    this.initYearOptions();

    // Initialize filtered comments
    this.filteredComments$ = this.comments$.pipe(
      map(comments => this.filterComments(comments))
    );
  }

  /**
   * Validates that pointerUrl is a valid Superset link (same domain as current dashboard)
   */
  isPointerUrlValid(): boolean {
    const trimmedUrl = this.pointerUrl.trim();
    if (!trimmedUrl) return true; // Empty is valid (optional field)

    // Relative paths are always valid
    if (trimmedUrl.startsWith('/')) {
      return true;
    }

    // Normalize URL - add https:// if no protocol
    let normalizedUrl = trimmedUrl;
    if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
      normalizedUrl = 'https://' + trimmedUrl;
    }

    // Validate it's from the same Superset instance
    if (!this.currentDashboardUrl) return false;

    try {
      const dashboardUrlObj = new URL(this.currentDashboardUrl);
      const pointerUrlObj = new URL(normalizedUrl);

      // Check same host (Superset instance)
      return dashboardUrlObj.host === pointerUrlObj.host;
    } catch {
      return false;
    }
  }

  /**
   * Returns normalized pointerUrl with protocol
   */
  getNormalizedPointerUrl(): string | undefined {
    const trimmedUrl = this.pointerUrl.trim();
    if (!trimmedUrl) return undefined;

    if (trimmedUrl.startsWith('/')) {
      return trimmedUrl;
    }

    if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
      return 'https://' + trimmedUrl;
    }

    return trimmedUrl;
  }

  private initYearOptions(): void {
    const currentYear = new Date().getFullYear();
    this.yearOptions = [
      {label: 'All', value: null},
      ...Array.from({length: 5}, (_, i) => ({
        label: String(currentYear - i),
        value: currentYear - i
      }))
    ];
  }

  onFilterChange(): void {
    this.filteredComments$ = this.comments$.pipe(
      map(comments => this.filterComments(comments))
    );
  }

  private filterComments(comments: CommentEntry[]): CommentEntry[] {
    if (!comments) return [];

    return comments.filter(comment => {
      const commentDate = new Date(comment.createdDate);
      const commentYear = commentDate.getFullYear();
      const commentQuarter = this.getQuarter(commentDate);

      // Filter by year
      if (this.selectedYear !== null && commentYear !== this.selectedYear) {
        return false;
      }

      // Filter by quarter (based on end date of quarter)
      if (this.selectedQuarter !== null && commentQuarter !== this.selectedQuarter) {
        return false;
      }

      return true;
    });
  }

  private getQuarter(date: Date): number {
    const month = date.getMonth(); // 0-11
    if (month <= 2) return 1;      // Jan, Feb, Mar -> Q1
    if (month <= 5) return 2;      // Apr, May, Jun -> Q2
    if (month <= 8) return 3;      // Jul, Aug, Sep -> Q3
    return 4;                       // Oct, Nov, Dec -> Q4
  }

  ngAfterViewInit(): void {
    this.scrollToBottom();
  }

  togglePointerUrlInput(): void {
    this.showPointerUrlInput = !this.showPointerUrlInput;
    if (!this.showPointerUrlInput) {
      this.pointerUrl = ''; // Clear pointerUrl when hiding
    }
  }


  submitComment(): void {
    const text = this.newCommentText.trim();
    if (!text) return;

    // Block submission if pointerUrl input is shown and URL is invalid
    if (this.showPointerUrlInput && this.pointerUrl.trim() && !this.isPointerUrlValid()) {
      return;
    }

    if (this.currentDashboardId && this.currentDashboardContextKey && this.currentDashboardUrl) {
      // Only include pointerUrl if input is shown
      const pointerUrl = this.showPointerUrlInput ? this.getNormalizedPointerUrl() : undefined;

      this.store.dispatch(addComment({
        dashboardId: this.currentDashboardId,
        contextKey: this.currentDashboardContextKey,
        dashboardUrl: this.currentDashboardUrl,
        text,
        pointerUrl
      }));
    }

    this.newCommentText = '';
    this.pointerUrl = '';
    this.showPointerUrlInput = false;

    setTimeout(() => {
      this.scrollToBottom();
    });
  }

  onPointerUrlClick(url: string): void {
    this.pointerUrlClick.emit(url);
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
