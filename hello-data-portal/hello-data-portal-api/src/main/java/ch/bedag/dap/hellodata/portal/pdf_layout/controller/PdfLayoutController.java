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
package ch.bedag.dap.hellodata.portal.pdf_layout.controller;

import ch.bedag.dap.hellodata.portal.pdf_layout.data.PdfLayoutDto;
import ch.bedag.dap.hellodata.portal.pdf_layout.data.PdfLayoutSaveDto;
import ch.bedag.dap.hellodata.portal.pdf_layout.data.PdfLayoutSummaryDto;
import ch.bedag.dap.hellodata.portal.pdf_layout.service.PdfLayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Saved custom PDF layouts of the current user (the PDF export builder). */
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/pdf-layouts")
public class PdfLayoutController {

    private final PdfLayoutService pdfLayoutService;

    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @GetMapping
    public List<PdfLayoutSummaryDto> getMyLayouts(@RequestParam(required = false) String contextKey) {
        return pdfLayoutService.findMyLayouts(contextKey);
    }

    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @GetMapping("/{id}")
    public PdfLayoutDto getLayout(@PathVariable UUID id) {
        return pdfLayoutService.loadLayout(id);
    }

    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PdfLayoutDto createLayout(@RequestBody PdfLayoutSaveDto saveDto) {
        return pdfLayoutService.createLayout(saveDto);
    }

    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @PutMapping("/{id}")
    public PdfLayoutDto updateLayout(@PathVariable UUID id, @RequestBody PdfLayoutSaveDto saveDto) {
        return pdfLayoutService.updateLayout(id, saveDto);
    }

    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLayout(@PathVariable UUID id) {
        pdfLayoutService.deleteLayout(id);
    }
}
