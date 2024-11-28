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
package ch.bedag.dap.hellodata.sidecars.dbt.listener;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.dbt.entities.User;
import ch.bedag.dap.hellodata.sidecars.dbt.repository.UserRepository;
import ch.bedag.dap.hellodata.sidecars.dbt.service.resource.DbtDocsUserResourceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.CREATE_USER;

@Log4j2
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S3516")
public class DbtDocsCreateUserConsumer {

    private final DbtDocsUserResourceProviderService userResourceProviderService;
    private final UserRepository userRepository;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = CREATE_USER, asyncRun = false)
    public void createUser(SubsystemUserUpdate supersetUserCreate) {
        log.info("------- Received dbt docs user creation request {}", supersetUserCreate);
        User user = userRepository.findByUserNameOrEmail(supersetUserCreate.getUsername(), supersetUserCreate.getEmail());
        if (user != null) {
            log.debug("User {} already exists in instance, omitting creation. Email: {}", supersetUserCreate.getUsername(), supersetUserCreate.getEmail());
            return;
        }
        log.info("Going to create new dbt docs user with email: {}", supersetUserCreate.getEmail());
        User dbtDocUser = toDbtDocUser(supersetUserCreate);
        userRepository.save(dbtDocUser);
        if (supersetUserCreate.isSendBackUsersList()) {
            userResourceProviderService.publishUsers();
        }
    }

    @NotNull
    private User toDbtDocUser(SubsystemUserUpdate supersetUserCreate) {
        User dbtDocUser = new User(supersetUserCreate.getUsername(), supersetUserCreate.getEmail());
        dbtDocUser.setRoles(new ArrayList<>());
        dbtDocUser.setFirstName(supersetUserCreate.getFirstName());
        dbtDocUser.setLastName(supersetUserCreate.getLastName());
        dbtDocUser.setEnabled(true);
        dbtDocUser.setSuperuser(false);
        return dbtDocUser;
    }
}
