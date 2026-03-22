# Help - Notification System

## Common Commands

Start local infra only:

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

Start full stack:

```bash
docker compose up --build -d
```

Run tests:

```bash
cd notification-api
mvn test

cd ../notification-worker
mvn test
```

## Troubleshooting

### API does not start

- Check if Postgres is running on `localhost:5433`.
- Verify DB credentials in `notification-api/src/main/resources/application.yaml`.

### Worker does not consume

- Ensure Kafka is healthy on `localhost:9092`.
- Check `KAFKA_BOOTSTRAP_SERVERS` in worker config.

### Duplicate notifications

- Verify Redis is reachable and idempotency keys are being written.

### Too many drops or delays

- Tune:
  - `worker.retry.max-attempts`
  - `worker.retry.backoff-ms`
  - Kafka consumer concurrency
  - provider timeout values

### Docker build failures

- Rebuild without cache:

```bash
docker compose build --no-cache
```

- Check Java and Maven compatibility (Java 21 and Maven 3.9+).
