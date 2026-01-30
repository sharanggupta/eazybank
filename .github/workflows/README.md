# GitHub Actions Workflow

Automated CI/CD pipeline for EazyBank.

## Deploy Workflow (`deploy.yml`)

Triggered on push to `main` branch (ignores documentation-only changes).

**Stages**:
1. **Build** — Tests and builds Docker images for all 4 services (account, card, loan, gateway)
2. **Deploy Staging** — Deploys to `eazybank-staging` namespace (automatic)
3. **Approval Gate** — Waits for manual approval (if production configured)
4. **Deploy Production** — Deploys to `eazybank-prod` namespace (requires approval)

The gateway is deployed without database secrets (it has no database). Backend services (account, card, loan) each receive database credentials.

## Required Secrets

**For Staging Only**:
```
KUBE_CONFIG    # Base64-encoded kubeconfig file
DB_PASSWORD    # Database password for backend services
```

**For Staging + Production**:
```
KUBE_CONFIG           # Staging kubeconfig (base64)
DB_PASSWORD           # Staging database password
KUBE_CONFIG_PROD      # Production kubeconfig (base64)
DB_PASSWORD_PROD      # Production database password
```

## Setup Instructions

See [README.md](../../README.md) for complete setup guide.

## Monitoring

Watch live logs on GitHub: https://github.com/sharanggupta/eazybank/actions

Check deployment status:
```bash
kubectl get pods -n eazybank-staging
kubectl get svc -n eazybank-staging
kubectl logs -f deployment/gateway -n eazybank-staging
```

Access the gateway (the only externally exposed service):
```bash
# Get the gateway NodePort
kubectl get svc gateway -n eazybank-staging -o jsonpath='{.spec.ports[0].nodePort}'

# Access via: http://CLUSTER_IP:NODE_PORT/swagger-ui.html
```

Backend services use ClusterIP and are only reachable within the cluster through the gateway. To debug directly, use port-forward:
```bash
kubectl port-forward svc/account 8080:8080 -n eazybank-staging &
curl http://localhost:8080/account/actuator/health
```
