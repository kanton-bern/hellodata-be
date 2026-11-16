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
package ch.bedag.dap.hellodata.portal.initialize.service;

import ch.bedag.dap.hellodata.portal.initialize.event.InitializationCompletedEvent;
import ch.bedag.dap.hellodata.portal.initialize.event.SyncAllUsersEvent;
import ch.bedag.dap.hellodata.portal.user.ContextsNotFetchedYetException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Log4j2
@Component
@RequiredArgsConstructor
public class Initializer implements CommandLineRunner {

    private static final int MAX_CONTEXT_ROLES_INIT_RETRIES = 10;
    private static final long RETRY_INTERVAL_MS = 10_000L;

    private final ContextsInitializer contextsInitializer;
    private final RolesInitializer rolesInitializer;
    private final DefaultUserInitializer defaultUserInitializer;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UsernameInitializer usernameInitializer;

    @Override
    public void run(String... args) throws Exception {
        contextsInitializer.initContexts();
        rolesInitializer.initSystemDefaultPortalRoles();
        boolean areContextRolesFetched = false;
        int attempts = 0;
        do {
            try {
                rolesInitializer.initContextRoles();
            } catch (ContextsNotFetchedYetException e) {
                attempts++;
                if (attempts >= MAX_CONTEXT_ROLES_INIT_RETRIES) {
                    log.error("Contexts not initialized after {} attempts. Aborting startup initialization.", attempts, e);
                    throw new IllegalStateException("Failed to initialize context roles after " + attempts + " attempts", e);
                }
                log.warn("Contexts not initialized yet (attempt {}/{}), waiting {} ms...", attempts, MAX_CONTEXT_ROLES_INIT_RETRIES, RETRY_INTERVAL_MS, e);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for context initialization", ie);
                }
                continue;
            }
            boolean defaultUsersInitiated = defaultUserInitializer.initDefaultUsers();
            rolesInitializer.initContextRolesToUsers();
            rolesInitializer.initDefaultUserAsSuperuser();
            if (defaultUsersInitiated) {
                applicationEventPublisher.publishEvent(new SyncAllUsersEvent());
            }
            areContextRolesFetched = true;
        } while (!areContextRolesFetched);
        usernameInitializer.initUsernames();
        applicationEventPublisher.publishEvent(new InitializationCompletedEvent());
    }
}
