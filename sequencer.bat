@echo off
cd /d "%~dp0"
bazel build //:Sequencer_deploy.jar
if errorlevel 1 exit /b %ERRORLEVEL%
start javaw -jar bazel-bin\Sequencer_deploy.jar %*
