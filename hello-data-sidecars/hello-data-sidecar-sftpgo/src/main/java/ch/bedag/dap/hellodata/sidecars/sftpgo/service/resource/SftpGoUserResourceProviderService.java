package ch.bedag.dap.hellodata.sidecars.sftpgo.service.resource;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemGetAllUsers;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.User;
import ch.bedag.dap.hellodata.sidecars.sftpgo.service.SftpGoService;
import ch.bedag.dap.hellodata.sidecars.sftpgo.service.cloud.PodUtilsProvider;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.GET_ALL_USERS;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_USER_RESOURCES;
import static ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.User.StatusEnum.NUMBER_1;

@Log4j2
@Service
@RequiredArgsConstructor
public class SftpGoUserResourceProviderService {
    private final NatsSenderService natsSenderService;
    private final SftpGoService sftpGoService;
    private final PodUtilsProvider podUtilsProvider;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @JetStreamSubscribe(event = GET_ALL_USERS)
    public void refreshUsers(SubsystemGetAllUsers subsystemGetAllUsers) throws URISyntaxException, IOException {
        log.info("--> Publish all users event {}", subsystemGetAllUsers);
        publishUsers();
    }

    @Scheduled(fixedDelayString = "${hello-data.sidecar.pubish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void publishUsers() {
        log.info("--> publishUsers()");
        List<User> allUsers = sftpGoService.getAllUsers();

        PodUtils<V1Pod> podUtils = podUtilsProvider.getIfAvailable();
        List<SubsystemUser> subsystemUsers = toSubsystemUsers(allUsers);
        if (podUtils != null) {
            V1Pod current = podUtils.currentPod().get();
            UserResource userResource = new UserResource(ModuleType.SFTPGO, this.instanceName, current.getMetadata().getNamespace(), subsystemUsers);
            natsSenderService.publishMessageToJetStream(PUBLISH_USER_RESOURCES, userResource);
        } else {
            //dummy info for tests
            UserResource userResource = new UserResource(ModuleType.SFTPGO, this.instanceName, "local", subsystemUsers);
            natsSenderService.publishMessageToJetStream(PUBLISH_USER_RESOURCES, userResource);
        }
    }

    private List<SubsystemUser> toSubsystemUsers(List<User> allUsers) {
        return allUsers.stream().map(this::toSubsystemUser).toList();
    }

    private SubsystemUser toSubsystemUser(User user) {
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setActive(user.getStatus() == NUMBER_1);
        subsystemUser.setEmail(user.getEmail());
        subsystemUser.setId(user.getId());
        subsystemUser.setUsername(user.getUsername());
        subsystemUser.setRoles(user.getGroups().stream().map(groupMapping -> {
            SubsystemRole role = new SubsystemRole();
            role.setName(groupMapping.getName());
            return role;
        }).toList());
        return subsystemUser;
    }
}
