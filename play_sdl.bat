@echo off
cd /d "%~dp0.."
set PATH=target\lib;%PATH%
start javaw -cp "target\nullpomino-7.6.0-SNAPSHOT.jar;target\lib\*" -Djava.library.path=target\lib -Djna.library.path=target\lib mu.nu.nullpo.gui.sdl.NullpoMinoSDL
