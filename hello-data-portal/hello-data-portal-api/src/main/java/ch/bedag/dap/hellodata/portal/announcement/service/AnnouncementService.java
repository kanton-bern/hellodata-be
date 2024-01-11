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
package ch.bedag.dap.hellodata.portal.announcement.service;

import ch.bedag.dap.hellodata.commons.HdHtmlSanitizer;
import ch.bedag.dap.hellodata.portal.announcement.data.AnnouncementCreateDto;
import ch.bedag.dap.hellodata.portal.announcement.data.AnnouncementDto;
import ch.bedag.dap.hellodata.portal.announcement.data.AnnouncementUpdateDto;
import ch.bedag.dap.hellodata.portal.announcement.entity.AnnouncementEntity;
import ch.bedag.dap.hellodata.portal.announcement.repository.AnnouncementRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Log4j2
@AllArgsConstructor
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getAllAnnouncements() {
        return announcementRepository.findAll().stream().map(entity -> modelMapper.map(entity, AnnouncementDto.class)).toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getPublishedAnnouncements() {
        return announcementRepository.findAllByPublishedIsTrue().stream().map(entity -> modelMapper.map(entity, AnnouncementDto.class)).toList();
    }

    @Transactional
    public void create(AnnouncementCreateDto announcementCreateDto) {
        AnnouncementEntity entity = modelMapper.map(announcementCreateDto, AnnouncementEntity.class);
        sanitizeMessage(announcementCreateDto.getMessage(), entity);
        updatePublishedDate(entity);
        announcementRepository.save(entity);
    }

    @Transactional
    public void update(AnnouncementUpdateDto announcementUpdateDto) {
        Optional<AnnouncementEntity> entity = announcementRepository.findById(announcementUpdateDto.getId());
        if (entity.isEmpty()) {
            log.error("Announcement with id {} not found", announcementUpdateDto.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        AnnouncementEntity entityToUpdate = entity.get();
        modelMapper.map(announcementUpdateDto, entityToUpdate);
        sanitizeMessage(announcementUpdateDto.getMessage(), entityToUpdate);
        updatePublishedDate(entityToUpdate);
        announcementRepository.save(entityToUpdate);
    }

    private static void sanitizeMessage(String unsanitizedMessage, AnnouncementEntity entity) {
        String sanitizedMessage = HdHtmlSanitizer.sanitizeHtml(unsanitizedMessage);
        entity.setMessage(sanitizedMessage);
    }

    private static void updatePublishedDate(AnnouncementEntity entityToUpdate) {
        if (entityToUpdate.getPublished() != null) {
            entityToUpdate.setPublishedDate(entityToUpdate.getPublished() ? LocalDateTime.now() : null);
        }
    }

    @Transactional
    public void delete(UUID id) {
        announcementRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public AnnouncementDto getAnnouncementById(UUID id) {
        Optional<AnnouncementEntity> entity = announcementRepository.findById(id);
        if (entity.isEmpty()) {
            log.error("Announcement with id {} not found", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return modelMapper.map(entity.get(), AnnouncementDto.class);
    }
}
