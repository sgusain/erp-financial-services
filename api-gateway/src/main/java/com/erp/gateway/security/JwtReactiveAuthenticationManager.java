package com.erp.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    // Must match TokenBlacklistService.KEY_PREFIX in user-service - both read/write
    // the same Redis keyspace to blacklist an access token at logout.
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";

    @Value("${jwt.secret}")
    private String secret;

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public JwtReactiveAuthenticationManager(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = String.valueOf(authentication.getCredentials());

        return reactiveStringRedisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return Mono.error(new BadCredentialsException("Token has been revoked"));
                    }
                    return authenticateToken(token);
                });
    }

    private Mono<Authentication> authenticateToken(String token) {
        return Mono.fromCallable(() -> parseClaims(token))
                .map(claims -> {
                    String role = claims.get("role", String.class);
                    List<SimpleGrantedAuthority> authorities = role != null
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            : List.of();
                    return new UsernamePasswordAuthenticationToken(claims.getSubject(), token, authorities);
                })
                .onErrorMap(JwtException.class,
                        ex -> new BadCredentialsException("Invalid JWT token", ex))
                .cast(Authentication.class);
    }

    private Claims parseClaims(String token) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
