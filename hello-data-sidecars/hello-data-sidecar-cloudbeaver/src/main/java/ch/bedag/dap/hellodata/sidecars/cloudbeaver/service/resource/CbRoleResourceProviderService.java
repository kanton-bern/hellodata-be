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
package ch.bedag.dap.hellodata.sidecars.cloudbeaver.service.resource;

import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.RoleResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.RolePermissions;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.RoleRepository;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.Privilege;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.Role;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
public class CbRoleResourceProviderService {
    private final RoleRepository roleRepository;
    private final ObjectProvider<DiscoveryClient> discoveryClientObjectProvider;
    private final ObjectProvider<PodUtils<V1Pod>> podUtilsObjectProvider;
    private final NatsSenderService natsSenderService;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Scheduled(fixedDelayString = "${hello-data.sidecar.publish-interval-seconds:300}", timeUnit = TimeUnit.SECONDS)
    public void publishRoles() {
        log.info("--> publishRoles()");

        DiscoveryClient discoveryClient = this.discoveryClientObjectProvider.getIfAvailable();
        if (discoveryClient != null) {
            discoveryClient.description();
            discoveryClient.getServices();
        }
        List<RolePermissions> data = getRolePermissions();
        PodUtils<V1Pod> podUtils = podUtilsObjectProvider.getIfAvailable();
        if (podUtils != null) {
            V1Pod current = podUtils.currentPod().get();
            RoleResource roleResource = new RoleResource(this.instanceName, current.getMetadata().getNamespace(), ModuleType.CLOUDBEAVER, data);
            natsSenderService.publishMessageToJetStream(PUBLISH_ROLE_RESOURCES, roleResource);
        } else {
            //dummy info for tests
            RoleResource roleResource = new RoleResource(this.instanceName, "local", ModuleType.CLOUDBEAVER, data);
            natsSenderService.publishMessageToJetStream(PUBLISH_ROLE_RESOURCES, roleResource);
        }
    }

    private List<RolePermissions> getRolePermissions() {
        List<RolePermissions> data = new ArrayList<>();
        List<Role> cbRoles = roleRepository.findAll();
        cbRoles.sort(Comparator.comparing(Role::getName));
        AtomicInteger atomicIndex = new AtomicInteger(1);
        cbRoles.forEach(role -> {
            List<Privilege> privileges = new ArrayList<>(role.getPrivileges());
            List<RolePermissions.PermissionNameViewMenuName> permissions =
                    IntStream.range(0, privileges.size()).mapToObj(i -> toSupersetPermissionNameViewMenuName(i, privileges.get(i), role.getKey())).toList();
            int index = atomicIndex.getAndIncrement();
            data.add(new RolePermissions(index, role.getName(), permissions));
        });
        return data;
    }

    private RolePermissions.PermissionNameViewMenuName toSupersetPermissionNameViewMenuName(int index, Privilege privilege, String projectKey) {
        return new RolePermissions.PermissionNameViewMenuName(index + 1, privilege.getName(), projectKey);
    }
}
