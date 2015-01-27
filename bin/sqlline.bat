@echo off
java -Djava.ext.dirs=%~dp0 sqlline.SqlLine %*
