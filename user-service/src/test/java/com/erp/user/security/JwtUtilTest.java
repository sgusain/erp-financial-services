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
        // Flip a character in the middle of the signature segment, not the
        // very last character of the token: base64url's final character can
        // have unused padding bits, so certain single-character swaps right
        // at the end can decode to the same underlying byte and leave the
        // signature genuinely unchanged - this was observed as a flaky
        // failure (each generated token has different bytes due to the
        // embedded timestamp, so whether the last character sits on such a
        // boundary varies run to run). A middle character always changes the
        // decoded signature bytes.
        int lastDot = token.lastIndexOf('.');
        int flipIndex = lastDot + (token.length() - lastDot) / 2;
        char original = token.charAt(flipIndex);
        char replacement = original == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, flipIndex) + replacement + token.substring(flipIndex + 1);

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
