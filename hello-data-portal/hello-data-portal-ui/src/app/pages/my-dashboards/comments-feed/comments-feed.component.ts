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

import {AfterViewInit, Component, effect, ElementRef, inject, input, OnDestroy, output, ViewChild} from "@angular/core";
import {TranslocoPipe} from "@jsverse/transloco";
import {FormsModule} from "@angular/forms";
import {Button} from "primeng/button";
import {CommentEntryComponent} from "./comment-entry/comment-entry.component";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {addComment, loadAvailableTags, loadDashboardComments} from "../../../store/my-dashboards/my-dashboards.action";
import {
  canViewMetadataAndVersions,
  selectAvailableTags,
  selectCurrentDashboardContextKey,
  selectCurrentDashboardId,
  selectCurrentDashboardUrl,
  selectVisibleComments
} from "../../../store/my-dashboards/my-dashboards.selector";
import {selectCurrentUserCommentPermissions} from "../../../store/auth/auth.selector";
import {AsyncPipe} from "@angular/common";
import {ConfirmDialog} from "primeng/confirmdialog";
import {PrimeTemplate} from "primeng/api";
import {Select} from "primeng/select";
import {Tooltip} from "primeng/tooltip";
import {DashboardCommentEntry, DashboardCommentStatus} from "../../../store/my-dashboards/my-dashboards.model";
import {BehaviorSubject, combineLatest, interval, map, Observable, Subscription, take} from "rxjs";
import {toSignal} from "@angular/core/rxjs-interop";
import {AutoComplete} from "primeng/autocomplete";
import {Dialog} from "primeng/dialog";
import {HttpClient} from "@angular/common/http";
import {environment} from "../../../../environments/environment";
import {NotificationService} from "../../../shared/services/notification.service";
import {DashboardCommentUtilsService} from "../services/dashboard-comment-utils.service";
import {filter, switchMap} from "rxjs/operators";

const COMMENTS_REFRESH_INTERVAL_MS = 30000; // 30 seconds

interface FilterOption {
  label: string;
  value: number | null;
}

interface TagFilterOption {
  label: string;
  value: string | null;
}

interface StatusFilterOption {
  label: string;
  value: DashboardCommentStatus | null;
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
    Tooltip,
    AutoComplete,
    Dialog
  ],
  styleUrls: ['./comments-feed.component.scss']
})
export class CommentsFeed implements AfterViewInit, OnDestroy {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly http = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);
  readonly commentUtils = inject(DashboardCommentUtilsService);
  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLElement>;
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  showCloseButton = input<boolean>(false);
  pointerUrlClick = output<string>();
  closePanel = output<void>();

  comments$ = this.store.select(selectVisibleComments);
  currentDashboardId$ = this.store.select(selectCurrentDashboardId);
  currentDashboardContextKey$ = this.store.select(selectCurrentDashboardContextKey);
  currentDashboardUrl$ = this.store.select(selectCurrentDashboardUrl);

  // Admin check for import/export functionality
  isAdmin$ = this.store.select(canViewMetadataAndVersions);

  // Write permission check for showing the comment form
  canWriteComments$: Observable<boolean> = combineLatest([
    this.store.select(selectCurrentDashboardContextKey),
    this.store.select(selectCurrentUserCommentPermissions)
  ]).pipe(
    map(([contextKey, commentPermissions]) => {
      if (!contextKey) return false;
      const perms = commentPermissions[contextKey];
      return !!perms?.writeComments;
    })
  );

  // Filter options
  yearOptions$: Observable<FilterOption[]>;
  quarterOptions: FilterOption[] = [
    {label: 'All', value: null},
    {label: 'Q1', value: 1},
    {label: 'Q2', value: 2},
    {label: 'Q3', value: 3},
    {label: 'Q4', value: 4}
  ];

  selectedYear: number | null = null;
  selectedQuarter: number | null = null;
  selectedStatus: DashboardCommentStatus | null = null;

  // Status filter options
  statusOptions: StatusFilterOption[] = [
    {label: '@All', value: null},
    {label: '@draft', value: DashboardCommentStatus.DRAFT},
    {label: '@ready for review', value: DashboardCommentStatus.READY_FOR_REVIEW},
    {label: '@published', value: DashboardCommentStatus.PUBLISHED},
    {label: '@declined', value: DashboardCommentStatus.DECLINED},
    {label: '@deleted', value: DashboardCommentStatus.DELETED}
  ];

  filteredComments$: Observable<DashboardCommentEntry[]>;
  // Signal to track filtered comments for auto-scroll
  private readonly filteredCommentsSignal;
  private readonly filterTrigger$ = new BehaviorSubject<void>(undefined);

  newCommentText = '';
  pointerUrl = '';
  showPointerUrlInput = false;

  // Tags
  tagsDialogVisible = false;
  selectedTags: string[] = [];
  tagSuggestions: string[] = [];
  newTagText = '';
  availableTags$ = this.store.select(selectAvailableTags);
  selectedTagFilter: string | null = null;
  tagFilterOptions$: Observable<TagFilterOption[]>;

  private currentDashboardId: number | undefined;
  private currentDashboardContextKey: string | undefined;
  private currentDashboardUrl: string | undefined;
  private commentsRefreshSubscription: Subscription | null = null;

  // Auto-scroll control: enabled by default, disabled when user scrolls up
  private autoScrollEnabled = true;
  private readonly scrollThreshold = 50; // pixels from bottom to consider "at bottom"

  constructor() {
    this.currentDashboardId$.subscribe(id => {
      this.currentDashboardId = id;
      this.loadTagsIfNeeded();
    });
    this.currentDashboardContextKey$.subscribe(key => {
      this.currentDashboardContextKey = key;
      this.loadTagsIfNeeded();
    });
    this.currentDashboardUrl$.subscribe(url => this.currentDashboardUrl = url);

    // Initialize year options based on comments
    this.yearOptions$ = this.comments$.pipe(
      map(comments => {
        const years = new Set<number>();
        if (comments) {
          comments.forEach(c => {
            const date = new Date(c.createdDate);
            years.add(date.getFullYear());
          });
        }
        const sortedYears = Array.from(years).sort((a, b) => b - a);
        return [
          {label: 'All', value: null},
          ...sortedYears.map(year => ({
            label: String(year),
            value: year
          }))
        ];
      })
    );

    // Initialize tag filter options
    this.tagFilterOptions$ = this.availableTags$.pipe(
      map(tags => [
        {label: 'All', value: null},
        ...tags.map(t => ({label: t, value: t}))
      ])
    );

    // Initialize filtered comments
    this.filteredComments$ = combineLatest([
      this.comments$,
      this.filterTrigger$
    ]).pipe(
      map(([comments]) => this.filterComments(comments))
    );

    // Re-assign signal after filteredComments$ is initialized
    this.filteredCommentsSignal = toSignal(this.filteredComments$);

    this.startRefreshTimer();

    // Auto-scroll to bottom when comments change (only if enabled)
    effect(() => {
      // Read the signal to track changes
      const comments = this.filteredCommentsSignal();

      // Add delay to ensure DOM is fully updated with new comments
      if (comments && comments.length > 0 && this.autoScrollEnabled) {
        setTimeout(() => {
          this.scrollToBottom();
        }, 150);
      }
    });
  }

  private loadTagsIfNeeded(): void {
    if (this.currentDashboardId && this.currentDashboardContextKey) {
      this.store.dispatch(loadAvailableTags({
        dashboardId: this.currentDashboardId,
        contextKey: this.currentDashboardContextKey
      }));
    }
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

  onFilterChange(): void {
    if (this.selectedStatus === DashboardCommentStatus.DELETED) {
      this.reloadComments(true);
    } else {
      // If we previously had DELETED selected, we should reload without deleted
      this.reloadComments(false);
    }
    this.filterTrigger$.next();
  }

  private filterComments(comments: DashboardCommentEntry[]): DashboardCommentEntry[] {
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

      // Filter by tag - read from active version in history
      if (this.selectedTagFilter !== null && this.selectedTagFilter !== '') {
        const activeVer = comment.history?.find(v => v.version === comment.activeVersion);
        if (!activeVer?.tags?.includes(this.selectedTagFilter)) {
          return false;
        }
      }

      // Filter by status
      const activeVersion = comment.history?.find(v => v.version === comment.activeVersion);
      return this.shouldShowByStatus(comment, activeVersion);
    });
  }

  private shouldShowByStatus(comment: DashboardCommentEntry, activeVersion?: any): boolean {
    if (this.selectedStatus !== null) {
      if (activeVersion && activeVersion.status !== this.selectedStatus) {
        // Special case: if entire comment is deleted, treat it as DELETED status
        return this.selectedStatus === DashboardCommentStatus.DELETED && !!comment.deleted;
      }
      return true;
    }

    // If "All" is selected, hide deleted comments
    return !comment.deleted && activeVersion?.status !== DashboardCommentStatus.DELETED;
  }

  private getQuarter(date: Date): number {
    const month = date.getMonth(); // 0-11
    if (month <= 2) return 1;      // Jan, Feb, Mar -> Q1
    if (month <= 5) return 2;      // Apr, May, Jun -> Q2
    if (month <= 8) return 3;      // Jul, Aug, Sep -> Q3
    return 4;                       // Oct, Nov, Dec -> Q4
  }

  ngAfterViewInit(): void {
    // Enable auto-scroll when panel opens and scroll to bottom
    this.autoScrollEnabled = true;
    this.scrollToBottom();

    // Listen for scroll events to manage auto-scroll behavior
    const container = this.scrollContainer?.nativeElement;
    if (container) {
      container.addEventListener('scroll', () => this.onScroll());
    }
  }

  ngOnDestroy(): void {
    this.stopRefreshTimer();
  }

  private startRefreshTimer(): void {
    this.stopRefreshTimer();
    this.commentsRefreshSubscription = interval(COMMENTS_REFRESH_INTERVAL_MS).pipe(
      switchMap(() => combineLatest([
        this.store.select(selectCurrentDashboardId),
        this.store.select(selectCurrentDashboardContextKey),
        this.store.select(selectCurrentDashboardUrl)
      ]).pipe(take(1))),
      filter(([id, key, url]) => id !== null && key !== null && url !== null)
    ).subscribe(() => {
      this.reloadComments(this.selectedStatus === DashboardCommentStatus.DELETED);
    });
  }

  private stopRefreshTimer(): void {
    if (this.commentsRefreshSubscription) {
      this.commentsRefreshSubscription.unsubscribe();
      this.commentsRefreshSubscription = null;
    }
  }

  private onScroll(): void {
    const container = this.scrollContainer?.nativeElement;
    if (!container) return;

    const isAtBottom = container.scrollHeight - container.scrollTop - container.clientHeight <= this.scrollThreshold;

    if (isAtBottom) {
      // User scrolled back to bottom - re-enable auto-scroll
      this.autoScrollEnabled = true;
    } else {
      // User scrolled up - disable auto-scroll
      this.autoScrollEnabled = false;
    }
  }

  togglePointerUrlInput(): void {
    this.showPointerUrlInput = !this.showPointerUrlInput;
    if (!this.showPointerUrlInput) {
      this.pointerUrl = ''; // Clear pointerUrl when hiding
    }
  }

  // Tags methods
  openTagsDialog(): void {
    this.tagsDialogVisible = true;
  }

  closeTagsDialog(): void {
    this.tagsDialogVisible = false;
    this.newTagText = '';
  }

  searchTags(event: { query: string }): void {
    this.availableTags$.subscribe(tags => {
      const query = event.query.toLowerCase();
      this.tagSuggestions = tags.filter(tag =>
        tag.toLowerCase().includes(query) && !this.selectedTags.includes(tag)
      );
    }).unsubscribe();
  }

  addTag(): void {
    const tag = this.newTagText.trim().toLowerCase().substring(0, 10);
    if (tag && !this.selectedTags.includes(tag)) {
      this.selectedTags = [...this.selectedTags, tag];
    }
    this.newTagText = '';
  }

  removeTag(tag: string): void {
    this.selectedTags = this.selectedTags.filter(t => t !== tag);
  }

  onTagSelect(event: { value: string }): void {
    const tag = event.value.toLowerCase().substring(0, 10);
    if (tag && !this.selectedTags.includes(tag)) {
      this.selectedTags = [...this.selectedTags, tag];
    }
    this.newTagText = '';
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
      // Include tags if any are selected
      const tags = this.selectedTags.length > 0 ? this.selectedTags : undefined;

      this.store.dispatch(addComment({
        dashboardId: this.currentDashboardId,
        contextKey: this.currentDashboardContextKey,
        dashboardUrl: this.currentDashboardUrl,
        text,
        pointerUrl,
        tags
      }));

      // Re-enable auto-scroll to show the new comment
      this.autoScrollEnabled = true;
    }

    this.newCommentText = '';
    this.pointerUrl = '';
    this.showPointerUrlInput = false;
    this.selectedTags = [];
    this.tagsDialogVisible = false;
  }

  onPointerUrlClick(url: string): void {
    this.pointerUrlClick.emit(url);
  }

  private scrollToBottom(): void {
    const container = this.scrollContainer?.nativeElement;
    if (!container) return;

    // Use double requestAnimationFrame to ensure DOM is fully rendered
    // First frame: DOM updates are scheduled
    // Second frame: DOM is actually rendered
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        // Scroll to maximum possible value to ensure we reach the bottom
        container.scrollTo({
          top: container.scrollHeight + 1000, // Add extra offset to guarantee we reach the bottom
          behavior: 'smooth'
        });
      });
    });
  }

  // Export comments to JSON file
  exportComments(): void {
    if (!this.currentDashboardId || !this.currentDashboardContextKey) {
      this.notificationService.warn('@No dashboard selected');
      return;
    }

    this.http.get<{ dashboardTitle?: string }>(
      `${environment.portalApi}/dashboards/${this.currentDashboardContextKey}/${this.currentDashboardId}/comments/export`
    ).pipe(take(1)).subscribe({
      next: (data) => {
        const blob = new Blob([JSON.stringify(data, null, 2)], {type: 'application/json'});
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        // Use dashboard title in filename (sanitized for filesystem)
        const sanitizedTitle = data.dashboardTitle
          ? data.dashboardTitle.replace(/[^a-zA-Z0-9-_]/g, '_').substring(0, 50)
          : `dashboard_${this.currentDashboardId}`;
        link.download = `comments_${this.currentDashboardContextKey}_${sanitizedTitle}_${new Date().toISOString().slice(0, 10)}.json`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.notificationService.success('@Comments exported successfully');
      },
      error: () => {
        this.notificationService.error('@Failed to export comments');
      }
    });
  }

  // Trigger file input click
  triggerImport(): void {
    this.fileInput.nativeElement.click();
  }

  // Handle file selection
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    if (!file.name.endsWith('.json')) {
      this.notificationService.error('@File must be JSON format');
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const content = e.target?.result as string;
        const data = JSON.parse(content);
        this.importComments(data);
      } catch {
        this.notificationService.error('@Invalid JSON file');
      }
      input.value = '';
    };
    reader.readAsText(file);
  }

  // Import comments from JSON data
  private importComments(data: unknown): void {
    if (!this.currentDashboardId || !this.currentDashboardContextKey) {
      this.notificationService.warn('@No dashboard selected');
      return;
    }

    this.http.post<{ imported: number; updated: number; skipped: number; message: string }>(
      `${environment.portalApi}/dashboards/${this.currentDashboardContextKey}/${this.currentDashboardId}/comments/import`,
      data
    ).pipe(take(1)).subscribe({
      next: (response) => {
        if (response.updated > 0) {
          this.notificationService.success('@Comments imported and updated successfully', {
            imported: response.imported,
            updated: response.updated
          });
        } else {
          this.notificationService.success('@Comments imported successfully', {count: response.imported});
        }
        // Reload comments list
        this.reloadComments(this.selectedStatus === DashboardCommentStatus.DELETED);
        // Reload tags
        this.loadTagsIfNeeded();
      },
      error: (err) => {
        const message = err.error?.message || '@Failed to import comments';
        this.notificationService.error(message);
      }
    });
  }

  // Reload comments from backend
  private reloadComments(includeDeleted: boolean = false): void {
    if (this.currentDashboardId && this.currentDashboardContextKey && this.currentDashboardUrl) {
      this.store.dispatch(loadDashboardComments({
        dashboardId: this.currentDashboardId,
        contextKey: this.currentDashboardContextKey,
        dashboardUrl: this.currentDashboardUrl,
        includeDeleted
      }));
    }
  }
}
