# Observability Guide

EazyBank includes comprehensive observability using modern, production-grade tools:

- **Metrics**: Micrometer + Prometheus
- **Tracing**: OpenTelemetry with OTLP export
- **Logging**: Structured JSON with trace correlation
- **Visualization**: Grafana dashboards
- **Log Aggregation**: Grafana Loki + Alloy

## Local Development Setup

### Quick Start

Start the complete observability stack with the services:

```bash
cd deploy/dev
./build-images.sh        # Build all service images
docker compose up -d     # Start all services including observability stack
```

This starts:
- **4 Microservices**: Account (8080), Card (9000), Loan (8090), Gateway (8000)
- **Observability Stack**:
  - Prometheus: http://localhost:9090
  - Grafana: http://localhost:3000 (admin/admin)
  - Loki: http://localhost:3100
  - Alloy: http://localhost:12345
  - OTel Collector: http://localhost:4318

### Verify Services Are Healthy

```bash
# Check all services
curl http://localhost:8080/account/actuator/health
curl http://localhost:9000/card/actuator/health
curl http://localhost:8090/loan/actuator/health
curl http://localhost:8000/actuator/health

# Check Prometheus targets
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'

# Check Prometheus metrics
curl http://localhost:9090/api/v1/query?query=up | jq '.data.result'
```

## Accessing Dashboards

### Grafana

Open http://localhost:3000 and login with `admin`/`admin`.

Navigate to **"EazyBank"** folder to see available dashboards:

1. **JVM Metrics** - Heap memory, GC pause time, thread count, class loading
2. **HTTP Metrics** - Request rate, response latency (p95), error rate, status codes
3. **Circuit Breakers** - Circuit breaker states, failure rates, buffered calls

### Prometheus

Open http://localhost:9090 to query metrics directly.

**Example Queries:**

```promql
# JVM Memory Usage
jvm_memory_used_bytes{area="heap"}

# HTTP Request Rate
rate(http_server_requests_seconds_count[5m])

# Response Time (p95)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Circuit Breaker State (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
resilience4j_circuitbreaker_state

# Active Connections to Database
r2dbc_pool_acquired_connections
```

## Distributed Tracing

All requests are automatically traced end-to-end across services.

### View Traces in Logs

Generate a request to create a trace:

```bash
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "mobileNumber": "1234567890"
  }'
```

Check the logs from any service to find the `trace_id`:

```bash
docker logs eazybank-customergateway | grep trace_id
```

You'll see output like:
```json
{
  "timestamp": "2026-02-07T10:15:30.123Z",
  "level": "INFO",
  "service": "customer-gateway",
  "trace_id": "abc123def456",
  "span_id": "789ghi",
  "message": "Customer onboarded successfully"
}
```

### Query Logs by Trace ID

In Grafana, go to **Explore** > **Loki** and use LogQL:

```logql
{service=~".*"} | json | trace_id="abc123def456"
```

This will show all logs across all services for that trace, helping you see the full request journey:
- customer-gateway (entry point)
- account service
- card service
- loan service

## Key Metrics

### JVM Metrics

- `jvm_memory_used_bytes` - Heap/non-heap memory usage
- `jvm_gc_pause_seconds_sum` - GC pause time (cumulative)
- `jvm_gc_pause_seconds_count` - GC pause count
- `jvm_threads_live_threads` - Number of live threads
- `jvm_classes_loaded_classes` - Number of loaded classes

### HTTP Server Metrics

- `http_server_requests_seconds_count` - Total request count
- `http_server_requests_seconds_sum` - Total request time (for averaging)
- `http_server_requests_seconds_bucket` - Request latency histogram buckets
  - Labels: `application`, `method`, `status`, `uri`

Examples:
```promql
# Request rate per minute
rate(http_server_requests_seconds_count[1m])

# Error rate (5xx status)
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# Average response time
rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])

# P95 latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

### R2DBC Connection Pool Metrics

- `r2dbc_pool_acquired_connections` - Connections currently in use
- `r2dbc_pool_idle_connections` - Idle connections waiting
- `r2dbc_pool_pending_connections` - Requests waiting for connection

### Circuit Breaker Metrics (Gateway)

- `resilience4j_circuitbreaker_state` - Current state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `resilience4j_circuitbreaker_failure_rate` - Failure percentage (0-100)
- `resilience4j_circuitbreaker_buffered_calls` - Current buffered calls
  - Labels: `kind` = "failed" or "successful"
- `resilience4j_circuitbreaker_calls_total` - Total calls since startup
  - Labels: `outcome` = "success", "failure", "timeout"
- `resilience4j_circuitbreaker_not_permitted_calls_total` - Calls rejected when OPEN

## Logging

### JSON Structured Logging

In **production** and **staging** profiles, logs are output as JSON with full tracing context:

```json
{
  "timestamp": "2026-02-07T10:15:30.123Z",
  "level": "INFO",
  "service": "account",
  "trace_id": "abc123def456",
  "span_id": "789ghi",
  "thread": "reactor-http-epoll-5",
  "logger": "dev.sharanggupta.account.service.AccountServiceImpl",
  "message": "Account created successfully"
}
```

### Plain Text Logging (Development)

In **dev** profile, logs are human-readable:

```
2026-02-07 10:15:30.123 INFO [account,abc123def456,789ghi] --- [reactor-http-epoll-5] AccountServiceImpl : Account created successfully
```

### Enabling JSON Logs Locally

To test JSON logging locally:

```bash
SPRING_PROFILES_ACTIVE=prod docker compose up gateway
```

## Kubernetes Deployment

### Prerequisites

Deploy Prometheus Operator and Grafana to the `observability` namespace:

```bash
# Install Prometheus Operator
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus-operator prometheus-community/kube-prometheus-stack \
  -n observability --create-namespace

# Deploy Grafana with Loki backend
helm repo add grafana https://grafana.github.io/helm-charts
helm install loki grafana/loki-stack -n observability
```

### Service Metrics

Services expose Prometheus metrics at:

- Account: `http://account.default.svc.cluster.local:8080/account/actuator/prometheus`
- Card: `http://card.default.svc.cluster.local:9000/card/actuator/prometheus`
- Loan: `http://loan.default.svc.cluster.local:8090/loan/actuator/prometheus`
- Gateway: `http://customer-gateway.default.svc.cluster.local:8000/actuator/prometheus`

### ServiceMonitor Resources

Helm chart automatically creates ServiceMonitor resources when `observability.metrics.enabled=true`.

Prometheus Operator will discover these and start scraping:

```bash
kubectl get servicemonitors -A
kubectl describe servicemonitor -n default <service-name>
```

### Distributed Tracing

Configure the OTel Collector endpoint via Helm values:

```yaml
observability:
  tracing:
    enabled: true
    samplingRate: 0.1  # 10% in production
```

Services send traces to:
```
http://otel-collector.observability.svc.cluster.local:4318/v1/traces
```

## Performance Impact

- **Metrics Collection**: <1% CPU overhead, ~50MB memory
- **Distributed Tracing**: <2% CPU (with 10% sampling), ~100MB memory
- **JSON Logging**: ~10% serialization cost vs plain text (mitigated by async appenders)

**Total observability overhead**: ~5% CPU, ~200MB memory (acceptable for production)

## Troubleshooting

### No Metrics Appearing

**Problem**: Prometheus targets show DOWN or no metrics data

**Solution**:
1. Check actuator endpoint is exposed:
   ```bash
   curl http://localhost:8080/account/actuator/prometheus
   ```

2. Verify Prometheus config has correct paths (including context path):
   ```yaml
   metrics_path: '/account/actuator/prometheus'  # Don't forget context path!
   ```

3. Check service logs for actuator initialization errors:
   ```bash
   docker logs eazybank-account 2>&1 | grep -i actuator
   ```

### No Traces Being Collected

**Problem**: OTel Collector logs don't show "Traces received"

**Solution**:
1. Verify OpenTelemetry dependency is present:
   ```bash
   docker logs eazybank-account 2>&1 | grep -i opentelemetry
   ```

2. Check OTEL endpoint configuration:
   ```bash
   docker exec eazybank-account env | grep OTEL
   ```

3. Verify OTel Collector is running:
   ```bash
   curl http://localhost:4318/v1/traces -X POST -d '{}' 2>&1
   ```

### JSON Logs Not Appearing

**Problem**: Logs are plain text instead of JSON

**Solution**:
1. Check active profile:
   ```bash
   docker exec eazybank-account env | grep SPRING_PROFILES_ACTIVE
   ```

2. JSON logs only appear in `prod` or `staging` profiles:
   ```bash
   SPRING_PROFILES_ACTIVE=prod docker compose up account
   ```

3. Verify logstash-logback-encoder dependency:
   ```bash
   docker logs eazybank-account 2>&1 | grep -i logstash
   ```

### Circuit Breaker Metrics Missing

**Problem**: No `resilience4j_circuitbreaker_*` metrics

**Solution**:
1. Ensure `resilience4j-micrometer` dependency in customer-gateway pom.xml
2. Check circuit breaker is registered:
   ```bash
   curl http://localhost:8000/actuator/circuitbreakers
   ```

3. Trigger the circuit breaker:
   ```bash
   # Stop account service
   docker stop eazybank-account

   # Make requests through gateway
   for i in {1..20}; do
     curl http://localhost:8000/api/customer/1234567890
     sleep 1
   done

   # Check metrics now show OPEN state
   curl http://localhost:8000/actuator/prometheus | grep resilience4j
   ```

## Development Tips

### Monitor Circuit Breaker in Real-Time

Add this to your Grafana dashboard for real-time monitoring:

```promql
# Show current state with color: 0=green (CLOSED), 1=yellow (OPEN), 2=red (HALF_OPEN)
resilience4j_circuitbreaker_state{name="account_service"}

# Show failure rate trend
rate(resilience4j_circuitbreaker_calls_total{name="account_service",outcome="failure"}[1m])
```

### Generate Load and Watch Metrics

```bash
# Terminal 1: Generate load
while true; do
  curl -X POST http://localhost:8000/api/customer/onboard \
    -H "Content-Type: application/json" \
    -d '{"name":"Test","email":"test@example.com","mobileNumber":"9999999999"}' \
    -s > /dev/null
  sleep 0.5
done

# Terminal 2: Watch Prometheus metrics in real-time
watch 'curl -s "http://localhost:9090/api/v1/query?query=rate(http_server_requests_seconds_count%5B1m%5D)" | jq'
```

### Capture a Trace

```bash
# Get trace ID from logs
TRACE_ID=$(docker logs eazybank-customergateway 2>&1 | grep trace_id | head -1 | grep -o '"trace_id":"[^"]*' | cut -d'"' -f4)

# Query Loki for that trace
curl "http://localhost:3100/loki/api/v1/query?query=%7Bservice%3D%7E%22.*%22%7D%20%7C%20json%20%7C%20trace_id%3D%22$TRACE_ID%22" | jq
```

## References

- [Spring Boot Actuator Observability](https://docs.spring.io/spring-boot/reference/actuator/observability.html)
- [Micrometer Prometheus](https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Grafana Alloy Documentation](https://grafana.com/docs/alloy/latest/)
- [Grafana Loki Documentation](https://grafana.com/docs/loki/latest/)
