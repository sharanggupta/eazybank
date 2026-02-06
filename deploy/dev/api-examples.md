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
curl "http://localhost:8000/api/customer/details?mobileNumber=1234567890"
# Expected: 200 OK
# Response: JSON with name, email, mobileNumber, account, card, loan details

# Update customer profile
curl -X PUT http://localhost:8000/api/customer/update \
  -H "Content-Type: application/json" \
  -d '{"name": "John Updated", "email": "john.updated@example.com", "mobileNumber": "1234567890", "account": {"accountNumber": "00010012345678901", "accountType": "Savings", "branchAddress": "456 New Address"}}'
# Expected: 200 OK

# Offboard customer (removes all data)
curl -X DELETE "http://localhost:8000/api/customer/offboard?mobileNumber=1234567890"
# Expected: 200 OK
```

---

## Direct Downstream APIs

For debugging individual services and bypassing the gateway.

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
curl -X POST http://localhost:9000/card/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "cardType": "Credit Card", "totalLimit": 100000}'

# Fetch card by mobile number
curl http://localhost:9000/card/api/1234567890

# Update card
curl -X PUT http://localhost:9000/card/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "cardNumber": "1234567890123456", "cardType": "Credit Card", "totalLimit": 200000, "amountUsed": 5000}'

# Delete card
curl -X DELETE http://localhost:9000/card/api/1234567890
```

### Loan Service (port 8090)

```bash
# Create loan
curl -X POST http://localhost:8090/loan/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "loanType": "Home Loan", "totalLoan": 500000}'

# Fetch loan by mobile number
curl http://localhost:8090/loan/api/1234567890

# Update loan
curl -X PUT http://localhost:8090/loan/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "loanNumber": "123456789012", "loanType": "Home Loan", "totalLoan": 500000, "amountPaid": 50000}'

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
2. **Get full profile**: GET /api/customer/details?mobileNumber={id}
3. **Update information**: PUT /api/customer/update
4. **Clean up**: DELETE /api/customer/offboard?mobileNumber={id}

Refer to [resilience-testing.md](resilience-testing.md) for advanced testing scenarios involving service failures and circuit breakers.
