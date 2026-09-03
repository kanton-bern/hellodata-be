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

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.DashboardPaletteResponse;
import ch.bedag.dap.hellodata.portal.base.util.PageUtil;
import ch.bedag.dap.hellodata.portal.superset.data.DashboardAccessDto;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardDto;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetQueryDto;
import ch.bedag.dap.hellodata.portal.superset.data.UpdateSupersetDashboardMetadataDto;
import ch.bedag.dap.hellodata.portal.superset.pdfexport.PaletteClient;
import ch.bedag.dap.hellodata.portal.superset.pdfexport.PdfExportService;
import ch.bedag.dap.hellodata.portal.superset.pdfexport.PdfLayoutRequest;
import ch.bedag.dap.hellodata.portal.superset.service.DashboardAccessService;
import ch.bedag.dap.hellodata.portal.superset.service.DashboardService;
import ch.bedag.dap.hellodata.portal.superset.service.QueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/superset")
public class SupersetController {

    private final DashboardService dashboardService;
    private final QueryService queryService;
    private final DashboardAccessService dashboardAccessService;
    private final PdfExportService pdfExportService;
    private final PaletteClient paletteClient;

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
    public void uploadFile(@RequestParam MultipartFile file, @PathVariable String contextKey, @RequestParam(defaultValue = "false") boolean prune) {
        dashboardService.uploadDashboardsFile(file, contextKey, prune);
    }

    @PreAuthorize("hasAnyAuthority('QUERIES')")
    @GetMapping(value = "/queries/{contextKey}")
    public ResponseEntity<Page<SupersetQueryDto>> fetchQueries(
            @PathVariable String contextKey,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageUtil.createPageable(page, size, sort, "changedOn", Sort.Direction.DESC);
        Page<SupersetQueryDto> queries = queryService.findQueries(contextKey, pageable, search);
        return ResponseEntity.ok(queries);
    }

    @PreAuthorize("hasAnyAuthority('DASHBOARD_ACCESS')")
    @GetMapping(value = "/dashboard_access")
    public ResponseEntity<Page<DashboardAccessDto>> fetchDashboardAccess(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String contextKey) {
        Pageable pageable = PageUtil.createPageable(page, size, sort, "dttm", Sort.Direction.DESC);
        Page<DashboardAccessDto> result = dashboardAccessService.findDashboardAccess(contextKey, pageable, search);
        return ResponseEntity.ok(result);
    }

    /** Charts of one dashboard for the PDF builder palette. */
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @GetMapping(value = "/dashboards/{instanceName}/{dashboardId}/charts")
    public List<DashboardPaletteResponse.ChartRef> charts(@PathVariable String instanceName, @PathVariable long dashboardId) {
        dashboardService.assertCurrentUserMayAccess(instanceName, dashboardId);
        return paletteClient.fetchPalette(instanceName, dashboardId).getCharts();
    }

    /** Existing markdown/text blocks of one dashboard for the PDF builder palette. */
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @GetMapping(value = "/dashboards/{instanceName}/{dashboardId}/markdown")
    public List<String> markdown(@PathVariable String instanceName, @PathVariable long dashboardId) throws Exception {
        dashboardService.assertCurrentUserMayAccess(instanceName, dashboardId);
        return paletteClient.markdownBlocks(paletteClient.fetchPalette(instanceName, dashboardId));
    }

    /** Single-chart screenshot preview for a builder grid tile, sized to the cell's span and rendered
     *  as the current user. The browser can request these lazily per tile while the layout is built. */
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @GetMapping(value = "/dashboards/{instanceName}/{dashboardId}/charts/{chartId}/preview", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> chartPreview(
            @PathVariable String instanceName, @PathVariable long dashboardId, @PathVariable long chartId,
            @RequestParam(defaultValue = "2") int cols, @RequestParam(defaultValue = "2") int rows,
            @RequestParam(defaultValue = "portrait") String template) {
        byte[] png = pdfExportService.chartPreview(instanceName, dashboardId, chartId, cols, rows, template);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    /** Custom grid layout export designed in the Angular builder. */
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @PostMapping(value = "/dashboards/pdf/custom", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportCustomPdf(@Valid @RequestBody PdfLayoutRequest request) {
        byte[] pdf = pdfExportService.exportCustom(request);
        return pdfResponse(pdf, "dashboard-" + request.dashboardId() + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

}
