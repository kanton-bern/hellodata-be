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
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.permission.PermissionResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.permission.response.superset.SupersetPermission;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowPermissionsResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.service.provider.AirflowClientProvider;
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
import java.util.stream.IntStream;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_PERMISSION_RESOURCES;

@Log4j2
@Service
@RequiredArgsConstructor
public class AirflowPermissionResourceProviderService {
    private final NatsSenderService natsSenderService;
    private final AirflowClientProvider airflowClientProvider;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Scheduled(fixedDelayString = "${hello-data.sidecar.publish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void publishPermissions() throws URISyntaxException, IOException {
        log.info("--> publishPermissions()");
        AirflowPermissionsResponse response = airflowClientProvider.getAirflowClientInstance().permissions();

        List<AirflowPermissionsResponse.Action> airflowPermissions = response.getActions();
        List<SupersetPermission> supersetPermissions = toSupersetPermissions(airflowPermissions);
        PermissionResource permissionResource = new PermissionResource(ModuleType.AIRFLOW, this.instanceName, supersetPermissions);
        natsSenderService.publishMessageToJetStream(PUBLISH_PERMISSION_RESOURCES, permissionResource);
    }

    private List<SupersetPermission> toSupersetPermissions(List<AirflowPermissionsResponse.Action> airflowPermissions) {
        if (airflowPermissions.isEmpty()) {
            return new ArrayList<>();
        }
        airflowPermissions.sort(Comparator.comparing(AirflowPermissionsResponse.Action::getName));
        return IntStream.range(0, airflowPermissions.size()).mapToObj(i -> toSupersetPermission(i, airflowPermissions.get(i))).toList();
    }

    private SupersetPermission toSupersetPermission(int index, AirflowPermissionsResponse.Action airflowAction) {
        SupersetPermission supersetPermission = new SupersetPermission();
        supersetPermission.setId(index + 1);
        supersetPermission.setName(airflowAction.getName());
        return supersetPermission;
    }
}
