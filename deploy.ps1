# ═════════════════════════════════════════════════════════════════════════════
# SchemeBridge Deployment Script for Windows (PowerShell)
# ═════════════════════════════════════════════════════════════════════════════

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  SchemeBridge Automated Production Deployment         " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# Check if Docker is installed
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue

if ($dockerCmd) {
    Write-Host "[MODE] Docker detected. Starting Docker Compose deployment..." -ForegroundColor Green

    # Ensure .env exists
    if (-not (Test-Path ".env")) {
        Write-Host "INFO: Copying .env.docker -> .env" -ForegroundColor Yellow
        Copy-Item ".env.docker" ".env"
    }

    Write-Host "1. Building and starting containers..." -ForegroundColor Green
    docker compose up --build -d

    Start-Sleep -Seconds 8
    Write-Host "2. Checking container status..." -ForegroundColor Green
    docker compose ps

    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Cyan
    Write-Host "  SchemeBridge is deployed and live!                   " -ForegroundColor Cyan
    Write-Host "  Frontend: http://localhost                            " -ForegroundColor White
    Write-Host "  Auth API: http://localhost:8080/actuator/health       " -ForegroundColor White
    Write-Host "  Scheme API: http://localhost:8081/actuator/health     " -ForegroundColor White
    Write-Host "========================================================" -ForegroundColor Cyan
} else {
    Write-Host "[NOTICE] Docker is not installed on this Windows machine." -ForegroundColor Yellow
    Write-Host "You have two options:" -ForegroundColor White
    Write-Host "  Option A (Recommended for Cloud): Push to GitHub and run 'docker compose up -d' on your Linux/Cloud VPS." -ForegroundColor White
    Write-Host "  Option B (Local Production): Build JARs and run directly with local Java and MongoDB." -ForegroundColor White
    Write-Host ""
    Write-Host "For detailed instructions, please see DEPLOYMENT.md." -ForegroundColor Cyan
}
