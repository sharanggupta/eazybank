# Getting Started with EazyBank

A complete guide to running EazyBank locally for the first time.

---

## Prerequisites

- **Docker & Docker Compose** - For containerized services
- **Java 25** - For local development with hot-reload
- **Maven Wrapper** - Included in each service directory (./mvnw)

---

## Quick Start (5 minutes)

### 1. Build Docker Images

```bash
cd deploy/dev
./build-images.sh
```

This uses Google Jib to build images for all 4 services directly to your Docker daemon.

### 2. Start Everything

```bash
docker compose up -d
```

This starts:
- **PostgreSQL 17** (3 databases: accountdb, carddb, loandb)
- **4 Microservices**:
  - Gateway (port 8000) - API entry point
  - Account (port 8080) - Customer accounts
  - Card (port 9000) - Card management
  - Loan (port 8090) - Loan management
- **Observability Stack**:
  - Prometheus (port 9090) - Metrics
  - Grafana (port 3000) - Dashboards
  - Loki (port 3100) - Logs
  - OTel Collector (port 4318) - Traces

### 3. Verify Services Are Running

```bash
# Check health
curl http://localhost:8000/actuator/health        # Gateway
curl http://localhost:8080/account/actuator/health  # Account
curl http://localhost:9000/card/actuator/health     # Card
curl http://localhost:8090/loan/actuator/health     # Loan

# Check Grafana dashboard
open http://localhost:3000  # Login: admin/admin
```

### 4. Test an API Call

```bash
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "mobileNumber": "1234567890"
  }'

# Retrieve the customer
curl http://localhost:8000/api/customer/1234567890
```

### 5. Stop Services

```bash
docker compose down  # Keep data
docker compose down -v  # Remove data
```

---

## Option 2: Local Development with Hot-Reload

For development with automatic code reload:

### 1. Start Only the Database

```bash
cd deploy/dev && docker compose up -d postgres
```

### 2. Run Services Individually

In separate terminal windows, from the repository root:

```bash
# Terminal 1: Account service
cd account && ./mvnw spring-boot:run

# Terminal 2: Card service
cd card && ./mvnw spring-boot:run

# Terminal 3: Loan service
cd loan && ./mvnw spring-boot:run

# Terminal 4: Gateway service
cd customer-gateway && ./mvnw spring-boot:run
```

### 3. Test API

```bash
curl http://localhost:8000/api/customer/1234567890
```

**Benefits**:
- Code changes apply instantly (no container rebuild needed)
- Faster iteration during development
- Easier debugging with IDE

**Drawbacks**:
- No observability stack running
- Services run on your machine (uses local resources)

---

## Accessing the Application

### API Gateway (Entry Point)

All client requests go through the gateway on port 8000:

```bash
# Customer operations
POST   http://localhost:8000/api/customer/onboard
GET    http://localhost:8000/api/customer/{mobileNumber}
PUT    http://localhost:8000/api/customer/update
DELETE http://localhost:8000/api/customer/delete

# Card operations (through gateway)
POST   http://localhost:8000/api/customer/{mobileNumber}/card
GET    http://localhost:8000/api/customer/{mobileNumber}/card

# Loan operations (through gateway)
POST   http://localhost:8000/api/customer/{mobileNumber}/loan
GET    http://localhost:8000/api/customer/{mobileNumber}/loan
```

### Swagger UI

Interactive API documentation available at:
- **Gateway**: http://localhost:8000/swagger-ui.html
- **Account**: http://localhost:8080/account/swagger-ui.html (local dev only)
- **Card**: http://localhost:9000/card/swagger-ui.html (local dev only)
- **Loan**: http://localhost:8090/loan/swagger-ui.html (local dev only)

### Monitoring Dashboards

When running with `docker compose up -d`:

**Grafana** (http://localhost:3000):
1. Login with `admin` / `admin`
2. Go to "EazyBank" folder
3. View dashboards:
   - **Application Overview** - Health, request rate, error rate, latency
   - **HTTP Metrics** - Status codes, response times, percentiles
   - **JVM Metrics** - Memory usage, garbage collection, threads
   - **Circuit Breakers** - Resilience4j states and failure rates

**Prometheus** (http://localhost:9090):
- Query metrics directly
- View scrape targets
- Example query: `rate(http_server_requests_seconds_count[5m])`

---

## Database Connection

PostgreSQL 17 runs on `localhost:5432` with these databases:

| Database | Service | Purpose |
|----------|---------|---------|
| accountdb | Account | Customer account data |
| carddb | Card | Credit/debit card data |
| loandb | Loan | Loan data |

**Credentials** (default):
- Username: `postgres`
- Password: `postgres`

**Connect manually** (optional):

```bash
psql -h localhost -U postgres -d accountdb
```

---

## Understanding the Architecture

```
                    ┌──────────────┐
    Client ────────► │   Gateway    │
                    │   :8000      │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ Account  │ │   Card   │ │   Loan   │
        │  :8080   │ │  :9000   │ │  :8090   │
        └────┬─────┘ └────┬─────┘ └────┬─────┘
             ▼            ▼            ▼
        ┌──────────────────────────────────┐
        │    PostgreSQL 17 Databases       │
        │ (accountdb, carddb, loandb)      │
        └──────────────────────────────────┘
```

### Key Points

- **Gateway** (port 8000) - Only entry point for clients
- **Backend Services** (ports 8080, 9000, 8090) - Internal, for debugging only
- **Databases** - Each service has its own database for data isolation
- **All communication** is through the gateway in production

---

## Common Operations

### View Logs

**Docker Compose**:
```bash
docker compose logs -f gateway      # Follow gateway logs
docker compose logs -f account      # Account service logs
docker compose logs account | grep ERROR  # Filter for errors
```

**Local Development**:
```bash
# Logs appear in the terminal where you ran the service
# Press Ctrl+C to stop the service
```

### Restart a Service

**Docker Compose**:
```bash
docker compose restart gateway
docker compose restart account
```

**Local Development**:
```bash
# Stop the service (Ctrl+C in the terminal)
# Run it again
cd account && ./mvnw spring-boot:run
```

### Clear Database

```bash
docker compose down -v
docker compose up -d  # Recreates clean databases
```

### Run Tests

```bash
cd account && ./mvnw test
cd card && ./mvnw test
cd loan && ./mvnw test
cd customer-gateway && ./mvnw test
```

---

## Next Steps

Once you have the application running:

1. **Explore APIs**: Use the [API_GUIDE.md](API_GUIDE.md) or Swagger UI to test endpoints

2. **Understand Resilience**: See [RESILIENCE.md](RESILIENCE.md) for circuit breaker patterns and testing

3. **Monitor with Observability**: See [docs/observability.md](observability.md) for dashboards and tracing

4. **Deploy to Kubernetes**: See [DEPLOYMENT.md](DEPLOYMENT.md) for staging/production setup

5. **Understand Code Patterns**: See [ARCHITECTURE.md](ARCHITECTURE.md) for code conventions

---

## Troubleshooting

### Services won't start

**Problem**: `docker compose up` fails with connection errors

**Solution**:
```bash
# Check if ports are already in use
lsof -i :8000
lsof -i :9000
lsof -i :8080
lsof -i :8090

# Remove existing containers
docker compose down
docker system prune
docker compose up -d
```

### Database connection refused

**Problem**: Services fail to connect to PostgreSQL

**Solution**:
```bash
# Verify postgres container is running
docker compose ps | grep postgres

# Check postgres logs
docker compose logs postgres

# Restart just the database
docker compose restart postgres
```

### Ports already in use

**Problem**: Cannot start services because ports 8000, 9000, etc. are in use

**Solution**:
```bash
# Check what's using the port
lsof -i :8000

# Either stop that service or modify docker-compose.yml port mapping
# Then try again
docker compose up -d
```

### Metrics not appearing in Grafana

**Problem**: Dashboards are empty, no metrics visible

**Solution**:
1. Wait 30-60 seconds for metrics to be collected
2. Verify Prometheus is scraping targets: http://localhost:9090/targets
3. Check if you're generating traffic (make API calls)
4. See [docs/observability.md](observability.md) for detailed troubleshooting

---

## More Information

- **Complete API Reference**: [API_GUIDE.md](API_GUIDE.md)
- **Production Deployment**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **Configuration Options**: [CONFIGURATION.md](CONFIGURATION.md)
- **Code Patterns**: [ARCHITECTURE.md](ARCHITECTURE.md)
- **Resilience Patterns**: [RESILIENCE.md](RESILIENCE.md)
- **Troubleshooting**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- **Monitoring Setup**: [observability.md](observability.md)
