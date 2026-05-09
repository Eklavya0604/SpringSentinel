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

    // ============================================================
    // ATOMIC RATE LIMIT SCRIPT
    // ============================================================

    private static final String RATE_LIMIT_SCRIPT =

            "local current = redis.call('INCR', KEYS[1]) " +

                    "if current == 1 then " +
                    "   redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
                    "end " +

                    "return current";

    public boolean isRateLimited(String ipAddress) {

        String key = "rate_limit:" + ipAddress;

        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>();

        script.setScriptText(RATE_LIMIT_SCRIPT);

        script.setResultType(Long.class);

        Long count = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                "60"
        );

        return count != null &&
                count > MAX_REQUESTS_PER_MINUTE;
    }
}