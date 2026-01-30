# EazyBank

A microservices-based banking application demonstrating independent, scalable microservices architecture with Spring Boot and Kubernetes.

[![Build and Deploy](https://github.com/sharanggupta/eazybank/actions/workflows/deploy.yml/badge.svg)](https://github.com/sharanggupta/eazybank/actions/workflows/deploy.yml)

## Overview

EazyBank consists of four microservices. The **Gateway** is the single entry point — all client traffic flows through it. The backend services (Account, Card, Loan) are internal and not directly accessible.

| Service | Port | Context | Database | Purpose |
|---------|------|---------|----------|---------|
| Gateway | 8000 | — | — | API gateway, aggregation, routing |
| Account | 8080 | `/account` | accountdb | Customer account management |
| Card | 9000 | `/card` | carddb | Credit/debit card management |
| Loan | 8090 | `/loan` | loandb | Loan management |

### Architecture

```
                    ┌──────────────┐
    Clients ──────► │   Gateway    │ (NodePort - externally accessible)
                    │   :8000      │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ Account  │ │   Card   │ │   Loan   │  (ClusterIP - internal only)
        │  :8080   │ │  :9000   │ │  :8090   │
        └────┬─────┘ └────┬─────┘ └────┬─────┘
             ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │accountdb │ │  carddb  │ │  loandb  │  (PostgreSQL 17)
        └──────────┘ └──────────┘ └──────────┘
```

The gateway exposes:
- `/api/customer` — Customer lifecycle (onboard, fetch details, update profile, offboard)
- `/api/customer/{mobileNumber}/card` — Card management (issue, fetch, update, cancel)
- `/api/customer/{mobileNumber}/loan` — Loan management (apply, fetch, update, close)
- `/account/**`, `/card/**`, `/loan/**` — Proxied routes to backend services

## Tech Stack

- **Runtime**: Java 25, Spring Boot 4.0
- **Gateway**: Spring Cloud Gateway (WebMVC)
- **Data**: PostgreSQL 17
- **Containers**: Docker, Docker Compose
- **Orchestration**: Kubernetes (k3s), Helm
- **CI/CD**: GitHub Actions
- **Testing**: JUnit 5, Testcontainers
- **Building**: Maven, Google Jib

## Quick Start

### Option 1: Docker Compose

**Prerequisites**: Java 25, Docker, Docker Compose

```bash
# Build all images
cd deploy/dev
./build-images.sh

# Start everything (postgres + account + card + loan + gateway)
docker compose up -d
```

Gateway is available at `http://localhost:8000`. Backend services are also exposed directly for debugging.

See [deploy/dev/README.md](deploy/dev/README.md) for full details and API examples.

### Option 2: Local Development (with hot-reload)

```bash
# Start only the database
cd deploy/dev && docker compose up -d postgres

# In separate terminals from the repository root
cd account && ./mvnw spring-boot:run
cd card && ./mvnw spring-boot:run
cd loan && ./mvnw spring-boot:run
cd gateway && ./mvnw spring-boot:run
```

### Option 3: Kubernetes Deployment

**Prerequisites**: Kubernetes cluster (k3s or similar), kubectl configured

```bash
# Step 1: Prepare kubeconfig (ensure server IP is not 127.0.0.1)
cat ~/.kube/config | base64 -w 0

# Step 2: Add 2 GitHub Secrets
# KUBE_CONFIG = base64 output from above
# DB_PASSWORD = openssl rand -base64 32

# Step 3: Push to main
git push origin main
```

GitHub Actions automatically:
1. Builds Docker images for all 4 services and pushes to GitHub Container Registry
2. Deploys to `eazybank-staging` namespace
3. Waits for manual approval, then deploys to `eazybank-prod` namespace

**Access the Gateway** (the only externally exposed service):
```bash
# Find NodePort assignment
kubectl get svc gateway -n eazybank-staging

# Output example:
# NAME      TYPE       CLUSTER-IP    EXTERNAL-IP   PORT(S)          AGE
# gateway   NodePort   10.43.x.x    <none>        8000:31234/TCP   5m

# Access Swagger UI
# http://YOUR_CLUSTER_IP:31234/swagger-ui.html
```

Backend services (account, card, loan) use ClusterIP and are only reachable from within the cluster via the gateway.

For advanced Kubernetes configuration: [deploy/helm/README.md](deploy/helm/README.md)

## Development Workflow

### 1. Make Changes

```bash
cd gateway  # or account, card, loan
# Edit code
./mvnw test  # Run tests
```

### 2. Test Locally

```bash
cd deploy/dev && docker compose up -d postgres

# Run the service you're working on + gateway
cd account && ./mvnw spring-boot:run
cd gateway && ./mvnw spring-boot:run

# Verify
curl http://localhost:8000/api/customer/1234567890
```

### 3. Push to Repository

```bash
git add .
git commit -m "feat: add new feature"
git push origin main
```

### 4. Automatic Deployment (if k8s configured)

GitHub Actions automatically:
1. Builds Docker images (multi-platform: amd64 + arm64)
2. Deploys all 4 services to staging
3. On approval, deploys to production

Workflow file: [.github/workflows/deploy.yml](.github/workflows/deploy.yml)

## API Documentation

Swagger UI available through the gateway:
- **Gateway**: http://localhost:8000/swagger-ui.html

Backend services also expose Swagger UI directly (useful for local development):
- **Account**: http://localhost:8080/account/swagger-ui.html
- **Card**: http://localhost:9000/card/swagger-ui.html
- **Loan**: http://localhost:8090/loan/swagger-ui.html

Health checks:
```bash
# Gateway
curl http://localhost:8000/actuator/health

# Backend services (direct, for local dev)
curl http://localhost:8080/account/actuator/health
curl http://localhost:9000/card/actuator/health
curl http://localhost:8090/loan/actuator/health
```

## Project Structure

```
eazybank/
├── gateway/                    # API Gateway (entry point)
│   ├── src/
│   │   ├── main/java/
│   │   │   └── .../gateway/
│   │   │       ├── controller/     # REST controllers
│   │   │       ├── service/        # Business logic interfaces
│   │   │       │   └── impl/       # Service implementations
│   │   │       ├── client/         # Downstream service clients
│   │   │       │   ├── dto/        # Client transport DTOs
│   │   │       │   └── impl/       # Client implementations
│   │   │       ├── dto/            # API DTOs
│   │   │       ├── exception/      # Exception hierarchy
│   │   │       └── config/         # Spring configuration
│   │   └── main/resources/
│   │       ├── application.yaml
│   │       └── application-prod.yaml
│   └── pom.xml
│
├── account/                    # Account microservice
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   └── pom.xml
│
├── card/                       # Card microservice (same structure)
├── loan/                       # Loan microservice (same structure)
│
├── deploy/
│   ├── dev/                    # Local development
│   │   ├── docker-compose.yml
│   │   ├── build-images.sh
│   │   ├── init-databases.sql
│   │   └── README.md
│   │
│   └── helm/                   # Kubernetes deployment
│       ├── service-chart/      # Reusable Helm chart
│       ├── services/           # Per-service configs
│       │   ├── account/
│       │   ├── card/
│       │   ├── loan/
│       │   └── gateway/
│       │       └── environments/
│       │           ├── dev/
│       │           ├── staging/
│       │           └── prod/
│       ├── deploy.sh
│       ├── template.sh
│       └── README.md
│
└── .github/workflows/
    └── deploy.yml              # CI/CD pipeline
```

## Testing

### Unit and Integration Tests

Each backend service has unit and integration tests using Testcontainers (PostgreSQL in Docker):

```bash
# Test a single service
cd account && ./mvnw test

# Test all services
./account/mvnw -f account/pom.xml test
./card/mvnw -f card/pom.xml test
./loan/mvnw -f loan/pom.xml test
```

### API Testing

#### Local Development

```bash
cd deploy/dev && docker compose up -d

# Via gateway (port 8000)
curl -X POST http://localhost:8000/api/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "mobileNumber": "1234567890"}'

curl http://localhost:8000/api/customer/1234567890
```

#### Kubernetes (Staging)

The gateway is the only externally exposed service (NodePort):

```bash
# Get gateway NodePort
GATEWAY_PORT=$(kubectl get svc gateway -n eazybank-staging -o jsonpath='{.spec.ports[0].nodePort}')

# All requests go through the gateway
curl http://<CLUSTER_IP>:$GATEWAY_PORT/api/customer/1234567890

# Swagger UI
# http://<CLUSTER_IP>:$GATEWAY_PORT/swagger-ui.html
```

#### Kubernetes (Production)

Same pattern — gateway is the single entry point via NodePort:

```bash
GATEWAY_PORT=$(kubectl get svc gateway -n eazybank-prod -o jsonpath='{.spec.ports[0].nodePort}')
curl http://<CLUSTER_IP>:$GATEWAY_PORT/api/customer/1234567890
```

To debug backend services directly, use port-forward:
```bash
kubectl port-forward svc/account 8080:8080 -n eazybank-prod &
curl http://localhost:8080/account/actuator/health
```

### Monitoring

```bash
# Pod resource usage
kubectl top pods -n eazybank-staging

# HPA scaling status
kubectl get hpa -n eazybank-staging -w

# Logs
kubectl logs -f deployment/gateway -n eazybank-staging
kubectl logs -f deployment/account -n eazybank-staging
```

## Docker Images

Images are **automatically built and pushed** by GitHub Actions on push to main.

**Registry**: GitHub Container Registry (GHCR)
- Uses GitHub's built-in authentication
- Automatic builds via GitHub Actions

**Manual local builds** (optional):

```bash
cd deploy/dev
./build-images.sh

# Or individually
cd account && ./mvnw jib:dockerBuild
cd gateway && ./mvnw jib:dockerBuild
```

## Security

- Database passwords use placeholders (`__DB_PASSWORD__`) in configuration
- GitHub Secrets store sensitive data (kubeconfig, passwords)
- Kubernetes Secrets used for runtime configuration
- No hardcoded credentials in source code
- Each backend service has its own database (data isolation)
- Backend services are not externally accessible (ClusterIP) — only the gateway is exposed

## Pipeline

When you push to `main`:

**1. Build** (Automatic)
- Builds Docker images for all 4 services (multi-platform: amd64 + arm64)
- Generates semantic version (v0.0.1, v0.0.2, etc.)
- Pushes to GitHub Container Registry

**2. Deploy to Staging** (Automatic)
- Deploys all 4 services to `eazybank-staging` namespace
- Backend services: ClusterIP (internal)
- Gateway: NodePort (externally accessible)

**3. Deploy to Production** (Manual Approval Required)
- Pauses and waits for approval
- Go to: https://github.com/sharanggupta/eazybank/actions
- Click running workflow → "Review deployments" → "Approve and deploy"
- Deploys to `eazybank-prod` namespace

## Common Tasks

### Scale a Service

Edit `deploy/helm/services/{service}/environments/{env}/k8s-values.yaml`:
```yaml
k8s:
  replicas: 3
```

### Access Logs

```bash
kubectl logs -f deployment/gateway -n eazybank-staging
kubectl logs -f deployment/account -n eazybank-staging
```

### Rollback a Deployment

```bash
helm rollback gateway 1 -n eazybank-staging
```

## Additional Documentation

- [deploy/dev/README.md](deploy/dev/README.md) — Local Docker Compose development
- [deploy/helm/README.md](deploy/helm/README.md) — Advanced Kubernetes configuration

## License

Licensed under Apache License 2.0 — see [LICENSE](LICENSE)
