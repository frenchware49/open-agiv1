@echo off
setlocal enabledelayedexpansion
set "JDK=C:\Program Files\Android\openjdk\jdk-21.0.8"
set "WEKA="

rem 1) Proje icindeki lib\weka.jar
if exist "lib\weka.jar" set "WEKA=lib\weka.jar"

rem 2) Weka installer ile kurulduysa
if not defined WEKA if exist "C:\Program Files\Weka-3-8-7\lib\weka.jar" set "WEKA=C:\Program Files\Weka-3-8-7\lib\weka.jar"
if not defined WEKA if exist "C:\Program Files\Weka-3-8-6\lib\weka.jar" set "WEKA=C:\Program Files\Weka-3-8-6\lib\weka.jar"
if not defined WEKA if exist "C:\Program Files (x86)\Weka-3-8-7\lib\weka.jar" set "WEKA=C:\Program Files (x86)\Weka-3-8-7\lib\weka.jar"

rem 3) Indirilenler klasorlerinde weka*.jar ara
if not defined WEKA (
    for %%F in ("%USERPROFILE%\Downloads\weka*.jar" "%USERPROFILE%\OneDrive\Downloads\weka*.jar" "%USERPROFILE%\OneDrive\Indirilenler\weka*.jar") do (
        if exist "%%~F" if not defined WEKA set "WEKA=%%~F"
    )
)

if not defined WEKA goto :bulunamadi

echo Kullanilan Weka JAR: %WEKA%

if exist "%JDK%\bin\javac.exe" (
    set "JAVAC=%JDK%\bin\javac.exe"
    set "JAVA=%JDK%\bin\java.exe"
) else (
    set "JAVAC=javac"
    set "JAVA=java"
)

if not exist out-weka mkdir out-weka
"%JAVAC%" --release 8 -encoding UTF-8 -Xlint:-options -cp "%WEKA%" -d out-weka src\mlweka\WekaDemo.java
if errorlevel 1 goto :fail
"%JAVA%" --add-opens java.base/java.lang=ALL-UNNAMED -cp "out-weka;%WEKA%" mlweka.WekaDemo
goto :eof

:bulunamadi
echo weka.jar bulunamadi.
echo.
echo Yapilacaklar:
echo   1. Su adresten weka-3-8-7.zip indirin:
echo      https://sourceforge.net/projects/weka/files/weka-3-8/3.8.7/
echo   2. Zip'i acin, icindeki weka.jar dosyasini proje klasorundeki
echo      "lib" klasorune kopyalayin.
echo   3. Bu dosyayi tekrar calistirin.
exit /b 1

:fail
echo Derleme hatasi.
exit /b 1
