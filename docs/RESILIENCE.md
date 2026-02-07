# Resilience & Fault Tolerance

How EazyBank handles failures in downstream services using circuit breakers and graceful degradation.

---

## Overview

EazyBank implements three layers of resilience:

1. **Circuit Breaker** - Detects failing services and prevents cascading failures
2. **Write Gate** - Blocks writes when any service is degraded
3. **Graceful Degradation** - Read operations return partial data instead of failing

---

## Circuit Breaker Pattern

### How It Works

```
CLOSED (Healthy)
  ↓ (failure rate exceeds threshold)
OPEN (Broken - requests fail fast)
  ↓ (after wait duration elapses)
HALF_OPEN (Testing - single request sent)
  ↓ (success → CLOSED, failure → OPEN)
```

### States Explained

| State | Behavior | Requests | Recovery |
|-------|----------|----------|----------|
| **CLOSED** | Normal operation | Pass through to service | N/A |
| **OPEN** | Service is failing | Fail immediately (no network call) | Wait, then try HALF_OPEN |
| **HALF_OPEN** | Testing recovery | Allow single request | Success → CLOSED, Failure → OPEN |

### Configuration

#### Development (Fast Feedback)

File: `customer-gateway/src/main/resources/application-dev.yaml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      account-service:
        failure-rate-threshold: 100        # Trip on 1st failure
        minimum-number-of-calls: 1         # After 1 call
        sliding-window-size: 2             # Sample size: 2 calls
        wait-duration-in-open-state: 5s    # Recover after 5s
      card-service:                        # Same for card
        failure-rate-threshold: 100
        minimum-number-of-calls: 1
        sliding-window-size: 2
        wait-duration-in-open-state: 5s
      loan-service:                        # Same for loan
        failure-rate-threshold: 100
        minimum-number-of-calls: 1
        sliding-window-size: 2
        wait-duration-in-open-state: 5s
```

**Purpose**: Circuit breaker trips immediately when service fails, making testing easy

#### Production (Stable)

File: `customer-gateway/src/main/resources/application.yaml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      account-service:
        failure-rate-threshold: 50         # Trip at 50% failure rate
        minimum-number-of-calls: 5         # After 5 calls
        sliding-window-size: 10            # Sample size: 10 calls
        wait-duration-in-open-state: 30s   # Recover after 30s
```

**Purpose**: Prevent false positives; allow some failures without breaking circuit

### Check Circuit Breaker Status

```bash
# See all circuit breakers
curl http://localhost:8000/actuator/circuitbreakers

# Response example:
{
  "circuitBreakers": [
    "account-service",
    "card-service",
    "loan-service"
  ]
}

# Detailed status of one circuit breaker
curl http://localhost:8000/actuator/circuitbreakers/account-service

# Response:
{
  "failureRate": 50.0,
  "numberOfNotPermittedCalls": 3,
  "numberOfFailedCalls": 1,
  "numberOfSuccessfulCalls": 1,
  "state": "OPEN"  # or "CLOSED" or "HALF_OPEN"
}
```

---

## Write Gate Pattern

### How It Works

Writes are blocked when ANY circuit breaker is OPEN or HALF_OPEN:

```
Request to @ProtectedWrite endpoint
  ↓
Aspect checks: Any CB open/half-open?
  ↓
  If YES → Reject with 503 Service Unavailable
  If NO  → Allow request to proceed
```

### Why Write Gate?

Prevents partial writes across services:

```
Without Write Gate:
1. Customer created successfully ✓
2. Card creation fails ✗
3. Result: Inconsistent data (customer without card)

With Write Gate:
1. Card service is down
2. Write Gate detects OPEN circuit breaker
3. Rejects customer creation request with 503
4. No partial data created
```

### Protected Endpoints

```java
@PostMapping("/api/customer/onboard")
@ProtectedWrite  // Blocked if any service down
public Mono<ResponseEntity<ResponseDto>> onboardCustomer(...) { ... }

@PutMapping("/api/customer/update")
@ProtectedWrite  // Blocked if any service down
public Mono<ResponseEntity<ResponseDto>> updateCustomer(...) { ... }

@DeleteMapping("/api/customer/offboard/{mobileNumber}")
@ProtectedWrite  // Blocked if any service down
public Mono<ResponseEntity<ResponseDto>> offboardCustomer(...) { ... }

@PostMapping("/api/customer/{mobileNumber}/card")
@ProtectedWrite  // Blocked if any service down
public Mono<ResponseEntity<CardDto>> createCard(...) { ... }

// etc.
```

### Test Write Gate

```bash
# 1. Service is healthy - write succeeds
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane Doe","email":"jane@example.com","mobileNumber":"9876543210"}'
# Response: 201 Created

# 2. Stop account service
docker compose stop account

# Wait for circuit breaker to open (few seconds with dev profile)
sleep 10

# 3. Try to write - blocked by Write Gate
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name":"John Smith","email":"john@example.com","mobileNumber":"9999999999"}'
# Response: 503 Service Unavailable
# Error: "Writes blocked: account-service circuit breaker is open"

# 4. Service recovers
docker compose start account
sleep 10

# 5. Write succeeds again
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name":"John Smith","email":"john@example.com","mobileNumber":"9999999999"}'
# Response: 201 Created
```

---

## Graceful Degradation

### How It Works

Read operations have fallback methods that return partial data instead of failing:

```
Request to GET /api/customer/details/{mobileNumber}
  ↓
Fetch account (critical) → Fail if down
  ↓
Fetch card (optional) → Return empty if circuit open
  ↓
Fetch loan (optional) → Return empty if circuit open
  ↓
Combine results → Return what we have
```

### Implementation

```java
// Card service with fallback
@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    @CircuitBreaker(name = "card-service", fallbackMethod = "fallbackFetchCard")
    public Mono<CardDto> fetchCard(String mobileNumber) {
        return webClient.get()
            .uri("/api/" + mobileNumber)
            .retrieve()
            .bodyToMono(CardDto.class);
    }

    // When circuit is OPEN, this method is called instead
    private Mono<CardDto> fallbackFetchCard(String mobileNumber, Throwable t) {
        log.warn("Card service unavailable for mobile: {}", mobileNumber);
        return Mono.empty();  // Return empty, not error
    }
}

// Loan service with fallback (same pattern)
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    @CircuitBreaker(name = "loan-service", fallbackMethod = "fallbackFetchLoan")
    public Mono<LoanDto> fetchLoan(String mobileNumber) {
        return webClient.get()
            .uri("/api/" + mobileNumber)
            .retrieve()
            .bodyToMono(LoanDto.class);
    }

    private Mono<LoanDto> fallbackFetchLoan(String mobileNumber, Throwable t) {
        log.warn("Loan service unavailable for mobile: {}", mobileNumber);
        return Mono.empty();
    }
}

// Account service WITHOUT fallback (critical, not optional)
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    @CircuitBreaker(name = "account-service")  // No fallback!
    public Mono<AccountDto> fetchAccount(String mobileNumber) {
        return webClient.get()
            .uri("/api/" + mobileNumber)
            .retrieve()
            .bodyToMono(AccountDto.class);
    }
}

// Customer profile aggregation
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    public Mono<CustomerProfile> getCustomerDetails(String mobileNumber) {
        return Mono.zip(
            accountService.fetchAccount(mobileNumber),     // Critical, fails if down
            cardService.fetchCard(mobileNumber),           // Optional, gracefully degrades
            loanService.fetchLoan(mobileNumber)            // Optional, gracefully degrades
        )
        .map(tuple -> new CustomerProfile(
            tuple.getT1(),  // Account (non-null)
            tuple.getT2(),  // Card (may be null)
            tuple.getT3()   // Loan (may be null)
        ));
    }
}
```

### Test Graceful Degradation

```bash
# 1. Create complete customer profile
curl -X POST http://localhost:8000/api/customer/onboard \
  -d '{"name":"Test User","email":"test@example.com","mobileNumber":"5555555555"}'

curl -X POST http://localhost:8000/api/customer/5555555555/card \
  -d '{"cardType":"Credit Card","totalLimit":100000}'

curl -X POST http://localhost:8000/api/customer/5555555555/loan \
  -d '{"loanType":"Home Loan","totalLoan":500000}'

# 2. Verify complete profile
curl http://localhost:8000/api/customer/details/5555555555
# Response: {"account": {...}, "card": {...}, "loan": {...}}

# 3. Stop card service
docker compose stop card
sleep 10  # Wait for circuit breaker to open

# 4. Fetch details - card missing, but account and loan present
curl http://localhost:8000/api/customer/details/5555555555
# Response: {"account": {...}, "card": null, "loan": {...}}
#           ↑ Account present      ↑ Card absent     ↑ Loan present

# 5. Stop loan service too
docker compose stop loan
sleep 10

# 6. Fetch details - only account present
curl http://localhost:8000/api/customer/details/5555555555
# Response: {"account": {...}, "card": null, "loan": null}
#           ↑ Account present      ↑ Both optional services down

# 7. Try to stop account service
docker compose stop account
sleep 10

# 8. Fetch details - complete failure (account is critical)
curl http://localhost:8000/api/customer/details/5555555555
# Response: 503 Service Unavailable
#           ↑ Can't degrade without account data

# 9. Recover services
docker compose up -d
sleep 10

# 10. Everything works again
curl http://localhost:8000/api/customer/details/5555555555
# Response: {"account": {...}, "card": {...}, "loan": {...}}
```

---

## Testing Resilience

### Recommended Test Plan

1. **Healthy State**
   - All services running
   - All endpoints succeed
   - All circuit breakers CLOSED
   - Metrics show 0 failures

2. **Graceful Degradation**
   - Stop non-critical service (card or loan)
   - Verify reads succeed with partial data
   - Verify writes blocked by Write Gate

3. **Cascading Failure**
   - Stop multiple services
   - Verify writes always blocked
   - Verify reads degrade to account only

4. **Recovery**
   - Restart services
   - Verify circuit breaker transitions (OPEN → HALF_OPEN → CLOSED)
   - Verify full functionality returns

### Full Test Script

```bash
#!/bin/bash

echo "=== Test 1: Healthy State ==="
curl -s http://localhost:8000/api/customer/details/5555555555 | jq .

echo -e "\n=== Test 2: Stop Card Service ==="
docker compose stop card
sleep 10
echo "Fetching details (should have card=null)..."
curl -s http://localhost:8000/api/customer/details/5555555555 | jq .

echo -e "\n=== Test 3: Try Write (should fail) ==="
curl -s -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","mobileNumber":"1111111111"}' | jq .

echo -e "\n=== Test 4: Recover Card ==="
docker compose start card
sleep 10
curl -s http://localhost:8000/api/customer/details/5555555555 | jq .

echo -e "\n=== Test Complete ==="
```

---

## Monitoring Resilience

### Check Circuit Breaker Metrics

```bash
# All circuit breaker metrics
curl http://localhost:9090/api/v1/query?query=resilience4j_circuitbreaker | jq

# Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
curl 'http://localhost:9090/api/v1/query?query=resilience4j_circuitbreaker_state{name="account-service"}' | jq
```

### Grafana Dashboard

Open http://localhost:3000 and navigate to "Circuit Breakers" dashboard to see:
- Circuit breaker state for each service
- Failure rate trends
- Number of buffered calls
- Call latency by outcome

### Common Metrics

| Metric | Meaning |
|--------|---------|
| `resilience4j_circuitbreaker_state` | Current state (0,1,2) |
| `resilience4j_circuitbreaker_failure_rate` | Percentage failures |
| `resilience4j_circuitbreaker_calls_total` | Total calls since startup |
| `resilience4j_circuitbreaker_buffered_calls` | Calls queued (HALF_OPEN) |
| `resilience4j_circuitbreaker_not_permitted_calls_total` | Calls rejected (OPEN) |

---

## Key Takeaways

✅ **Circuit Breaker**: Detects failures and fails fast instead of hanging

✅ **Write Gate**: Prevents partial writes when services are degraded

✅ **Graceful Degradation**: Returns partial data instead of complete failure

✅ **Cascading Failure Prevention**: One service down doesn't break the entire system

✅ **Fast Recovery**: HALF_OPEN state allows quick recovery testing

---

## More Information

- **Getting Started**: [GETTING_STARTED.md](GETTING_STARTED.md)
- **Architecture**: [ARCHITECTURE.md](ARCHITECTURE.md)
- **API Guide**: [API_GUIDE.md](API_GUIDE.md)
- **Troubleshooting**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- **Observability**: [observability.md](observability.md)
