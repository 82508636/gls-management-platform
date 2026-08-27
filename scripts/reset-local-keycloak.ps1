[CmdletBinding()]
param(
    [string]$PlatformAdminUsername = "ltft-admin",
    [string]$PlatformAdminEmail = "ltft-admin@localhost.local",
    [SecureString]$PlatformAdminPassword
)

$ErrorActionPreference = "Stop"
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $repositoryRoot

function Get-LocalSetting([string]$Name, [string]$DefaultValue) {
    $processValue = [Environment]::GetEnvironmentVariable($Name, "Process")
    if (-not [string]::IsNullOrWhiteSpace($processValue)) {
        return $processValue
    }

    $envFile = Join-Path $repositoryRoot ".env"
    if (Test-Path -LiteralPath $envFile) {
        $prefix = "$Name="
        $line = Get-Content -LiteralPath $envFile |
            Where-Object { ([string]$_).StartsWith($prefix, [StringComparison]::Ordinal) } |
            Select-Object -Last 1
        if ($line) {
            return $line.Substring($prefix.Length).Trim()
        }
    }

    return $DefaultValue
}

function Wait-Keycloak([int]$TimeoutSeconds = 180) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $null = Invoke-RestMethod -Uri "http://localhost:8180/realms/ltft/.well-known/openid-configuration" -TimeoutSec 3
            return
        } catch {
            Start-Sleep -Seconds 3
        }
    } while ((Get-Date) -lt $deadline)

    throw "O Keycloak não ficou disponível dentro de $TimeoutSeconds segundos. Consulte: docker compose logs keycloak"
}

function Find-KeycloakUser([hashtable]$Headers, [string]$Username, [int]$Attempts = 10) {
    $encodedUsername = [Uri]::EscapeDataString($Username)
    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        $response = Invoke-RestMethod -Headers $Headers `
            -Uri "http://localhost:8180/admin/realms/ltft/users?username=$encodedUsername&exact=true"
        $users = @($response | Where-Object {
            $null -ne $_ -and -not [string]::IsNullOrWhiteSpace([string]$_.id)
        })
        if ($users.Count -gt 0) {
            return $users
        }
        Start-Sleep -Milliseconds 500
    }
    return @()
}

function Get-KeycloakAdminHeaders([string]$Username, [string]$Password) {
    $tokenResponse = Invoke-RestMethod -Method Post `
        -Uri "http://localhost:8180/realms/master/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{ client_id = "admin-cli"; username = $Username; password = $Password; grant_type = "password" }
    if ([string]::IsNullOrWhiteSpace([string]$tokenResponse.access_token)) {
        throw "O Keycloak não devolveu um token administrativo."
    }
    return @{ Authorization = "Bearer $($tokenResponse.access_token)" }
}

if (-not (Test-Path -LiteralPath (Join-Path $repositoryRoot "compose.yaml"))) {
    throw "compose.yaml não encontrado na raiz do repositório."
}

& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "O Docker Engine não está acessível. Inicie o Docker Desktop e execute este script no PowerShell do utilizador Windows."
}

$composeConfig = (& docker compose config --format json 2>$null | Out-String | ConvertFrom-Json)
$projectName = $composeConfig.name
if ([string]::IsNullOrWhiteSpace($projectName)) {
    throw "Não foi possível determinar o nome do projeto Docker Compose."
}

$keycloakVolume = "${projectName}_keycloak-data"
$volumeNames = @(& docker volume ls --quiet)
$volumeExists = $volumeNames -contains $keycloakVolume
if ($volumeExists) {
    $volumeInspection = @(& docker volume inspect $keycloakVolume | Out-String | ConvertFrom-Json)
    if ($volumeInspection.Count -ne 1 -or $volumeInspection[0].Name -ne $keycloakVolume) {
        throw "Não foi possível confirmar com segurança o volume $keycloakVolume; a reposição foi cancelada."
    }
    $composeVolumeLabel = $volumeInspection[0].Labels.'com.docker.compose.volume'
    if (-not [string]::IsNullOrWhiteSpace($composeVolumeLabel) -and $composeVolumeLabel -ne "keycloak-data") {
        throw "O volume $keycloakVolume não tem a etiqueta esperada; a reposição foi cancelada."
    }

    Write-Host "A remover exclusivamente o volume local do Keycloak: $keycloakVolume" -ForegroundColor Yellow
    & docker compose stop keycloak
    & docker compose rm --force keycloak
    & docker volume rm $keycloakVolume
    if ($LASTEXITCODE -ne 0) {
        throw "Não foi possível remover o volume $keycloakVolume."
    }
} else {
    Write-Host "Não existe volume anterior do Keycloak; será criado um novo." -ForegroundColor Yellow
}

& docker compose up --detach keycloak
if ($LASTEXITCODE -ne 0) {
    throw "Não foi possível iniciar o Keycloak."
}
Wait-Keycloak

$bootstrapUsername = Get-LocalSetting "KEYCLOAK_ADMIN" "admin"
$bootstrapPassword = Get-LocalSetting "KEYCLOAK_ADMIN_PASSWORD" "admin_local"

if (-not $PlatformAdminPassword) {
    $PlatformAdminPassword = Read-Host "Password para o novo administrador da plataforma '$PlatformAdminUsername'" -AsSecureString
}

$headers = Get-KeycloakAdminHeaders -Username $bootstrapUsername -Password $bootstrapPassword

$requiredRoles = @("ADMIN", "OPERATOR", "ACCOUNTING", "CUSTOMER", "DRIVER", "FRONT_DESK")
$availableRoles = @(Invoke-RestMethod -Headers $headers -Uri "http://localhost:8180/admin/realms/ltft/roles")
$missingRoles = @($requiredRoles | Where-Object { $_ -notin $availableRoles.name })
if ($missingRoles.Count -gt 0) {
    throw "O realm foi importado sem os perfis obrigatórios: $($missingRoles -join ', ')"
}

$existingUsers = @(Find-KeycloakUser -Headers $headers -Username $PlatformAdminUsername -Attempts 1)
if ($existingUsers.Count -eq 0) {
    $newUser = @{
        username = $PlatformAdminUsername
        email = $PlatformAdminEmail
        firstName = "Administrador"
        lastName = "LTFT"
        enabled = $true
        emailVerified = $true
    } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8180/admin/realms/ltft/users" `
        -ContentType "application/json" -Body $newUser | Out-Null
    $existingUsers = @(Find-KeycloakUser -Headers $headers -Username $PlatformAdminUsername)
}

if ($existingUsers.Count -ne 1) {
    throw "Não foi possível identificar inequivocamente o novo administrador da plataforma."
}
$userId = $existingUsers[0].id
if ([string]::IsNullOrWhiteSpace([string]$userId)) {
    throw "O Keycloak devolveu um utilizador sem identificador; a password não foi alterada."
}

$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($PlatformAdminPassword)
try {
    $headers = Get-KeycloakAdminHeaders -Username $bootstrapUsername -Password $bootstrapPassword
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $credential = @{ type = "password"; value = $plainPassword; temporary = $false } | ConvertTo-Json
    Invoke-RestMethod -Method Put -Headers $headers `
        -Uri "http://localhost:8180/admin/realms/ltft/users/$userId/reset-password" `
        -ContentType "application/json" -Body $credential | Out-Null
} finally {
    $plainPassword = $null
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}

$headers = Get-KeycloakAdminHeaders -Username $bootstrapUsername -Password $bootstrapPassword
$adminRole = Invoke-RestMethod -Headers $headers -Uri "http://localhost:8180/admin/realms/ltft/roles/ADMIN"
Invoke-RestMethod -Method Post -Headers $headers `
    -Uri "http://localhost:8180/admin/realms/ltft/users/$userId/role-mappings/realm" `
    -ContentType "application/json" -Body (ConvertTo-Json -InputObject @($adminRole) -Depth 10) | Out-Null

Write-Host "Keycloak recriado com sucesso." -ForegroundColor Green
Write-Host "PostgreSQL preservado: o volume postgres-data não foi alterado."
Write-Host "Novo administrador da plataforma: $PlatformAdminUsername"
Write-Host "Perfis confirmados: $($requiredRoles -join ', ')"
