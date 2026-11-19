@echo off
setlocal
set "JDK=C:\Program Files\Android\openjdk\jdk-21.0.8"

if exist "%JDK%\bin\javac.exe" (
    set "JAVAC=%JDK%\bin\javac.exe"
    set "JAVA=%JDK%\bin\java.exe"
) else (
    set "JAVAC=javac"
    set "JAVA=java"
)

if not exist out-agent mkdir out-agent
"%JAVAC%" --release 8 -encoding UTF-8 -Xlint:-options -d out-agent src\neuralnet\*.java src\agent\*.java src\agent\tools\*.java src\agent\ui\*.java src\agent\server\*.java src\ownmodel\*.java
if errorlevel 1 goto :fail
"%JAVA%" -cp out-agent agent.Main
goto :eof

:fail
echo Derleme hatasi.
exit /b 1
