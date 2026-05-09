package com.Vaish.SpringSentinel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RedisService redisService;

    public void handleBotInteraction(
            Long userId,
            String message
    ) {

        // ========================================================
        // TRY TO ACQUIRE NOTIFICATION COOLDOWN
        // ========================================================

        boolean allowed =
                redisService.trySetNotificationCooldown(
                        userId
                );

        // ========================================================
        // SEND IMMEDIATELY
        // ========================================================

        if (allowed) {

            log.info(
                    "Push Notification Sent to User {}: {}",
                    userId,
                    message
            );

        } else {

            redisService.pushPendingNotification(
                    userId,
                    message
            );
        }
    }
}