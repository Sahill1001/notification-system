package com.sahil.notification.ratelimit;

import com.sahil.notification.model.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final long MAX_PER_MINUTE = 60L;

    private final StringRedisTemplate redis;

    public void assertWithinLimit(NotificationEvent event) {

        if (event.getUserId() == null) {
            return;
        }

        String key = "notification:ratelimit:" + event.getUserId();

        Long count = redis.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofMinutes(1));
        }

        if (count != null && count > MAX_PER_MINUTE) {
            throw new IllegalStateException("Rate limit exceeded for userId=" + event.getUserId());
        }
    }
}
