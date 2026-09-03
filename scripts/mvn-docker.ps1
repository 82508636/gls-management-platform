[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArguments = @("clean", "test")
)

$ErrorActionPreference = "Stop"
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

if ($MavenArguments.Count -eq 0) {
    $MavenArguments = @("clean", "test")
}

$docker = (Get-Command docker -ErrorAction Stop).Source

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& $docker info *> $null
$dockerInfoExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorActionPreference

if ($dockerInfoExitCode -ne 0) {
    throw "O Docker Engine não está acessível. Inicie o Docker Desktop e volte a executar o comando."
}

Write-Host "Maven 3.9.11 / Java 21 (Docker)" -ForegroundColor Cyan
Write-Host "Projeto: $repositoryRoot"
Write-Host "Goals:   $($MavenArguments -join ' ')"

$dockerArguments = @(
    "run",
    "--rm",
    "--volume", "${repositoryRoot}:/workspace",
    "--volume", "ltft-maven-cache:/root/.m2",
    "--workdir", "/workspace",
    "maven:3.9.11-eclipse-temurin-21",
    "mvn",
    "--batch-mode"
) + $MavenArguments

& $docker @dockerArguments
$mavenExitCode = $LASTEXITCODE

if ($mavenExitCode -ne 0) {
    throw "O Maven terminou com o código $mavenExitCode."
}

Write-Host "Maven concluído com sucesso." -ForegroundColor Green
