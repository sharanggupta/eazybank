# EazyBank Kubernetes Deployment

This directory contains Helm charts and configurations for deploying EazyBank microservices to Kubernetes.

## Table of Contents

- [Quick Start](#quick-start)
- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Directory Structure](#directory-structure)
- [Local Development (Dev Environment)](#local-development-dev-environment)
- [Staging Environment](#staging-environment)
- [Production Environment](#production-environment)
- [GitHub Actions CI/CD](#github-actions-cicd)
- [Configuration Reference](#configuration-reference)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

**Fastest way to get running**

### How It Works

```
Push code to main
    ↓
GitHub Actions: Build images → Push to GitHub Container Registry (FREE)
    ↓
GitHub Actions: Deploy to Staging namespace
    ↓
Services auto-exposed via NodePort
    ↓
✅ Access via: http://CLUSTER_IP:NODEPORT/service-path/swagger-ui.html
```

See [DEPLOYMENT.md](../../DEPLOYMENT.md) for detailed setup instructions and operational tasks.

### Prerequisites

- Kubernetes cluster with k3s or similar (e.g., Hetzner Cloud, DigitalOcean, local k3s)
- `kubectl` configured with cluster access
- GitHub repository with this code

### Quick Setup (5 minutes)

**Step 1: Prepare kubeconfig**

```bash
# Ensure your kubeconfig points to the correct server IP (not 127.0.0.1)
cat ~/.kube/config | base64 -w 0
# Copy the entire base64 output
```

**Step 2: Add GitHub Secrets**

Go to: https://github.com/YOUR_USERNAME/eazybank/settings/secrets/actions

Add 2 secrets:
- `KUBE_CONFIG` = Base64-encoded kubeconfig
- `DB_PASSWORD` = `openssl rand -base64 32`

**Step 3: Push to GitHub**

```bash
git push origin main
```

**Step 4: Watch deployment**

Go to: https://github.com/YOUR_USERNAME/eazybank/actions

Workflow automatically:
1. Builds all 3 services
2. Pushes to GitHub Container Registry
3. Deploys to `eazybank-staging` namespace
4. Done (~5 minutes total)

### Access Services

```bash
# Port-forward from your local machine
kubectl --kubeconfig ~/.kube/config port-forward svc/account 8080:8080 -n eazybank-staging &
kubectl --kubeconfig ~/.kube/config port-forward svc/card 9000:9000 -n eazybank-staging &
kubectl --kubeconfig ~/.kube/config port-forward svc/loan 8090:8090 -n eazybank-staging &

# Test
curl http://localhost:8080/account/actuator/health
```

### Update Services

```bash
# Just push to main - everything is automatic
git commit -am "Update account service"
git push origin main
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Kubernetes Cluster                           │
├─────────────────────────────────────────────────────────────────────┤
│  Namespace: eazybank-staging          Namespace: eazybank-prod      │
│  ┌─────────────────────────┐          ┌─────────────────────────┐   │
│  │ account                 │          │ account                 │   │
│  │  ├── Deployment         │          │  ├── Deployment (HPA)   │   │
│  │  ├── Service            │          │  ├── Service            │   │
│  │  ├── Ingress            │          │  ├── Ingress            │   │
│  │  └── ConfigMap/Secret   │          │  └── ConfigMap/Secret   │   │
│  │                         │          │                         │   │
│  │ account-postgresql      │          │ account-postgresql      │   │
│  │  ├── StatefulSet        │          │  ├── StatefulSet        │   │
│  │  ├── Service            │          │  ├── Service            │   │
│  │  ├── PVC (1Gi)          │          │  ├── PVC (20Gi)         │   │
│  │  └── Secret             │          │  └── Secret             │   │
│  └─────────────────────────┘          └─────────────────────────┘   │
│                                                                     │
│  (Same structure for card and loan services)                        │
└─────────────────────────────────────────────────────────────────────┘
```

Each microservice:
- Owns its own PostgreSQL database (true microservice independence)
- Can be deployed, scaled, and updated independently
- Has separate configurations per environment

---

## Prerequisites

### Required Tools

| Tool | Version | Installation |
|------|---------|--------------|
| kubectl | 1.28+ | https://kubernetes.io/docs/tasks/tools/ |
| Helm | 3.14+ | https://helm.sh/docs/intro/install/ |
| Docker | 24+ | https://docs.docker.com/get-docker/ |

### Kubernetes Cluster Access

You need `kubectl` configured with access to your target cluster(s):

```bash
# Verify cluster access
kubectl cluster-info
kubectl get nodes
```

### Required Kubernetes Resources

Ensure your cluster has:

1. **Ingress Controller** (any of these):
   - NGINX Ingress Controller
   - Traefik
   - Cloud-native (AWS ALB, GKE Ingress, etc.)

2. **Storage Class** for PostgreSQL PVCs:
   ```bash
   # Check available storage classes
   kubectl get storageclass
   ```

3. **Metrics Server** (for HPA in production):
   ```bash
   kubectl top nodes  # Should return metrics
   ```

---

## Directory Structure

```
deploy/helm/
├── service-chart/                      # Reusable Helm chart (DO NOT MODIFY per-service)
│   ├── Chart.yaml                      # Chart metadata
│   ├── values.yaml                     # Default values schema
│   └── templates/                      # Kubernetes manifest templates
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── configmap.yaml
│       ├── secret.yaml
│       ├── ingress.yaml
│       ├── hpa.yaml
│       ├── serviceaccount.yaml
│       └── postgresql/
│           ├── statefulset.yaml
│           ├── service.yaml
│           └── secret.yaml
│
├── services/                           # Per-service configurations
│   ├── account/
│   │   ├── values.yaml                 # Service identity (name, port, image)
│   │   └── environments/
│   │       ├── dev/                    # Local development
│   │       │   ├── app-values.yaml     # Application config
│   │       │   └── k8s-values.yaml     # Kubernetes resources
│   │       ├── staging/                # Staging environment
│   │       │   ├── app-values.yaml
│   │       │   └── k8s-values.yaml
│   │       └── prod/                   # Production environment
│   │           ├── app-values.yaml
│   │           └── k8s-values.yaml
│   ├── card/
│   │   └── (same structure)
│   └── loan/
│       └── (same structure)
│
├── deploy.sh                           # Deployment helper script
├── template.sh                         # Template rendering script
└── README.md                           # This file
```

---

## Local Development (Dev Environment)

The dev environment runs on your local Kubernetes (Docker Desktop, minikube, kind, etc.).

### Step 1: Start Local Kubernetes

```bash
# Docker Desktop: Enable Kubernetes in settings

# OR minikube
minikube start --memory=4096 --cpus=2

# OR kind
kind create cluster --name eazybank
```

### Step 2: Build Docker Images

```bash
# From repository root
cd /path/to/eazybank

# Build all services
./account/mvnw -f account/pom.xml jib:dockerBuild
./card/mvnw -f card/pom.xml jib:dockerBuild
./loan/mvnw -f loan/pom.xml jib:dockerBuild

# Verify images
docker images | grep eazybank
```

### Step 3: Configure Secrets

Edit the dev app-values.yaml files to set your database password:

```bash
# Replace placeholder with actual password
sed -i 's/__DB_PASSWORD__/your-dev-password/g' \
  deploy/helm/services/*/environments/dev/app-values.yaml
```

Or set it inline during deployment:

```bash
helm upgrade --install account ./deploy/helm/service-chart \
  --namespace eazybank-dev \
  --set app.datasource.password=your-dev-password \
  --set postgresql.credentials.password=your-dev-password \
  -f ./deploy/helm/services/account/values.yaml \
  -f ./deploy/helm/services/account/environments/dev/app-values.yaml \
  -f ./deploy/helm/services/account/environments/dev/k8s-values.yaml
```

### Step 4: Deploy Services

```bash
# Create namespace
kubectl create namespace eazybank-dev

# Deploy using helper script
./deploy/helm/deploy.sh account dev
./deploy/helm/deploy.sh card dev
./deploy/helm/deploy.sh loan dev

# OR deploy all at once
for service in account card loan; do
  ./deploy/helm/deploy.sh $service dev
done
```

### Step 5: Access Services

```bash
# Port-forward to access locally
kubectl port-forward svc/account 8080:8080 -n eazybank-dev &
kubectl port-forward svc/card 9000:9000 -n eazybank-dev &
kubectl port-forward svc/loan 8090:8090 -n eazybank-dev &

# Test endpoints
curl http://localhost:8080/account/actuator/health
curl http://localhost:9000/card/actuator/health
curl http://localhost:8090/loan/actuator/health
```

### Step 6: View Logs

```bash
# Follow logs for a service
kubectl logs -f deployment/account -n eazybank-dev

# Follow PostgreSQL logs
kubectl logs -f statefulset/account-postgresql -n eazybank-dev
```

### Cleanup Dev Environment

```bash
# Delete all releases
helm uninstall account card loan -n eazybank-dev

# Delete namespace (PVCs are retained)
kubectl delete namespace eazybank-dev

# To also delete PVCs (DATA LOSS)
kubectl delete pvc --all -n eazybank-dev
```

---

## Staging Environment

Staging mirrors production configuration with reduced resources.

### Prerequisites for Staging

1. **Kubernetes Cluster**: Dedicated staging cluster or namespace
2. **Ingress Controller**: Configured with staging domain
3. **DNS**: `staging.eazybank.local` pointing to ingress
4. **Docker Registry**: Images pushed to accessible registry

### Step 1: Configure Cluster Access

```bash
# Set kubectl context to staging cluster
kubectl config use-context staging-cluster

# Verify access
kubectl cluster-info
```

### Step 2: Create Namespace

```bash
kubectl create namespace eazybank-staging
```

### Step 3: Configure Secrets

**Option A: Direct replacement (not recommended for shared repos)**
```bash
sed -i 's/__DB_PASSWORD__/staging-secure-password/g' \
  deploy/helm/services/*/environments/staging/app-values.yaml
sed -i 's/__IMAGE_TAG__/v1.0.0/g' \
  deploy/helm/services/*/environments/staging/app-values.yaml
```

**Option B: Inline overrides (recommended)**
```bash
helm upgrade --install account ./deploy/helm/service-chart \
  --namespace eazybank-staging \
  --set image.tag=v1.0.0 \
  --set app.datasource.password=$DB_PASSWORD \
  --set postgresql.credentials.password=$DB_PASSWORD \
  -f ./deploy/helm/services/account/values.yaml \
  -f ./deploy/helm/services/account/environments/staging/app-values.yaml \
  -f ./deploy/helm/services/account/environments/staging/k8s-values.yaml
```

### Step 4: Deploy Services

```bash
# Set environment variables
export DB_PASSWORD="staging-secure-password"
export IMAGE_TAG="v1.0.0"

# Deploy each service
for service in account card loan; do
  helm upgrade --install $service ./deploy/helm/service-chart \
    --namespace eazybank-staging \
    --wait --timeout 5m \
    --set image.tag=$IMAGE_TAG \
    --set app.datasource.password=$DB_PASSWORD \
    --set postgresql.credentials.password=$DB_PASSWORD \
    -f ./deploy/helm/services/$service/values.yaml \
    -f ./deploy/helm/services/$service/environments/staging/app-values.yaml \
    -f ./deploy/helm/services/$service/environments/staging/k8s-values.yaml
done
```

### Step 5: Verify Deployment

```bash
# Check all resources
kubectl get all -n eazybank-staging

# Check ingress
kubectl get ingress -n eazybank-staging

# Test health endpoints (via ingress)
curl https://staging.eazybank.local/account/actuator/health
curl https://staging.eazybank.local/card/actuator/health
curl https://staging.eazybank.local/loan/actuator/health
```

---

## Production Environment

Production has higher resources, HPA enabled, and stricter configurations.

### Prerequisites for Production

1. **Kubernetes Cluster**: Production-grade cluster with:
   - Multiple nodes for high availability
   - Metrics Server for HPA
   - Proper RBAC configured

2. **Ingress Controller**: With TLS termination
3. **DNS**: `api.eazybank.com` pointing to ingress
4. **TLS Certificates**: Valid certificates for the domain
5. **Docker Registry**: Production images tagged with semver

### Step 1: Configure Cluster Access

```bash
# Set kubectl context to production cluster
kubectl config use-context production-cluster

# Verify access (be careful!)
kubectl cluster-info
```

### Step 2: Create Namespace

```bash
kubectl create namespace eazybank-prod
```

### Step 3: Configure TLS (if not using cert-manager)

```bash
# Create TLS secret from certificates
kubectl create secret tls eazybank-tls \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key \
  -n eazybank-prod
```

Update `k8s-values.yaml` for production:
```yaml
k8s:
  ingress:
    tls:
      - hosts:
          - api.eazybank.com
        secretName: eazybank-tls
```

### Step 4: Deploy Services

```bash
# Set environment variables (use secure method in practice)
export DB_PASSWORD="production-secure-password"
export IMAGE_TAG="v1.0.0"

# Deploy each service
for service in account card loan; do
  helm upgrade --install $service ./deploy/helm/service-chart \
    --namespace eazybank-prod \
    --wait --timeout 10m \
    --set image.tag=$IMAGE_TAG \
    --set app.datasource.password=$DB_PASSWORD \
    --set postgresql.credentials.password=$DB_PASSWORD \
    -f ./deploy/helm/services/$service/values.yaml \
    -f ./deploy/helm/services/$service/environments/prod/app-values.yaml \
    -f ./deploy/helm/services/$service/environments/prod/k8s-values.yaml
done
```

### Step 5: Verify Production Deployment

```bash
# Check deployment status
kubectl get deployments -n eazybank-prod

# Check HPA status
kubectl get hpa -n eazybank-prod

# Check pod distribution across nodes
kubectl get pods -n eazybank-prod -o wide

# Verify endpoints
curl https://api.eazybank.com/account/actuator/health
curl https://api.eazybank.com/card/actuator/health
curl https://api.eazybank.com/loan/actuator/health
```

---

## GitHub Actions CI/CD

### Overview

Single unified workflow handles the complete CI/CD pipeline:

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| [deploy.yml](.github/workflows/deploy.yml) | Push to main | **Build, test, and deploy all services** |

### Deployment Workflow

**Simple 4-step deployment:**

```
Push to main
    ↓
Build: Tests run & Docker images built
    ↓
Deploy to Staging: Automatic deployment
    ↓
Approval Gate: Manual approval (if production configured)
    ↓
Deploy to Production: Optional, requires approval
```

**Setup Required:**

1. Add GitHub Secrets (see Quick Start section above)
2. Push to main
3. Done

**Automatic on every push:**
```bash
git push origin main
# → Tests run for all 3 services
# → Docker images built
# → Deployed to eazybank-staging namespace
# → (Optional) Wait for approval for production
```

### GitHub Secrets

**For Staging Only:**

| Secret | Description |
|--------|-------------|
| `KUBE_CONFIG` | Base64-encoded kubeconfig |
| `DB_PASSWORD` | Staging database password |

Generate password:
```bash
openssl rand -base64 32
```

**For Staging + Production:**

| Secret | Description |
|--------|-------------|
| `KUBE_CONFIG` | Staging cluster kubeconfig |
| `DB_PASSWORD` | Staging database password |
| `KUBE_CONFIG_PROD` | Production cluster kubeconfig |
| `DB_PASSWORD_PROD` | Production database password |

See [.github/workflows/README.md](.github/workflows/README.md) for complete workflow details.

### Monitoring Deployments

**Real-time logs:**
1. Go to https://github.com/YOUR_USERNAME/eazybank/actions
2. Click the running workflow
3. Click "Deploy Services" job
4. Watch live logs

**After deployment:**
```bash
# SSH to Hetzner VM
ssh root@YOUR_SERVER_IP

# Check deployment status
kubectl get all -n eazybank-staging

# Follow logs
kubectl logs -f deployment/account -n eazybank-staging
```

### Troubleshooting CI/CD

**Error: "KUBE_CONFIG secret not configured"**
- Add `KUBE_CONFIG` secret to GitHub repository
- Ensure value is base64-encoded
- Test locally: `kubectl --kubeconfig configfile cluster-info`

**Error: "Connection refused"**
- Check kubeconfig has correct server IP (not 127.0.0.1)
- Verify Kubernetes cluster is accessible: `kubectl cluster-info`

**Error: Build or deployment fails**
- Check GitHub Actions logs: https://github.com/YOUR_USERNAME/eazybank/actions
- Click the failing workflow for detailed error messages

---

## Configuration Reference

### app-values.yaml Options

```yaml
image:
  tag: "v1.0.0"                          # Docker image tag

app:
  spring:
    profiles: staging                     # Spring profile (dev/staging/prod)
  datasource:
    username: postgres                    # Database username
    password: "__DB_PASSWORD__"           # Replaced by CI/CD
  support:
    contactName: "Support Team"           # Support contact name
    contactEmail: "support@example.com"   # Support email
    message: "Welcome message"            # Welcome message
  env:                                    # Additional environment variables
    FEATURE_FLAG_X: "true"

postgresql:
  credentials:
    username: postgres
    password: "__DB_PASSWORD__"           # Replaced by CI/CD
```

### k8s-values.yaml Options

```yaml
k8s:
  replicas: 2                             # Pod replicas (ignored if HPA enabled)

  resources:
    requests:
      memory: "512Mi"
      cpu: "200m"
    limits:
      memory: "1Gi"
      cpu: "500m"

  hpa:
    enabled: true                         # Enable Horizontal Pod Autoscaler
    minReplicas: 2
    maxReplicas: 10
    targetCPU: 70                         # Scale when CPU > 70%
    targetMemory: 80                      # Scale when Memory > 80%

  ingress:
    enabled: true
    className: "nginx"                    # Ingress class
    annotations:                          # Ingress controller annotations
      nginx.ingress.kubernetes.io/rewrite-target: /
    host: "api.eazybank.com"
    path: /account
    tls:
      - hosts:
          - api.eazybank.com
        secretName: eazybank-tls

  service:
    type: ClusterIP                       # Service type (ClusterIP/NodePort/LoadBalancer)
    nodePort: null                        # NodePort number (if type is NodePort)

  probes:
    liveness:
      enabled: true
      initialDelaySeconds: 45
      periodSeconds: 10
    readiness:
      enabled: true
      initialDelaySeconds: 15
      periodSeconds: 5

  nodeSelector: {}                        # Node selection constraints
  tolerations: []                         # Pod tolerations
  affinity: {}                            # Pod affinity rules

postgresql:
  storage: 5Gi                            # PVC storage size
  storageClassName: ""                    # Storage class (empty = default)
  resources:
    requests:
      memory: "512Mi"
      cpu: "200m"
    limits:
      memory: "1Gi"
      cpu: "500m"
```

---

## Troubleshooting

### Common Issues

#### 1. Pod stuck in Pending

```bash
# Check pod events
kubectl describe pod <pod-name> -n <namespace>

# Common causes:
# - Insufficient resources: Check node capacity
# - PVC not bound: Check storage class exists
# - Image pull error: Check image name and registry credentials
```

#### 2. Pod stuck in CrashLoopBackOff

```bash
# Check pod logs
kubectl logs <pod-name> -n <namespace> --previous

# Common causes:
# - Database connection failed: Check PostgreSQL is running
# - Wrong credentials: Verify secrets
# - Application error: Check Spring Boot logs
```

#### 3. Service not accessible via Ingress

```bash
# Check ingress status
kubectl describe ingress <ingress-name> -n <namespace>

# Check ingress controller logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller

# Common causes:
# - DNS not configured: Verify DNS records
# - Ingress class mismatch: Check ingressClassName
# - Backend service not ready: Check service endpoints
```

#### 4. HPA not scaling

```bash
# Check HPA status
kubectl describe hpa <hpa-name> -n <namespace>

# Check metrics server
kubectl top pods -n <namespace>

# Common causes:
# - Metrics server not installed
# - Resource requests not set on deployment
```

### Useful Commands

```bash
# View all resources for a service
kubectl get all -l app.kubernetes.io/name=account -n eazybank-staging

# View Helm release history
helm history account -n eazybank-staging

# Rollback to previous version
helm rollback account 1 -n eazybank-staging

# View rendered templates
./deploy/helm/template.sh account staging

# Debug Helm installation
helm upgrade --install account ./deploy/helm/service-chart \
  --namespace eazybank-staging \
  --debug --dry-run \
  -f ./deploy/helm/services/account/values.yaml \
  -f ./deploy/helm/services/account/environments/staging/app-values.yaml \
  -f ./deploy/helm/services/account/environments/staging/k8s-values.yaml
```

---

## Advanced: Using Ingress + Domain + HTTPS (Optional)

By default, services use **NodePort** - simple direct access via IP:PORT.

For custom domain + HTTPS, see: **[INGRESS_SETUP.md](../../INGRESS_SETUP.md)**

This sets up:
- **Free domain** (DuckDNS)
- **Free HTTPS** (Let's Encrypt via cert-manager)
- **Ingress routing** (NGINX controller)

All automatic, zero cost, auto-renewing certificates.

---

## Security Notes

1. **Never commit real passwords** - Use `__PLACEHOLDER__` syntax
2. **Use Kubernetes secrets** - Passwords are stored as K8s Secrets, not ConfigMaps
3. **PVC retention** - Database PVCs are retained even after `helm uninstall`
4. **RBAC** - Use minimal permissions for CI/CD service accounts
5. **Network policies** - Consider adding NetworkPolicies in production
