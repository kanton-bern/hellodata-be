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
package ch.bedag.dap.hellodata.sidecars.cloudbeaver.listener;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserDelete;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.User;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.UserRepository;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.service.resource.CbUserResourceProviderService;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.DELETE_USER;

@Log4j2
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S3516")
public class CbDeleteUserConsumer {
    private final CbUserResourceProviderService userResourceProviderService;
    private final UserRepository userRepository;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = DELETE_USER)
    public CompletableFuture<Void> deleteUser(SubsystemUserDelete subsystemUserDelete) {
        log.info("------- Received cloudbeaver user deletion request {}", subsystemUserDelete);
        User user = userRepository.findByUserNameOrEmail(subsystemUserDelete.getUsername(), subsystemUserDelete.getEmail());
        if (user == null) {
            log.info("User {} doesn't exist in instance, omitting deletion. Email: {}", subsystemUserDelete.getUsername(), subsystemUserDelete.getEmail());
            return null;//NOSONAR
        }
        log.info("Going to delete cloudbeaver user with email: {}", subsystemUserDelete.getEmail());
        userRepository.delete(user);
        userResourceProviderService.publishUsers();
        return null;//NOSONAR
    }
}
