@REM
@REM The Gradle wrapper script for Windows.
@REM
@IF "%DEBUG%" == "" @SET DEBUG=0

@REM Set default locale to avoid issues with different number formats
@SET JAVA_TOOL_OPTIONS=-Duser.language=en -Duser.country=US

@REM Determine the Java command to use to launch the JVM.
@IF EXIST "%JAVA_HOME%\bin\java.exe" SET JAVA_EXE="%JAVA_HOME%\bin\java.exe"
@IF NOT EXIST "%JAVA_EXE%" SET JAVA_EXE=java

@REM Ensure that the Java executable can be found
@FOR %%i IN (%JAVA_EXE%) DO @IF "%%~fi" NEQ "" SET JAVA_EXE="%%~fi"

@IF EXIST "%JAVA_EXE%" GOTO foundJava
@echo.
@echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
@echo.
@echo Please set the JAVA_HOME variable in your environment to match the
@echo location of your Java installation, or add 'java.exe' to your PATH.
@echo.
@GOTO :EOF

:foundJava

@REM Find the project root directory.
@SET CURRENT_DIR=%CD%
@SET APP_HOME=%CURRENT_DIR%
:findAppHome
@IF EXIST "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" GOTO foundAppHome
@CD ..
@SET APP_HOME=%CD%
@IF "%APP_HOME%" == "%CD%" GOTO :EOF
@GOTO findAppHome
:foundAppHome
@CD "%CURRENT_DIR%"

@REM Execute Gradle
"%JAVA_EXE%" %JAVA_OPTS% "-Dorg.gradle.appname=gradlew" -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
