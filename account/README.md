# Account Service

Manages customer account information and lifecycle.

## Overview

The Account Service is the foundational microservice for EazyBank. It manages:
- Customer account creation and deletion
- Customer profile information (name, email, mobile number)
- Account details (account number, type, branch address)
- Account persistence in PostgreSQL

This is a **critical service** — all other operations depend on it. No graceful degradation is possible if this service fails.

## API Endpoints

### Create Account

```http
POST /account/api/create
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
  "name": "John Doe",
  "email": "john@example.com",
  "accountNumber": 1234567890,
  "accountType": "Savings",
  "branchAddress": "123 Main Street"
}
```

### Fetch Account

```http
GET /account/api/fetch?mobileNumber=1234567890
```

**Response (200 OK)**:
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "accountNumber": 1234567890,
  "accountType": "Savings",
  "branchAddress": "123 Main Street"
}
```

### Update Account

```http
PUT /account/api/update
Content-Type: application/json

{
  "name": "John Updated",
  "email": "john.updated@example.com",
  "mobileNumber": "1234567890",
  "accountDto": {
    "accountNumber": 1234567890,
    "accountType": "Checking",
    "branchAddress": "456 New Street"
  }
}
```

**Response (204 No Content)**

### Delete Account

```http
DELETE /account/api/delete?mobileNumber=1234567890
```

**Response (204 No Content)**

---

## Models

### AccountRequest

```java
public record AccountRequest(
    String name,
    String email,
    String mobileNumber,
    AccountDto accountDto
) {}

public record AccountDto(
    Long accountNumber,
    String accountType,
    String branchAddress
) {}
```

### AccountResponse

```java
public record AccountResponse(
    String name,
    String email,
    AccountDto accountDto
) {}
```

### AccountInfoDto

Used by the gateway for aggregation:

```java
public class AccountInfoDto {
    String name;
    String email;
    Long accountNumber;
    String accountType;
    String branchAddress;
}
```

---

## Configuration

### Database

```
Service: account
Database: accountdb
Port: 5432
Host: localhost (dev) or postgres (Docker)
```

### Application Settings

**Port**: 8080
**Context Path**: `/account`
**Health Check**: `GET /account/actuator/health`

### Environment Variables

```bash
SPRING_PROFILES_ACTIVE=dev           # Development mode
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/accountdb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SERVER_PORT=8080
```

---

## Running Locally

### Start Database Only

```bash
docker compose -f deploy/dev/docker-compose.yml up -d postgres
```

### Run Service

```bash
cd account
./mvnw spring-boot:run
```

Service available at: `http://localhost:8080/account`

---

## Circuit Breaker Protection

All account operations are protected by a Resilience4J circuit breaker:

```java
@Service
public class AccountServiceImpl implements AccountService {
    private static final String CIRCUIT_BREAKER = "account-service";

    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "...")
    public void createAccount(...) { ... }
}
```

**Dev Configuration** (trip after 1 failure):
```
slidingWindowSize: 2
minimumNumberOfCalls: 1
failureRateThreshold: 100%
waitDurationInOpenState: 5s
```

---

## Testing

### Health Check

```bash
curl http://localhost:8080/account/actuator/health
```

### Quick Test

```bash
# Create account
curl -X POST http://localhost:8080/account/api/create \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "mobileNumber": "1234567890"}'

# Fetch account
curl http://localhost:8080/account/api/fetch?mobileNumber=1234567890

# Update account
curl -X PUT http://localhost:8080/account/api/update \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Updated", "email": "test@example.com", "mobileNumber": "1234567890", "accountDto": {"accountNumber": 12345, "accountType": "Savings", "branchAddress": "Main St"}}'

# Delete account
curl -X DELETE http://localhost:8080/account/api/delete?mobileNumber=1234567890
```

### Run Unit Tests

```bash
cd account
./mvnw test
```

---

## Limitations & Notes

- **Validation**: Name must be 5-30 characters, email must be valid format
- **Uniqueness**: Mobile number is the unique identifier
- **Deletion Cascades**: Deleting account doesn't automatically delete associated cards/loans (gateway handles this)
- **No Graceful Degradation**: This is a critical service; if it fails, entire system is unavailable

---

See [../docs/configuration-reference.md](../docs/configuration-reference.md) for full configuration options.
