@echo off
:: sqlline.bat - Windows script to launch SQL shell
java -cp "%~dp0\..\target\*" sqlline.SqlLine %*

:: End sqlline.bat
