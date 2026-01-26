# Kubernetes Deployment with Helm

Reference guide for deploying EazyBank microservices to Kubernetes using Helm.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Kubernetes Cluster                           │
├─────────────────────────────────────────────────────────────────────┤
│  Namespace: eazybank-staging          Namespace: eazybank-prod      │
│  ┌─────────────────────────┐          ┌─────────────────────────┐   │
│  │ account                 │          │ account                 │   │
│  │  ├── Deployment         │          │  ├── Deployment (HPA)   │   │
│  │  ├── Service (NodePort) │          │  ├── Service (NodePort) │   │
│  │  └── ConfigMap/Secret   │          │  └── ConfigMap/Secret   │   │
│  │                         │          │                         │   │
│  │ account-postgresql      │          │ account-postgresql      │   │
│  │  ├── StatefulSet        │          │  ├── StatefulSet        │   │
│  │  ├── PVC (5Gi)          │          │  ├── PVC (20Gi)         │   │
│  │  └── Secret             │          │  └── Secret             │   │
│  └─────────────────────────┘          └─────────────────────────┘   │
│                                                                     │
│  (Same structure for card and loan services)                        │
└─────────────────────────────────────────────────────────────────────┘
```

Each microservice:
- Owns its own PostgreSQL database (true independence)
- Can be deployed, scaled, updated independently
- Has separate configurations per environment

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
    └── loan/
```

## Helm Values (3-Level Inheritance)

**Level 1: Chart defaults** (`service-chart/values.yaml`)
- Schema and defaults for all services

**Level 2: Service identity** (`services/{service}/values.yaml`)
- Service name, port, context path, database name

**Level 3: Environment overrides** (`services/{service}/environments/{env}/`)
- `app-values.yaml` - Spring Boot config, replicas, storage
- `k8s-values.yaml` - K8s resources, HPA, ingress, probes

Final values = Chart defaults + Service identity + Environment overrides

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
      memory: "512Mi"
      cpu: "200m"
    limits:
      memory: "1Gi"
      cpu: "500m"

  hpa:
    enabled: false                       # Horizontal Pod Autoscaler
    minReplicas: 2
    maxReplicas: 10
    targetCPU: 70

  ingress:
    enabled: false                       # Optional: custom domain + HTTPS
    className: nginx
    host: custom.domain.com
    path: /account

  service:
    type: NodePort                       # Direct access via cluster IP:port
```

## Production Considerations

### Data Persistence

Database PVCs are configured with:
- `whenDeleted: Retain` - PVC survives `helm uninstall`
- `whenScaled: Retain` - PVC survives pod scale-down
- Automatic snapshots recommended for backups

### Security

- Passwords stored as Kubernetes Secrets (not ConfigMaps)
- Never commit real passwords - use `__PLACEHOLDER__` syntax
- Each service isolated: separate namespace, separate database
- RBAC least-privilege recommended

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

### High Availability

Production should enable:
- HPA (Horizontal Pod Autoscaler)
- Multiple replicas (min 3)
- Pod disruption budgets
- Node affinity for spread
- Resource requests/limits

## Manual Deployment (If Needed)

If you need to deploy without GitHub Actions:

```bash
# Set environment variables
export DB_PASSWORD="your-secure-password"
export IMAGE_TAG="v0.0.1"

# Deploy single service
helm upgrade --install account ./deploy/helm/service-chart \
  --namespace eazybank-staging \
  --set image.repository=ghcr.io/sharanggupta/eazybank/account \
  --set image.tag=$IMAGE_TAG \
  --set app.datasource.password=$DB_PASSWORD \
  --set postgresql.credentials.password=$DB_PASSWORD \
  -f ./deploy/helm/services/account/values.yaml \
  -f ./deploy/helm/services/account/environments/staging/app-values.yaml \
  -f ./deploy/helm/services/account/environments/staging/k8s-values.yaml

# Verify
kubectl get pods -n eazybank-staging
kubectl get svc -n eazybank-staging
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

- [README.md](../../README.md) - Quick start and overview
- [DEPLOYMENT.md](../../DEPLOYMENT.md) - Operational tasks (logs, scale, rollback)
- [INGRESS_SETUP.md](../../INGRESS_SETUP.md) - Optional custom domain + HTTPS
- [deploy/dev/README.md](../dev/README.md) - Local Docker Compose development
- [.github/workflows/README.md](../../.github/workflows/README.md) - CI/CD pipeline
