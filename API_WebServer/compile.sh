
arduinoInstallDir=~/arduino-1.8.13
arduinoInstallDir2=~/arduino-1.8.12

function cleanPrevBinFiles {
    cd ./bin
    rm -r *
    cd ..
}

function compile {
    echo .:$arduinoInstallDir/lib/pde.jar
    javac -cp .:$arduinoInstallDir/lib/pde.jar:$arduinoInstallDir/lib/arduino-core.jar:$arduinoInstallDir/lib/rsyntaxtextarea-3.0.3-SNAPSHOT.jar:./tool/json-20200518.jar:./tool/Java-WebSocket-1.5.1.jar:./tool/autocomplete-3.0.4.jar -d bin ./src/*.java
    buildStatus=$?
}

function makeJar {
    cd bin
    jar cvf API_WebServer.jar *
    cd ..
}

function copyfiles {
    cp ./bin/API_WebServer.jar ./tool/API_WebServer.jar
    cp -r ./tool/* $arduinoInstallDir/tools/API_WebServer/tool
    cp -r ./src/* $arduinoInstallDir/tools/API_WebServer/src

    cp -r ./tool/* $arduinoInstallDir2/tools/API_WebServer/tool
    cp -r ./src/* $arduinoInstallDir2/tools/API_WebServer/src
    echo ***************
    echo *** Success ***
    echo ***************
}

function doStuff {
    cleanPrevBinFiles
    compile
    success=0
    if [ $buildStatus -eq $success ];
    then
        makeJar
        copyfiles	
    else
        echo Compile error
    fi
}

doStuff

##read -p "Press [Enter] key to continue..."