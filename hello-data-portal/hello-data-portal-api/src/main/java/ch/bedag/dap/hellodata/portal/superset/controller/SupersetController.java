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
package ch.bedag.dap.hellodata.portal.superset.controller;

import ch.bedag.dap.hellodata.portal.base.util.PageUtil;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardDto;
import ch.bedag.dap.hellodata.portal.superset.data.UpdateSupersetDashboardMetadataDto;
import ch.bedag.dap.hellodata.portal.superset.service.DashboardService;
import ch.bedag.dap.hellodata.portal.superset.service.QueryService;
import ch.bedag.dap.hellodata.portal.user.data.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/superset")
public class SupersetController {

    private final DashboardService dashboardService;
    private final QueryService queryService;

    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @GetMapping(value = "/my-dashboards")
    public ResponseEntity<Set<SupersetDashboardDto>> fetchMyDashboards() {
        Set<SupersetDashboardDto> dashboardsWithAccess = dashboardService.fetchMyDashboards();
        return ResponseEntity.ok(dashboardsWithAccess);
    }

    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @PatchMapping(value = "/dashboards/{instanceName}/{subsystemId}")
    public void updateDashboard(@PathVariable String instanceName, @PathVariable int subsystemId,
                                @NotNull @Valid @RequestBody UpdateSupersetDashboardMetadataDto updateSupersetDashboardMetadataDto) {
        dashboardService.updateDashboard(instanceName, subsystemId, updateSupersetDashboardMetadataDto);
    }

    @PostMapping(value = "/upload-dashboards/{contextKey}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAnyAuthority('DASHBOARD_IMPORT_EXPORT')")
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadFile(@RequestParam MultipartFile file, @PathVariable String contextKey) throws IOException {
        dashboardService.uploadDashboardsFile(file, contextKey);
    }

    @PreAuthorize("hasAnyAuthority('QUERIES')")
    @GetMapping(value = "/queries/{contextKey}")
    public ResponseEntity<Page<UserDto>> fetchQueries(
            @PathVariable String contextKey,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageUtil.createPageable(page, size, sort, search);
        List<Object> queries = queryService.fetchQueries(contextKey);
        log.info("Fetched queries {} for contextKey: {}", queries, contextKey);
        Page result = new PageImpl(queries, pageable, queries.size());
        return ResponseEntity.ok(result);
    }

}
