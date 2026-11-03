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

import java.util.Collection;
import java.util.Locale;

/**
 * Maps the role keys of the sidecar (hd_role.key) to the ids of the CloudBeaver teams (cb_team.team_id) the gateway
 * sends in the X-Role header. CloudBeaver saves the user's teams by their exact id, so an id that does not exist makes
 * the reverse proxy login fail.
 */
public final class CbTeams {

    private CbTeams() {
    }

    /**
     * The upper-cased role key is the team id of the data domain teams (e.g. DEFAULT_DATA_DOMAIN_READ_DWH). Newer
     * CloudBeaver versions lower-case the teams of initial-data.conf, so on a fresh database the admin team is called
     * admin, while installations created before still have ADMIN. Therefore, the upper-cased key wins if such a team
     * exists, otherwise a team with the same id in another case, and the upper-cased key if there is none at all.
     */
    public static String toTeamId(String roleKey, Collection<String> teamIds) {
        String upperCased = roleKey.toUpperCase(Locale.ENGLISH);
        if (teamIds.contains(upperCased)) {
            return upperCased;
        }
        return teamIds.stream()
                .filter(teamId -> teamId.equalsIgnoreCase(roleKey))
                .findFirst()
                .orElse(upperCased);
    }
}
