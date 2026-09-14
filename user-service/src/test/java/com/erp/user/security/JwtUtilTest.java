package com.erp.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-256-bits-long-for-hs256";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 900000L);
    }

    @Test
    void generateToken_includesSubjectAndRoleClaim() {
        String token = jwtUtil.generateToken("alice@example.com", "ADMIN");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void extractUsernameAndRole_roundTripForDifferentUsers() {
        String token = jwtUtil.generateToken("bob@example.com", "USER");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("bob@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void isTokenValid_trueForFreshlyGeneratedToken() {
        String token = jwtUtil.generateToken("carol@example.com", "USER");

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_falseForGarbageToken() {
        assertThat(jwtUtil.isTokenValid("not-a-jwt-at-all")).isFalse();
    }

    @Test
    void isTokenValid_falseForTamperedToken() {
        String token = jwtUtil.generateToken("dave@example.com", "USER");
        // Flip the last character of the signature segment to corrupt it.
        String tampered = token.substring(0, token.length() - 1)
                + (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_falseForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String expiredToken = jwtUtil.generateToken("eve@example.com", "USER");

        assertThat(jwtUtil.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void isTokenValid_falseWhenSignedWithDifferentSecret() {
        JwtUtil otherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherJwtUtil, "secret", "a-completely-different-secret-key-that-is-also-256-bits-long");
        ReflectionTestUtils.setField(otherJwtUtil, "expiration", 900000L);
        String tokenFromOtherKey = otherJwtUtil.generateToken("frank@example.com", "USER");

        assertThat(jwtUtil.isTokenValid(tokenFromOtherKey)).isFalse();
    }
}
