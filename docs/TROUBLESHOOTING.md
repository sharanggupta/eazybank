# Troubleshooting Guide

Common issues and solutions for EazyBank development and deployment.

---

## Local Development (Docker Compose)

### Services Won't Start

**Symptom**: `docker compose up` fails with connection errors

**Check what's running**:
```bash
docker ps
docker compose ps
```

**Check logs**:
```bash
docker compose logs postgres  # Database
docker compose logs account   # Account service
docker compose logs gateway   # Gateway
```

**Solutions**:

1. **Ports already in use**:
   ```bash
   lsof -i :8000  # Find what's using port 8000
   lsof -i :5432  # Find what's using database port

   # Either stop that service or kill it
   kill -9 <PID>
   ```

2. **Docker daemon not running**:
   ```bash
   docker ps  # Should show running containers, not error

   # On Mac/Linux, start Docker Desktop or Docker daemon
   ```

3. **Stale containers/images**:
   ```bash
   docker compose down -v  # Remove data
   docker system prune      # Clean up
   docker compose up -d     # Start fresh
   ```

4. **Database schema not initialized**:
   ```bash
   # Check postgres logs
   docker compose logs postgres | grep -i error

   # Rebuild and restart
   docker compose down -v
   docker compose up -d
   ```

---

### Database Connection Refused

**Symptom**: Services fail with "connection refused" or "postgres: name or service not known"

**Check database is running**:
```bash
docker compose ps | grep postgres
# Should show: postgres ... Up
```

**Check connection**:
```bash
docker compose exec postgres psql -U postgres -l
# Lists all databases
```

**Verify network**:
```bash
docker network ls
docker inspect eazybank_default  # Default compose network
```

**Solutions**:

1. **Restart database**:
   ```bash
   docker compose restart postgres
   ```

2. **Check environment variables**:
   ```bash
   docker compose exec account env | grep SPRING_R2DBC
   # Should show: SPRING_R2DBC_URL=r2dbc:postgresql://postgres:5432/accountdb
   ```

3. **Force recreate**:
   ```bash
   docker compose down -v
   docker compose up -d
   ```

---

### Services Timeout on Health Check

**Symptom**: `docker compose up` hangs, containers don't become healthy

**Check health status**:
```bash
docker compose ps
# STATUS should show "healthy" not "starting"

# Or check health endpoint directly
curl http://localhost:8080/account/actuator/health
# Should return: {"status":"UP"}
```

**Check logs for startup errors**:
```bash
docker compose logs account | tail -50
# Look for error messages during startup
```

**Solutions**:

1. **Increase timeout** in docker-compose.yml:
   ```yaml
   healthcheck:
     test: ["CMD", "curl", "-f", "http://localhost:8080/account/actuator/health"]
     interval: 10s
     timeout: 5s
     retries: 30  # Increase from default
     start_period: 30s
   ```

2. **Wait for database** before services start:
   ```bash
   # Manually start postgres first
   docker compose up -d postgres
   sleep 10  # Wait for startup
   docker compose up -d account card loan gateway
   ```

---

### No Metrics in Grafana

**Symptom**: Grafana dashboards are empty, no metrics visible

**Wait for collection**:
```bash
# Prometheus needs 30-60 seconds to collect first metrics
# And you need to generate traffic (make API calls)
```

**Verify Prometheus is scraping**:
```bash
# Check targets
curl http://localhost:9090/api/v1/targets
# All targets should have "health": "up"

# Or open http://localhost:9090/targets in browser
```

**Verify services expose metrics**:
```bash
curl http://localhost:8080/account/actuator/prometheus
# Should return Prometheus format metrics
```

**Generate traffic**:
```bash
# Make API calls to generate metrics
for i in {1..5}; do
  curl http://localhost:8000/api/customer/1234567890
  sleep 1
done
```

**Check Prometheus logs**:
```bash
docker compose logs prometheus | grep -i error
```

**Solutions**:

1. **Restart Prometheus**:
   ```bash
   docker compose restart prometheus
   ```

2. **Check prometheus.yml** configuration:
   ```bash
   docker compose exec prometheus cat /etc/prometheus/prometheus.yml
   # Verify all targets are listed
   ```

3. **Reset Prometheus data**:
   ```bash
   docker compose down -v
   # Delete prometheus data volume
   docker compose up -d prometheus
   ```

---

### Services Can't Find Each Other

**Symptom**: Gateway gets 503 errors trying to reach Account/Card/Loan services

**Check services are running**:
```bash
docker compose ps
# All should show "Up"
```

**Verify DNS resolution**:
```bash
# From gateway container
docker compose exec gateway nslookup account
# Should resolve to IP address
```

**Check network connectivity**:
```bash
# From gateway, test account service
docker compose exec gateway curl http://account:8080/account/actuator/health
```

**Check service URLs in environment**:
```bash
docker compose exec gateway env | grep SERVICES
# Should show:
# SERVICES_ACCOUNT_URL=http://account:8080
# SERVICES_CARD_URL=http://card:9000
# SERVICES_LOAN_URL=http://loan:8090
```

**Solutions**:

1. **Verify compose file service names**:
   ```yaml
   # docker-compose.yml should have:
   services:
     account:
       ...
     card:
       ...
     loan:
       ...
     gateway:
       ...
   ```

2. **Ensure all services in same network**:
   ```bash
   docker network ls
   docker inspect eazybank_default
   # All services should be listed
   ```

3. **Restart all services**:
   ```bash
   docker compose down
   docker compose up -d
   ```

---

## Local Development (Hot-Reload)

### Application Won't Start

**Symptom**: `./mvnw spring-boot:run` fails or hangs

**Check Java version**:
```bash
java -version
# Should show Java 25+
```

**Check Maven**:
```bash
./mvnw --version
# Should work without errors
```

**Check database is running**:
```bash
# Ensure postgres is running before starting services
cd deploy/dev
docker compose up -d postgres
```

**Check dependencies**:
```bash
./mvnw clean install  # Download all dependencies
./mvnw spring-boot:run
```

**Check port availability**:
```bash
lsof -i :8080  # Check if Account port is free
lsof -i :8000  # Check if Gateway port is free
```

### Hot-Reload Not Working

**Symptom**: Make code changes but service doesn't reload

**Verify DevTools is enabled**:
```xml
<!-- In pom.xml, should have: -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

**Enable auto-build in IDE**:
- IntelliJ IDEA: File → Settings → Build, Execution, Deployment → Compiler → "Build project automatically"
- VS Code: Install "Spring Boot Extension Pack"

**Alternative - manual restart**:
```bash
# Stop the service (Ctrl+C)
# Start it again
./mvnw spring-boot:run
```

---

## Kubernetes Deployment

### Pod Won't Start (CrashLoopBackOff)

**Symptom**: Pod keeps restarting, never becomes Ready

**Check pod status**:
```bash
kubectl get pods -n eazybank-prod
# STATUS shows CrashLoopBackOff

# Get more details
kubectl describe pod <pod-name> -n eazybank-prod
```

**Check logs**:
```bash
kubectl logs <pod-name> -n eazybank-prod
# Look for error messages
```

**Common causes**:

1. **Database connection failed**:
   ```bash
   # Verify database is running
   kubectl get pods -n eazybank-prod | grep postgres

   # Check connection string
   kubectl set env pod/<pod-name> --list -n eazybank-prod | grep R2DBC
   ```

2. **Secret not found**:
   ```bash
   # Check secrets exist
   kubectl get secrets -n eazybank-prod

   # Create if missing
   kubectl create secret generic db-credentials \
     --from-literal=password='your-password' \
     -n eazybank-prod
   ```

3. **Image pull failed**:
   ```bash
   # Check image name is correct
   kubectl describe pod <pod-name> -n eazybank-prod | grep Image

   # Verify image exists in registry
   # Check authentication to registry
   ```

**Solutions**:

```bash
# Restart pod
kubectl delete pod <pod-name> -n eazybank-prod

# Or restart entire deployment
kubectl rollout restart deployment/account -n eazybank-prod

# Check logs after restart
kubectl logs -f deployment/account -n eazybank-prod
```

---

### Service Discovery Failed

**Symptom**: Services can't reach each other (503 errors), pods are running

**Check service DNS**:
```bash
# From a pod, test DNS resolution
kubectl exec -it <pod-name> -n eazybank-prod -- nslookup account
# Should resolve to ClusterIP

# From your machine
nslookup account.eazybank-prod.svc.cluster.local
```

**Verify services exist**:
```bash
kubectl get svc -n eazybank-prod
# All services (account, card, loan, gateway) should exist
```

**Check service selectors**:
```bash
kubectl describe svc account -n eazybank-prod
# Endpoints should show pod IPs
# If Endpoints is empty, selector might be wrong
```

**Solutions**:

```bash
# Verify deployment labels match service selector
kubectl get deployment account -n eazybank-prod -o yaml | grep labels

# Redeploy via Helm
helm upgrade account ./service-chart \
  -f ./deploy/helm/services/account/values.yaml \
  -f ./deploy/helm/services/account/environments/prod/app-values.yaml \
  -f ./deploy/helm/services/account/environments/prod/k8s-values.yaml \
  -n eazybank-prod
```

---

### Prometheus Targets DOWN

**Symptom**: Prometheus shows targets with health "DOWN"

**Check endpoints**:
```bash
# Port-forward to Prometheus
kubectl port-forward svc/prometheus 9090:9090 -n observability-prod

# Open http://localhost:9090/targets
# Targets should all show "UP"
```

**Check service metrics endpoint**:
```bash
# Port-forward to account service
kubectl port-forward svc/account 8080:8080 -n eazybank-prod

# Test metrics endpoint
curl http://localhost:8080/account/actuator/prometheus
# Should return metrics in Prometheus format
```

**Check service is healthy**:
```bash
kubectl get pods -n eazybank-prod
# Pods should show "Running" and "Ready"

# Check health
kubectl port-forward svc/account 8080:8080 -n eazybank-prod
curl http://localhost:8080/account/actuator/health
```

**Solutions**:

```bash
# Restart observability stack
kubectl delete pod -n observability-prod -l app=prometheus

# Or full redeploy
helm upgrade observability ./observability-chart \
  -f ./deploy/helm/observability-chart/values.yaml \
  -f ./deploy/helm/observability-chart/environments/prod/values.yaml \
  -n observability-prod
```

---

### Out of Memory / High CPU

**Symptom**: Pods are OOMKilled or using excessive CPU

**Check resource usage**:
```bash
kubectl top pods -n eazybank-prod
# Shows CPU and memory for each pod

kubectl top nodes
# Shows resource usage by node
```

**Check pod limits**:
```bash
kubectl describe pod <pod-name> -n eazybank-prod | grep -A 3 "Limits\|Requests"
```

**Check for issues**:

1. **Trace sampling too high** (default is 100%):
   ```yaml
   # In application.yaml, reduce sampling
   management:
     tracing:
       sampling:
         probability: 0.1  # 10% instead of 100%
   ```

2. **Observability stack retention too long**:
   ```bash
   # Check Prometheus/Loki retention
   kubectl get prometheus -n observability-prod -o yaml | grep retention

   # Reduce if needed (via Helm values)
   ```

3. **Pod limits too low**:
   ```bash
   # Increase limits in Helm values
   # deploy/helm/services/{service}/environments/prod/k8s-values.yaml
   ```

---

## Common Error Messages

### "Mobile number must be 10 digits"

**Cause**: Invalid phone number format

**Solution**: Use 10-digit number: `1234567890`

---

### "Customer already exists"

**Cause**: Trying to create customer with existing phone number

**Solution**: Use different phone number or delete existing customer first

---

### "Service unavailable - writes blocked"

**Cause**: Write Gate pattern detected a circuit breaker is open

**Solution**:
```bash
# Check which service is down
curl http://localhost:8000/actuator/circuitbreakers

# Restart the failing service
docker compose restart <service-name>
```

---

### "Connection refused"

**Cause**: Service not running or wrong port

**Solution**:
```bash
docker compose ps  # Verify service is running
docker compose logs <service-name>  # Check logs
```

---

## Health Checks

### Quick System Health

```bash
#!/bin/bash
echo "Checking EazyBank Health..."

echo "Gateway:"
curl -s http://localhost:8000/actuator/health | jq .status

echo "Account:"
curl -s http://localhost:8080/account/actuator/health | jq .status

echo "Card:"
curl -s http://localhost:9000/card/actuator/health | jq .status

echo "Loan:"
curl -s http://localhost:8090/loan/actuator/health | jq .status

echo "Prometheus:"
curl -s http://localhost:9090/-/healthy

echo "Grafana:"
curl -s http://localhost:3000/api/health | jq .database
```

### Detailed Diagnostics

```bash
# View application logs
docker compose logs gateway account card loan | grep ERROR

# Check database
docker compose exec postgres psql -U postgres -l

# Check network
docker network inspect eazybank_default

# Check Prometheus scrape targets
curl http://localhost:9090/api/v1/targets | jq '.data | length'
```

---

## More Information

- **Getting Started**: [GETTING_STARTED.md](GETTING_STARTED.md)
- **Configuration**: [CONFIGURATION.md](CONFIGURATION.md)
- **Deployment**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **Observability**: [observability.md](observability.md)
