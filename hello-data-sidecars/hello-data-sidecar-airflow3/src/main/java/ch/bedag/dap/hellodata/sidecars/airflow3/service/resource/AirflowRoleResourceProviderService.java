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
package ch.bedag.dap.hellodata.sidecars.airflow3.service.resource;

import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.RoleResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.RolePermissions;
import ch.bedag.dap.hellodata.sidecars.airflow3.client.AirflowClient;
import ch.bedag.dap.hellodata.sidecars.airflow3.client.user.response.AirflowRole;
import ch.bedag.dap.hellodata.sidecars.airflow3.client.user.response.AirflowRoleAction;
import ch.bedag.dap.hellodata.sidecars.airflow3.client.user.response.AirflowRolesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_ROLE_RESOURCES;

/** Publishes the Airflow 3 FAB roles (with their permissions) so the portal workspace can show them. */
@Log4j2
@Service
@RequiredArgsConstructor
public class AirflowRoleResourceProviderService {
    private final AirflowClient apiClient;
    private final NatsSenderService natsSenderService;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Scheduled(fixedDelayString = "${hello-data.sidecar.publish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void publishRoles() throws URISyntaxException, IOException {
        log.info("--> publishRoles()");
        AirflowRolesResponse rolesResponse = apiClient.roles();
        List<AirflowRole> roles = new ArrayList<>(rolesResponse.getRoles());
        roles.sort(Comparator.comparing(AirflowRole::getName));
        List<RolePermissions> data = new ArrayList<>();
        int roleIndex = 1;
        for (AirflowRole role : roles) {
            List<RolePermissions.PermissionNameViewMenuName> permissions = new ArrayList<>();
            int permIndex = 1;
            for (AirflowRoleAction action : role.getActions()) {
                if (action.getAction() == null || action.getResource() == null) {
                    continue;
                }
                permissions.add(new RolePermissions.PermissionNameViewMenuName(permIndex++, action.getAction().getName(), action.getResource().getName()));
            }
            data.add(new RolePermissions(roleIndex++, role.getName(), permissions));
        }
        RoleResource roleResource = new RoleResource(this.instanceName, ModuleType.AIRFLOW3, data);
        natsSenderService.publishMessageToJetStream(PUBLISH_ROLE_RESOURCES, roleResource);
    }
}
