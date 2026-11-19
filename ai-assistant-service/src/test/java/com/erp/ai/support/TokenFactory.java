package com.erp.ai.support;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/** Makes the same kind of token user-service makes: HS256, subject = username, claim "role". */
public final class TokenFactory {

    public static final String SECRET = "test-secret-test-secret-test-secret-123456";

    private TokenFactory() {}

    public static String token(String username, String role) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(username)
                    .claim("role", role)
                    .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
