package com.sahil.notification.retry;

import com.sahil.notification.model.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeadLetterPublisher {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void publish(NotificationEvent event) {

        String key = event.getId() == null ? null : String.valueOf(event.getId());
        kafkaTemplate.send("notification-events-dlq", key, event);
    }
}
