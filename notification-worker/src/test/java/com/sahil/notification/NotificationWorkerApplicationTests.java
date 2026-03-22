package com.sahil.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahil.notification.idempotency.IdempotencyService;
import com.sahil.notification.model.NotificationEvent;
import com.sahil.notification.processor.NotificationProcessor;
import com.sahil.notification.provider.EmailProvider;
import com.sahil.notification.provider.ProviderHttpClient;
import com.sahil.notification.provider.PushProvider;
import com.sahil.notification.provider.SmsProvider;
import com.sahil.notification.ratelimit.RateLimitService;
import com.sahil.notification.retry.DeadLetterPublisher;
import com.sahil.notification.retry.RetryService;
import com.sahil.notification.routing.ProviderRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationWorkerApplicationTests {

	@Mock
	private IdempotencyService idempotencyService;

	@Mock
	private RateLimitService rateLimitService;

	@Mock
	private ProviderRouter providerRouter;

	@InjectMocks
	private NotificationProcessor notificationProcessor;

	@Mock
	private EmailProvider emailProvider;

	@Mock
	private SmsProvider smsProvider;

	@Mock
	private PushProvider pushProvider;

	@Mock
	private DeadLetterPublisher deadLetterPublisher;

	@Test
	void shouldSkipDuplicateMessages() {
		NotificationEvent event = new NotificationEvent(1L, 101L, "EMAIL", "hello", "QUEUED");
		when(idempotencyService.isDuplicate(1L)).thenReturn(true);

		notificationProcessor.process(event);

		verify(providerRouter, never()).route(any(NotificationEvent.class));
	}

	@Test
	void shouldProcessNonDuplicateMessages() {
		NotificationEvent event = new NotificationEvent(2L, 101L, "SMS", "otp", "QUEUED");
		when(idempotencyService.isDuplicate(2L)).thenReturn(false);

		notificationProcessor.process(event);

		verify(rateLimitService).assertWithinLimit(event);
		verify(providerRouter).route(event);
	}

	@Test
	void shouldRouteToEmailProvider() {
		ProviderRouter router = new ProviderRouter(emailProvider, smsProvider, pushProvider);

		router.route(new NotificationEvent(1L, 99L, "EMAIL", "x", "QUEUED"));

		verify(emailProvider).send(any(NotificationEvent.class));
		verify(smsProvider, never()).send(any(NotificationEvent.class));
		verify(pushProvider, never()).send(any(NotificationEvent.class));
	}

	@Test
	void shouldRouteToSmsProvider() {
		ProviderRouter router = new ProviderRouter(emailProvider, smsProvider, pushProvider);

		router.route(new NotificationEvent(1L, 99L, "SMS", "x", "QUEUED"));

		verify(smsProvider).send(any(NotificationEvent.class));
	}

	@Test
	void shouldThrowWhenChannelMissing() {
		ProviderRouter router = new ProviderRouter(emailProvider, smsProvider, pushProvider);

		assertThrows(IllegalArgumentException.class,
				() -> router.route(new NotificationEvent(1L, 99L, "", "x", "QUEUED")));
	}

	@Test
	void shouldPublishToDlqAtMaxAttempt() {
		RetryService retryService = new RetryService(deadLetterPublisher);
		ReflectionTestUtils.setField(retryService, "maxAttempts", 3);

		retryService.handleFailure(new NotificationEvent(7L, 9L, "PUSH", "x", "QUEUED"),
				3, new RuntimeException("boom"));

		verify(deadLetterPublisher).publish(any(NotificationEvent.class));
	}

	@Test
	void shouldNotFailWhenProviderEndpointNotConfigured() {
		ProviderHttpClient providerHttpClient = new ProviderHttpClient(new ObjectMapper());
		ReflectionTestUtils.setField(providerHttpClient, "timeoutMs", 1000L);

		assertDoesNotThrow(() -> providerHttpClient.send("EMAIL", "",
				new NotificationEvent(10L, 20L, "EMAIL", "hello", "QUEUED")));
	}

}
