@echo off
setlocal
cd /d "%~dp0"
set "JDK=C:\Program Files\Android\openjdk\jdk-21.0.8\bin"

if exist "%JDK%\java.exe" (
    set "JAVA=%JDK%\java.exe"
) else (
    set "JAVA=java"
)

echo ============================================
echo   MODEL KARSILASTIRMA (/bench 6 soru)
echo ============================================
echo.

for %%M in (gemma4:e4b deepseek-r1:7b gemma4:12b qwen2.5:7b-instruct) do (
    echo ---------- %%M ----------
    echo /bench | "%JAVA%" -cp out-gui agent.Main %%M 2^>nul | findstr /C:"SONUC" /C:"PASS" /C:"FAIL"
    echo.
)
