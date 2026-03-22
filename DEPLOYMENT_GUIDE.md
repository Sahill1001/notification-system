# Deployment Guide - Notification System

## Pre-Deployment Checklist

- Java 21+
- Maven 3.9+
- Docker and Docker Compose
- Accessible Postgres, Kafka, Redis
- Environment variables for prod profile

## Option A: Run with Root Docker Compose

From repo root:

```bash
docker compose up --build -d
```

This starts:

- Zookeeper
- Kafka
- Postgres
- Redis
- notification-api
- notification-worker

Stop stack:

```bash
docker compose down
```

## Option B: Infra + Local JVM Apps

```bash
docker compose -f infrastructure/docker-compose.yml up -d

cd notification-api
mvn spring-boot:run

cd ../notification-worker
mvn spring-boot:run
```

## Option C: Production-Like Compose

```bash
docker compose -f infrastructure/docker-compose.prod.yml up --build -d
```

## Service Environment Variables

### API

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`
- `REDIS_HOST`
- `REDIS_PORT`

### Worker

- `SPRING_PROFILES_ACTIVE=prod`
- `KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_CONSUMER_GROUP`
- `REDIS_HOST`
- `REDIS_PORT`
- `WORKER_RETRY_MAX_ATTEMPTS`
- `WORKER_RETRY_BACKOFF_MS`
- `WORKER_PROVIDER_TIMEOUT_MS`
- `WORKER_PROVIDER_EMAIL_URL`
- `WORKER_PROVIDER_SMS_URL`
- `WORKER_PROVIDER_PUSH_URL`

## Container Images

Build manually:

```bash
docker build -t notification-api:local ./notification-api
docker build -t notification-worker:local ./notification-worker
```

Or build from root multi-target Dockerfile:

```bash
docker build --target api -t notification-api:root .
docker build --target worker -t notification-worker:root .
```

Run manually:

```bash
docker run --rm -p 8080:8080 --name notification-api notification-api:local
docker run --rm -p 8082:8082 --name notification-worker notification-worker:local
```

## Production Notes

- Use external managed Postgres/Redis/Kafka when possible.
- Set stronger credentials and secret management.
- Add monitoring and alerting using Spring Actuator metrics.
- Use multiple worker replicas for higher throughput.
- Use backup and retention policy for DB and Kafka.
