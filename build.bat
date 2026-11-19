@echo off
setlocal
set "JDK=C:\Program Files\Android\openjdk\jdk-21.0.8"

where javac >nul 2>&1
if %errorlevel%==0 (
    set "JAVAC=javac"
    set "JAVA=java"
) else (
    set "JAVAC=%JDK%\bin\javac.exe"
    set "JAVA=%JDK%\bin\java.exe"
)

if not exist out mkdir out
"%JAVAC%" --release 8 -encoding UTF-8 -Xlint:-options -d out src\neuralnet\*.java
if errorlevel 1 goto :fail
"%JAVA%" -cp out neuralnet.Main
goto :eof

:fail
echo Derleme hatasi.
exit /b 1
