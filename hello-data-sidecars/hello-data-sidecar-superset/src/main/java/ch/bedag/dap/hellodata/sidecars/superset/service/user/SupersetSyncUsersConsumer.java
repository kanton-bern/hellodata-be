package ch.bedag.dap.hellodata.sidecars.superset.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolesResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UsersContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.List;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.SYNC_USERS;

@Log4j2
@Service
@AllArgsConstructor
public class SupersetSyncUsersConsumer {

    private final SupersetUpdateUserContextRoleConsumer supersetUpdateUserContextRoleConsumer;
    private final SupersetClientProvider supersetClientProvider;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = SYNC_USERS, timeoutMinutes = 15L)
    public void subscribe(UsersContextRoleUpdate usersContextRoleUpdate) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("[SYNC_USERS] Started users synchronization");
        List<UserContextRoleUpdate> userContextRoleUpdates = usersContextRoleUpdate.getUserContextRoleUpdates();
        for (UserContextRoleUpdate userContextRoleUpdate : userContextRoleUpdates) {
            try {
                SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
                SupersetRolesResponse allRoles = supersetClient.roles();
                supersetUpdateUserContextRoleConsumer.updateUserRoles(userContextRoleUpdate, supersetClient, allRoles);
            } catch (Exception e) {
                log.error("Could not synchronize username {}, email {}", userContextRoleUpdate.getUsername(), userContextRoleUpdate.getEmail(), e);
            }
        }
        log.info("[SYNC_USERS] Finished all users synchronization. Operation took {}", stopWatch.formatTime());
    }
}
