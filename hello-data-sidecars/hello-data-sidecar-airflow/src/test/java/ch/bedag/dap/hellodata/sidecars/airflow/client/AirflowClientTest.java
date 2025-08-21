/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.sidecars.airflow.client;

import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.*;
import ch.bedag.dap.hellodata.sidecars.airflow.service.provider.AirflowClientProvider;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled("Only working for local development")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Log4j2
@EnabledIf(expression = "${tests.spring.enabled:false}")
class AirflowClientTest {

    @Autowired
    private AirflowClientProvider airflowClientProvider;

    @Test
    void users() throws URISyntaxException, IOException {
        //When
        AirflowUsersResponse users = airflowClientProvider.getAirflowClientInstance().users();

        //Then
        assertThat(users).isNotNull();
        assertThat(users.getTotalEntries()).isGreaterThanOrEqualTo(1);
        assertFalse(users.getUsers().isEmpty());
    }

    @Test
    void createUser() throws URISyntaxException, IOException {
        //Given
        AirflowUser user = new AirflowUser();
        String randomUsername = "airflow0" + UUID.randomUUID().toString().replace("-", "");
        user.setUsername(String.format("%s", randomUsername));
        user.setEmail(String.format("%s@bedag.ch", randomUsername));
        user.setFirstName("Klaus");
        user.setLastName("Bond");
        user.setPassword(user.getUsername());
        user.setRoles(new ArrayList<>());

        //When
        AirflowUserResponse userResponse = airflowClientProvider.getAirflowClientInstance().createUser(user);

        //Then
        assertThat(userResponse).isNotNull();
        assertThat(userResponse.isActive()).isTrue();
        assertThat(userResponse.getUsername()).isEqualTo(user.getUsername());
        assertThat(userResponse.getRoles()).isNotEmpty();
    }

    @Test
    void updateUser() throws IOException, URISyntaxException {
        //Given
        String usernameToUpdate = "airflow009";
        AirflowUserRolesUpdate userRolesUpdate = createAirflowUserRolesUpdate(usernameToUpdate);

        //When
        AirflowUserResponse airflowUserResponse = airflowClientProvider.getAirflowClientInstance().updateUser(userRolesUpdate, usernameToUpdate);

        //Then
        assertNotNull(airflowUserResponse);
        assertThat(airflowUserResponse.getRoles()).containsExactlyInAnyOrderElementsOf(userRolesUpdate.getRoles());
    }

    @Test
    void roles() throws URISyntaxException, IOException {
        //When
        AirflowRolesResponse roles = airflowClientProvider.getAirflowClientInstance().roles();

        //Then
        assertThat(roles).isNotNull();
        assertThat(roles.getTotalEntries()).isGreaterThan(0);
        assertThat(roles.getRoles()).isNotEmpty();
    }

    @Test
    void permissions() throws URISyntaxException, IOException {
        //When
        AirflowPermissionsResponse permissions = airflowClientProvider.getAirflowClientInstance().permissions();

        //Then
        assertThat(permissions).isNotNull();
        assertThat(permissions.getTotalEntries()).isGreaterThan(0);
        assertThat(permissions.getActions()).isNotEmpty();
    }

    private static AirflowUserRolesUpdate createAirflowUserRolesUpdate(String usernameToUpdate) {
        AirflowUserRolesUpdate userRolesUpdate = new AirflowUserRolesUpdate();
        userRolesUpdate.setUsername(usernameToUpdate);
        userRolesUpdate.setEmail("test");
        userRolesUpdate.setFirstName("test");
        userRolesUpdate.setLastName("last");
        userRolesUpdate.setPassword("test");
        ArrayList<AirflowUserRole> roles = new ArrayList<>();
        AirflowUserRole role1 = new AirflowUserRole();
        role1.setName("Viewer");
        roles.add(role1);
        AirflowUserRole role2 = new AirflowUserRole();
        role2.setName("Admin");
        roles.add(role2);
        userRolesUpdate.setRoles(roles);
        return userRolesUpdate;
    }
}