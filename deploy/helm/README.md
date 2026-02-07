# Kubernetes Deployment with Helm

Reference guide for deploying EazyBank microservices to Kubernetes using Helm.

## Architecture

```
                        ┌──────────────────────┐
        Clients ──────► │   Gateway (NodePort)  │
                        │   :8000               │
                        └──────────┬────────────┘
                                   │
                  ┌────────────────┼────────────────┐
                  ▼                ▼                ▼
           ┌────────────┐  ┌────────────┐  ┌────────────┐
           │  Account   │  │    Card    │  │    Loan    │  (ClusterIP)
           │  :8080     │  │   :9000    │  │   :8090    │
           └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
                 ▼               ▼               ▼
           ┌────────────┐  ┌────────────┐  ┌────────────┐
           │ accountdb  │  │   carddb   │  │   loandb   │  (PostgreSQL 17)
           │ StatefulSet│  │ StatefulSet│  │ StatefulSet│
           └────────────┘  └────────────┘  └────────────┘
```

- **Gateway** is the single entry point — exposed via NodePort
- **Backend services** (account, card, loan) use ClusterIP — only reachable within the cluster
- Each backend service owns its own PostgreSQL database
- All services can be deployed, scaled, and updated independently

## Directory Structure

```
deploy/helm/
├── service-chart/                      # Reusable Helm chart
│   ├── Chart.yaml                      # Chart metadata
│   ├── values.yaml                     # Default value schema
│   └── templates/                      # Kubernetes manifests
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── configmap.yaml
│       ├── secret.yaml
│       ├── ingress.yaml
│       ├── hpa.yaml
│       └── postgresql/
│           ├── statefulset.yaml
│           ├── service.yaml
│           └── secret.yaml
│
└── services/                           # Per-service configurations
    ├── account/
    │   ├── values.yaml                 # Service identity
    │   └── environments/
    │       ├── dev/
    │       ├── staging/
    │       └── prod/
    │           ├── app-values.yaml     # App config
    │           └── k8s-values.yaml     # K8s resources
    ├── card/
    ├── loan/
    └── customer-gateway/
```

## Helm Values (3-Level Inheritance)

**Level 1: Chart defaults** (`service-chart/values.yaml`)
- Schema and defaults for all services

**Level 2: Service identity** (`services/{service}/values.yaml`)
- Service name, port, context path, database name, downstream URLs

**Level 3: Environment overrides** (`services/{service}/environments/{env}/`)
- `app-values.yaml` — Spring Boot config, version
- `k8s-values.yaml` — K8s resources, HPA, ingress, service type, probes

Final values = Chart defaults + Service identity + Environment overrides

## Service Configuration

### Gateway

The gateway has no database (`postgresql.enabled: false`) and routes to backend services via environment variables:

```yaml
# services/customer-gateway/values.yaml
service:
  name: customer-gateway
  port: 8000

app:
  env:
    SERVICES_ACCOUNT_URL: "http://account:8080"
    SERVICES_CARD_URL: "http://card:9000"
    SERVICES_LOAN_URL: "http://loan:8090"

postgresql:
  enabled: false
```

### Backend Services (Account, Card, Loan)

Each has its own database and uses ClusterIP:

```yaml
# services/account/values.yaml
service:
  name: account
  port: 8080
  contextPath: /account

postgresql:
  database: accountdb
```

## Configuration Reference

### app-values.yaml Options

```yaml
image:
  tag: "v0.0.1"

app:
  spring:
    profiles: staging
  datasource:
    username: postgres
    password: __DB_PASSWORD__  # Replaced by CI/CD
  support:
    contactName: "Support Team"
    contactEmail: "support@example.com"

postgresql:
  credentials:
    password: __DB_PASSWORD__
```

### k8s-values.yaml Options

```yaml
k8s:
  replicas: 2                            # Pod count

  resources:
    requests:
      memory: "224Mi"
      cpu: "100m"
    limits:
      memory: "448Mi"
      cpu: "350m"

  hpa:
    enabled: false                       # Horizontal Pod Autoscaler
    minReplicas: 1
    maxReplicas: 2
    targetCPU: 95
    targetMemory: 95

  ingress:
    enabled: false
    className: nginx
    host: custom.domain.com

  service:
    type: NodePort                       # Gateway: NodePort, Backend: ClusterIP
```

## Manual Deployment

If you need to deploy without GitHub Actions:

```bash
# Deploy a backend service
helm upgrade --install account ./deploy/helm/service-chart \
  --namespace eazybank-staging \
  --set image.repository=ghcr.io/sharanggupta/eazybank/account \
  --set image.tag=$IMAGE_TAG \
  --set app.datasource.password=$DB_PASSWORD \
  --set postgresql.credentials.password=$DB_PASSWORD \
  -f ./deploy/helm/services/account/values.yaml \
  -f ./deploy/helm/services/account/environments/staging/app-values.yaml \
  -f ./deploy/helm/services/account/environments/staging/k8s-values.yaml

# Deploy the customer-gateway (no database secrets needed)
helm upgrade --install customer-gateway ./deploy/helm/service-chart \
  --namespace eazybank-staging \
  --set image.repository=ghcr.io/sharanggupta/eazybank/customer-gateway \
  --set image.tag=$IMAGE_TAG \
  -f ./deploy/helm/services/customer-gateway/values.yaml \
  -f ./deploy/helm/services/customer-gateway/environments/staging/app-values.yaml \
  -f ./deploy/helm/services/customer-gateway/environments/staging/k8s-values.yaml

# Or use the deploy script
./deploy.sh customer-gateway staging
./deploy.sh account staging

# Verify
kubectl get pods -n eazybank-staging
kubectl get svc -n eazybank-staging
```

## Production Considerations

### Data Persistence

Database PVCs are configured with:
- `whenDeleted: Retain` — PVC survives `helm uninstall`
- `whenScaled: Retain` — PVC survives pod scale-down

### Security

- Passwords stored as Kubernetes Secrets (not ConfigMaps)
- Never commit real passwords — use `__PLACEHOLDER__` syntax
- Each service isolated: separate database
- Backend services use ClusterIP — not externally accessible
- Only the gateway is exposed via NodePort

### Scaling

**Vertical scaling** (more powerful pods):
Edit `k8s-values.yaml` → resources.limits

**Horizontal scaling** (more pods):
Edit `k8s-values.yaml` → replicas OR enable HPA

**Auto-scaling** (based on metrics):
Enable HPA + install metrics-server:
```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

## Troubleshooting

### Pod stuck in Pending

```bash
kubectl describe pod <pod-name> -n eazybank-staging
# Check Events section for resource or volume issues
```

### Pod stuck in CrashLoopBackOff

```bash
kubectl logs <pod-name> -n eazybank-staging --previous
# Check for database connection, config, or application errors
```

### Database PVC not binding

```bash
kubectl get pvc -n eazybank-staging
kubectl describe pvc <pvc-name> -n eazybank-staging
# Ensure storage class exists: kubectl get storageclass
```

### Helm upgrade fails

```bash
# Dry-run to see what would happen
helm upgrade --install account ./deploy/helm/service-chart \
  --namespace eazybank-staging \
  --dry-run --debug \
  [... other flags ...]
```

## See Also

- [README.md](../../README.md) — Quick start and overview
- [deploy/dev/README.md](../dev/README.md) — Local Docker Compose development
- [.github/workflows/deploy.yml](../../.github/workflows/deploy.yml) — CI/CD pipeline
