@echo off
cd /d "%~dp0"
start javaw -Xmx512m -cp "target\nullpomino-7.6.0-SNAPSHOT.jar;target\lib\*" -Djava.library.path=target\lib mu.nu.nullpo.tool.airankstool.AIRanksTool
