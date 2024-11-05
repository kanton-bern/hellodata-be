package ch.bedag.dap.hellodata.sidecars.superset.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUsersResponse;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import ch.bedag.dap.hellodata.sidecars.superset.service.resource.UserResourceProviderService;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserActiveUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.DISABLE_USER;

@Log4j2
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S3516")
public class SupersetDisableUserConsumer {

    private final UserResourceProviderService userResourceProviderService;
    private final SupersetClientProvider supersetClientProvider;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = DISABLE_USER)
    public void disableUser(SubsystemUserUpdate subsystemUserUpdate) {
        try {
            log.info("------- Received superset user disable request {}", subsystemUserUpdate);
            SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
            SupersetUsersResponse users = supersetClient.users();
            Optional<SubsystemUser> supersetUserResult = users.getResult().stream().filter(user -> user.getEmail().equalsIgnoreCase(subsystemUserUpdate.getEmail())).findFirst();
            if (supersetUserResult.isEmpty()) {
                log.info("User {} doesn't exist in instance, omitting disable action", subsystemUserUpdate.getEmail());
                return;
            }
            SupersetUserActiveUpdate supersetUserActiveUpdate = new SupersetUserActiveUpdate();
            supersetUserActiveUpdate.setActive(false);
            supersetClient.updateUsersActiveFlag(supersetUserActiveUpdate, supersetUserResult.get().getId());
            userResourceProviderService.publishUsers();
            log.info("User with email: {} disabled", subsystemUserUpdate.getEmail());
        } catch (URISyntaxException | IOException e) {
            log.error("Could not disable user {}", subsystemUserUpdate.getEmail(), e);
        }
    }
}
