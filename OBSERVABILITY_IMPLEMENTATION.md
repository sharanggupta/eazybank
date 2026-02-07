# EazyBank Observability Stack - Technical Reference

Complete technical documentation of the observability implementation. For setup instructions, see [README_OBSERVABILITY.md](README_OBSERVABILITY.md) and [QUICK_START.md](deploy/helm/observability-chart/QUICK_START.md).

---

## Table of Contents

1. [Architecture](#architecture)
2. [Technology Stack](#technology-stack)
3. [Application Instrumentation](#application-instrumentation)
4. [Metrics Reference](#metrics-reference)
5. [Configuration Options](#configuration-options)
6. [Design Decisions](#design-decisions)
7. [Compatibility Matrix](#compatibility-matrix)

---

## Architecture

### 3-Tier Observability Stack

```
┌──────────────────────────────────────────────────┐
│           VISUALIZATION LAYER                    │
│  Grafana (Dashboards, Alerts, Analytics)        │
│  - Application Overview Dashboard                │
│  - HTTP Metrics Dashboard                        │
│  - JVM Metrics Dashboard                         │
│  - Circuit Breakers Dashboard                    │
└──────────────────────────────────────────────────┘
        ↑             ↑            ↑
┌───────┴─────────────┴────────────┴───────────────┐
│    DATA COLLECTION & AGGREGATION LAYER           │
├──────────────────────────────────────────────────┤
│ Prometheus    Grafana Loki      OTel Collector   │
│ (Metrics)     (Logs)            (Traces/Metrics) │
│ 15-30d        3-30d             Real-time        │
│ retention     retention         processing       │
└──────────────────────────────────────────────────┘
       ↑              ↑                  ↑
┌──────┴──────────────┴──────────────────┴────────┐
│    INSTRUMENTATION LAYER                        │
├──────────────────────────────────────────────────┤
│ Spring Boot 4.0 Services (Account, Card, Loan,  │
│ Customer Gateway)                               │
│                                                 │
│ • Micrometer + Prometheus Registry (metrics)   │
│ • OpenTelemetry OTLP Exporter (traces)         │
│ • Logstash Logback Encoder (structured logs)   │
│ • Spring Boot Actuator endpoints                │
│                                                 │
│ Grafana Alloy (Log Collection Agent)           │
│ • Discovers container logs via Docker socket   │
│ • Parses JSON and extracts fields              │
│ • Forwards to Loki with labels                 │
└──────────────────────────────────────────────────┘
```

---

## Technology Stack

| Component | Version | Purpose | Notes |
|-----------|---------|---------|-------|
| Spring Boot | 4.0.x | Application framework | Java 21 with virtual threads |
| Micrometer | 1.16.x | Metrics abstraction | Managed by Spring Boot BOM |
| OpenTelemetry | 1.x | Distributed tracing | OTLP protocol via HTTP |
| Prometheus | 2.52.0 | Metrics storage | Community chart dependency |
| Grafana | 11.0.0 | Visualization | Grafana chart dependency |
| Loki | 3.0.0 | Log aggregation | Grafana chart dependency |
| Grafana Alloy | latest | Log collection | Replaces deprecated Promtail |
| OTel Collector | 0.97.0 | Trace collection | Custom template |
| logstash-logback | 8.0 | JSON logging | MDC correlation support |

---

## Application Instrumentation

### Maven Dependencies

Added to all 4 service pom.xml files:

```xml
<!-- Metrics: Micrometer + Prometheus -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
  <scope>runtime</scope>
</dependency>

<!-- Distributed Tracing: OpenTelemetry -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

<!-- Structured Logging: JSON with MDC -->
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>8.0</version>
</dependency>
```

### Configuration (application.yml)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      probes:
        enabled: true
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
    tags:
      application: ${spring.application.name}
      environment: ${SPRING_PROFILES_ACTIVE:default}
  tracing:
    sampling:
      probability: 1.0  # Override in prod: 0.1
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}

logging:
  pattern:
    level: "%5p [${spring.application.name},%X{traceId},%X{spanId}]"
```

### Logback Configuration (logback-spring.xml)

**JSON Output** (Production/Staging):
```xml
<appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
  <encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <customFields>{"service":"${springAppName}"}</customFields>
    <includeMdcKeyName>traceId</includeMdcKeyName>
    <includeMdcKeyName>spanId</includeMdcKeyName>
  </encoder>
</appender>
```

**Plain Text Output** (Development):
```xml
<pattern>%d{HH:mm:ss} %5p [${springAppName},%X{traceId},%X{spanId}] %logger{36} : %m%n</pattern>
```

---

## Metrics Reference

### HTTP Metrics (Spring WebFlux)

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `http_server_requests_seconds_count` | Counter | Total requests | service, method, uri, status |
| `http_server_requests_seconds_sum` | Counter | Total request duration | service, method, uri, status |
| `http_server_requests_seconds_bucket` | Histogram | Request latency buckets | (same) |
| `http_client_requests_seconds_*` | (same) | Outbound requests | (same) |

**Queryable in Grafana**:
```promql
# Request rate
sum(rate(http_server_requests_seconds_count[1m]))

# p95 latency per service
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le, service))

# Error rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
```

### JVM Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `jvm_memory_usage_bytes` | Gauge | Heap and non-heap memory usage |
| `jvm_memory_used_bytes` | Gauge | Currently allocated memory |
| `jvm_memory_max_bytes` | Gauge | Maximum available memory |
| `jvm_threads_live_threads` | Gauge | Active thread count |
| `jvm_gc_memory_promoted_bytes_total` | Counter | Objects promoted to old gen |
| `jvm_classes_loaded_classes` | Gauge | Loaded class count |

### Resilience4j Circuit Breaker Metrics

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `resilience4j_circuitbreaker_state` | Gauge | 0=CLOSED, 1=OPEN, 2=HALF_OPEN | name |
| `resilience4j_circuitbreaker_failure_rate` | Gauge | Percentage (0-100) | name |
| `resilience4j_circuitbreaker_calls_total` | Counter | Total calls | name, outcome |
| `resilience4j_circuitbreaker_buffered_calls` | Gauge | Queued calls | name, state |

### Application Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `process_uptime_seconds` | Gauge | Seconds since service started |
| `process_cpu_usage` | Gauge | CPU utilization percentage |
| `logback_events_total` | Counter | Logs emitted by level |
| `spring_boot_application_info` | Gauge | Service version & info |

---

## Configuration Options

### Per-Environment Overrides

**Staging** (`environments/staging/values.yaml`):
- 1 replica per component
- 7-day metrics, 3-day logs retention
- 512MB-1GB memory per pod
- 20GB storage

**Production** (`environments/prod/values.yaml`):
- 2 replicas per component (HA)
- 30-day retention (both metrics and logs)
- 1GB-2GB memory per pod
- 100GB storage
- Ingress enabled with TLS
- Network policies enabled

### Storage Class

Default: `standard`

For production, override to faster storage:
```yaml
storageClassName: fast
```

### Resource Scaling

Override requests/limits for each component:
```yaml
prometheus:
  prometheusSpec:
    resources:
      requests:
        cpu: 500m
        memory: 1Gi
      limits:
        cpu: 1000m
        memory: 2Gi
```

---

## Design Decisions

### Why OpenTelemetry (Not Sleuth/Zipkin)

- Spring Boot 4.0 has native OpenTelemetry support via Micrometer
- Spring Cloud Sleuth is deprecated (incompatible with SB 3+)
- OpenTelemetry is the CNCF standard (Zipkin is legacy)
- OTLP protocol is simpler than Zipkin API

### Why Grafana Alloy (Not Promtail)

- Promtail is deprecated (EOL Feb 2025)
- Grafana Alloy is the unified replacement for logs, metrics, and traces
- Single agent instead of multiple collectors
- Native Docker integration via socket

### Why Official Helm Charts

- Tested and maintained by communities
- Reduces custom template burden
- Easier version management and upgrades
- Dependencies explicitly defined in Chart.yaml

### Why Loki (Not ELK/Splunk)

- No index overhead (uses TSDB backend)
- Logs stored in same format as metrics (YAML/JSON)
- Natural Grafana integration (no plugins)
- Lower operational complexity

### Why Micrometer (Not Custom Metrics)

- Industry standard
- Automatic Spring Boot integration
- Works with any backend (Prometheus, Datadog, etc.)
- Built-in JVM metrics

---

## Compatibility Matrix

### Spring Boot 4.0 + Micrometer

| Component | Version | Status |
|-----------|---------|--------|
| micrometer-core | 1.16.x | ✅ Included in SB 4.0 |
| micrometer-tracing-bridge-otel | Latest | ✅ Works with SB 4.0 |
| spring-boot-starter-actuator | 4.0.x | ✅ Required |
| logstash-logback-encoder | 8.0+ | ✅ Jackson 3.0+ compat |

### Kubernetes Requirements

| Component | Version | Status |
|-----------|---------|--------|
| Kubernetes | 1.24+ | ✅ Tested |
| Helm | 3.10+ | ✅ Required |
| Prometheus Operator | 0.50+ | ✅ Optional (ServiceMonitor) |
| Ingress Controller | nginx 1.0+ | ✅ For prod only |

### Storage Requirements

| Environment | Prometheus | Loki | Grafana | Total |
|-------------|-----------|------|---------|-------|
| Staging | 20GB | 20GB | 5GB | 45GB |
| Production | 100GB | 100GB | 10GB | 210GB |

---

## Troubleshooting Reference

### Prometheus Targets DOWN

**Root Cause**: Service metrics endpoint not responding

**Diagnostics**:
```promql
up{job=~"account|card|loan|customer-gateway"} == 0
```

**Fix**:
1. Verify service is running: `kubectl get pods -n eazybank-prod`
2. Check metrics endpoint: `curl http://account:8080/account/actuator/prometheus`
3. Verify OTLP env var: `kubectl set env ... OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318/v1/traces`

### High Memory/CPU

**Causes**: High trace sampling, long retention, insufficient replicas

**Fixes**:
- Reduce sampling: `management.tracing.sampling.probability: 0.01`
- Reduce retention: `prometheus.retention: 7d`
- Scale replicas: `prometheus.replicas: 2`

---

## References

- **Prometheus Docs**: https://prometheus.io/docs/
- **Grafana Docs**: https://grafana.com/docs/grafana/latest/
- **OpenTelemetry Java**: https://opentelemetry.io/docs/instrumentation/java/
- **Micrometer**: https://micrometer.io/
- **Grafana Alloy**: https://grafana.com/docs/alloy/latest/

---

## Version History

- **v1.0** (Feb 2025): Complete implementation with official Helm charts
  - Removed deprecated logging exporter
  - Added 4 Grafana dashboards
  - Spring Boot 4.0 + OpenTelemetry native support
  - Grafana Alloy log collection

---

**Last Updated**: Feb 7, 2025 | **Status**: Production Ready | **Next Review**: Q2 2025
