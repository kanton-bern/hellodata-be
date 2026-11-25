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
package ch.bedag.dap.hellodata.portal.user_audit.service;

import ch.bedag.dap.hellodata.portal.user_audit.data.UserAuditChange;

import java.util.*;

/**
 * Point-in-time view of everything that defines a user's access. Two snapshots are diffed to get the audit changes.
 * Values are Strings, Booleans or sorted Lists of Strings so the diff is stable and readable as JSON.
 */
public final class UserStateSnapshot {

    public static final UserStateSnapshot EMPTY = new UserStateSnapshot(null, Collections.emptySortedMap());

    private final String email;
    private final SortedMap<Key, Object> values;

    UserStateSnapshot(String email, SortedMap<Key, Object> values) {
        this.email = email;
        this.values = values;
    }

    public String email() {
        return email;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * @return the changed fields, ordered by field and context key; empty if nothing changed
     */
    public List<UserAuditChange> diff(UserStateSnapshot after) {
        SortedSet<Key> keys = new TreeSet<>(values.keySet());
        keys.addAll(after.values.keySet());
        List<UserAuditChange> changes = new ArrayList<>();
        for (Key key : keys) {
            Object before = values.get(key);
            Object afterValue = after.values.get(key);
            if (!Objects.equals(before, afterValue)) {
                changes.add(new UserAuditChange(key.field(), key.contextKey(), before, afterValue));
            }
        }
        return changes;
    }

    record Key(String field, String contextKey) implements Comparable<Key> {
        private static final Comparator<Key> ORDER = Comparator.comparing(Key::field)
                .thenComparing(Key::contextKey, Comparator.nullsFirst(Comparator.naturalOrder()));

        @Override
        public int compareTo(Key other) {
            return ORDER.compare(this, other);
        }
    }
}
