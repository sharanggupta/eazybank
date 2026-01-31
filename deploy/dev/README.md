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

## Circuit Breaker & Resilience Testing

The gateway implements circuit breaker protection using Resilience4J. When a downstream service becomes unavailable, the circuit breaker trips and **blocks all write operations** while allowing read operations to gracefully degrade.

### Prerequisites

All commands in this section assume you're running them from the **`deploy/dev/` directory** (where `docker-compose.yml` is located):

```bash
cd deploy/dev
# All docker compose commands below run from here
```

### Quick Note: Dev Configuration

When running with `docker compose`, the gateway is **pre-configured with dev-friendly circuit breaker thresholds** (in `docker-compose.yml`):
- **slidingWindowSize**: 2 (default: 100) — Evaluate on tiny sample for immediate feedback
- **minimumNumberOfCalls**: 1 (default: 10) — Trip after first failure
- **failureRateThreshold**: 100% (default: 50%) — Any failure triggers circuit break
- **waitDurationInOpenState**: 5s (default: 60s) — Quick recovery for faster testing

This makes it super easy to test circuit breaker behavior locally. If you need production-like thresholds, update the environment variables in `docker-compose.yml`.

### Understanding the Behavior

1. **Healthy State**: All services responding normally → Reads and writes work
2. **Service Failure**: Service returns 500 or times out → Circuit breaker opens after 1 failing request
3. **Circuit Open**: Writes are blocked with 503 Service Unavailable → Reads continue without the failed service
4. **Recovery**: Service recovers → Circuit closes after 60 seconds

### Testing the Write Gate (Blocking Writes on Circuit Breaker)

The gateway prevents **all write operations** (POST, PUT, DELETE) when any service's circuit breaker is open.

#### Step 1: Verify Healthy State

```bash
# All services up, writes should succeed
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'
# Expected: 201 Created
```

#### Step 2: Artificially Induce Card Service Failure

**Run from `deploy/dev/` directory**

**Option A: Stop the card service**
```bash
docker compose stop card
# This stops the card service container
```

**If you're in a different directory**, use:
```bash
docker compose -f deploy/dev/docker-compose.yml stop card
```

**Option B: Make requests to trigger the circuit breaker** (if service is still running but failing)
```bash
# Trigger enough failures to trip the breaker (1 request with minimal threshold)
curl http://localhost:8000/api/customer/1234567890/card
# First failure will trip the breaker
```

#### Step 3: Verify Graceful Read Degradation (Circuit Breaker Active ✅)

Reads continue to work but gracefully degrade (card data is omitted):

```bash
# Reads work, but card data is absent
curl http://localhost:8000/api/customer/1234567890
# Expected: 200 OK with graceful degradation
# Response includes account & loan, but "card" field is MISSING
# Example response:
# {
#   "mobileNumber": "1234567890",
#   "account": {
#     "name": "John Complete",
#     "email": "complete@example.com",
#     "accountNumber": 1241297897,
#     "accountType": "Savings",
#     "branchAddress": "123 Main Street, New York"
#   },
#   "loan": {
#     "loanNumber": "959721905015",
#     "loanType": "Home Loan",
#     "totalLoan": 500000,
#     "amountPaid": 0,
#     "outstandingAmount": 500000
#   }
#   ← Notice: "card" field is completely omitted (circuit breaker is working!)
# }
```

**This demonstrates the circuit breaker fallback behavior**: When the card service fails, the `CardServiceImpl.getCard()` method's fallback returns `Optional.empty()`, allowing reads to continue without card data.

#### Step 5: Recover the Service

```bash
# Bring card service back online (run from deploy/dev/)
docker compose start card

# Wait 5 seconds for container to be ready
sleep 5

# IMPORTANT: Make a READ request to help circuit breaker transition from OPEN → CLOSED
# The circuit breaker needs a successful call to verify the service is healthy
curl http://localhost:8000/api/customer/1234567890
# Expected: 200 OK (reads continue to work)

# Wait 3 more seconds for circuit breaker state machine to transition
sleep 3

# Now attempt write - should succeed
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice Wonder", "email": "alice@example.com", "mobileNumber": "5555555555"}'
# Expected: 201 Created ✓
```

**How Circuit Breaker Recovery Works**:
1. Service goes down → Circuit opens (blocks writes)
2. Service restarts → Wait `waitDurationInOpenState` (5s in dev)
3. Circuit enters HALF_OPEN state (testing mode)
4. Make a read request → If successful, circuit transitions to CLOSED
5. Writes are now allowed again

**If running from a different directory**:
```bash
docker compose -f deploy/dev/docker-compose.yml start card
sleep 5
curl http://localhost:8000/api/customer/1234567890   # Help circuit breaker transition
sleep 3
# Then run the write curl command above
```

### Testing Individual Service Resilience

Each service's circuit breaker trips after 1 failure (with dev config). Graceful degradation allows reads to continue without the failed service, while writes are blocked.

**All commands below assume you're in the `deploy/dev/` directory.**

#### Card Service Circuit Breaker

```bash
# Step 1: Stop card service (simulate failure) - run from deploy/dev/
docker compose stop card

# Step 2: Trigger the circuit breaker (1st request fails, CB opens)
curl http://localhost:8000/api/customer/1234567890
# Response: 200 OK - but card field is missing/null

# Step 3: Verify writes are blocked
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "2222222222"}'
# Response: 503 Service Unavailable ✗

# Step 4: Bring card service back
docker compose start card
sleep 5

# Step 5: Help circuit breaker transition (make a read request)
curl 'http://localhost:8000/api/customer/9876543210'
sleep 3

# Step 6: Verify writes work again
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "2222222222"}'
# Response: 201 Created ✓
```

#### Loan Service Circuit Breaker

```bash
# Step 1: Stop loan service
docker compose stop loan

# Step 2: Read customer (graceful degradation - loan data missing)
curl http://localhost:8000/api/customer/1234567890
# Response: 200 OK - account & card present, loan is missing

# Step 3: Writes blocked
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "3333333333"}'
# Response: 503 Service Unavailable ✗

# Step 4: Recover
docker compose start loan
sleep 5

# Step 5: Help circuit breaker transition (make a read request)
curl 'http://localhost:8000/api/customer/9876543210'
sleep 3

# Step 6: Verify writes work
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "3333333333"}'
# Response: 201 Created ✓
```

#### Account Service Circuit Breaker (Critical)

Account service is **critical** — if it fails, **all customer operations fail** (no graceful degradation possible).

```bash
# Step 1: Stop account service
docker compose stop account

# Step 2: Try to read customer (fails - account required)
curl http://localhost:8000/api/customer/1234567890
# Response: 503 Service Unavailable ✗

# Step 3: Try to write (also blocked)
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "4444444444"}'
# Response: 503 Service Unavailable ✗

# Step 4: Recover account service
docker compose start account
sleep 5

# Step 5: Help circuit breaker transition (make a read request)
curl 'http://localhost:8000/api/customer/9876543210'
sleep 3

# Step 6: Operations work again
curl http://localhost:8000/api/customer/1234567890
# Response: 200 OK ✓

curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "4444444444"}'
# Response: 201 Created ✓
```

### Circuit Breaker Configuration (Already Optimized for Dev)

When using `docker compose`, the gateway is **automatically configured with dev-friendly thresholds** via environment variables in `docker-compose.yml`.

**Current Dev Config** (already applied when you run `docker compose up`):
```
slidingWindowSize: 2               ✓ Evaluate on small sample
minimumNumberOfCalls: 1            ✓ Trip after first failure
failureRateThreshold: 100%         ✓ Any failure opens circuit
waitDurationInOpenState: 5s        ✓ Fast recovery (5 seconds)
```

**Default Production Config** (reference only, if you run services locally):
```yaml
resilience4j:
  circuitbreaker:
    instances:
      account-service:
        slidingWindowSize: 100            # Monitor last 100 calls
        minimumNumberOfCalls: 10          # Need 10+ calls to evaluate
        failureRateThreshold: 50          # Open if 50%+ fail
        waitDurationInOpenState: 60s      # Wait 60 seconds before retry
```

**To override dev config**: Edit `docker-compose.yml` and change the `RESILIENCE4J_*` environment variables for the gateway service.

### Manual Circuit Breaker Testing (in test environment)

The gateway includes comprehensive end-to-end tests that simulate circuit breaker behavior:

```bash
cd gateway

# Run all integration tests (including circuit breaker tests)
./mvnw test

# Run only write-gate tests
./mvnw test -Dtest=WriteGateTest

# Run only customer journey tests
./mvnw test -Dtest=CustomerJourneyTest
```

#### What the Tests Do

**WriteGateTest**: Validates that writes are blocked when circuit breaker is open
- Simulates card service failure
- Verifies writes return 503
- Verifies reads continue with graceful degradation

**CustomerJourneyTest**: Full user workflows
- Onboard customer (write)
- Fetch with all services healthy (read)
- Graceful degradation when card service is down
- Offboard even when services fail

### Monitoring Circuit Breaker State

#### Via Actuator Endpoints

```bash
# View circuit breaker status
curl http://localhost:8000/actuator/circuitbreakers
# Response shows all circuit breakers and their state (CLOSED, OPEN, HALF_OPEN)

# Details for a specific circuit breaker
curl http://localhost:8000/actuator/circuitbreakers/card-service
```

#### Via Logs

Gateway logs all circuit breaker events:

```bash
# Watch gateway logs
docker compose logs -f gateway | grep -i "circuit\|rejecting"

# Example output:
# [WriteGateInterceptor] Rejecting POST /api/customer — circuit breaker 'card-service' is open
```

### Full Scenario: Simulate Production Incident

A complete walkthrough demonstrating all resilience patterns.

**Run this from the `deploy/dev/` directory** (where `docker-compose.yml` is):
```bash
cd deploy/dev
```

Then run this script:

```bash
echo "=== STEP 1: System Healthy - Onboard Customer ==="
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'
# Expected: 201 Created ✓

echo "=== STEP 2: Issue Card to Customer ==="
curl -X POST http://localhost:8000/api/customer/1234567890/card \
  -H "Content-Type: application/json" \
  -d '{"cardType": "Credit Card", "totalLimit": 100000}'
# Expected: 201 Created ✓

echo "=== STEP 3: Issue Loan to Customer ==="
curl -X POST http://localhost:8000/api/customer/1234567890/loan \
  -H "Content-Type: application/json" \
  -d '{"loanType": "Home Loan", "totalLoan": 500000}'
# Expected: 201 Created ✓

echo -e "\n=== STEP 4: Card Service Goes Down (Simulating Incident) ==="
docker compose stop card

echo -e "\n=== STEP 5: Read Request - Graceful Degradation ==="
curl http://localhost:8000/api/customer/1234567890
# Expected: 200 OK
# Response shows: account ✓, card ✗ (missing), loan ✓

echo -e "\n=== STEP 6: Write Request - Blocked (Write Gate) ==="
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Jane Doe", "email": "jane@example.com", "mobileNumber": "9876543210"}'
# Expected: 503 Service Unavailable ✗

echo -e "\n=== STEP 7: Service Recovery ==="
docker compose start card
echo "Waiting 5 seconds for container to be ready..."
sleep 5

echo -e "\n=== STEP 8: Help Circuit Breaker Transition ==="
echo "Making a read request to help circuit breaker transition from OPEN → CLOSED..."
curl http://localhost:8000/api/customer/1234567890
# Expected: 200 OK (reads work, card data now present)
echo -e "\nWaiting 3 more seconds for circuit breaker state transition..."
sleep 3

echo -e "\n=== STEP 9: Writes Restored - System Fully Operational ==="
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Jane Doe", "email": "jane@example.com", "mobileNumber": "9876543210"}'
# Expected: 201 Created ✓
```

**Run this as a bash script from `deploy/dev/`** for a clear demonstration of all resilience patterns!

You can also save it to a file and run it:
```bash
# From deploy/dev/ directory
bash simulate-incident.sh  # if saved to file
```
