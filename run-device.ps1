# Run Pay&Track on a connected device with live logs (Flutter-run style).
# Usage:  .\run-device.ps1
# Stop:   Ctrl+C

$ErrorActionPreference = "Stop"
$package = "com.sumedh.moneytracker"
$activity = "$package/.MainActivity"

$connected = adb devices | Select-String "`tdevice$"
if (-not $connected) {
    Write-Host "No device connected. Plug in your Samsung and enable USB debugging." -ForegroundColor Red
    exit 1
}

Write-Host "Building & installing debug APK..." -ForegroundColor Cyan
& .\gradlew.bat :app:installDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Launching app..." -ForegroundColor Cyan
adb shell am force-stop $package 2>$null
adb logcat -c
adb shell am start -n $activity | Out-Null
Start-Sleep -Seconds 1

$appPid = ((adb shell pidof -s $package) | Out-String).Trim()
Write-Host "---- live logs (Ctrl+C to stop) ----" -ForegroundColor Green

if ($appPid) {
    Write-Host "App PID: $appPid" -ForegroundColor DarkGray
    adb logcat --pid=$appPid
} else {
    Write-Host "PID not found yet — showing runtime + app-tagged logs." -ForegroundColor Yellow
    adb logcat *:S AndroidRuntime:E System.err:W ActivityManager:I
}
