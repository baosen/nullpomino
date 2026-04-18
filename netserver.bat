@echo off
cd /d "%~dp0"
bazel build //:NetServer_deploy.jar
if errorlevel 1 exit /b %ERRORLEVEL%
start java -jar bazel-bin\NetServer_deploy.jar %*
