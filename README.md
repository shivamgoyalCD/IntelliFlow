# IntelliFlow

IntelliFlow is a production-grade real-time data pipeline monitoring platform.

## Modules

- common
- order-service
- auth-service
- payment-service
- log-aggregator
- anomaly-detector
- metrics-collector
- ai-summarizer
- api-gateway
- dashboard
- load-simulator

## Local Infrastructure

Start the local infrastructure:

```powershell
docker compose up -d
docker compose ps
```

Wait until the Kafka containers are healthy, then create Kafka topics:

```powershell
.\scripts\create-kafka-topics.ps1
```

Expected topics:

- logs
- metrics
- alerts
- incidents
- dlq

Verify produce and consume with the `logs` topic.

In one terminal, start a consumer:

```powershell
docker compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:29092 --topic logs --from-beginning
```

In another terminal, produce a test event:

```powershell
docker compose exec kafka-1 kafka-console-producer --bootstrap-server kafka-1:29092 --topic logs
```

Then paste this message into the producer and press Enter:

```json
{"eventId":"local-test","timestamp":"2026-07-06T00:00:00Z","serviceName":"local","level":"INFO","traceId":"trace-local","spanId":"span-local","message":"local kafka verification","metadata":{"source":"manual"}}
```

Stop the local infrastructure:

```powershell
docker compose down
```

## End-to-End Local Verification

Build and start infrastructure plus producer services:

```powershell
docker compose up -d --build
```

Create the Kafka topics:

```powershell
.\scripts\create-kafka-topics.ps1
```

Check service health:

```powershell
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

Start a logs consumer:

```powershell
docker compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:29092 --topic logs --from-beginning
```

Create an order with a trace id:

```powershell
$headers = @{ "X-Trace-Id" = "trace-local-e2e" }
$body = @{
  customerId = "customer-local-1"
  amount = 120.50
  itemCount = 2
  paymentMethod = "LOCAL_DEV_CARD"
} | ConvertTo-Json

$order = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/orders -Headers $headers -ContentType "application/json" -Body $body
$order
```

Fetch the created order:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8081/api/v1/orders/$($order.id)" -Headers $headers
```

The `logs` topic should contain order and payment events with the same `traceId`.
