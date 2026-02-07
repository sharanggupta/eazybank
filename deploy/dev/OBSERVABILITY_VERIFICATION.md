# Observability Stack Verification Guide

This guide provides step-by-step verification of the complete observability setup for EazyBank.

## Prerequisites

```bash
cd deploy/dev
./build-images.sh        # Build all service images
docker compose up -d     # Start all services
sleep 30                 # Wait for services to be ready
```

## Verification Checklist

### 1. Service Health Verification

Verify all 4 services are healthy:

```bash
echo "=== Checking Service Health ==="
curl -s http://localhost:8080/account/actuator/health | jq '.status'
curl -s http://localhost:9000/card/actuator/health | jq '.status'
curl -s http://localhost:8090/loan/actuator/health | jq '.status'
curl -s http://localhost:8000/actuator/health | jq '.status'
```

**Expected Output**: All services should return `"UP"`

### 2. Metrics Endpoint Verification

Verify Prometheus metrics are being exposed:

```bash
echo "=== Checking Metrics Endpoints ==="

# Account Service
curl -s http://localhost:8080/account/actuator/prometheus | head -20
echo ""

# Verify key metrics are present
curl -s http://localhost:8080/account/actuator/prometheus | grep "jvm_memory_used_bytes"
curl -s http://localhost:8080/account/actuator/prometheus | grep "http_server_requests_seconds"
```

**Expected Output**:
- Metrics data (lines starting with `#` for comments and metric names like `jvm_memory_used_bytes`)
- Should NOT return 404 or 403

### 3. Prometheus Targets Verification

Verify Prometheus has discovered all services:

```bash
echo "=== Checking Prometheus Targets ==="
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'
```

**Expected Output**:
```json
{
  "job": "account",
  "health": "up"
}
{
  "job": "card",
  "health": "up"
}
{
  "job": "loan",
  "health": "up"
}
{
  "job": "customer-gateway",
  "health": "up"
}
{
  "job": "otel-collector",
  "health": "up"
}
```

### 4. Prometheus Metrics Query Verification

Verify Prometheus can query metrics:

```bash
echo "=== Checking Prometheus Metrics ==="

# Query JVM memory usage
curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes' | jq '.data.result | length'

# Query HTTP request count
curl -s 'http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count' | jq '.data.result | length'

# Query circuit breaker state
curl -s 'http://localhost:9090/api/v1/query?query=resilience4j_circuitbreaker_state' | jq '.data.result | length'
```

**Expected Output**:
- At least 1 result for each query (non-zero numbers)

### 5. OpenTelemetry Collector Verification

Verify OTel Collector is receiving traces:

```bash
echo "=== Checking OTel Collector Logs ==="
docker logs eazybank-otel-collector 2>&1 | tail -30
```

**Expected Output**:
- Look for messages like "TracesReceiver" or "Metrics received"
- No error messages about connection refused

### 6. Generate Traffic and Verify Traces

Generate a sample request to create traces:

```bash
echo "=== Generating Sample Request ==="
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "mobileNumber": "9999999999"
  }' | jq '.'
```

**Expected Output**:
- HTTP 201 status
- JSON response with success message

### 7. Log Verification with Trace ID

Check that logs contain trace IDs:

```bash
echo "=== Checking Logs for Trace IDs ==="
docker logs eazybank-customergateway 2>&1 | grep -i trace | head -5
```

**Expected Output**:
- Logs should contain `"trace_id"` or `"traceId"` fields

### 8. Loki Log Ingestion Verification

Verify logs are being ingested into Loki:

```bash
echo "=== Checking Loki Logs ==="

# Query Loki for logs from customer-gateway
curl -s 'http://localhost:3100/loki/api/v1/query?query=%7Bservice%3D%22customer-gateway%22%7D' | jq '.data.result | length'

# Should return at least 1 result
if [[ $(curl -s 'http://localhost:3100/loki/api/v1/query?query=%7Bservice%3D%22customer-gateway%22%7D' | jq '.data.result | length') -gt 0 ]]; then
  echo "✓ Loki is receiving logs"
else
  echo "✗ Loki not receiving logs yet (may need to wait or generate more traffic)"
fi
```

**Expected Output**:
- At least 1 log entry from customer-gateway service

### 9. Grafana Dashboard Verification

Access Grafana and verify dashboards are available:

```bash
echo "=== Checking Grafana Datasources ==="

# Query Grafana for Prometheus datasource
curl -s http://localhost:3000/api/datasources -H "Authorization: Bearer $(curl -s -X POST http://localhost:3000/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"user":"admin","password":"admin"}' | jq -r '.token')" | jq '.[] | {name: .name, type: .type}'
```

Or manually:
1. Open http://localhost:3000
2. Login: `admin` / `admin`
3. Click on "Dashboards" > "EazyBank" folder
4. You should see:
   - JVM Metrics dashboard
   - HTTP Metrics dashboard
   - Circuit Breakers dashboard

### 10. Circuit Breaker Metrics Verification

Verify circuit breaker metrics are available:

```bash
echo "=== Checking Circuit Breaker Metrics ==="

# Query for circuit breaker state
curl -s 'http://localhost:9090/api/v1/query?query=resilience4j_circuitbreaker_state%7Bapplication%3D%22customer-gateway%22%7D' | jq '.data.result'

# Should show: account_service, card_service, loan_service
curl -s 'http://localhost:9090/api/v1/query?query=resilience4j_circuitbreaker_state' | jq '.data.result[].metric.name' | sort | uniq
```

**Expected Output**:
- Circuit breaker names: `account_service`, `card_service`, `loan_service`
- State values: `0` (CLOSED), `1` (OPEN), `2` (HALF_OPEN)

### 11. Trace Correlation Verification

Verify trace IDs are correlated across services:

```bash
echo "=== Checking Trace Correlation ==="

# Generate a request
RESPONSE=$(curl -s -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Trace Test User",
    "email": "tracetest@example.com",
    "mobileNumber": "8888888888"
  }')

echo "Response: $RESPONSE"

# Extract trace ID from logs
sleep 2  # Wait for logs to appear
TRACE_ID=$(docker logs eazybank-customergateway 2>&1 | grep -o '"trace_id":"[^"]*' | cut -d'"' -f4 | tail -1)

echo "Found Trace ID: $TRACE_ID"

if [ -n "$TRACE_ID" ]; then
  echo "Checking logs across services for this trace:"

  # Check gateway logs
  if docker logs eazybank-customergateway 2>&1 | grep -q "$TRACE_ID"; then
    echo "✓ Gateway has trace"
  fi

  # Check account logs
  if docker logs eazybank-account 2>&1 | grep -q "$TRACE_ID"; then
    echo "✓ Account service has trace"
  fi

  # Check card logs
  if docker logs eazybank-card 2>&1 | grep -q "$TRACE_ID"; then
    echo "✓ Card service has trace"
  fi

  # Check loan logs
  if docker logs eazybank-loan 2>&1 | grep -q "$TRACE_ID"; then
    echo "✓ Loan service has trace"
  fi
fi
```

**Expected Output**:
- Trace ID should appear in at least Gateway and Account service logs
- Checkmarks for all services that participated in the request

### 12. Circuit Breaker Degradation Test

Test circuit breaker behavior:

```bash
echo "=== Testing Circuit Breaker Degradation ==="

# Stop the account service to trigger circuit breaker
echo "Stopping account service..."
docker stop eazybank-account

# Wait a moment
sleep 5

# Make requests to trigger circuit breaker
echo "Making requests to trigger circuit breaker..."
for i in {1..15}; do
  curl -s http://localhost:8000/api/customer/1234567890 > /dev/null
  echo -n "."
done
echo ""

# Check circuit breaker metrics
echo "Checking circuit breaker state (should be OPEN = 1):"
curl -s 'http://localhost:9090/api/v1/query?query=resilience4j_circuitbreaker_state%7Bname%3D%22account_service%22%7D' | jq '.data.result[0].value'

# Check not permitted calls
echo "Checking not permitted calls (should be > 0):"
curl -s 'http://localhost:9090/api/v1/query?query=resilience4j_circuitbreaker_not_permitted_calls_total%7Bname%3D%22account_service%22%7D' | jq '.data.result[0].value'

# Restart account service
echo "Restarting account service..."
docker start eazybank-account
sleep 10

echo "✓ Circuit breaker test complete"
```

**Expected Output**:
- Circuit breaker state changes to 1 (OPEN)
- Not permitted calls increase
- After restart, state returns to 0 (CLOSED)

### 13. JSON Logging Verification (Production Profile)

Verify JSON structured logging works with production profile:

```bash
echo "=== Testing JSON Logging ==="

# Restart gateway with prod profile
echo "Testing with prod profile..."
SPRING_PROFILES_ACTIVE=prod docker compose up -d gateway

sleep 10

# Generate traffic
curl -s -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{
    "name": "JSON Log Test",
    "email": "jsonlog@example.com",
    "mobileNumber": "7777777777"
  }' > /dev/null

# Check logs are JSON format
echo "Checking for JSON formatted logs:"
docker logs eazybank-customergateway 2>&1 | grep -o '{"timestamp"' | head -1

if docker logs eazybank-customergateway 2>&1 | grep -q '{"timestamp"'; then
  echo "✓ JSON logging is active"
else
  echo "⚠ JSON logging not found (may need to wait for logs to appear)"
fi

# Return to dev profile
echo "Returning to dev profile..."
SPRING_PROFILES_ACTIVE=dev docker compose up -d gateway
```

**Expected Output**:
- Logs starting with `{"timestamp":"..."`
- Contains fields: `timestamp`, `level`, `service`, `trace_id`, `message`

### 14. Full Integration Test

Run a complete end-to-end test:

```bash
echo "=== Running Full Integration Test ==="

# 1. Create a customer
echo "1. Creating customer..."
RESPONSE=$(curl -s -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Integration Test User",
    "email": "integration@example.com",
    "mobileNumber": "6666666666"
  }')

echo "Response: $RESPONSE"

# 2. Fetch customer details
echo ""
echo "2. Fetching customer details..."
curl -s http://localhost:8000/api/customer/details/6666666666 | jq '.'

# 3. Wait and check metrics increased
sleep 5
echo ""
echo "3. Checking metrics increased..."
curl -s 'http://localhost:9090/api/v1/query?query=rate(http_server_requests_seconds_count%5B1m%5D)' | jq '.data.result[] | {service: .metric.application, rate: .value[1]}'

echo ""
echo "✓ Full integration test complete"
```

## Success Criteria

All of the following should be TRUE for observability to be fully functional:

- [ ] All 4 services report `status: UP`
- [ ] Prometheus has 5 active targets (account, card, loan, gateway, otel-collector)
- [ ] Prometheus can query `jvm_memory_used_bytes`, `http_server_requests_seconds_count`
- [ ] OTel Collector logs show traces being received
- [ ] Loki contains logs from all services
- [ ] Trace IDs are present in logs
- [ ] Traces are correlated across services
- [ ] Circuit breaker metrics available and respond to degradation
- [ ] Grafana dashboards display data
- [ ] JSON logging works in prod profile
- [ ] Complete request flow works end-to-end

## Troubleshooting

### No metrics in Prometheus

```bash
# Check if service is running
docker ps | grep eazybank

# Check actuator endpoint directly
curl http://localhost:8080/account/actuator/prometheus | head -20

# Check Prometheus target details
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.job=="account")'
```

### No traces in OTel Collector

```bash
# Check collector logs
docker logs eazybank-otel-collector

# Verify endpoint is accessible
curl -X POST http://localhost:4318/v1/traces -d '{}' -H 'Content-Type: application/json' -v
```

### Loki not receiving logs

```bash
# Check Alloy logs
docker logs eazybank-alloy

# Verify Loki is running
curl http://localhost:3100/loki/api/v1/labels

# Check docker socket mount
docker inspect eazybank-alloy | grep -A10 Mounts
```

## Cleanup

To stop all services:

```bash
docker compose down

# To remove volumes (database data)
docker compose down -v
```

## Next Steps

After verification:

1. Review [docs/observability.md](../../docs/observability.md) for detailed usage
2. Create Grafana alerts for critical metrics
3. Set up log retention policies in Loki
4. Configure ingestion rate limits in Loki
5. Test circuit breaker degradation scenarios
