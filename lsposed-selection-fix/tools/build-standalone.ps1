param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk"
)

# Native Java tools often send ordinary compiler notes to stderr. Explicitly
# check each native tool's exit code instead of promoting notes to exceptions.
$ErrorActionPreference = 'Continue'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$build = Join-Path $root 'app\build\standalone'
$sdkBuildTools = Join-Path $Sdk 'build-tools\35.0.0'
$androidJar = Join-Path $Sdk 'platforms\android-35\android.jar'
$xposedApi = Join-Path $root 'research\xposed-api-82.jar'
$javaSource = Join-Path $root 'app\src\main\java\dev\voidream\chatgptselectionfix\SelectionFix.java'
$classes = Join-Path $build 'classes'
$dex = Join-Path $build 'dex'

New-Item -ItemType Directory -Force -Path $classes, $dex | Out-Null
if (!(Test-Path $androidJar) -or !(Test-Path $xposedApi)) { throw 'Missing Android SDK 35 or Xposed API 82 JAR' }

$javac = (Get-Command javac.exe -ErrorAction Stop).Source
$jar = (Get-Command jar.exe -ErrorAction Stop).Source
& $javac -source 17 -target 17 -cp "$androidJar;$xposedApi" -d $classes $javaSource
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

$classesJar = Join-Path $build 'classes.jar'
& $jar cf $classesJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }

& (Join-Path $sdkBuildTools 'd8.bat') --min-api 32 --lib $androidJar --output $dex $classesJar
if ($LASTEXITCODE -ne 0) { throw 'D8 failed' }

$resZip = Join-Path $build 'resources.zip'
& (Join-Path $sdkBuildTools 'aapt2.exe') compile --dir (Join-Path $root 'app\src\main\res') -o $resZip
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }

$sourceManifest = Join-Path $root 'app\src\main\AndroidManifest.xml'
$standaloneManifest = Join-Path $build 'AndroidManifest.xml'
$manifestContent = [System.IO.File]::ReadAllText($sourceManifest)
$manifestContent = $manifestContent.Replace('<manifest xmlns:android=', '<manifest package="dev.voidream.chatgptselectionfix" android:versionCode="2" android:versionName="0.2.0" xmlns:android=')
[System.IO.File]::WriteAllText($standaloneManifest, $manifestContent, (New-Object System.Text.UTF8Encoding $false))

$unsigned = Join-Path $build 'unsigned.apk'
& (Join-Path $sdkBuildTools 'aapt2.exe') link -o $unsigned -I $androidJar `
    --manifest $standaloneManifest `
    --min-sdk-version 32 --target-sdk-version 35 `
    --auto-add-overlay -A (Join-Path $root 'app\src\main\assets') `
    $resZip
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

# Add DEX; aapt2 already placed both Xposed assets in the archive.
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::Open($unsigned, [System.IO.Compression.ZipArchiveMode]::Update)
if ($null -eq $archive) { throw 'Could not open unsigned APK archive' }
try {
    $entry = $archive.CreateEntry('classes.dex')
    $inputFile = [System.IO.File]::OpenRead((Join-Path $dex 'classes.dex'))
    try {
        $outputStream = $entry.Open()
        try { $inputFile.CopyTo($outputStream) }
        finally { $outputStream.Dispose() }
    } finally { $inputFile.Dispose() }
} finally { $archive.Dispose() }

$check = [System.IO.Compression.ZipFile]::OpenRead($unsigned)
try {
    foreach ($required in @('classes.dex', 'assets/xposed_init', 'assets/xposed_scope')) {
        if ($null -eq $check.GetEntry($required)) { throw "Required APK entry missing: $required" }
    }
} finally { $check.Dispose() }

$aligned = Join-Path $build 'aligned.apk'
& (Join-Path $sdkBuildTools 'zipalign.exe') -f 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

$keystore = Join-Path $build 'debug.keystore'
if (!(Test-Path $keystore)) {
    & (Get-Command keytool.exe -ErrorAction Stop).Source -genkeypair -keystore $keystore `
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 3650 `
        -storepass android -keypass android -dname 'CN=Android Debug,O=Android,C=US' -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }
}

$output = Join-Path $build 'chatgpt-selection-fix-debug.apk'
& (Join-Path $sdkBuildTools 'apksigner.bat') sign --ks $keystore `
    --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android `
    --out $output $aligned
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }

& (Join-Path $sdkBuildTools 'apksigner.bat') verify --verbose $output
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
Write-Output "APK_READY=$output"
