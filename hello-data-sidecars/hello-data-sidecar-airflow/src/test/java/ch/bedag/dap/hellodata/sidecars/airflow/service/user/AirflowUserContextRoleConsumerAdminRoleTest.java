package ch.bedag.dap.hellodata.sidecars.airflow.service.user;

import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.client.AirflowClient;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserRolesUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.service.provider.AirflowClientProvider;
import ch.bedag.dap.hellodata.sidecars.airflow.service.resource.AirflowRoleResourceProviderService;
import ch.bedag.dap.hellodata.sidecars.airflow.service.resource.AirflowUserResourceProviderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reproduces the asymmetry in {@link AirflowUserContextRoleConsumer#updateUserRoles}: the Airflow
 * "Admin" role is only added/removed on the branch where the user still has at least one
 * DATA_DOMAIN-typed context role. A user whose data domain roles are all NONE
 * ({@code HdRoleName.NONE.getContextType() == null}) takes the else-branch, which never touches Admin.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AirflowUserContextRoleConsumerAdminRoleTest {

    private static final String ADMIN = "Admin";
    private static final String PUBLIC = "Public";

    @Mock
    private AirflowClientProvider airflowClientProvider;
    @Mock
    private AirflowRoleResourceProviderService roleResourceProviderService;
    @Mock
    private AirflowUserResourceProviderService userResourceProviderService;
    @Mock
    private AirflowClient airflowClient;

    private static UserContextRoleUpdate.ContextRole contextRole(String key, HdRoleName role) {
        UserContextRoleUpdate.ContextRole cr = new UserContextRoleUpdate.ContextRole();
        cr.setContextKey(key);
        cr.setRoleName(role);
        return cr;
    }

    private static AirflowUserResponse userWithAdminRole() {
        AirflowUserResponse user = new AirflowUserResponse();
        user.setUsername("jdoe");
        user.setEmail("jdoe@example.org");
        user.setFirstName("J");
        user.setLastName("Doe");
        user.setRoles(new ArrayList<>(List.of(new AirflowUserRole(ADMIN))));
        return user;
    }

    private static List<AirflowRole> allRoles() {
        AirflowRole admin = new AirflowRole();
        admin.setName(ADMIN);
        AirflowRole pub = new AirflowRole();
        pub.setName(PUBLIC);
        return List.of(admin, pub);
    }

    /**
     * Demotion: the user was HELLODATA_ADMIN and is set to NONE everywhere.
     * Expected: Airflow "Admin" is revoked. Actual: it is kept.
     */
    @Test
    void adminRoleIsNotRevoked_whenAllContextRolesBecomeNone() throws Exception {
        AirflowUserContextRoleConsumer consumer =
                new AirflowUserContextRoleConsumer(airflowClientProvider, roleResourceProviderService, userResourceProviderService);

        AirflowUserResponse existing = userWithAdminRole();
        when(airflowClient.getUser("jdoe")).thenReturn(existing);

        UserContextRoleUpdate update = new UserContextRoleUpdate();
        update.setUsername("jdoe");
        update.setEmail("jdoe@example.org");
        update.setSendBackUsersList(false);
        // exactly what the portal publishes after demoting an admin: NONE in the business domain and in every data domain
        update.setContextRoles(List.of(
                contextRole("Business Domain", HdRoleName.NONE),
                contextRole("dd_one", HdRoleName.NONE),
                contextRole("dd_two", HdRoleName.NONE)));

        consumer.updateUserRoles(update, airflowClient, allRoles());

        ArgumentCaptor<AirflowUserRolesUpdate> captor = ArgumentCaptor.forClass(AirflowUserRolesUpdate.class);
        verify(airflowClient).updateUser(captor.capture(), eq("jdoe"));
        List<String> pushedRoles = captor.getValue().getRoles().stream().map(AirflowUserRole::getName).toList();

        assertThat(pushedRoles)
                .as("Airflow Admin must be revoked when the user no longer holds HELLODATA_ADMIN")
                .doesNotContain(ADMIN);
    }

    /**
     * Control: the very same demotion handled by the DISABLE_USER consumer does revoke Admin,
     * showing the intended behaviour exists elsewhere in the module.
     */
    @Test
    void adminRoleIsRevoked_whenAtLeastOneDataDomainRoleIsPresent() throws Exception {
        AirflowUserContextRoleConsumer consumer =
                new AirflowUserContextRoleConsumer(airflowClientProvider, roleResourceProviderService, userResourceProviderService);

        AirflowUserResponse existing = userWithAdminRole();
        when(airflowClient.getUser("jdoe")).thenReturn(existing);
        when(airflowClient.roles()).thenReturn(rolesResponse());

        UserContextRoleUpdate update = new UserContextRoleUpdate();
        update.setUsername("jdoe");
        update.setEmail("jdoe@example.org");
        update.setSendBackUsersList(false);
        update.setContextRoles(List.of(
                contextRole("Business Domain", HdRoleName.NONE),
                contextRole("dd_one", HdRoleName.DATA_DOMAIN_VIEWER)));

        consumer.updateUserRoles(update, airflowClient, allRoles());

        ArgumentCaptor<AirflowUserRolesUpdate> captor = ArgumentCaptor.forClass(AirflowUserRolesUpdate.class);
        verify(airflowClient).updateUser(captor.capture(), eq("jdoe"));
        List<String> pushedRoles = captor.getValue().getRoles().stream().map(AirflowUserRole::getName).toList();

        assertThat(pushedRoles).doesNotContain(ADMIN);
    }

    private static ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRolesResponse rolesResponse() {
        ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRolesResponse response =
                new ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRolesResponse();
        response.setRoles(new ArrayList<>(allRoles()));
        return response;
    }
}
