# System Design - Notification System

## High-Level Architecture

```text
Client
  |
  v
notification-api (8080)
  |- validate + persist in Postgres
  |- publish event to Kafka topic: notification-events
  v
Kafka
  v
notification-worker (8082)
  |- idempotency check (Redis)
  |- rate limiting (Redis)
  |- provider routing (EMAIL/SMS/PUSH)
  |- retry with backoff
  |- DLQ publish on terminal failure
  v
Provider endpoints (optional external HTTP)
```

## Components

### notification-api

- `NotificationController`: `POST /notifications`
- `NotificationService`: default status + persistence + event publish
- `NotificationRepository`: JPA access
- `NotificationProducer`: Kafka publish
- `KafkaTopicConfig`: topic bootstrap

### notification-worker

- `NotificationEventConsumer`: Kafka consumer with manual ack
- `NotificationProcessor`: orchestration
- `IdempotencyService`: duplicate protection via Redis `SETNX`
- `RateLimitService`: per-user limits via Redis counter + TTL
- `ProviderRouter`: channel based dispatch
- `EmailProvider` / `SmsProvider` / `PushProvider`: channel handlers
- `RetryService`: retry attempts with delay
- `DeadLetterPublisher`: publish to DLQ topic

## End-to-End Delivery Flow

1. Client calls `POST /notifications`.
2. API validates and stores in Postgres.
3. API publishes event to Kafka.
4. Worker consumes the event.
5. Worker validates idempotency key.
6. Worker enforces per-user rate limit.
7. Worker routes by channel.
8. Worker tries provider call.
9. On repeated failure, worker writes to DLQ.
10. Consumer acknowledges offset.

## Reliability Design

- At-least-once delivery through Kafka + manual ack.
- Idempotency guard to avoid duplicate notifications.
- Rate limiter to protect provider systems.
- Retry with bounded attempts and backoff.
- Dead-letter topic for failures requiring manual replay/inspection.

## Data Stores and Responsibilities

- Postgres: source-of-truth notification records.
- Redis: fast state for idempotency + rate limiting.
- Kafka: asynchronous decoupling between ingest and delivery.

## Failure Path

- Provider timeout or non-2xx response
- Retry until `worker.retry.max-attempts`
- Publish final failed payload to `notification-events-dlq`

## Scalability Notes

- API is stateless and horizontally scalable.
- Worker concurrency controlled via Kafka listener concurrency and consumer group scaling.
- Redis and Kafka can be clustered for high throughput.
- Postgres should use indexing, pooling, and optional read replicas for growth.
