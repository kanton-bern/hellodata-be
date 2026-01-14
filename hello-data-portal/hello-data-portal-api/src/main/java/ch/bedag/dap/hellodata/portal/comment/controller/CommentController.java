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
package ch.bedag.dap.hellodata.portal.comment.controller;

import ch.bedag.dap.hellodata.portal.comment.data.CommentCreateDto;
import ch.bedag.dap.hellodata.portal.comment.data.CommentDto;
import ch.bedag.dap.hellodata.portal.comment.data.CommentUpdateDto;
import ch.bedag.dap.hellodata.portal.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboards/{contextKey}/{dashboardId}/comments")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    public List<CommentDto> getComments(
            @PathVariable String contextKey,
            @PathVariable int dashboardId) {
        return commentService.getComments(contextKey, dashboardId);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    public CommentDto createComment(
            @PathVariable String contextKey,
            @PathVariable int dashboardId,
            @RequestBody CommentCreateDto createDto) {
        return commentService.createComment(contextKey, dashboardId, createDto);
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    public CommentDto updateComment(
            @PathVariable String contextKey,
            @PathVariable int dashboardId,
            @PathVariable String commentId,
            @RequestBody CommentUpdateDto updateDto) {
        return commentService.updateComment(contextKey, dashboardId, commentId, updateDto);
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    public CommentDto deleteComment(
            @PathVariable String contextKey,
            @PathVariable int dashboardId,
            @PathVariable String commentId) {
        return commentService.deleteComment(contextKey, dashboardId, commentId);
    }

    @PostMapping("/{commentId}/publish")
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    public CommentDto publishComment(
            @PathVariable String contextKey,
            @PathVariable int dashboardId,
            @PathVariable String commentId) {
        return commentService.publishComment(contextKey, dashboardId, commentId);
    }

    @PostMapping("/{commentId}/unpublish")
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    public CommentDto unpublishComment(
            @PathVariable String contextKey,
            @PathVariable int dashboardId,
            @PathVariable String commentId) {
        return commentService.unpublishComment(contextKey, dashboardId, commentId);
    }

    @PostMapping("/{commentId}/clone")
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    public CommentDto cloneCommentForEdit(
            @PathVariable String contextKey,
            @PathVariable int dashboardId,
            @PathVariable String commentId,
            @RequestBody CommentUpdateDto updateDto) {
        return commentService.cloneCommentForEdit(contextKey, dashboardId, commentId, updateDto);
    }

    @PostMapping("/{commentId}/restore/{versionNumber}")
    @PreAuthorize("hasAnyAuthority('DASHBOARDS')")
    public CommentDto restoreVersion(
            @PathVariable String contextKey,
            @PathVariable int dashboardId,
            @PathVariable String commentId,
            @PathVariable int versionNumber) {
        return commentService.restoreVersion(contextKey, dashboardId, commentId, versionNumber);
    }
}

