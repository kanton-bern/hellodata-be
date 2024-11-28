package ch.bedag.dap.hellodata.sidecars.airflow.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.client.AirflowClient;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.service.provider.AirflowClientProvider;
import ch.bedag.dap.hellodata.sidecars.airflow.service.resource.AirflowUserResourceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.DISABLE_USER;
import static ch.bedag.dap.hellodata.sidecars.airflow.service.user.AirflowUserUtil.*;

@Log4j2
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S3516")
public class AirflowDisableUserConsumer {

    private final AirflowUserResourceProviderService userResourceProviderService;
    private final AirflowClientProvider airflowClientProvider;
    private final AirflowUserResourceProviderService airflowUserResourceProviderService;


    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = DISABLE_USER, asyncRun = false)
    public void disableUser(SubsystemUserUpdate supersetUserUpdate) {
        try {
            log.info("------- Received airflow user disable request {}", supersetUserUpdate);
            AirflowClient airflowClient = airflowClientProvider.getAirflowClientInstance();
            AirflowUserResponse airflowUser = airflowClient.getUser(supersetUserUpdate.getUsername());
            List<AirflowRole> allAirflowRoles = CollectionUtils.emptyIfNull(airflowClient.roles().getRoles()).stream().toList();
            if (airflowUser != null) {
                removeRoleFromUser(airflowUser, ADMIN_ROLE_NAME, allAirflowRoles);
                removeRoleFromUser(airflowUser, VIEWER_ROLE_NAME, allAirflowRoles);
                removeRoleFromUser(airflowUser, AF_OPERATOR_ROLE_NAME, allAirflowRoles);
                removeAllDataDomainRolesFromUser(airflowUser);
                addRoleToUser(airflowUser, PUBLIC_ROLE_NAME, allAirflowRoles);
                updateUser(airflowUser, airflowClient, airflowUserResourceProviderService, supersetUserUpdate.isSendBackUsersList());
                log.info("User with email: {} disabled", supersetUserUpdate.getEmail());
            } else {
                log.warn("User with email: {} not found", supersetUserUpdate.getEmail());
            }
        } catch (URISyntaxException | IOException e) {
            log.error("Could not disable user {}", supersetUserUpdate.getEmail(), e);
        }
    }

}
