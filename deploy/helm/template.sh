#!/bin/bash
set -e

# Render Helm templates without deploying (for validation)
# Usage: ./template.sh <service> <environment>

SERVICE=$1
ENVIRONMENT=$2

if [[ -z "$SERVICE" || -z "$ENVIRONMENT" ]]; then
    echo "Usage: ./template.sh <service> <environment>"
    echo ""
    echo "Renders templates without deploying. Useful for validation."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHART_PATH="$SCRIPT_DIR/service-chart"
VALUES_PATH="$SCRIPT_DIR/services/$SERVICE"
NAMESPACE="eazybank-$ENVIRONMENT"

helm template "$SERVICE" "$CHART_PATH" \
    --namespace "$NAMESPACE" \
    -f "$VALUES_PATH/values.yaml" \
    -f "$VALUES_PATH/environments/$ENVIRONMENT/app-values.yaml" \
    -f "$VALUES_PATH/environments/$ENVIRONMENT/k8s-values.yaml"
