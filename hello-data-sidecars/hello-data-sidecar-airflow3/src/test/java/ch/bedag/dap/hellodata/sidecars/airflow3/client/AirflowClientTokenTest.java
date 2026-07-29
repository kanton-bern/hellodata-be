/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package ch.bedag.dap.hellodata.sidecars.airflow3.client;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TEMPORARY (remove before merge). Verifies the sidecar mints a bearer token with exactly the
 * structure the Airflow 3 api-server accepted live (HS512, header kid="not-used",
 * aud="apache-airflow", sub=<user id>) — the same shape that returned HTTP 200 from
 * https://airflow3.dev.hellodatabedag.ch/api/v2/dags during implementation.
 *
 * See AirflowClientLiveIT (disabled) for the end-to-end call against a running api-server.
 */
class AirflowClientTokenTest {

    // HS512 needs a >= 64-byte key
    private static final String SECRET = "0123456789012345678901234567890123456789012345678901234567890123";
    private static final String USER_ID = "2";

    @Test
    void mintsTokenMatchingAirflow3ApiContract() {
        AirflowClient client = new AirflowClient("localhost", 8080, SECRET, USER_ID, 300);

        String token = client.mintToken();

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);

        assertEquals("HS512", jws.getHeader().getAlgorithm());
        assertEquals("not-used", jws.getHeader().get("kid"));
        Claims claims = jws.getPayload();
        assertEquals(USER_ID, claims.getSubject());
        assertEquals(java.util.Set.of("apache-airflow"), claims.getAudience());
        assertNotNull(claims.getId());                           // jti
        assertTrue(claims.getId().length() > 0);
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }
}
