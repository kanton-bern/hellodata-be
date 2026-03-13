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

import ch.bedag.dap.hellodata.commons.HdHtmlSanitizer;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.portal.faq.data.FaqCreateDto;
import ch.bedag.dap.hellodata.portal.faq.data.FaqDto;
import ch.bedag.dap.hellodata.portal.faq.data.FaqMessage;
import ch.bedag.dap.hellodata.portal.faq.data.FaqUpdateDto;
import ch.bedag.dap.hellodata.portal.faq.entity.FaqEntity;
import ch.bedag.dap.hellodata.portal.faq.repository.FaqRepository;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@AllArgsConstructor
public class FaqService {
    private final ModelMapper modelMapper;
    private final FaqRepository faqRepository;
    private final UserService userService;
    private final HdContextRepository contextRepository;

    @Transactional(readOnly = true)
    public List<FaqDto> getAll() {
        List<FaqEntity> faqEntities;
        if (SecurityUtils.isSuperuser()) {
            faqEntities = faqRepository.findAll();
        } else {
            Set<UserContextRoleEntity> currentUserContextRoles = userService.getCurrentUserDataDomainRolesWithoutNone();
            if (currentUserContextRoles.isEmpty()) {
                faqEntities = faqRepository.findAllWithoutContext();
            } else {
                List<String> contextKeys = currentUserContextRoles.stream().map(UserContextRoleEntity::getContextKey).toList();
                faqEntities = faqRepository.findByContextKeyIsNullOrContextKeyIn(contextKeys);
            }
        }

        // Batch-resolve all context names in a single query instead of N+1
        Set<String> contextKeys = faqEntities.stream()
                .map(FaqEntity::getContextKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> contextKeyToName = Collections.emptyMap();
        if (!contextKeys.isEmpty()) {
            contextKeyToName = contextRepository.findAllByContextKeyIn(contextKeys)
                    .stream()
                    .collect(Collectors.toMap(HdContextEntity::getContextKey, HdContextEntity::getName, (a, b) -> a));
        }

        Map<String, String> finalContextKeyToName = contextKeyToName;
        return faqEntities.stream()
                .map(this::map)
                .map(dto -> {
                    if (dto.getContextKey() != null) {
                        dto.setContextName(finalContextKeyToName.get(dto.getContextKey()));
                    }
                    return dto;
                })
                .toList();
    }


    private FaqDto map(FaqEntity entity) {
        FaqDto faqDto = modelMapper.map(entity, FaqDto.class);
        if (faqDto.getMessages() == null) {
            faqDto.setMessages(new HashMap<>());
        }
        return faqDto;
    }


    @Transactional
    public void create(FaqCreateDto faqCreateDto) {
        sanitizeFaqMessages(faqCreateDto.getMessages());
        FaqEntity entity = modelMapper.map(faqCreateDto, FaqEntity.class);
        userService.validateUserHasAccessToContext(entity.getContextKey(), "User doesn't have permissions to create the faq");
        faqRepository.save(entity);
    }

    @Transactional
    public void update(FaqUpdateDto faqUpdateDto) {
        Optional<FaqEntity> entity = faqRepository.findById(faqUpdateDto.getId());
        if (entity.isEmpty()) {
            log.error("FAQ with id {} not found", faqUpdateDto.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        FaqEntity entityToUpdate = entity.get();
        userService.validateUserHasAccessToContext(entityToUpdate.getContextKey(), "User doesn't have permissions to update the faq");
        sanitizeFaqMessages(faqUpdateDto.getMessages());
        modelMapper.map(faqUpdateDto, entityToUpdate);
        faqRepository.save(entityToUpdate);
    }

    private void sanitizeFaqMessages(Map<Locale, FaqMessage> messages) {
        if (messages != null) {
            for (FaqMessage faqMessage : messages.values()) {
                faqMessage.setMessage(HdHtmlSanitizer.sanitizeHtml(faqMessage.getMessage()));
            }
        }
    }

    @Transactional
    public void delete(UUID id) {
        Optional<FaqEntity> byId = faqRepository.findById(id);
        if (byId.isPresent()) {
            FaqEntity entity = byId.get();
            userService.validateUserHasAccessToContext(entity.getContextKey(), "User doesn't have permissions to delete the faq");
            faqRepository.deleteById(id);
        }
    }

    @Transactional(readOnly = true)
    public FaqDto getById(UUID id) {
        Optional<FaqEntity> entity = faqRepository.findById(id);
        if (entity.isEmpty()) {
            log.error("FAQ with id {} not found", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        FaqEntity faqEntity = entity.get();
        userService.validateUserHasAccessToContext(faqEntity.getContextKey(), "User doesn't have permissions to view the faq");
        return map(faqEntity);
    }
}
