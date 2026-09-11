package ch.bedag.dap.hellodata.sidecars.airflow.service.user;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUser;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserRole;
import org.junit.jupiter.api.Test;

import static ch.bedag.dap.hellodata.sidecars.airflow.service.user.AirflowUserUtil.PUBLIC_ROLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirflowUserUtilTest {

    /**
     * A freshly invited user must be created in Airflow with the baseline {@code Public} role, so they have
     * orchestration access immediately (mirrors the Superset create path) rather than only after the next
     * bulk user sync assigns it.
     */
    @Test
    void toAirflowUser_assignsPublicRoleAtCreation() {
        SubsystemUserUpdate update = new SubsystemUserUpdate();
        update.setEmail("jane.doe@example.com");
        update.setUsername("jane.doe@example.com");
        update.setFirstName("Jane");
        update.setLastName("Doe");

        AirflowUser airflowUser = AirflowUserUtil.toAirflowUser(update);

        assertTrue(airflowUser.getRoles().contains(new AirflowUserRole(PUBLIC_ROLE_NAME)),
                "invited user must get the Public role at creation");
        assertEquals(1, airflowUser.getRoles().size(), "only the baseline Public role is expected at creation");
        // sanity: the rest of the mapping is preserved
        assertEquals("jane.doe@example.com", airflowUser.getEmail());
        assertEquals("jane.doe@example.com", airflowUser.getUsername());
        assertEquals("Jane", airflowUser.getFirstName());
        assertEquals("Doe", airflowUser.getLastName());
    }

    /**
     * The roles list must stay mutable — the role-update path later adds/removes roles on the same object.
     */
    @Test
    void toAirflowUser_rolesListIsMutable() {
        AirflowUser airflowUser = AirflowUserUtil.toAirflowUser(new SubsystemUserUpdate());
        airflowUser.getRoles().add(new AirflowUserRole("Admin")); // must not throw
        assertEquals(2, airflowUser.getRoles().size());
    }
}
