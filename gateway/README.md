# API Gateway

Central entry point for EazyBank client applications.

## Overview

The API Gateway is the single point of entry for all client applications. It:
- **Aggregates** data from Account, Card, and Loan microservices
- **Protects** the system from cascading failures via circuit breakers
- **Orchestrates** customer lifecycle operations across multiple services
- **Blocks** write operations when downstream services degrade

This is the **Write Gate pattern** — preventing partial writes and data inconsistency.

---

## Responsibilities

### Data Aggregation

The gateway combines data from three independent microservices into unified customer profiles:

```json
GET /api/customer/1234567890
{
  "mobileNumber": "1234567890",
  "account": { ... },        // From account service
  "card": { ... },           // From card service (optional)
  "loan": { ... }            // From loan service (optional)
}
```

### Write Gate Protection

When any downstream service's circuit breaker is **OPEN**, the gateway blocks ALL write operations:

```
User tries: POST /api/customer (create new customer)
Gateway checks: Is any circuit breaker OPEN?
  → YES (card service is down)
Response: 503 Service Unavailable
  "System is temporarily unavailable for writes — card-service is degraded"
```

This prevents:
- Creating customers with missing card/loan setup
- Updating profiles without being able to update all related data
- Corrupting data with partial updates

### Graceful Read Degradation

When a downstream service fails, reads continue but omit that service's data:

```json
GET /api/customer/1234567890 (with card service down)
{
  "mobileNumber": "1234567890",
  "account": { ... },    // ✓ Present
  "card": null,          // ✗ Omitted
  "loan": { ... }        // ✓ Present
}
```

Users can see what they need, with degraded but not broken service.

---

## Architecture

### Circuit Breaker Pattern

Each microservice has a circuit breaker that monitors its health:

```
Account Service Circuit Breaker: CLOSED (healthy)
Card Service Circuit Breaker: OPEN (failing)
Loan Service Circuit Breaker: CLOSED (healthy)
```

**Circuit breaker states:**

```
CLOSED (healthy)
  ↓ (failures exceed threshold)
OPEN (broken)
  ↓ (waitDurationInOpenState passes)
HALF_OPEN (testing)
  ↓ (request succeeds)
CLOSED (recovered)
```

### Write Gate Interceptor

All HTTP requests pass through `WriteGateInterceptor` before reaching controllers:

```java
public class WriteGateInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        if (isWriteRequest(request)) {
            rejectIfAnyCircuitBreakerOpen(request);  // Check all circuit breakers
        }
        return true;
    }

    private boolean isWriteRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return !(GET.equals(method) || HEAD.equals(method));
    }
}
```

**Write methods blocked:**
- `POST /api/customer` (create)
- `PUT /api/customer/{id}` (update)
- `DELETE /api/customer/{id}` (delete)

**Read methods allowed:**
- `GET /api/customer/{id}`
- `GET /api/customer/{id}/card`
- `GET /api/customer/{id}/loan`

### Service Implementations with Fallbacks

Each service has fallback methods for graceful degradation:

```java
@Service
public class CardServiceImpl implements CardService {
    private static final String CIRCUIT_BREAKER = "card-service";

    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "getCardFallback")
    public Optional<CardInfoDto> getCard(String mobileNumber) {
        return cardClient.fetchCardByMobileNumber(mobileNumber);
    }

    private Optional<CardInfoDto> getCardFallback(String mobileNumber, Exception cause) {
        log.warn("Card service unavailable for {}", mobileNumber, cause);
        return Optional.empty();  // Graceful degradation
    }
}
```

---

## API Endpoints

### Customer Lifecycle

```
POST   /api/customer                    Create customer
GET    /api/customer/{mobileNumber}     Get customer profile
PUT    /api/customer/{mobileNumber}     Update profile
DELETE /api/customer/{mobileNumber}     Delete customer
```

### Card Management

```
POST   /api/customer/{id}/card          Issue card
GET    /api/customer/{id}/card          Get card details
PUT    /api/customer/{id}/card          Update card
DELETE /api/customer/{id}/card          Cancel card
```

### Loan Management

```
POST   /api/customer/{id}/loan          Apply for loan
GET    /api/customer/{id}/loan          Get loan details
PUT    /api/customer/{id}/loan          Update loan
DELETE /api/customer/{id}/loan          Close loan
```

See [../deploy/dev/api-examples.md](../deploy/dev/api-examples.md) for detailed examples.

---

## Configuration

### Downstream Services

```yaml
services:
  account-url: http://localhost:8080
  card-url: http://localhost:9000
  loan-url: http://localhost:8090
```

Set via environment:
```bash
SERVICES_ACCOUNT_URL=http://account:8080
SERVICES_CARD_URL=http://card:9000
SERVICES_LOAN_URL=http://loan:8090
```

### Circuit Breaker Configuration

**Dev config** (trip after 1 failure, fast recovery):
```yaml
slidingWindowSize: 2
minimumNumberOfCalls: 1
failureRateThreshold: 100%
waitDurationInOpenState: 5s
```

**Production config** (trip after 5 failures, slow recovery):
```yaml
slidingWindowSize: 100
minimumNumberOfCalls: 10
failureRateThreshold: 50%
waitDurationInOpenState: 60s
```

---

## Application Settings

**Port**: 8000
**Context Path**: `/`
**Health Check**: `GET /actuator/health`
**Swagger UI**: `http://localhost:8000/swagger-ui.html`

---

## Running Locally

### Start Downstream Services

```bash
docker compose -f deploy/dev/docker-compose.yml up postgres account card loan
```

### Run Gateway

```bash
cd gateway
./mvnw spring-boot:run
```

Gateway available at: `http://localhost:8000`

---

## Monitoring

### Health & Status

```bash
# Gateway health
curl http://localhost:8000/actuator/health

# All circuit breaker states
curl http://localhost:8000/actuator/circuitbreakers

# Specific circuit breaker
curl http://localhost:8000/actuator/circuitbreakers/card-service
```

### Logs

```bash
# Watch for circuit breaker events
docker compose logs -f gateway | grep -i "circuit\|rejecting"

# Example output:
# [WriteGateInterceptor] Rejecting POST /api/customer — circuit breaker 'card-service' is open
# [CardServiceImpl] Card service unavailable for 1234567890
```

---

## Testing

### Quick Test (All Services Healthy)

```bash
# Create customer
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "1234567890"}'

# Get customer (should have all data)
curl http://localhost:8000/api/customer/1234567890
```

### Test Graceful Degradation

```bash
# Stop card service
docker compose -f deploy/dev/docker-compose.yml stop card

# Get customer (card field missing)
curl http://localhost:8000/api/customer/1234567890

# Try write (blocked)
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "Another", "email": "another@example.com", "mobileNumber": "9999999999"}'
# Response: 503 Service Unavailable
```

### Run Unit Tests

```bash
cd gateway
./mvnw test -Dtest=WriteGateTest     # Test write gate blocking
./mvnw test -Dtest=CustomerJourneyTest  # Test full customer lifecycle
```

---

## Error Handling

### Validation Errors

```json
400 Bad Request
{
  "apiPath": "/api/customer",
  "errorCode": "400 BAD_REQUEST",
  "errorMessage": "name: size must be between 5 and 30",
  "errorTime": "2026-01-31T13:00:00Z"
}
```

### Not Found

```json
404 Not Found
{
  "apiPath": "/api/customer/9999999999",
  "errorCode": "404 NOT_FOUND",
  "errorMessage": "Customer not found",
  "errorTime": "2026-01-31T13:00:00Z"
}
```

### Service Unavailable

```json
503 Service Unavailable
{
  "apiPath": "/api/customer",
  "errorCode": "503 SERVICE_UNAVAILABLE",
  "errorMessage": "System is temporarily unavailable for writes — card-service is degraded",
  "errorTime": "2026-01-31T13:00:00Z"
}
```

---

## Implementation Details

### Key Files

```
src/main/java/dev/sharanggupta/gateway/
├── config/
│   ├── WriteGateInterceptor.java         # Request interceptor for write blocking
│   ├── WebMvcConfig.java                 # Register interceptor
│   └── RestClientConfig.java             # REST client configuration
├── controller/
│   ├── CustomerController.java           # Customer endpoints
│   ├── CardController.java               # Card endpoints
│   └── LoanController.java               # Loan endpoints
├── service/
│   ├── CustomerService.java              # Customer orchestration
│   ├── CardService.java                  # Card operations
│   ├── LoanService.java                  # Loan operations
│   └── impl/
│       ├── CustomerServiceImpl.java
│       ├── CardServiceImpl.java           # With @CircuitBreaker
│       ├── LoanServiceImpl.java           # With @CircuitBreaker
│       └── AccountServiceImpl.java        # With @CircuitBreaker
└── exception/
    ├── GlobalExceptionHandler.java       # Unified error responses
    └── ServiceUnavailableException.java  # When circuit breaker open
```

---

## Failure Modes

### Account Service Down

**Impact**: Everything fails (account is critical)
```
GET /api/customer/1234567890 → 503 Service Unavailable
POST /api/customer → 503 Service Unavailable
```

### Card Service Down

**Impact**: Write gate blocks, reads degrade
```
GET /api/customer/1234567890 → 200 OK (card omitted)
POST /api/customer → 503 Service Unavailable
```

### Loan Service Down

**Impact**: Write gate blocks, reads degrade
```
GET /api/customer/1234567890 → 200 OK (loan omitted)
POST /api/customer → 503 Service Unavailable
```

---

## Performance Considerations

- **Parallel Fetching**: Card and Loan fetches run in parallel via `CompletableFuture`
- **Fast Fail**: Circuit breaker returns immediately, no timeout waits
- **Caching**: Not implemented (could be added for read-heavy workloads)
- **Batching**: Not supported (each request is individual)

---

See [../deploy/dev/resilience-testing.md](../deploy/dev/resilience-testing.md) for advanced testing.
See [../docs/configuration-reference.md](../docs/configuration-reference.md) for full configuration options.
