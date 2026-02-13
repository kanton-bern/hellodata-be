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
package ch.bedag.dap.hellodata.portal.dashboard_group.controller;

import ch.bedag.dap.hellodata.portal.base.util.PageUtil;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupCreateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupDomainUserDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupUpdateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard-groups")
@PreAuthorize("hasAnyAuthority('DASHBOARD_GROUPS_MANAGEMENT')")
public class DashboardGroupController {
    private final DashboardGroupService dashboardGroupService;

    @GetMapping
    public ResponseEntity<Page<DashboardGroupDto>> getAllDashboardGroups(
            @RequestParam String contextKey,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageUtil.createPageable(page, size, sort);
        Page<DashboardGroupDto> result = dashboardGroupService.getAllDashboardGroups(contextKey, pageable, search);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public DashboardGroupDto getDashboardGroupById(@PathVariable UUID id) {
        return dashboardGroupService.getDashboardGroupById(id);
    }

    @GetMapping("/eligible-users")
    public ResponseEntity<List<DashboardGroupDomainUserDto>> getEligibleUsersForDomain(
            @RequestParam String contextKey) {
        List<DashboardGroupDomainUserDto> users = dashboardGroupService.getEligibleUsersForDomain(contextKey);
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public void createDashboardGroup(@RequestBody DashboardGroupCreateDto createDto) {
        dashboardGroupService.create(createDto);
    }

    @PutMapping
    public void updateDashboardGroup(@RequestBody DashboardGroupUpdateDto updateDto) {
        dashboardGroupService.update(updateDto);
    }

    @DeleteMapping("/{id}")
    public void deleteDashboardGroup(@PathVariable UUID id) {
        dashboardGroupService.delete(id);
    }
}
