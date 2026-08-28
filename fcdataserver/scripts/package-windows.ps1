# Build frontend + Spring Boot jar, then jpackage + Inno Setup Windows installer.
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1

$ErrorActionPreference = "Stop"

$ServerRoot = Split-Path -Parent $PSScriptRoot
$WorkspaceRoot = Split-Path -Parent $ServerRoot
$FrontendRoot = Join-Path $WorkspaceRoot "fcplayerdata"
$DistRoot = Join-Path $ServerRoot "dist"
$StagingRoot = Join-Path $DistRoot "staging"
$InputDir = Join-Path $StagingRoot "input"
$AppImageDir = Join-Path $StagingRoot "FC26Career"
$IssFile = Join-Path $PSScriptRoot "windows\setup.iss"
$IconPng = Join-Path $PSScriptRoot "windows\app-icon.png"
$IconIco = Join-Path $PSScriptRoot "windows\app.ico"
$AppName = "FC26Career"
$AppVersion = "1.0.0"

function Convert-PngToIco {
    param(
        [Parameter(Mandatory = $true)][string]$PngPath,
        [Parameter(Mandatory = $true)][string]$IcoPath
    )
    Add-Type -AssemblyName System.Drawing
    $src = [System.Drawing.Image]::FromFile((Resolve-Path $PngPath))
    try {
        $sizes = @(16, 24, 32, 48, 64, 128, 256)
        $images = New-Object System.Collections.Generic.List[byte[]]
        foreach ($size in $sizes) {
            $bmp = New-Object System.Drawing.Bitmap $size, $size
            $g = [System.Drawing.Graphics]::FromImage($bmp)
            try {
                $g.Clear([System.Drawing.Color]::White)
                $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $pad = [Math]::Max(1, [int]($size * 0.08))
                $g.DrawImage($src, $pad, $pad, $size - (2 * $pad), $size - (2 * $pad))
                $ms = New-Object System.IO.MemoryStream
                $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
                [void]$images.Add($ms.ToArray())
                $ms.Dispose()
            } finally {
                $g.Dispose()
                $bmp.Dispose()
            }
        }

        $count = $images.Count
        $msIco = New-Object System.IO.MemoryStream
        $bw = New-Object System.IO.BinaryWriter $msIco
        try {
            $bw.Write([uint16]0)
            $bw.Write([uint16]1)
            $bw.Write([uint16]$count)
            $offset = 6 + (16 * $count)
            for ($i = 0; $i -lt $count; $i++) {
                $w = $sizes[$i]
                $dim = if ($w -ge 256) { [byte]0 } else { [byte]$w }
                $bw.Write($dim)
                $bw.Write($dim)
                $bw.Write([byte]0)
                $bw.Write([byte]0)
                $bw.Write([uint16]1)
                $bw.Write([uint16]32)
                $bw.Write([uint32]$images[$i].Length)
                $bw.Write([uint32]$offset)
                $offset += $images[$i].Length
            }
            foreach ($pngBytes in $images) {
                $bw.Write($pngBytes)
            }
            $bw.Flush()
            [System.IO.File]::WriteAllBytes($IcoPath, $msIco.ToArray())
        } finally {
            $bw.Dispose()
            $msIco.Dispose()
        }
    } finally {
        $src.Dispose()
    }
}

function Find-CommandPath {
    param([string[]]$Names)
    foreach ($name in $Names) {
        $cmd = Get-Command $name -ErrorAction SilentlyContinue
        if ($cmd) {
            return $cmd.Source
        }
    }
    return $null
}

function Get-JarMainClass {
    param([string]$JarPath)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $entry = $zip.GetEntry("META-INF/MANIFEST.MF")
        if (-not $entry) {
            throw "META-INF/MANIFEST.MF not found in jar"
        }
        $reader = New-Object System.IO.StreamReader($entry.Open())
        try {
            $manifest = $reader.ReadToEnd()
        } finally {
            $reader.Close()
        }
        if ($manifest -match "(?m)^Main-Class:\s*(\S+)") {
            return $Matches[1]
        }
        throw "Main-Class missing in MANIFEST.MF"
    } finally {
        $zip.Dispose()
    }
}

$npm = Find-CommandPath @("npm.cmd", "npm")
$mvn = Find-CommandPath @("mvn.cmd", "mvn")
$jpackage = Find-CommandPath @("jpackage.exe", "jpackage")
$iscc = @(
    (Join-Path $env:LOCALAPPDATA "Programs\Inno Setup 6\ISCC.exe"),
    "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
    "${env:ProgramFiles}\Inno Setup 6\ISCC.exe"
) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
if (-not $iscc) {
    $isccCmd = Find-CommandPath @("ISCC.exe", "iscc")
    if ($isccCmd) { $iscc = $isccCmd }
}

if (-not $npm) { throw "npm not found. Install Node.js first." }
if (-not $mvn) { throw "mvn not found. Install Maven and add it to PATH." }
if (-not $jpackage) { throw "jpackage not found. Use JDK 17+ and add it to PATH." }
if (-not $iscc) { throw "ISCC.exe not found. Install Inno Setup 6 first." }
if (-not (Test-Path $FrontendRoot)) { throw "Frontend directory not found: $FrontendRoot" }
if (-not (Test-Path $IssFile)) { throw "Inno script not found: $IssFile" }
if (-not (Test-Path $IconPng)) { throw "icon png not found: $IconPng" }

Write-Host "==> 1/4 build frontend"
Push-Location $FrontendRoot
try {
    & $npm run build
    if ($LASTEXITCODE -ne 0) { throw "frontend build failed" }
} finally {
    Pop-Location
}

Write-Host "==> 2/4 package Spring Boot"
Push-Location $ServerRoot
try {
    & $mvn -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Maven package failed" }
} finally {
    Pop-Location
}

$jar = Get-ChildItem (Join-Path $ServerRoot "target") -Filter "fcdataserver-*.jar" |
    Where-Object { $_.Name -notlike "*.original" -and $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
    Select-Object -First 1
if (-not $jar) { throw "executable jar not found: target/fcdataserver-*.jar" }

$mainClass = Get-JarMainClass -JarPath $jar.FullName
Write-Host ("    jar: {0}" -f $jar.Name)
Write-Host ("    Main-Class: {0}" -f $mainClass)

Write-Host "==> 3/4 jpackage app-image (bundled JRE)"
if (Test-Path $StagingRoot) {
    Remove-Item $StagingRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $InputDir | Out-Null
Copy-Item $jar.FullName (Join-Path $InputDir "app.jar")
Convert-PngToIco -PngPath $IconPng -IcoPath $IconIco

$jpackageArgs = @(
    "--type", "app-image",
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--vendor", "FCData",
    "--description", "FC26 Career Stats",
    "--dest", $StagingRoot,
    "--input", $InputDir,
    "--main-jar", "app.jar",
    "--main-class", $mainClass,
    "--icon", $IconIco,
    "--add-modules", "java.se,jdk.unsupported,jdk.localedata,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.zipfs,jdk.charsets,jdk.management,jdk.management.agent,jdk.jfr",
    "--java-options", "-Dfcdata.packaged=true",
    "--java-options", "-Dfcdata.open-browser=true",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Djava.awt.headless=false"
)
& $jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }
if (-not (Test-Path (Join-Path $AppImageDir "$AppName.exe"))) {
    throw ("jpackage did not create {0}.exe" -f $AppName)
}

Write-Host "==> 4/4 Inno Setup installer (desktop shortcut)"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$utf8Bom = New-Object System.Text.UTF8Encoding $true
$issText = [System.IO.File]::ReadAllText($IssFile, $utf8NoBom)
[System.IO.File]::WriteAllText($IssFile, $issText, $utf8Bom)

New-Item -ItemType Directory -Force -Path $DistRoot | Out-Null
Get-ChildItem $DistRoot -Filter "*.exe" -ErrorAction SilentlyContinue | Remove-Item -Force
$isccArgs = @(
    "/Q",
    ("/DAppImageDir={0}" -f $AppImageDir),
    ("/DOutputDir={0}" -f $DistRoot),
    $IssFile
)
& $iscc @isccArgs
if ($LASTEXITCODE -ne 0) { throw "Inno Setup compile failed" }

$installer = Get-ChildItem $DistRoot -Filter "FC26Career-Setup.exe" | Select-Object -First 1
if (-not $installer) {
    $installer = Get-ChildItem $DistRoot -Filter "*.exe" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
}
Write-Host ""
Write-Host "Installer created:"
Write-Host ("  {0}" -f $installer.FullName)
Write-Host "Desktop shortcut: FC26 Career"
Write-Host "User data: %LOCALAPPDATA%\FC26Career\data"
