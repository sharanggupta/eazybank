# API Examples & Testing

Complete reference for all API endpoints in EazyBank.

## Gateway API (Aggregated)

The primary interface for client applications. Aggregates data from downstream services.

### Customer Lifecycle

```bash
# Onboard new customer
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'
# Expected: 201 Created
# Response: {"statusCode":"201","statusMessage":"Customer onboarded successfully"}

# Get customer details (aggregated account, card, loan)
curl http://localhost:8000/api/customer/1234567890
# Expected: 200 OK
# Response: JSON with mobileNumber, account, card, loan details

# Update customer profile
curl -X PUT http://localhost:8000/api/customer/1234567890 \
  -H "Content-Type: application/json" \
  -d '{"name": "John Updated", "email": "john.updated@example.com"}'
# Expected: 204 No Content

# Offboard customer (removes all data)
curl -X DELETE http://localhost:8000/api/customer/1234567890
# Expected: 204 No Content
```

### Card Management

```bash
# Issue a new card
curl -X POST http://localhost:8000/api/customer/1234567890/card \
  -H "Content-Type: application/json" \
  -d '{"cardType": "Credit Card", "totalLimit": 100000}'
# Expected: 201 Created
# Response: {"statusCode":"201","statusMessage":"Card issued successfully"}

# Get card details
curl http://localhost:8000/api/customer/1234567890/card
# Expected: 200 OK
# Response: {cardNumber, cardType, totalLimit, amountUsed, availableAmount}

# Update card limit and usage
curl -X PUT http://localhost:8000/api/customer/1234567890/card \
  -H "Content-Type: application/json" \
  -d '{"cardType": "Credit Card", "totalLimit": 200000, "amountUsed": 5000}'
# Expected: 204 No Content

# Cancel card
curl -X DELETE http://localhost:8000/api/customer/1234567890/card
# Expected: 204 No Content
```

### Loan Management

```bash
# Apply for loan
curl -X POST http://localhost:8000/api/customer/1234567890/loan \
  -H "Content-Type: application/json" \
  -d '{"loanType": "Home Loan", "totalLoan": 500000}'
# Expected: 201 Created
# Response: {"statusCode":"201","statusMessage":"Loan created successfully"}

# Get loan details
curl http://localhost:8000/api/customer/1234567890/loan
# Expected: 200 OK
# Response: {loanNumber, loanType, totalLoan, amountPaid, outstandingAmount}

# Update loan payment progress
curl -X PUT http://localhost:8000/api/customer/1234567890/loan \
  -H "Content-Type: application/json" \
  -d '{"loanType": "Home Loan", "totalLoan": 500000, "amountPaid": 50000}'
# Expected: 204 No Content

# Close loan
curl -X DELETE http://localhost:8000/api/customer/1234567890/loan
# Expected: 204 No Content
```

---

## Direct Downstream APIs

For debugging individual services and bypassing the gateway.

### Account Service (port 8080)

```bash
# Create account
curl -X POST http://localhost:8080/account/api/create \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'

# Fetch account by mobile number
curl http://localhost:8080/account/api/fetch?mobileNumber=1234567890

# Update account details
curl -X PUT http://localhost:8080/account/api/update \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Updated",
    "email": "john@example.com",
    "mobileNumber": "1234567890",
    "accountDto": {
      "accountNumber": 1241297897,
      "accountType": "Savings",
      "branchAddress": "123 Main Street, New York"
    }
  }'

# Delete account
curl -X DELETE "http://localhost:8080/account/api/delete?mobileNumber=1234567890"
```

### Card Service (port 9000)

```bash
# Create card
curl -X POST http://localhost:9000/card/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "cardType": "Credit Card", "totalLimit": 100000}'

# Fetch card by mobile number
curl http://localhost:9000/card/api?mobileNumber=1234567890

# Update card
curl -X PUT http://localhost:9000/card/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "cardType": "Credit Card", "totalLimit": 200000, "amountUsed": 5000}'

# Delete card
curl -X DELETE "http://localhost:9000/card/api?mobileNumber=1234567890"
```

### Loan Service (port 8090)

```bash
# Create loan
curl -X POST http://localhost:8090/loan/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "loanType": "Home Loan", "totalLoan": 500000}'

# Fetch loan by mobile number
curl http://localhost:8090/loan/api?mobileNumber=1234567890

# Update loan
curl -X PUT http://localhost:8090/loan/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "loanType": "Home Loan", "totalLoan": 500000, "amountPaid": 50000}'

# Delete loan
curl -X DELETE "http://localhost:8090/loan/api?mobileNumber=1234567890"
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

Swagger UI allows you to:
- View all endpoints and their parameters
- Try requests directly from the browser
- See response schemas and examples
- Download OpenAPI specifications

---

## Error Responses

All services return consistent error responses:

### Gateway Error

```json
{
  "apiPath": "/api/customer",
  "errorCode": "404 NOT_FOUND",
  "errorMessage": "Customer not found",
  "errorTime": "2026-01-31T13:00:00Z"
}
```

### Common Status Codes

| Code | Meaning |
|------|---------|
| 201 | Created successfully |
| 204 | Updated/deleted successfully (no content) |
| 400 | Bad request (validation failed) |
| 404 | Customer/resource not found |
| 503 | Service unavailable (circuit breaker open) |

---

## Testing Workflow

1. **Create a customer**: POST /api/customer
2. **Add a card**: POST /api/customer/{id}/card
3. **Apply for a loan**: POST /api/customer/{id}/loan
4. **Get full profile**: GET /api/customer/{id}
5. **Update information**: PUT endpoints
6. **Clean up**: DELETE endpoints

Refer to [resilience-testing.md](resilience-testing.md) for advanced testing scenarios involving service failures and circuit breakers.
