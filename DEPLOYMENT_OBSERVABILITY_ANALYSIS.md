# Comprehensive Deployment Observability Analysis

## Executive Summary

Your CI/CD pipeline and Helm charts are **well-architected for microservice deployment** with smart versioning and environment progression (dev → staging → prod). However, **observability infrastructure components (Prometheus, Grafana, Loki, Tempo, OTel Collector, Alloy) are completely absent from the Kubernetes deployment strategy**.

While these components work perfectly in local Docker Compose (`deploy/dev/docker-compose.yml`), they won't be deployed to Kubernetes clusters. Additionally, **environment variable handling for observability is inconsistent** between local development and Kubernetes deployment.

---

## Critical Gaps Identified

### Gap 1: No Observability Helm Chart

**Current State:**
- Observability stack only defined in `deploy/dev/docker-compose.yml`
- Services: Prometheus, Grafana, Loki, Tempo, OpenTelemetry Collector, Grafana Alloy
- No Kubernetes manifests or Helm templates exist

**Impact:**
- Staging environment has no observability infrastructure
- Production environment has no observability infrastructure
- Cannot collect metrics, traces, or logs from Kubernetes deployments
- No visibility into service behavior post-deployment

**Required Components:**
```
Prometheus (metrics scraping & storage)
├── StatefulSet for data persistence
├── PersistentVolume for time-series data
├── Service (ClusterIP for internal access)
└── ConfigMap (prometheus.yml scrape configs)

Grafana (visualization & dashboards)
├── Deployment
├── Service (NodePort for UI access)
├── ConfigMap (datasource provisioning)
├── PersistentVolume (dashboard storage)
└── Secret (admin credentials)

Loki (log aggregation)
├── StatefulSet for log storage
├── PersistentVolume for indexes
├── Service (ClusterIP for internal access)
└── ConfigMap (loki-config.yaml)

Tempo (trace storage)
├── StatefulSet for trace storage
├── PersistentVolume for trace data
├── Service (OTLP receiver on port 4318)
└── ConfigMap (tempo-config.yaml)

OpenTelemetry Collector (trace processing)
├── Deployment (stateless)
├── Service (OTLP receiver on 4318, Prometheus exporter on 8888)
└── ConfigMap (otel-collector-config.yaml)

Grafana Alloy (log collection agent)
├── DaemonSet (runs on every node)
├── ConfigMap (alloy-config.alloy)
└── RBAC (permissions to read pod logs)
```

---

### Gap 2: Inconsistent Environment Variable Handling

**Current State - Docker Compose (Dev):**
```yaml
# deploy/dev/docker-compose.yml
services:
  account:
    environment:
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318/v1/traces
  # Plus hardcoded in app-values.yaml:
  # MANAGEMENT_OTLP_TRACING_ENDPOINT: "http://otel-collector:4318/v1/traces"
```

**Current State - Kubernetes (Staging/Prod):**
```yaml
# deploy/helm/services/account/environments/dev/app-values.yaml
app:
  env:
    MANAGEMENT_OTLP_TRACING_ENDPOINT: "http://otel-collector:4318/v1/traces"
```

**Problems:**
1. ❌ `MANAGEMENT_OTLP_TRACING_ENDPOINT` is **hardcoded** in all environments
2. ❌ Assumes `otel-collector` service exists in Kubernetes (it won't without observability chart)
3. ❌ No fallback endpoint for when observability is disabled
4. ❌ Different environment names cause confusion:
   - Docker: `OTEL_EXPORTER_OTLP_ENDPOINT`
   - Kubernetes: `MANAGEMENT_OTLP_TRACING_ENDPOINT`
5. ❌ No way to disable observability if infrastructure isn't deployed

**Required Solution:**
```yaml
# Unified naming convention
OTEL_EXPORTER_OTLP_ENDPOINT: "${OTEL_ENDPOINT:http://otel-collector:4318/v1/traces}"

# Configurable per environment
dev:
  OTEL_ENDPOINT: http://otel-collector:4318/v1/traces
staging:
  OTEL_ENDPOINT: http://otel-collector.otel:4318/v1/traces  # Different namespace
prod:
  OTEL_ENDPOINT: http://otel-collector.otel:4318/v1/traces
  # OR external managed service
  OTEL_ENDPOINT: https://otlp.observability-company.com/v1/traces
```

---

### Gap 3: Missing Environment Variable Propagation

**Current Deployment Process:**

Workflow file (`deploy-service.yml:240-251`):
```yaml
helm upgrade --install ${{ inputs.service }} ./deploy/helm/service-chart \
  --set image.repository=${{ inputs.registry }}/... \
  --set image.tag=${{ needs.build.outputs.image_tag }} \
  --set app.datasource.password="${{ secrets.db_password }}" \
  --set postgresql.credentials.password="${{ secrets.db_password }}" \
  -f ./deploy/helm/services/${{ inputs.service }}/values.yaml \
  -f ./deploy/helm/services/${{ inputs.service }}/environments/staging/app-values.yaml \
  -f ./deploy/helm/services/${{ inputs.service }}/environments/staging/k8s-values.yaml
```

**Gap:**
- ✅ Database passwords are passed as `--set` flags (secure)
- ❌ **Observability environment variables are NOT passed as `--set` flags**
- ❌ They're hardcoded in app-values.yaml files
- ❌ No mechanism to override observability endpoint at deployment time

**Solution:**
```yaml
helm upgrade --install ... \
  --set image.tag=${{ needs.build.outputs.image_tag }} \
  --set app.datasource.password=${{ secrets.db_password }} \
  --set postgresql.credentials.password=${{ secrets.db_password }} \
  --set app.env.OTEL_EXPORTER_OTLP_ENDPOINT="${{ secrets.otel_endpoint }}" \
  -f ./deploy/helm/services/${{ inputs.service }}/values.yaml \
  ...
```

**Additional Secrets Needed:**
```yaml
# GitHub repository secrets
OTEL_ENDPOINT_DEV: http://otel-collector:4318/v1/traces
OTEL_ENDPOINT_STAGING: http://otel-collector.otel:4318/v1/traces
OTEL_ENDPOINT_PROD: https://managed-otel.company.com/v1/traces
```

---

### Gap 4: No Grafana UI Exposure

**Current State:**
- Grafana only accessible via `docker-compose up` locally
- No service definition for Kubernetes
- No NodePort/LoadBalancer service for external access
- No documented URL for accessing dashboards

**Impact:**
- Staging team cannot view dashboards
- Production observability team has no UI access
- Cannot demonstrate observability to stakeholders

**Solution:**
```yaml
# Create Grafana service with NodePort exposure
apiVersion: v1
kind: Service
metadata:
  name: grafana
spec:
  type: NodePort
  selector:
    app: grafana
  ports:
    - port: 3000
      targetPort: 3000
      nodePort: 30030  # External access via worker-node-ip:30030
```

**Access Pattern:**
```
Dev (local): http://localhost:3000
Staging: http://<staging-node-ip>:30030
Production: http://<prod-node-ip>:30030 OR https://grafana.eazybank.com (via Ingress)
```

---

### Gap 5: Inconsistent ConfigMap Template Usage

**Current Deployment Template** (`deployment.yaml:56-62`):
```yaml
envFrom:
  - configMapRef:
      name: {{ include "eazybank-service.fullname" . }}
  {{- if or .Values.app.datasource.url .Values.postgresql.enabled }}
  - secretRef:
      name: {{ include "eazybank-service.fullname" . }}
  {{- end }}
```

**Issue:**
- All environment variables from `app-values.yaml` are loaded via ConfigMap
- ConfigMap immutability not enforced
- No validation that required variables exist
- No documentation of expected environment variables

**Best Practice:**
```yaml
# Separate variables by source/concern
envFrom:
  - configMapRef:
      name: {{ include "eazybank-service.fullname" . }}-config
  - configMapRef:
      name: {{ include "eazybank-service.fullname" . }}-observability
  {{- if or .Values.app.datasource.url .Values.postgresql.enabled }}
  - secretRef:
      name: {{ include "eazybank-service.fullname" . }}-db
  {{- end }}
```

---

### Gap 6: Missing Observability Configuration Documentation

**Current State:**
- Observability configuration scattered across:
  - `.github/workflows/*.yml` (no documentation of environment variable handling)
  - `deploy/helm/services/*/environments/*/app-values.yaml` (hardcoded values)
  - `account/src/main/resources/application.yml` (Spring Boot config)
  - `CLAUDE.md` (project context, not deployment docs)

**Missing Documentation:**
- [ ] How observability endpoints are configured per environment
- [ ] What observability components are deployed in each environment
- [ ] How to troubleshoot observability failures
- [ ] Grafana access URLs and default credentials
- [ ] Metrics/traces/logs retention policies per environment

---

## Cross-Environment Configuration Comparison

### Current State

| Component | Dev (Docker) | Dev (K8s) | Staging (K8s) | Prod (K8s) |
|-----------|-------------|-----------|---------------|-----------|
| **Prometheus** | ✅ Deployed | ❌ Missing | ❌ Missing | ❌ Missing |
| **Grafana** | ✅ Deployed | ❌ Missing | ❌ Missing | ❌ Missing |
| **Loki** | ✅ Deployed | ❌ Missing | ❌ Missing | ❌ Missing |
| **Tempo** | ✅ Deployed | ❌ Missing | ❌ Missing | ❌ Missing |
| **OTel Collector** | ✅ Deployed | ❌ Missing | ❌ Missing | ❌ Missing |
| **Alloy** | ✅ Deployed | ❌ Missing | ❌ Missing | ❌ Missing |
| **OTEL Endpoint** | `http://otel-collector:4318` | hardcoded | hardcoded | hardcoded |
| **Trace Sampling** | 100% | 100% | 10% (in code) | 10% (in code) |
| **Storage Retention** | 24 hours | None | None | None |
| **Dashboard Access** | http://localhost:3000 | None | None | None |

### Desired State (After Implementation)

| Component | Dev (Docker) | Dev (K8s) | Staging (K8s) | Prod (K8s) |
|-----------|-------------|-----------|---------------|-----------|
| **Prometheus** | ✅ | ✅ 2Gi | ✅ 10Gi | ✅ 50Gi |
| **Grafana** | ✅ | ✅ NodePort:3000 | ✅ NodePort:3000 | ✅ Ingress |
| **Loki** | ✅ | ✅ 5Gi | ✅ 20Gi | ✅ 100Gi |
| **Tempo** | ✅ | ✅ 5Gi | ✅ 20Gi | ✅ 50Gi |
| **OTel Collector** | ✅ | ✅ StatelessSet | ✅ StatelessSet | ✅ StatelessSet |
| **Alloy** | ✅ | ✅ DaemonSet | ✅ DaemonSet | ✅ DaemonSet |
| **OTEL Endpoint** | Local | Env-injected | Env-injected | Env-injected |
| **Trace Sampling** | 100% | 100% | 10% | 1% |
| **Storage Retention** | 24h | 7d | 14d | 30d |
| **Dashboard Access** | http://localhost:3000 | http://node:30030 | http://node:30030 | https://grafana.eazybank.com |

---

## Implementation Roadmap

### Phase 1: Environment Configuration Refactoring (Low Risk)
**Files to Modify:**
- `deploy/helm/service-chart/values.yaml` - Add observability config schema
- `deploy-service.yml` - Add OTEL endpoint secret parameter
- All `app-values.yaml` files - Use environment variables

**Changes:**
```yaml
# Before
app:
  env:
    MANAGEMENT_OTLP_TRACING_ENDPOINT: "http://otel-collector:4318/v1/traces"

# After
app:
  observability:
    enabled: true
    otlpEndpoint: "{{ .Values.observability.otlpEndpoint }}"
  env:
    OTEL_EXPORTER_OTLP_ENDPOINT: "{{ .Values.observability.otlpEndpoint }}"
    MANAGEMENT_TRACING_SAMPLING_PROBABILITY: "{{ .Values.observability.sampling }}"
```

### Phase 2: Observability Helm Chart Creation (Medium Risk)
**New Files:**
- `deploy/helm/observability-chart/Chart.yaml`
- `deploy/helm/observability-chart/values.yaml`
- `deploy/helm/observability-chart/templates/prometheus-*`
- `deploy/helm/observability-chart/templates/grafana-*`
- `deploy/helm/observability-chart/templates/loki-*`
- `deploy/helm/observability-chart/templates/tempo-*`
- `deploy/helm/observability-chart/templates/otel-collector-*`
- `deploy/helm/observability-chart/templates/alloy-*`

**Per-Environment Values:**
- `deploy/helm/observability-chart/environments/dev/values.yaml`
- `deploy/helm/observability-chart/environments/staging/values.yaml`
- `deploy/helm/observability-chart/environments/prod/values.yaml`

### Phase 3: Deployment Workflow Integration (Medium Risk)
**Modified Files:**
- `.github/workflows/deploy.yml` - Add observability stack deployment
- `.github/workflows/deploy-service.yml` - Pass OTEL endpoint to helm
- Add secrets: `OTEL_ENDPOINT_DEV`, `OTEL_ENDPOINT_STAGING`, `OTEL_ENDPOINT_PROD`

**Deployment Order:**
1. Deploy observability stack (if not exists)
2. Wait for OTel Collector readiness
3. Deploy services with OTEL_ENDPOINT_* secret
4. Verify trace collection

### Phase 4: Documentation (Low Risk)
**New Files:**
- `docs/observability-deployment.md` - Kubernetes deployment guide
- `docs/observability-troubleshooting.md` - Common issues
- `docs/grafana-access.md` - Dashboard access per environment

---

## Specific Implementation Requirements

### 1. Grafana NodePort Service

```yaml
# deploy/helm/observability-chart/templates/grafana-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: grafana
spec:
  type: NodePort
  ports:
    - port: 3000
      targetPort: 3000
      nodePort: 30030
      name: ui
  selector:
    app: grafana
```

**Access:**
- Dev/Staging: `http://<node-ip>:30030`
- Production (with Ingress):
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: grafana
spec:
  ingressClassName: nginx
  rules:
  - host: grafana.eazybank.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: grafana
            port:
              number: 3000
```

### 2. Unified Environment Variable Configuration

**Normalize all observability endpoints:**

```bash
# Deploy-service.yml changes
if [ "$ENVIRONMENT" == "dev" ]; then
  OTEL_ENDPOINT="${{ secrets.OTEL_ENDPOINT_DEV }}"
elif [ "$ENVIRONMENT" == "staging" ]; then
  OTEL_ENDPOINT="${{ secrets.OTEL_ENDPOINT_STAGING }}"
else
  OTEL_ENDPOINT="${{ secrets.OTEL_ENDPOINT_PROD }}"
fi

helm upgrade --install ... \
  --set app.observability.otlpEndpoint="$OTEL_ENDPOINT" \
  --set observability.enabled=true
```

**GitHub Secrets Required:**
```
OTEL_ENDPOINT_DEV: http://otel-collector:4318/v1/traces
OTEL_ENDPOINT_STAGING: http://otel-collector.otel:4318/v1/traces
OTEL_ENDPOINT_PROD: http://otel-collector.otel:4318/v1/traces
```

### 3. Storage and Retention Policies

**Development (local):**
- Prometheus: 24 hours (in-memory)
- Loki: 24 hours
- Tempo: 24 hours

**Staging (Kubernetes):**
```yaml
storage:
  prometheus: 10Gi (7 days @ ~1.4Gi/day)
  loki: 20Gi (14 days)
  tempo: 20Gi (7 days)
retention:
  prometheus: 7d
  loki: 14d
  tempo: 7d
```

**Production (Kubernetes):**
```yaml
storage:
  prometheus: 50Gi (30 days @ ~1.7Gi/day)
  loki: 100Gi (30 days)
  tempo: 50Gi (14 days)
retention:
  prometheus: 30d
  loki: 30d
  tempo: 14d
```

### 4. Service Account and RBAC for Alloy

Alloy needs permissions to read pod logs via Kubernetes API:

```yaml
# deploy/helm/observability-chart/templates/alloy-rbac.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: alloy
rules:
- apiGroups: [""]
  resources: ["pods", "pods/log"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["namespaces"]
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: alloy
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: alloy
subjects:
- kind: ServiceAccount
  name: alloy
  namespace: otel
```

---

## Verification Checklist

After implementation, verify:

- [ ] **Dev Cluster (Docker)**
  - [ ] All services expose metrics on `/actuator/prometheus`
  - [ ] Prometheus scrapes all services at 30s interval
  - [ ] Grafana dashboards display live data
  - [ ] Loki shows logs from all services
  - [ ] Tempo shows complete trace flows across services
  - [ ] Trace IDs correlate logs and traces

- [ ] **Dev Cluster (Kubernetes)**
  - [ ] Observability stack deploys in `otel` namespace
  - [ ] Services in `eazybank-dev` namespace reach OTel Collector
  - [ ] Prometheus scrapes all services
  - [ ] Grafana accessible on http://node:30030
  - [ ] Traces appear in Tempo within 5 seconds

- [ ] **Staging Cluster**
  - [ ] Observability stack deploys in `otel` namespace
  - [ ] Trace sampling at 10% (not 100%)
  - [ ] Storage retention set to 14 days
  - [ ] Grafana accessible on http://node:30030
  - [ ] Dashboards show staging environment metrics

- [ ] **Production Cluster**
  - [ ] Observability stack deploys in `otel` namespace
  - [ ] Trace sampling at 1% (minimal overhead)
  - [ ] Storage retention set to 30 days
  - [ ] Grafana accessible via https://grafana.eazybank.com
  - [ ] Alerts configured for critical metrics
  - [ ] Logs retained for compliance

---

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|-----------|
| Observability stack consumes excessive resources | Medium | Set resource limits in Helm, use appropriate storage sizing |
| Environment variable misconfiguration breaks deployments | Medium | Add validation in Helm templates, test in staging first |
| RBAC insufficient for Alloy log collection | Medium | Test on staging cluster, use `kubectl auth can-i` to verify |
| Trace sampling missing in production | Low | Hardcode sampling in app-values.yaml, override if needed |
| Existing services don't trace correctly | Low | Services already configured with Spring Boot 4.0 OTel support |

---

## Summary: What Needs to Happen

### Critical Missing Pieces:
1. ❌ Observability Helm chart for Prometheus, Grafana, Loki, Tempo, OTel Collector, Alloy
2. ❌ Grafana NodePort/Ingress service definition
3. ❌ Environment variable injection for OTEL_EXPORTER_OTLP_ENDPOINT
4. ❌ GitHub Actions secrets for environment-specific endpoints
5. ❌ Workflow step to deploy observability stack before services

### What Works:
1. ✅ Services are already instrumented with Spring Boot 4.0 OTel support
2. ✅ Docker Compose observability stack is production-ready
3. ✅ Helm service chart is modular and reusable
4. ✅ CI/CD workflow structure supports additional deployments

### Effort Estimate (in task order):
1. Phase 1 (Config refactor): ~1-2 hours
2. Phase 2 (Observability chart): ~3-4 hours
3. Phase 3 (Workflow integration): ~1-2 hours
4. Phase 4 (Documentation): ~1 hour

