@echo off
setlocal

rem Keep the Windows console and Spring Boot logs on the same UTF-8 encoding.
chcp 65001 >nul
set "JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS% -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"

pushd "%~dp0"
call mvnw.cmd spring-boot:run %*
set "EXIT_CODE=%ERRORLEVEL%"
popd

exit /b %EXIT_CODE%
