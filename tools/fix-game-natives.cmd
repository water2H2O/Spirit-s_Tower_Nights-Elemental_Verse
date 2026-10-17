@echo off
rem ===============================================================
rem  Repair missing LWJGL native DLLs for a Minecraft instance.
rem
rem  Symptom: the game exits right after
rem           "Loading ImmediateWindowProvider fmlearlywindow"
rem           with no stack trace in the log (it dies in native code).
rem  Cause:   PCL extracts the *-natives-windows.jar files into
rem           <version>-natives\ but sometimes only writes one DLL
rem           (observed: only lwjgl.dll, missing glfw/opengl/...).
rem
rem  This wrapper runs the PowerShell script with -ExecutionPolicy
rem  Bypass, so it works even when script execution is disabled.
rem
rem  Usage:
rem    tools\fix-game-natives.cmd
rem    tools\fix-game-natives.cmd "D:\other\game" 1.21.1-NeoForge_21.1.262
rem
rem  NOTE: keep this file ASCII with CRLF line endings.
rem        cmd.exe reads batch files with the OEM codepage.
rem ===============================================================
setlocal

set "SCRIPT=%~dp0fix-game-natives.ps1"
set "GAMEDIR=%~1"
set "VERSION=%~2"
if "%GAMEDIR%"=="" set "GAMEDIR=D:\vibe_coding\mod"
if "%VERSION%"=="" set "VERSION=1.21.1-NeoForge_21.1.262"

if not exist "%SCRIPT%" (
  echo [x] Missing script: %SCRIPT%
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -GameDir "%GAMEDIR%" -Version "%VERSION%"
set "RC=%ERRORLEVEL%"

echo.
if "%RC%"=="0" (
  echo [OK] All native DLLs are in place. You can start the game now.
) else (
  echo [x] Repair incomplete ^(exit=%RC%^). See the messages above.
  echo     If this keeps happening, an antivirus is probably deleting
  echo     the extracted DLLs. Add this folder to its allowlist:
  echo     %GAMEDIR%\versions\%VERSION%\%VERSION%-natives
)
echo.
pause
exit /b %RC%
