# Dev Environment Setup

This directory contains the configuration to run EazyBank microservices in the dev environment.

## Prerequisites

- Docker and Docker Compose installed
- Java 25
- Maven (or use the included Maven wrapper in each service)

## Running Everything in Docker

### 1. Build the Docker Images

First, build the Docker images for all microservices using the provided script:

```bash
./build-images.sh
```

This uses Google Jib to build container images directly to your local Docker daemon.

### 2. Start All Services

```bash
docker compose up -d
```

This starts:
- **eazybank-postgres** - PostgreSQL 17 database
- **eazybank-account** - Account microservice (port 8080)
- **eazybank-card** - Card microservice (port 9000)
- **eazybank-loan** - Loan microservice (port 8090)
- **eazybank-gateway** - API Gateway (port 8000)

### 3. Stop All Services

```bash
docker compose down
```

To also remove the persisted database data:
```bash
docker compose down -v
```

## Running Microservices Locally (Alternative)

If you prefer to run microservices directly on your machine (useful for development with hot-reload):

### 1. Start Only the Database

```bash
docker compose up -d postgres
```

### 2. Run Each Microservice

From the repository root:

**Account Service (port 8080):**
```bash
cd ../../account
./mvnw spring-boot:run
```

**Card Service (port 9000):**
```bash
cd ../../card
./mvnw spring-boot:run
```

**Loan Service (port 8090):**
```bash
cd ../../loan
./mvnw spring-boot:run
```

**Gateway (port 8000):**
```bash
cd ../../gateway
./mvnw spring-boot:run
```

When running locally, the gateway connects to services on their default localhost ports (account:8080, card:9000, loan:8090) as configured in `application.yaml`.

## Database Details

PostgreSQL 17 with three databases:
- `accountdb` - for the account microservice
- `carddb` - for the card microservice
- `loandb` - for the loan microservice

Connection details (when running locally):
- Host: `localhost`
- Port: `5432`
- Username: `postgres`
- Password: `postgres`

## Verifying Services

### Health Checks

```bash
# Gateway health
curl http://localhost:8000/actuator/health
# Expected: {"status":"UP"}

# Account service health (direct)
curl http://localhost:8080/account/actuator/health
# Expected: {"status":"UP"}

# Card service health (direct)
curl http://localhost:9000/card/actuator/health
# Expected: {"status":"UP"}

# Loan service health (direct)
curl http://localhost:8090/loan/actuator/health
# Expected: {"status":"UP"}
```

### Gateway API

The gateway exposes aggregated customer APIs and proxies downstream service APIs.

**Customer lifecycle:**
```bash
# Onboard a customer
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'
# Expected: {"statusCode":"201","statusMessage":"Customer onboarded successfully"}

# Get customer details (aggregated account, card, and loan)
curl http://localhost:8000/api/customer/1234567890
# Expected: JSON with mobileNumber, account, card, and loan details

# Update customer profile
curl -X PUT http://localhost:8000/api/customer/1234567890 \
  -H "Content-Type: application/json" \
  -d '{"name": "John Updated", "email": "john.updated@example.com"}'
# Expected: 204 No Content

# Offboard customer
curl -X DELETE http://localhost:8000/api/customer/1234567890
# Expected: 204 No Content
```

**Card management (via gateway):**
```bash
# Issue a card
curl -X POST http://localhost:8000/api/customer/1234567890/card \
  -H "Content-Type: application/json" \
  -d '{"cardType": "Credit Card", "totalLimit": 100000}'
# Expected: {"statusCode":"201","statusMessage":"Card issued successfully"}

# Get card details
curl http://localhost:8000/api/customer/1234567890/card
# Expected: JSON with card details

# Update card
curl -X PUT http://localhost:8000/api/customer/1234567890/card \
  -H "Content-Type: application/json" \
  -d '{"cardType": "Credit Card", "totalLimit": 200000, "amountUsed": 5000}'
# Expected: 204 No Content

# Cancel card
curl -X DELETE http://localhost:8000/api/customer/1234567890/card
# Expected: 204 No Content
```

**Loan management (via gateway):**
```bash
# Apply for a loan
curl -X POST http://localhost:8000/api/customer/1234567890/loan \
  -H "Content-Type: application/json" \
  -d '{"loanType": "Home Loan", "totalLoan": 500000}'
# Expected: {"statusCode":"201","statusMessage":"Loan created successfully"}

# Get loan details
curl http://localhost:8000/api/customer/1234567890/loan
# Expected: JSON with loan details

# Update loan
curl -X PUT http://localhost:8000/api/customer/1234567890/loan \
  -H "Content-Type: application/json" \
  -d '{"loanType": "Home Loan", "totalLoan": 500000, "amountPaid": 50000}'
# Expected: 204 No Content

# Close loan
curl -X DELETE http://localhost:8000/api/customer/1234567890/loan
# Expected: 204 No Content
```

### Direct Downstream APIs

The downstream services are also accessible directly for debugging:

**Account Service (port 8080):**
```bash
curl -X POST http://localhost:8080/account/api/create \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'

curl http://localhost:8080/account/api/fetch?mobileNumber=1234567890

curl -X PUT http://localhost:8080/account/api/update \
  -H "Content-Type: application/json" \
  -d '{"name": "John Updated", "email": "john@example.com", "mobileNumber": "1234567890", "accountDto": {"accountNumber": <ACCOUNT_NUMBER>, "accountType": "Savings", "branchAddress": "123 New Street"}}'

curl -X DELETE "http://localhost:8080/account/api/delete?mobileNumber=1234567890"
```

**Card Service (port 9000):**
```bash
curl -X POST http://localhost:9000/card/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "cardType": "Credit Card", "totalLimit": 100000}'

curl http://localhost:9000/card/api?mobileNumber=1234567890

curl -X PUT http://localhost:9000/card/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "cardType": "Credit Card", "totalLimit": 200000, "amountUsed": 5000}'

curl -X DELETE "http://localhost:9000/card/api?mobileNumber=1234567890"
```

**Loan Service (port 8090):**
```bash
curl -X POST http://localhost:8090/loan/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "loanType": "Home Loan", "totalLoan": 500000}'

curl http://localhost:8090/loan/api?mobileNumber=1234567890

curl -X PUT http://localhost:8090/loan/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "loanType": "Home Loan", "totalLoan": 500000, "amountPaid": 50000}'

curl -X DELETE "http://localhost:8090/loan/api?mobileNumber=1234567890"
```

### Swagger UI

API documentation is available at:
- Gateway: http://localhost:8000/swagger-ui.html
- Account: http://localhost:8080/account/swagger-ui.html
- Card: http://localhost:9000/card/swagger-ui.html
- Loan: http://localhost:8090/loan/swagger-ui.html
