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
package ch.bedag.dap.hellodata.portal.metainfo.controller;

import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoResourceService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static ch.bedag.dap.hellodata.commons.security.Permission.USER_MANAGEMENT;
import static ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind.HELLO_DATA_APP_INFO;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/metainfo")
public class MetaInfoResourceController {

    private final MetaInfoResourceService metaInfoResourceService;

    @PreAuthorize("hasAnyAuthority('WORKSPACES')")
    @GetMapping(value = "/resources/filtered/by-app-info")
    public List<HdResource> findAllByAppInfo(@RequestParam String apiVersion, @RequestParam String instanceName, @RequestParam ModuleType moduleType) {
        return metaInfoResourceService.findAllByAppInfo(apiVersion, instanceName, moduleType);
    }

    /**
     * the 'kind' will be with slash included (e.g.: hellodata/AppInfo) so query parameter used instead of path param
     *
     * @param kind (e.g.: hellodata/AppInfo)
     * @return list of resources
     */
    @GetMapping(value = "/kinds/kind/resources")
    public List<HdResource> findAllByKind(@RequestParam String kind) {
        Set<String> currentUserPermissions = SecurityUtils.getCurrentUserPermissions();
        if (HELLO_DATA_APP_INFO.equalsIgnoreCase(kind) || currentUserPermissions.contains(USER_MANAGEMENT.name())) {
            return metaInfoResourceService.findAllByKind(kind);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot fetch data, not enough privileges");
    }
}
