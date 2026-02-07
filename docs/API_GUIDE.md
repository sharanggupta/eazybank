# API Guide

Complete reference for all EazyBank API endpoints.

---

## Overview

EazyBank exposes APIs through the **Customer Gateway** (port 8000), which aggregates data from three backend services:
- **Account Service** (internal, port 8080)
- **Card Service** (internal, port 9000)
- **Loan Service** (internal, port 8090)

**All client requests go through the gateway on port 8000.**

---

## Quick Reference

| Operation | Endpoint | Method | Port |
|-----------|----------|--------|------|
| Onboard customer | `/api/customer/onboard` | POST | 8000 |
| Get customer | `/api/customer/details/{mobileNumber}` | GET | 8000 |
| Update customer | `/api/customer/update` | PUT | 8000 |
| Delete customer | `/api/customer/offboard/{mobileNumber}` | DELETE | 8000 |
| Create card | `/api/customer/{mobileNumber}/card` | POST | 8000 |
| Get card | `/api/customer/{mobileNumber}/card` | GET | 8000 |
| Update card | `/api/customer/{mobileNumber}/card` | PUT | 8000 |
| Delete card | `/api/customer/{mobileNumber}/card` | DELETE | 8000 |
| Create loan | `/api/customer/{mobileNumber}/loan` | POST | 8000 |
| Get loan | `/api/customer/{mobileNumber}/loan` | GET | 8000 |
| Update loan | `/api/customer/{mobileNumber}/loan` | PUT | 8000 |
| Delete loan | `/api/customer/{mobileNumber}/loan` | DELETE | 8000 |

---

## Customer Operations

### Onboard Customer

Create a new customer account.

**Request**:
```bash
POST http://localhost:8000/api/customer/onboard
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "mobileNumber": "1234567890"
}
```

**Parameters**:
- `name` (string, required) - Customer's full name (5-30 chars)
- `email` (string, required) - Valid email address
- `mobileNumber` (string, required) - 10-digit mobile number

**Response** (201 Created):
```json
{
  "statusCode": "201",
  "statusMessage": "Customer onboarded successfully"
}
```

**Error Cases**:
- `400` - Invalid input (invalid email, phone format, etc.)
- `409` - Customer already exists with this phone number

---

### Get Customer Details

Retrieve complete customer profile including account, card, and loan information.

**Request**:
```bash
GET http://localhost:8000/api/customer/details/1234567890
```

**Parameters**:
- `mobileNumber` (string, path) - Customer's 10-digit mobile number

**Response** (200 OK):
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "mobileNumber": "1234567890",
  "account": {
    "accountNumber": "00010012345678901",
    "accountType": "Savings",
    "branchAddress": "123 Main Street"
  },
  "card": {
    "cardNumber": "1234567890123456",
    "cardType": "Credit Card",
    "totalLimit": 100000,
    "amountUsed": 50000,
    "availableAmount": 50000
  },
  "loan": {
    "loanNumber": "123456789012",
    "loanType": "Home Loan",
    "totalLoan": 500000,
    "amountPaid": 100000
  }
}
```

**Error Cases**:
- `404` - Customer not found

---

### Update Customer Profile

Update customer details (name, email, account type, branch).

**Request**:
```bash
PUT http://localhost:8000/api/customer/update
Content-Type: application/json

{
  "name": "John Updated",
  "email": "john.updated@example.com",
  "mobileNumber": "1234567890",
  "account": {
    "accountNumber": "00010012345678901",
    "accountType": "Current",
    "branchAddress": "456 New Address"
  }
}
```

**Response** (200 OK):
```json
{
  "statusCode": "200",
  "statusMessage": "Customer profile updated successfully"
}
```

**Error Cases**:
- `400` - Invalid input
- `404` - Customer not found

---

### Offboard Customer

Delete customer and all associated data (account, cards, loans).

**Request**:
```bash
DELETE http://localhost:8000/api/customer/offboard/1234567890
```

**Parameters**:
- `mobileNumber` (string, path) - Customer's 10-digit mobile number

**Response** (200 OK):
```json
{
  "statusCode": "200",
  "statusMessage": "Customer offboarded successfully"
}
```

**Error Cases**:
- `404` - Customer not found

---

## Card Operations

Cards are nested resources under a customer. All operations use the customer's mobile number.

### Create Card

Issue a new credit/debit card for a customer.

**Request**:
```bash
POST http://localhost:8000/api/customer/1234567890/card
Content-Type: application/json

{
  "cardType": "Credit Card",
  "totalLimit": 100000
}
```

**Parameters**:
- `cardType` (string, required) - "Credit Card" or "Debit Card"
- `totalLimit` (integer, required) - Card spending limit

**Response** (201 Created):
```json
{
  "cardNumber": "1234567890123456",
  "cardType": "Credit Card",
  "totalLimit": 100000,
  "amountUsed": 0,
  "availableAmount": 100000
}
```

**Error Cases**:
- `404` - Customer not found
- `409` - Customer already has a card

---

### Get Card

Retrieve card details for a customer.

**Request**:
```bash
GET http://localhost:8000/api/customer/1234567890/card
```

**Response** (200 OK):
```json
{
  "cardNumber": "1234567890123456",
  "cardType": "Credit Card",
  "totalLimit": 100000,
  "amountUsed": 50000,
  "availableAmount": 50000
}
```

**Error Cases**:
- `404` - Customer or card not found

---

### Update Card

Update card limit or mark as active/inactive.

**Request**:
```bash
PUT http://localhost:8000/api/customer/1234567890/card
Content-Type: application/json

{
  "cardNumber": "1234567890123456",
  "cardType": "Credit Card",
  "totalLimit": 200000,
  "amountUsed": 50000
}
```

**Response** (204 No Content)

**Error Cases**:
- `404` - Customer or card not found

---

### Delete Card

Remove card from customer account.

**Request**:
```bash
DELETE http://localhost:8000/api/customer/1234567890/card
```

**Response** (204 No Content)

**Error Cases**:
- `404` - Customer or card not found

---

## Loan Operations

Loans are nested resources under a customer. All operations use the customer's mobile number.

### Create Loan

Issue a new loan to a customer.

**Request**:
```bash
POST http://localhost:8000/api/customer/1234567890/loan
Content-Type: application/json

{
  "loanType": "Home Loan",
  "totalLoan": 500000
}
```

**Parameters**:
- `loanType` (string, required) - Loan type ("Home Loan", "Personal Loan", etc.)
- `totalLoan` (integer, required) - Total loan amount

**Response** (201 Created):
```json
{
  "loanNumber": "123456789012",
  "loanType": "Home Loan",
  "totalLoan": 500000,
  "amountPaid": 0
}
```

**Error Cases**:
- `404` - Customer not found
- `409` - Customer already has a loan

---

### Get Loan

Retrieve loan details for a customer.

**Request**:
```bash
GET http://localhost:8000/api/customer/1234567890/loan
```

**Response** (200 OK):
```json
{
  "loanNumber": "123456789012",
  "loanType": "Home Loan",
  "totalLoan": 500000,
  "amountPaid": 100000
}
```

**Error Cases**:
- `404` - Customer or loan not found

---

### Update Loan

Update loan amount paid (simulate repayment).

**Request**:
```bash
PUT http://localhost:8000/api/customer/1234567890/loan
Content-Type: application/json

{
  "loanNumber": "123456789012",
  "loanType": "Home Loan",
  "totalLoan": 500000,
  "amountPaid": 150000
}
```

**Response** (204 No Content)

**Error Cases**:
- `404` - Customer or loan not found

---

### Delete Loan

Remove loan from customer account.

**Request**:
```bash
DELETE http://localhost:8000/api/customer/1234567890/loan
```

**Response** (204 No Content)

**Error Cases**:
- `404` - Customer or loan not found

---

## Account Operations

Account information is accessed through the gateway proxy to the Account Service.

### Get Account

Retrieve account details (direct proxy to account service).

**Request**:
```bash
GET http://localhost:8000/account/api/1234567890
```

**Response** (200 OK):
```json
{
  "accountNumber": "00010012345678901",
  "accountType": "Savings",
  "branchAddress": "123 Main Street"
}
```

**Error Cases**:
- `404` - Account not found

---

### Update Account

Update account type and branch address.

**Request**:
```bash
PUT http://localhost:8000/account/api
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "mobileNumber": "1234567890",
  "account": {
    "accountType": "Current",
    "branchAddress": "456 New Address"
  }
}
```

**Response** (200 OK)

**Error Cases**:
- `404` - Account not found

---

## HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | OK | Successfully retrieved or updated |
| 201 | Created | Resource created successfully |
| 204 | No Content | Update successful (empty response) |
| 400 | Bad Request | Invalid input (validation failed) |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource (e.g., customer already exists) |
| 500 | Internal Error | Server error |

---

## Error Response Format

All errors return a standardized response:

```json
{
  "path": "/api/customer/onboard",
  "status": "400",
  "message": "Mobile number must be 10 digits",
  "timestamp": "2026-02-07T10:15:30.123Z"
}
```

---

## Interactive Testing

### Swagger UI

Interactive API documentation with try-it-out functionality:

**Gateway**: http://localhost:8000/swagger-ui.html

**Backend Services** (local development only):
- Account: http://localhost:8080/account/swagger-ui.html
- Card: http://localhost:9000/card/swagger-ui.html
- Loan: http://localhost:8090/loan/swagger-ui.html

### cURL Examples

All examples above use cURL. Install with:
```bash
# macOS
brew install curl

# Linux (Debian/Ubuntu)
sudo apt-get install curl

# Windows (comes with Git Bash)
```

### Postman

1. Open Postman
2. Import the API collection (or create requests manually)
3. Set base URL to `http://localhost:8000`
4. Create requests using endpoints above

---

## Common Workflows

### Complete Customer Onboarding

```bash
# 1. Onboard customer
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane Doe","email":"jane@example.com","mobileNumber":"9876543210"}'

# 2. Create card
curl -X POST http://localhost:8000/api/customer/9876543210/card \
  -H "Content-Type: application/json" \
  -d '{"cardType":"Credit Card","totalLimit":100000}'

# 3. Create loan
curl -X POST http://localhost:8000/api/customer/9876543210/loan \
  -H "Content-Type: application/json" \
  -d '{"loanType":"Home Loan","totalLoan":500000}'

# 4. Get complete profile
curl http://localhost:8000/api/customer/details/9876543210
```

### Simulate Loan Repayment

```bash
# 1. Get current loan details
LOAN=$(curl http://localhost:8000/api/customer/1234567890/loan)

# 2. Update with new amountPaid
curl -X PUT http://localhost:8000/api/customer/1234567890/loan \
  -H "Content-Type: application/json" \
  -d '{"loanNumber":"123456789012","loanType":"Home Loan","totalLoan":500000,"amountPaid":200000}'
```

---

## Rate Limiting

No rate limiting is currently implemented. All endpoints accept unlimited requests.

---

## Authentication

No authentication is currently required. All endpoints are public.

---

## More Information

- **Getting Started**: [GETTING_STARTED.md](GETTING_STARTED.md)
- **Architecture & Code Patterns**: [ARCHITECTURE.md](ARCHITECTURE.md)
- **Resilience & Circuit Breakers**: [RESILIENCE.md](RESILIENCE.md)
- **Configuration**: [CONFIGURATION.md](CONFIGURATION.md)
