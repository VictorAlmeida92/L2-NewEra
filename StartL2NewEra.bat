@echo off
setlocal
title Lineage2 NewEra - Docker Runtime
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0StartL2NewEra.ps1" %*
if errorlevel 1 (
    echo.
    echo [ERRO] O comando do ambiente Lineage2 NewEra falhou.
    pause
)
endlocal
