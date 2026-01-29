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
package ch.bedag.dap.hellodata.portal.external_dashboard.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.portal.external_dashboard.data.CreateExternalDashboardDto;
import ch.bedag.dap.hellodata.portal.external_dashboard.data.ExternalDashboardDto;
import ch.bedag.dap.hellodata.portal.external_dashboard.data.UpdateExternalDashboardDto;
import ch.bedag.dap.hellodata.portal.external_dashboard.entity.ExternalDashboardEntity;
import ch.bedag.dap.hellodata.portal.external_dashboard.repository.ExternalDashboardRepository;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class ExternalDashboardService {
    private final ExternalDashboardRepository externalDashboardRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final HdContextRepository contextRepository;

    @Transactional(readOnly = true)
    public List<ExternalDashboardDto> getExternalDashboards() {
        Set<UserContextRoleEntity> currentUserContextRoles = userService.getCurrentUserDataDomainRolesWithoutNone();
        if (!currentUserContextRoles.isEmpty()) {
            List<String> contextKeys = currentUserContextRoles.stream().map(UserContextRoleEntity::getContextKey).toList();
            return externalDashboardRepository.findAll()
                    .stream()
                    .filter(entity -> contextKeys.contains(entity.getContextKey()))
                    .map(entity -> modelMapper.map(entity, ExternalDashboardDto.class))
                    .map(dto -> {
                        Optional<HdContextEntity> byContextKey = contextRepository.getByContextKey(dto.getContextKey());
                        if (byContextKey.isPresent()) {
                            HdContextEntity context = byContextKey.get();
                            dto.setContextName(context.getName());
                        }
                        return dto;
                    })
                    .toList();
        }
        return Collections.emptyList();
    }

    @Transactional
    public void create(CreateExternalDashboardDto createExternalDashboardDto) {
        ExternalDashboardEntity entity = modelMapper.map(createExternalDashboardDto, ExternalDashboardEntity.class);
        userService.validateUserHasAccessToContext(entity.getContextKey(), "User doesn't have permissions to create the external dashboard");
        externalDashboardRepository.save(entity);
    }

    @Transactional
    public void update(UpdateExternalDashboardDto updateExternalDashboardDto) {
        Optional<ExternalDashboardEntity> byId = externalDashboardRepository.findById(updateExternalDashboardDto.getId());
        if (byId.isPresent()) {
            ExternalDashboardEntity externalDashboardEntity = byId.get();
            userService.validateUserHasAccessToContext(externalDashboardEntity.getContextKey(), "User doesn't have permissions to update the external dashboard");
            modelMapper.map(updateExternalDashboardDto, externalDashboardEntity);
            externalDashboardRepository.save(externalDashboardEntity);
        } else {
            String errMsg = "External dashboard not found. Id: " + updateExternalDashboardDto.getId();
            log.error(errMsg);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errMsg);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Optional<ExternalDashboardEntity> byId = externalDashboardRepository.findById(id);
        if (byId.isPresent()) {
            ExternalDashboardEntity externalDashboardEntity = byId.get();
            userService.validateUserHasAccessToContext(externalDashboardEntity.getContextKey(), "User doesn't have permissions to delete the external dashboard");
            externalDashboardRepository.deleteById(id);
        }
    }

    @Transactional(readOnly = true)
    public ExternalDashboardDto getExternalDashboard(UUID id) {
        Optional<ExternalDashboardEntity> byId = externalDashboardRepository.findById(id);
        if (byId.isPresent()) {
            ExternalDashboardEntity externalDashboardEntity = byId.get();
            userService.validateUserHasAccessToContext(externalDashboardEntity.getContextKey(), "User doesn't have permissions to view the external dashboard");
            return modelMapper.map(externalDashboardEntity, ExternalDashboardDto.class);
        } else {
            String errMsg = "External dashboard not found. Id: " + id;
            log.error(errMsg);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errMsg);
        }
    }
}
