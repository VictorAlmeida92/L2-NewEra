[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet("init", "validate", "build", "up", "down", "restart", "status", "logs")]
    [string] $Action = "up",

    [switch] $SkipBuild,
    [switch] $Follow,
    [string[]] $Services = @()
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$composeFile = Join-Path $repoRoot "deploy\docker\docker-compose.yml"
$envFile = Join-Path $repoRoot ".env"
$envExample = Join-Path $repoRoot ".env.example"
$gradle = Join-Path $repoRoot "gradlew.bat"

function Assert-LastExitCode {
    param([string] $Description)
    if ($LASTEXITCODE -ne 0) {
        throw "$Description falhou com exit code $LASTEXITCODE."
    }
}

function Assert-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker nao foi encontrado no PATH. Instale/inicie o Docker Desktop."
    }
    & docker info --format "{{.ServerVersion}}" | Out-Null
    Assert-LastExitCode "Verificacao do Docker"
}

function Assert-EnvironmentFile {
    if (-not (Test-Path -LiteralPath $envFile)) {
        throw ".env nao encontrado. Execute './StartL2NewEra.ps1 init' e ajuste as credenciais antes de subir o stack."
    }
}

function Invoke-Compose {
    param([Parameter(Mandatory = $true)][string[]] $Arguments)
    & docker compose --env-file $envFile -f $composeFile @Arguments
    Assert-LastExitCode ("docker compose " + ($Arguments -join " "))
}

function Invoke-Build {
    Invoke-Compose @("build", "migrate", "login-server", "game-server")
}

function Invoke-Up {
    if (-not $SkipBuild) {
        Invoke-Build
    }
    Invoke-Compose @("up", "-d", "--wait", "--remove-orphans")
    Invoke-Compose @("ps")
}

Set-Location $repoRoot

if ($Action -eq "init") {
    if (Test-Path -LiteralPath $envFile) {
        Write-Host ".env ja existe; nenhuma alteracao foi feita."
        exit 0
    }
    Copy-Item -LiteralPath $envExample -Destination $envFile
    Write-Host ".env criado a partir de .env.example. Ajuste senhas e identidade do GameServer antes de usar um ambiente compartilhado."
    exit 0
}

Assert-Docker
Assert-EnvironmentFile

switch ($Action) {
    "validate" {
        Invoke-Compose @("config", "--quiet")
        & $gradle --no-daemon --no-parallel checkRuntimeScripts
        Assert-LastExitCode "Validacao dos scripts de runtime"
    }
    "build" {
        Invoke-Compose @("config", "--quiet")
        Invoke-Build
    }
    "up" {
        Invoke-Compose @("config", "--quiet")
        Invoke-Up
    }
    "down" {
        Invoke-Compose @("down", "--remove-orphans")
    }
    "restart" {
        Invoke-Compose @("down", "--remove-orphans")
        Invoke-Up
    }
    "status" {
        Invoke-Compose @("ps")
    }
    "logs" {
        $arguments = @("logs", "--tail", "200")
        if ($Follow) {
            $arguments += "--follow"
        }
        if ($Services.Count -eq 0) {
            $arguments += @("migrate", "login-server", "game-server")
        }
        else {
            $arguments += $Services
        }
        Invoke-Compose $arguments
    }
}
