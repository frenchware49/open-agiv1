@echo off
setlocal
cd /d "%~dp0"

py -3.12 --version >nul 2>&1
if errorlevel 1 (
    echo Python 3.12 bulunamadi.
    exit /b 1
)

py -3.12 tools\finetune\gemma4_lora.py --epochs 3
if errorlevel 1 (
    echo LoRA egitimi basarisiz. Bkz. tools\finetune\README.md
    exit /b 1
)

echo.
echo Bitti. Yeni model:
echo   cd tools\finetune ^&^& ollama create yerel-ajen-lora -f Modelfile.lora
