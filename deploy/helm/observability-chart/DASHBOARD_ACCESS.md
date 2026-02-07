# Accessing Grafana Dashboards

This document explains how to access the EazyBank observability dashboards in different deployment environments.

## Local Development (Docker Compose)

Grafana dashboards are automatically provisioned and accessible at:

```
http://localhost:3000
```

**Default Credentials:**
- Username: `admin`
- Password: `admin`

**Available Dashboards:**
1. **Application Overview** - Key metrics (RPS, error rate, p95 latency, error trends)
2. **HTTP Metrics** - Request distribution, latency percentiles by service
3. **JVM Metrics** - Heap memory usage, garbage collection, thread count
4. **Circuit Breakers** - Resilience4j state, failure rates (gateway only)

---

## Kubernetes - Staging

Grafana is exposed via **NodePort** for easy external access:

```
http://<node-ip>:30300
```

**Steps:**
1. Get a node IP from your Kubernetes cluster:
   ```bash
   kubectl get nodes -o wide
   ```

2. Access Grafana at: `http://<node-ip>:30300`

3. Default credentials:
   - Username: `admin`
   - Password: `admin` (override in `environments/staging/values.yaml`)

**Related NodePorts:**
- Prometheus: `http://<node-ip>:30090`
- Loki: `http://<node-ip>:30100` (for log queries)

### Deploy to Staging:

```bash
helm upgrade --install observability ./observability-chart \
  -f ./values.yaml \
  -f ./environments/staging/values.yaml \
  -n observability-staging \
  --create-namespace
```

---

## Kubernetes - Production

Grafana is exposed via **Ingress** with TLS and authentication:

```
https://grafana.eazybank.com
```

**Access Methods:**

1. **Via Ingress (Recommended):**
   ```
   https://grafana.eazybank.com
   ```
   - TLS encrypted
   - Basic authentication enabled
   - Hostname must be resolvable (via DNS or /etc/hosts)

2. **Via NodePort (Fallback):**
   ```
   http://<node-ip>:30300
   ```
   - No authentication
   - No TLS
   - Use only for debugging

**Ingress Configuration:**
- Class: `nginx`
- TLS: Let's Encrypt certificate (production issuer)
- Auth: Basic authentication (username/password required)
- Hostname: `grafana.eazybank.com`

**To view Ingress status:**
```bash
kubectl get ingress -n observability-prod
kubectl describe ingress prometheus-grafana -n observability-prod
```

**To port-forward for testing (if Ingress not accessible):**
```bash
kubectl port-forward -n observability-prod svc/grafana 3000:3000
# Access at http://localhost:3000
```

### Deploy to Production:

```bash
helm upgrade --install observability ./observability-chart \
  -f ./values.yaml \
  -f ./environments/prod/values.yaml \
  -n observability-prod \
  --create-namespace
```

**Pre-requisites:**
1. Nginx Ingress Controller installed
2. cert-manager installed (for TLS)
3. DNS configured for `grafana.eazybank.com`
4. Basic auth credentials configured in secret:
   ```bash
   kubectl create secret generic observability-basic-auth \
     --from-literal=auth=$(htpasswd -cb /dev/null admin password123 | cut -d: -f2) \
     -n observability-prod
   ```

---

## Dashboard Details

### Application Overview Dashboard

**Key Metrics:**
- **Services Healthy**: Count of services that are UP
- **Total Request Rate**: Requests per second across all services
- **Error Rate (5xx)**: Percentage of 5xx errors (yellow >= 1%, red >= 5%)
- **p95 Latency**: 95th percentile response time (yellow >= 200ms, red >= 500ms)
- **Request Rate by Service**: Time series showing RPS for each service
- **Requests by Status Code**: Stacked bar chart of requests by HTTP status
- **Error Rate Trend**: 5xx error rate over time (indicator of system health)

### HTTP Metrics Dashboard

**Panels:**
- **HTTP Status Codes**: Pie chart of 2xx, 3xx, 4xx, 5xx distribution
- **Request Rate (RPS)**: Current request rate across all services
- **HTTP Latency Percentiles**: p50, p95, p99 latency by service
- **Request Status Distribution**: Success vs client error vs server error by service

### JVM Metrics Dashboard

**Panels:**
- **Heap Memory %**: Individual gauges for each service (green < 70%, yellow 70-90%, red > 90%)
- **Heap Memory Usage**: Time series showing used vs max heap for all services
- **Live Threads Count**: Thread count per service
- **GC Memory Promoted**: Garbage collection activity (5-minute window)

### Circuit Breakers Dashboard (Gateway Only)

**Panels:**
- **Circuit Breaker States**: Pie chart showing CLOSED vs OPEN vs HALF_OPEN
- **Failure Rate**: Percentage of failures for each circuit breaker
- **Buffered Calls**: Calls queued when in HALF_OPEN state
- **Total Calls**: Success vs failure call counts
- **Circuit Breaker Latency**: p95 latency for breaker calls

---

## Troubleshooting

### Dashboards Not Showing Data

1. **Check if services are emitting metrics:**
   ```bash
   # Local dev
   curl http://localhost:8080/account/actuator/prometheus | head -20

   # Kubernetes
   kubectl port-forward svc/account 8080:8080 -n eazybank-staging
   curl http://localhost:8080/account/actuator/prometheus | head -20
   ```

2. **Check if Prometheus is scraping:**
   - Local: http://localhost:9090/targets
   - Kubernetes: `kubectl port-forward svc/prometheus 9090:9090 -n observability-staging`
   - Then visit http://localhost:9090/targets

3. **Verify datasources in Grafana:**
   - Go to Grafana → Configuration → Data Sources
   - Click "Prometheus" and test the connection
   - Should show "Datasource is working" in green

### Dashboard Files Missing (Kubernetes)

The dashboards are provisioned via ConfigMaps. To verify:
```bash
kubectl get configmap -n observability-staging | grep grafana-dashboards
kubectl describe configmap observability-grafana-dashboards -n observability-staging
```

If missing, the Helm template `grafana-dashboards-configmap.yaml` should create them. Verify the template file exists:
```bash
ls -la deploy/helm/observability-chart/templates/grafana-dashboards-configmap.yaml
ls -la deploy/helm/observability-chart/dashboards/*.json
```

### Cannot Access Grafana

**Local Development:**
```bash
docker ps | grep grafana
docker logs <grafana-container-id>
```

**Kubernetes Staging:**
```bash
kubectl get svc -n observability-staging grafana
kubectl get pods -n observability-staging -l app=grafana
kubectl logs -f deployment/grafana -n observability-staging
```

**Kubernetes Production:**
```bash
kubectl get ingress -n observability-prod
kubectl describe ingress prometheus-grafana -n observability-prod
kubectl logs -f deployment/grafana -n observability-prod
```

---

## Notes

### Service Uptime vs Error Rate
Previous dashboards used `process_uptime_seconds` which always increases and is not useful for real-time monitoring. This has been replaced with **Error Rate Trend** which shows actual 5xx errors per second - a real indicator of system health.

### Kubernetes NodePort Ports
The following NodePorts are reserved for observability:
- `30090` - Prometheus
- `30100` - Loki
- `30300` - Grafana

Ensure these ports are available on your cluster nodes.

### Dashboard Persistence
- **Local Dev**: Dashboards are provisioned from JSON files in `deploy/dev/grafana/dashboards/`
- **Kubernetes**: Dashboards are provisioned from ConfigMaps created from the same JSON files
- **Changes**: Edit the JSON files and re-apply Helm chart to update dashboards

### Backup Dashboards
Export important dashboards regularly:
```bash
# Via Grafana UI: Dashboard → Settings → JSON Model → Copy

# Or via API:
curl -s http://<grafana>:3000/api/dashboards/db/app-overview | jq > dashboard-backup.json
```
