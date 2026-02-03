# Circuit Breaker & Resilience Testing

Testing and understanding the gateway's circuit breaker protection against cascading failures.

## Overview

The gateway implements circuit breaker protection using Resilience4J. When a downstream service becomes unavailable, the circuit breaker **blocks all write operations** while allowing read operations to **gracefully degrade** (return partial data).

This prevents:
- Partial/inconsistent data from partial writes
- Cascading failures across services
- Long timeout waits when a service is down

## Architecture

### Three-Layer Protection

**1. Circuit Breaker Per Service**
- Each microservice (Account, Card, Loan) has its own circuit breaker
- Circuit breakers are applied to ALL operations (reads AND writes) via `@CircuitBreaker` annotations
- When a service fails, its circuit breaker opens immediately

**2. Write Gate Interceptor**
- Every HTTP request passes through `WriteGateInterceptor` BEFORE reaching the controller
- For **write requests** (POST, PUT, DELETE), checks: "Is ANY circuit breaker OPEN?"
- If yes → Rejects with 503 Service Unavailable
- If no → Allows request to proceed

**3. Read Resilience via Fallbacks**
- Read operations have fallback methods in `@CircuitBreaker` annotations
- When circuit breaker is OPEN, calls fallback instead of throwing error
- Example: If card service fails, `getCard()` returns `Optional.empty()` → card field omitted from response
- Result: Reads succeed with graceful degradation (partial data)

**Implementation Files**:
- `gateway/src/main/java/dev/sharanggupta/gateway/config/WriteGateInterceptor.java` - Blocks writes
- `gateway/src/main/java/dev/sharanggupta/gateway/service/impl/CardServiceImpl.java` - With fallback
- `gateway/src/main/java/dev/sharanggupta/gateway/service/impl/LoanServiceImpl.java` - With fallback
- `gateway/src/main/java/dev/sharanggupta/gateway/service/impl/AccountServiceImpl.java` - With fallback

## Circuit Breaker States

```
CLOSED (Healthy)
  ↓ (service fails 100% of time in dev config)
OPEN (Broken)
  ↓ (after 5 seconds in dev config)
HALF_OPEN (Testing)
  ↓ (successful request needed)
CLOSED (Recovered)
```

## Dev Configuration

When running with `docker compose`, the gateway is pre-configured with **dev-friendly thresholds** (in `docker-compose.yml`):

| Setting | Dev | Prod | Purpose |
|---------|-----|------|---------|
| slidingWindowSize | 2 | 100 | Evaluate on small sample for quick feedback |
| minimumNumberOfCalls | 1 | 10 | Trip after first failure in dev |
| failureRateThreshold | 100% | 50% | Any failure triggers circuit break |
| waitDurationInOpenState | 5s | 60s | Fast recovery for testing |

This makes circuit breaker behavior immediately visible for testing.

---

## Testing Write Gate

The gateway prevents all write operations when any circuit breaker is open.

### Step 1: Verify Healthy State

```bash
# All services up, writes should succeed
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'
# Expected: 201 Created ✓
```

### Step 2: Stop a Service (Induce Failure)

**Option A: Stop the service container**
```bash
docker compose stop card
```

**Option B: Trigger via failing requests**
```bash
# First request fails, trips the breaker (1 call threshold in dev)
curl http://localhost:8000/api/customer/1234567890/card
```

### Step 3: Verify Graceful Read Degradation

Reads continue but omit the failed service's data:

```bash
curl http://localhost:8000/api/customer/1234567890
# Expected: 200 OK with graceful degradation
```

**Expected response** (card field completely missing):
```json
{
  "mobileNumber": "1234567890",
  "account": {
    "name": "John Complete",
    "email": "complete@example.com",
    "accountNumber": 1241297897,
    "accountType": "Savings",
    "branchAddress": "123 Main Street, New York"
  },
  "loan": {
    "loanNumber": "959721905015",
    "loanType": "Home Loan",
    "totalLoan": 500000,
    "amountPaid": 0,
    "outstandingAmount": 500000
  }
}
```

### Step 4: Verify Writes Are Blocked

While circuit breaker is open, **all write operations** are rejected:

```bash
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice Wonder", "email": "alice@example.com", "mobileNumber": "5555555555"}'
# Expected: 503 Service Unavailable ✗
```

**Response**:
```json
{
  "apiPath": "/api/customer",
  "errorCode": "503 SERVICE_UNAVAILABLE",
  "errorMessage": "System is temporarily unavailable for writes — card-service is degraded",
  "errorTime": "2026-01-31T13:05:00Z"
}
```

**Why this works**: Even though we're writing to the *Account* service, the gateway blocks it because the *Card* service is degraded. This prevents partial data writes.

### Step 5: Recovery

```bash
# Bring service back online
docker compose start card
sleep 5

# Make a READ request (critical! helps circuit breaker transition)
curl http://localhost:8000/api/customer/1234567890
# Expected: 200 OK (card data now present)

sleep 3

# Now writes succeed
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice Wonder", "email": "alice@example.com", "mobileNumber": "5555555555"}'
# Expected: 201 Created ✓
```

**Recovery Process**:
1. Service goes down → Circuit opens
2. Service restarts → Wait `waitDurationInOpenState` (5s in dev)
3. Circuit enters HALF_OPEN state (testing mode)
4. Next request succeeds → Circuit transitions to CLOSED
5. System fully operational

---

## Testing Individual Services

### Card Service Circuit Breaker

```bash
# Stop card service
docker compose stop card

# Reads work but degrade (no card data)
curl http://localhost:8000/api/customer/1234567890
# Response: 200 OK - card field missing

# Writes blocked
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "2222222222"}'
# Response: 503 Service Unavailable ✗

# Recovery
docker compose start card
sleep 5
curl http://localhost:8000/api/customer/9876543210  # Read request
sleep 3

# Writes work again
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "2222222222"}'
# Response: 201 Created ✓
```

### Loan Service Circuit Breaker

```bash
# Stop loan service
docker compose stop loan

# Reads degrade (account & card present, loan missing)
curl http://localhost:8000/api/customer/1234567890

# Writes blocked
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "3333333333"}'
# Response: 503 Service Unavailable ✗

# Recovery
docker compose start loan
sleep 5
curl http://localhost:8000/api/customer/9876543210
sleep 3

curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "3333333333"}'
# Response: 201 Created ✓
```

### Account Service Circuit Breaker (Critical)

Account service is **critical** — no graceful degradation possible. Both reads and writes fail:

```bash
# Stop account service
docker compose stop account

# Reads fail (account is required for all operations)
curl http://localhost:8000/api/customer/1234567890
# Response: 503 Service Unavailable ✗

# Writes also fail
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "4444444444"}'
# Response: 503 Service Unavailable ✗

# Recovery
docker compose start account
sleep 5
curl http://localhost:8000/api/customer/9876543210
sleep 3

# All operations work again
curl http://localhost:8000/api/customer/1234567890
# Response: 200 OK ✓

curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "4444444444"}'
# Response: 201 Created ✓
```

---

## Full Production Incident Simulation

Complete walkthrough of all resilience patterns:

```bash
# STEP 1: System Healthy - Onboard Customer
echo "=== STEP 1: System Healthy - Onboard Customer ==="
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'
# Expected: 201 Created ✓

# STEP 2: Issue Card
echo "=== STEP 2: Issue Card to Customer ==="
curl -X POST http://localhost:8000/api/customer/1234567890/card \
  -H "Content-Type: application/json" \
  -d '{"cardType": "Credit Card", "totalLimit": 100000}'
# Expected: 201 Created ✓

# STEP 3: Issue Loan
echo "=== STEP 3: Issue Loan to Customer ==="
curl -X POST http://localhost:8000/api/customer/1234567890/loan \
  -H "Content-Type: application/json" \
  -d '{"loanType": "Home Loan", "totalLoan": 500000}'
# Expected: 201 Created ✓

# STEP 4: Service Failure
echo -e "\n=== STEP 4: Card Service Goes Down (Simulating Incident) ==="
docker compose stop card

# STEP 5: Graceful Degradation (Read works)
echo -e "\n=== STEP 5: Read Request - Graceful Degradation ==="
curl http://localhost:8000/api/customer/1234567890
# Expected: 200 OK - account & loan present, card missing

# STEP 6: Write Blocked (Protection)
echo -e "\n=== STEP 6: Write Request - Blocked (Write Gate) ==="
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Jane Doe", "email": "jane@example.com", "mobileNumber": "9876543210"}'
# Expected: 503 Service Unavailable ✗

# STEP 7: Service Recovery
echo -e "\n=== STEP 7: Service Recovery ==="
docker compose start card
sleep 5

# STEP 8: Circuit Transition (Read + Wait)
echo -e "\n=== STEP 8: Help Circuit Breaker Transition ==="
curl http://localhost:8000/api/customer/1234567890
sleep 3

# STEP 9: System Fully Operational
echo -e "\n=== STEP 9: Writes Restored - System Fully Operational ==="
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Jane Doe", "email": "jane@example.com", "mobileNumber": "9876543210"}'
# Expected: 201 Created ✓
```

Save as `simulate-incident.sh` and run:
```bash
bash simulate-incident.sh
```

---

## Monitoring Circuit Breaker State

### Via Actuator Endpoints

```bash
# View all circuit breakers
curl http://localhost:8000/actuator/circuitbreakers
# Response: List of all circuit breakers and their states (CLOSED, OPEN, HALF_OPEN)

# Details for specific circuit breaker
curl http://localhost:8000/actuator/circuitbreakers/card-service
```

### Via Logs

```bash
# Watch customergateway logs for circuit breaker events
docker compose logs -f customergateway | grep -i "circuit\|rejecting"

# Example output:
# [WriteGateInterceptor] Rejecting POST /api/customer — circuit breaker 'card-service' is open
# [CardServiceImpl] Card service unavailable for 1234567890
```

---

## Unit Tests

The gateway includes comprehensive tests:

```bash
cd customergateway

# All tests (including circuit breaker tests)
./mvnw test

# Only write gate tests
./mvnw test -Dtest=WriteGateTest

# Only customer journey tests
./mvnw test -Dtest=CustomerJourneyTest
```

**WriteGateTest**:
- Simulates service failure
- Verifies writes return 503
- Verifies reads continue with graceful degradation

**CustomerJourneyTest**:
- Full customer lifecycle (onboard, fetch, update, offboard)
- Tests with all services healthy
- Tests graceful degradation when card service is down
- Verifies offboarding works even when services fail

---

## Configuration

To change circuit breaker thresholds, edit `docker-compose.yml`:

```yaml
environment:
  RESILIENCE4J_CIRCUITBREAKER_INSTANCES_CARD_SERVICE_SLIDINGWINDOWSIZE: "2"
  RESILIENCE4J_CIRCUITBREAKER_INSTANCES_CARD_SERVICE_MINIMUMNUMBEROFCALLS: "1"
  RESILIENCE4J_CIRCUITBREAKER_INSTANCES_CARD_SERVICE_FAILURERATHRESHOLD: "100"
  RESILIENCE4J_CIRCUITBREAKER_INSTANCES_CARD_SERVICE_WAITDURATIONINOPENSTATE: "5s"
```

For production-like behavior, use:
```yaml
slidingWindowSize: 100
minimumNumberOfCalls: 10
failureRateThreshold: 50
waitDurationInOpenState: 60s
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Writes still fail after recovery | Wait 5s after starting service, make a read request, wait 3s more |
| All operations fail even though services are up | Check if account service is running (it's critical) |
| Circuit breaker won't close | Ensure at least one successful request after HALF_OPEN state |
| Inconsistent degradation behavior | Check actuator endpoint to see actual circuit breaker states |

For more details on API examples, see [api-examples.md](api-examples.md).
