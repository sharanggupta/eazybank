# GitHub Actions CI/CD Pipelines

Automated deployment pipelines for EazyBank microservices.

## Pipeline Options

### Option 1: Full-Stack Deployment (All Services)

**File**: `deploy.yml`

Deploys all 4 services together in one workflow.

**When to use**:
- Initial setup
- Major platform changes
- Want to ensure all services are in sync
- Testing entire system

**Trigger**: Push to `main` (any service changes)

**Stages**:
1. Build all 4 services in parallel
2. Deploy all to staging (automatic)
3. Wait for approval
4. Deploy all to production (after approval)

### Option 2: Independent Service Deployments (Recommended)

**Files**:
- `deploy-account.yml` — Account service only
- `deploy-card.yml` — Card service only
- `deploy-loan.yml` — Loan service only
- `deploy-gateway.yml` — Gateway only

Deploy individual services independently without affecting others.

**When to use**:
- Bug fix in one service
- Feature development for specific service
- Faster iteration on single service
- Different release schedules per service

**Trigger**: Push to service-specific directories only
```
deploy-account.yml triggers when: account/** OR deploy/helm/services/account/**
deploy-card.yml triggers when: card/** OR deploy/helm/services/card/**
deploy-loan.yml triggers when: loan/** OR deploy/helm/services/loan/**
deploy-gateway.yml triggers when: gateway/** OR deploy/helm/services/gateway/**
```

Each service workflow:
1. Builds only the changed service
2. Deploys to staging (automatic)
3. Waits for approval
4. Deploys to production (after approval)

## Shared Workflow (`deploy-service.yml`)

**File**: `deploy-service.yml`

Reusable workflow called by all individual service workflows. Contains the actual build/deploy logic.
- Not meant to be triggered directly
- Called via `uses: ./.github/workflows/deploy-service.yml` from other workflows
- Handles smart semantic versioning, building, and deployment for any service

**Smart Versioning** (via `get-version.yml`):
- Automatically calculates version bumps based on commit messages
- `feat:` commits → MINOR version bump
- `fix:/refactor:/perf:` commits → PATCH version bump
- `BREAKING CHANGE:` → MAJOR version bump
- `docs:/test:/chore:/ci:` → Skip build (no version bump)
- Creates git tags after successful production deployment

📖 See [VERSIONING.md](../VERSIONING.md) for complete versioning guide.

---

## Workflow Comparison

| Feature | Full-Stack (`deploy.yml`) | Individual (`deploy-*.yml`) | Shared (`deploy-service.yml`) |
|---------|---------------------------|----------------------------|-------------------------------|
| **Trigger** | Any service changes | Service-specific changes | Called by other workflows |
| **Services** | All 4 at once | Single service | N/A (helper) |
| **Speed** | Slower (builds all) | Faster (single service) | N/A |
| **Risk** | Higher (affects all) | Lower (single service) | N/A |
| **Use Case** | Full platform updates | Quick bug fixes, iteration | Code reuse |

---

## Required Secrets

**For Staging Only**:
```
KUBE_CONFIG    # Base64-encoded kubeconfig file
DB_PASSWORD    # Database password for backend services
```

**For Staging + Production**:
```
KUBE_CONFIG           # Staging kubeconfig (base64)
DB_PASSWORD           # Staging database password
KUBE_CONFIG_PROD      # Production kubeconfig (base64)
DB_PASSWORD_PROD      # Production database password
```

## Examples

### Scenario 1: Fix Bug in Account Service

```bash
# Edit account service code
cd account/src/main/java
# ... fix a bug ...

# Commit and push
git add .
git commit -m "fix: resolve account validation issue"
git push origin main

# Result:
# ✓ Only deploy-account.yml triggers
# ✓ Builds account image
# ✓ Deploys to staging
# ✓ Waits for production approval
# Other services unaffected and not redeployed
```

### Scenario 2: Add Feature to Card Service

```bash
# Edit card service
cd card/src/main/java
# ... add new endpoint ...

# Also update Helm config
cd deploy/helm/services/card/environments/staging
# ... update values ...

# Push
git add .
git commit -m "feat: add new card management endpoint"
git push

# Result:
# ✓ Only deploy-card.yml triggers
# ✓ Builds and deploys card service
# Can approve for production independently
# Other services on their own schedules
```

### Scenario 3: Platform-Wide Update (All Services)

```bash
# Update shared dependency or configuration
cd deploy/helm/service-chart
# ... update Helm chart ...

# Push (doesn't trigger individual workflows)
git add .
git commit -m "chore: update Helm chart for all services"
git push

# Manually trigger full-stack deployment:
# Go to GitHub → Actions → Build and Deploy → Run workflow
# Select 'main' branch and click "Run workflow"

# Result:
# ✓ All 4 services built and deployed together
# ✓ Ensures platform consistency
```

### Scenario 4: Manual Single-Service Deploy

Even without code changes, you can manually redeploy a service:

```bash
# Go to GitHub → Actions → Deploy Account Service
# Click "Run workflow" → select 'main' → Run

# Result:
# ✓ Rebuilds account service from current main
# ✓ Deploys to staging automatically
# ✓ Waits for approval to deploy to production
```

---

## Setup Instructions

See [README.md](../../README.md) for complete setup guide.

## Best Practices

### 1. Use Individual Workflows for Development

- Makes deployments faster (only changed service builds)
- Reduces risk (only one service affected)
- Enables parallel development (team members work on different services)
- Cleaner deployment history

### 2. Use Full-Stack Workflow for Major Changes

- Shared infrastructure updates (Helm chart, database schema)
- Cross-service changes (API contract changes)
- Release milestones
- Initial production setup

### 3. Version Services Independently

Each service automatically gets its own version based on commit messages:
- Versions are calculated by `get-version.yml` workflow
- Git tags track versions: `v-account-1.2.3`, `v-card-2.1.0`
- Docker images tagged with calculated version
- See [VERSIONING.md](../VERSIONING.md) for how versions are calculated

Services naturally maintain different versions simultaneously based on their commit history.

### 4. Test Before Merging

All workflows test code before building images:
```bash
# Locally, before push:
cd account && ./mvnw clean test
cd card && ./mvnw clean test
cd loan && ./mvnw clean test
cd gateway && ./mvnw clean test
```

### 5. Review Staging Before Production

Always verify staging deployment before approving production:
```bash
kubectl logs -f deployment/account -n eazybank-staging
curl http://CLUSTER_IP:NODEPORT/swagger-ui.html  # Test gateway
```

---

## Monitoring & Troubleshooting

### Watch Live Workflow

https://github.com/sharanggupta/eazybank/actions

Click on the running workflow to see:
- Build logs
- Deployment steps
- Error messages
- Summary

### Check Deployment Status

```bash
# View all pods
kubectl get pods -n eazybank-staging

# View services
kubectl get svc -n eazybank-staging

# View a specific service's logs
kubectl logs -f deployment/account -n eazybank-staging
kubectl logs -f deployment/card -n eazybank-staging
kubectl logs -f deployment/loan -n eazybank-staging
kubectl logs -f deployment/gateway -n eazybank-staging
```

### Access Services

**Gateway** (externally exposed via NodePort):
```bash
# Get NodePort
NODEPORT=$(kubectl get svc gateway -n eazybank-staging -o jsonpath='{.spec.ports[0].nodePort}')

# Access Swagger UI
# http://CLUSTER_IP:$NODEPORT/swagger-ui.html

# Test health
curl http://CLUSTER_IP:$NODEPORT/actuator/health
```

**Backend Services** (only via ClusterIP, use port-forward for debugging):
```bash
# Forward account service
kubectl port-forward svc/account 8080:8080 -n eazybank-staging &
curl http://localhost:8080/account/actuator/health

# Forward card service
kubectl port-forward svc/card 9000:9000 -n eazybank-staging &
curl http://localhost:9000/card/actuator/health

# Forward loan service
kubectl port-forward svc/loan 8090:8090 -n eazybank-staging &
curl http://localhost:8090/loan/actuator/health
```

### Troubleshooting

**Workflow won't trigger**:
- Check that files match path filter in workflow YAML
- Documentation-only changes are ignored (add non-doc file)
- Verify branch is `main`

**Build fails**:
- Check build logs in GitHub Actions
- Run locally: `cd {service} && ./mvnw clean test`
- Ensure Java 25 installed locally

**Deployment fails**:
- Verify KUBE_CONFIG secret is set correctly (base64-encoded)
- Check cluster connectivity: `kubectl cluster-info`
- Review Helm syntax in service values files

**Production approval stuck**:
- Go to GitHub UI → Environments → production → Required reviewers
- Ensure reviewers have dismissed any prior deployment reviews
- Manually trigger production deployment from Actions UI

---

## Secrets Configuration

Required GitHub secrets for CI/CD:

```
KUBE_CONFIG           # Base64-encoded kubeconfig for staging
KUBE_CONFIG_PROD      # Base64-encoded kubeconfig for production
DB_PASSWORD           # Database password for staging
DB_PASSWORD_PROD      # Database password for production
```

To generate base64-encoded kubeconfig:
```bash
cat ~/.kube/config | base64 -w 0
```

Set in GitHub UI: Settings → Secrets and variables → Actions
