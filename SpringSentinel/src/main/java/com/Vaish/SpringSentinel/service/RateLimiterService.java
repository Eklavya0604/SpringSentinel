package com.Vaish.SpringSentinel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    // ── Cached Lua script ────────────────────────────────────────
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;

    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setScriptText(
                "local current = redis.call('INCR', KEYS[1]) " +
                        "if current == 1 then " +
                        "   redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
                        "end " +
                        "return current"
        );
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    public boolean isRateLimited(String ipAddress) {
        Long count = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                Collections.singletonList(
                        "rate_limit:" + ipAddress),
                "60"
        );
        return count != null &&
                count > MAX_REQUESTS_PER_MINUTE;
    }
}