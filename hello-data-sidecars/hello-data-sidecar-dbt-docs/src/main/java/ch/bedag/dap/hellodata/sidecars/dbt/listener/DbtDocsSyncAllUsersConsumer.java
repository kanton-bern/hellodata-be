package ch.bedag.dap.hellodata.sidecars.dbt.listener;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.AllUsersContextRoleUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.List;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.SYNC_ALL_USERS;

@Log4j2
@Service
@AllArgsConstructor
public class DbtDocsSyncAllUsersConsumer {
    private final DbtDocsUserContextRoleConsumer dbtDocsUserContextRoleConsumer;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = SYNC_ALL_USERS, timeoutMinutes = 15L)
    public void subscribe(AllUsersContextRoleUpdate allUsersContextRoleUpdate) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("[SYNC_ALL_USERS] Started all users synchronization");
        List<UserContextRoleUpdate> userContextRoleUpdates = allUsersContextRoleUpdate.getUserContextRoleUpdates();
        for (UserContextRoleUpdate userContextRoleUpdate : userContextRoleUpdates) {
            try {
                dbtDocsUserContextRoleConsumer.subscribe(userContextRoleUpdate);
            } catch (Exception e) {
                log.error("Could not synchronize user {}", userContextRoleUpdate.getEmail(), e);
            }
        }
        log.info("[SYNC_ALL_USERS] Finished all users synchronization. Operation took {}", stopWatch.formatTime());
    }
}
