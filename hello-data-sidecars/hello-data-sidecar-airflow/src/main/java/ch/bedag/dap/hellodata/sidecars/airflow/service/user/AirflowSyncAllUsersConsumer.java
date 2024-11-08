package ch.bedag.dap.hellodata.sidecars.airflow.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.AllUsersContextRoleUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.SYNC_ALL_USERS;

@Log4j2
@Service
@AllArgsConstructor
public class AirflowSyncAllUsersConsumer {
    private final AirflowUserContextRoleConsumer airflowUserContextRoleConsumer;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = SYNC_ALL_USERS)
    public void subscribe(AllUsersContextRoleUpdate allUsersContextRoleUpdate) {
        List<UserContextRoleUpdate> userContextRoleUpdates = allUsersContextRoleUpdate.getUserContextRoleUpdates();
        for (UserContextRoleUpdate userContextRoleUpdate : userContextRoleUpdates) {
            try {
                airflowUserContextRoleConsumer.subscribe(userContextRoleUpdate);
            } catch (Exception e) {
                log.error("Could not synchronize user {}", userContextRoleUpdate.getEmail(), e);
            }
        }
    }
}
