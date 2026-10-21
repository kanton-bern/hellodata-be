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
package ch.bedag.dap.hellodata.portal.dashboard_comment.util;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Resolves the quarter (1-4) a dashboard comment refers to.
 * If no quarter is set explicitly, it is derived from the comment creation date.
 */
@UtilityClass
public class CommentQuarterUtil {

    /**
     * Same zone as the backfill in the db changelog, so derived quarters stay consistent.
     */
    public static final ZoneId QUARTER_ZONE = ZoneId.of("Europe/Zurich");

    public static int quarterOf(long epochMillis) {
        int month = Instant.ofEpochMilli(epochMillis).atZone(QUARTER_ZONE).getMonthValue();
        return (month - 1) / 3 + 1;
    }

    /**
     * Returns the given quarter if set, otherwise the quarter derived from the creation date.
     * Returns null only if neither is available.
     */
    public static Integer resolveQuarter(Integer quarter, Long createdDate) {
        if (quarter != null) {
            return quarter;
        }
        return createdDate != null ? quarterOf(createdDate) : null;
    }

    public static void validateQuarter(Integer quarter) {
        if (quarter != null && (quarter < 1 || quarter > 4)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quarter must be between 1 and 4");
        }
    }
}
