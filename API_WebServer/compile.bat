@echo off

set arduinoInstallDir=G:\arduino-1.8.13
set arduinoSketchbookDir=G:\ArduinoSketchbook
cd bin
del/F/Q/S *
cd ..

javac -cp "%arduinoInstallDir%\lib\pde.jar;%arduinoInstallDir%\lib\arduino-core.jar;%arduinoInstallDir%\lib\rsyntaxtextarea-3.0.3-SNAPSHOT.jar;tool\json-20200518.jar;tool\Java-WebSocket-1.5.1.jar;tool\autocomplete-3.0.4.jar" -d bin src\*.java
if errorlevel 1 goto compileError
copy MANIFEST.MF bin\MANIFEST.MF
cd bin
echo ******************
echo *** adding jar ***
echo ******************
jar cmf MANIFEST.MF API_WebServer.jar *
cd..
echo *********************
echo *** copy jar file ***
echo *********************
copy .\bin\API_WebServer.jar .\tool\API_WebServer.jar
echo ***********************
echo *** copy tool files ***
echo ***********************
copy %~dp0tool\* %arduinoSketchbookDir%\tools\API_WebServer\tool\*
echo **********************
echo *** copy src files ***
echo **********************
copy %~dp0src\* %arduinoSketchbookDir%\tools\API_WebServer\src\*

echo ***************
echo *** Success ***
echo ***************
pause
exit

:compileError
echo Compile error
pause