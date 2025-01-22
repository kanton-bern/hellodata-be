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

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemGetAllUsers;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.Role;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.User;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.UserRepository;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.service.cloud.PodUtilsProvider;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.GET_ALL_USERS;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_USER_RESOURCES;

@Log4j2
@Service
@RequiredArgsConstructor
public class CbUserResourceProviderService {

    private final UserRepository userRepository;
    private final NatsSenderService natsSenderService;
    private final PodUtilsProvider podUtilsProvider;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @JetStreamSubscribe(event = GET_ALL_USERS)
    public void refreshUsers(SubsystemGetAllUsers subsystemGetAllUsers) throws URISyntaxException, IOException {
        log.info("--> Publish all users event {}", subsystemGetAllUsers);
        publishUsers();
    }

    @Scheduled(fixedDelayString = "${hello-data.sidecar.pubish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    @Transactional(readOnly = true)
    public void publishUsers() {
        log.info("--> publishUsers()");
        PodUtils<V1Pod> podUtils = podUtilsProvider.getIfAvailable();
        //ToDo: Remove this conversion to SupersetUsers, should use a generic interface
        List<User> users = userRepository.findAll();
        List<SubsystemUser> cbUsers = toSubsystemSetUser(users);
        if (podUtils != null) {
            V1Pod current = podUtils.currentPod().get();
            UserResource userResource = new UserResource(ModuleType.CLOUDBEAVER, this.instanceName, current.getMetadata().getNamespace(), cbUsers);
            natsSenderService.publishMessageToJetStream(PUBLISH_USER_RESOURCES, userResource);
        } else {
            //dummy info for tests
            UserResource userResource = new UserResource(ModuleType.CLOUDBEAVER, this.instanceName, "local", cbUsers);
            natsSenderService.publishMessageToJetStream(PUBLISH_USER_RESOURCES, userResource);
        }
    }

    /**
     * Since dbt doesn't return an id for a user, we just invent one of our own
     */
    private List<SubsystemUser> toSubsystemSetUser(List<User> users) {
        if (users.isEmpty()) {
            return new ArrayList<>();
        }
        List<User> modifiableList = new ArrayList<>(users);
        modifiableList.sort(Comparator.comparing(User::getCreatedDate));
        return IntStream.range(0, modifiableList.size()).mapToObj(i -> toSubsystemUser(i + 2, modifiableList.get(i))).collect(Collectors.toList());
    }

    @NotNull
    private SubsystemUser toSubsystemUser(int index, User dbtUser) {
        SubsystemUser supersetUser = new SubsystemUser();
        supersetUser.setId(index);
        supersetUser.setUsername(dbtUser.getUserName());
        supersetUser.setEmail(dbtUser.getEmail());
        supersetUser.setFirstName(dbtUser.getFirstName());
        supersetUser.setLastName(dbtUser.getLastName());
        supersetUser.setActive(dbtUser.isEnabled());
        supersetUser.setRoles(toSupersetRoles(dbtUser.getRoles()));
        return supersetUser;
    }

    private List<SubsystemRole> toSupersetRoles(Collection<Role> dbtUserRoles) {
        return dbtUserRoles.stream().map(this::toSupersetRole).collect(Collectors.toList());
    }

    private SubsystemRole toSupersetRole(Role dbtRole) {
        SubsystemRole supersetRole = new SubsystemRole();
        supersetRole.setId(getRoleId(dbtRole));
        supersetRole.setName(dbtRole.getName());
        return supersetRole;
    }

    /**
     * Some small arbitrary mapping of dbt rolename to an Integer
     */
    private int getRoleId(Role airflowUserRole) {
        return airflowUserRole.getKey().equalsIgnoreCase(Role.ADMIN_ROLE_KEY) ? 1 : 2;
    }
}
