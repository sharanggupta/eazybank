# API Examples & Testing

Complete reference for all API endpoints in EazyBank.

## Gateway API (Aggregated)

The primary interface for client applications. Aggregates data from downstream services.

### Customer Lifecycle

```bash
# Onboard new customer
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'
# Expected: 201 Created
# Response: {"statusCode":"201","statusMessage":"Customer onboarded successfully"}

# Get customer details (aggregated account, card, loan)
curl http://localhost:8000/api/customer/details/1234567890
# Expected: 200 OK
# Response: JSON with name, email, mobileNumber, account, card, loan details

# Update customer profile
curl -X PUT http://localhost:8000/api/customer/update \
  -H "Content-Type: application/json" \
  -d '{"name": "John Updated", "email": "john.updated@example.com", "mobileNumber": "1234567890", "account": {"accountNumber": "00010012345678901", "accountType": "Savings", "branchAddress": "456 New Address"}}'
# Expected: 200 OK

# Offboard customer (removes all data)
curl -X DELETE http://localhost:8000/api/customer/offboard/1234567890
# Expected: 200 OK
```

---

## Card & Loan as Nested Resources

Cards and loans are accessed as nested sub-resources of a customer via the gateway.
All requests go through gateway port 8000.

### Card Operations

```bash
# Create card for customer
curl -X POST http://localhost:8000/api/customer/1234567890/card \
  -H "Content-Type: application/json" \
  -d '{"cardType": "Credit Card", "totalLimit": 100000}'
# Expected: 201 Created

# Fetch card
curl http://localhost:8000/api/customer/1234567890/card
# Expected: 200 OK with card details

# Update card
curl -X PUT http://localhost:8000/api/customer/1234567890/card \
  -H "Content-Type: application/json" \
  -d '{"cardNumber": "1234567890123456", "cardType": "Credit Card", "totalLimit": 200000, "amountUsed": 5000}'
# Expected: 204 No Content

# Delete card
curl -X DELETE http://localhost:8000/api/customer/1234567890/card
# Expected: 204 No Content
```

### Loan Operations

```bash
# Create loan for customer
curl -X POST http://localhost:8000/api/customer/1234567890/loan \
  -H "Content-Type: application/json" \
  -d '{"loanType": "Home Loan", "totalLoan": 500000}'
# Expected: 201 Created

# Fetch loan
curl http://localhost:8000/api/customer/1234567890/loan
# Expected: 200 OK with loan details

# Update loan
curl -X PUT http://localhost:8000/api/customer/1234567890/loan \
  -H "Content-Type: application/json" \
  -d '{"loanNumber": "123456789012", "loanType": "Home Loan", "totalLoan": 500000, "amountPaid": 50000}'
# Expected: 204 No Content

# Delete loan
curl -X DELETE http://localhost:8000/api/customer/1234567890/loan
# Expected: 204 No Content
```

### Account Operations (via gateway proxy)

```bash
# Fetch account
curl http://localhost:8000/account/api/1234567890

# Update account
curl -X PUT http://localhost:8000/account/api \
  -H "Content-Type: application/json" \
  -d '{"name": "John Updated", "email": "john@example.com", "mobileNumber": "1234567890", "account": {"accountType": "Savings", "branchAddress": "456 New Address"}}'

# Delete account
curl -X DELETE http://localhost:8000/account/api/1234567890
```

---

## Direct Service APIs (for debugging)

For debugging, you can bypass the gateway and call services directly.

### Account Service (port 8080)

```bash
# Create account
curl -X POST http://localhost:8080/account/api \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'

# Fetch account by mobile number
curl http://localhost:8080/account/api/1234567890

# Update account details
curl -X PUT http://localhost:8080/account/api \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Updated",
    "email": "john@example.com",
    "mobileNumber": "1234567890",
    "account": {
      "accountNumber": "00010012345678901",
      "accountType": "Savings",
      "branchAddress": "123 Main Street, New York"
    }
  }'

# Delete account
curl -X DELETE http://localhost:8080/account/api/1234567890
```

### Card Service (port 9000)

```bash
# Create card
curl -X POST http://localhost:9000/card/api/1234567890 \
  -H "Content-Type: application/json" \
  -d '{"cardType": "Credit Card", "totalLimit": 100000}'

# Fetch card by mobile number
curl http://localhost:9000/card/api/1234567890

# Update card
curl -X PUT http://localhost:9000/card/api/1234567890 \
  -H "Content-Type: application/json" \
  -d '{"cardNumber": "1234567890123456", "cardType": "Credit Card", "totalLimit": 200000, "amountUsed": 5000}'

# Delete card
curl -X DELETE http://localhost:9000/card/api/1234567890
```

### Loan Service (port 8090)

```bash
# Create loan
curl -X POST http://localhost:8090/loan/api/1234567890 \
  -H "Content-Type: application/json" \
  -d '{"loanType": "Home Loan", "totalLoan": 500000}'

# Fetch loan by mobile number
curl http://localhost:8090/loan/api/1234567890

# Update loan
curl -X PUT http://localhost:8090/loan/api/1234567890 \
  -H "Content-Type: application/json" \
  -d '{"loanNumber": "123456789012", "loanType": "Home Loan", "totalLoan": 500000, "amountPaid": 50000}'

# Delete loan
curl -X DELETE http://localhost:8090/loan/api/1234567890
```

---

## Interactive API Documentation

Visit Swagger UI for live API exploration:

| Service | URL |
|---------|-----|
| **Gateway** | http://localhost:8000/swagger-ui.html |
| **Account** | http://localhost:8080/account/swagger-ui.html |
| **Card** | http://localhost:9000/card/swagger-ui.html |
| **Loan** | http://localhost:8090/loan/swagger-ui.html |

---

## Error Responses

All services return consistent error responses:

### Gateway Error

```json
{
  "apiPath": "/api/customer/onboard",
  "errorCode": "400 BAD_REQUEST",
  "errorMessage": "Customer already registered with mobile number 1234567890",
  "errorTimestamp": "2026-01-31T13:00:00Z"
}
```

### Common Status Codes

| Code | Meaning |
|------|---------|
| 201 | Created successfully |
| 200 | Updated/deleted successfully |
| 400 | Bad request (validation failed or duplicate) |
| 404 | Customer/resource not found |
| 503 | Service unavailable (circuit breaker open) |

---

## Testing Workflow

1. **Create a customer**: POST /api/customer/onboard
2. **Get full profile**: GET /api/customer/details/{mobileNumber}
3. **Update information**: PUT /api/customer/update
4. **Clean up**: DELETE /api/customer/offboard/{mobileNumber}

Refer to [resilience-testing.md](resilience-testing.md) for advanced testing scenarios involving service failures and circuit breakers.
