# EazyBank Documentation

Complete documentation for the EazyBank microservices platform.

## Quick Navigation

### Getting Started

| Document | Description |
|----------|-------------|
| [Main README](../README.md) | Project overview, quick start, architecture |
| [Local Development](../deploy/dev/README.md) | Docker Compose setup for local development |
| [Kubernetes Deployment](../deploy/helm/README.md) | Helm charts and K8s deployment |

### API & Testing

| Document | Description |
|----------|-------------|
| [API Examples](../deploy/dev/api-examples.md) | Complete API reference with curl examples |
| [Resilience Testing](../deploy/dev/resilience-testing.md) | Circuit breaker and graceful degradation testing |

### Configuration & Operations

| Document | Description |
|----------|-------------|
| [Configuration Reference](configuration-reference.md) | All environment variables and settings |
| [Deployment Operations](../DEPLOYMENT.md) | Production operations and troubleshooting |

### CI/CD & Versioning

| Document | Description |
|----------|-------------|
| [CI/CD Pipelines](../.github/workflows/README.md) | GitHub Actions workflow documentation |
| [Versioning Strategy](../.github/VERSIONING.md) | Semantic versioning and release process |

### Service Documentation

| Service | Description |
|---------|-------------|
| [Customer Gateway](../customer-gateway/README.md) | API Gateway, Write Gate pattern, resilience |
| [Account Service](../account/README.md) | Customer account management |
| [Card Service](../card/README.md) | Credit card management, graceful degradation |
| [Loan Service](../loan/README.md) | Loan management, graceful degradation |

---

## Documentation Structure

```
eazybank/
|
+-- README.md                    # Project overview & quick start
+-- DEPLOYMENT.md                # Production operations
+-- LICENSE                      # Apache 2.0
|
+-- docs/
|   +-- README.md                # This file - documentation index
|   +-- configuration-reference.md  # All configuration options
|
+-- .github/
|   +-- workflows/README.md      # CI/CD pipeline docs
|   +-- VERSIONING.md            # Semantic versioning guide
|
+-- customer-gateway/
|   +-- README.md                # Gateway architecture & API
|
+-- account/
|   +-- README.md                # Account service API
|
+-- card/
|   +-- README.md                # Card service API
|
+-- loan/
|   +-- README.md                # Loan service API
|
+-- deploy/
    +-- dev/
    |   +-- README.md            # Local Docker setup
    |   +-- api-examples.md      # API reference
    |   +-- resilience-testing.md  # Circuit breaker testing
    |
    +-- helm/
        +-- README.md            # Kubernetes deployment
```

---

## Reading Order

**For new developers**:
1. [Main README](../README.md) - Understand the architecture
2. [Local Development](../deploy/dev/README.md) - Get services running
3. [API Examples](../deploy/dev/api-examples.md) - Test the APIs
4. [Customer Gateway](../customer-gateway/README.md) - Understand the entry point

**For operations**:
1. [Kubernetes Deployment](../deploy/helm/README.md) - Deploy to K8s
2. [CI/CD Pipelines](../.github/workflows/README.md) - Understand the pipelines
3. [Configuration Reference](configuration-reference.md) - Configure services
4. [Deployment Operations](../DEPLOYMENT.md) - Day-to-day operations

**For understanding resilience**:
1. [Customer Gateway](../customer-gateway/README.md) - Write Gate pattern
2. [Resilience Testing](../deploy/dev/resilience-testing.md) - Test scenarios
3. [Card Service](../card/README.md) - Graceful degradation example
