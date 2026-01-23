# EazyBank

A microservices-based banking application built with Spring Boot.

## Architecture

EazyBank consists of three independent microservices:

| Service | Port | Description |
|---------|------|-------------|
| Account | 8080 | Customer account management |
| Card | 9000 | Credit/debit card management |
| Loan | 8090 | Loan management |

Each service has its own PostgreSQL database and can be developed, deployed, and scaled independently.

## Tech Stack

- Java 25
- Spring Boot 4.0.1
- PostgreSQL 17
- Docker Compose (local development)
- Testcontainers (integration testing)

## Getting Started

### Prerequisites

- Java 25
- Docker and Docker Compose
- Maven (or use the included wrapper)

### Running Locally

1. Start the databases:
   ```bash
   cd deploy/dev
   docker compose up -d
   ```

2. Run the microservices (in separate terminals):
   ```bash
   # Account service
   cd account && ./mvnw spring-boot:run

   # Card service
   cd card && ./mvnw spring-boot:run

   # Loan service
   cd loan && ./mvnw spring-boot:run
   ```

3. Verify the services are running:
   ```bash
   curl http://localhost:8080/account/actuator/health
   curl http://localhost:9000/card/actuator/health
   curl http://localhost:8090/loan/actuator/health
   ```

See [deploy/dev/README.md](deploy/dev/README.md) for detailed setup instructions and API examples.

## API Documentation

Swagger UI is available for each service:

- Account: http://localhost:8080/account/swagger-ui.html
- Card: http://localhost:9000/card/swagger-ui.html
- Loan: http://localhost:8090/loan/swagger-ui.html

## Project Structure

```
eazybank/
├── account/          # Account microservice
├── card/             # Card microservice
├── loan/             # Loan microservice
└── deploy/
    ├── dev/          # Local development (Docker Compose)
    └── production/   # Production deployment (Helm charts)
```

## Running Tests

Each microservice has its own test suite using Testcontainers:

```bash
# Run tests for a specific service
cd account && ./mvnw test

# Or run all tests
cd account && ./mvnw test
cd card && ./mvnw test
cd loan && ./mvnw test
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
