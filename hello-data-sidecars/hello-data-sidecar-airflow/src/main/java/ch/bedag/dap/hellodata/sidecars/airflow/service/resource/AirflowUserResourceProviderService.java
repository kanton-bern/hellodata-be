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

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemGetAllUsers;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUsersResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.service.cloud.PodUtilsProvider;
import ch.bedag.dap.hellodata.sidecars.airflow.service.provider.AirflowClientProvider;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.GET_ALL_USERS;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_USER_RESOURCES;

@Log4j2
@Service
@RequiredArgsConstructor
public class AirflowUserResourceProviderService {

    private final NatsSenderService natsSenderService;
    private final AirflowClientProvider airflowClientProvider;
    private final PodUtilsProvider podUtilsProvider;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @JetStreamSubscribe(event = GET_ALL_USERS)
    public void refreshUsers(SubsystemGetAllUsers subsystemGetAllUsers) throws URISyntaxException, IOException {
        log.info("--> Publish all users event {}", subsystemGetAllUsers);
        publishUsers();
    }

    @Scheduled(fixedDelayString = "${hello-data.sidecar.pubish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void publishUsers() throws URISyntaxException, IOException {
        log.info("--> publishUsers()");
        AirflowUsersResponse response = airflowClientProvider.getAirflowClientInstance().users();

        PodUtils<V1Pod> podUtils = podUtilsProvider.getIfAvailable();
        List<AirflowUserResponse> airflowUsers = CollectionUtils.emptyIfNull(response.getUsers()).stream().toList();
        //ToDo: Remove this conversion to SupersetUsers, should use a generic interface
        List<SubsystemUser> subsystemUsers = toSupsetSetUsers(airflowUsers);
        if (podUtils != null) {
            V1Pod current = podUtils.currentPod().get();
            UserResource userResource = new UserResource(ModuleType.AIRFLOW, this.instanceName, current.getMetadata().getNamespace(), subsystemUsers);
            natsSenderService.publishMessageToJetStream(PUBLISH_USER_RESOURCES, userResource);
        } else {
            //dummy info for tests
            UserResource userResource = new UserResource(ModuleType.AIRFLOW, this.instanceName, "local", subsystemUsers);
            natsSenderService.publishMessageToJetStream(PUBLISH_USER_RESOURCES, userResource);
        }
    }

    /**
     * Since airflow doesn't return an id for a user, we just invent one of our own
     */
    private List<SubsystemUser> toSupsetSetUsers(List<AirflowUserResponse> airflowUsers) {
        if (airflowUsers.isEmpty()) {
            return new ArrayList<>();
        }
        List<AirflowUserResponse> modifiableList = new ArrayList<>(airflowUsers);
        modifiableList.sort(Comparator.comparing(AirflowUserResponse::getCreatedOn));
        return IntStream.range(0, modifiableList.size()).mapToObj(i -> toSubsystemUser(i + 2, modifiableList.get(i))).toList();
    }

    @NotNull
    private SubsystemUser toSubsystemUser(int index, AirflowUserResponse airflowUser) {
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setId(index);
        subsystemUser.setUsername(airflowUser.getUsername());
        subsystemUser.setEmail(airflowUser.getEmail());
        subsystemUser.setFirstName(airflowUser.getFirstName());
        subsystemUser.setLastName(airflowUser.getLastName());
        subsystemUser.setActive(airflowUser.isActive());
        subsystemUser.setRoles(toSupersetRoles(airflowUser.getRoles()));
        return subsystemUser;
    }

    private List<SubsystemRole> toSupersetRoles(List<AirflowUserRole> airflowUserRoles) {
        return airflowUserRoles.stream().map(this::toSupersetRole).toList();
    }

    private SubsystemRole toSupersetRole(AirflowUserRole airflowUserRole) {
        SubsystemRole supersetRole = new SubsystemRole();
        supersetRole.setId(getRoleId(airflowUserRole));
        supersetRole.setName(airflowUserRole.getName());
        return supersetRole;
    }

    /**
     * Some small arbitrary mapping of airflow rolename to an Integer
     */
    private int getRoleId(AirflowUserRole airflowUserRole) {
        return airflowUserRole.getName().equalsIgnoreCase("Admin") ? 1 : 2;
    }
}
