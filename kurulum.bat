@echo off
setlocal
cd /d "%~dp0"
set "MODEL=gemma4:e4b"

echo ============================================
echo   Yerel Ajan - Otomatik Kurulum
echo ============================================
echo.

where ollama >nul 2>&1
if errorlevel 1 (
    echo [1/3] Ollama bulunamadi.
    echo       Lutfen once Ollama'yi kurun: https://ollama.com/download
    echo       Kurulduktan sonra bu dosyayi tekrar calistirin.
    exit /b 1
)
echo [1/3] Ollama hazir.

ollama list | findstr /I "%MODEL%" >nul
if errorlevel 1 (
    echo [2/3] Model indiriliyor: %MODEL% ~6.6 GB
    echo       (ilk kurulumda birkaq dakika surebilir)
    ollama pull %MODEL%
    if errorlevel 1 (
        echo Model indirilemedi. Baglantinizi kontrol edin.
        exit /b 1
    )
) else (
    echo [2/3] Model zaten kurulu: %MODEL%
)

echo [3/3] Java kodlari derleniyor...
if not exist out-gui mkdir out-gui
"%JDK%\bin\javac.exe" --version >nul 2>&1
if exist "C:\Program Files\Android\openjdk\jdk-21.0.8\bin\javac.exe" (
    "C:\Program Files\Android\openjdk\jdk-21.0.8\bin\javac.exe" --release 8 -encoding UTF-8 -Xlint:-options -d out-gui src\neuralnet\*.java src\agent\*.java src\agent\tools\*.java src\agent\ui\*.java src\agent\server\*.java src\ownmodel\*.java
) else (
    javac --release 8 -encoding UTF-8 -Xlint:-options -d out-gui src\neuralnet\*.java src\agent\*.java src\agent\tools\*.java src\agent\ui\*.java src\agent\server\*.java src\ownmodel\*.java
)
if errorlevel 1 (
    echo Derleme hatasi.
    exit /b 1
)

echo.
echo KURULUM TAMAM. GUI'yi acmak icin Calistir-GUI.vbs dosyasina basin.
