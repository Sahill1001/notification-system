package com.sahil.notification.provider;

import com.sahil.notification.model.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsProvider implements NotificationProvider {

    private final ProviderHttpClient providerHttpClient;

    @Value("${worker.providers.sms.url:}")
    private String smsEndpoint;

    @Override
    public void send(NotificationEvent event) {
        providerHttpClient.send("SMS", smsEndpoint, event);
    }
}
