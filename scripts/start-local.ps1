[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runtimeDirectory = Join-Path $repositoryRoot "work\runtime"
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
Set-Location -LiteralPath $repositoryRoot

function Import-LocalEnvironment {
    $envFile = Join-Path $repositoryRoot ".env"
    if (-not (Test-Path -LiteralPath $envFile)) {
        return
    }
    foreach ($line in Get-Content -LiteralPath $envFile) {
        $trimmed = ([string]$line).Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }
        $parts = $trimmed.Split("=", 2)
        if ($parts.Count -eq 2 -and -not [string]::IsNullOrWhiteSpace($parts[0])) {
            [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
        }
    }
}

function Wait-Http([string]$Name, [string]$Url, [int]$TimeoutSeconds = 180) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri $Url -TimeoutSec 3 -UseBasicParsing
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                Write-Host "$Name disponível: $Url" -ForegroundColor Green
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)
    throw "$Name não ficou disponível. Consulte os logs em $runtimeDirectory."
}

function Test-Http([string]$Url) {
    try {
        $null = Invoke-WebRequest -Uri $Url -TimeoutSec 2 -UseBasicParsing
        return $true
    } catch {
        return $false
    }
}

function Test-Microservices {
    $healthUrls = @(
        "http://localhost:8081/actuator/health",
        "http://localhost:8082/actuator/health",
        "http://localhost:8083/actuator/health",
        "http://localhost:8084/actuator/health",
        "http://localhost:8085/actuator/health",
        "http://localhost:8086/actuator/health"
    )
    foreach ($healthUrl in $healthUrls) {
        if (-not (Test-Http $healthUrl)) {
            return $false
        }
    }
    return $true
}

Import-LocalEnvironment

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& docker info *> $null
$dockerInfoExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorActionPreference
if ($dockerInfoExitCode -ne 0) {
    throw "O Docker Engine não está acessível. Execute este script no PowerShell do utilizador Windows com o Docker Desktop ativo."
}

Write-Host "A iniciar PostgreSQL e Keycloak..." -ForegroundColor Cyan
& docker compose up --detach postgres keycloak
if ($LASTEXITCODE -ne 0) {
    throw "Não foi possível iniciar PostgreSQL e Keycloak."
}
Wait-Http "Keycloak" "http://localhost:8180/realms/ltft/.well-known/openid-configuration"

if (-not (Test-Http "http://localhost:8080/actuator/health")) {
    $localJava = Join-Path $repositoryRoot ".tools\jdk-21"
    if (Test-Path -LiteralPath $localJava) {
        $env:JAVA_HOME = (Resolve-Path -LiteralPath $localJava).Path
        $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    }

    $localMaven = Join-Path $repositoryRoot ".tools\maven\bin\mvn.cmd"
    $maven = if (Test-Path -LiteralPath $localMaven) {
        (Resolve-Path -LiteralPath $localMaven).Path
    } else {
        (Get-Command mvn.cmd -ErrorAction Stop).Source
    }

    Write-Host "A iniciar backend..." -ForegroundColor Cyan
    $backendProcess = Start-Process -FilePath $maven `
        -ArgumentList @("-f", "backend\pom.xml", "spring-boot:run") `
        -WorkingDirectory $repositoryRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $runtimeDirectory "backend.out.log") `
        -RedirectStandardError (Join-Path $runtimeDirectory "backend.err.log")
    Set-Content -LiteralPath (Join-Path $runtimeDirectory "backend.pid") -Value $backendProcess.Id
}
Wait-Http "Backend" "http://localhost:8080/actuator/health"

Write-Host "A iniciar microserviços e API Gateway..." -ForegroundColor Cyan
& docker compose up --detach --build --wait api-gateway
if ($LASTEXITCODE -ne 0) {
    Write-Host "O Docker assinalou um health check tardio. A confirmar diretamente os microserviços..." -ForegroundColor Yellow
    $microservicesDeadline = (Get-Date).AddSeconds(60)
    do {
        if (Test-Microservices) {
            Write-Host "Os microserviços estão disponíveis. A iniciar o API Gateway..." -ForegroundColor Green
            & docker compose up --detach --no-deps api-gateway
            if ($LASTEXITCODE -ne 0) {
                throw "Os microserviços estão disponíveis, mas não foi possível iniciar o API Gateway."
            }
            break
        }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $microservicesDeadline)

    if (-not (Test-Microservices)) {
        throw "Não foi possível iniciar os microserviços e o API Gateway. Consulte 'docker compose logs'."
    }
}
Wait-Http "API Gateway" "http://localhost:8090/actuator/health"

if (-not (Test-Http "http://localhost:5173")) {
    $frontendDirectory = Join-Path $repositoryRoot "frontend"
    $npm = (Get-Command npm.cmd -ErrorAction Stop).Source
    if (-not (Test-Path -LiteralPath (Join-Path $frontendDirectory "node_modules"))) {
        Write-Host "A instalar dependências do frontend..." -ForegroundColor Cyan
        Push-Location -LiteralPath $frontendDirectory
        try {
            & $npm install
            $installExitCode = $LASTEXITCODE
        } finally {
            Pop-Location
        }
        if ($installExitCode -ne 0) {
            throw "A instalação das dependências do frontend falhou."
        }
    }

    Write-Host "A iniciar frontend..." -ForegroundColor Cyan
    $frontendProcess = Start-Process -FilePath $npm `
        -ArgumentList @("run", "dev") `
        -WorkingDirectory $frontendDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $runtimeDirectory "frontend.out.log") `
        -RedirectStandardError (Join-Path $runtimeDirectory "frontend.err.log")
    Set-Content -LiteralPath (Join-Path $runtimeDirectory "frontend.pid") -Value $frontendProcess.Id
}
Wait-Http "Frontend" "http://localhost:5173"

Write-Host "" 
Write-Host "LTFT Comand Center está operacional." -ForegroundColor Green
Write-Host "Frontend:  http://localhost:5173"
Write-Host "Gateway:   http://localhost:8090/actuator/health"
Write-Host "Backend:   http://localhost:8080/actuator/health"
Write-Host "Keycloak:  http://localhost:8180/admin"
Write-Host "Logs:      $runtimeDirectory"
& docker compose ps
