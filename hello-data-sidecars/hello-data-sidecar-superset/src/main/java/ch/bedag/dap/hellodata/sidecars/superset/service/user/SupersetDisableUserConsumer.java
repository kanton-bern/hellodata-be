package ch.bedag.dap.hellodata.sidecars.superset.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserDelete;
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
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<Void> disableUser(SubsystemUserDelete subsystemUserDelete) {
        try {
            log.info("------- Received superset user deletion request {}", subsystemUserDelete);
            SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
            SupersetUsersResponse users = supersetClient.users();
            Optional<SubsystemUser> supersetUserResult = users.getResult().stream().filter(user -> user.getEmail().equalsIgnoreCase(subsystemUserDelete.getEmail())).findFirst();
            if (supersetUserResult.isEmpty()) {
                log.info("User {} doesn't exist in instance, omitting deletion", subsystemUserDelete.getEmail());
                return null;//NOSONAR
            }
            log.info("Going to delete user with email: {}", subsystemUserDelete.getEmail());
            SupersetUserActiveUpdate supersetUserActiveUpdate = new SupersetUserActiveUpdate();
            supersetUserActiveUpdate.setActive(false);
            supersetClient.updateUsersActiveFlag(supersetUserActiveUpdate, supersetUserResult.get().getId());
            userResourceProviderService.publishUsers();
        } catch (URISyntaxException | IOException e) {
            log.error("Could not delete user {}", subsystemUserDelete.getEmail(), e);
        }
        return null;//NOSONAR
    }
}
