@echo off
setlocal
cd /d "%~dp0"
set "JDK=C:\Program Files\Android\openjdk\jdk-21.0.8"

if exist "%JDK%\bin\javac.exe" (
    set "JAVAC=%JDK%\bin\javac.exe"
    set "JAVA=%JDK%\bin\java.exe"
) else (
    set "JAVAC=javac"
    set "JAVA=java"
)

if not exist out-server mkdir out-server
"%JAVAC%" --release 8 -encoding UTF-8 -Xlint:-options -d out-server src\neuralnet\*.java src\agent\*.java src\agent\tools\*.java src\agent\server\*.java src\ownmodel\*.java
if errorlevel 1 goto :fail
"%JAVA%" -cp out-server agent.server.AgentServer
goto :eof

:fail
echo Derleme hatasi.
exit /b 1
