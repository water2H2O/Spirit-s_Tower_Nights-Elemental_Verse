<#
.SYNOPSIS
    修复 Minecraft 实例缺失的 native DLL（LWJGL）。

.DESCRIPTION
    PCL 启动时会把 libraries 里的 *-natives-windows.jar 解压进
    <版本>-natives\ 目录。这台机器上这一步**反复只解出 1 个文件**
    （只出 lwjgl.dll，少了 glfw/opengl/freetype/…），
    结果是游戏在 "Loading ImmediateWindowProvider fmlearlywindow" 处直接退出，
    日志里没有任何异常栈 —— 因为崩在原生层。

    本脚本直接从 libraries 里的 jar 重新解压全部 DLL，可反复执行。

.PARAMETER GameDir
    游戏目录（含 versions 与 libraries 的那一层）。默认用工作区里那个实例。

.PARAMETER Version
    版本文件夹名。默认 1.21.1-NeoForge_21.1.262。

.EXAMPLE
    pwsh -File tools\fix-game-natives.ps1
#>
[CmdletBinding()]
param(
    [string]$GameDir = 'D:\vibe_coding\mod',
    [string]$Version = '1.21.1-NeoForge_21.1.262'
)

$ErrorActionPreference = 'Stop'

$versionDir = Join-Path $GameDir "versions\$Version"
$natives = Join-Path $versionDir "$Version-natives"
$libraries = Join-Path $GameDir 'libraries'

if (-not (Test-Path $versionDir)) {
    Write-Host "[!] 找不到版本目录：$versionDir" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $libraries)) {
    Write-Host "[!] 找不到 libraries：$libraries" -ForegroundColor Red
    exit 1
}

# 这里要的 DLL：LWJGL 3.3.3 在 1.21.1 上的全套
$expected = @(
    'lwjgl.dll', 'glfw.dll', 'lwjgl_opengl.dll', 'freetype.dll',
    'jemalloc.dll', 'OpenAL.dll', 'lwjgl_stb.dll', 'lwjgl_tinyfd.dll'
)

Write-Host "[*] 版本目录：$versionDir"
Write-Host "[*] natives  ：$natives"

if (-not (Test-Path $natives)) {
    New-Item -ItemType Directory -Path $natives -Force | Out-Null
    Write-Host "[*] 已创建 natives 目录"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

$jars = Get-ChildItem $libraries -Recurse -Filter '*natives-windows.jar' -ErrorAction SilentlyContinue
Write-Host "[*] 找到 $($jars.Count) 个 natives jar"

$extracted = 0
foreach ($jar in $jars) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.Name -notlike '*.dll') { continue }
            $target = Join-Path $natives $entry.Name
            try {
                [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
                Write-Host "    + $($entry.Name)  <- $($jar.Name)"
                $extracted++
            } catch {
                Write-Host "    ! 写不进去：$($entry.Name) —— $($_.Exception.Message)" -ForegroundColor Yellow
            }
        }
    } finally {
        $zip.Dispose()
    }
}

Write-Host ""
Write-Host "[*] 共解压 $extracted 个 DLL" -ForegroundColor Cyan

$missing = @()
foreach ($name in $expected) {
    if (-not (Test-Path (Join-Path $natives $name))) { $missing += $name }
}

if ($missing.Count -gt 0) {
    Write-Host "[!] 仍然缺少：$($missing -join ', ')" -ForegroundColor Red
    Write-Host "    如果反复出现，多半是杀毒软件把解压出来的 DLL 删掉了。" -ForegroundColor Yellow
    Write-Host "    把这个目录加入杀软白名单：$natives" -ForegroundColor Yellow
    exit 2
}

Write-Host "[OK] 8 个 native DLL 齐全，可以启动游戏了" -ForegroundColor Green
