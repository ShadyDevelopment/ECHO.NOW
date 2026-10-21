[CmdletBinding()]
param(
    [string]$OutputDirectory = (Join-Path $env:LOCALAPPDATA 'ECHO-NOW\Tizen'),
    [string]$TizenCli = 'C:\tizen-studio\tools\ide\bin\tizen.bat',
    [string]$Sdb = 'C:\tizen-studio\tools\sdb.exe',
    [string]$SecurityProfile = 'ECHONOWF',
    [string]$TargetDevice = '192.168.1.102:26101',
    [switch]$Install,
    [switch]$Run
)

$ErrorActionPreference = 'Stop'
$baseDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
if (-not $baseDir) { $baseDir = '.' }
$ConfigPath = Join-Path $baseDir '..\config.cfg'
$configPath = (Resolve-Path $ConfigPath).Path
if (-not (Test-Path -LiteralPath $TizenCli -PathType Leaf)) {
    throw "Tizen CLI not found: $TizenCli"
}

if (-not $PSBoundParameters.ContainsKey('SecurityProfile')) {
    $profilesXmlPath = 'C:\tizen-studio-data\profile\profiles.xml'
    if (Test-Path -LiteralPath $profilesXmlPath) {
        try {
            [xml]$px = Get-Content -LiteralPath $profilesXmlPath
            if ($px.profiles.active) {
                $SecurityProfile = $px.profiles.active
            }
        } catch {}
    }
}

$config = @{}
$credentialKeys = @('SUBDL_API_KEY', 'THEMOVIEDB_API_KEY', 'THEMOVIEDB_API_READ_ACCESS_TOKEN')
foreach ($line in [System.IO.File]::ReadAllLines($configPath)) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith('#') -or $trimmed.StartsWith(';')) {
        continue
    }
    if ($trimmed -match '^([^:=\s]+)\s*[:=]\s*(.*)$') {
        $config[$matches[1]] = $matches[2].Trim().Trim('"').Trim("'")
    }
}

$requiredKeys = @('APP_NAME', 'APP_VERSION', 'APP_WEB')
$requiredKeys += $credentialKeys
$missing = @($requiredKeys | Where-Object { -not $config.ContainsKey($_) })
if ($missing.Count) {
    throw "Missing config.cfg keys: $($missing -join ', ')"
}
if (-not $config.APP_NAME -or -not $config.APP_VERSION) {
    throw 'APP_NAME and APP_VERSION must not be empty.'
}
if (-not $config.THEMOVIEDB_API_KEY -and -not $config.THEMOVIEDB_API_READ_ACCESS_TOKEN) {
    throw 'At least one TMDB credential must be configured.'
}

$outputPath = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
$buildId = [Guid]::NewGuid().ToString('N')
$stagingRoot = Join-Path $env:TEMP "echo-now-tizen-$buildId"
$appPath = Join-Path $stagingRoot 'tizenApp'
$packageOutput = Join-Path $stagingRoot 'package'

try {
    New-Item -ItemType Directory -Path $appPath -Force | Out-Null
    Get-ChildItem -LiteralPath $PSScriptRoot -Force |
        Where-Object { $_.Name -ne 'build.ps1' -and $_.Name -ne 'install.ps1' } |
        Copy-Item -Destination $appPath -Recurse -Force
    $manifestPath = Join-Path $appPath 'config.xml'
    [xml]$manifest = [System.IO.File]::ReadAllText($manifestPath)
    $manifest.DocumentElement.SetAttribute('version', $config.APP_VERSION)
    $nameNode = $manifest.SelectSingleNode('/*[local-name()="widget"]/*[local-name()="name"]')
    if (-not $nameNode) {
        throw 'The Tizen manifest does not contain an app name element.'
    }
    $nameNode.InnerText = $config.APP_NAME
    $applicationNode = $manifest.SelectSingleNode('/*[local-name()="widget"]/*[local-name()="application"]')
    if (-not $applicationNode) {
        throw 'The Tizen manifest does not contain an application element.'
    }
    $appId = $applicationNode.GetAttribute('id')
    $targetTizenVersion = $applicationNode.GetAttribute('required_version')
    if (-not $targetTizenVersion) {
        throw 'The Tizen application does not declare a minimum Tizen version.'
    }
    $descriptionNode = $manifest.SelectSingleNode('/*[local-name()="widget"]/*[local-name()="description"]')
    if ($descriptionNode) {
        $descriptionNode.InnerText = "Discover movies and series with $($config.APP_NAME)."
    }
    $manifest.Save($manifestPath)

    $buildConfig = [ordered]@{
        appName = $config.APP_NAME
        appVersion = $config.APP_VERSION
        appWeb = $config.APP_WEB
        credentialsLocked = $true
    }
    $buildConfig.subdlApiKey = $config.SUBDL_API_KEY
    $buildConfig.tmdbApiKey = $config.THEMOVIEDB_API_KEY
    $buildConfig.tmdbReadAccessToken = $config.THEMOVIEDB_API_READ_ACCESS_TOKEN
    $buildConfig.uiColors = [ordered]@{
        menuBarBackground = if ($config.UI_MENU_BAR_BACKGROUND) { $config.UI_MENU_BAR_BACKGROUND } else { '#0f0f0f' }
        pageBackgroundStart = if ($config.UI_PAGE_BACKGROUND_START) { $config.UI_PAGE_BACKGROUND_START } else { '#0d0c0c' }
        pageBackgroundEnd = if ($config.UI_PAGE_BACKGROUND_END) { $config.UI_PAGE_BACKGROUND_END } else { '#02070a' }
        cardBackground = if ($config.UI_CARD_BACKGROUND) { $config.UI_CARD_BACKGROUND } else { '#092224' }
        surfaceColor = if ($config.UI_SURFACE_COLOR) { $config.UI_SURFACE_COLOR } else { '#092224' }
        accentPrimary = if ($config.UI_ACCENT_PRIMARY) { $config.UI_ACCENT_PRIMARY } else { '#607d8b' }
        accentSecondary = if ($config.UI_ACCENT_SECONDARY) { $config.UI_ACCENT_SECONDARY } else { '#00838f' }
        accentTertiary = if ($config.UI_ACCENT_TERTIARY) { $config.UI_ACCENT_TERTIARY } else { '#31a7a0' }
        accentFocus = if ($config.UI_ACCENT_FOCUS) { $config.UI_ACCENT_FOCUS } else { '#31a7a0' }
        textPrimary = if ($config.UI_TEXT_PRIMARY) { $config.UI_TEXT_PRIMARY } else { '#f7f7fb' }
        textMuted = if ($config.UI_TEXT_MUTED) { $config.UI_TEXT_MUTED } else { '#a6a5b0' }
        dropdownBackground = if ($config.UI_DROPDOWN_BACKGROUND) { $config.UI_DROPDOWN_BACKGROUND } else { '#0d1117' }
    }
    $buildConfig = $buildConfig | ConvertTo-Json -Depth 4 -Compress
    $buildConfigPath = Join-Path $appPath 'js\build-config.js'
    [System.IO.File]::WriteAllText(
        $buildConfigPath,
        "`"use strict`";`nwindow.ECHO_BUILD_CONFIG = Object.freeze($buildConfig);`n",
        [System.Text.UTF8Encoding]::new($false)
    )

    Write-Output "Preparing Tizen 4.0 compatibility (downleveling JS and applying CSS fallbacks)..."
    & node "$PSScriptRoot\prepare-tizen.js" "$appPath"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to prepare app for Tizen."
    }

    Write-Output "Packaging with profile [$SecurityProfile]..."
    & $TizenCli package -t wgt -s $SecurityProfile -o $packageOutput -- $appPath
    if ($LASTEXITCODE -ne 0) {
        throw "Tizen packaging failed with exit code $LASTEXITCODE."
    }
    $builtPackage = Join-Path $packageOutput 'ECHO NOW.wgt'
    if (-not (Test-Path -LiteralPath $builtPackage -PathType Leaf)) {
        throw "Tizen CLI did not produce the expected WGT: $builtPackage"
    }

    $safeName = ($config.APP_NAME -replace '[^\w.-]+', '-').Trim('-')
    $artifactName = "$safeName-$($config.APP_VERSION)-tizen-$targetTizenVersion-configured-dev.wgt"
    $artifactPath = Join-Path $outputPath $artifactName
    Copy-Item -LiteralPath $builtPackage -Destination $artifactPath -Force
    Write-Output "Built Tizen WGT: $artifactPath"
    Write-Output "SHA-256: $((Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash)"
    Write-Warning 'This package embeds config.cfg credentials. Do not publish or share it.'

    if ($Install) {
        Write-Output "Connecting to target device $TargetDevice..."
        $deviceIp = ($TargetDevice -split ':')[0]
        if (Test-Path -LiteralPath $Sdb) {
            & $Sdb connect $deviceIp | Out-Null
        }

        Write-Output "Authorizing device with active certificate profile (install-permit)..."
        & $TizenCli install-permit -s $TargetDevice
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "install-permit exited with code $LASTEXITCODE, proceeding with install attempt..."
        }

        Write-Output "Installing package $artifactPath to TV..."
        & $TizenCli install -s $TargetDevice -n $artifactPath
        if ($LASTEXITCODE -ne 0) {
            throw "Tizen installation failed with exit code $LASTEXITCODE."
        }
        Write-Output "Application installed successfully!"

        if ($Run) {
            if (Test-Path -LiteralPath $Sdb) {
                Write-Output "Launching application [$appId] on TV..."
                & $Sdb -s $TargetDevice shell 0 was_execute $appId
            }
        }
    }
}
finally {
    if (Test-Path -LiteralPath $stagingRoot) {
        Remove-Item -LiteralPath $stagingRoot -Recurse -Force
    }
}
