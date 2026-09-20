#!/usr/bin/env bash
# ═════════════════════════════════════════════════════════════════════════════
# SchemeBridge One-Click Docker Compose Deployment Script
# ═════════════════════════════════════════════════════════════════════════════

set -e

echo "========================================================"
echo "  Starting SchemeBridge Production Deployment (Docker)  "
echo "========================================================"

# Check if docker is installed
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed or not in PATH."
    echo "Please install Docker from https://docs.docker.com/get-docker/ before continuing."
    exit 1
fi

# Check if docker compose is available
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
else
    echo "ERROR: Docker Compose is not installed."
    echo "Please install Docker Compose plugin."
    exit 1
fi

# Ensure .env exists, copy from .env.docker if missing
if [ ! -f .env ]; then
    echo "INFO: No .env found. Copying .env.docker -> .env"
    cp .env.docker .env
fi

echo "1. Pulling base images..."
$COMPOSE_CMD pull mongodb || true

echo "2. Building and starting all services in detached mode..."
$COMPOSE_CMD up --build -d

echo "3. Waiting for services to initialize..."
sleep 10

echo "4. Checking container status..."
$COMPOSE_CMD ps

echo ""
echo "========================================================"
echo "  SchemeBridge is live!"
echo "  Frontend Application : http://localhost"
echo "  Auth Service Health  : http://localhost:8080/actuator/health"
echo "  Scheme Service Health: http://localhost:8081/actuator/health"
echo "========================================================"
