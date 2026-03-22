package com.sahil.notification.consumer;

import com.sahil.notification.model.NotificationEvent;
import com.sahil.notification.processor.NotificationProcessor;
import com.sahil.notification.retry.RetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationProcessor processor;
    private final RetryService retryService;

    @KafkaListener(
            topics = "notification-events",
            groupId = "notification-workers",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.sahil.notification.model.NotificationEvent"
            }
    )
    public void consume(NotificationEvent event, Acknowledgment ack) {

        log.info("Received notification event: {}", event);

        int attempt = 1;

        while (true) {
            try {
                processor.process(event);
                ack.acknowledge();
                return;
            } catch (Exception e) {
                retryService.handleFailure(event, attempt, e);

                if (!retryService.canRetry(attempt)) {
                    ack.acknowledge();
                    return;
                }

                retryService.backoffBeforeNextAttempt();
                attempt++;
            }
        }
    }
}
