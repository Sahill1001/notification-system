package com.sahil.notification.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redis;

    public boolean isDuplicate(Long notificationId) {

        if (notificationId == null) {
            return false;
        }

        String key = "notification:processed:" + notificationId;

        Boolean success = redis.opsForValue()
                .setIfAbsent(key, "1", Duration.ofHours(24));

        return Boolean.FALSE.equals(success);
    }
}
