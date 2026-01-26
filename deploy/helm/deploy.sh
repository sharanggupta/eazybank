#!/bin/bash
set -e

# EazyBank Helm Deployment Script
# Usage: ./deploy.sh <service> <environment> [--dry-run]

SERVICE=$1
ENVIRONMENT=$2
DRY_RUN=""

if [[ "$3" == "--dry-run" ]]; then
    DRY_RUN="--dry-run"
fi

if [[ -z "$SERVICE" || -z "$ENVIRONMENT" ]]; then
    echo "Usage: ./deploy.sh <service> <environment> [--dry-run]"
    echo ""
    echo "Services: account, card, loan"
    echo "Environments: dev, staging, prod"
    echo ""
    echo "Examples:"
    echo "  ./deploy.sh account dev"
    echo "  ./deploy.sh card staging --dry-run"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHART_PATH="$SCRIPT_DIR/service-chart"
VALUES_PATH="$SCRIPT_DIR/services/$SERVICE"
NAMESPACE="eazybank-$ENVIRONMENT"

# Validate paths exist
if [[ ! -d "$VALUES_PATH" ]]; then
    echo "Error: Service '$SERVICE' not found at $VALUES_PATH"
    exit 1
fi

if [[ ! -d "$VALUES_PATH/environments/$ENVIRONMENT" ]]; then
    echo "Error: Environment '$ENVIRONMENT' not found for service '$SERVICE'"
    exit 1
fi

echo "Deploying $SERVICE to $ENVIRONMENT (namespace: $NAMESPACE)"
echo ""

# Create namespace if it doesn't exist
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

# Deploy with Helm
helm upgrade --install "$SERVICE" "$CHART_PATH" \
    --namespace "$NAMESPACE" \
    --wait \
    --timeout 5m \
    -f "$VALUES_PATH/values.yaml" \
    -f "$VALUES_PATH/environments/$ENVIRONMENT/app-values.yaml" \
    -f "$VALUES_PATH/environments/$ENVIRONMENT/k8s-values.yaml" \
    $DRY_RUN

if [[ -z "$DRY_RUN" ]]; then
    echo ""
    echo "Deployment complete. Verifying rollout..."
    kubectl rollout status deployment/"$SERVICE" --namespace "$NAMESPACE" --timeout=5m
fi
