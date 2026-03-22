package com.sahil.notification.provider;

import com.sahil.notification.model.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailProvider implements NotificationProvider {

    private final ProviderHttpClient providerHttpClient;

    @Value("${worker.providers.email.url:}")
    private String emailEndpoint;

    @Override
    public void send(NotificationEvent event) {
        providerHttpClient.send("EMAIL", emailEndpoint, event);
    }
}
