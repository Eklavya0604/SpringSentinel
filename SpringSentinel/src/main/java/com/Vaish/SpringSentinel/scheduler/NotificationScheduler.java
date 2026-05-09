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

    @Scheduled(fixedRate = 300000)
    public void sweepPendingNotifications() {

        log.info("CRON Sweeper running...");

        Set<String> keys =
                redisService.getAllPendingNotifKeys();

        if (keys == null || keys.isEmpty()) {
            log.info("CRON Sweeper: No pending notifications.");
            return;
        }

        log.info("CRON Sweeper: {} users with pending notifications.",
                keys.size());

        for (String key : keys) {
            try {
                processUserNotifications(key);
            } catch (Exception e) {
                log.error(
                        "CRON Sweeper: Failed for key {}: {}",
                        key, e.getMessage());
            }
        }

        log.info("CRON Sweeper: Finished.");
    }

    private void processUserNotifications(String key) {
        String userIdStr = key
                .replace("user:", "")
                .replace(":pending_notifs", "");
        Long userId = Long.parseLong(userIdStr);

        List<String> messages =
                redisService.popAllPendingNotifications(userId);

        if (messages.isEmpty()) return;

        String first = messages.get(0);
        int others = messages.size() - 1;

        if (others > 0) {
            log.info(
                    "Summarized Push for User {}: " +
                            "{} and [{}] others interacted with your posts.",
                    userId, first, others);
        } else {
            log.info(
                    "Summarized Push for User {}: {}",
                    userId, first);
        }
    }
}