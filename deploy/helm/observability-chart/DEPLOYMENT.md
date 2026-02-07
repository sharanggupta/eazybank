# Observability Stack Deployment Guide

## Overview

This Helm chart deploys a production-grade observability stack for EazyBank microservices:
- **Prometheus**: Metrics collection and storage
- **Grafana**: Visualization and dashboarding
- **Loki**: Centralized log aggregation
- **Grafana Alloy**: Log collection agent (replaces Promtail)
- **OpenTelemetry Collector**: Trace collection and metrics export

## Prerequisites

### Kubernetes Cluster Requirements
- Kubernetes 1.24+
- Helm 3.10+
- 4+ CPU cores, 8GB+ RAM available in cluster
- Storage provisioner (PersistentVolumes) available

### Required Namespaces
- `observability-staging` (for staging environment)
- `observability-prod` (for production environment)

## Deployment

### Staging Environment

```bash
# Deploy observability stack to staging
helm upgrade --install observability ./deploy/helm/observability-chart \
  --namespace observability-staging \
  --create-namespace \
  --wait --timeout 10m \
  -f ./deploy/helm/observability-chart/values.yaml \
  -f ./deploy/helm/observability-chart/environments/staging/values.yaml
```

### Production Environment

```bash
# Deploy observability stack to production
helm upgrade --install observability ./deploy/helm/observability-chart \
  --namespace observability-prod \
  --create-namespace \
  --wait --timeout 10m \
  -f ./deploy/helm/observability-chart/values.yaml \
  -f ./deploy/helm/observability-chart/environments/prod/values.yaml
```

### Via GitHub Actions

The observability stack is automatically deployed when you trigger the main deployment workflow:

```bash
# Trigger deployment
gh workflow run deploy.yml --ref main
```

This will:
1. Build all service images
2. Deploy to staging (including observability stack)
3. Wait for manual approval
4. Deploy to production (including observability stack)

## Accessing the Observability Stack

### Local Development (Port Forwarding)

```bash
# Prometheus
kubectl port-forward -n observability-staging svc/prometheus 9090:9090
# Access: http://localhost:9090

# Grafana
kubectl port-forward -n observability-staging svc/grafana 3000:3000
# Access: http://localhost:3000 (admin/admin)

# Loki
kubectl port-forward -n observability-staging svc/loki 3100:3100
# Access: http://localhost:3100

# Alloy UI
kubectl port-forward -n observability-staging ds/alloy 12345:12345
# Access: http://localhost:12345
```

### Production (Ingress)

When Ingress is enabled (production), access via:
- Prometheus: https://prometheus.eazybank.com
- Grafana: https://grafana.eazybank.com
- Loki: https://loki.eazybank.com

Authentication: Basic Auth (HTTP Basic Authentication)

## Configuration

### Environment Variables

Set these in your environment before deployment:

```bash
# Grafana admin password (base64 encoded)
GRAFANA_ADMIN_PASSWORD=$(echo -n "your-password" | base64)

# Ingress hostnames (production only)
PROMETHEUS_HOST="prometheus.eazybank.com"
GRAFANA_HOST="grafana.eazybank.com"
LOKI_HOST="loki.eazybank.com"
```

### Storage Classes

By default, the chart uses the `standard` storage class. For production, use a faster storage class:

```bash
helm ... --set prometheus.storage.storageClass=fast \
         --set loki.storage.storageClass=fast \
         --set grafana.persistence.storageClass=fast
```

### Resource Limits

Customize resource requests/limits per environment in the environment-specific values files.

## Verification

### 1. Check Pod Status

```bash
# Staging
kubectl get pods -n observability-staging

# Production
kubectl get pods -n observability-prod
```

Expected output: All pods should be `Running`

### 2. Verify Prometheus Targets

```bash
# Port-forward to Prometheus
kubectl port-forward -n observability-staging svc/prometheus 9090:9090

# Visit http://localhost:9090/targets
# All service targets should show "UP" status
```

### 3. Verify Service Scraping

```bash
# Query Prometheus for service metrics
kubectl exec -it -n observability-staging prometheus-0 -- \
  curl -s localhost:9090/api/v1/targets | jq '.data.activeTargets[] | .labels.service' | sort | uniq
```

Expected: `account`, `card`, `loan`, `customer-gateway`

### 4. Verify Loki Log Collection

```bash
# Check if Loki is receiving logs
kubectl exec -it -n observability-staging loki-0 -- \
  curl -s http://localhost:3100/loki/api/v1/label/service/values | jq .

# Expected: Services like "account", "card", etc.
```

### 5. Access Grafana

```bash
# Port-forward
kubectl port-forward -n observability-staging svc/grafana 3000:3000

# Open http://localhost:3000
# Login: admin / admin
# Verify dashboards appear:
#   - Application Overview
#   - HTTP Metrics
#   - JVM Metrics
#   - Circuit Breakers
```

## Troubleshooting

### Prometheus Targets Down

**Symptoms**: Targets show "DOWN" in Prometheus UI

**Solution**:
1. Verify services are deployed: `kubectl get pods -n eazybank-staging`
2. Check service metrics endpoint is responding:
   ```bash
   kubectl exec -it -n eazybank-staging <account-pod> -- \
     curl -s localhost:8080/account/actuator/prometheus | head -20
   ```
3. Check Prometheus configuration:
   ```bash
   kubectl get configmap -n observability-staging prometheus-config -o yaml
   ```

### Loki Not Receiving Logs

**Symptoms**: Grafana Loki datasource shows no logs

**Solution**:
1. Verify Alloy is running: `kubectl get ds -n observability-staging alloy`
2. Check Alloy logs: `kubectl logs -n observability-staging -l app=alloy`
3. Verify Loki is accessible: `curl http://loki:3100/ready`

### High Memory/CPU Usage

**Symptoms**: Observability pods consuming excessive resources

**Solution**:
1. Reduce retention period in values.yaml
2. Increase resource limits
3. Enable compression in Prometheus storage
4. Reduce scrape interval if not needed

### Storage Full

**Symptoms**: Prometheus or Loki pods crash, PersistentVolume full

**Solution**:
1. Increase storage size: `helm ... --set prometheus.storage.size=50Gi`
2. Reduce retention period: `helm ... --set prometheus.storage.retention=7d`
3. Scale up PersistentVolumes if possible
4. Implement data cleanup/archival

## Upgrade

### Upgrading to New Version

```bash
# Staging
helm upgrade observability ./deploy/helm/observability-chart \
  --namespace observability-staging \
  --wait --timeout 10m \
  -f ./deploy/helm/observability-chart/values.yaml \
  -f ./deploy/helm/observability-chart/environments/staging/values.yaml

# Production
helm upgrade observability ./deploy/helm/observability-chart \
  --namespace observability-prod \
  --wait --timeout 10m \
  -f ./deploy/helm/observability-chart/values.yaml \
  -f ./deploy/helm/observability-chart/environments/prod/values.yaml
```

## Backup & Restore

### Backing Up Prometheus Data

```bash
# Create a backup
kubectl exec -n observability-prod prometheus-0 -- \
  tar czf /tmp/prometheus-backup.tar.gz /prometheus

# Download backup
kubectl cp observability-prod/prometheus-0:/tmp/prometheus-backup.tar.gz ./prometheus-backup.tar.gz
```

### Backing Up Grafana Dashboards

```bash
# Export all dashboards
kubectl exec -n observability-prod grafana-0 -- \
  grafana-cli admin export-dashboard

# Or use Grafana API
curl -H "Authorization: Bearer $API_TOKEN" \
  http://grafana:3000/api/dashboards/db > dashboards-backup.json
```

## Security Considerations

### Production Checklist

- [ ] Update Grafana admin password from default
- [ ] Enable Ingress with TLS/SSL certificates
- [ ] Configure Network Policies to restrict access
- [ ] Enable RBAC for Kubernetes access
- [ ] Implement Pod Security Policies
- [ ] Set up firewall rules for database access
- [ ] Enable audit logging in Kubernetes
- [ ] Rotate API tokens/keys regularly
- [ ] Enable encryption at rest for storage
- [ ] Set up monitoring alerts in Prometheus

### Network Policies

For production, uncomment network policies in prod values.yaml:

```yaml
networkPolicy:
  enabled: true
```

This restricts traffic to only allow from EazyBank pods.

## Monitoring the Observability Stack Itself

### Key Metrics to Watch

```promql
# Prometheus memory usage
container_memory_usage_bytes{pod=~"prometheus.*"}

# Grafana uptime
up{job="grafana"}

# Loki ingestion rate
rate(loki_ingester_chunks_created_total[5m])

# Alloy logs processed
rate(alloy_logs_processed_total[5m])
```

### Create Alerts

Example Prometheus alert rule:

```yaml
groups:
  - name: observability
    rules:
      - alert: PrometheusHighMemory
        expr: |
          container_memory_usage_bytes{pod=~"prometheus.*"} /
          container_spec_memory_limit_bytes > 0.8
        for: 5m
        annotations:
          summary: "Prometheus memory usage is {{ $value | humanizePercentage }}"
```

## Support & Documentation

- **Prometheus Docs**: https://prometheus.io/docs/
- **Grafana Docs**: https://grafana.com/docs/grafana/latest/
- **Loki Docs**: https://grafana.com/docs/loki/latest/
- **Grafana Alloy**: https://grafana.com/docs/alloy/latest/
- **OpenTelemetry**: https://opentelemetry.io/docs/

## Rollback

### Rollback to Previous Release

```bash
# View release history
helm history observability -n observability-staging

# Rollback to specific version
helm rollback observability <REVISION> -n observability-staging
```

Example:
```bash
# Rollback to previous release
helm rollback observability -n observability-staging
```
