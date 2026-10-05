@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d C:\repo\campilot
echo JAVA_HOME: %JAVA_HOME%
echo Java version:
"%JAVA_HOME%\bin\java.exe" -version
echo.
echo Compiling...
call gradlew.bat assembleDebug
