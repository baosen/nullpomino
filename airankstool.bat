@echo off
cd /d "%~dp0"
bazel build //:AIRanksTool_deploy.jar
if errorlevel 1 exit /b %ERRORLEVEL%
start javaw -Xmx512m -jar bazel-bin\AIRanksTool_deploy.jar %*
