#!/bin/bash

# Build Docker images for all EazyBank microservices using Jib
# Run this script from the deploy/dev directory

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/../.."

echo "Building Docker images for EazyBank microservices..."

echo ""
echo "Building account service..."
cd "$ROOT_DIR/account"
./mvnw compile jib:dockerBuild -q

echo ""
echo "Building card service..."
cd "$ROOT_DIR/card"
./mvnw compile jib:dockerBuild -q

echo ""
echo "Building loan service..."
cd "$ROOT_DIR/loan"
./mvnw compile jib:dockerBuild -q

echo ""
echo "All images built successfully:"
docker images | grep eazybank
