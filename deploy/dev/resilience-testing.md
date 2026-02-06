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
- When a service fails, its circuit breaker opens after reaching failure threshold

**2. Write Gate Aspect**
- Methods annotated with `@ProtectedWrite` are intercepted by `WriteGateAspect`
- Before execution, checks: "Is ANY circuit breaker OPEN or HALF_OPEN?"
- If yes → Rejects with 503 Service Unavailable
- If no → Allows operation to proceed

**3. Read Resilience via Fallbacks**
- Read operations have fallback methods in `@CircuitBreaker` annotations
- When circuit breaker is OPEN, calls fallback instead of throwing error
- Example: If card service fails, `fetchCard()` returns `Mono.empty()` → card field omitted from response
- Result: Reads succeed with graceful degradation (partial data)

**Implementation Files**:
- `customer-gateway/src/main/java/dev/sharanggupta/customergateway/annotation/ProtectedWrite.java` - Annotation marker
- `customer-gateway/src/main/java/dev/sharanggupta/customergateway/aspect/WriteGateAspect.java` - AOP enforcement
- `customer-gateway/src/main/java/dev/sharanggupta/customergateway/service/WriteGateImpl.java` - Circuit breaker check
- `customer-gateway/src/main/java/dev/sharanggupta/customergateway/service/CardServiceImpl.java` - With fallback
- `customer-gateway/src/main/java/dev/sharanggupta/customergateway/service/LoanServiceImpl.java` - With fallback
- `customer-gateway/src/main/java/dev/sharanggupta/customergateway/service/AccountServiceImpl.java` - No fallback (critical)

## Circuit Breaker States

```
CLOSED (Healthy)
  ↓ (failure rate threshold exceeded)
OPEN (Broken - writes blocked)
  ↓ (after wait-duration-in-open-state)
HALF_OPEN (Testing - writes blocked)
  ↓ (successful request)
CLOSED (Recovered)
```

## Dev Configuration

When running with `docker compose` using dev profile, the gateway has **aggressive thresholds** for quick feedback:

| Setting | Dev | Prod | Purpose |
|---------|-----|------|---------|
| slidingWindowSize | 2 | 10 | Evaluate on small sample for quick feedback |
| minimumNumberOfCalls | 1 | 5 | Trip after first failure in dev |
| failureRateThreshold | 100% | 50% | Any failure triggers circuit break |
| waitDurationInOpenState | 5s | 30s | Fast recovery for testing |

This makes circuit breaker behavior immediately visible for testing.

---

## Testing Write Gate

The gateway prevents all write operations when any circuit breaker is OPEN or HALF_OPEN.

### Step 1: Verify Healthy State

```bash
# All services up, writes should succeed
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'
# Expected: 201 Created
```

### Step 2: Stop a Service (Induce Failure)

```bash
docker compose stop card
```

### Step 3: Trigger Circuit Breaker

Make a few read requests to trigger the circuit breaker:

```bash
# Each request to card service fails, contributing to circuit breaker
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"
```

### Step 4: Verify Circuit Breaker State

```bash
curl http://localhost:8000/actuator/circuitbreakers
# Expected: card_service state = "OPEN"
```

### Step 5: Verify Graceful Read Degradation

Reads continue but omit the failed service's data:

```bash
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"
# Expected: 200 OK with graceful degradation
```

**Expected response** (card field completely missing):
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "mobileNumber": "1234567890",
  "account": {
    "accountNumber": "00010012345678901",
    "accountType": "Savings",
    "branchAddress": "123 Main Street, New York"
  }
}
```

### Step 6: Verify Writes Are Blocked

While circuit breaker is OPEN or HALF_OPEN, **all write operations** are rejected:

```bash
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice Wonder", "email": "alice@example.com", "mobileNumber": "5555555555"}'
# Expected: 503 Service Unavailable
```

**Response**:
```json
{
  "apiPath": "/api/customer/onboard",
  "errorCode": "503 SERVICE_UNAVAILABLE",
  "errorMessage": "Write operations are blocked because the card_service circuit breaker is open. This prevents partial updates across services. Please try again in a few moments.",
  "errorTimestamp": "2026-01-31T13:05:00Z"
}
```

**Why this works**: Even though we're writing to the *Account* service, the gateway blocks it because the *Card* service is degraded. This prevents partial data writes.

### Step 7: Recovery

```bash
# Bring service back online
docker compose start card
sleep 10

# Make a READ request (helps circuit breaker transition)
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"
# Expected: 200 OK (card data now present)

sleep 3

# Verify circuit breaker closed
curl http://localhost:8000/actuator/circuitbreakers
# Expected: card_service state = "CLOSED"

# Now writes succeed
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice Wonder", "email": "alice@example.com", "mobileNumber": "5555555555"}'
# Expected: 201 Created
```

---

## Testing Individual Services

### Card Service Circuit Breaker

```bash
# Stop card service
docker compose stop card

# Trigger circuit breaker with read requests
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"

# Reads work but degrade (no card data)
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"
# Response: 200 OK - card field missing

# Writes blocked
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "2222222222"}'
# Response: 503 Service Unavailable

# Recovery
docker compose start card
sleep 10
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"
sleep 3

# Writes work again
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "2222222222"}'
# Response: 201 Created
```

### Account Service Circuit Breaker (Critical)

Account service is **critical** - no graceful degradation possible. Both reads and writes fail:

```bash
# Stop account service
docker compose stop account

# Reads fail (account is required for all operations)
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"
# Response: 503 Service Unavailable

# Writes also fail
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "4444444444"}'
# Response: 503 Service Unavailable

# Recovery
docker compose start account
sleep 10
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"
# Response: 200 OK
```

---

## Monitoring Circuit Breaker State

### Via Actuator Endpoints

```bash
# View all circuit breakers
curl http://localhost:8000/actuator/circuitbreakers
# Response: List of all circuit breakers and their states (CLOSED, OPEN, HALF_OPEN)
```

### Via Logs

```bash
# Watch gateway logs for circuit breaker events
docker compose logs -f gateway | grep -i "circuit\|WriteGate\|blocking"

# Example output:
# [WriteGate] Blocking write operation - circuit breaker 'card_service' is open
# Card service unavailable for mobile: 1234567890
```

---

## Configuration

Circuit breaker configuration is in `application.yaml` (prod defaults) and `application-dev.yaml` (dev overrides).

**Production defaults** (in application.yaml):
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

**Dev overrides** (in application-dev.yaml):
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 2
        minimum-number-of-calls: 1
        failure-rate-threshold: 100
        wait-duration-in-open-state: 5s
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Writes still fail after recovery | Wait 10s after starting service, make a read request, wait 3s more |
| All operations fail even though services are up | Check if account service is running (it's critical) |
| Circuit breaker won't close | Ensure at least one successful request after HALF_OPEN state |
| Inconsistent degradation behavior | Check actuator endpoint to see actual circuit breaker states |
| 400 errors opening circuit breaker | Expected behavior - check `ignore-exceptions` config |

For more details on API examples, see [api-examples.md](api-examples.md).
