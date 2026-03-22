package com.sahil.notification.retry;

import com.sahil.notification.model.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {

    private final DeadLetterPublisher deadLetterPublisher;

    @Value("${worker.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${worker.retry.backoff-ms:500}")
    private long backoffMs;

    public boolean canRetry(int attempt) {
        return attempt < maxAttempts;
    }

    public void handleFailure(NotificationEvent event, int attempt, Exception exception) {

        if (!canRetry(attempt)) {
            log.error("Max retries exhausted for notificationId={}. Publishing to DLQ. reason={}",
                    event.getId(), exception.getMessage());
            deadLetterPublisher.publish(event);
            return;
        }

        log.warn("Attempt {} failed for notificationId={}. reason={}",
                attempt, event.getId(), exception.getMessage());
    }

    public void backoffBeforeNextAttempt() {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry backoff interrupted", e);
        }
    }
}
