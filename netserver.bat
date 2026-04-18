@echo off
cd /d "%~dp0.."
start java -cp "target\nullpomino-7.6.0-SNAPSHOT.jar;target\lib\*" mu.nu.nullpo.game.net.NetServer %1
