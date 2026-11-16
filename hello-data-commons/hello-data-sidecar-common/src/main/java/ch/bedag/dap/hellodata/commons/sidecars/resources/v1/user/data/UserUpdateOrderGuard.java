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

import lombok.extern.log4j.Log4j2;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Keeps the user updates of a sidecar in the order the portal read the users' state.
 * <p>
 * The sidecars process NATS messages in parallel, and a user can be in a full synchronization ({@code SYNC_USERS},
 * read at its start and worked through user by user) and in a single update ({@code UPDATE_USER_CONTEXT_ROLE}) at the
 * same time. Without this guard the older state can be applied last, e.g. the full synchronization takes away a
 * dashboard that an admin granted a moment later. The guard runs the updates of one user one after another and skips
 * an update whose {@link UserContextRoleUpdate#getStateReadAt()} is older than the newest one already applied.
 * <p>
 * Usage, with one guard per sidecar shared by both consumers:
 * <pre>{@code
 * try (UserUpdateOrderGuard.Permit permit = orderGuard.enter(update)) {
 *     if (permit.isStale()) {
 *         return;
 *     }
 *     // update the user in the subsystem
 *     permit.applied();
 * }
 * }</pre>
 * The state is kept in memory: after a restart the first update of a user is always applied, and the next full
 * synchronization corrects anything older.
 */
@Log4j2
public class UserUpdateOrderGuard {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Map<String, Long> lastApplied = new ConcurrentHashMap<>();

    /**
     * Waits until no other update of the same user runs and returns a permit, which must be closed. Updates without an
     * email are neither serialized nor checked.
     */
    public Permit enter(UserContextRoleUpdate update) {
        String user = update.getEmail() == null ? null : update.getEmail().toLowerCase(Locale.ROOT);
        ReentrantLock lock = user == null ? null : locks.computeIfAbsent(user, key -> new ReentrantLock());
        if (lock != null) {
            lock.lock();
        }
        return new Permit(user, update.getStateReadAt(), lock);
    }

    /** The right to update one user, held until it is closed. */
    public final class Permit implements AutoCloseable {
        private final String user;
        private final Long stateReadAt;
        private final ReentrantLock lock;

        private Permit(String user, Long stateReadAt, ReentrantLock lock) {
            this.user = user;
            this.stateReadAt = stateReadAt;
            this.lock = lock;
        }

        /** True if a newer state of the user was already applied, then the update must be skipped. */
        public boolean isStale() {
            if (user == null || stateReadAt == null) {
                return false;
            }
            Long newest = lastApplied.get(user);
            boolean stale = newest != null && stateReadAt < newest;
            if (stale) {
                log.info("Skipping the update of user {}: its state was read at {}, a newer one read at {} was already applied", user, stateReadAt, newest);
            }
            return stale;
        }

        /** Remembers the state of this update as applied, call it once the user was updated. */
        public void applied() {
            if (user != null && stateReadAt != null) {
                lastApplied.merge(user, stateReadAt, Math::max);
            }
        }

        @Override
        public void close() {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
}
