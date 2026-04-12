@echo off
cd /d "%~dp0.."
start javaw -cp "target\nullpomino-7.6.0-SNAPSHOT.jar;target\lib\*" -Djava.library.path=target\lib mu.nu.nullpo.gui.slick.NullpoMinoSlick
