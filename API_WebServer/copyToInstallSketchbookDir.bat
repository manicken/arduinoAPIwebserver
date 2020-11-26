set arduinoSketchbookDir=%HOMEDRIVE%%HOMEPATH%\Documents\Arduino

copy %~dp0tool\* %arduinoSketchbookDir%\tools\API_WebServer\tool\*
copy %~dp0src\* %arduinoSketchbookDir%\tools\API_WebServer\src\*

pause