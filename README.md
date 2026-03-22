# Notification System

Repository URL: https://github.com/Sahill1001/notification-system

Two Spring Boot services:

- `notification-api`: accepts notification requests, stores them in Postgres, publishes events to Kafka.
- `notification-worker`: consumes events, applies idempotency + rate limiting, routes to provider endpoints, retries, then DLQ.

## HLD (High-Level Design)

Components:

- Client applications call `notification-api` over HTTP.
- `notification-api` validates input, persists notification in Postgres, and publishes an event to Kafka topic `notification-events`.
- `notification-worker` consumes events from Kafka and processes delivery logic.
- Redis is used by worker for idempotency and rate limiting.
- Provider integrations (Email/SMS/Push) are called via HTTP endpoints from worker.
- Failed events after max retries are sent to `notification-events-dlq`.

High-level data flow:

`Client -> notification-api -> Postgres + Kafka(notification-events) -> notification-worker -> Provider`

Failure path:

`notification-worker -> retry -> Kafka(notification-events-dlq)`

## LLD (Low-Level Design)

`notification-api` module:

- `NotificationController`: exposes `POST /notifications`.
- `NotificationService`: sets default status (`QUEUED`), saves entity, publishes event.
- `NotificationRepository`: JPA repository for `notifications` table.
- `NotificationProducer`: publishes JSON event to Kafka with notification id as key.
- `KafkaTopicConfig`: auto-creates `notification-events` topic.

`notification-worker` module:

- `NotificationEventConsumer`: Kafka listener with manual acknowledgment and retry loop.
- `NotificationProcessor`: orchestration for idempotency, rate-limit check, and routing.
- `IdempotencyService`: Redis `SETNX` based duplicate protection.
- `RateLimitService`: Redis counter + TTL (per-user per-minute throttling).
- `ProviderRouter`: routes based on channel (`EMAIL`, `SMS`, `PUSH`).
- `EmailProvider` / `SmsProvider` / `PushProvider`: provider-specific send adapters.
- `ProviderHttpClient`: shared HTTP sender with timeout and response validation.
- `RetryService`: retry policy (max attempts + backoff) and terminal failure handling.
- `DeadLetterPublisher`: publishes terminal failures to `notification-events-dlq`.
- `KafkaTopicConfig`: auto-creates DLQ topic.

## End-to-End Flow

1. Client sends `POST /notifications` with `userId`, `channel`, `message`.
2. API validates payload and stores record in Postgres.
3. API publishes event to Kafka topic `notification-events`.
4. Worker consumes event from Kafka.
5. Worker checks idempotency key in Redis.
6. Worker applies per-user rate limit in Redis.
7. Worker routes to provider based on channel.
8. If provider call fails, worker retries with configured backoff.
9. If retries are exhausted, worker publishes event to `notification-events-dlq`.
10. Worker manually acknowledges Kafka message after success or DLQ handoff.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker

## One-Command Docker Run

You can run infra + both services directly from project root:

```bash
docker compose up --build -d
```

This uses root `docker-compose.yml`.

## Start Infra

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

Services:

- Kafka: `localhost:9092`
- Postgres: `localhost:5433`
- Redis: `localhost:6379`

## Run Services

Terminal 1:

```bash
cd notification-api
mvn spring-boot:run
```

Terminal 2:

```bash
cd notification-worker
mvn spring-boot:run
```

For a production-like local run:

```bash
docker compose -f infrastructure/docker-compose.prod.yml up --build -d
```

## Test Request

```bash
curl -X POST http://localhost:8080/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 101,
    "channel": "EMAIL",
    "message": "Welcome to the platform"
  }'
```

## Provider Endpoints (Optional)

By default, worker simulates sends if provider URLs are empty.

Configure in `notification-worker/src/main/resources/application.yaml`:

- `worker.providers.email.url`
- `worker.providers.sms.url`
- `worker.providers.push.url`

If configured, worker does HTTP `POST` with the notification JSON payload.

## Production Profile

Production configs are provided in:

- `notification-api/src/main/resources/application-prod.yaml`
- `notification-worker/src/main/resources/application-prod.yaml`

Activate with:

```bash
--spring.profiles.active=prod
```

## Retry + DLQ

- Retries controlled by:
  - `worker.retry.max-attempts`
  - `worker.retry.backoff-ms`
- After max attempts, event is published to Kafka topic: `notification-events-dlq`

## Running Tests

```bash
cd notification-api
mvn test

cd ../notification-worker
mvn test
```

## Docker Images

Build manually:

```bash
docker build -t notification-api:local ./notification-api
docker build -t notification-worker:local ./notification-worker
```

Root multi-target `Dockerfile` is also available:

```bash
docker build --target api -t notification-api:root .
docker build --target worker -t notification-worker:root .
```

Run manually:

```bash
docker run --rm -p 8080:8080 --name notification-api notification-api:local
docker run --rm -p 8082:8082 --name notification-worker notification-worker:local
```

## Additional Documentation

- `START_HERE.md`: quick onboarding and first run
- `SYSTEM_DESIGN.md`: architecture and flow breakdown
- `API_EXAMPLES.md`: practical API requests
- `DEPLOYMENT_GUIDE.md`: deployment options and env config
- `PROJECT_SUMMARY.md`: project scope and deliverables
- `HELP.md`: troubleshooting and common commands

## Author

**Sahilkumar Prasad**
- GitHub: [github.com/sahill1001](https://github.com/sahill1001)
- LinkedIn: [linkedin.com/in/sahilkumar-prasad-74abba272](https://linkedin.com/in/sahilkumar-prasad-74abba272)
- Email: prasadsahil06@gmail.com
