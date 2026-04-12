@echo off
cd /d "%~dp0.."
start javaw -cp "target\nullpomino-7.6.0-SNAPSHOT.jar;target\lib\*" -Dsun.java2d.translaccel=true -Dsun.java2d.d3dtexbpp=16 mu.nu.nullpo.gui.swing.NullpoMinoSwing
