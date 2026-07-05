@echo off
setlocal

set MVNW_DIR=%~dp0
set MAVEN_WRAPPER_JAR=%MVNW_DIR%\.mvn\wrapper\maven-wrapper.jar
set MAVEN_USER_HOME=%MVNW_DIR%\.mvn\maven

if "%JAVA_HOME%"=="" (
  set JAVA_CMD=java.exe
) else (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
)

if not exist "%MAVEN_WRAPPER_JAR%" (
  echo Missing Maven wrapper jar: %MAVEN_WRAPPER_JAR%
  exit /b 1
)

if not exist "%MAVEN_USER_HOME%" mkdir "%MAVEN_USER_HOME%"

"%JAVA_CMD%" ^
  -Dmaven.multiModuleProjectDirectory=%MVNW_DIR% ^
  -Dmaven.user.home=%MAVEN_USER_HOME% ^
  -classpath "%MAVEN_WRAPPER_JAR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

endlocal
