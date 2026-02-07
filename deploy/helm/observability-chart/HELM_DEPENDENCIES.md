# Observability Chart - Helm Dependencies

## Overview

The EazyBank observability Helm chart uses official community Helm charts as dependencies:

1. **kube-prometheus-stack** - Prometheus and Grafana
2. **grafana** - Standalone Grafana (can be disabled if using kube-prometheus-stack's Grafana)
3. **loki-stack** - Loki and log collection

## Installing Dependencies

Before deploying the chart, fetch the dependencies:

```bash
cd deploy/helm/observability-chart
helm dependency update
```

This will download the subchart dependencies to `charts/` directory.

## Chart Structure

```
observability-chart/
├── Chart.yaml                 # Main chart definition with dependencies
├── values.yaml               # Default values (overrides subcharts)
├── environments/
│   ├── staging/values.yaml   # Staging-specific overrides
│   └── prod/values.yaml      # Production-specific overrides
├── dashboards/               # Grafana dashboard JSON files
├── templates/
│   ├── otel-collector-deployment.yaml   # Custom OTEL collector
│   ├── alloy-daemonset.yaml             # Grafana Alloy for log collection
│   └── grafana-dashboards-configmap.yaml
└── charts/                   # Downloaded dependencies (auto-generated)
```

## Deploying the Stack

### Staging Deployment

```bash
helm dependency update ./deploy/helm/observability-chart

helm upgrade --install observability ./deploy/helm/observability-chart \
  --namespace observability \
  --wait --timeout 10m \
  -f ./deploy/helm/observability-chart/values.yaml \
  -f ./deploy/helm/observability-chart/environments/staging/values.yaml
```

### Production Deployment

```bash
helm upgrade --install observability ./deploy/helm/observability-chart \
  --namespace observability \
  --wait --timeout 10m \
  -f ./deploy/helm/observability-chart/values.yaml \
  -f ./deploy/helm/observability-chart/environments/prod/values.yaml
```

## Accessing Components

### Staging (via NodePort)

- **Prometheus**: http://\<node-ip\>:30090
- **Grafana**: http://\<node-ip\>:30300 (admin/admin)
- **Loki**: http://\<node-ip\>:30100 (for debugging)

### Production (via Ingress)

Configure DNS records for:
- prometheus.eazybank.com
- grafana.eazybank.com
- loki.eazybank.com

Then access via:
- https://prometheus.eazybank.com
- https://grafana.eazybank.com
- https://loki.eazybank.com

## Verifying Deployment

```bash
# Check all observability pods are running
kubectl get pods -n observability

# Check services
kubectl get svc -n observability

# Check Prometheus targets
kubectl port-forward -n observability svc/prometheus 9090:9090
# Then visit http://localhost:9090/targets

# Check Grafana dashboards
kubectl port-forward -n observability svc/grafana 3000:3000
# Then visit http://localhost:3000
```

## Troubleshooting

### Helm dependency update fails

Make sure you have the correct Helm repositories:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
```

### Subcharts not deploying

Check that conditions are met:

```bash
# Verify prometheus is enabled in values
helm get values observability -n observability | grep -A 2 "prometheus:"

# Check actual deployed resources
kubectl get all -n observability
```

### PVC not binding

Ensure storage class exists:

```bash
kubectl get storageclass
```

For staging, use `standard` storage class. For production, configure a `fast` storage class.

## Values Override Examples

### Disable Grafana, use only Prometheus

```yaml
grafana:
  enabled: false
prometheus:
  enabled: true
```

### Change retention periods

```yaml
prometheus:
  prometheusSpec:
    retention: 30d  # Change from default

loki:
  config:
    retention: 14d  # Change from default
```

### Custom storage class

```yaml
prometheus:
  storageSpec:
    volumeClaimTemplate:
      spec:
        storageClassName: my-storage-class
        resources:
          requests:
            storage: 50Gi
```

## Reference Documentation

- [kube-prometheus-stack Helm Chart](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack)
- [Grafana Helm Chart](https://github.com/grafana/helm-charts/tree/main/charts/grafana)
- [Loki-stack Helm Chart](https://github.com/grafana/helm-charts/tree/main/charts/loki-stack)
