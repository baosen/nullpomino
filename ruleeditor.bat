@echo off
cd /d "%~dp0"
start javaw -cp "target\nullpomino-7.6.0-SNAPSHOT.jar;target\lib\*" mu.nu.nullpo.tool.ruleeditor.RuleEditor
