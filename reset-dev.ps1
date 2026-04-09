# reset-dev.ps1 — Full development environment reset for EventManagement
# Usage: .\reset-dev.ps1
#
# This script:
#   1. Stops all Docker containers
#   2. Removes the PostgreSQL data volume (ALL DATA IS LOST)
#   3. Rebuilds and restarts all services
#   4. Waits for startup and shows backend logs
#
# Use this when:
#   - Flyway checksum mismatch errors occur
#   - You want a completely fresh database
#   - Testing the full migration chain from scratch

param(
    [switch]$Force  # Skip confirmation prompt
)

Write-Host ""
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  EventManagement Dev Environment Reset " -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "This will DELETE all data in your local PostgreSQL database!" -ForegroundColor Red

if (-not $Force) {
    $confirm = Read-Host "Type 'yes' to continue"
    if ($confirm -ne 'yes') {
        Write-Host "Aborted." -ForegroundColor Red
        exit 1
    }
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $projectRoot

try {
    Write-Host "`n[1/4] Stopping containers..." -ForegroundColor Cyan
    docker-compose down
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Warning: docker-compose down returned non-zero exit code" -ForegroundColor Yellow
    }

    Write-Host "`n[2/4] Removing database volume..." -ForegroundColor Cyan
    # Docker Compose v1 uses underscore, v2 uses hyphen — try both
    $volumes = docker volume ls --format "{{.Name}}" | Where-Object { $_ -match "eventmanagement.*postgres" }
    if ($volumes) {
        foreach ($vol in $volumes) {
            Write-Host "  Removing volume: $vol" -ForegroundColor Gray
            docker volume rm $vol 2>$null
        }
    } else {
        Write-Host "  No matching volumes found (this is OK for first run)" -ForegroundColor Gray
    }

    Write-Host "`n[3/4] Rebuilding and starting containers..." -ForegroundColor Cyan
    docker-compose up --build -d
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: docker-compose up failed!" -ForegroundColor Red
        exit 1
    }

    Write-Host "`n[4/4] Waiting for backend startup (30 seconds)..." -ForegroundColor Cyan
    Start-Sleep -Seconds 30

    Write-Host "`n--- Backend Logs (last 40 lines) ---" -ForegroundColor Cyan
    docker logs eventmanagement-backend --tail 40

    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "  Reset complete!                       " -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Check the logs above for:" -ForegroundColor White
    Write-Host '  - "Successfully applied 6 migration(s)"' -ForegroundColor White
    Write-Host '  - "Started EventManagementApplication"' -ForegroundColor White
    Write-Host ""
} finally {
    Pop-Location
}
