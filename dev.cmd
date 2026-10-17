@echo off
rem ===============================================================
rem  Elemental Verse - build / run helper
rem
rem  Why this script exists:
rem    1) This machine has no Java on PATH and no JAVA_HOME,
rem       but .minecraft ships a full JDK 21 (with javac).
rem    2) C:\Users\<you>\.gradle is not writable here, so the Gradle
rem       cache must be redirected into the workspace.
rem    3) Batch files must be ASCII + CRLF. Keep this file that way.
rem
rem  Usage (run from the repository root):
rem    dev.cmd build        compile, jar lands in build\libs\
rem    dev.cmd runClient    launch the game with the mod loaded
rem    dev.cmd runServer    launch a dedicated server with the mod
rem    dev.cmd data         run data generation
rem    dev.cmd clean        clean build outputs
rem    dev.cmd jar          package the jar only
rem
rem  Extra args are passed through:  dev.cmd build --offline
rem ===============================================================
setlocal

set "PROJ=%~dp0"
set "CACHE=%~dp0..\.dsh-cache"
set "GRADLE_HOME_DIR=%CACHE%\gradle"

rem ---------- 1) locate a JDK that has javac ----------
set "JDK="

if exist "%DSH_HOME%\dsh-runtimes\dsh-primary-runtime\dependencies\java\bin\javac.exe" (
  set "JDK=%DSH_HOME%\dsh-runtimes\dsh-primary-runtime\dependencies\java"
)
if not defined JDK if exist "%USERPROFILE%\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\java\bin\javac.exe" (
  set "JDK=%USERPROFILE%\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\java"
)

rem Fall back to the JDK bundled inside .minecraft (the usual case here).
if not defined JDK (
  for %%V in (java-runtime-delta java-runtime-epsilon java-runtime-gamma) do (
    if not defined JDK if exist "%APPDATA%\.minecraft\runtime\%%V\bin\javac.exe" (
      set "JDK=%APPDATA%\.minecraft\runtime\%%V"
    )
  )
)

rem Also accept an explicitly provided JAVA_HOME.
if not defined JDK if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JDK=%JAVA_HOME%"

if not defined JDK (
  echo [dev.cmd] No JDK 21 with javac found.
  echo [dev.cmd] Install JDK 21, or set JAVA_HOME to a JDK that has javac.exe.
  exit /b 1
)

set "JAVA_HOME=%JDK%"
set "PATH=%JDK%\bin;%PATH%"

rem ---------- 2) keep the Gradle cache inside the workspace ----------
if not exist "%GRADLE_HOME_DIR%" mkdir "%GRADLE_HOME_DIR%" >nul 2>&1
set "GRADLE_USER_HOME=%GRADLE_HOME_DIR%"

rem ---------- 3) misc ----------
set "GRADLE_OPTS=-Dfile.encoding=UTF-8 -Dorg.gradle.console=plain %GRADLE_OPTS%"

cd /d "%PROJ%"

rem ---------- 4) dispatch ----------
set "TASK=%~1"
if "%TASK%"=="" set "TASK=build"
shift

echo [dev.cmd] JAVA_HOME        = %JAVA_HOME%
echo [dev.cmd] GRADLE_USER_HOME = %GRADLE_USER_HOME%
echo [dev.cmd] task             = %TASK%
echo.

call gradlew.bat %TASK% %1 %2 %3 %4 %5 %6 %7 %8 %9
exit /b %ERRORLEVEL%
