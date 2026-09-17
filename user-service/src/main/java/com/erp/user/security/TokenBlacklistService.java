package com.erp.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Backs the access-token blacklist used at logout. The API gateway
 * (JwtReactiveAuthenticationManager) checks the same Redis keyspace before
 * accepting a token, so the key prefix here must stay in sync with the one
 * used there.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    static final String KEY_PREFIX = "blacklist:token:";

    private final StringRedisTemplate redisTemplate;

    public void blacklistToken(String token, long expiryMillis) {
        if (expiryMillis <= 0) {
            // Token has already expired on its own; nothing to blacklist.
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + token, "true", Duration.ofMillis(expiryMillis));
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }
}
