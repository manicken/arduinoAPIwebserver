set arduinoInstallDir=G:\arduino-1.8.13
set arduinoInstallDir2=G:\arduino-1.8.12

copy .\tool\*.* %arduinoInstallDir%\tools\API_WebServer\tool\*.*
copy .\src\*.* %arduinoInstallDir%\tools\API_WebServer\src\*.*

copy .\tool\*.* %arduinoInstallDir2%\tools\API_WebServer\tool\*.*
copy .\src\*.* %arduinoInstallDir2%\tools\API_WebServer\src\*.*

pause