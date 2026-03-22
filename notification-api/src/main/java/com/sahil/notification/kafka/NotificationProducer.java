package com.sahil.notification.kafka;

import com.sahil.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Notification> kafkaTemplate;

    public void sendNotification(Notification event) {

        String key = event.getId() == null ? null : String.valueOf(event.getId());
        kafkaTemplate.send("notification-events", key, event);

    }
}
