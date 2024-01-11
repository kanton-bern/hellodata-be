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

import ch.bedag.dap.hellodata.portal.announcement.data.AnnouncementCreateDto;
import ch.bedag.dap.hellodata.portal.announcement.data.AnnouncementDto;
import ch.bedag.dap.hellodata.portal.announcement.data.AnnouncementUpdateDto;
import ch.bedag.dap.hellodata.portal.announcement.entity.AnnouncementEntity;
import ch.bedag.dap.hellodata.portal.announcement.repository.AnnouncementRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
public class AnnouncementServiceTest {

    @InjectMocks
    private AnnouncementService announcementService;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @Test
    public void testGetAllAnnouncements() {
        // given
        AnnouncementEntity entity = new AnnouncementEntity();
        when(announcementRepository.findAll()).thenReturn(Collections.singletonList(entity));

        AnnouncementDto announcementDto = new AnnouncementDto();
        when(modelMapper.map(entity, AnnouncementDto.class)).thenReturn(announcementDto);

        // when
        List<AnnouncementDto> announcements = announcementService.getAllAnnouncements();

        // then
        assertEquals(1, announcements.size());
        assertEquals(announcementDto, announcements.get(0));
    }

    @Test
    public void testGetPublishedAnnouncements() {
        // given
        AnnouncementEntity entity = new AnnouncementEntity();
        when(announcementRepository.findAllByPublishedIsTrue()).thenReturn(Collections.singletonList(entity));

        AnnouncementDto announcementDto = new AnnouncementDto();
        when(modelMapper.map(entity, AnnouncementDto.class)).thenReturn(announcementDto);

        // when
        List<AnnouncementDto> announcements = announcementService.getPublishedAnnouncements();

        // then
        assertEquals(1, announcements.size());
        assertEquals(announcementDto, announcements.get(0));
    }

    @Test
    public void testCreate() {
        // given
        AnnouncementCreateDto createDto = new AnnouncementCreateDto();

        // when
        announcementService.create(createDto);

        // then
        verify(announcementRepository, times(1)).save(any());
    }

    @Test
    public void testUpdate() {
        // given
        AnnouncementUpdateDto updateDto = new AnnouncementUpdateDto();
        updateDto.setId(UUID.randomUUID());

        AnnouncementEntity existingEntity = new AnnouncementEntity();
        existingEntity.setId(updateDto.getId());
        when(announcementRepository.findById(updateDto.getId())).thenReturn(Optional.of(existingEntity));

        // when
        announcementService.update(updateDto);

        // then
        verify(announcementRepository, times(1)).save(existingEntity);
    }

    @Test
    public void testUpdateNotFound() {
        // given
        AnnouncementUpdateDto updateDto = new AnnouncementUpdateDto();
        updateDto.setId(UUID.randomUUID());

        when(announcementRepository.findById(updateDto.getId())).thenReturn(Optional.empty());

        // when
        assertThrows(ResponseStatusException.class, () -> announcementService.update(updateDto));

        // then
        verify(announcementRepository, never()).save(any(AnnouncementEntity.class));
    }

    @Test
    public void testDelete() {
        // given
        UUID announcementId = UUID.randomUUID();

        // when
        announcementService.delete(announcementId);

        // then
        verify(announcementRepository, times(1)).deleteById(announcementId);
    }

    @Test
    public void testGetAnnouncementById() {
        // given
        UUID announcementId = UUID.randomUUID();

        AnnouncementEntity existingEntity = new AnnouncementEntity();
        existingEntity.setId(announcementId);
        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(existingEntity));

        // when
        AnnouncementDto resultDto = announcementService.getAnnouncementById(announcementId);

        // then
        assertNotNull(resultDto);
        assertEquals(announcementId.toString(), resultDto.getId());
    }

    @Test
    public void testGetAnnouncementByIdNotFound() {
        // given
        UUID announcementId = UUID.randomUUID();

        when(announcementRepository.findById(announcementId)).thenReturn(Optional.empty());

        // when
        assertThrows(ResponseStatusException.class, () -> announcementService.getAnnouncementById(announcementId));

        // then
        verify(modelMapper, never()).map(any(AnnouncementEntity.class), eq(AnnouncementDto.class));
    }
}

