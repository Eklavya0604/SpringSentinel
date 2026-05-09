package com.Vaish.SpringSentinel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // ── Cached Lua scripts ───────────────────────────────────────
    private static final DefaultRedisScript<Long>
            INCREMENT_BOT_SCRIPT;
    private static final DefaultRedisScript<List>
            POP_NOTIFS_SCRIPT;

    static {
        INCREMENT_BOT_SCRIPT = new DefaultRedisScript<>();
        INCREMENT_BOT_SCRIPT.setScriptText(
                "local current = " +
                        "tonumber(redis.call('GET', KEYS[1])) or 0 " +
                        "if current < tonumber(ARGV[1]) then " +
                        "   redis.call('INCR', KEYS[1]) " +
                        "   return 1 " +
                        "else " +
                        "   return 0 " +
                        "end"
        );
        INCREMENT_BOT_SCRIPT.setResultType(Long.class);

        POP_NOTIFS_SCRIPT = new DefaultRedisScript<>();
        POP_NOTIFS_SCRIPT.setScriptText(
                "local messages = " +
                        "redis.call('LRANGE', KEYS[1], 0, -1) " +
                        "redis.call('DEL', KEYS[1]) " +
                        "return messages"
        );
        POP_NOTIFS_SCRIPT.setResultType(List.class);
    }

    // ── Virality ─────────────────────────────────────────────────
    public void incrementVirality(Long postId, int points) {
        redisTemplate.opsForValue().increment(
                "post:" + postId + ":virality_score", points);
    }

    public String getVirality(Long postId) {
        return redisTemplate.opsForValue().get(
                "post:" + postId + ":virality_score");
    }

    // ── Horizontal Cap ───────────────────────────────────────────
    public boolean tryIncrementBotCount(Long postId, int cap) {
        Long result = redisTemplate.execute(
                INCREMENT_BOT_SCRIPT,
                Collections.singletonList(
                        "post:" + postId + ":bot_count"),
                String.valueOf(cap)
        );
        return Long.valueOf(1L).equals(result);
    }

    public void decrementBotCount(Long postId) {
        redisTemplate.opsForValue()
                .decrement("post:" + postId + ":bot_count");
    }

    // ── Cooldown Cap ─────────────────────────────────────────────
    public boolean trySetCooldown(Long botId, Long humanId) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(
                        "cooldown:bot_" + botId + ":human_" + humanId,
                        "1", 10, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(success);
    }

    public void removeCooldown(Long botId, Long humanId) {
        redisTemplate.delete(
                "cooldown:bot_" + botId + ":human_" + humanId);
    }

    public boolean isCooldownActive(Long botId, Long humanId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(
                        "cooldown:bot_" + botId + ":human_" + humanId));
    }

    // ── Notification Throttling ──────────────────────────────────
    public boolean trySetNotificationCooldown(Long userId) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(
                        "notif_cooldown:user_" + userId,
                        "1", 15, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(success);
    }

    public boolean isNotificationCooldownActive(Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(
                        "notif_cooldown:user_" + userId));
    }

    // ── Pending Notifications ────────────────────────────────────
    public void pushPendingNotification(
            Long userId, String message) {
        redisTemplate.opsForList().rightPush(
                "user:" + userId + ":pending_notifs", message);
    }

    @SuppressWarnings("unchecked")
    public List<String> popAllPendingNotifications(Long userId) {
        List<String> result = redisTemplate.execute(
                POP_NOTIFS_SCRIPT,
                Collections.singletonList(
                        "user:" + userId + ":pending_notifs")
        );
        return result != null ? result : List.of();
    }

    // ── Safe SCAN ────────────────────────────────────────────────
    public Set<String> getAllPendingNotifKeys() {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match("user:*:pending_notifs")
                .count(100)
                .build();
        try (Cursor<String> cursor =
                     redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }
}