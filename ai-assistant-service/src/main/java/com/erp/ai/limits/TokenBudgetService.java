package com.erp.ai.limits;

import java.time.Duration;
import java.time.LocalDate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.erp.ai.config.AiProperties;
import com.erp.ai.exception.TokenBudgetExceededException;

/** A daily cap on tokens, per user and for everyone together. This protects the AI bill. */
@Component
public class TokenBudgetService {

    private static final String GLOBAL = "ai:tokens:global:";

    private final StringRedisTemplate redis;
    private final AiProperties properties;

    public TokenBudgetService(StringRedisTemplate redis, AiProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** Call before sending a request to the model. */
    public void check(String username) {
        if (used(userKey(username)) >= properties.limits().dailyTokensPerUser()) {
            throw new TokenBudgetExceededException("Your daily AI limit is reached. Try again tomorrow.");
        }
        if (used(GLOBAL + LocalDate.now()) >= properties.limits().dailyTokensGlobal()) {
            throw new TokenBudgetExceededException("The daily AI limit for the whole system is reached.");
        }
    }

    /** Call after the model answers, with the tokens it used. */
    public void add(String username, long tokens) {
        if (tokens <= 0) {
            return;
        }
        increment(userKey(username), tokens);
        increment(GLOBAL + LocalDate.now(), tokens);
    }

    public long usedToday(String username) {
        return used(userKey(username));
    }

    private String userKey(String username) {
        return "ai:tokens:" + username + ":" + LocalDate.now();
    }

    private long used(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

    private void increment(String key, long tokens) {
        redis.opsForValue().increment(key, tokens);
        redis.expire(key, Duration.ofHours(26));
    }
}
