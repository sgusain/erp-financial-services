package com.erp.user.security;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks failed login attempts per username and enforces a temporary lockout
 * after too many consecutive failures.
 *
 * NOTE: this state is in-memory and per-instance. It resets on service restart
 * and is NOT shared across multiple instances of user-service if horizontally
 * scaled - each instance tracks its own attempt counts independently. A
 * production deployment would back this with Redis or a shared DB table so
 * lockout state is consistent across instances; that is tracked as follow-up
 * work and intentionally out of scope here.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private record AttemptRecord(int failedAttempts, LocalDateTime lockedUntil) {}

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        AttemptRecord record = attempts.get(username);
        if (record == null || record.lockedUntil() == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(record.lockedUntil())) {
            attempts.remove(username);
            return false;
        }
        return true;
    }

    public void recordFailure(String username) {
        attempts.compute(username, (key, existing) -> {
            int newCount = (existing == null ? 0 : existing.failedAttempts()) + 1;
            LocalDateTime lockedUntil = newCount >= MAX_ATTEMPTS
                    ? LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES)
                    : null;
            return new AttemptRecord(newCount, lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(username);
    }
}
