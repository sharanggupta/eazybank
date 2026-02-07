# Configuration Guide

All configuration options for EazyBank services across development, staging, and production.

---

## Quick Reference

### Service Ports

| Service | Port | Context | Health Check |
|---------|------|---------|--------------|
| **Gateway** | 8000 | `/` | `/actuator/health` |
| **Account** | 8080 | `/account` | `/account/actuator/health` |
| **Card** | 9000 | `/card` | `/card/actuator/health` |
| **Loan** | 8090 | `/loan` | `/loan/actuator/health` |
| **PostgreSQL** | 5432 | N/A | N/A |

### Database Connection

```
PostgreSQL 17 with 3 databases:
  accountdb  - Account service
  carddb     - Card service
  loandb     - Loan service

Default Credentials:
  Host:     localhost (Docker) or docker hostname (Kubernetes)
  Port:     5432
  Username: postgres
  Password: postgres (dev) or $DB_PASSWORD env var (prod)
```

---

## Environment Variables

### By Environment

#### Development (Docker Compose)

```bash
SPRING_PROFILES_ACTIVE=dev
SPRING_R2DBC_URL=r2dbc:postgresql://postgres:5432/accountdb
SPRING_R2DBC_USERNAME=postgres
SPRING_R2DBC_PASSWORD=postgres
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318/v1/traces
```

#### Staging (Kubernetes)

```bash
SPRING_PROFILES_ACTIVE=staging
SPRING_R2DBC_URL=r2dbc:postgresql://postgres:5432/accountdb
SPRING_R2DBC_USERNAME=postgres
SPRING_R2DBC_PASSWORD=${DB_PASSWORD}
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector.observability-staging:4318/v1/traces
```

#### Production (Kubernetes)

```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_R2DBC_URL=r2dbc:postgresql://postgres:5432/accountdb
SPRING_R2DBC_USERNAME=postgres
SPRING_R2DBC_PASSWORD=${DB_PASSWORD_PROD}
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector.observability-prod:4318/v1/traces
```

### Gateway-Specific Variables

```bash
# Downstream service URLs (for service-to-service communication)
SERVICES_ACCOUNT_URL=http://account:8080      # Account service
SERVICES_CARD_URL=http://card:9000            # Card service
SERVICES_LOAN_URL=http://loan:8090            # Loan service
```

---

## Spring Profiles

Services support profiles for different environments:

| Profile | Usage | Trace Sampling | Logging |
|---------|-------|----------------|---------|
| `dev` | Local development | 100% | Plain text |
| `staging` | Kubernetes staging | 100% | JSON |
| `prod` | Kubernetes production | 10% | JSON |

**Set via environment variable**:
```bash
SPRING_PROFILES_ACTIVE=dev
SPRING_PROFILES_ACTIVE=staging
SPRING_PROFILES_ACTIVE=prod
```

---

## Circuit Breaker Configuration

The gateway uses Resilience4j for fault tolerance. Configuration differs by profile:

### Development Profile

File: `customer-gateway/src/main/resources/application-dev.yaml`

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 2
        minimum-number-of-calls: 1
        failure-rate-threshold: 100        # Trip on first failure
        wait-duration-in-open-state: 5s    # Fast recovery
        ignore-exceptions:
          - WebClientResponseException$BadRequest
          - WebClientResponseException$NotFound
```

**Purpose**: Quick feedback during development

### Production Profile

File: `customer-gateway/src/main/resources/application.yaml`

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50         # Trip at 50% failure rate
        wait-duration-in-open-state: 30s   # Longer recovery window
        ignore-exceptions:
          - WebClientResponseException$BadRequest
          - WebClientResponseException$NotFound
```

**Purpose**: Stable production behavior

### Configuration Details

| Setting | Meaning | Dev Value | Prod Value |
|---------|---------|-----------|-----------|
| `sliding-window-size` | Number of recent calls analyzed | 2 | 10 |
| `minimum-number-of-calls` | Min calls before considering | 1 | 5 |
| `failure-rate-threshold` | Threshold to open circuit | 100% | 50% |
| `wait-duration-in-open-state` | Time before trying half-open | 5s | 30s |
| `ignore-exceptions` | Errors that don't count as failures | 4xx, 5xx errors | 4xx, 5xx errors |

---

## Observability Configuration

### Tracing (OpenTelemetry)

```yaml
management:
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces  # Default
  tracing:
    sampling:
      probability: 1.0      # Dev/Staging: 100% sampling
      probability: 0.1      # Production: 10% sampling
```

### Metrics (Prometheus)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
  endpoint:
    prometheus:
      enabled: true
```

All services expose metrics at: `http://service:port/actuator/prometheus`

### Logging

**Development** (plain text):
```
2026-02-07 10:15:30.123 INFO [account,abc123def456,789ghi] --- [reactor-http-epoll-5] AccountServiceImpl : Account created
```

**Staging/Production** (JSON with trace context):
```json
{
  "timestamp": "2026-02-07T10:15:30.123Z",
  "level": "INFO",
  "service": "account",
  "trace_id": "abc123def456",
  "span_id": "789ghi",
  "thread": "reactor-http-epoll-5",
  "logger": "dev.sharanggupta.account.service.AccountServiceImpl",
  "message": "Account created"
}
```

---

## Database Connection Pooling

All services use R2DBC with connection pooling:

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 10
      max-size: 20
      max-idle-time: 30m
```

| Setting | Value | Purpose |
|---------|-------|---------|
| `initial-size` | 10 | Connections created on startup |
| `max-size` | 20 | Maximum connections allowed |
| `max-idle-time` | 30m | Close connections unused for 30min |

---

## Kubernetes Secrets

In production (Kubernetes), sensitive data is stored as secrets:

```bash
# Create namespace
kubectl create namespace eazybank-prod

# Create database password secret
kubectl create secret generic db-credentials \
  --from-literal=password='your-secure-password' \
  -n eazybank-prod

# Reference in Helm values:
# app.datasource.password is populated from this secret
```

---

## Docker Compose Override

For local development, override default configuration:

```yaml
# Example: docker-compose.override.yml
version: '3.8'
services:
  account:
    environment:
      SPRING_PROFILES_ACTIVE: prod  # Test production settings locally
      OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4318/v1/traces
```

---

## Common Configuration Tasks

### Enable JSON Logging Locally

```bash
# Start services with prod profile to test JSON logging
SPRING_PROFILES_ACTIVE=prod docker compose up account
```

### Disable Tracing to Reduce Overhead

```yaml
# In application.yaml
management:
  tracing:
    sampling:
      probability: 0.0  # Disable all tracing
```

### Increase Database Connections

```yaml
# In application.yaml
spring:
  r2dbc:
    pool:
      max-size: 50  # Increase from 20
```

### Change Circuit Breaker Thresholds

```yaml
# In customer-gateway/src/main/resources/application.yaml
resilience4j:
  circuitbreaker:
    instances:
      account-service:
        failure-rate-threshold: 30  # Stricter threshold
```

---

## Configuration Files by Service

### Account Service

```
account/src/main/resources/
├── application.yaml           # Base config
├── application-dev.yaml       # Dev overrides
└── application-prod.yaml      # Prod overrides
```

### Card & Loan Services

Same structure as Account Service

### Gateway Service

```
customer-gateway/src/main/resources/
├── application.yaml           # Base config (circuit breaker settings)
├── application-dev.yaml       # Dev overrides (aggressive thresholds)
└── application-prod.yaml      # Prod overrides (if different from base)
```

---

## Validating Configuration

### Check Running Configuration

```bash
# View active configuration
curl http://localhost:8080/account/actuator/env

# View specific property
curl http://localhost:8080/account/actuator/env?name=spring.profiles.active
```

### Verify Database Connection

```bash
# Check database connectivity via health endpoint
curl http://localhost:8080/account/actuator/health/db

# Response should show: "status": "UP"
```

### Verify Observability Setup

```bash
# Check if tracing is configured
curl http://localhost:8080/account/actuator/health/livenessState

# View active metrics (should see http_server_requests_seconds)
curl http://localhost:9090/api/v1/labels | jq '.data[] | select(. | contains("http"))'
```

---

## Troubleshooting

### Services won't start

**Check logs for configuration errors**:
```bash
docker compose logs account | grep -i config
```

### Database connection refused

**Verify connection string and credentials**:
```bash
SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/accountdb
SPRING_R2DBC_USERNAME=postgres
SPRING_R2DBC_PASSWORD=postgres
```

### Circuit breaker not triggering

**Verify configuration is loaded**:
```bash
curl http://localhost:8000/actuator/circuitbreakers
```

### No metrics appearing

**Verify metrics endpoint is exposed**:
```bash
curl http://localhost:8080/account/actuator/prometheus | head -20
```

---

## More Information

- **Complete Configuration Reference**: [configuration-reference.md](configuration-reference.md)
- **Getting Started**: [GETTING_STARTED.md](GETTING_STARTED.md)
- **Deployment**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **Observability**: [observability.md](observability.md)
