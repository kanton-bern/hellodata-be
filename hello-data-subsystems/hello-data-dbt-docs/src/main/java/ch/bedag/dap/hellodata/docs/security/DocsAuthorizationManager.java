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
package ch.bedag.dap.hellodata.docs.security;

import ch.bedag.dap.hellodata.docs.entities.Privilege;
import ch.bedag.dap.hellodata.docs.entities.Role;
import ch.bedag.dap.hellodata.docs.entities.User;
import ch.bedag.dap.hellodata.docs.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.ObjectUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

@Log4j2
@RequiredArgsConstructor
public class DocsAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String GET_PROJECTS_DOCS_BY_PATH_URI = "/api/projects-docs/get-by-path";

    private final SecurityService securityService;

    private static String getRequestParameterPath(@NotNull HttpServletRequest request) {
        String[] parameterValues = request.getParameterValues("path");
        if (parameterValues.length == 1) {
            return parameterValues[0];
        }
        return "";
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        log.info("Request uri: {}", request.getRequestURI());
        log.info("Request url: {}", request.getRequestURL());
        log.info("Server context-path: {}", request.getContextPath());

        Authentication authentication = authenticationSupplier.get();
        if (authentication instanceof DbtDocsAuthenticationToken) {
            User user = (User) authentication.getPrincipal();
            if (user == null) {
                return new AuthorizationDecision(false);
            }
            String preferredUsername = user.getUserName();
            String email = user.getEmail();
            log.info("Received Principal with username {} and email {}. Requesting Uri: {}", preferredUsername, email,
                request.getRequestURI());
            if (requestedEndpoint(request, GET_PROJECTS_DOCS_BY_PATH_URI)) {
                String requestParameterPath = getRequestParameterPath(request);
                String requestedProject = getRequestedProject(requestParameterPath);
                log.debug("Requested project: {}", requestedProject);
                return authorizeRequestedProject(user, requestedProject);
            } else if (!requestedEndpoint(request, "/api")) {
                String projectName = getProjectName(request);
                log.info("Received project name url request {}", projectName);
                if (!securityService.canAccessProject(user.getRoles(), projectName)) {
                    return new AuthorizationDecision(false);
                }
            }

            return new AuthorizationDecision(true);
        }
        return new AuthorizationDecision(false);
    }

    private boolean requestedEndpoint(@NotNull HttpServletRequest request, String endpointPath) {
        Path endpointPathWithContextPath = Paths.get(request.getContextPath(), endpointPath);
        Path requestedUriPath = Paths.get(request.getRequestURI().toLowerCase());
        log.info("endpointPathWithContextPath: {}", endpointPathWithContextPath);
        log.info("requestedUriPath: {}", requestedUriPath);
        log.info("Startswith? {}", requestedUriPath.startsWith(endpointPathWithContextPath));
        return requestedUriPath.startsWith(endpointPathWithContextPath);
    }

    private static String getProjectName(HttpServletRequest request) {
        String[] parts = request.getRequestURI().split("/", 4);
        if (request.getContextPath().isEmpty()) {
            return parts.length > 1 ? parts[1] : "";
        }
        return parts.length > 2 ? parts[2] : "";
    }

    @NotNull
    private AuthorizationDecision authorizeRequestedProject(User user, String projectName) {
        if (ObjectUtils.isEmpty(projectName)) {
            return new AuthorizationDecision(false);
        }
        boolean isAuthorized = isUserAuthorizedOnProject(user, projectName);
        return new AuthorizationDecision(isAuthorized);
    }

    public boolean isUserAuthorizedOnProject(User user, String projectName) {
        if (user.getRoles().stream().anyMatch(r -> r.getKey().equals(Role.ADMIN_ROLE_KEY))) {
            return true;
        }
        return user.getRoles()
            .stream()
            .filter(Role::isEnabled)
            .anyMatch(r -> r.getKey().equalsIgnoreCase(projectName) && r.getPrivileges().stream().anyMatch(p -> p.getName().equals(Privilege.READ_PRIVILEGE)));
    }

    private String getRequestedProject(@NotNull String path) {
        log.info("Request path: " + path);
        String[] split = path.split("/");
        if (split.length == 0) {
            return null;
        }
        if (split[0].isBlank() && split.length > 1) {
            return split[1];
        }
        return null;
    }
}
