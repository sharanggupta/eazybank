# Dev Environment Setup

This directory contains the configuration to run EazyBank microservices in the dev environment.

## Prerequisites

- Docker and Docker Compose installed
- Java 25
- Maven (or use the included Maven wrapper in each service)

## Starting the Database

From this directory (`deploy/dev/`), run:

```bash
docker compose up -d
```

This starts a PostgreSQL 17 instance with three databases:
- `accountdb` - for the account microservice (port 8080)
- `carddb` - for the card microservice (port 9000)
- `loandb` - for the loan microservice (port 8090)

Connection details:
- Host: `localhost`
- Port: `5432`
- Username: `postgres`
- Password: `postgres`

## Running the Microservices

After starting the database, run each microservice from the repository root:

**Account Service (port 8080):**
```bash
cd ../../account
./mvnw spring-boot:run
```

**Card Service (port 9000):**
```bash
cd ../../card
./mvnw spring-boot:run
```

**Loan Service (port 8090):**
```bash
cd ../../loan
./mvnw spring-boot:run
```

## Stopping the Database

```bash
docker compose down
```

To also remove the persisted data:
```bash
docker compose down -v
```

## Verifying Services

### Health Checks

```bash
# Account service health
curl http://localhost:8080/account/actuator/health
# Expected: {"status":"UP"}

# Card service health
curl http://localhost:9000/card/actuator/health
# Expected: {"status":"UP"}

# Loan service health
curl http://localhost:8090/loan/actuator/health
# Expected: {"status":"UP"}
```

### Account Service API

```bash
# Create an account
curl -X POST http://localhost:8080/account/api/create \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'
# Expected: {"statusCode":"201","statusMessage":"Account created successfully"}

# Fetch account by mobile number
curl http://localhost:8080/account/api/fetch?mobileNumber=1234567890
# Expected: JSON with customer details including name, email, mobileNumber, and accountDto

# Update account
curl -X PUT http://localhost:8080/account/api/update \
  -H "Content-Type: application/json" \
  -d '{"name": "John Updated", "email": "john@example.com", "mobileNumber": "1234567890", "accountDto": {"accountNumber": <ACCOUNT_NUMBER>, "accountType": "Savings", "branchAddress": "123 New Street"}}'
# Expected: 204 No Content

# Delete account
curl -X DELETE "http://localhost:8080/account/api/delete?mobileNumber=1234567890"
# Expected: 204 No Content
```

### Card Service API

```bash
# Create a card
curl -X POST http://localhost:9000/card/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "cardType": "Credit Card", "totalLimit": 100000}'
# Expected: {"statusCode":"201","statusMessage":"Card created successfully"}

# Fetch card by mobile number
curl http://localhost:9000/card/api?mobileNumber=1234567890
# Expected: JSON with card details including cardNumber, cardType, totalLimit, amountUsed, availableAmount

# Update card
curl -X PUT http://localhost:9000/card/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "cardType": "Credit Card", "totalLimit": 200000, "amountUsed": 5000}'
# Expected: 204 No Content

# Delete card
curl -X DELETE "http://localhost:9000/card/api?mobileNumber=1234567890"
# Expected: 204 No Content
```

### Loan Service API

```bash
# Create a loan
curl -X POST http://localhost:8090/loan/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "loanType": "Home Loan", "totalLoan": 500000}'
# Expected: {"statusCode":"201","statusMessage":"Loan created successfully"}

# Fetch loan by mobile number
curl http://localhost:8090/loan/api?mobileNumber=1234567890
# Expected: JSON with loan details including loanNumber, loanType, totalLoan, amountPaid, outstandingAmount

# Update loan
curl -X PUT http://localhost:8090/loan/api \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "1234567890", "loanType": "Home Loan", "totalLoan": 500000, "amountPaid": 50000}'
# Expected: 204 No Content

# Delete loan
curl -X DELETE "http://localhost:8090/loan/api?mobileNumber=1234567890"
# Expected: 204 No Content
```

### Swagger UI

API documentation is available at:
- Account: http://localhost:8080/account/swagger-ui.html
- Card: http://localhost:9000/card/swagger-ui.html
- Loan: http://localhost:8090/loan/swagger-ui.html
