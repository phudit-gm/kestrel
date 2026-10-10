# Builds Kestrel for every supported Minecraft version and collects the files to upload into release/dist.
# Run from the repo root:  powershell -File release\build-release.ps1
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $PSScriptRoot 'dist'
Remove-Item $dist -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory $dist | Out-Null
$version = (Select-String -Path (Join-Path $root 'mc-26.2\gradle.properties') -Pattern '^version=(.+)$').Matches[0].Groups[1].Value
foreach ($mc in '1.21.11', '26.2', '26.3') {
    Push-Location (Join-Path $root "mc-$mc")
    try {
        & .\gradlew.bat clean build
        if ($LASTEXITCODE -ne 0) { throw "build failed for $mc" }
    } finally { Pop-Location }
    Copy-Item (Join-Path $root "mc-$mc\build\libs\kestrel-$version.jar") (Join-Path $dist "kestrel-$version+mc$mc.jar")
}
Compress-Archive -Path (Join-Path $root 'datapacks\kestrel_rules') -DestinationPath (Join-Path $dist "kestrel_rules-datapack-$version.zip")
Get-ChildItem $dist | Select-Object Name, Length
