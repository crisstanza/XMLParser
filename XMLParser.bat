@echo off

REM
REM Command line main menu for XMLParser.
REM
REM Author: Cris Stanza, 30-Jul-2015
REM

:MAIN_MENU
	cls
	echo.
	echo   XMLParser
	echo   =========
	echo.

	echo  [1] Compile      [2] Run      [3] Clean      [i] Invert ".\in" files
	echo  [q] quit
	echo.

	set OPTION=
	set /p OPTION=: 

	if [%OPTION%] == [1] goto OPTION_1
	if [%OPTION%] == [2] goto OPTION_2
	if [%OPTION%] == [3] goto OPTION_3
	if [%OPTION%] == [i] goto OPTION_I
	if [%OPTION%] == [q] goto OPTION_Q
	goto MAIN_MENU

:OPTION_1
	echo.
	if not exist .\classes md .\classes
	cd .\src
	javac -d ..\classes -cp . *.java
	cd ..
	pause
	goto MAIN_MENU

:OPTION_2
	echo.
	java -cp .;.\classes Main
	pause
	goto MAIN_MENU

:OPTION_3
	echo.
	if exist .\classes rmdir /s/q .\classes
	pause
	goto MAIN_MENU

:OPTION_I
	echo.
	cd .\in
	ren in.xml __in.xml
	ren _in.xml in.xml
	ren __in.xml _in.xml
	cd ..
	pause
	goto MAIN_MENU

:OPTION_Q
	echo.
	goto END

:END
	REM pause
