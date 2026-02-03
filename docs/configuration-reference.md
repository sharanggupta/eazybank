# Configuration Reference

Complete reference for all environment variables and configuration options.

## Service Ports

| Service | Port | Context Path | Health |
|---------|------|--------------|--------|
| **Gateway** | 8000 | `/` | `/actuator/health` |
| **Account** | 8080 | `/account` | `/account/actuator/health` |
| **Card** | 9000 | `/card` | `/card/actuator/health` |
| **Loan** | 8090 | `/loan` | `/loan/actuator/health` |
| **PostgreSQL** | 5432 | N/A | N/A |

## Database Configuration

### PostgreSQL Connection

All services connect to PostgreSQL 17:

```
Host:     localhost (Docker) or docker-host (remote)
Port:     5432
Username: postgres
Password: postgres
```

### Databases

| Database | Service | Purpose |
|----------|---------|---------|
| `accountdb` | Account | Customer account data |
| `carddb` | Card | Credit card information |
| `loandb` | Loan | Loan applications and details |

### Connection String Format

```
jdbc:postgresql://{host}:{port}/{database}?currentSchema=public
```

## Environment Variables

### Gateway Configuration

**Downstream Service URLs**:
```
SERVICES_ACCOUNT_URL=http://account:8080    # Account service URL
SERVICES_CARD_URL=http://card:9000          # Card service URL
SERVICES_LOAN_URL=http://loan:8090          # Loan service URL
```

### Circuit Breaker Configuration

**For each service** (account-service, card-service, loan-service):

```
# Sliding window size (number of calls to evaluate)
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_{SERVICE}_SLIDINGWINDOWSIZE=2

# Minimum calls needed before circuit breaker evaluates state
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_{SERVICE}_MINIMUMNUMBEROFCALLS=1

# Failure rate threshold (%) to trip circuit breaker
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_{SERVICE}_FAILURERATHRESHOLD=100

# Duration to wait before transitioning from OPEN to HALF_OPEN
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_{SERVICE}_WAITDURATIONINOPENSTATE=5s

# Calls permitted in HALF_OPEN state before evaluating
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_{SERVICE}_PERMITTEDNUMBEROFCALLSINHALFOPPENSTATE=3
```

**Dev Default Values** (in `deploy/dev/docker-compose.yml`):
```yaml
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_ACCOUNT_SERVICE_SLIDINGWINDOWSIZE: "2"
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_ACCOUNT_SERVICE_MINIMUMNUMBEROFCALLS: "1"
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_ACCOUNT_SERVICE_FAILURERATHRESHOLD: "100"
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_ACCOUNT_SERVICE_WAITDURATIONINOPENSTATE: "5s"

# (Same pattern for card-service and loan-service)
```

**Production Recommended Values**:
```yaml
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_ACCOUNT_SERVICE_SLIDINGWINDOWSIZE: "100"
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_ACCOUNT_SERVICE_MINIMUMNUMBEROFCALLS: "10"
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_ACCOUNT_SERVICE_FAILURERATHRESHOLD: "50"
RESILIENCE4J_CIRCUITBREAKER_INSTANCES_ACCOUNT_SERVICE_WAITDURATIONINOPENSTATE: "60s"
```

## Spring Profiles

Services support multiple Spring profiles for different environments:

```
SPRING_PROFILES_ACTIVE=dev     # Development profile
SPRING_PROFILES_ACTIVE=prod    # Production profile
```

### Available Profiles

- **dev**: Development configuration with lower circuit breaker thresholds
- **prod**: Production configuration with standard thresholds (not yet configured, uses defaults)

## application.yaml Configuration Files

### Gateway (`gateway/src/main/resources/application.yaml`)

```yaml
server:
  port: 8000                           # Gateway port

spring:
  application:
    name: customergateway                      # Service name
  threads:
    virtual:
      enabled: true                    # Enable virtual threads (Java 21+)
  jackson:
    default-property-inclusion: non_null  # Exclude null fields from JSON

services:
  account-url: http://localhost:8080   # Account service URL
  card-url: http://localhost:9000      # Card service URL
  loan-url: http://localhost:8090      # Loan service URL

resilience4j:
  circuitbreaker:
    instances:
      account-service:
        registerHealthIndicator: true
        failureRateThreshold: 50
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 10s
      card-service:
        registerHealthIndicator: true
        failureRateThreshold: 50
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 10s
      loan-service:
        registerHealthIndicator: true
        failureRateThreshold: 50
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 10s

management:
  endpoints:
    web:
      exposure:
        include: health,info,customergateway,circuitbreakers
  health:
    circuitbreakers:
      enabled: true
```

### Account Service

```yaml
server:
  port: 8080
  servlet:
    context-path: /account

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/accountdb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
  application:
    name: account
```

### Card Service

```yaml
server:
  port: 9000
  servlet:
    context-path: /card

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/carddb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
  application:
    name: card
```

### Loan Service

```yaml
server:
  port: 8090
  servlet:
    context-path: /loan

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/loandb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
  application:
    name: loan
```

## Overriding Configuration

### Via Environment Variables

Docker Compose and Kubernetes automatically apply environment variable overrides:

```bash
# Override in docker-compose.yml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/accountdb
  SPRING_DATASOURCE_USERNAME: postgres
  SERVICES_CARD_URL: http://card:9000
```

### Via Command Line

When running locally:

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="\
    --server.port=8080,\
    --spring.datasource.url=jdbc:postgresql://localhost:5432/accountdb"
```

### Via application-{profile}.yaml

Create service-specific configuration files:

```
src/main/resources/
├── application.yaml           # Default (dev)
├── application-prod.yaml      # Production overrides
└── application-local.yaml     # Local development overrides
```

Then activate:
```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## JVM Configuration

For optimal performance:

```bash
# Enable G1GC (recommended for Spring Boot)
-XX:+UseG1GC \

# Virtual threads (Java 21+)
-Dspring.threads.virtual.enabled=true \

# Heap size for development
-Xms256m -Xmx512m \

# Heap size for production
-Xms1g -Xmx2g
```

## Health Check Endpoints

All services expose health endpoints:

```bash
# Default health check
GET /actuator/health

# With context path
GET /account/actuator/health
GET /card/actuator/health
GET /loan/actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "circuitbreakers": {
      "status": "UP"
    }
  }
}
```

## Logging Configuration

### Default Log Level

```
INFO    # Most logs
WARN    # Warnings and errors
ERROR   # Errors only
```

### Change Log Level via Environment

```bash
LOGGING_LEVEL_ROOT=DEBUG
LOGGING_LEVEL_DEV_SHARANGGUPTA=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=DEBUG
LOGGING_LEVEL_ORG_HIBERNATE=DEBUG
```

### Log Format

Spring Boot default format:
```
timestamp [thread] LEVEL package.class : message
2026-01-31 13:00:00.000 [main] INFO  o.s.b.StartupInfoLogger : EazyBank started
```

## Actuator Endpoints

Exposed endpoints in `application.yaml`:

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Service health status |
| `/actuator/info` | Application information |
| `/actuator/circuitbreakers` | Circuit breaker states |
| `/actuator/gateway` | Gateway routes (gateway only) |

Example:
```bash
# Gateway circuit breaker status
curl http://localhost:8000/actuator/circuitbreakers

# Specific circuit breaker
curl http://localhost:8000/actuator/circuitbreakers/card-service
```

## Docker Compose Environment

All services in `docker-compose.yml` use `SPRING_PROFILES_ACTIVE=dev`:

```yaml
services:
  gateway:
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SERVICES_ACCOUNT_URL: http://account:8080
      SERVICES_CARD_URL: http://card:9000
      SERVICES_LOAN_URL: http://loan:8090
```

To override for production testing:

```bash
SPRING_PROFILES_ACTIVE=prod docker compose up
```

---

## Quick Reference

| Task | Environment Variable | Value |
|------|----------------------|-------|
| Change account service URL | `SERVICES_ACCOUNT_URL` | `http://account:8080` |
| Trip circuit breaker faster | `RESILIENCE4J_CIRCUITBREAKER_INSTANCES_CARD_SERVICE_MINIMUMNUMBEROFCALLS` | `1` |
| Slower circuit breaker recovery | `RESILIENCE4J_CIRCUITBREAKER_INSTANCES_CARD_SERVICE_WAITDURATIONINOPENSTATE` | `60s` |
| Enable debug logging | `LOGGING_LEVEL_ROOT` | `DEBUG` |
| Change gateway port | `SERVER_PORT` | `9000` |
| Change database host | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://remote-db:5432/accountdb` |

---

For setup instructions, see [../deploy/dev/README.md](../deploy/dev/README.md)
For API examples, see [../deploy/dev/api-examples.md](../deploy/dev/api-examples.md)
For resilience testing, see [../deploy/dev/resilience-testing.md](../deploy/dev/resilience-testing.md)
