@echo off
cd /d "%~dp0"
bazel build //:NetAdmin_deploy.jar
if errorlevel 1 exit /b %ERRORLEVEL%
start javaw -jar bazel-bin\NetAdmin_deploy.jar %*
