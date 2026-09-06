package com.codelens.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS_PER_HOUR = 20;
    private static final Duration WINDOW_DURATION = Duration.ofHours(1);

    public boolean allowRequest(UUID userId) {
        String key = "rate_limit:" + userId.toString();

        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            if (Boolean.FALSE.equals(hasKey)) {
                redisTemplate.opsForValue().set(key, String.valueOf(MAX_REQUESTS_PER_HOUR - 1), WINDOW_DURATION);
                log.info("Initialized new Redis rate limit bucket for user {} (19 tokens remaining)", userId);
                return true;
            }

            Long remaining = redisTemplate.opsForValue().decrement(key);
            if (remaining != null && remaining >= 0) {
                log.debug("Rate limit decrement for user {}: {} tokens left", userId, remaining);
                return true;
            } else {
                log.warn("Rate limit exceeded for user {}", userId);
                return false;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for rate limiting (failing open for user {}): {}", userId, e.getMessage());
            return true;
        }
    }

    public long getRemainingTokens(UUID userId) {
        String key = "rate_limit:" + userId.toString();
        try {
            String val = redisTemplate.opsForValue().get(key);
            if (val != null) {
                return Math.max(0, Long.parseLong(val));
            }
        } catch (Exception e) {
            log.warn("Could not fetch remaining rate limit tokens: {}", e.getMessage());
        }
        return MAX_REQUESTS_PER_HOUR;
    }
}
