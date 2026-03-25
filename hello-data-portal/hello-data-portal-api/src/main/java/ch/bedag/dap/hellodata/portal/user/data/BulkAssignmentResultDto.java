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
package ch.bedag.dap.hellodata.portal.user.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignmentResultDto {
    private int updatedCount;
    private int skippedCount;
    private int failedCount;
    private List<String> errors = new ArrayList<>();
    private List<UserDetail> updatedUsers = new ArrayList<>();
    private List<UserDetail> skippedUsers = new ArrayList<>();
    private List<UserDetail> failedUsers = new ArrayList<>();

    public void addUpdated(String email, String firstName, String lastName) {
        updatedCount++;
        updatedUsers.add(new UserDetail(email, firstName, lastName, null));
    }

    public void addSkipped(String email, String firstName, String lastName, String reason) {
        skippedCount++;
        skippedUsers.add(new UserDetail(email, firstName, lastName, reason));
    }

    public void addFailed(String email, String firstName, String lastName, String reason) {
        failedCount++;
        failedUsers.add(new UserDetail(email, firstName, lastName, reason));
        errors.add(email + ": " + reason);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDetail {
        private String email;
        private String firstName;
        private String lastName;
        private String reason;
    }
}
