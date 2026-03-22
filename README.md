# Notification System

Two Spring Boot services:

- `notification-api`: accepts notification requests, stores them in Postgres, publishes events to Kafka.
- `notification-worker`: consumes events, applies idempotency + rate limiting, routes to provider endpoints, retries, then DLQ.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker

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

Run manually:

```bash
docker run --rm -p 8080:8080 --name notification-api notification-api:local
docker run --rm -p 8082:8082 --name notification-worker notification-worker:local
```
