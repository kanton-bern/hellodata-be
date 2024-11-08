package ch.bedag.dap.hellodata.sidecars.superset.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolesResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.AllUsersContextRoleUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.List;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.SYNC_ALL_USERS;

@Log4j2
@Service
@AllArgsConstructor
public class SupersetSyncAllUsersConsumer {

    private final SupersetUpdateUserContextRoleConsumer supersetUpdateUserContextRoleConsumer;
    private final SupersetClientProvider supersetClientProvider;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = SYNC_ALL_USERS, timeoutMinutes = 15L)
    public void subscribe(AllUsersContextRoleUpdate allUsersContextRoleUpdate) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("[SYNC_ALL_USERS] Started all users synchronization");
        List<UserContextRoleUpdate> userContextRoleUpdates = allUsersContextRoleUpdate.getUserContextRoleUpdates();
        for (UserContextRoleUpdate userContextRoleUpdate : userContextRoleUpdates) {
            try {
                SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
                SupersetRolesResponse allRoles = supersetClient.roles();
                supersetUpdateUserContextRoleConsumer.updateUserRoles(userContextRoleUpdate, supersetClient, allRoles);
            } catch (Exception e) {
                log.error("Could not synchronize user {}", userContextRoleUpdate.getEmail(), e);
            }
        }
        log.info("[SYNC_ALL_USERS] Finished all users synchronization. Operation took {}", stopWatch.formatTime());
    }
}
