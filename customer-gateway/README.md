# Customer Gateway

API Gateway and orchestration layer for EazyBank microservices.

## Overview

The Customer Gateway is the single entry point for all client traffic. It provides:

- **API Aggregation**: Combines data from Account, Card, and Loan services into unified responses
- **Request Routing**: Proxies requests to downstream services
- **Resilience**: Circuit breaker protection with graceful degradation
- **Write Protection**: Blocks write operations when any downstream service is degraded

This is the only externally accessible service in production (via NodePort). Backend services use ClusterIP.

---

## Architecture

```
                    +-------------------+
    Clients ------> |  Customer Gateway |
                    |     :8000         |
                    +--------+----------+
                             |
            +----------------+----------------+
            |                |                |
            v                v                v
     +----------+     +----------+     +----------+
     | Account  |     |   Card   |     |   Loan   |
     |  :8080   |     |  :9000   |     |  :8090   |
     +----------+     +----------+     +----------+
```

### Write Gate Pattern

The gateway implements a **Write Gate** that prevents partial data corruption:

1. **Before any write operation**: Check if ANY circuit breaker is OPEN or HALF_OPEN
2. **If degraded**: Reject with `503 Service Unavailable`
3. **If healthy**: Allow operation to proceed

This ensures that customer onboarding either succeeds completely (account + card + loan) or fails cleanly.

---

## API Endpoints

### Customer Lifecycle

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/customer/onboard` | Create new customer with account |
| `GET` | `/api/customer/details?mobileNumber={mobile}` | Get aggregated customer profile |
| `PUT` | `/api/customer/update` | Update customer information |
| `DELETE` | `/api/customer/offboard?mobileNumber={mobile}` | Remove customer and all data |

### Onboard Customer

```http
POST /api/customer/onboard
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "mobileNumber": "1234567890"
}
```

**Response (201 Created)**:
```json
{
  "statusCode": "201",
  "statusMessage": "Customer onboarded successfully"
}
```

### Get Customer Details

```http
GET /api/customer/details?mobileNumber=1234567890
```

**Response (200 OK)**:
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "mobileNumber": "1234567890",
  "account": {
    "accountNumber": "00010012345678901",
    "accountType": "Savings",
    "branchAddress": "123 Main Street, New York"
  },
  "card": {
    "cardNumber": "1234567890123456",
    "cardType": "Credit Card",
    "totalLimit": 100000,
    "amountUsed": 5000,
    "availableAmount": 95000
  },
  "loan": {
    "loanNumber": "123456789012",
    "loanType": "Home Loan",
    "totalLoan": 500000,
    "amountPaid": 50000,
    "outstandingAmount": 450000
  }
}
```

**Graceful Degradation**: If Card or Loan service is down, those fields are omitted (not null, completely absent from response).

### Update Customer

```http
PUT /api/customer/update
Content-Type: application/json

{
  "name": "John Updated",
  "email": "john.updated@example.com",
  "mobileNumber": "1234567890",
  "account": {
    "accountNumber": "00010012345678901",
    "accountType": "Savings",
    "branchAddress": "456 New Address"
  }
}
```

**Response (200 OK)**:
```json
{
  "statusCode": "200",
  "statusMessage": "Customer updated successfully"
}
```

### Offboard Customer

```http
DELETE /api/customer/offboard?mobileNumber=1234567890
```

**Response (200 OK)**:
```json
{
  "statusCode": "200",
  "statusMessage": "Customer offboarded successfully"
}
```

---

## Proxy Routes

The gateway also proxies requests directly to downstream services:

| Route Pattern | Target Service |
|---------------|----------------|
| `/account/**` | Account Service (:8080) |
| `/card/**` | Card Service (:9000) |
| `/loan/**` | Loan Service (:8090) |

Example:
```bash
# These are equivalent:
curl http://localhost:8000/account/api/1234567890
curl http://localhost:8080/account/api/1234567890  # Direct (local dev only)
```

---

## Resilience Architecture

### Circuit Breaker Configuration

Each downstream service has its own circuit breaker:

| Circuit Breaker | Service | Fallback Behavior |
|-----------------|---------|-------------------|
| `account_service` | Account | **No fallback** - critical service |
| `card_service` | Card | Returns empty - graceful degradation |
| `loan_service` | Loan | Returns empty - graceful degradation |

### Circuit Breaker States

```
CLOSED (Healthy)
  | (failure threshold exceeded)
  v
OPEN (Broken - writes blocked)
  | (after wait-duration)
  v
HALF_OPEN (Testing - writes still blocked)
  | (successful request)
  v
CLOSED (Recovered)
```

### Dev vs Production Thresholds

| Setting | Dev | Prod | Purpose |
|---------|-----|------|---------|
| `slidingWindowSize` | 2 | 10 | Sample size for evaluation |
| `minimumNumberOfCalls` | 1 | 5 | Min calls before tripping |
| `failureRateThreshold` | 100% | 50% | Failure rate to trip |
| `waitDurationInOpenState` | 5s | 30s | Recovery wait time |

### Write Gate Implementation

Write operations are protected by the `@ProtectedWrite` annotation:

```java
@ProtectedWrite
public Mono<ResponseDto> onboardCustomer(CustomerAccount request) {
    // This method is blocked if ANY circuit breaker is OPEN/HALF_OPEN
}
```

**Implementation files**:
- `annotation/ProtectedWrite.java` - Annotation marker
- `aspect/WriteGateAspect.java` - AOP interceptor
- `service/WriteGateImpl.java` - Circuit breaker state check

---

## Configuration

### Service URLs

```yaml
services:
  account-url: http://localhost:8080   # Account service
  card-url: http://localhost:9000      # Card service
  loan-url: http://localhost:8090      # Loan service
```

### Environment Variables

```bash
SERVICES_ACCOUNT_URL=http://account:8080
SERVICES_CARD_URL=http://card:9000
SERVICES_LOAN_URL=http://loan:8090
SPRING_PROFILES_ACTIVE=dev
```

### Spring Profiles

- **default**: Production-like thresholds
- **dev**: Aggressive thresholds for quick feedback (configured in `application-dev.yaml`)

---

## Running Locally

### With Docker Compose

```bash
cd deploy/dev
./build-images.sh
docker compose up -d
```

Gateway available at: `http://localhost:8000`

### Standalone (Hot Reload)

```bash
# Start database and downstream services first
cd deploy/dev && docker compose up -d postgres account card loan

# Run gateway with hot reload
cd customer-gateway
./mvnw spring-boot:run
```

---

## Testing

### Health Check

```bash
curl http://localhost:8000/actuator/health
```

### Circuit Breaker Status

```bash
curl http://localhost:8000/actuator/circuitbreakers
```

### Quick API Test

```bash
# Onboard customer
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name": "Test User", "email": "test@example.com", "mobileNumber": "1234567890"}'

# Get customer details
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"

# Offboard customer
curl -X DELETE "http://localhost:8000/api/customer/offboard?mobileNumber=1234567890"
```

### Run Unit Tests

```bash
cd customer-gateway
./mvnw test
```

---

## Error Responses

All errors follow a consistent format:

```json
{
  "apiPath": "/api/customer/onboard",
  "errorCode": "503 SERVICE_UNAVAILABLE",
  "errorMessage": "Write operations are blocked because the card_service circuit breaker is open.",
  "errorTimestamp": "2026-01-31T13:00:00Z"
}
```

### Common Status Codes

| Code | Meaning |
|------|---------|
| 201 | Customer created successfully |
| 200 | Operation successful |
| 400 | Validation error or duplicate customer |
| 404 | Customer not found |
| 503 | Service unavailable (circuit breaker open) |

---

## See Also

- [API Examples](../deploy/dev/api-examples.md) - Complete API reference
- [Resilience Testing](../deploy/dev/resilience-testing.md) - Circuit breaker testing guide
- [Configuration Reference](../docs/configuration-reference.md) - All configuration options
- [Account Service](../account/README.md) - Account API details
- [Card Service](../card/README.md) - Card API details
- [Loan Service](../loan/README.md) - Loan API details
