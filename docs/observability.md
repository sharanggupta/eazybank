# EazyBank Observability Implementation Guide

This document tracks the implementation of modern observability across the EazyBank microservices platform. We follow a phased approach using Spring Boot Actuator, Micrometer, OpenTelemetry, and Grafana.

---

## PHASE 1: Spring Boot Actuator Foundation

**Purpose**: Enable health checks and basic metrics exposure that form the foundation for all observability.

**Status**: ✅ COMPLETE

### What is Spring Boot Actuator?

Spring Boot Actuator is a module that **exposes operational endpoints** over HTTP. These endpoints provide:
- **Health checks** - for Kubernetes liveness/readiness probes
- **Metrics** - raw metrics data (JVM, HTTP, databases)
- **Prometheus endpoint** - Prometheus-formatted metrics at `/actuator/prometheus`
- **Info** - application metadata (version, build info)
- **Custom endpoints** - gateway status, circuit breaker state, etc.

### Why This Matters

1. **Kubernetes Integration**: Kubernetes uses `/actuator/health/liveness` and `/actuator/health/readiness` probes to:
   - **Restart pods** if the app is stuck (liveness)
   - **Route traffic** only to ready pods (readiness)

2. **Metrics Foundation**: `/actuator/prometheus` provides the **single source of truth** for metrics that Prometheus scrapes every 30 seconds.

3. **Production Observability**: Without Actuator, there is **no way to observe** your application health, performance, or problems.

### Architecture Notes

```
┌──────────────────────────────────────────────────────────┐
│ Spring Boot Application (4.0.x)                         │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ Spring Boot Actuator (Auto-configured)         │    │
│  │                                                 │    │
│  │  GET /actuator                 → Index        │    │
│  │  GET /actuator/health          → Overall      │    │
│  │  GET /actuator/health/liveness → Liveness     │    │
│  │  GET /actuator/health/readiness→ Readiness    │    │
│  │  GET /actuator/metrics         → Metric names │    │
│  │  GET /actuator/prometheus      → Prometheus   │    │
│  │  GET /actuator/info            → App info     │    │
│  │                                                 │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
│  Exposed on separate management port (default: same)   │
└──────────────────────────────────────────────────────────┘
```

### Configuration Applied (PHASE 1)

**File**: All 4 services' `application.yml`
- `account/src/main/resources/application.yml`
- `card/src/main/resources/application.yml`
- `loan/src/main/resources/application.yml`
- `customer-gateway/src/main/resources/application.yaml`

**Configuration**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: always
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

**What Each Setting Does**:

| Setting | Purpose | Value |
|---------|---------|-------|
| `endpoints.web.exposure.include` | Which endpoints to expose over HTTP | `health,info,metrics,prometheus` |
| `endpoint.health.probes.enabled` | Enable Kubernetes probe endpoints | `true` |
| `endpoint.health.show-details` | Return full health details (not just UP/DOWN) | `always` |
| `health.livenessState.enabled` | Enable `/health/liveness` endpoint | `true` |
| `health.readinessState.enabled` | Enable `/health/readiness` endpoint | `true` |

### HTTP Endpoints Exposed

After this configuration, each service exposes:

| Endpoint | Purpose | Example Response |
|----------|---------|-----------------|
| `GET /health` | Overall health status | `{"status":"UP"}` |
| `GET /health/liveness` | Is app alive? (restart if DOWN) | `{"status":"UP"}` |
| `GET /health/readiness` | Ready to accept traffic? (drain if DOWN) | `{"status":"UP"}` |
| `GET /metrics` | List all available metrics | `["http.server.requests", "jvm.memory.used", ...]` |
| `GET /prometheus` | Prometheus-format metrics | `# HELP http_server_requests ...` |
| `GET /info` | Application info | `{"app":"account","version":"1.0.0"}` |

### How Kubernetes Uses These Endpoints

**Liveness Probe** (restarts pod if fails):
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```
→ If app deadlocks or crashes internally, Kubernetes detects and **restarts the pod**

**Readiness Probe** (stops routing traffic if fails):
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 5
```
→ If app can't handle requests (DB down, dependencies unavailable), Kubernetes **removes from load balancer**

### Local Testing - PHASE 1 Verification

To verify everything works, start the services locally and test:

**1. Health Endpoint (Basic)**
```bash
curl http://localhost:8080/account/actuator/health
```
Expected:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "livenessState": {"status": "UP"},
    "readinessState": {"status": "UP"},
    "r2dbc": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

**2. Liveness Probe**
```bash
curl http://localhost:8080/account/actuator/health/liveness
```
Expected: `{"status":"UP"}`

**3. Readiness Probe**
```bash
curl http://localhost:8080/account/actuator/health/readiness
```
Expected: `{"status":"UP"}`

**4. Application Info**
```bash
curl http://localhost:8080/account/actuator/info
```
Expected:
```json
{
  "app": {
    "build": {"version": "1.0.0"},
    "support": {
      "contact": {"name": "Account Service Support Team"}
    }
  }
}
```

**5. Available Metrics (Names Only)**
```bash
curl http://localhost:8080/account/actuator/metrics
```
Expected: List of metric names like `jvm.memory.used`, `http.server.requests`, etc.

**6. Prometheus Format (What Prometheus Scrapes)**
```bash
curl http://localhost:8080/account/actuator/prometheus
```
Expected: Metrics in Prometheus text format:
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap"} 123456789

# HELP http_server_requests_seconds HTTP requests
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/account/health"} 5
http_server_requests_seconds_sum{...} 0.125
```

### How Operators Will Use This (Future Phases)

- **Now (PHASE 1)**: Kubernetes uses probes to manage pod lifecycle
- **PHASE 2**: Prometheus scrapes `/actuator/prometheus` every 30s
- **PHASE 3**: Tracing captures flow through services
- **PHASE 4**: Logs include trace IDs for correlation
- **PHASE 7**: Grafana queries Prometheus for dashboards

### Production Impact

| Scenario | Without Actuator | With Actuator |
|----------|------------------|---------------|
| App crashes but process runs | **Pod keeps running** (stuck) | **Pod restarted** (liveness detects) |
| Database connection pool exhausted | **Requests hang indefinitely** | **Pod removed from LB** (readiness detects) |
| Metrics not collected | **No visibility** into performance | **Visible** via Prometheus/Grafana |
| Debugging production | **Blind** - no health data | **Can see** health, probe status, metrics |

### What Signals This Produces

**Metrics exposed at `/actuator/prometheus`**:
- JVM: `jvm_memory_used_bytes`, `jvm_threads_count`, `jvm_gc_pause_seconds`
- HTTP: `http_server_requests_seconds_count`, `http_server_requests_seconds_max`
- Process: `process_uptime_seconds`, `process_cpu_usage`

**Health states**:
- `UP` - all checks pass, ready for traffic
- `DOWN` - critical component failed, remove from LB
- Component-specific: `diskSpace`, `r2dbc`, `livenessState`, `readinessState`

### Git Status

**Files Modified**:
- `account/src/main/resources/application.yml`
- `card/src/main/resources/application.yml`
- `loan/src/main/resources/application.yml`
- `customer-gateway/src/main/resources/application.yaml`

**Changed**:
- Exposed `metrics` and `prometheus` endpoints
- Enabled health probes (liveness/readiness)
- Set `show-details: always` for full health info

---

## PHASE 2: Structured JSON Logging with Trace Correlation

**Status**: ✅ COMPLETE

Services now emit structured JSON logs with automatic trace ID/span ID correlation.

---

## PHASE 3: Distributed Tracing with OpenTelemetry

**Status**: ✅ COMPLETE

Services auto-instrument and send OTLP traces to OpenTelemetry Collector.

---

## PHASE 4: Environment-Specific Configuration

**Status**: ✅ COMPLETE

All observability settings (sampling probability, endpoints) defined in Helm values files for dev/staging/prod.

---

## PHASE 5: Complete Local Observability Stack

**Status**: ⏳ IN PROGRESS

Deploy complete observability infrastructure locally and wire services together.

### What PHASE 5 Provides

**Updated docker-compose.yml:**
- Adds OTEL_EXPORTER_OTLP_ENDPOINT to all 4 services (so they can reach OTel Collector)
- Manages complete observability stack as services
- Proper service dependencies

**New Configuration Files:**

1. **otel-collector-config.yaml** - OpenTelemetry Collector
   - Receives OTLP traces on port 4318
   - Logs traces for debugging
   - Can be extended to send to Tempo/Jaeger

2. **prometheus.yml** - Prometheus metrics scraper
   - Scrapes `/actuator/prometheus` from all 4 services every 30s
   - Stores time-series metrics in local storage

3. **loki-config.yaml** - Loki log aggregation
   - Receives JSON logs from Alloy
   - 168-hour retention
   - BoltDB + filesystem backend

4. **alloy-config.alloy** - Grafana Alloy log collector
   - Discovers EazyBank Docker containers
   - Tails container logs
   - Parses JSON logs
   - Sends to Loki with proper labels (level, service, traceId, spanId)

**Infrastructure Services:**
```
otel-collector (port 4318)     ← receives OTLP traces from services
  ↓
prometheus (port 9090)         ← scrapes /actuator/prometheus
  ↓
grafana (port 3000)            ← visualizes metrics, logs, traces
  ↑
loki (port 3100)               ← aggregates logs from Alloy
  ↑
alloy (port 12345)             ← collects Docker container logs
```

### How the Data Flows

**Traces:**
```
Service → OTel SDK → OTLP HTTP → OTel Collector:4318 → (Logs to stdout)
```

**Metrics:**
```
Service → Micrometer → /actuator/prometheus → Prometheus scraper → Prometheus DB → Grafana
```

**Logs:**
```
Service → stdout → Docker daemon → Alloy → Loki → Grafana
```

### Local Testing PHASE 5

After deployment:

1. **Check all services are running:**
   ```bash
   docker compose ps
   ```

2. **Make requests to generate traces:**
   ```bash
   curl http://localhost:8080/account/actuator/health
   ```

3. **Verify OTel Collector received traces:**
   ```bash
   docker logs eazybank-otel-collector | grep ResourceSpans
   ```

4. **Check Prometheus targets:**
   - Visit http://localhost:9090/targets
   - All 4 services should show as UP

5. **View metrics in Grafana:**
   - Visit http://localhost:3000 (admin/admin)
   - Add Prometheus as data source
   - Create dashboards for JVM, HTTP, circuit breaker metrics

6. **View logs in Loki:**
   - In Grafana, add Loki data source
   - Query logs: `{service="account"}`
   - Filter by level, trace_id, etc.

7. **Find traces by ID across services:**
   - Make request: `curl http://localhost:8000/api/customer/details/1234567890`
   - Extract traceId from any service log
   - Search all services: `{traceId="4c5c3f2e-..."}`

---

## Summary

After PHASE 1, each service now:
1. ✅ Exposes health endpoints (Kubernetes can probe)
2. ✅ Exposes metrics endpoint (Prometheus can scrape)
3. ✅ Automatically instruments JVM + HTTP metrics
4. ✅ Reports application info

**Next**: Add Micrometer Prometheus registry to actually **export** metrics.
