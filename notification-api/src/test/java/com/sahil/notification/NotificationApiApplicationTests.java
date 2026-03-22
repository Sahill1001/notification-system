package com.sahil.notification;

import com.sahil.notification.entity.Notification;
import com.sahil.notification.kafka.NotificationProducer;
import com.sahil.notification.repository.NotificationRepository;
import com.sahil.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationApiApplicationTests {

	@Mock
	private NotificationRepository repository;

	@Mock
	private NotificationProducer producer;

	@InjectMocks
	private NotificationService notificationService;

	@Test
	void shouldSetQueuedStatusWhenMissingAndPublishEvent() {
		Notification input = Notification.builder()
				.userId(42L)
				.channel("EMAIL")
				.message("welcome")
				.build();

		Notification persisted = Notification.builder()
				.id(100L)
				.userId(42L)
				.channel("EMAIL")
				.message("welcome")
				.status("QUEUED")
				.build();

		when(repository.save(input)).thenReturn(persisted);

		Notification result = notificationService.createNotification(input);

		assertEquals("QUEUED", result.getStatus());
		verify(repository).save(input);
		verify(producer).sendNotification(persisted);
	}

	@Test
	void shouldKeepProvidedStatus() {
		Notification input = Notification.builder()
				.userId(42L)
				.channel("SMS")
				.message("otp")
				.status("PROCESSING")
				.build();

		when(repository.save(input)).thenReturn(input);

		Notification result = notificationService.createNotification(input);

		assertEquals("PROCESSING", result.getStatus());
		verify(producer).sendNotification(input);
	}

}
