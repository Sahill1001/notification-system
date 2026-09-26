package com.sahil.notification.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redis;

    // Completed-delivery deduplication, not an exactly-once delivery guarantee.
    public boolean isProcessed(Long notificationId) {
        return notificationId != null
                && Boolean.TRUE.equals(redis.hasKey("notification:processed:" + notificationId));
    }

    public void markProcessed(Long notificationId) {
        if (notificationId != null) {
            redis.opsForValue().set("notification:processed:" + notificationId, "1", Duration.ofHours(24));
        }
    }
}
