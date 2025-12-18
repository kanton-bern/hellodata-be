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

import {AfterViewInit, Component, ElementRef, ViewChild} from "@angular/core";
import {TranslocoPipe} from "@jsverse/transloco";
import {FormsModule} from "@angular/forms";
import {Button, ButtonDirective} from "primeng/button";
import {Ripple} from "primeng/ripple";
import {CommentEntryComponent} from "./comment-entry/comment-entry.component";

export enum CommentStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED'
}

export interface CommentEntry {
  text: string;
  author: string;
  status: CommentStatus;
  createdDate: number;
  publishedDate?: number;
  lastEditedDate?: number;
}

@Component({
  selector: 'app-comments-feed',
  templateUrl: './comments-feed.component.html',
  imports: [
    TranslocoPipe,
    FormsModule,
    Button,
    Ripple,
    ButtonDirective,
    CommentEntryComponent
  ],
  styleUrls: ['./comments-feed.component.scss']
})
export class CommentsFeed implements AfterViewInit {
  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLElement>;

  comments: CommentEntry[] = [
    {
      text: 'First test comment.',
      author: 'John Doe',
      status: CommentStatus.PUBLISHED,
      createdDate: new Date('2024-06-01T09:30:00').getTime(),
      publishedDate: new Date('2024-06-01T09:30:00').getTime(),
    },
    {
      text: 'Great data, thanks for sharing!',
      author: 'Anne Smith',
      status: CommentStatus.PUBLISHED,
      createdDate: new Date('2024-06-02T14:15:00').getTime(),
      publishedDate: new Date('2024-06-02T14:15:00').getTime(),
    },
  ];

  newCommentText = '';

  ngAfterViewInit(): void {
    this.scrollToBottom();
  }


  submitComment(): void {
    const text = this.newCommentText.trim();
    if (!text) return;

    this.comments = [
      ...this.comments,
      {
        text,
        author: 'Anonymous',
        status: CommentStatus.DRAFT,
        createdDate: Date.now(),
        publishedDate: Date.now(),
      },
    ];

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
