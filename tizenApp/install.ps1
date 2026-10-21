[CmdletBinding()]
param(
    [string]$TargetDevice = '192.168.1.102:26101',
    [string]$PackagePath,
    [string]$TizenCli = 'C:\tizen-studio\tools\ide\bin\tizen.bat',
    [string]$Sdb = 'C:\tizen-studio\tools\sdb.exe',
    [switch]$Run = $true
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $TizenCli)) {
    throw "Tizen CLI not found at: $TizenCli"
}
if (-not (Test-Path -LiteralPath $Sdb)) {
    throw "SDB executable not found at: $Sdb"
}

# Auto-detect latest WGT if not specified
if (-not $PackagePath) {
    $searchDirs = @(
        (Join-Path $env:LOCALAPPDATA 'ECHO-NOW\Tizen'),
        'E:\ECHO-NOW\ECHO NOW\release'
    )
    $candidate = Get-ChildItem -Path $searchDirs -Filter "*.wgt" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($candidate) {
        $PackagePath = $candidate.FullName
    } else {
        throw "No .wgt package specified or found in search paths."
    }
}

if (-not (Test-Path -LiteralPath $PackagePath)) {
    throw "Package not found: $PackagePath"
}

$deviceIp = ($TargetDevice -split ':')[0]
Write-Output "Ensuring SDB connection to $deviceIp..."
& $Sdb connect $deviceIp | Out-Null

Write-Output "Authorizing TV device with active certificate profile (install-permit)..."
& $TizenCli install-permit -s $TargetDevice

Write-Output "Installing package $PackagePath on $TargetDevice..."
& $TizenCli install -s $TargetDevice -n $PackagePath
if ($LASTEXITCODE -ne 0) {
    throw "Tizen installation failed with exit code $LASTEXITCODE."
}

Write-Output "Application installed successfully!"

if ($Run) {
    Write-Output "Launching echonowTV1.ECHONOW on TV..."
    & $Sdb -s $TargetDevice shell 0 was_execute echonowTV1.ECHONOW
}
