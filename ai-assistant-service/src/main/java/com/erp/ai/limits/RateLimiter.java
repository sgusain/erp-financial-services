package com.erp.ai.limits;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.erp.ai.config.AiProperties;
import com.erp.ai.exception.RateLimitExceededException;

/** At most N AI requests per user per minute. One Redis counter per user per minute. */
@Component
public class RateLimiter {

    private final StringRedisTemplate redis;
    private final AiProperties properties;

    public RateLimiter(StringRedisTemplate redis, AiProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void check(String username) {
        String key = "ai:rate:" + username + ":" + (System.currentTimeMillis() / 60_000);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofSeconds(70));
        }
        if (count != null && count > properties.limits().requestsPerMinute()) {
            throw new RateLimitExceededException("Too many AI requests. Please wait a minute.");
        }
    }
}
