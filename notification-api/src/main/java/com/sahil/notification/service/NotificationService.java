package com.sahil.notification.service;

import com.sahil.notification.entity.Notification;
import com.sahil.notification.kafka.NotificationProducer;
import com.sahil.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String STATUS_QUEUED = "QUEUED";

    private final NotificationRepository repository;
    private final NotificationProducer producer;

    public Notification createNotification(Notification notification) {

        if (notification.getStatus() == null || notification.getStatus().isBlank()) {
            notification.setStatus(STATUS_QUEUED);
        }

        Notification saved = repository.save(notification);

        producer.sendNotification(saved);

        return saved;

    }
}
