# Card Service

Manages credit card issuance, updates, and cancellations.

## Overview

The Card Service handles:
- Card issuance for customers
- Card limit and usage tracking
- Card information retrieval
- Card cancellation

**Resilience**: This service supports graceful degradation. If the card service fails:
- Card data is omitted from customer profiles (not marked as error)
- Reads continue to work without card information
- Writes to account/loan services continue

---

## API Endpoints

### Create Card

```http
POST /card/api
Content-Type: application/json

{
  "mobileNumber": "1234567890",
  "cardType": "Credit Card",
  "totalLimit": 100000
}
```

**Response (201 Created)**:
```json
{
  "cardNumber": "1234-5678-9012-3456",
  "cardType": "Credit Card",
  "totalLimit": 100000,
  "amountUsed": 0,
  "availableAmount": 100000
}
```

### Fetch Card

```http
GET /card/api?mobileNumber=1234567890
```

**Response (200 OK)**:
```json
{
  "cardNumber": "1234-5678-9012-3456",
  "cardType": "Credit Card",
  "totalLimit": 100000,
  "amountUsed": 5000,
  "availableAmount": 95000
}
```

**Response (404 Not Found)**: If no card exists for customer

### Update Card

```http
PUT /card/api
Content-Type: application/json

{
  "mobileNumber": "1234567890",
  "cardType": "Credit Card",
  "totalLimit": 200000,
  "amountUsed": 5000
}
```

**Response (204 No Content)**

### Delete Card

```http
DELETE /card/api?mobileNumber=1234567890
```

**Response (204 No Content)**

---

## Models

### CardRequest

```java
public record CardRequest(
    String mobileNumber,
    String cardNumber,      // null on creation
    String cardType,
    int totalLimit,
    int amountUsed
) {}
```

### CardResponse

```java
public record CardResponse(
    String cardNumber,
    String cardType,
    int totalLimit,
    int amountUsed,
    int availableAmount
) {}
```

### CardInfoDto

Used by gateway for aggregation:

```java
public record CardInfoDto(
    String cardNumber,
    String cardType,
    int totalLimit,
    int amountUsed,
    int availableAmount
) {}
```

---

## Configuration

### Database

```
Service: card
Database: carddb
Port: 5432
Host: localhost (dev) or postgres (Docker)
```

### Application Settings

**Port**: 9000
**Context Path**: `/card`
**Health Check**: `GET /card/actuator/health`

### Environment Variables

```bash
SPRING_PROFILES_ACTIVE=dev           # Development mode
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/carddb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SERVER_PORT=9000
```

---

## Running Locally

### Start Database Only

```bash
docker compose -f deploy/dev/docker-compose.yml up -d postgres
```

### Run Service

```bash
cd card
./mvnw spring-boot:run
```

Service available at: `http://localhost:9000/card`

---

## Circuit Breaker Protection

All card operations are protected by a Resilience4J circuit breaker with fallback:

```java
@Service
public class CardServiceImpl implements CardService {
    private static final String CIRCUIT_BREAKER = "card-service";

    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "getCardFallback")
    public Optional<CardInfoDto> getCard(String mobileNumber) { ... }

    private Optional<CardInfoDto> getCardFallback(String mobileNumber, Exception cause) {
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
- Read requests return empty card (graceful degradation)
- Write requests are blocked by gateway (prevent partial writes)
- Circuit closes after successful read once service recovers

---

## Testing

### Health Check

```bash
curl http://localhost:9000/card/actuator/health
```

### Quick Test

```bash
# Create card
curl -X POST http://localhost:9000/card/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "cardType": "Credit Card", "totalLimit": 100000}'

# Fetch card
curl http://localhost:9000/card/api?mobileNumber=1234567890

# Update card
curl -X PUT http://localhost:9000/card/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "cardType": "Credit Card", "totalLimit": 200000, "amountUsed": 5000}'

# Delete card
curl -X DELETE http://localhost:9000/card/api?mobileNumber=1234567890
```

### Run Unit Tests

```bash
cd card
./mvnw test
```

---

## Graceful Degradation Example

When card service fails:

```bash
# GET /api/customer/1234567890 on customergateway with card service down
{
  "mobileNumber": "1234567890",
  "account": { ... },
  "card": null,           // ← Gracefully omitted
  "loan": { ... }
}
```

Customer profile is still returned with account and loan data intact.

---

## Failure Scenarios

### Card Service Down

**During healthy state → service fails:**
1. Gateway gets timeout/connection error
2. Circuit breaker opens after 1 failure (dev config)
3. Next read request gets `Optional.empty()`
4. Card field omitted from customer profile
5. Customer profile still returned (200 OK)

**Write attempt while circuit breaker open:**
1. Gateway's WriteGateInterceptor catches open circuit breaker
2. Returns 503 Service Unavailable immediately
3. Prevents partial writes to account/loan

**Service recovers:**
1. Wait 5 seconds (`waitDurationInOpenState`)
2. Circuit enters HALF_OPEN state
3. Make a read request to test service
4. If successful, circuit closes to CLOSED
5. Normal operation resumes

---

## Limitations & Notes

- **One Card Per Customer**: Service assumes max one card per mobile number
- **Amount Validation**: `amountUsed` must be ≤ `totalLimit`
- **Lazy Initialization**: Card is created on-demand, not with account
- **No Refunds**: `amountUsed` only increases, doesn't decrease
- **Independent Service**: Card service doesn't know about accounts/loans

---

See [../docs/configuration-reference.md](../docs/configuration-reference.md) for full configuration options.
See [../deploy/dev/resilience-testing.md](../deploy/dev/resilience-testing.md) to test graceful degradation.
Updated card documentation
Updated card service documentation - test run
