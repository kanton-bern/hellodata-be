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
package ch.bedag.dap.hellodata.portal.faq.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entities.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repositories.HdContextRepository;
import ch.bedag.dap.hellodata.portal.faq.data.FaqCreateDto;
import ch.bedag.dap.hellodata.portal.faq.data.FaqDto;
import ch.bedag.dap.hellodata.portal.faq.data.FaqUpdateDto;
import ch.bedag.dap.hellodata.portal.faq.entity.FaqEntity;
import ch.bedag.dap.hellodata.portal.faq.repository.FaqRepository;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.UserContextRoleEntity;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
public class FaqServiceTest {

    @InjectMocks
    private FaqService faqService;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @Mock
    private FaqRepository faqRepository;

    @Mock
    private UserService userService;

    @Mock
    private HdContextRepository contextRepository;

    @Test
    public void testGetAllWithCurrentUserContextRoles() {
        // given
        UserContextRoleEntity roleEntity = new UserContextRoleEntity();
        roleEntity.setContextKey("contextKey1");
        Set<UserContextRoleEntity> currentUserContextRoles = new HashSet<>();
        currentUserContextRoles.add(roleEntity);
        when(userService.getCurrentUserDataDomainRolesWithoutNone()).thenReturn(currentUserContextRoles);

        HdContextEntity contextEntity = new HdContextEntity();
        contextEntity.setContextKey("contextKey1");
        contextEntity.setName("ContextName");
        when(contextRepository.getByContextKey("contextKey1")).thenReturn(Optional.of(contextEntity));

        FaqEntity faqEntity = new FaqEntity();
        faqEntity.setContextKey("contextKey1");
        when(faqRepository.findAll()).thenReturn(Collections.singletonList(faqEntity));

        // when
        List<FaqDto> faqList = faqService.getAll();

        // then
        assertEquals(1, faqList.size());
        FaqDto resultDto = faqList.get(0);
        assertEquals("contextKey1", resultDto.getContextKey());
        assertEquals("ContextName", resultDto.getContextName());
    }

    @Test
    public void testGetAllWithoutCurrentUserContextRoles() {
        // given
        when(userService.getCurrentUserDataDomainRolesWithoutNone()).thenReturn(Collections.emptySet());

        // when
        List<FaqDto> faqList = faqService.getAll();

        // then
        assertTrue(faqList.isEmpty());
    }

    @Test
    public void testCreate() {
        // given
        FaqCreateDto createDto = new FaqCreateDto();
        createDto.setContextKey("contextKey");

        // when
        faqService.create(createDto);

        // then
        verify(faqRepository, times(1)).save(any(FaqEntity.class));
    }

    @Test
    public void testUpdate() {
        // given
        FaqUpdateDto updateDto = new FaqUpdateDto();
        updateDto.setId(UUID.randomUUID());

        FaqEntity existingEntity = new FaqEntity();
        existingEntity.setId(updateDto.getId());
        when(faqRepository.findById(updateDto.getId())).thenReturn(Optional.of(existingEntity));

        // when
        faqService.update(updateDto);

        // then
        verify(faqRepository, times(1)).save(existingEntity);
    }

    @Test
    public void testUpdateNotFound() {
        // given
        FaqUpdateDto updateDto = new FaqUpdateDto();
        updateDto.setId(UUID.randomUUID());

        when(faqRepository.findById(updateDto.getId())).thenReturn(Optional.empty());

        // when
        assertThrows(ResponseStatusException.class, () -> faqService.update(updateDto));

        // then
        verify(faqRepository, never()).save(any(FaqEntity.class));
    }

    @Test
    public void testDelete() {
        // given
        UUID faqId = UUID.randomUUID();

        FaqEntity existingEntity = new FaqEntity();
        existingEntity.setId(faqId);
        when(faqRepository.findById(faqId)).thenReturn(Optional.of(existingEntity));

        // when
        faqService.delete(faqId);

        // then
        verify(faqRepository, times(1)).deleteById(faqId);
    }

    @Test
    public void testDeleteNotFound() {
        // given
        UUID faqId = UUID.randomUUID();

        when(faqRepository.findById(faqId)).thenReturn(Optional.empty());

        // when
        faqService.delete(faqId);

        // then
        verify(faqRepository, never()).deleteById(faqId);
    }

    @Test
    public void testGetById() {
        // given
        UUID faqId = UUID.randomUUID();

        FaqEntity existingEntity = new FaqEntity();
        existingEntity.setId(faqId);
        when(faqRepository.findById(faqId)).thenReturn(Optional.of(existingEntity));

        // when
        FaqDto resultDto = faqService.getById(faqId);

        // then
        assertNotNull(resultDto);
        assertEquals(faqId.toString(), resultDto.getId());
    }

    @Test
    public void testGetByIdNotFound() {
        // given
        UUID faqId = UUID.randomUUID();

        when(faqRepository.findById(faqId)).thenReturn(Optional.empty());

        // when
        assertThrows(ResponseStatusException.class, () -> faqService.getById(faqId));

        // then
        verify(modelMapper, never()).map(any(FaqEntity.class), eq(FaqDto.class));
    }
}

