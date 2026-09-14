package com.erp.notification.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

/**
 * Signs JWTs with the same secret configured in src/test/resources/application.yml
 * (jwt.secret) so JwtAuthenticationFilter/JwtValidator accept them in tests.
 */
public final class TestJwtUtil {

    public static final String SECRET = "test-only-erp-financial-services-secret-key-2024-must-be-256-bits";

    private TestJwtUtil() {
    }

    public static String token(String username, String role) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 900_000))
                .signWith(key)
                .compact();
    }

    public static String userToken() {
        return token("test-user", "USER");
    }

    public static String adminToken() {
        return token("test-admin", "ADMIN");
    }
}
