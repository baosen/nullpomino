@echo off
cd /d "%~dp0"
bazel build //:MusicListEditor_deploy.jar
if errorlevel 1 exit /b %ERRORLEVEL%
start javaw -jar bazel-bin\MusicListEditor_deploy.jar %*
