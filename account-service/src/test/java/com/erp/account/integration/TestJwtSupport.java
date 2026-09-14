package com.erp.account.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpHeaders;

import java.security.Key;
import java.util.Date;

/**
 * account-service never issues JWTs itself (that's user-service's job) - it only
 * validates them (JwtValidator). So integration tests hand-craft tokens here, signed
 * with the same secret the test context is configured with
 * (AbstractIntegrationTest.TEST_JWT_SECRET), the same way user-service's JwtUtil does.
 */
final class TestJwtSupport {

    private TestJwtSupport() {
    }

    private static Key signingKey() {
        return Keys.hmacShaKeyFor(AbstractIntegrationTest.TEST_JWT_SECRET.getBytes());
    }

    static String token(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    static HttpHeaders bearerHeaders(String username, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token(username, role));
        return headers;
    }
}
