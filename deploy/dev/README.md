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

## Next Steps

- **API Examples & Testing**: See [api-examples.md](api-examples.md)
- **Circuit Breaker & Resilience**: See [resilience-testing.md](resilience-testing.md)
- **Configuration Options**: See [../../docs/configuration-reference.md](../../docs/configuration-reference.md)