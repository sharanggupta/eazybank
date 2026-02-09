# Dev Environment Setup

This directory contains the configuration to run EazyBank microservices in the dev environment.

## Prerequisites

- Docker and Docker Compose installed
- Java 25
- Maven (or use the included Maven wrapper in each service)

## Running Everything in Docker

### 1. Build the Docker Images

```bash
./build-images.sh
```

Uses Google Jib to build container images directly to your local Docker daemon.

### 2. Start All Services

```bash
docker compose up -d
```

Starts all services:
- **postgres** - PostgreSQL 17 database
- **account** - Account microservice (port 8080)
- **card** - Card microservice (port 9000)
- **loan** - Loan microservice (port 8090)
- **gateway** - API Gateway (port 8000)

### 3. Stop All Services

```bash
docker compose down
```

To also remove database data:
```bash
docker compose down -v
```

## Running Microservices Locally

For development with hot-reload, run services directly on your machine:

### 1. Start Only the Database

```bash
docker compose up -d postgres
```

### 2. Run Each Microservice

From repository root:

```bash
# Account (port 8080)
cd account && ./mvnw spring-boot:run

# Card (port 9000)
cd card && ./mvnw spring-boot:run

# Loan (port 8090)
cd loan && ./mvnw spring-boot:run

# Gateway (port 8000)
cd customer-gateway && ./mvnw spring-boot:run
```

Services connect via `application.yaml` configuration.

## Database

PostgreSQL 17 with three separate databases:
- `accountdb` - Account microservice
- `carddb` - Card microservice
- `loandb` - Loan microservice

**Connection details** (localhost):
- Host: `localhost:5432`
- Username: `postgres`
- Password: `postgres`

## Verify Services

### Health Checks

```bash
curl http://localhost:8000/actuator/health           # Gateway
curl http://localhost:8080/account/actuator/health   # Account
curl http://localhost:9000/card/actuator/health      # Card
curl http://localhost:8090/loan/actuator/health      # Loan
```

Expected response: `{"status":"UP"}`

### API Access

**Gateway API** (aggregated endpoints):
```bash
curl http://localhost:8000/api/customer              # Get customer
curl http://localhost:8000/api/customer/{id}/card    # Get card
curl http://localhost:8000/api/customer/{id}/loan    # Get loan
```

**Swagger UI**:
- Gateway: http://localhost:8000/swagger-ui.html
- Account: http://localhost:8080/account/swagger-ui.html
- Card: http://localhost:9000/card/swagger-ui.html
- Loan: http://localhost:8090/loan/swagger-ui.html

## Testing Observability (Traces, Metrics, Logs)

Complete observability stack runs with Docker Compose. After `docker compose up -d`, verify all components are healthy.

### Generate Test Traces

Create a customer (calls all 4 services):
```bash
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "mobileNumber": "9876543210"
  }'
```

Or fetch customer details:
```bash
curl http://localhost:8000/api/customer/details/9876543210
```

### View Traces in Grafana

1. **Access Grafana**: http://localhost:3000 (admin / admin)
2. **Go to Explore** (left sidebar)
3. **Select Tempo** datasource (top-left dropdown)
4. **Select service**: `customer-gateway`, `account`, `card`, or `loan`
5. **Run Query** - click green button
6. **Click any trace** to see full request flow across services

Each trace shows:
- All participating services with timing
- HTTP methods, URLs, status codes
- Trace ID and span hierarchy
- Service dependency chain

### View Metrics

1. **Go to Explore** in Grafana
2. **Select Prometheus** datasource
3. **Sample queries**:
   ```
   rate(http_server_requests_seconds_count[5m])  # Request rate
   histogram_quantile(0.95, http_server_requests_seconds_bucket)  # P95 latency
   ```

### View Logs

1. **Go to Explore** in Grafana
2. **Select Loki** datasource
3. **Query by service**: `{service="account"}`
4. **Or by trace**: `{traceId="<trace-id-from-tempo>"}`

### Verify Components

Check traces are flowing:
```bash
docker compose logs otel-collector | grep "ResourceSpans"
```

Check metrics are scraped:
```bash
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets | length'
```

Check logs are aggregated:
```bash
curl 'http://localhost:3100/loki/api/v1/query?query={service="account"}' | jq '.data.result | length'
```

---

## Next Steps

- **API Examples & Testing**: See [api-examples.md](api-examples.md)
- **Circuit Breaker & Resilience**: See [resilience-testing.md](resilience-testing.md)
- **Configuration Options**: See [../../docs/configuration-reference.md](../../docs/configuration-reference.md)