[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$customersSql = Join-Path $PSScriptRoot 'seed-local-fake-customers.sql'
$shipmentsSql = Join-Path $PSScriptRoot 'seed-local-demo-shipments.sql'

Push-Location $repositoryRoot
try {
    docker compose exec -T postgres pg_isready -U gls -d gls_management | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'O PostgreSQL local não está disponível.' }

    Get-Content -Raw -LiteralPath $customersSql |
        docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U gls -d gls_management
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar os clientes locais.' }

    Get-Content -Raw -LiteralPath $shipmentsSql |
        docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U gls -d gls_management
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar os envios locais.' }

    Write-Host 'Dados locais criados: 200 clientes e 6 envios para destinos diferentes.' -ForegroundColor Green
}
finally {
    Pop-Location
}
