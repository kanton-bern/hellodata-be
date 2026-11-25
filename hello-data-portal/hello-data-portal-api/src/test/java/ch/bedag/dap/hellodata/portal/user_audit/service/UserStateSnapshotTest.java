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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserStateSnapshotTest {

    private static UserStateSnapshot snapshot(Map<UserStateSnapshot.Key, Object> values) {
        return new UserStateSnapshot("user@example.com", new TreeMap<>(values));
    }

    private static UserStateSnapshot.Key key(String field, String contextKey) {
        return new UserStateSnapshot.Key(field, contextKey);
    }

    @Test
    void diff_noChange_isEmpty() {
        UserStateSnapshot before = snapshot(Map.of(key("enabled", null), true, key("role", "ctx1"), "DATA_DOMAIN_VIEWER"));
        UserStateSnapshot after = snapshot(Map.of(key("enabled", null), true, key("role", "ctx1"), "DATA_DOMAIN_VIEWER"));

        assertTrue(before.diff(after).isEmpty());
    }

    @Test
    void diff_addedValue_hasNullBefore() {
        UserStateSnapshot before = snapshot(Map.of());
        UserStateSnapshot after = snapshot(Map.of(key("role", "ctx1"), "DATA_DOMAIN_ADMIN"));

        assertEquals(List.of(new UserAuditChange("role", "ctx1", null, "DATA_DOMAIN_ADMIN")), before.diff(after));
    }

    @Test
    void diff_removedValue_hasNullAfter() {
        UserStateSnapshot before = snapshot(Map.of(key("rlsRoles", "ctx1"), List.of("RLS_01")));
        UserStateSnapshot after = snapshot(Map.of());

        assertEquals(List.of(new UserAuditChange("rlsRoles", "ctx1", List.of("RLS_01"), null)), before.diff(after));
    }

    @Test
    void diff_changedValue_hasBothValues() {
        UserStateSnapshot before = snapshot(Map.of(key("enabled", null), true));
        UserStateSnapshot after = snapshot(Map.of(key("enabled", null), false));

        assertEquals(List.of(new UserAuditChange("enabled", null, true, false)), before.diff(after));
    }

    @Test
    void diff_isOrderedByFieldAndContextKey() {
        UserStateSnapshot before = snapshot(Map.of());
        UserStateSnapshot after = snapshot(Map.of(
                key("role", "ctx2"), "B",
                key("role", "ctx1"), "A",
                key("enabled", null), true,
                key("dashboards", "ctx1"), List.of("D")));

        List<UserAuditChange> changes = before.diff(after);

        assertEquals(List.of("dashboards:ctx1", "enabled:null", "role:ctx1", "role:ctx2"),
                changes.stream().map(change -> change.field() + ":" + change.contextKey()).toList());
    }

    @Test
    void diff_fromEmpty_containsEverything() {
        UserStateSnapshot after = snapshot(Map.of(key("enabled", null), true));

        assertEquals(1, UserStateSnapshot.EMPTY.diff(after).size());
        assertTrue(UserStateSnapshot.EMPTY.isEmpty());
    }
}
