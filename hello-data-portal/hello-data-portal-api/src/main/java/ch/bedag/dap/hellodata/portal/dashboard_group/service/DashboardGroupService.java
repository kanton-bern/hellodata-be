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
package ch.bedag.dap.hellodata.portal.dashboard_group.service;

import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupCreateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupUpdateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.repository.DashboardGroupRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@AllArgsConstructor
public class DashboardGroupService {
    private final DashboardGroupRepository dashboardGroupRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public Page<DashboardGroupDto> getAllDashboardGroups(Pageable pageable, String search) {
        Page<DashboardGroupEntity> page;
        if (search != null && !search.isBlank()) {
            page = dashboardGroupRepository.searchByNameOrDashboardTitle(search, pageable);
        } else {
            page = dashboardGroupRepository.findAll(pageable);
        }
        return page.map(entity -> modelMapper.map(entity, DashboardGroupDto.class));
    }

    @Transactional(readOnly = true)
    public DashboardGroupDto getDashboardGroupById(UUID id) {
        Optional<DashboardGroupEntity> entity = dashboardGroupRepository.findById(id);
        if (entity.isEmpty()) {
            log.error("Dashboard group with id {} not found", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return modelMapper.map(entity.get(), DashboardGroupDto.class);
    }

    @Transactional
    public void create(DashboardGroupCreateDto createDto) {
        // Check if name already exists
        if (dashboardGroupRepository.existsByNameIgnoreCase(createDto.getName())) {
            log.error("Dashboard group with name '{}' already exists", createDto.getName());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dashboard group with this name already exists");
        }
        DashboardGroupEntity entity = modelMapper.map(createDto, DashboardGroupEntity.class);
        dashboardGroupRepository.save(entity);
    }

    @Transactional
    public void update(DashboardGroupUpdateDto updateDto) {
        Optional<DashboardGroupEntity> entity = dashboardGroupRepository.findById(updateDto.getId());
        if (entity.isEmpty()) {
            log.error("Dashboard group with id {} not found", updateDto.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // Check if name already exists for another group
        if (dashboardGroupRepository.existsByNameIgnoreCaseAndIdNot(updateDto.getName(), updateDto.getId())) {
            log.error("Dashboard group with name '{}' already exists", updateDto.getName());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dashboard group with this name already exists");
        }
        DashboardGroupEntity entityToUpdate = entity.get();
        modelMapper.map(updateDto, entityToUpdate);
        dashboardGroupRepository.save(entityToUpdate);
    }

    @Transactional
    public void delete(UUID id) {
        dashboardGroupRepository.deleteById(id);
    }
}
