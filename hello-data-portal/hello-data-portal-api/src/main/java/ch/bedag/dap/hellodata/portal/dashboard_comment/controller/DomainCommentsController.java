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
package ch.bedag.dap.hellodata.portal.dashboard_comment.controller;

import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DomainDashboardCommentDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.service.DashboardCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for aggregated domain-level comments across all dashboards.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard-comments")
public class DomainCommentsController {

    private final DashboardCommentService commentService;

    /**
     * Get all comments for a specific data domain (contextKey).
     * Admins see all comments, viewers only see comments for dashboards they have access to.
     */
    @GetMapping("/domain/{contextKey}")
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    public List<DomainDashboardCommentDto> getCommentsForDomain(
            @PathVariable String contextKey,
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted) {
        return commentService.getCommentsForDomain(contextKey, includeDeleted);
    }
}

