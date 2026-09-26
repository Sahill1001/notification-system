package com.sahil.notification;

import com.sahil.notification.consumer.NotificationEventConsumer;
import com.sahil.notification.idempotency.IdempotencyService;
import com.sahil.notification.model.NotificationEvent;
import com.sahil.notification.processor.NotificationProcessor;
import com.sahil.notification.ratelimit.RateLimitService;
import com.sahil.notification.retry.DeadLetterPublisher;
import com.sahil.notification.retry.RetryService;
import com.sahil.notification.routing.ProviderRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationRetryTests {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final ProviderRouter router = mock(ProviderRouter.class);
    private final RateLimitService limiter = mock(RateLimitService.class);
    private final DeadLetterPublisher dlq = mock(DeadLetterPublisher.class);
    private final Acknowledgment ack = mock(Acknowledgment.class);
    private final Set<String> completed = new HashSet<>();
    private final NotificationEvent event = new NotificationEvent(42L, 7L, "EMAIL", "hello", "QUEUED");
    private NotificationEventConsumer consumer;
    private NotificationProcessor processor;

    @BeforeEach void setup() {
        when(redis.hasKey(anyString())).thenAnswer(inv -> completed.contains(inv.getArgument(0)));
        when(redis.opsForValue()).thenReturn(values);
        doAnswer(inv -> { completed.add(inv.getArgument(0)); return null; })
                .when(values).set(anyString(), anyString(), any(Duration.class));
        processor = new NotificationProcessor(new IdempotencyService(redis), limiter, router);
        var retry = new RetryService(dlq);
        ReflectionTestUtils.setField(retry, "maxAttempts", 3);
        ReflectionTestUtils.setField(retry, "backoffMs", 0L);
        consumer = new NotificationEventConsumer(processor, retry);
    }

    @Test void failedDeliveryIsRetriedBeforeAcknowledgment() {
        doThrow(new IllegalStateException("provider unavailable")).doAnswer(inv -> {
            assertTrue(completed.isEmpty(), "Failure must not create a completed marker");
            verify(ack, never()).acknowledge();
            return null;
        }).when(router).route(event);
        consumer.consume(event, ack);
        verify(router, times(2)).route(event);
        verify(ack).acknowledge();
        verifyNoInteractions(dlq);
        assertTrue(completed.contains("notification:processed:42"));
        verify(values).set("notification:processed:42", "1", Duration.ofHours(24));
    }

    @Test void successfulDeliveryIsSkippedOnRedelivery() {
        consumer.consume(event, ack);
        consumer.consume(event, ack);
        verify(router).route(event);
        verify(ack, times(2)).acknowledge();
    }

    @Test void exhaustedRetriesReachDlqWithoutCompletedMarker() {
        doThrow(new IllegalStateException("provider unavailable")).when(router).route(event);
        consumer.consume(event, ack);
        verify(router, times(3)).route(event);
        verify(dlq).publish(event);
        verify(ack).acknowledge();
        assertTrue(completed.isEmpty());
    }

    @Test void rateLimitFailureDoesNotSuppressLaterDelivery() {
        doThrow(new IllegalStateException("rate limit")).doNothing().when(limiter).assertWithinLimit(event);
        consumer.consume(event, ack);
        verify(limiter, times(2)).assertWithinLimit(event);
        verify(router).route(event);
        verify(ack).acknowledge();
    }

    @Test void redisReadFailureDoesNotCallProviderOrAcknowledge() {
        when(redis.hasKey(anyString())).thenThrow(new IllegalStateException("redis unavailable"));
        assertThrows(IllegalStateException.class, () -> processor.process(event));
        verifyNoInteractions(router, ack);
    }
}
