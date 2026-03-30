///
/// Copyright © 2024, Kanton Bern
/// All rights reserved.
///
/// Redistribution and use in source and binary forms, with or without
/// modification, are permitted provided that the following conditions are met:
///     * Redistributions of source code must retain the above copyright
///       notice, this list of conditions and the following disclaimer.
///     * Redistributions in binary form must reproduce the above copyright
///       notice, this list of conditions and the following disclaimer in the
///       documentation and/or other materials provided with the distribution.
///     * Neither the name of the <organization> nor the
///       names of its contributors may be used to endorse or promote products
///       derived from this software without specific prior written permission.
///
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
/// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
/// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
/// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
/// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
/// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
/// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
/// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
/// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
/// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
///

import {inject, Injectable} from "@angular/core";
import {asyncScheduler, BehaviorSubject, Observable, scheduled} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {Announcement, AnnouncementCreate, AnnouncementUpdate} from "./announcement.model";
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class AnnouncementService {
  protected httpClient = inject(HttpClient);


  baseUrl = `${environment.portalApi}/announcements`;
  public hiddenAnnouncementIds = new BehaviorSubject<string[]>([]);
  private readonly HIDDEN_ANNOUNCEMENTS_KEY = 'hidden_announcements';

  constructor() {
    const stored = localStorage.getItem(this.HIDDEN_ANNOUNCEMENTS_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      // Migrate from legacy format (full Announcement objects) to ID-only
      const ids: string[] = parsed.map((item: any) => typeof item === 'string' ? item : item.id);
      localStorage.setItem(this.HIDDEN_ANNOUNCEMENTS_KEY, JSON.stringify(ids));
      this.hiddenAnnouncementIds.next(ids);
    }
  }

  public getAllAnnouncements(): Observable<Announcement[]> {
    return this.httpClient.get<Announcement[]>(`${this.baseUrl}`);
  }

  public getPublishedAnnouncements(): Observable<Announcement[]> {
    return this.httpClient.get<Announcement[]>(`${this.baseUrl}/published`);
  }

  public getAnnouncementById(announcementId: string): Observable<Announcement> {
    return this.httpClient.get<Announcement>(`${this.baseUrl}/${announcementId}`);
  }

  public deleteAnnouncementById(announcementId: string): Observable<any> {
    return this.httpClient.delete<void>(`${this.baseUrl}/${announcementId}`);
  }

  public createAnnouncement(announcementCreate: AnnouncementCreate): Observable<any> {
    return this.httpClient.post<any>(`${this.baseUrl}`, announcementCreate);
  }

  public updateAnnouncement(announcementUpdate: AnnouncementUpdate): Observable<any> {
    return this.httpClient.put<any>(`${this.baseUrl}`, announcementUpdate);
  }

  public hideAnnouncement(announcement: Announcement) {
    console.debug('Hiding announcement', announcement.id);
    const stored = localStorage.getItem(this.HIDDEN_ANNOUNCEMENTS_KEY);
    const ids: string[] = stored ? JSON.parse(stored) : [];
    if (!ids.includes(announcement.id)) {
      ids.push(announcement.id);
    }
    localStorage.setItem(this.HIDDEN_ANNOUNCEMENTS_KEY, JSON.stringify(ids));
    this.hiddenAnnouncementIds.next(ids);
    return scheduled([announcement], asyncScheduler);
  }

  public getHiddenAnnouncementIds(): Observable<string[]> {
    return this.hiddenAnnouncementIds.asObservable();
  }

}
