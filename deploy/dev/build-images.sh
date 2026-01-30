#!/bin/bash

# Build Docker images for all EazyBank microservices using Jib
# This script compiles, tests (including integration tests), and builds images for all services
# Run this script from the deploy/dev directory

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/../.."

build_service() {
    local service=$1
    local service_name=$2

    echo ""
    echo "Building $service_name service..."
    cd "$ROOT_DIR/$service"

    # Build and compile, running all tests including integration tests
    ./mvnw clean package -q || {
        echo "❌ Build failed for $service_name (compilation or tests)"
        exit 1
    }

    # Build Docker image using Jib
    ./mvnw jib:dockerBuild -q || { echo "❌ Docker image build failed for $service_name"; exit 1; }

    echo "✓ $service_name service built successfully"
}

echo "Building Docker images for EazyBank microservices..."

build_service "account" "Account"
build_service "card" "Card"
build_service "loan" "Loan"
build_service "gateway" "Gateway"

echo ""
echo "✓ All images built successfully:"
docker images | grep eazybank
