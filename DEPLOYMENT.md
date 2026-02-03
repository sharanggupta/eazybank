# Deployment Reference

Practical guide for managing EazyBank deployments. For initial setup, see [README.md](README.md).

## Common Deployment Tasks

### Deploy Updates (Push to Main)

```bash
# Edit code
nano customergateway/src/main/java/...

# Test locally
cd customergateway && ./mvnw test

# Commit and push
git commit -am "Update gateway service"
git push origin main

# All 4 services automatically deploy to eazybank-staging
# Watch: https://github.com/sharanggupta/eazybank/actions
```

### Check Deployment Status

**Via GitHub Actions**:
- https://github.com/sharanggupta/eazybank/actions
- Click running workflow to see live logs

**Via kubectl**:
```bash
kubectl get pods -n eazybank-staging
kubectl get svc -n eazybank-staging
kubectl describe pod POD_NAME -n eazybank-staging
```

### View Logs

```bash
# Gateway
kubectl logs -f deployment/customergateway -n eazybank-staging

# Backend services
kubectl logs -f deployment/account -n eazybank-staging
kubectl logs -f deployment/card -n eazybank-staging
kubectl logs -f deployment/loan -n eazybank-staging

# PostgreSQL logs
kubectl logs -f statefulset/account-postgresql -n eazybank-staging
```

### Scale a Service

```bash
# Edit values for permanent scaling
nano deploy/helm/services/customergateway/environments/staging/k8s-values.yaml
# Change: k8s.replicas: N

# Or scale immediately
kubectl scale deployment customergateway --replicas=3 -n eazybank-staging
```

### Access the Gateway

The gateway is the only externally exposed service (NodePort). Backend services use ClusterIP and are only reachable within the cluster.

```bash
# Get the customergateway NodePort
kubectl get svc customergateway -n eazybank-staging

# Example output:
# NAME      TYPE       CLUSTER-IP   EXTERNAL-IP   PORT(S)          AGE
# customergateway   NodePort   10.x.x.x    <none>        8000:31234/TCP   5m
#
# Access via: http://CLUSTER_IP:31234/swagger-ui.html

# Backend services (ClusterIP — debug via port-forward)
kubectl port-forward svc/account 8080:8080 -n eazybank-staging &
curl http://localhost:8080/account/actuator/health
```

### Rollback a Deployment

```bash
# View history
helm history customergateway -n eazybank-staging

# Rollback to previous version
helm rollback customergateway 1 -n eazybank-staging
```

### Approve Production Deployment

After staging deployment completes, if production is configured:

```bash
# Via GitHub Actions UI
1. Go to https://github.com/sharanggupta/eazybank/actions
2. Click the running workflow
3. Click "Review deployments"
4. Select "production" environment
5. Click "Approve and deploy"

# OR via GitHub CLI
gh run list --workflow deploy.yml --status waiting
gh run view RUN_ID --log
```

## Cleanup

### Delete Services (Keep Infrastructure)

```bash
kubectl delete namespace eazybank-staging
```

### Delete Everything

Stop all charges by terminating infrastructure via your cloud provider's console.

## Troubleshooting

### Pods Stuck in ImagePullBackOff

**Cause**: Docker image not built or pushed yet.
**Fix**:
- Check GitHub Actions: https://github.com/sharanggupta/eazybank/actions
- Ensure "Build" job succeeded
- Verify images exist: https://github.com/sharanggupta/eazybank/pkgs/container

### Pods Stuck in Pending

**Cause**: Insufficient resources or PVC not binding.
**Fix**:
```bash
kubectl describe pod POD_NAME -n eazybank-staging
# Check Events section for details
```

### Connection Refused / Cannot Access Gateway

**Cause**: Kubeconfig server IP is 127.0.0.1 (localhost).
**Fix**: Edit kubeconfig to use actual server IP:
```bash
kubectl config view  # Check current context
# Edit to replace 127.0.0.1 with actual cluster IP
```

### Workflow Won't Trigger on Push

**Cause**: Branch protection or workflow disabled.
**Fix**:
- Ensure pushing to `main` branch
- Check: Settings → Actions → Runners
- Manually trigger: Actions → Build and Deploy → Run workflow

### GitHub Secrets Not Being Used

**Cause**: Secrets not added or typo in name.
**Fix**:
```bash
# Verify secrets exist
# Settings → Secrets and variables → Actions
# Ensure exact names: KUBE_CONFIG, DB_PASSWORD
```

## More Information

- **Setup**: See [README.md](README.md)
- **Local Development**: See [deploy/dev/README.md](deploy/dev/README.md)
- **Kubernetes Details**: See [deploy/helm/README.md](deploy/helm/README.md)
- **Workflow Details**: See [.github/workflows/README.md](.github/workflows/README.md)
