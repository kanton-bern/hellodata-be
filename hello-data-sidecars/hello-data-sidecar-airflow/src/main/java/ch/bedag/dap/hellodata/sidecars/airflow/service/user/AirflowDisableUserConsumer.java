package ch.bedag.dap.hellodata.sidecars.airflow.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.client.AirflowClient;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUsersResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.service.provider.AirflowClientProvider;
import ch.bedag.dap.hellodata.sidecars.airflow.service.resource.AirflowUserResourceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
    @JetStreamSubscribe(event = DISABLE_USER)
    public CompletableFuture<Void> disableUser(SubsystemUserUpdate supersetUserUpdate) {
        try {
            log.info("------- Received airflow user disable request {}", supersetUserUpdate);

            AirflowClient airflowClient = airflowClientProvider.getAirflowClientInstance();
            AirflowUsersResponse users = airflowClient.users();
            List<AirflowRole> allAirflowRoles = CollectionUtils.emptyIfNull(airflowClient.roles().getRoles()).stream().toList();

            // Airflow only allows unique username and email, so we make sure there is nobody with either of these already existing, before creating a new one
            Optional<AirflowUserResponse> userResult = users.getUsers()
                    .stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(supersetUserUpdate.getEmail()) ||
                            user.getUsername().equalsIgnoreCase(supersetUserUpdate.getUsername()))
                    .findFirst();

            if (userResult.isPresent()) {
                AirflowUserResponse airflowUser = userResult.get();
                removeRoleFromUser(airflowUser, ADMIN_ROLE_NAME, allAirflowRoles);
                removeRoleFromUser(airflowUser, VIEWER_ROLE_NAME, allAirflowRoles);
                removeRoleFromUser(airflowUser, AF_OPERATOR_ROLE_NAME, allAirflowRoles);
                removeAllDataDomainRolesFromUser(airflowUser);
                addRoleToUser(airflowUser, PUBLIC_ROLE_NAME, allAirflowRoles);
                updateUser(airflowUser, airflowClient, airflowUserResourceProviderService);
                userResourceProviderService.publishUsers();
                log.info("User with email: {} disabled", supersetUserUpdate.getEmail());
            } else {
                log.warn("User with email: {} not found", supersetUserUpdate.getEmail());
            }
        } catch (URISyntaxException | IOException e) {
            log.error("Could not disable user {}", supersetUserUpdate.getEmail(), e);
        }
        return null;//NOSONAR
    }

}
