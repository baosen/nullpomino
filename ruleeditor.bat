@echo off
cd /d "%~dp0"
bazel build //:RuleEditor_deploy.jar
if errorlevel 1 exit /b %ERRORLEVEL%
start javaw -jar bazel-bin\RuleEditor_deploy.jar %*
