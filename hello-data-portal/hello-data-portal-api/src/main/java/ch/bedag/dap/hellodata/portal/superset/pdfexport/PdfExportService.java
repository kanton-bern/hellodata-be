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
package ch.bedag.dap.hellodata.portal.superset.pdfexport;

import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.screenshot.ChartScreenshotRequest.ChartSpec;
import ch.bedag.dap.hellodata.portal.superset.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Orchestrates the custom-layout PDF export: authZ gate, chart sizing, screenshot fetch over
 *  NATS, grid assembly and PDF rendering. */
@Log4j2
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private static final Duration SCREENSHOT_TIMEOUT = Duration.ofSeconds(120);
    private static final int SCREENSHOT_DPI = ReportTemplate.DEFAULT_SCREENSHOT_DPI;

    private final DashboardService dashboardService;
    private final ScreenshotClient screenshotClient;
    private final PdfRenderer pdfRenderer;

    /** Custom grid export designed in the Angular builder. */
    public byte[] exportCustom(PdfLayoutRequest request) {
        dashboardService.assertCurrentUserMayAccess(request.instanceName(), request.dashboardId());
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalStateException("Layout has no cells to export");
        }
        ReportTemplate template = ReportTemplate.fromId(request.template());

        // One screenshot per distinct (chart, cols, rows); specId disambiguates the same chart at two sizes.
        Map<String, ChartSpec> specsById = new LinkedHashMap<>();
        for (PdfLayoutRequest.Item item : request.items()) {
            if (!item.isChart()) {
                continue;
            }
            int cols = LayoutGridPacker.clampCols(item.cols());
            int rows = LayoutGridPacker.clampRowsToBand(item.y(), item.rows());
            String specId = LayoutGridPacker.specId(item.chartId(), cols, rows);
            int[] size = template.chartScreenshotPx(cols, rows, SCREENSHOT_DPI);
            specsById.putIfAbsent(specId, new ChartSpec(specId, item.chartId(), size[0], size[1]));
        }

        // Render as the requesting user so Superset applies that user's row-level-security filters.
        String userEmail = SecurityUtils.getCurrentUserEmail();
        Map<String, byte[]> pngs = screenshotClient.fetchScreenshots(request.instanceName(), userEmail, new ArrayList<>(specsById.values()), SCREENSHOT_TIMEOUT);

        CustomLayout layout = LayoutGridPacker.buildCustomLayout(request, pngs);
        return pdfRenderer.renderCustom(layout, template);
    }
}
