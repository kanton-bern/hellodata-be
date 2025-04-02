package ch.bedag.dap.hellodata.sidecars.cloudbeaver.listener;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.AllUsersContextRoleUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.List;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.SYNC_USERS;

@Log4j2
@Service
@AllArgsConstructor
public class CbSyncUsersConsumer {
    private final CbUserContextRoleConsumer cbUserContextRoleConsumer;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = SYNC_USERS, timeoutMinutes = 15L)
    public void subscribe(AllUsersContextRoleUpdate allUsersContextRoleUPdate) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("[SYNC_USERS] Started users synchronization");
        List<UserContextRoleUpdate> userContextRoleUpdates = allUsersContextRoleUPdate.getUserContextRoleUpdates();
        for (UserContextRoleUpdate userContextRoleUpdate : userContextRoleUpdates) {
            try {
                cbUserContextRoleConsumer.processContextRoleUpdate(userContextRoleUpdate);
            } catch (Exception e) {
                log.error("Could not synchronize user {}", userContextRoleUpdate.getEmail(), e);
            }
        }
        log.info("[SYNC_USERS] Finished users synchronization. Operation took {}", stopWatch.formatTime());
    }
}
