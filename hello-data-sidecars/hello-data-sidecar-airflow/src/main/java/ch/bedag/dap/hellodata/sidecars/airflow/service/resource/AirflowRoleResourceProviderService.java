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
package ch.bedag.dap.hellodata.sidecars.airflow.service.resource;

import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.RoleResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.RolePermissions;
import ch.bedag.dap.hellodata.sidecars.airflow.client.AirflowClient;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRoleAction;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRolesResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.service.provider.AirflowClientProvider;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_ROLE_RESOURCES;

@Log4j2
@Service
@RequiredArgsConstructor
public class AirflowRoleResourceProviderService {
    private final ObjectProvider<DiscoveryClient> discoveryClientObjectProvider;
    private final ObjectProvider<PodUtils<V1Pod>> podUtilsObjectProvider;
    private final NatsSenderService natsSenderService;
    private final AirflowClientProvider airflowClientProvider;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Scheduled(fixedDelayString = "${hello-data.sidecar.pubish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void publishRoles() throws URISyntaxException, IOException {
        log.info("--> publishRoles()");
        List<RolePermissions> data = getRolePermissions();

        DiscoveryClient discoveryClient = this.discoveryClientObjectProvider.getIfAvailable();
        if (discoveryClient != null) {
            discoveryClient.description();
            discoveryClient.getServices();
        }
        PodUtils<V1Pod> podUtils = podUtilsObjectProvider.getIfAvailable();
        if (podUtils != null) {
            V1Pod current = podUtils.currentPod().get();
            RoleResource roleResource = new RoleResource(this.instanceName, current.getMetadata().getNamespace(), ModuleType.AIRFLOW, data);
            natsSenderService.publishMessageToJetStream(PUBLISH_ROLE_RESOURCES, roleResource);
        } else {
            //dummy info for tests
            RoleResource roleResource = new RoleResource(this.instanceName, "local", ModuleType.AIRFLOW, data);
            natsSenderService.publishMessageToJetStream(PUBLISH_ROLE_RESOURCES, roleResource);
        }
    }

    private List<RolePermissions> getRolePermissions() throws URISyntaxException, IOException {
        List<RolePermissions> data = new ArrayList<>();
        AirflowClient airflowClientInstance = airflowClientProvider.getAirflowClientInstance();
        AirflowRolesResponse rolesResponse = airflowClientInstance.roles();
        List<AirflowRole> airflowRoles = rolesResponse.getRoles();
        airflowRoles.sort(Comparator.comparing(AirflowRole::getName));
        AtomicInteger atomicIndex = new AtomicInteger(1);
        airflowRoles.forEach(role -> {
            List<AirflowRoleAction> roleActions = role.getActions();
            List<RolePermissions.PermissionNameViewMenuName> permissions =
                    IntStream.range(0, roleActions.size()).mapToObj(i -> toSupersetPermissionNameViewMenuName(i, roleActions.get(i))).toList();
            int index = atomicIndex.getAndIncrement();
            data.add(new RolePermissions(index, role.getName(), permissions));
        });
        return data;
    }

    private RolePermissions.PermissionNameViewMenuName toSupersetPermissionNameViewMenuName(int index, AirflowRoleAction airflowRoleAction) {
        return new RolePermissions.PermissionNameViewMenuName(index + 1, airflowRoleAction.getAction().getName(), airflowRoleAction.getResource().getName());
    }
}
