# Notification System - Start Here

This is a quick onboarding guide for the notification system.

## 1) What This Project Does

Two Spring Boot services work together:

- `notification-api` accepts HTTP requests and stores notifications in Postgres.
- `notification-worker` consumes Kafka events and delivers via provider adapters with idempotency, rate limiting, retries, and DLQ.

## 2) Quick Start (Local Dev)

From project root:

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

Run API:

```bash
cd notification-api
mvn spring-boot:run
```

Run Worker (new terminal):

```bash
cd notification-worker
mvn spring-boot:run
```

## 3) Quick Start (Fully Dockerized)

From project root:

```bash
docker compose up --build -d
```

This uses the root `docker-compose.yml` and starts infra + both app services.

## 4) Test API

```bash
curl -X POST http://localhost:8080/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 101,
    "channel": "EMAIL",
    "message": "Welcome to Notification System"
  }'
```

## 5) Key Ports

- API: `8080`
- Worker: `8082`
- Kafka: `9092`
- Postgres: `5433`
- Redis: `6379`

## 6) Core Topics

- Main topic: `notification-events`
- Dead-letter topic: `notification-events-dlq`

## 7) Next Docs

- `README.md`: complete reference
- `SYSTEM_DESIGN.md`: HLD + LLD + delivery flows
- `API_EXAMPLES.md`: request/response examples
- `DEPLOYMENT_GUIDE.md`: prod deployment patterns
- `PROJECT_SUMMARY.md`: project checklist and structure
- `HELP.md`: troubleshooting shortcuts
