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
package ch.bedag.dap.hellodata.sidecars.superset.service.resource;

import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.RoleResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.RolePermissions;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolePermissionsResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolesResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_ROLE_RESOURCES;

@Log4j2
@Service
@RequiredArgsConstructor
public class RoleResourceProviderService {
    private final NatsSenderService natsSenderService;
    private final SupersetClientProvider supersetClientProvider;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Scheduled(fixedDelayString = "${hello-data.sidecar.publish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void publishRoles() throws URISyntaxException, IOException {
        log.info("--> publishRoles()");
        List<RolePermissions> data = getRolePermissions();

        RoleResource roleResource = new RoleResource(this.instanceName, ModuleType.SUPERSET, data);
        natsSenderService.publishMessageToJetStream(PUBLISH_ROLE_RESOURCES, roleResource);
    }

    private static final int BATCH_SIZE = 40; // stay well under Superset's 50 req/s rate limit
    private static final long BATCH_PAUSE_MS = 1100; // pause between batches to let the rate-limit window reset

    private List<RolePermissions> getRolePermissions() throws URISyntaxException, IOException {
        List<RolePermissions> data = new ArrayList<>();
        SupersetClient supersetClientInstance = supersetClientProvider.getSupersetClientInstance();
        SupersetRolesResponse roles = supersetClientInstance.roles();
        var roleList = roles.getResult();
        for (int i = 0; i < roleList.size(); i++) {
            if (i > 0 && i % BATCH_SIZE == 0) {
                log.debug("Pausing for {} ms after {} role-permission requests to avoid Superset rate limit", BATCH_PAUSE_MS, BATCH_SIZE);
                try {
                    Thread.sleep(BATCH_PAUSE_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while throttling role-permission requests", e); //NOSONAR
                }
            }
            var role = roleList.get(i);
            try {
                SupersetRolePermissionsResponse supersetRolePermissionsResponse = supersetClientInstance.rolePermissions(role.getId());
                List<RolePermissions.PermissionNameViewMenuName> permissions = supersetRolePermissionsResponse.getResult()
                        .stream()
                        .map(permission -> new RolePermissions.PermissionNameViewMenuName(
                                permission.getId(), permission.getPermissionName(),
                                permission.getViewMenuName()))
                        .toList();
                data.add(new RolePermissions(role.getId(), role.getName(), permissions));
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e); //NOSONAR
            }
        }
        return data;
    }
}
