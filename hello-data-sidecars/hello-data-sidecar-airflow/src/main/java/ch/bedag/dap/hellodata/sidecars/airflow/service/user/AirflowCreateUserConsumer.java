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
package ch.bedag.dap.hellodata.sidecars.airflow.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.client.AirflowClient;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUser;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUsersResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.service.provider.AirflowClientProvider;
import ch.bedag.dap.hellodata.sidecars.airflow.service.resource.AirflowUserResourceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.CREATE_USER;
import static ch.bedag.dap.hellodata.sidecars.airflow.service.user.AirflowUserUtil.toAirflowUser;

@Log4j2
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S3516")
public class AirflowCreateUserConsumer {

    private final AirflowUserResourceProviderService userResourceProviderService;
    private final AirflowClientProvider airflowClientProvider;


    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = CREATE_USER)
    public void createUser(SubsystemUserUpdate supersetUserCreate) {
        try {
            log.info("------- Received airflow user creation request {}", supersetUserCreate);

            AirflowClient airflowClient = airflowClientProvider.getAirflowClientInstance();
            AirflowUsersResponse users = airflowClient.users();

            // Airflow only allows unique username and email, so we make sure there is nobody with either of these already existing, before creating a new one
            Optional<AirflowUserResponse> userResult = users.getUsers()
                    .stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(supersetUserCreate.getEmail()) ||
                            user.getUsername().equalsIgnoreCase(supersetUserCreate.getUsername()))
                    .findFirst();
            if (userResult.isPresent()) {
                log.info("User {} already exists in instance, omitting creation. Email: {}", supersetUserCreate.getUsername(), supersetUserCreate.getEmail());
                return;
            }

            log.info("Going to create new user with email: {}", supersetUserCreate.getEmail());
            AirflowUser airflowUser = toAirflowUser(supersetUserCreate);
            airflowClient.createUser(airflowUser);
            userResourceProviderService.publishUsers();
        } catch (URISyntaxException | IOException e) {
            log.error("Could not create user {}", supersetUserCreate.getEmail(), e);
        }
    }
}
