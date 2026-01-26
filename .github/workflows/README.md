# GitHub Actions Workflow

Automated CI/CD pipeline for EazyBank.

## Deploy Workflow (`deploy.yml`)

Triggered on push to `main` branch.

**Stages**:
1. **Build** - Tests and builds Docker images for all 3 services
2. **Deploy Staging** - Deploys to `eazybank-staging` namespace (automatic)
3. **Approval Gate** - Waits for manual approval (if production configured)
4. **Deploy Production** - Deploys to `eazybank-prod` namespace (requires approval)

## Required Secrets

**For Staging Only**:
```
KUBE_CONFIG    # Base64-encoded kubeconfig file
DB_PASSWORD    # Database password
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
kubectl logs -f deployment/account -n eazybank-staging
```

Find NodePort:
```bash
# Services are automatically exposed via NodePort
kubectl get svc account -n eazybank-staging -o jsonpath='{.spec.ports[0].nodePort}'
```
