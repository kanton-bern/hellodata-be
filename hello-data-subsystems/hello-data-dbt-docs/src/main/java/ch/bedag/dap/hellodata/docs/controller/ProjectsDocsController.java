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
package ch.bedag.dap.hellodata.docs.controller;

import ch.bedag.dap.hellodata.docs.entities.User;
import ch.bedag.dap.hellodata.docs.model.ProjectDoc;
import ch.bedag.dap.hellodata.docs.service.ProjectDocService;
import ch.bedag.dap.hellodata.docs.service.SecurityService;
import ch.bedag.dap.hellodata.docs.service.StorageTraverseService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/projects-docs")
public class ProjectsDocsController {
    private final ProjectDocService projectDocService;
    private final SecurityService securityService;
    private final StorageTraverseService storageTraverseService;

    @GetMapping
    @Operation(summary = "Get all projects meta-info.")
    public List<ProjectDoc> getAllProjectsInfo(@AuthenticationPrincipal User user) {
        return securityService.getProjectDocsFilteredByUser(user, projectDocService.getAllProjectsDocs());
    }

    @GetMapping("/{name}/info")
    @Operation(summary = "Get project meta-info by name.")
    public ProjectDoc getInfoByName(@AuthenticationPrincipal User user, @PathVariable("name") String name) {
        securityService.validateUserIsAllowedOnProjectDoc(user, name);
        Optional<ProjectDoc> doc = projectDocService.getByName(name);
        if (doc.isPresent()) {
            return doc.get();
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doc not found by name: " + name);
    }

    @GetMapping("/get-by-path")
    @Operation(summary = "Get project html page by path (does the redirect to <server>/<path>)")
    public void getByPath(@AuthenticationPrincipal User user, HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path) throws IOException {
        securityService.validateIsAllowedToAccessPath(user, path);
        Path pathWithContextPath = Paths.get(request.getContextPath(), path);
        response.sendRedirect(pathWithContextPath.normalize().toString().replace("\\", "/"));
    }

    @GetMapping("/list-data-domain-directories")
    @Operation(summary = "List all data domain directories")
    public List<String> getByPath(HttpServletRequest request) {
        return storageTraverseService.listDataDomainDirectories();
    }
}
