package com.sahil.notification.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahil.notification.model.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProviderHttpClient {

    private final ObjectMapper objectMapper;

    @Value("${worker.providers.timeout-ms:3000}")
    private long timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public void send(String providerName, String endpoint, NotificationEvent event) {

        if (endpoint == null || endpoint.isBlank()) {
            log.info("{} endpoint not configured. Simulating send for notificationId={}",
                    providerName, event.getId());
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(event)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException(providerName + " provider call failed with status " + statusCode);
            }

            log.info("{} provider accepted notificationId={} with statusCode={}",
                    providerName, event.getId(), statusCode);
        } catch (IOException e) {
            throw new IllegalStateException(providerName + " provider I/O failure", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(providerName + " provider call interrupted", e);
        }
    }

    private String toJson(NotificationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize notification payload", e);
        }
    }
}
