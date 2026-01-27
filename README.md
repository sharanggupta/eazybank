# EazyBank

A microservices-based banking application demonstrating independent, scalable microservices architecture with Spring Boot and Kubernetes.

[![Build and Deploy](https://github.com/sharanggupta/eazybank/actions/workflows/deploy.yml/badge.svg)](https://github.com/sharanggupta/eazybank/actions/workflows/deploy.yml)

## 📋 Overview

EazyBank consists of three independent microservices, each with its own database. Each service can be developed, tested, deployed, and scaled independently.

| Service | Port | Context | Database | Purpose |
|---------|------|---------|----------|---------|
| Account | 8080 | `/account` | accountdb | Customer account management |
| Card | 9000 | `/card` | carddb | Credit/debit card management |
| Loan | 8090 | `/loan` | loandb | Loan management |

## 🛠️ Tech Stack

- **Runtime**: Java 25, Spring Boot 4.0.1
- **Data**: PostgreSQL 17
- **Containers**: Docker, Docker Compose
- **Orchestration**: Kubernetes (k3s), Helm
- **CI/CD**: GitHub Actions
- **Testing**: JUnit 5, Testcontainers
- **Building**: Maven, Google Jib

## 🚀 Quick Start

### Option 1: Local Development

**Prerequisites**: Java 25, Docker, Docker Compose

```bash
cd deploy/dev
docker compose up -d

# In separate terminals
cd account && ./mvnw spring-boot:run
cd card && ./mvnw spring-boot:run
cd loan && ./mvnw spring-boot:run
```

See [deploy/dev/README.md](deploy/dev/README.md) for details.

### Option 2: Kubernetes Deployment

**Prerequisites**: Kubernetes cluster (k3s or similar), kubectl configured

**Setup (5 minutes)**:

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
1. Builds Docker images → Pushes to GitHub Container Registry (FREE)
2. Deploys all 3 services to `eazybank-staging` namespace
3. Creates Kubernetes services automatically (exposed via NodePort)
4. Done (~5 minutes)

**Access Services** (automatically exposed via NodePort):
```bash
# Find NodePort assignments
kubectl get svc -n eazybank-staging

# Output example:
# NAME      TYPE       CLUSTER-IP    EXTERNAL-IP   PORT(S)             AGE
# account   NodePort   10.43.x.x     <none>        8080:31234/TCP      5m
# card      NodePort   10.43.x.x     <none>        9000:31567/TCP      5m
# loan      NodePort   10.43.x.x     <none>        8090:31890/TCP      5m

# Access Swagger UI in browser:
# Account: http://YOUR_CLUSTER_IP:31234/account/swagger-ui.html
# Card:    http://YOUR_CLUSTER_IP:31567/card/swagger-ui.html
# Loan:    http://YOUR_CLUSTER_IP:31890/loan/swagger-ui.html
```

**Monitor deployment:**

GitHub Actions workflow status:
```
https://github.com/sharanggupta/eazybank/actions
```

Cluster deployment status:
```bash
# Check pods
kubectl get pods -n eazybank-staging
kubectl get pods -n eazybank-prod

# Check services
kubectl get svc -n eazybank-staging
kubectl get svc -n eazybank-prod

# View logs
kubectl logs -f deployment/account -n eazybank-staging
```

For advanced setup (staging + production clusters): [deploy/helm/README.md](deploy/helm/README.md)

## 🔄 Development Workflow

### 1. Make Changes

```bash
# Edit code in your service (e.g., account/)
cd account
nano src/main/java/...

# Run tests
./mvnw test
```

### 2. Test Locally

```bash
# Start local environment
cd deploy/dev && docker compose up -d

# Run service
cd account && ./mvnw spring-boot:run

# Verify in another terminal
curl http://localhost:8080/account/swagger-ui.html
```

### 3. Push to Repository

```bash
git add .
git commit -m "feat: add new feature"
git push origin main
```

### 4. Automatic Deployment (if k8s configured)

- GitHub Actions automatically:
  1. Runs tests
  2. Builds Docker image
  3. Deploys to Kubernetes cluster
  4. Verifies health

GitHub Actions workflow file: [.github/workflows/deploy.yml](.github/workflows/deploy.yml)

## 📚 API Documentation

Swagger UI available for each service:

- **Account**: http://localhost:8080/account/swagger-ui.html
- **Card**: http://localhost:9000/card/swagger-ui.html
- **Loan**: http://localhost:8090/loan/swagger-ui.html

Health checks:
```bash
curl http://localhost:8080/account/actuator/health
curl http://localhost:9000/card/actuator/health
curl http://localhost:8090/loan/actuator/health
```

## 📁 Project Structure

```
eazybank/
├── account/                    # Account microservice
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── application-prod.yml
│   │   └── test/
│   └── pom.xml
│
├── card/                       # Card microservice (same structure)
├── loan/                       # Loan microservice (same structure)
│
└── deploy/
    ├── dev/                    # Local development
    │   ├── docker-compose.yml
    │   ├── init-databases.sql
    │   └── README.md
    │
    └── helm/                   # Kubernetes deployment
        ├── service-chart/      # Reusable Helm chart
        ├── services/           # Per-service configs
        │   ├── account/
        │   ├── card/
        │   └── loan/
        │       └── environments/
        │           ├── dev/      (local only)
        │           ├── staging/
        │           └── prod/
        ├── deploy.sh
        ├── template.sh
        └── README.md
```

## ✅ Testing the Application

### Unit & Integration Tests

Each service has unit and integration tests using Testcontainers (PostgreSQL in Docker):

```bash
# Test a single service
cd account && ./mvnw test

# Test all services
./account/mvnw -f account/pom.xml test
./card/mvnw -f card/pom.xml test
./loan/mvnw -f loan/pom.xml test
```

### API Testing Strategies

#### Strategy 1: Local Development (Port 8080, 9000, 8090)

Perfect for rapid iteration and debugging:

```bash
# Start local dev environment
cd deploy/dev && docker compose up -d

# In separate terminals, start each service
cd account && ./mvnw spring-boot:run  # http://localhost:8080/account
cd card && ./mvnw spring-boot:run     # http://localhost:9000/card
cd loan && ./mvnw spring-boot:run     # http://localhost:8090/loan

# Health checks
curl http://localhost:8080/account/actuator/health
curl http://localhost:9000/card/actuator/health
curl http://localhost:8090/loan/actuator/health

# Swagger UI
# http://localhost:8080/account/swagger-ui.html
# http://localhost:9000/card/swagger-ui.html
# http://localhost:8090/loan/swagger-ui.html
```

#### Strategy 2: Staging Environment (NodePort - External Access)

Staging services are exposed via **NodePort** for testing from outside the cluster:

```bash
# 1. Get NodePort assignments
kubectl get svc -n eazybank-staging

# Example output:
# NAME      TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)             AGE
# account   NodePort   10.43.46.248    <none>        8080:30874/TCP      5m
# card      NodePort   10.43.158.146   <none>        9000:31234/TCP      5m
# loan      NodePort   10.43.200.66    <none>        8090:31567/TCP      5m

# Extract specific NodePort with grep (example: account)
ACCOUNT_PORT=$(kubectl get svc account -n eazybank-staging -o jsonpath='{.spec.ports[0].nodePort}')
CARD_PORT=$(kubectl get svc card -n eazybank-staging -o jsonpath='{.spec.ports[0].nodePort}')
LOAN_PORT=$(kubectl get svc loan -n eazybank-staging -o jsonpath='{.spec.ports[0].nodePort}')

echo "Account: <CLUSTER_IP>:$ACCOUNT_PORT"
echo "Card: <CLUSTER_IP>:$CARD_PORT"
echo "Loan: <CLUSTER_IP>:$LOAN_PORT"

# 2. Test health
curl http://<CLUSTER_IP>:$ACCOUNT_PORT/account/actuator/health
curl http://<CLUSTER_IP>:$CARD_PORT/card/actuator/health
curl http://<CLUSTER_IP>:$LOAN_PORT/loan/actuator/health

# 3. Access Swagger UI in browser
# http://<CLUSTER_IP>:30874/account/swagger-ui.html
# http://<CLUSTER_IP>:31234/card/swagger-ui.html
# http://<CLUSTER_IP>:31567/loan/swagger-ui.html
```

#### Strategy 3: Production Environment (ClusterIP - Port-Forward)

Production services use **ClusterIP** (internal only), but you can test via port-forward:

```bash
# 1. Get service names and ports
kubectl get svc -n eazybank-prod

# Example output:
# NAME      TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
# account   ClusterIP  10.43.36.251    <none>        8080/TCP   3m
# card      ClusterIP  10.43.102.123   <none>        9000/TCP   2m
# loan      ClusterIP  10.43.200.66    <none>        8090/TCP   1m

# 2. Port-forward (temporary tunnel to test)
kubectl port-forward svc/account 8080:8080 -n eazybank-prod &
kubectl port-forward svc/card 9000:9000 -n eazybank-prod &
kubectl port-forward svc/loan 8090:8090 -n eazybank-prod &

# 3. Test via localhost (port-forwarded)
curl http://localhost:8080/account/actuator/health
curl http://localhost:9000/card/actuator/health
curl http://localhost:8090/loan/actuator/health

# Access Swagger UI
# http://localhost:8080/account/swagger-ui.html
# http://localhost:9000/card/swagger-ui.html
# http://localhost:8090/loan/swagger-ui.html

# 4. Stop port-forward (cleanup)
killall kubectl  # or kill individual PID
```

#### Strategy 4: Production with Ingress (Domain-based - Optional)

If ingress is configured with a custom domain:

```bash
# Check ingress configuration
kubectl get ingress -n eazybank-prod

# Example output:
# NAME                           CLASS   HOSTS               ADDRESS          PORTS   AGE
# account-eazybank-service       nginx   api.eazybank.com    <INGRESS_IP>     80      3m
# card-eazybank-service          nginx   api.eazybank.com    <INGRESS_IP>     80      2m
# loan-eazybank-service          nginx   api.eazybank.com    <INGRESS_IP>     80      1m

# Health checks (requires DNS or /etc/hosts entry)
curl http://api.eazybank.com/account/actuator/health
curl http://api.eazybank.com/card/actuator/health
curl http://api.eazybank.com/loan/actuator/health

# Or with HTTPS (if Let's Encrypt configured)
curl https://api.eazybank.com/account/actuator/health

# Swagger UI
# http://api.eazybank.com/account/swagger-ui.html
# http://api.eazybank.com/card/swagger-ui.html
# http://api.eazybank.com/loan/swagger-ui.html
```

### Health Checks & Status Verification

All services provide health check endpoints:

```bash
# Liveness probe (is service running?)
curl -s http://localhost:8080/account/actuator/health/liveness | jq

# Readiness probe (is service ready to accept traffic?)
curl -s http://localhost:8080/account/actuator/health/readiness | jq

# Full health info
curl -s http://localhost:8080/account/actuator/health | jq

# Check specific health indicators
curl -s http://localhost:8080/account/actuator/health/db | jq
curl -s http://localhost:8080/account/actuator/health/diskSpace | jq
```

### Performance Monitoring

```bash
# Pod resource usage
kubectl top pods -n eazybank-staging
kubectl top pods -n eazybank-prod

# HPA scaling status
kubectl get hpa -n eazybank-staging -w
kubectl get hpa -n eazybank-prod -w

# Watch replicas scale based on load
watch kubectl get deployment -n eazybank-prod

# Check metrics
kubectl get --raw /apis/custom.metrics.k8s.io/v1beta1/namespaces/eazybank-staging/pods/*/cpu_usage_seconds_total
```

### Troubleshooting

```bash
# Check pod logs
kubectl logs -f deployment/account -n eazybank-staging

# Describe pod for events
kubectl describe pod <POD_NAME> -n eazybank-staging

# Check service endpoints
kubectl get endpoints account -n eazybank-staging

# Check if pod is passing readiness probe
kubectl get pod <POD_NAME> -n eazybank-staging -o jsonpath='{.status.conditions[?(@.type=="Ready")]}'
```

## 🐳 Docker Images

Images are **automatically built and pushed** by GitHub Actions on push to main.

**Registry**: GitHub Container Registry (GHCR) - **FREE**
- No Docker Hub account needed
- Uses GitHub's built-in authentication
- Automatic builds via GitHub Actions (see `.github/workflows/`)

**Manual local builds** (optional):

```bash
# Build images locally
cd account && ./mvnw jib:dockerBuild
cd card && ./mvnw jib:dockerBuild
cd loan && ./mvnw jib:dockerBuild

# Verify
docker images | grep eazybank
```

## 🔐 Security Notes

- Database passwords use placeholders (`__DB_PASSWORD__`) in configuration
- GitHub Secrets store sensitive data (kubeconfig, passwords)
- Kubernetes Secrets used for runtime configuration
- No hardcoded credentials in source code
- Each service has own database (data isolation)

## 📖 Additional Documentation

- [deploy/dev/README.md](deploy/dev/README.md) - Local Docker Compose development
- [deploy/helm/README.md](deploy/helm/README.md) - Advanced Kubernetes configuration
- [INGRESS_SETUP.md](INGRESS_SETUP.md) - Optional: Custom domain + HTTPS via Ingress (free domain + Let's Encrypt)

## ✨ How It Works (Automated Pipeline)

When you push to `main`, the workflow automatically:

**1. Build** (Automatic)
- Tests all 3 services (JUnit 5 + Testcontainers)
- Builds Docker images with Docker buildx (multi-platform: amd64 + arm64)
- Generates semantic version (v0.0.1, v0.0.2, etc.)
- Pushes to GitHub Container Registry (free)
- Watch: https://github.com/sharanggupta/eazybank/actions

**2. Deploy to Staging** (Automatic)
- Deploys all 3 services to `eazybank-staging` namespace
- Services exposed via NodePort
- Access: `http://YOUR_CLUSTER_IP:NODEPORT/service-path/swagger-ui.html`

**3. Deploy to Production** (Manual Approval Required)
- Pauses and waits for approval
- Go to: https://github.com/sharanggupta/eazybank/actions
- Click running workflow → "Review deployments" → "Approve and deploy"
- Deploys to `eazybank-prod` namespace

**Setup production approval:**
1. Go to: https://github.com/sharanggupta/eazybank/settings/environments
2. Create environment: `production`
3. (Optional) Add required reviewers
4. Next push will pause at approval gate

## 💡 Common Tasks

### Deploy to Kubernetes

1. Set up Kubernetes cluster with k3s or similar
2. Add kubeconfig and password to GitHub Secrets
3. Push to main
4. Watch GitHub Actions → workflow completes → services deployed

### Scale a Service

Edit `deploy/helm/services/{service}/environments/{env}/k8s-values.yaml`:
```yaml
k8s:
  replicas: 5  # Change this
```

### Access Logs

```bash
# Use kubectl to access cluster logs
kubectl logs -f deployment/account -n eazybank-staging
```

### Rollback a Deployment

```bash
helm rollback account 1 -n eazybank-staging
```

## 📝 License

Licensed under Apache License 2.0 - see [LICENSE](LICENSE)
