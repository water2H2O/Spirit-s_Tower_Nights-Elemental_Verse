@echo off
rem ===============================================================
rem  Install the built mod jar into the test Minecraft instance.
rem
rem  WHY THIS SCRIPT EXISTS
rem    Replacing a mod jar while Minecraft is running makes the JVM
rem    load classes from a file whose bytes have already changed.
rem    The classic symptom (seen 2026-10-20 20:28) is:
rem
rem      java.lang.NoClassDefFoundError:
rem          com/water2h2o/elementalverse/menu/RitualAltarMenu$1
rem      Caused by: java.lang.ClassNotFoundException: ...RitualAltarMenu$1
rem
rem    ...even though that .class file IS inside the jar. Nothing is
rem    wrong with the build: the game just needs a restart.
rem
rem  This script refuses to copy while javaw.exe / java.exe is running.
rem
rem  Usage:
rem    tools\install-jar.cmd
rem    tools\install-jar.cmd "D:\other\game" 1.21.1-NeoForge_21.1.262
rem
rem  NOTE: keep this file ASCII with CRLF line endings.
rem ===============================================================
setlocal

set "ROOT=%~dp0.."
set "SRC=%ROOT%\dist\elemental_verse-0.1.0.jar"
set "GAMEDIR=%~1"
set "VERSION=%~2"
if "%GAMEDIR%"=="" set "GAMEDIR=D:\vibe_coding\mod"
if "%VERSION%"=="" set "VERSION=1.21.1-NeoForge_21.1.262"
set "DST=%GAMEDIR%\versions\%VERSION%\mods\elemental_verse-0.1.0.jar"
set "WINROOT=%SystemRoot%"

if not exist "%SRC%" (
  echo [x] Not built yet: %SRC%
  echo     Run: dev.cmd build
  exit /b 1
)

rem --- refuse to touch the jar while the game is open ---
set "RUNNING="
for %%P in (javaw.exe java.exe) do (
  rem Gradle daemons are java.exe too, so match the window title.
  tasklist /V /FI "IMAGENAME eq %%P" 2>nul | "%WINROOT%\System32\find.exe" /I "Minecraft" >nul && set "RUNNING=1"
)
if defined RUNNING (
  echo [x] Minecraft is still running - close the game first.
  echo     Replacing the jar under a live JVM causes
  echo     NoClassDefFoundError on the next class it loads.
  exit /b 2
)

if not exist "%GAMEDIR%\versions\%VERSION%\mods" (
  echo [x] No mods folder: %GAMEDIR%\versions\%VERSION%\mods
  exit /b 1
)

rem --- natives 自检：杀毒软件会反复删掉解压出来的 DLL ---
set "NATIVES=%GAMEDIR%\versions\%VERSION%\%VERSION%-natives"
set "DLLCOUNT=0"
if exist "%NATIVES%\lwjgl_opengl.dll" set "DLLCOUNT=1"
if exist "%NATIVES%\lwjgl_stb.dll" set "DLLCOUNT=2"
if exist "%NATIVES%\lwjgl_tinyfd.dll" set "DLLCOUNT=3"
if exist "%NATIVES%\OpenAL.dll" set "DLLCOUNT=4"
if exist "%NATIVES%\jemalloc.dll" set "DLLCOUNT=5"
if exist "%NATIVES%\freetype.dll" set "DLLCOUNT=6"
if "%DLLCOUNT%"=="6" goto natives_ok
echo [!] Native DLLs are missing - repairing before install...
call "%~dp0fix-game-natives.cmd" "%GAMEDIR%" "%VERSION%" <nul
:natives_ok

copy /Y "%SRC%" "%DST%" >nul
if errorlevel 1 (
  echo [x] Copy failed: %DST%
  exit /b 1
)

echo [OK] Installed:
echo      %DST%
echo     Start the game now.
exit /b 0
