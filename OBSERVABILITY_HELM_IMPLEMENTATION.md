# Observability Helm Chart Implementation Guide

## Overview

This guide explains how to integrate the new observability Helm chart into your CI/CD pipeline and Kubernetes deployments.

The observability stack includes:
- **Prometheus**: Metrics collection and storage
- **Grafana**: Metrics visualization with NodePort/Ingress access
- **Loki**: Log aggregation
- **Tempo**: Distributed trace storage
- **OpenTelemetry Collector**: Trace and metrics processing
- **Grafana Alloy**: Kubernetes-native log collection agent

---

## Files Created

### Helm Chart Structure
```
deploy/helm/observability-chart/
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── _helpers.tpl
│   ├── namespace.yaml
│   ├── prometheus-*.yaml (4 files)
│   ├── grafana-*.yaml (6 files)
│   ├── loki-*.yaml (3 files)
│   ├── tempo-*.yaml (3 files)
│   ├── otel-collector-*.yaml (4 files)
│   └── alloy-*.yaml (3 files)
└── environments/
    ├── dev/values.yaml
    ├── staging/values.yaml
    └── prod/values.yaml
```

### Total: 30 new files

---

## Phase 1: Update Service Chart for Observability Configuration

### 1.1 Update Base Values Schema

**File:** `deploy/helm/service-chart/values.yaml`

Add observability section to schema:

```yaml
# Add this to the app section
app:
  # ... existing config ...
  observability:
    enabled: true
    samplingRate: "1.0"
    otlpEndpoint: "http://otel-collector:4318/v1/traces"
  env: {}
```

### 1.2 Update Service Deployment Template

**File:** `deploy/helm/service-chart/templates/deployment.yaml`

Modify the ConfigMap mounting to pass observability environment variables:

```yaml
# In the containers[0].envFrom section, add:
env:
  - name: OTEL_EXPORTER_OTLP_ENDPOINT
    value: "{{ .Values.app.observability.otlpEndpoint }}"
  - name: MANAGEMENT_TRACING_SAMPLING_PROBABILITY
    value: "{{ .Values.app.observability.samplingRate }}"
```

---

## Phase 2: Update Environment-Specific Service Configurations

### 2.1 Update Dev Environment App Values

**File:** `deploy/helm/services/account/environments/dev/app-values.yaml`

Replace hardcoded tracing endpoint with parameterization:

```yaml
# BEFORE
app:
  env:
    MANAGEMENT_OTLP_TRACING_ENDPOINT: "http://otel-collector:4318/v1/traces"

# AFTER
app:
  observability:
    enabled: true
    samplingRate: "1.0"
    otlpEndpoint: "http://otel-collector:4318/v1/traces"
```

**Apply this change to all services:**
- `account/environments/dev/app-values.yaml`
- `card/environments/dev/app-values.yaml`
- `loan/environments/dev/app-values.yaml`
- `customer-gateway/environments/dev/app-values.yaml`

### 2.2 Update Staging Environment App Values

**File:** `deploy/helm/services/account/environments/staging/app-values.yaml`

```yaml
app:
  observability:
    enabled: true
    samplingRate: "0.1"  # 10% sampling
    otlpEndpoint: "http://otel-collector.otel:4318/v1/traces"
```

**Apply to all services in staging**

### 2.3 Update Production Environment App Values

**File:** `deploy/helm/services/account/environments/prod/app-values.yaml`

```yaml
app:
  observability:
    enabled: true
    samplingRate: "0.01"  # 1% sampling
    otlpEndpoint: "http://otel-collector.otel:4318/v1/traces"
```

**Apply to all services in production**

---

## Phase 3: GitHub Actions Workflow Integration

### 3.1 Add GitHub Secrets

**Location:** GitHub Repository Settings → Secrets and variables → Actions

Add these secrets:

```
# Observability endpoints
OTEL_ENDPOINT_DEV: http://otel-collector:4318/v1/traces
OTEL_ENDPOINT_STAGING: http://otel-collector.otel:4318/v1/traces
OTEL_ENDPOINT_PROD: http://otel-collector.otel:4318/v1/traces

# Grafana passwords (optional - can use default)
GRAFANA_PASSWORD_STAGING: <random-password>
GRAFANA_PASSWORD_PROD: <random-password>
```

### 3.2 Update Deploy Service Workflow

**File:** `.github/workflows/deploy-service.yml`

Modify the helm deployment step to inject OTEL endpoint:

```yaml
# Line 242-251: Deploy to staging
- name: Deploy ${{ inputs.service }} to staging
  run: |
    helm upgrade --install ${{ inputs.service }} ./deploy/helm/service-chart \
      --namespace eazybank-staging \
      --wait --timeout 5m \
      --set image.repository=${{ inputs.registry }}/${{ inputs.image_prefix }}/${{ inputs.service }} \
      --set image.tag=${{ needs.build.outputs.image_tag }} \
      --set app.datasource.password="${{ secrets.db_password }}" \
      --set postgresql.credentials.password="${{ secrets.db_password }}" \
      --set app.observability.otlpEndpoint="${{ secrets.OTEL_ENDPOINT_STAGING }}" \
      -f ./deploy/helm/services/${{ inputs.service }}/values.yaml \
      -f ./deploy/helm/services/${{ inputs.service }}/environments/staging/app-values.yaml \
      -f ./deploy/helm/services/${{ inputs.service }}/environments/staging/k8s-values.yaml
```

**Apply same change to production deployment (line 328-339)**

### 3.3 Add Observability Stack Deployment to Main Workflow

**File:** `.github/workflows/deploy.yml`

Add a new job before service deployments:

```yaml
jobs:
  # =================================================================
  # Deploy Observability Stack (new job)
  # =================================================================
  deploy-observability:
    name: Deploy Observability Stack
    runs-on: ubuntu-latest
    strategy:
      matrix:
        environment: [staging, prod]
    environment:
      name: ${{ matrix.environment }}

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Helm
        uses: azure/setup-helm@v4
        with:
          version: 'v3.14.0'

      - name: Set up kubectl
        uses: azure/setup-kubectl@v4

      - name: Configure kubeconfig
        run: |
          mkdir -p ~/.kube
          if [ "${{ matrix.environment }}" == "staging" ]; then
            echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > ~/.kube/config
          else
            echo "${{ secrets.KUBE_CONFIG_PROD }}" | base64 -d > ~/.kube/config
          fi
          chmod 600 ~/.kube/config

      - name: Create otel namespace
        run: |
          kubectl create namespace otel --dry-run=client -o yaml | kubectl apply -f -

      - name: Deploy observability stack
        run: |
          helm upgrade --install observability ./deploy/helm/observability-chart \
            --namespace otel \
            --wait --timeout 10m \
            -f ./deploy/helm/observability-chart/values.yaml \
            -f ./deploy/helm/observability-chart/environments/${{ matrix.environment }}/values.yaml \
            --set grafana.admin.password="${{ secrets.GRAFANA_PASSWORD_${{ matrix.environment }} }}"

      - name: Verify observability stack
        run: |
          kubectl get pods -n otel
          kubectl get svc -n otel

  # =================================================================
  # Deploy Services (existing jobs) - add dependency
  # =================================================================
  build-account:
    needs: [deploy-observability]
    # ... rest of workflow
```

---

## Phase 4: Detailed Implementation Steps

### Step 1: Helm Values Schema Update (Low Risk)

```bash
# 1. Backup current values
cp deploy/helm/service-chart/values.yaml deploy/helm/service-chart/values.yaml.bak

# 2. Add observability section to values.yaml
# Edit file to add:
#
# app:
#   observability:
#     enabled: true
#     samplingRate: "1.0"
#     otlpEndpoint: "http://otel-collector:4318/v1/traces"
```

### Step 2: Update Deployment Template (Medium Risk)

```bash
# 1. Add environment variables to deployment.yaml
# Modify deployment.yaml to inject OTEL environment variables
# This requires editing the containers[0].env section

# 2. Test locally with different values
helm template account ./deploy/helm/service-chart \
  -f deploy/helm/services/account/values.yaml \
  -f deploy/helm/services/account/environments/dev/app-values.yaml \
  | grep -A 5 "OTEL_EXPORTER"
```

### Step 3: Update Service Configurations (Low Risk)

```bash
# For each service and environment:
# 1. Update app-values.yaml to use observability block
# 2. Verify helm template generates correct values
# 3. Commit in logical groupings

# Example:
helm template account ./deploy/helm/service-chart \
  -f deploy/helm/services/account/values.yaml \
  -f deploy/helm/services/account/environments/dev/app-values.yaml \
  | grep -i "observability"
```

### Step 4: Test Helm Chart Locally (Medium Risk)

```bash
# Validate chart syntax
helm lint deploy/helm/observability-chart

# Test template generation
helm template observability deploy/helm/observability-chart \
  -f deploy/helm/observability-chart/environments/dev/values.yaml

# Dry-run install (doesn't actually deploy)
helm install observability deploy/helm/observability-chart \
  -f deploy/helm/observability-chart/environments/dev/values.yaml \
  --dry-run \
  --debug
```

### Step 5: GitHub Secrets Configuration (Medium Risk)

```bash
# Via GitHub CLI:
gh secret set OTEL_ENDPOINT_STAGING --body "http://otel-collector.otel:4318/v1/traces"
gh secret set OTEL_ENDPOINT_PROD --body "http://otel-collector.otel:4318/v1/traces"

# Or via GitHub UI: Settings → Secrets and variables → Actions
```

### Step 6: Update GitHub Workflows (High Risk - requires testing)

```bash
# 1. Create a test branch
git checkout -b feature/observability-helm

# 2. Update deploy-service.yml and deploy.yml
# 3. Commit changes
# 4. Create PR for review
# 5. Run workflow on staging first
```

### Step 7: Deployment Validation

```bash
# After workflow runs:

# Check observability stack deployed
kubectl get pods -n otel
kubectl get svc -n otel

# Check services deployed
kubectl get pods -n eazybank-staging

# Verify traces flowing
kubectl logs -n otel -l app=otel-collector -f

# Access Grafana
kubectl port-forward -n otel svc/grafana 3000:3000
# Visit http://localhost:3000
# Login: admin / <password>

# Verify metrics collection
kubectl port-forward -n otel svc/prometheus 9090:9090
# Visit http://localhost:9090
# Check targets: Status → Targets (should show eazybank services UP)
```

---

## Configuration Reference

### Observability Endpoints by Environment

| Environment | Endpoint | Description |
|-------------|----------|-------------|
| **Dev (Docker)** | `http://otel-collector:4318/v1/traces` | Local container network |
| **Dev (K8s)** | `http://otel-collector:4318/v1/traces` | Same namespace as services |
| **Staging** | `http://otel-collector.otel:4318/v1/traces` | Different namespace |
| **Prod** | `http://otel-collector.otel:4318/v1/traces` | Shared observability namespace |

### Grafana Access

| Environment | URL | Type |
|-------------|-----|------|
| **Dev** | `http://localhost:3000` | Docker port-forward |
| **Dev (K8s)** | `http://<node-ip>:30030` | NodePort |
| **Staging** | `http://<node-ip>:30030` | NodePort |
| **Prod** | `https://grafana.eazybank.com` | Ingress |

### Default Credentials

| Component | Username | Password | Location |
|-----------|----------|----------|----------|
| **Grafana** | admin | *environment-specific* | `values.yaml` |

---

## Troubleshooting

### Services Not Collecting Traces

**Symptom:** No traces in Tempo UI

**Diagnosis:**
```bash
# Check if services can reach OTel Collector
kubectl exec -n eazybank-staging deployment/account -- \
  curl -v http://otel-collector.otel:4318/v1/traces

# Check OTel Collector logs
kubectl logs -n otel -l app=otel-collector -f

# Check OTEL_EXPORTER_OTLP_ENDPOINT environment variable
kubectl exec -n eazybank-staging deployment/account -- \
  env | grep OTEL
```

**Solution:**
- Verify OTel Collector is running: `kubectl get pods -n otel`
- Check service DNS: `kubectl get svc -n otel`
- Verify endpoint in app-values.yaml matches service name

### Grafana Can't Connect to Datasources

**Symptom:** Datasource test fails in Grafana

**Diagnosis:**
```bash
# Check if Prometheus is running
kubectl get pods -n otel -l app=prometheus

# Check service endpoints
kubectl get endpoints -n otel

# Port-forward and test directly
kubectl port-forward -n otel svc/prometheus 9090:9090
curl http://localhost:9090/api/v1/query
```

**Solution:**
- Ensure Prometheus StatefulSet has replicas running
- Check Prometheus scrape config: `kubectl get cm -n otel prometheus-config`
- Verify service endpoints are populated

### Alloy Not Collecting Logs

**Symptom:** Empty logs in Loki

**Diagnosis:**
```bash
# Check Alloy DaemonSet is running
kubectl get ds -n otel -l app=alloy

# Check Alloy logs for errors
kubectl logs -n otel -l app=alloy -f

# Verify Docker socket is accessible
kubectl exec -n otel -it <alloy-pod> -- \
  ls -la /var/run/docker.sock

# Check Alloy config
kubectl get cm -n otel alloy-config -o yaml
```

**Solution:**
- Ensure Alloy DaemonSet pods are running on all nodes
- Check Docker socket mount on host
- Verify log discovery filter in Alloy config matches service names

---

## Rollback Procedure

If observability deployment causes issues:

```bash
# Remove observability stack
helm uninstall observability -n otel

# Services will continue running without observability
# Re-deploy once issues are resolved

# To restore:
helm install observability ./deploy/helm/observability-chart \
  -n otel \
  -f deploy/helm/observability-chart/environments/staging/values.yaml
```

---

## Next Steps

1. **Create Feature Branch:** `git checkout -b feature/observability-helm`
2. **Implement Phase 1:** Update Helm values schema
3. **Implement Phase 2:** Update environment configurations
4. **Test Locally:** `helm template` and dry-run
5. **Update Workflows:** Add secrets and update CI/CD
6. **Test in Staging:** Run full deployment workflow
7. **Document:** Update team runbooks
8. **Deploy to Production:** After staging validation

---

## Summary

The observability Helm chart provides:

✅ Complete observability stack deployment
✅ Environment-specific configurations (dev/staging/prod)
✅ Grafana NodePort access in dev/staging
✅ Grafana Ingress for production
✅ Trace correlation across services
✅ Log aggregation from all pods
✅ Metrics collection and visualization
✅ Scalable storage per environment

All configured as production-ready Helm templates with proper RBAC, storage, and resource limits.

