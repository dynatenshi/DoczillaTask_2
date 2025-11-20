@echo off
call clean.bat
call compile.bat
call package.bat
timeout 2
call launch.bat