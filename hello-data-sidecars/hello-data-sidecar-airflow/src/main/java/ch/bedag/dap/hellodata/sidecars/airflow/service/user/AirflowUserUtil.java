package ch.bedag.dap.hellodata.sidecars.airflow.service.user;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.client.AirflowClient;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.*;
import ch.bedag.dap.hellodata.sidecars.airflow.service.resource.AirflowUserResourceProviderService;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@UtilityClass
public class AirflowUserUtil {

    public static final String PUBLIC_ROLE_NAME = "Public";
    public static final String DATA_DOMAIN_ROLE_PREFIX = "DD_";
    public static final String VIEWER_ROLE_NAME = "Viewer";
    public static final String ADMIN_ROLE_NAME = "Admin";
    public static final String AF_OPERATOR_ROLE_NAME = "AF_OPERATOR";

    public static AirflowUser toAirflowUser(SubsystemUserUpdate supersetUserCreate) {
        AirflowUser airflowUser = new AirflowUser();
        airflowUser.setEmail(supersetUserCreate.getEmail());
        airflowUser.setRoles(new ArrayList<>()); // Default User-Roles are defined in Airflow-Config
        airflowUser.setUsername(supersetUserCreate.getUsername());
        airflowUser.setFirstName(supersetUserCreate.getFirstName());
        airflowUser.setLastName(supersetUserCreate.getLastName());
        airflowUser.setPassword(supersetUserCreate.getUsername()); // Login will be handled by Keycloak
        return airflowUser;
    }

    public static void addRoleToUser(AirflowUserResponse airflowUser, String roleName, List<AirflowRole> allAirflowRoles) {
        Optional<AirflowUserRole> roleResult = CollectionUtils.emptyIfNull(allAirflowRoles)
                .stream()
                .filter(airflowRole -> airflowRole.getName().equalsIgnoreCase(roleName))
                .map(airflowRole -> new AirflowUserRole(airflowRole.getName()))
                .findFirst();
        if (roleResult.isPresent()) {
            AirflowUserRole airflowUserRoleToBeAdded = roleResult.get();
            if (!CollectionUtils.emptyIfNull(airflowUser.getRoles()).contains(airflowUserRoleToBeAdded)) {
                airflowUser.getRoles().add(airflowUserRoleToBeAdded);
            }
        }
    }

    public void removeRoleFromUser(AirflowUserResponse airflowUser, String name, List<AirflowRole> allAirflowRoles) {
        Optional<AirflowUserRole> roleResult = CollectionUtils.emptyIfNull(allAirflowRoles)
                .stream()
                .filter(airflowUserRole -> airflowUserRole.getName().equalsIgnoreCase(name))
                .map(airflowRole -> new AirflowUserRole(airflowRole.getName()))
                .findFirst();
        if (roleResult.isPresent()) {
            AirflowUserRole airflowUserRoleToBeRemoved = roleResult.get();
            if (CollectionUtils.emptyIfNull(airflowUser.getRoles()).contains(airflowUserRoleToBeRemoved)) {
                airflowUser.getRoles().remove(airflowUserRoleToBeRemoved);
            }
        }
    }

    public static void updateUser(AirflowUserResponse airflowUser, AirflowClient airflowClient, AirflowUserResourceProviderService airflowUserResourceProviderService, boolean sendBackUsersList) throws IOException, URISyntaxException {
        if (airflowUser == null) {
            return;
        }
        AirflowUserRolesUpdate airflowUserRolesUpdate = new AirflowUserRolesUpdate();
        airflowUserRolesUpdate.setRoles(airflowUser.getRoles());
        airflowUserRolesUpdate.setEmail(airflowUser.getEmail());
        airflowUserRolesUpdate.setUsername(airflowUser.getUsername());
        airflowUserRolesUpdate.setPassword(airflowUser.getPassword() == null ? "" : airflowUser.getPassword());
        airflowUserRolesUpdate.setFirstName(airflowUser.getFirstName());
        airflowUserRolesUpdate.setLastName(airflowUser.getLastName());
        airflowClient.updateUser(airflowUserRolesUpdate, airflowUser.getUsername());
        if (sendBackUsersList) {
            airflowUserResourceProviderService.publishUsers();
        }
    }

    public static AirflowUserResponse getAirflowUser(UserContextRoleUpdate userContextRoleUpdate, AirflowClient airflowClient) throws URISyntaxException, IOException {
        List<AirflowUserResponse> usersByEmail =
                airflowClient.users().getUsers().stream().filter(user -> userContextRoleUpdate.getEmail().equalsIgnoreCase(user.getEmail()) && userContextRoleUpdate.getUsername().equalsIgnoreCase(user.getUsername())).toList();
        if (CollectionUtils.isNotEmpty(usersByEmail) && usersByEmail.size() > 1) {
            log.warn("[Found more than one user by an email] --- {} has usernames: [{}]", userContextRoleUpdate.getEmail(),
                    usersByEmail.stream().map(AirflowUser::getUsername).collect(Collectors.joining(",")));
            for (AirflowUserResponse airflowUserResponse : usersByEmail) {
                if (!airflowUserResponse.getEmail().equalsIgnoreCase(airflowUserResponse.getUsername())) {
                    log.warn("[Found more than one user by an email] --- returning user with username != email");
                    return airflowUserResponse;
                }
            }
        }
        if (CollectionUtils.isEmpty(usersByEmail)) {
            return null;
        }
        return usersByEmail.get(0);
    }

    public static void removeAllDataDomainRolesFromUser(AirflowUser airflowUser) {
        if (airflowUser == null) {
            return;
        }
        List<AirflowUserRole> airflowUserRoles = CollectionUtils.emptyIfNull(airflowUser.getRoles())
                .stream()
                .filter(airflowRole -> !airflowRole.getName().startsWith(DATA_DOMAIN_ROLE_PREFIX))
                .filter(airflowRole -> !airflowRole.getName().equalsIgnoreCase(AF_OPERATOR_ROLE_NAME))
                .toList();
        airflowUser.setRoles(new ArrayList<>(airflowUserRoles));
    }

}
