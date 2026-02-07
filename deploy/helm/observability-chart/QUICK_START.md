# Observability Stack - Quick Start

## Minimal Deployment (30 seconds)

**Prerequisites**: kubectl, helm 3.10+, kubeconfig configured

### Staging
```bash
# Add Helm repositories
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Deploy
cd deploy/helm/observability-chart
helm dependency update
helm upgrade --install observability . \
  --namespace observability-staging \
  --create-namespace \
  --wait \
  -f values.yaml \
  -f environments/staging/values.yaml
```

### Production
```bash
# Same as staging, but use environments/prod/values.yaml
helm upgrade --install observability . \
  --namespace observability-prod \
  --create-namespace \
  --wait \
  -f values.yaml \
  -f environments/prod/values.yaml
```

## Access Dashboards

```bash
# Staging - Port forward
kubectl port-forward -n observability-staging svc/grafana 3000:3000
# Open: http://localhost:3000 (admin/admin)

# Production - Uses Ingress
# https://grafana.eazybank.com (requires TLS cert configured)
```

## Verify Deployment

```bash
# Check pods
kubectl get pods -n observability-staging

# View Prometheus targets
kubectl port-forward -n observability-staging svc/prometheus 9090:9090
# Open: http://localhost:9090/targets

# Check logs
kubectl logs -f -n observability-staging -l app=grafana
```

## Troubleshooting

| Issue | Command |
|-------|---------|
| Pods not starting | `kubectl describe pod <name> -n observability-staging` |
| Helm deployment fails | `helm upgrade --dry-run -f values.yaml -f environments/staging/values.yaml .` |
| No metrics | Check `kubectl port-forward svc/prometheus 9090:9090` → /targets |
| No logs | Check `kubectl logs -l app=alloy -n observability-staging` |

## Key Files

- `Chart.yaml` - Chart metadata + dependencies
- `values.yaml` - Base configuration (Prometheus, Grafana, Loki)
- `environments/staging/values.yaml` - Staging overrides (1 replica, 7-day retention)
- `environments/prod/values.yaml` - Production overrides (2 replicas, 30-day retention)
- `templates/otel-collector-deployment.yaml` - OpenTelemetry Collector
- `templates/alloy-daemonset.yaml` - Grafana Alloy (log collector)

## Important Notes

1. **Change Grafana password**: Set `grafana.adminPassword` in prod values
2. **TLS certificates**: For production, ensure cert-manager and Let's Encrypt issuer configured
3. **Storage class**: Update `storageClassName` if using non-standard storage
4. **Ingress**: Production requires nginx-ingress controller and basic auth secret

## Full Documentation

See `DEPLOYMENT.md` for comprehensive guide including:
- Prerequisites
- Configuration options
- Backup & restore
- Security considerations
- Rollback procedures
