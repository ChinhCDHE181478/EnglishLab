$ErrorActionPreference = 'Stop'

# The active Windows console must decode the UTF-8 bytes emitted by Spring/Logback.
chcp.com 65001 > $null
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8".Trim()

Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd spring-boot:run @args
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
