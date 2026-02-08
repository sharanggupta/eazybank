# End-to-End Trace Visualization Guide

This guide demonstrates how to use the complete observability stack to visualize requests flowing across microservices in Grafana.

## Quick Start

### 1. Start the Stack

```bash
cd deploy/dev
docker compose up -d
```

Wait for all services to be healthy:
```bash
docker compose ps
```

Expected output: All services should show "Up" status and be healthy.

### 2. Access Grafana

- **URL**: http://localhost:3000
- **Username**: admin
- **Password**: admin

You should see the Grafana dashboard. Navigate to **Explore** (left sidebar).

### 3. Verify Datasources Are Auto-Provisioned

Click the datasource dropdown (top-left of Explore page). You should see:
- ✅ **Prometheus** - metrics storage
- ✅ **Loki** - logs storage
- ✅ **Tempo** - distributed traces

If datasources are missing, check Docker logs:
```bash
docker compose logs grafana | grep -i datasource
```

---

## Generate a Test Request

### Option 1: Create a New Customer (Fanout to All Services)

This request will trigger calls to Account, Card, and Loan services:

```bash
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "mobileNumber": "9876543210"
  }'
```

Expected response: `201 Created` with success message.

### Option 2: Fetch Customer Details (Aggregates Multiple Services)

First, create an account using Option 1 above, then fetch it:

```bash
curl http://localhost:8000/api/customer/details/9876543210
```

This request will:
1. Hit the Customer Gateway
2. Gateway calls Account service for account details
3. Gateway calls Card service for card details
4. Gateway calls Loan service for loan details
5. Gateway aggregates and returns combined response

The trace will show all 4 services participating in a single logical request.

### Option 3: Direct Service Calls

Test individual services:

```bash
# Create account directly
curl -X POST http://localhost:8080/account/api \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "email": "jane@example.com",
    "mobileNumber": "9876543211"
  }'

# View Prometheus metrics
curl http://localhost:8080/account/actuator/prometheus | head -20
```

---

## Viewing Traces in Grafana

### Step 1: Navigate to Tempo in Explore

1. Go to http://localhost:3000/explore
2. Select **Tempo** from datasource dropdown (top-left)
3. Select **Search** tab

### Step 2: Search by Service Name

Under **Service Name**, select one of:
- `customer-gateway`
- `account`
- `card`
- `loan`

Click **Run Query** (or press Ctrl+Enter)

### Step 3: View the Trace Results

You'll see a list of traces. Click on any trace to view:
- **Trace ID**: Unique identifier for the entire request
- **Service Name**: Which service handled the span
- **Operation Name**: What was executed (e.g., "GET", "POST")
- **Duration**: How long the operation took
- **Status**: Success (green) or error (red)

### Step 4: Expand Trace Timeline

Click on a trace to see the full timeline showing:
- **Service dependency graph**: How requests flowed (gateway → account, gateway → card, etc.)
- **Span hierarchy**: Parent and child spans with timing information
- **Individual span details**: Hover over any span to see:
  - `http.method`: GET, POST, etc.
  - `http.url`: The request path
  - `http.status_code`: Response status (200, 201, 404, etc.)
  - `service.name`: Which service handled it

### Step 5: Trace-to-Logs Integration

Click on any span, then click **Show logs** (or similar) to jump to the structured logs for that span in Loki. This shows:
- Log messages from that service
- The same `traceId` and `spanId` values
- Timestamp correlation with the trace

### Step 6: Service Dependency Graph (Optional)

1. Still in Trace view, look for **Service Graph** or **Dependency Graph** tab
2. This shows visual connections between services
3. Hover over connections to see request volume and latency

---

## Example Trace: Customer Onboarding

When you run the **Option 1** request above, here's what you should see in the trace:

```
Trace ID: 4a8f5c3b2e1d9f7a
Duration: 245ms

├─ POST /api/customer/onboard (customer-gateway) [0-245ms]
│  ├─ POST /account/api (account service) [12-85ms]
│  ├─ POST /card/api (card service) [95-160ms]
│  └─ POST /loan/api (loan service) [170-220ms]
└─ Response aggregated [230-245ms]
```

Each line is clickable to see:
- HTTP method and path
- Service name
- Start and end time
- Response status code
- Request/response payloads (if captured)

---

## Troubleshooting

### Traces Not Appearing in Grafana

1. **Check OTel Collector is receiving traces**:
   ```bash
   docker compose logs otel-collector | tail -20
   ```
   Look for `ResourceSpans` in the logs - indicates traces were received.

2. **Check Tempo is running**:
   ```bash
   docker compose logs tempo | tail -20
   ```
   Should show Tempo server is listening on `:3200`.

3. **Check Grafana datasources**:
   ```bash
   curl http://localhost:3000/api/datasources
   ```
   Should return JSON with prometheus, loki, and tempo datasources.

4. **Test Tempo API directly**:
   ```bash
   curl http://localhost:3200/api/traces
   ```
   Should return list of available traces.

### Logs Not Appearing in Loki

1. **Check Alloy is running and collecting**:
   ```bash
   docker compose logs alloy | tail -20
   ```

2. **Query Loki directly**:
   ```bash
   curl 'http://localhost:3100/loki/api/v1/query?query={service="account"}'
   ```

### Services Not Communicating

1. **Check service logs**:
   ```bash
   docker compose logs account | grep -i error
   docker compose logs gateway | grep -i error
   ```

2. **Verify network connectivity**:
   ```bash
   docker compose exec account ping card
   docker compose exec gateway ping account
   ```

3. **Check actuator endpoints**:
   ```bash
   curl http://localhost:8080/account/actuator/health
   curl http://localhost:8000/actuator/health
   ```

---

## Key Metrics to Monitor

Once traces are flowing, check these in Prometheus (http://localhost:9090):

1. **Request Rate**:
   ```promql
   rate(http_server_requests_seconds_count[5m])
   ```

2. **P95 Latency**:
   ```promql
   histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
   ```

3. **Error Rate**:
   ```promql
   rate(http_server_requests_seconds_count{status=~"5.."}[5m])
   ```

4. **Circuit Breaker Status**:
   ```promql
   resilience4j_circuitbreaker_state{job="gateway"}
   ```

---

## Complete Test Scenario

Run this sequence to see full observability in action:

```bash
#!/bin/bash

echo "1. Creating customer (triggers multi-service request)..."
CUSTOMER=$(curl -s -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Test Customer $(date +%s)\",
    \"email\": \"test@example.com\",
    \"mobileNumber\": \"$(printf '%010d' $((RANDOM * 100)))\"
  }")

echo "Response: $CUSTOMER"
MOBILE=$(echo $CUSTOMER | grep -o '"[0-9]\{10\}"' | head -1 | tr -d '"')

echo -e "\n2. Fetching customer details (aggregates data from all services)..."
curl -X GET "http://localhost:8000/api/customer/details/$MOBILE" -H "Content-Type: application/json"

echo -e "\n\n3. Open Grafana: http://localhost:3000"
echo "4. In Explore (Tempo), search for traces with service_name='customer-gateway'"
echo "5. Click on the latest trace to see full request flow across services"
```

Save this as `test-trace-flow.sh`, make executable, and run:
```bash
chmod +x test-trace-flow.sh
./test-trace-flow.sh
```

---

## What You Should See

✅ **Trace ID in logs**: Check docker logs, you'll see traceId and spanId in JSON formatted logs

✅ **Traces in Grafana Tempo**: Navigate to Explore → Tempo → select service → run query

✅ **Service dependency**: Visual graph showing account ← gateway → card, loan

✅ **Span durations**: Each service's contribution to total request time

✅ **Trace-to-logs**: Click a span to see corresponding logs in Loki with same traceId

---

## Next Steps

- **Create custom dashboards**: In Grafana, create dashboards using Prometheus metrics
- **Set up alerts**: Configure alert rules in Prometheus for error rates, latency thresholds
- **Performance analysis**: Use traces to identify bottleneck services
- **Distributed context**: Validate that traceId propagates correctly across all services

## References

- [Grafana Tempo Documentation](https://grafana.com/docs/tempo/latest/)
- [OpenTelemetry Distributed Tracing](https://opentelemetry.io/docs/concepts/signals/traces/)
- [Spring Boot Observability](https://docs.spring.io/spring-boot/reference/actuator/observability.html)
