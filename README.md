# api-webserver README

This extension make it possible to take control of Arduino IDE from a Web Page based client.

## Install

* global:<br>
&nbsp;&nbsp;download this repository by either Code-Download Zip or<br>
&nbsp;&nbsp;&nbsp;&nbsp;by git clone https://github.com/manicken/arduinoAPIwebserver.git<br>
&nbsp;&nbsp;then extract/open the repository<br>

* global (into sketchbook folder (defined in Arduino IDE - Preferenses):<br>
&nbsp;&nbsp;make a new folder in the above defined sketchbook folder<br>
&nbsp;&nbsp;called tools<br>
&nbsp;&nbsp;then copy the API_WebServer from the repository into this new "tools" folder.<br>

### Alternative install

* on windows / linux (into Arduino IDE install dir):<br>
&nbsp;&nbsp;copy folder API_WebServer to [Arduino IDE install location]/tools directory<br>
&nbsp;&nbsp;ex: /Arduino-1.8.13/tools<br>

* on mac (into Arduino IDE package):<br>
&nbsp;&nbsp;In Applications right click and click on "Show Package Contents", then browse Contents -> Java -> tools<br>
&nbsp;&nbsp;by holding the Option key(copy) drag folder API_WebServer from the downloaded repository to the open tools folder above<br>
&nbsp;&nbsp;select replace it you allready have an older version<br>



## Compiling

Download and Install Java SDK8 (1.8) 32bit<br>
(Arduino IDE uses Java 8 (1.8))<br>

two script is provided:<br>
&nbsp;&nbsp;for windows the .bat file<br>
&nbsp;&nbsp;for linux/mac the .sh file<br>

## Features

### POST request with data contents in json format:
```
{
    "files":[
        {
            "name":"Main.c",
            "contents":""
            "overwrite_file": true
        },
        {
            "name":"Main.h",
            "contents":""
            "overwrite_file": true
        }
    ],
    "removeOtherFiles":true,
    "keywords":[
        {
            "token":"MainClass",
            "type":"KEYWORD2"
        },
        {
            "token":"SecondClass",
            "type":"KEYWORD2"
        },
        {
            "token":"SomeConstantVariable",
            "type":"LITERAL1"
        }
    ]
}
```
in above json:<br>
removeOtherFiles<br>
&nbsp;mean that when this is set to true<br>
&nbsp;then files that is not present in the JSON is removed <br>
&nbsp;from the sketch (note. the main sketch file is never removed)<br>
&nbsp;if the main sketch file also should be replaced then a file named main.cpp should be present in the JSON<br>
&nbsp;example when it should be set to false:<br>
&nbsp;when only some permanent files needs to be updated ex. the JSON file.<br>

overwrite_file <br>
&nbsp;mean that when this is set to true<br>
&nbsp;the destination file is overwritten with the contents given<br>
&nbsp;when set to false<br>
&nbsp;this is useful together with removeOtherFiles set to true<br>
&nbsp;when having files in the sketch that should not be deleted<br>
&nbsp;i.e. big source files that contain huge binary data arrays<br>
 
&nbsp;keywords uses the same naming as keywords.txt<br>

&nbsp;POST JSON the three main objects "files", "removeOtherFiles" and "keywords" is optional<br>
&nbsp;but the removeOtherFiles is allways used together with "files"<br>

### GET request<br>
possible query strings:
```
http://localhost:8080?cmd=getFile&fileName=fileNameWithExt
http://localhost:8080?cmd=renameFile&from=fromNameWithExt&to=toNameWithExt
http://localhost:8080?cmd=removeFile&fileName=fileNameWithExt
http://localhost:8080?cmd=compile
http://localhost:8080?cmd=upload
http://localhost:8080?cmd=ping
```

* uses sketch location keywords.txt for additional custom keywords<br>

### Midi WebSocketServer Bridge
Just as the title say, it's a WebSocket Server that is bridged with midi input and output devices<br>
(all received commands are converted to lowercase before parsing)<br>
accepts the following commands: <br>
midisend(0x90, 60, 0x3F)  the parameters can be mixed hexadecimal (0x) & decimal<br>
midigetdevices   this sends back two messages: midiDevicesIn("device1", "device2", "device3) and midiDevicesOut("device1", "device2", "device3)<br>
midisetdevicein(0)  sets and connects the in device with index 0<br>
midisetdeviceout(0)  sets and connects the out device with index 0<br>

### Midi WebSocketServer Bridge - standalone (same functionality as above Midi WebSocketServer Bridge)
the jar is executable in standalone mode<br>
with only the MidiWebSocketBridge active<br>
<br>
this can be used when using the VSCODE extension<br>
because the VSCODE (Node.js) have no native support for midi devices.<br>
<br>
in this mode it have the following parameters:<br>
listdevices   (list midi devices) just to quick check connected devices.<br>
port 3001     (starts the websocket server at port 3001)<br>

## Requirements
```
Java-WebSocket-1.5.1.jar (included) from [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket)
json-20200518.jar (included) from [JSON-Java](https://github.com/stleary/JSON-java)
```
## Extension Settings

## Known Issues
```
when using terminal capture and send (allways on in this version)<br>
and using compile output with alot of data the data sent to client is alot after<br>
and continues to output long after compilation is finished.<br>
Fix is to not use compile output log. The result is allways printed.<br>
```
## Release Notes

### 1.0.0

Initial release of API_Webserver

### 1.0.1

Add GET and POST requests

### 1.0.2

Add Settings file

### 1.0.3

Add terminal capture and send to connected WebSocket client

### 1.0.4

* Add POST JSON data removeOtherFiles<br>
* Fix File write flag so that it overwrites existing files<br>

### 1.0.5

* Add sketch location keywords.txt file
* Add POST JSON data keywords that also save to sketch location keywords_temp.txt
* in POST JSON the three main objects "files", "removeOtherFiles" and "keywords" is optional

### 1.0.6

* Splitted out classes from API_Webserver.java
* ConfigDialog.java
* MyConsoleOutputStream.java (not currently used) replaced by simpler System.out hook
* MyHttpHandler.java
* MyWebSocketServer.java
* AutoCompleteProvider.java<br>
&nbsp;&nbsp;&nbsp;&nbsp;finally Arduino IDE gets autocomplete (experimental)<br>
&nbsp;&nbsp;&nbsp;&nbsp;can be activated on Tools-API Web Server-activate autocomplete<br>
&nbsp;&nbsp;&nbsp;&nbsp;define file is in tool dir of API_WebServer called c.xml<br>
&nbsp;&nbsp;&nbsp;&nbsp;(later this is getting a seperate repository as additional plugin)<br>

### 1.0.7

Created new classes
* CustomMenu.java
* IDEhelper.java
* Reflect.java // static reflect helper methods
* MyConsoleOutputStream.java is now used again (but currently not working)

### 1.0.8

Created new class<br>
* MidiHelper.java

added "overwrite_file" parameter to JSON post file <br>

### 1.0.9

Created new classes
* MidiWebSocketBridge.java 
* Main.java (making this jar standalone executable used by MidiWebSocketBridge)
supports exec. arguments:<br>
listdevices   (list midi devices)<br>
port 3001     (starts the websocket server at port 3001)<br>

-----------------------------------------------------------------------------------------------------------
