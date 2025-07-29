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

import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardDto;
import ch.bedag.dap.hellodata.portal.superset.data.UpdateSupersetDashboardMetadataDto;
import ch.bedag.dap.hellodata.portal.superset.service.DashboardService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/superset")
@PreAuthorize("hasAnyAuthority('DASHBOARDS')")
public class SupersetDashboardController {

    private final DashboardService dashboardService;

    @GetMapping(value = "/my-dashboards")
    public ResponseEntity<Set<SupersetDashboardDto>> fetchMyDashboards() {
        Set<SupersetDashboardDto> dashboardsWithAccess = dashboardService.fetchMyDashboards();
        return ResponseEntity.ok(dashboardsWithAccess);
    }

    @PatchMapping(value = "/dashboards/{instanceName}/{subsystemId}")
    public void updateDashboard(@PathVariable String instanceName, @PathVariable int subsystemId,
                                @NotNull @Valid @RequestBody UpdateSupersetDashboardMetadataDto updateSupersetDashboardMetadataDto) {
        dashboardService.updateDashboard(instanceName, subsystemId, updateSupersetDashboardMetadataDto);
    }

    @PostMapping(value = "/upload-dashboards/{contextKey}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('DASHBOARD_IMPORT_EXPORT')")
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadFile(
            @RequestParam("formData") MultipartFile file,
            @RequestParam(name = "overwrite", defaultValue = "false") boolean overwrite,
            @RequestParam(name = "passwords", required = false) String passwordsJson,
            @PathVariable String contextKey
    ) throws IOException {
        dashboardService.uploadDashboardsFile(file, passwordsJson, overwrite, contextKey);
    }
}
