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

import ch.bedag.dap.hellodata.commons.HdHtmlSanitizer;
import ch.bedag.dap.hellodata.portal.documentation.data.DocumentationDto;
import ch.bedag.dap.hellodata.portal.documentation.entity.DocumentationEntity;
import ch.bedag.dap.hellodata.portal.documentation.repository.DocumentationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class DocumentationService {

    private final DocumentationRepository documentationRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public DocumentationDto getDocumentation() {
        Optional<DocumentationEntity> first = documentationRepository.findFirstByOrderByIdAsc();
        if (first.isPresent()) {
            DocumentationEntity entity = first.get();
            DocumentationDto documentationDto = modelMapper.map(entity, DocumentationDto.class);
            //FIXME temporary workaround for existing, old non-i18n documentation entity
            //@Deprecated(forRemoval = true)
            if (documentationDto.getTexts() == null || documentationDto.getTexts().isEmpty()) {
                documentationDto.setTexts(new HashMap<>());
                Locale oldDefault = Locale.forLanguageTag("de-CH");
                documentationDto.getTexts().put(oldDefault, entity.getText());
            }
            return documentationDto;
        }
        return null;
    }

    @Transactional
    public void createOrUpdateDocumentation(DocumentationDto documentation) {
        Map<Locale, String> texts = documentation.getTexts();
        for (Map.Entry<Locale, String> entry : texts.entrySet()) {
            entry.setValue(HdHtmlSanitizer.sanitizeHtml(entry.getValue()));
        }
        if (documentationRepository.count() == 0) {
            DocumentationEntity entity = new DocumentationEntity();
            entity.setTexts(texts);
            documentationRepository.save(entity);
        } else {
            Optional<DocumentationEntity> found = documentationRepository.findFirstByOrderByIdAsc();
            if (found.isPresent()) {
                DocumentationEntity entity = found.get();
                entity.setTexts(texts);
                documentationRepository.save(entity);
            }
        }
    }
}
