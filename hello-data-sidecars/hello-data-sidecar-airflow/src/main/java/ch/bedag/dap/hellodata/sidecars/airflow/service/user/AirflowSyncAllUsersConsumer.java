package ch.bedag.dap.hellodata.sidecars.airflow.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.AllUsersContextRoleUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.client.AirflowClient;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRole;
import ch.bedag.dap.hellodata.sidecars.airflow.service.provider.AirflowClientProvider;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.List;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.SYNC_ALL_USERS;

@Log4j2
@Service
@AllArgsConstructor
public class AirflowSyncAllUsersConsumer {
    private final AirflowUserContextRoleConsumer airflowUserContextRoleConsumer;
    private final AirflowClientProvider airflowClientProvider;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = SYNC_ALL_USERS, timeoutMinutes = 15L)
    public void subscribe(AllUsersContextRoleUpdate allUsersContextRoleUpdate) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("[SYNC_ALL_USERS] Started all users synchronization");
        List<UserContextRoleUpdate> userContextRoleUpdates = allUsersContextRoleUpdate.getUserContextRoleUpdates();
        for (UserContextRoleUpdate userContextRoleUpdate : userContextRoleUpdates) {
            try {
                AirflowClient airflowClient = airflowClientProvider.getAirflowClientInstance();
                List<AirflowRole> allAirflowRoles = CollectionUtils.emptyIfNull(airflowClient.roles().getRoles()).stream().toList();
                airflowUserContextRoleConsumer.updateUserRoles(userContextRoleUpdate, airflowClient, allAirflowRoles);
            } catch (Exception e) {
                log.error("Could not synchronize user {}", userContextRoleUpdate.getEmail(), e);
            }
        }
        log.info("[SYNC_ALL_USERS] Finished all users synchronization. Operation took {}", stopWatch.formatTime());
    }
}
