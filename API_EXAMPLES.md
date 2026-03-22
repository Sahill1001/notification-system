# API Examples - Notification System

## Base URL

`http://localhost:8080`

## 1) Create Notification

```bash
curl -X POST http://localhost:8080/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 101,
    "channel": "EMAIL",
    "message": "Welcome to the platform"
  }'
```

Sample response:

```json
{
  "id": 1,
  "userId": 101,
  "channel": "EMAIL",
  "message": "Welcome to the platform",
  "status": "QUEUED"
}
```

## 2) SMS Notification

```bash
curl -X POST http://localhost:8080/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 202,
    "channel": "SMS",
    "message": "Your OTP is 948275"
  }'
```

## 3) PUSH Notification

```bash
curl -X POST http://localhost:8080/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 303,
    "channel": "PUSH",
    "message": "You have a new promotion"
  }'
```

## 4) Validation Error Example

```bash
curl -X POST http://localhost:8080/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": null,
    "channel": "",
    "message": ""
  }'
```

Expected behavior:

- API returns a 4xx validation response.

## 5) High Throughput Test

```bash
for i in {1..20}; do
  curl -s -X POST http://localhost:8080/notifications \
    -H "Content-Type: application/json" \
    -d "{\"userId\": 999, \"channel\": \"EMAIL\", \"message\": \"bulk-$i\"}";
  echo
done
```

## Operational Checks

- Kafka events should appear on `notification-events`.
- Worker failures should appear on `notification-events-dlq`.
- Redis keys should be created for idempotency and rate-limit windows.
