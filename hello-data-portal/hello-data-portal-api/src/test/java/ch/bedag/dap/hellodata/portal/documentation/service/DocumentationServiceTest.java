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
package ch.bedag.dap.hellodata.portal.documentation.service;

import ch.bedag.dap.hellodata.portal.documentation.data.DocumentationDto;
import ch.bedag.dap.hellodata.portal.documentation.entity.DocumentationEntity;
import ch.bedag.dap.hellodata.portal.documentation.repository.DocumentationRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
class DocumentationServiceTest {

    @Mock
    private DocumentationRepository documentationRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private DocumentationService documentationService;

    @Test
    void testGetDocumentation() {
        // given
        DocumentationEntity documentationEntity = new DocumentationEntity();
        documentationEntity.setText("Sample documentation text");

        when(documentationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(documentationEntity));

        DocumentationDto documentationDto = new DocumentationDto();
        Map<Locale, String> textMap = new HashMap<>();
        textMap.put(Locale.ENGLISH, "Sample documentation text");
        documentationDto.setTexts(textMap);
        when(modelMapper.map(documentationEntity, DocumentationDto.class)).thenReturn(documentationDto);

        // when
        DocumentationDto result = documentationService.getDocumentation();

        // then
        assertEquals("Sample documentation text", result.getTexts().get(Locale.ENGLISH));

        verify(documentationRepository, times(1)).findFirstByOrderByIdAsc();
        verify(modelMapper, times(1)).map(documentationEntity, DocumentationDto.class);
    }

    @Test
    void testCreateOrUpdateDocumentation() {
        // given
        DocumentationDto documentationDto = new DocumentationDto();
        Map<Locale, String> textMap = new HashMap<>();
        textMap.put(Locale.ENGLISH, "Updated documentation text");
        documentationDto.setTexts(textMap);

        when(documentationRepository.count()).thenReturn(1L);

        DocumentationEntity existingEntity = new DocumentationEntity();
        when(documentationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingEntity));

        // when
        documentationService.createOrUpdateDocumentation(documentationDto);

        // then
        assertEquals("Updated documentation text", existingEntity.getTexts().get(Locale.ENGLISH));

        verify(documentationRepository, times(1)).count();
        verify(documentationRepository, times(1)).findFirstByOrderByIdAsc();
        verify(documentationRepository, times(1)).save(existingEntity);
    }
}

