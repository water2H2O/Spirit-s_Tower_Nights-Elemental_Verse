@echo off
rem ---------------------------------------------------------------
rem  Elemental Verse - 构建 / 运行 一键脚本
rem
rem  为什么需要它：
rem    1) 本机没装 Java、也没有 JAVA_HOME，但 .minecraft 里自带完整 JDK 21
rem    2) 沙箱/权限限制下 C:\Users\<你>\.gradle 写不进去，
rem       Gradle 缓存必须重定向到工作区里
rem    3) 中文路径与编码问题，统一在这里设好
rem
rem  用法（在仓库根目录）：
rem    dev.cmd build        编译，产物在 build\libs\
rem    dev.cmd runClient    启动带模组的客户端
rem    dev.cmd runServer    启动带模组的服务端
rem    dev.cmd data         跑数据生成
rem    dev.cmd clean        清理
rem    dev.cmd jar          只打包 jar
rem    dev.cmd tasks        看所有可用任务
rem
rem  透传参数：dev.cmd build --offline
rem ---------------------------------------------------------------
setlocal

set "PROJ=%~dp0"
set "CACHE=%~dp0..\.dsh-cache"
set "GRADLE_HOME_DIR=%CACHE%\gradle"

rem ---------- 1) 找 JDK 21 ----------
set "DEP=%DSH_HOME%\dsh-runtimes\dsh-primary-runtime\dependencies"
set "JDK="

rem 优先用 DSH 自带的运行时（如果里面有 java）
if exist "%DEP%\java\bin\javac.exe" set "JDK=%DEP%\java"
if not defined JDK if exist "%USERPROFILE%\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\java\bin\javac.exe" set "JDK=%USERPROFILE%\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\java"

rem 其次用 .minecraft 自带的 JDK（本机就是这一条生效）
if not defined JDK (
  for %%V in (java-runtime-delta java-runtime-epsilon java-runtime-gamma) do (
    if not defined JDK if exist "%APPDATA%\.minecraft\runtime\%%V\bin\javac.exe" set "JDK=%APPDATA%\.minecraft\runtime\%%V"
  )
)

if not defined JDK (
  echo [dev.cmd] 找不到带 javac 的 JDK 21。
  echo           请安装 JDK 21 并设置 JAVA_HOME，或用 -Dorg.gradle.java.home 指定。
  exit /b 1
)

set "JAVA_HOME=%JDK%"
set "PATH=%JDK%\bin;%PATH%"

rem ---------- 2) Gradle 缓存放进工作区 ----------
if not exist "%GRADLE_HOME_DIR%" mkdir "%GRADLE_HOME_DIR%" >nul 2>&1
set "GRADLE_USER_HOME=%GRADLE_HOME_DIR%"

rem ---------- 3) 杂项 ----------
set "GRADLE_OPTS=-Dfile.encoding=UTF-8 -Dorg.gradle.console=plain %GRADLE_OPTS%"

cd /d "%PROJ%"

rem ---------- 4) 分发 ----------
set "TASK=%1"
if "%TASK%"=="" set "TASK=build"
shift

echo [dev.cmd] JDK          = %JAVA_HOME%
echo [dev.cmd] GRADLE_USER_HOME = %GRADLE_USER_HOME%
echo [dev.cmd] 任务         = %TASK%

call gradlew.bat %TASK% %1 %2 %3 %4 %5 %6 %7 %8 %9
exit /b %ERRORLEVEL%
