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
package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserUpdateOrderGuardTest {

    private final UserUpdateOrderGuard guard = new UserUpdateOrderGuard();

    private static UserContextRoleUpdate update(String email, Long stateReadAt) {
        UserContextRoleUpdate update = new UserContextRoleUpdate();
        update.setEmail(email);
        update.setStateReadAt(stateReadAt);
        return update;
    }

    /** Runs the update through the guard like a consumer does and tells whether it was applied. */
    private boolean apply(UserContextRoleUpdate update) {
        try (UserUpdateOrderGuard.Permit permit = guard.enter(update)) {
            if (permit.isStale()) {
                return false;
            }
            permit.applied();
            return true;
        }
    }

    @Test
    void appliesTheFirstUpdateOfAUser() {
        assertTrue(apply(update("viewer@hellodata.ch", 100L)));
    }

    @Test
    void skipsAnOlderUpdateAfterANewerOne() {
        // the full synchronization read the user before an admin changed it, but is processed after the change
        assertTrue(apply(update("viewer@hellodata.ch", 200L)));
        assertFalse(apply(update("viewer@hellodata.ch", 100L)));
    }

    @Test
    void appliesNewerAndEqualUpdates() {
        assertTrue(apply(update("viewer@hellodata.ch", 100L)));
        assertTrue(apply(update("viewer@hellodata.ch", 100L)));
        assertTrue(apply(update("viewer@hellodata.ch", 200L)));
    }

    @Test
    void comparesEmailsIgnoringCase() {
        assertTrue(apply(update("Viewer@HelloDATA.ch", 200L)));
        assertFalse(apply(update("viewer@hellodata.ch", 100L)));
    }

    @Test
    void keepsTheUsersApart() {
        assertTrue(apply(update("viewer@hellodata.ch", 200L)));
        assertTrue(apply(update("editor@hellodata.ch", 100L)));
    }

    @Test
    void appliesUpdatesWithoutTimestampOrEmail() {
        // updates of portals that do not send the timestamp yet behave as before
        assertTrue(apply(update("viewer@hellodata.ch", 200L)));
        assertTrue(apply(update("viewer@hellodata.ch", null)));
        assertTrue(apply(update(null, 100L)));
    }

    @Test
    void doesNotRememberAnUpdateThatWasNotApplied() {
        // e.g. the subsystem was not reachable: an older update may still be applied afterwards
        try (UserUpdateOrderGuard.Permit permit = guard.enter(update("viewer@hellodata.ch", 200L))) {
            assertFalse(permit.isStale());
        }
        assertTrue(apply(update("viewer@hellodata.ch", 100L)));
    }

    @Test
    void runsTheUpdatesOfOneUserOneAfterAnother() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            AtomicBoolean running = new AtomicBoolean();
            AtomicBoolean overlapped = new AtomicBoolean();
            CountDownLatch start = new CountDownLatch(1);
            List<Long> applied = Collections.synchronizedList(new ArrayList<>());
            List<Future<?>> futures = new ArrayList<>();
            for (long readAt = 1; readAt <= 50; readAt++) {
                long stateReadAt = readAt;
                futures.add(executor.submit(() -> {
                    start.await();
                    try (UserUpdateOrderGuard.Permit permit = guard.enter(update("viewer@hellodata.ch", stateReadAt))) {
                        if (!running.compareAndSet(false, true)) {
                            overlapped.set(true);
                        }
                        if (!permit.isStale()) {
                            applied.add(stateReadAt);
                            permit.applied();
                        }
                        running.set(false);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
            assertFalse(overlapped.get(), "two updates of the same user ran at the same time");
            // whatever order the threads ran in, the applied states only go forward and end with the newest one
            assertEquals(applied.stream().sorted().toList(), applied);
            assertEquals(50L, applied.get(applied.size() - 1));
        } finally {
            executor.shutdownNow();
        }
    }
}
