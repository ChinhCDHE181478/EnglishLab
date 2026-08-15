#Requires -Version 5.1
param(
    [ValidateSet('status', 'snapshot', 'sheet', 'current')]
    [string]$Mode = 'status'
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$envFile = Join-Path $repoRoot 'backend\.env'
$backupDir = Join-Path $repoRoot 'database-backups'
$stateFile = Join-Path $backupDir 'sheet-toggle-state.json'
$currentDump = Join-Path $backupDir 'englishlab-current.dump'
$sheetDump = Join-Path $backupDir 'englishlab-sheet.dump'

if (-not (Test-Path $envFile)) {
    throw "Khong tim thay $envFile"
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

function Get-PostgresBin {
    $cmd = Get-Command pg_dump -ErrorAction SilentlyContinue
    if ($cmd -and $cmd.Source) {
        return Split-Path $cmd.Source -Parent
    }
    $direct = Get-ChildItem -Path 'C:\Program Files\PostgreSQL\*\bin\pg_dump.exe', 'C:\Program Files (x86)\PostgreSQL\*\bin\pg_dump.exe' -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1
    if ($direct) {
        return $direct.DirectoryName
    }
    throw "Khong tim thay pg_dump.exe. Them C:\Program Files\PostgreSQL\<version>\bin vao PATH."
}

function Read-DotEnv {
    param([string]$Path)
    $map = @{}
    Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $idx = $line.IndexOf('=')
        if ($idx -lt 1) { return }
        $map[$line.Substring(0, $idx).Trim()] = $line.Substring($idx + 1).Trim()
    }
    return $map
}

function Set-DotEnvValue {
    param(
        [string]$Path,
        [string]$Key,
        [string]$Value
    )
    $lines = Get-Content -LiteralPath $Path -Encoding UTF8
    $found = $false
    $updated = foreach ($line in $lines) {
        if ($line -match ('^\s*' + [regex]::Escape($Key) + '\s*=')) {
            $found = $true
            "$Key=$Value"
        } else {
            $line
        }
    }
    if (-not $found) {
        $updated = @($updated) + "$Key=$Value"
    }
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllLines($Path, [string[]]$updated, $utf8)
}

function Get-DbName {
    param([string]$JdbcUrl)
    if ($JdbcUrl -match 'jdbc:postgresql://[^/]+/([^?\s]+)') {
        return $Matches[1]
    }
    throw "Khong doc duoc ten database tu DB_URL"
}

function Get-JdbcUrlWithDb {
    param(
        [string]$JdbcUrl,
        [string]$DbName
    )
    return [regex]::Replace($JdbcUrl, '(jdbc:postgresql://[^/]+/)([^?\s]+)', "`${1}$DbName")
}

function Invoke-Psql {
    param(
        [string]$Psql,
        [string]$User,
        [string]$Database,
        [string]$Sql
    )
    & $Psql -U $User -d $Database -v ON_ERROR_STOP=1 -c $Sql
    if ($LASTEXITCODE -ne 0) { throw "psql failed for $Database" }
}

function Invoke-PgDump {
    param(
        [string]$PgDump,
        [string]$User,
        [string]$Database,
        [string]$OutFile
    )
    & $PgDump -U $User -Fc -d $Database -f $OutFile
    if ($LASTEXITCODE -ne 0) { throw "pg_dump failed for $Database" }
}

$envMap = Read-DotEnv $envFile
$jdbcUrl = $envMap['DB_URL']
$dbUser = $envMap['DB_USERNAME']
$dbPassword = $envMap['DB_PASSWORD']
if (-not $jdbcUrl -or -not $dbUser) {
    throw 'DB_URL hoac DB_USERNAME thieu trong backend/.env'
}

$pgBin = Get-PostgresBin
$pgDump = Join-Path $pgBin 'pg_dump.exe'
$psql = Join-Path $pgBin 'psql.exe'
if (-not (Test-Path $pgDump) -or -not (Test-Path $psql)) {
    throw "Thieu pg_dump/psql trong $pgBin"
}
Write-Host "Postgres tools=$pgBin"
$env:PGPASSWORD = $dbPassword
$env:PGCLIENTENCODING = 'UTF8'

$currentDb = Get-DbName $jdbcUrl
$sheetDb = 'englishlab_sheet'
if ($currentDb -eq $sheetDb) {
    $liveDb = 'englishlab'
} else {
    $liveDb = $currentDb
}

Write-Host "Mode=$Mode"
Write-Host "DB_URL database=$currentDb"
Write-Host "Current snapshot DB=$liveDb"
Write-Host "Sheet DB=$sheetDb"

if ($Mode -eq 'status') {
    Write-Host "APP_SEED_SHEET_ENABLED=$($envMap['APP_SEED_SHEET_ENABLED'])"
    Write-Host "APP_SEED_TEST_ENABLED=$($envMap['APP_SEED_TEST_ENABLED'])"
    Write-Host "current dump exists=$(Test-Path $currentDump)"
    Write-Host "sheet dump exists=$(Test-Path $sheetDump)"
    return
}

if ($Mode -eq 'snapshot') {
    Write-Host "Dumping $liveDb -> $currentDump"
    Invoke-PgDump -PgDump $pgDump -User $dbUser -Database $liveDb -OutFile $currentDump
    Write-Host 'Snapshot current xong.'
    return
}

if ($Mode -eq 'sheet') {
    if (-not (Test-Path $currentDump)) {
        Write-Host "Chua co snapshot. Dang dump $liveDb..."
        Invoke-PgDump -PgDump $pgDump -User $dbUser -Database $liveDb -OutFile $currentDump
    }

    $exists = & $psql -U $dbUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$sheetDb'"
    if ("$exists".Trim() -ne '1') {
        Invoke-Psql -Psql $psql -User $dbUser -Database 'postgres' -Sql "CREATE DATABASE $sheetDb"
    }

    $sheetUrl = Get-JdbcUrlWithDb -JdbcUrl $jdbcUrl -DbName $sheetDb
    Set-DotEnvValue -Path $envFile -Key 'DB_URL' -Value $sheetUrl
    Set-DotEnvValue -Path $envFile -Key 'APP_SEED_SHEET_ENABLED' -Value 'true'
    Set-DotEnvValue -Path $envFile -Key 'APP_SEED_TEST_ENABLED' -Value 'true'
    @{
        liveDb = $liveDb
        sheetDb = $sheetDb
        switchedAt = (Get-Date).ToString('s')
    } | ConvertTo-Json | Set-Content -LiteralPath $stateFile -Encoding UTF8

    Write-Host "Da chuyen .env sang database $sheetDb"
    Write-Host 'Khoi dong lai backend de JPA tao schema va seeder nap sheet data.'
    Write-Host 'Tai khoan demo: Password123!'
    Write-Host 'GV: alien1062004@gmail.com'
    Write-Host 'HV: 0386852628z@gmail.com'
    return
}

if ($Mode -eq 'current') {
    if ($currentDb -eq $sheetDb) {
        Write-Host "Dumping sheet DB truoc khi doi ve current..."
        try {
            Invoke-PgDump -PgDump $pgDump -User $dbUser -Database $sheetDb -OutFile $sheetDump
        } catch {
            Write-Warning 'pg_dump sheet that bai, van chuyen ve current.'
        }
    }
    if (-not (Test-Path $currentDump) -and $currentDb -eq $sheetDb) {
        throw "Chua co $currentDump. Khong the doi ve data cu."
    }
    $liveUrl = Get-JdbcUrlWithDb -JdbcUrl $jdbcUrl -DbName $liveDb
    Set-DotEnvValue -Path $envFile -Key 'DB_URL' -Value $liveUrl
    Set-DotEnvValue -Path $envFile -Key 'APP_SEED_SHEET_ENABLED' -Value 'false'
    Write-Host "Da chuyen .env ve database $liveDb"
    Write-Host 'Khoi dong lai backend. Data may hien tai duoc giu nguyen trong snapshot/DB cu.'
}
