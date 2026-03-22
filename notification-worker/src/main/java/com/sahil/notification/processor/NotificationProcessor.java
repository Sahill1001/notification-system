package com.sahil.notification.processor;

import com.sahil.notification.idempotency.IdempotencyService;
import com.sahil.notification.model.NotificationEvent;
import com.sahil.notification.ratelimit.RateLimitService;
import com.sahil.notification.routing.ProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessor {

    private final IdempotencyService idempotencyService;
    private final RateLimitService rateLimitService;
    private final ProviderRouter router;

    public void process(NotificationEvent event) {

        if (idempotencyService.isDuplicate(event.getId())) {

            log.warn("Duplicate notification {}", event.getId());

            return;
        }

        rateLimitService.assertWithinLimit(event);

        router.route(event);
    }
}
