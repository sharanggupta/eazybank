# Observability Stack - Quick Start

## ✅ Stack Status

All services are running and ready for testing:

- **Microservices**: Account (8080), Card (9000), Loan (8090), Gateway (8000)
- **Observability Stack**:
  - OpenTelemetry Collector: `http://localhost:4318` (receiving traces)
  - Grafana Tempo: `http://localhost:3200` (trace storage)
  - Prometheus: `http://localhost:9090` (metrics)
  - Loki: `http://localhost:3100` (logs)
  - Grafana: `http://localhost:3000` (visualization)

## 🚀 Generate Test Traces

### 1. Create a New Customer (Multi-Service Request)

```bash
MOBILE=$(printf "98%08d\n" $((RANDOM * 10000 % 100000000)))
curl -X POST http://localhost:8000/api/customer/onboard \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Test User\",\"email\":\"test@example.com\",\"mobileNumber\":\"$MOBILE\"}"
```

This request will:
1. Hit Customer Gateway (port 8000)
2. Gateway calls Account service (8080)
3. Gateway calls Card service (9000)
4. Gateway calls Loan service (8090)
5. All requests traced and exported to Tempo

### 2. Fetch Customer Details

```bash
# Use the mobile number from step 1
curl http://localhost:8000/api/customer/details/9876542771
```

## 📊 View Traces in Grafana

### Step 1: Open Grafana
- **URL**: http://localhost:3000
- **Username**: admin
- **Password**: admin

### Step 2: Navigate to Explore
- Click **Explore** in the left sidebar
- Select **Tempo** from the datasource dropdown (top-left)

### Step 3: Search for Traces
1. Click on **Service Name** field
2. Select `customer-gateway`, `account`, `card`, or `loan`
3. Click **Run Query** (or Ctrl+Enter)

### Step 4: View Trace Details
Click on any trace to see:
- **Service dependency chain**: How requests flowed through services
- **Span timing**: How long each operation took
- **HTTP attributes**: Method, URL, status code
- **Trace ID**: Click to view logs for this trace

## 🔗 Verify All Components

### Check Tempo Traces
```bash
# List available traces (check if any exist)
curl http://localhost:3200/api/traces
```

### Check Prometheus Metrics
```bash
# View service metrics
curl http://localhost:9090/api/v1/query?query=up
```

### Check Loki Logs
```bash
# Query logs from a service
curl 'http://localhost:3100/loki/api/v1/query?query={service="account"}'
```

### Check OTel Collector Status
```bash
# View collector metrics
curl http://localhost:4318/metrics
```

## 📝 Expected Trace Output

When viewing a trace for customer onboarding:

```
Trace ID: 4a8f5c3b2e1d9f7a
Duration: 245ms
Status: OK

Service Flow:
├─ customer-gateway POST /api/customer/onboard (0-245ms)
│  ├─ account POST /account/api (12-85ms)
│  ├─ card POST /card/api (95-160ms)
│  └─ loan POST /loan/api (170-220ms)
└─ Response sent (230-245ms)
```

Each service span shows:
- Service name
- HTTP method and path
- Response status (200, 201, etc.)
- Duration
- Trace ID and span ID

## 🔍 Troubleshooting

### Traces Not Appearing?
1. **Check OTel Collector is receiving**: `docker compose logs otel-collector | grep "ResourceSpans"`
2. **Verify Tempo has data**: `docker compose logs tempo | tail -20`
3. **Check services are exporting**: `docker compose logs account | grep -i trace | head -5`

### Grafana Datasources Missing?
1. Go to Grafana Settings → Data sources
2. Should see: Prometheus, Loki, Tempo
3. If missing, refresh page or restart Grafana: `docker compose restart grafana`

### Services Not Communicating?
1. Check container logs: `docker compose logs gateway` or `docker compose logs account`
2. Verify network: `docker compose exec gateway ping account`
3. Check actuator health: `curl http://localhost:8000/actuator/health`

## 📚 Full Documentation

For detailed setup and testing procedures, see: `TRACE_VISUALIZATION_GUIDE.md`

## 🎯 Success Criteria

✅ All services running (docker compose ps shows all Up)
✅ Can create customer via gateway API
✅ Traces appear in Tempo (Explore → Tempo)
✅ Multiple services visible in single trace
✅ Span durations and attributes visible
✅ Can correlate trace ID from logs and Tempo
