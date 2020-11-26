# api-webserver README

This extension make it possible to take control of Arduino IDE from a Web Page based client.

## Install

global:
&nbsp;&nbsp;download this repository by either Code-Download Zip or by git clone https://github.com/manicken/arduinoAPIwebserver.git
&nbsp;&nbsp;then extract/open the repository

on windows / linux:
&nbsp;&nbsp;copy folder API_WebServer to [Arduino IDE install location]/tools directory
&nbsp;&nbsp;ex: /Arduino-1.8.13/tools

on mac:
&nbsp;&nbsp;In Applications right click and click on "Show Package Contents", then browse Contents -> Java -> tools
&nbsp;&nbsp;by holding the Option key(copy) drag folder API_WebServer from the downloaded repository to the open tools folder above
&nbsp;&nbsp;select replace it you allready have an older version

## Features

* POST request with data contents in json format:
```
{
    "files":[
        {
            "name":"Main.c",
            "contents":""
        },
        {
            "name":"Main.h",
            "contents":""
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
in above json:
removeOtherFiles mean that when this is set to true
 then files that is not present in the JSON is removed 
 from the sketch (note. the main sketch file is never touched)
 it should be set to false if only known files need to be replaced/added.

 keywords uses the same naming as keywords.txt

 POST JSON the three main objects "files", "removeOtherFiles" and "keywords" is optional
 but the removeOtherFiles is allways used together with "files"

* GET request
possible query strings:
```
http://localhost:8080?cmd=getFile&fileName=fileNameWithExt
http://localhost:8080?cmd=renameFile&from=fromNameWithExt&to=toNameWithExt
http://localhost:8080?cmd=removeFile&fileName=fileNameWithExt
http://localhost:8080?cmd=compile
http://localhost:8080?cmd=upload
http://localhost:8080?cmd=ping
```

* uses sketch location keywords.txt for additional custom keywords

## Requirements
```
Java-WebSocket-1.5.1.jar (included) from [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket)
json-20200518.jar (included) from [JSON-Java](https://github.com/stleary/JSON-java)
```
## Extension Settings

## Known Issues
```
when using terminal capture and send (allways on in this version)
and using compile output with alot of data the data sent to client is alot after
and continues to output long after compilation is finished.
Fix is to not use compile output log. The result is allways printed.
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

Add POST JSON data removeOtherFiles
Fix File write flag so that it overwrites existing files

### 1.0.5

Add sketch location keywords.txt file
Add POST JSON data keywords that also save to sketch location keywords_temp.txt
in POST JSON the three main objects "files", "removeOtherFiles" and "keywords" is optional

### 1.0.6

Splitted out classes from API_Webserver.java
ConfigDialog.java
MyConsoleOutputStream.java (not currently used) replaced by simpler System.out hook
MyHttpHandler.java
MyWebSocketServer.java
AutoCompleteProvider.java finally Arduino IDE gets autocomplete
                          (this is getting a seperate repository as additional plugin)

-----------------------------------------------------------------------------------------------------------
