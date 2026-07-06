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
