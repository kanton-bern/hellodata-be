/*
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
package ch.bedag.dap.hellodata.portal.announcement.controller;

import ch.bedag.dap.hellodata.portal.announcement.data.AnnouncementCreateDto;
import ch.bedag.dap.hellodata.portal.announcement.data.AnnouncementDto;
import ch.bedag.dap.hellodata.portal.announcement.data.AnnouncementUpdateDto;
import ch.bedag.dap.hellodata.portal.announcement.service.AnnouncementService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;

    @GetMapping
    public List<AnnouncementDto> getAllAnnouncements() {
        return announcementService.getAllAnnouncements();
    }

    /**
     * this one is open for not logged-in users as well
     *
     * @return current announcements
     */
    @GetMapping("/published")
    public List<AnnouncementDto> getPublishedAnnouncements() {
        return announcementService.getPublishedAnnouncements();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ANNOUNCEMENT_MANAGEMENT')")
    public AnnouncementDto getAnnouncementById(@PathVariable UUID id) {
        return announcementService.getAnnouncementById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ANNOUNCEMENT_MANAGEMENT')")
    public void createAnnouncement(@RequestBody AnnouncementCreateDto announcementCreateDto) {
        announcementService.create(announcementCreateDto);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ANNOUNCEMENT_MANAGEMENT')")
    public void updateAnnouncement(@RequestBody AnnouncementUpdateDto announcementUpdateDto) {
        announcementService.update(announcementUpdateDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ANNOUNCEMENT_MANAGEMENT')")
    public void deleteAnnouncement(@PathVariable UUID id) {
        announcementService.delete(id);
    }
}
