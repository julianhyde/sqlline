@echo off
:: sqlline.bat - Windows script to launch SQL shell
java -Djava.ext.dirs=%~dp0 sqlline.SqlLine %*

:: End sqlline.bat
