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
package ch.bedag.dap.hellodata.cloudbeaver.gateway.repository;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CbTeamsTest {

    @Test
    void usesTheLowerCasedAdminTeamOfAFreshDatabase() {
        assertEquals("admin", CbTeams.toTeamId("admin", List.of("admin", "user", "default_data_domain_read_dwh")));
    }

    @Test
    void keepsTheUpperCasedAdminTeamOfAnOlderDatabase() {
        assertEquals("ADMIN", CbTeams.toTeamId("admin", List.of("ADMIN", "user")));
    }

    @Test
    void prefersTheUpperCasedTeamWhenBothExist() {
        List<String> teamIds = List.of("default_data_domain_read_dwh", "DEFAULT_DATA_DOMAIN_READ_DWH");
        assertEquals("DEFAULT_DATA_DOMAIN_READ_DWH", CbTeams.toTeamId("Default_Data_Domain_READ_DWH", teamIds));
    }

    @Test
    void upperCasesTheRoleKeyWithoutAMatchingTeam() {
        assertEquals("EXTRA_DATA_DOMAIN_READ_DM", CbTeams.toTeamId("Extra_Data_Domain_READ_DM", List.of()));
    }
}
