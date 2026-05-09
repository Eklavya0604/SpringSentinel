package com.Vaish.SpringSentinel.scheduler;

import com.Vaish.SpringSentinel.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final RedisService redisService;

    // ============================================================
    // CRON SWEEPER — runs every 5 minutes
    // ============================================================

    @Scheduled(fixedRate = 300000)
    public void sweepPendingNotifications() {

        log.info("CRON Sweeper running...");

        Set<String> keys = redisService.getAllPendingNotifKeys();

        if (keys == null || keys.isEmpty()) {
            log.info("CRON Sweeper: No pending notifications found.");
            return;
        }

        log.info("CRON Sweeper: Found {} users with pending notifications.", keys.size());

        for (String key : keys) {
            try {
                processUserNotifications(key);
            } catch (Exception e) {
                // ✅ One user failing doesn't stop others
                log.error(
                        "CRON Sweeper: Failed to process key {}: {}",
                        key, e.getMessage()
                );
            }
        }

        log.info("CRON Sweeper: Finished.");
    }

    // ============================================================
    // PROCESS SINGLE USER NOTIFICATIONS
    // ============================================================

    private void processUserNotifications(String key) {

        // ── Safe key parsing ─────────────────────────────────────
        // Key format: user:{id}:pending_notifs
        String userIdStr = key
                .replace("user:", "")
                .replace(":pending_notifs", "");

        Long userId = Long.parseLong(userIdStr);

        // ── Atomically pop all messages ──────────────────────────
        List<String> messages =
                redisService.popAllPendingNotifications(userId);

        if (messages.isEmpty()) return;

        // ── Build summarized notification ────────────────────────
        String first = messages.get(0);
        int others = messages.size() - 1;

        if (others > 0) {
            log.info(
                    "Summarized Push Notification for User {}: " +
                            "{} and [{}] others interacted with your posts.",
                    userId, first, others
            );
        } else {
            log.info(
                    "Summarized Push Notification for User {}: {}",
                    userId, first
            );
        }
    }
}