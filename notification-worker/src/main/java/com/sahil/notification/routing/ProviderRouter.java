package com.sahil.notification.routing;

import com.sahil.notification.model.NotificationEvent;
import com.sahil.notification.provider.EmailProvider;
import com.sahil.notification.provider.PushProvider;
import com.sahil.notification.provider.SmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderRouter {

    private final EmailProvider emailProvider;
    private final SmsProvider smsProvider;
    private final PushProvider pushProvider;

    public void route(NotificationEvent event) {

        if (event.getChannel() == null || event.getChannel().isBlank()) {
            throw new IllegalArgumentException("channel is required");
        }

        switch (event.getChannel().trim().toUpperCase()) {
            case "EMAIL" -> emailProvider.send(event);
            case "SMS" -> smsProvider.send(event);
            case "PUSH" -> pushProvider.send(event);
            default -> {
                log.warn("Unknown channel '{}' for notificationId={}. Falling back to EMAIL",
                        event.getChannel(), event.getId());
                emailProvider.send(event);
            }
        }
    }
}
