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
package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Coordinates one-time provisioning of a brand-new user on first access.
 * <p>
 * Provisioning used to run inside {@code HellodataAuthenticationConverter.convert()}, i.e. on the
 * per-request hot path of every authenticated call, where a first-login request burst had each
 * request attempt the full provisioning concurrently. This coordinator is invoked once, from the
 * profile endpoint (the first call the portal makes after login), and serializes provisioning
 * per user so the burst results in exactly one run on this node. Cross-node races are still handled
 * by {@link AutoProvisionService#autoProvisionIfEnabled} (existing-user double-check + the unique
 * constraint on the user record), so this lock is an optimization, not the correctness boundary.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class FirstLoginProvisioningCoordinator {

    // AutoProvisionService is a separate bean, so autoProvisionIfEnabled is called through the Spring
    // proxy and its @Transactional(REQUIRES_NEW) still applies (self-invocation would have bypassed it).
    private final AutoProvisionService autoProvisionService;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Provision the user if auto-provisioning is enabled and they do not exist yet. Idempotent:
     * a concurrent or repeated call returns the already-provisioned user without creating a duplicate.
     *
     * @return the provisioned (or already-existing) user, or {@code null} if auto-provisioning is off.
     */
    public UserEntity provisionOnFirstAccess(String email, String firstName, String lastName, String keycloakSubject) {
        if (email == null || keycloakSubject == null) {
            return null;
        }
        String key = email.toLowerCase(Locale.ROOT);
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            return autoProvisionService.autoProvisionIfEnabled(email, firstName, lastName, keycloakSubject);
        } finally {
            lock.unlock();
            // Best-effort cleanup; correctness does not depend on it (AutoProvisionService re-checks
            // for an existing user), it only keeps the map from growing unbounded.
            locks.remove(key, lock);
        }
    }
}
