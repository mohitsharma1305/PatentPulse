@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>&1
if errorlevel 1 (
 echo Java was not found. Install JDK 17 and add its bin directory to PATH.
 pause
 exit /b 1
)
where mvn >nul 2>&1
if errorlevel 1 (
 echo Maven was not found. Install Maven 3.9+ and add its bin directory to PATH.
 pause
 exit /b 1
)
echo Starting PatentPulse. The first launch downloads dependencies.
echo Open http://localhost:8080/patentpulse after startup completes.
call mvn test-compile exec:java -Dexec.mainClass=com.patentpulse.DemoServer -Dexec.classpathScope=test
pause
