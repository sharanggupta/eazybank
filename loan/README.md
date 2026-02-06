# Loan Service

Manages loan applications, tracking, and closures.

## Overview

The Loan Service handles:
- Loan application and creation
- Loan details tracking (amount, type, payment status)
- Loan information retrieval
- Loan closure

**Resilience**: This service supports graceful degradation. If the loan service fails:
- Loan data is omitted from customer profiles (not marked as error)
- Reads continue to work without loan information
- Writes to account/card services continue

---

## API Endpoints

### Create Loan

```http
POST /loan/api
Content-Type: application/json

{
  "mobileNumber": "1234567890",
  "loanType": "Home Loan",
  "totalLoan": 500000
}
```

**Response (201 Created)**:
```json
{
  "loanNumber": "LN-001-234567",
  "loanType": "Home Loan",
  "totalLoan": 500000,
  "amountPaid": 0,
  "outstandingAmount": 500000
}
```

### Fetch Loan

```http
GET /loan/api/{mobileNumber}
```

Example: `GET /loan/api/1234567890`

**Response (200 OK)**:
```json
{
  "loanNumber": "123456789012",
  "loanType": "Home Loan",
  "totalLoan": 500000,
  "amountPaid": 50000,
  "outstandingAmount": 450000
}
```

**Response (404 Not Found)**: If no loan exists for customer

### Update Loan

```http
PUT /loan/api
Content-Type: application/json

{
  "mobileNumber": "1234567890",
  "loanNumber": "123456789012",
  "loanType": "Home Loan",
  "totalLoan": 500000,
  "amountPaid": 100000
}
```

**Response (200 OK)**:
```json
{
  "statusCode": "200",
  "statusMessage": "Loan updated successfully"
}
```

### Delete Loan

```http
DELETE /loan/api/{mobileNumber}
```

Example: `DELETE /loan/api/1234567890`

**Response (200 OK)**:
```json
{
  "statusCode": "200",
  "statusMessage": "Loan deleted successfully"
}
```

---

## Models

### LoanRequest

```java
public record LoanRequest(
    String mobileNumber,
    String loanType,
    int totalLoan,
    int amountPaid
) {}
```

### LoanResponse

```java
public record LoanResponse(
    String loanNumber,
    String loanType,
    int totalLoan,
    int amountPaid,
    int outstandingAmount
) {}
```

### LoanInfoDto

Used by gateway for aggregation:

```java
public record LoanInfoDto(
    String loanNumber,
    String loanType,
    int totalLoan,
    int amountPaid,
    int outstandingAmount
) {}
```

---

## Configuration

### Database

```
Service: loan
Database: loandb
Port: 5432
Host: localhost (dev) or postgres (Docker)
```

### Application Settings

**Port**: 8090
**Context Path**: `/loan`
**Health Check**: `GET /loan/actuator/health`

### Environment Variables

```bash
SPRING_PROFILES_ACTIVE=dev           # Development mode
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/loandb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SERVER_PORT=8090
```

---

## Running Locally

### Start Database Only

```bash
docker compose -f deploy/dev/docker-compose.yml up -d postgres
```

### Run Service

```bash
cd loan
./mvnw spring-boot:run
```

Service available at: `http://localhost:8090/loan`

---

## Circuit Breaker Protection

All loan operations are protected by a Resilience4J circuit breaker with fallback:

```java
@Service
public class LoanServiceImpl implements LoanService {
    private static final String CIRCUIT_BREAKER = "loan-service";

    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "getLoanFallback")
    public Optional<LoanInfoDto> getLoan(String mobileNumber) { ... }

    private Optional<LoanInfoDto> getLoanFallback(String mobileNumber, Exception cause) {
        return Optional.empty();  // Return empty on failure
    }
}
```

**Dev Configuration** (trip after 1 failure):
```
slidingWindowSize: 2
minimumNumberOfCalls: 1
failureRateThreshold: 100%
waitDurationInOpenState: 5s
```

**Behavior When Circuit Breaker Opens**:
- Read requests return empty loan (graceful degradation)
- Write requests are blocked by gateway (prevent partial writes)
- Circuit closes after successful read once service recovers

---

## Testing

### Health Check

```bash
curl http://localhost:8090/loan/actuator/health
```

### Quick Test

```bash
# Create loan
curl -X POST http://localhost:8090/loan/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "loanType": "Home Loan", "totalLoan": 500000}'

# Fetch loan
curl http://localhost:8090/loan/api/1234567890

# Update loan (payment progress)
curl -X PUT http://localhost:8090/loan/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "loanNumber": "123456789012", "loanType": "Home Loan", "totalLoan": 500000, "amountPaid": 50000}'

# Delete loan
curl -X DELETE http://localhost:8090/loan/api/1234567890
```

### Run Unit Tests

```bash
cd loan
./mvnw test
```

---

## Graceful Degradation Example

When loan service fails:

```bash
# GET /api/customer/details?mobileNumber=1234567890 on customer-gateway with loan service down
{
  "mobileNumber": "1234567890",
  "account": { ... },
  "card": { ... },
  "loan": null         // ← Gracefully omitted
}
```

Customer profile is still returned with account and card data intact.

---

## Failure Scenarios

### Loan Service Down

**During healthy state → service fails:**
1. Gateway gets timeout/connection error
2. Circuit breaker opens after 1 failure (dev config)
3. Next read request gets `Optional.empty()`
4. Loan field omitted from customer profile
5. Customer profile still returned (200 OK)

**Write attempt while circuit breaker open:**
1. Gateway's WriteGateInterceptor catches open circuit breaker
2. Returns 503 Service Unavailable immediately
3. Prevents partial writes to account/card

**Service recovers:**
1. Wait 5 seconds (`waitDurationInOpenState`)
2. Circuit enters HALF_OPEN state
3. Make a read request to test service
4. If successful, circuit closes to CLOSED
5. Normal operation resumes

---

## Loan Types

Common loan types (extensible):
- `Home Loan`
- `Personal Loan`
- `Auto Loan`
- `Education Loan`

---

## Limitations & Notes

- **One Loan Per Customer**: Service assumes max one loan per mobile number
- **Amount Validation**: `amountPaid` must be ≤ `totalLoan`
- **Lazy Initialization**: Loan is created on-demand, not with account
- **No Adjustments**: `totalLoan` is fixed after creation (can only pay down)
- **Independent Service**: Loan service doesn't know about accounts/cards
- **Outstanding Amount**: Calculated as `totalLoan - amountPaid`

---

See [../docs/configuration-reference.md](../docs/configuration-reference.md) for full configuration options.
See [../deploy/dev/resilience-testing.md](../deploy/dev/resilience-testing.md) to test graceful degradation.
