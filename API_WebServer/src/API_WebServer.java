/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2008 Ben Fry and Casey Reas
  Copyright (c) 2020 Jannik LS Svensson (1984)- Sweden

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.manicken;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;

import java.net.InetSocketAddress;

import java.util.Map;
import java.util.Scanner;
import java.util.prefs.Preferences;

import javax.lang.model.util.ElementScanner6;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import java.awt.Desktop;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.sun.net.httpserver.HttpServer;

import processing.app.BaseNoGui;
import processing.app.Editor;
import processing.app.tools.Tool;
import processing.app.PreferencesData;
import processing.app.SerialPlotter;

import org.json.*;


import com.manicken.AutoCompleteProvider;
import com.manicken.ConfigDialog;
import com.manicken.MyConsoleOutputStream;
import com.manicken.MyWebSocketServer;
import com.manicken.MyHttpHandler;
import com.manicken.Reflect;
import com.manicken.CustomMenu;
import com.manicken.IDEhelper;

import java.awt.Frame;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Example Tools menu entry.
 */
public class API_WebServer implements Tool {
	public String thisToolMenuTitle = "API Web Server";

	// settings (theese also defines the default values)
	boolean debugPrint = false;
	boolean useSeparateExtensionsMainMenu = true;
	boolean autoCloseOtherEditor = false;
	int webServerPort = 8080;
	int tcdwssPort = 3000;
	int bddwssPort = 3001;
	boolean autostart = true;
	boolean autoConvertMainCppToSketchMainIno  = true;

	ConfigDialog cd = null;

	public Editor editor;

	CustomMenu cm;
	IDEhelper ideh;

	public int instanceIndex = 0;
	public MidiHelper midi;
	public HttpServer webServer;
	public MyWebSocketServer bddwss; // BiDirDataWebSocketServer
	public MyWebSocketServer tcdwss; // Terminal Capture Data Web Socket Server
	
	boolean started = false;
	
	public String getMenuTitle() {// required by tool loader
		return thisToolMenuTitle;
	}

	public void init(Editor editor) { // required by tool loader
		this.editor = editor;

		editor.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {  init(); }
			public void windowClosed(WindowEvent e) { DisconnectServers(); } // this ensure that Disconnect is done on closed "Editors"
		});
		editor.addComponentListener( new ComponentListener() {
			public void componentResized( ComponentEvent e ) {}
			public void componentMoved( ComponentEvent e ) {}
			public void componentShown( ComponentEvent e ) {}
			public void componentHidden( ComponentEvent e ) { DisconnectServers(); }
		} );
	}

	public void CopyInstances(API_WebServer other)
	{
		other.webServer = this.webServer;
		other.bddwss = this.bddwss;
		other.tcdwss = this.tcdwss;
		other.midi = this.midi;
		other.instanceIndex = this.instanceIndex + 1;
	}

	public void run() { // required by tool loader
		
		LoadSettings();
		startWebServer();
		startBiDirDataWebSocketServer();
		startTerminalCaptureDataWebSocketServer();
		OpenPreviousMidi();
		MyConsoleOutputStream.setCurrentEditorConsole(ideh.editorConsole, ideh.console_stdOutStyle, ideh.console_stdErrStyle, tcdwss);
	}
	public void DisconnectServers()
	{
		System.err.println("DisconnectServers @ " + editor.getSketch().getName());
		midi.CloseDevices();
		// stop BiDirData WebSocketServer
		try {
			if (bddwss != null){ bddwss.stop(0);
			System.out.println("BiDirData WebSocket Server was stopped!@ " + editor.getSketch().getName());}
		} catch (Exception e) { System.err.println("cannot stop prev BiDirData WebSocket websocket server!!!"); e.printStackTrace();}
		// stop Terminal Capture WebSocketServer
		try {
			if (tcdwss != null){ tcdwss.stop(0);
			System.out.println("Terminal Capture WebSocket Server was stopped!@ " + editor.getSketch().getName());}
		} catch (Exception e) { System.err.println("cannot stop prev Terminal Capture websocket server!!!"); e.printStackTrace();}
		// stop WebServer
		try {
			if (webServer != null){ webServer.stop(0);
			System.out.println("web server was stopped!@ " + editor.getSketch().getName());}
		} catch (Exception e) { System.err.println("cannot stop prev web server!!!"); e.printStackTrace();}
	}

	private void StartServers() {
		startWebServer();;
		startBiDirDataWebSocketServer();
		startTerminalCaptureDataWebSocketServer();
	}
	private void OpenPreviousMidi()
	{
		String midiInDevice = PreferencesData.get("manicken.apiWebServer.midiInDevice", "");
		String midiOutDevice = PreferencesData.get("manicken.apiWebServer.midiOutDevice", "");
		if (midi.OpenDevices(midiInDevice, midiOutDevice))
			System.out.println(" midi open hurray!");
	}

	private void startWebServer() {
		if (webServer != null)
			try { webServer.stop(0); } catch (Exception e) {System.err.println(e + " @ " + e.getStackTrace() + e.getStackTrace()[0].getLineNumber());}
		try {
			webServer = HttpServer.create(new InetSocketAddress("localhost", webServerPort), 0);
			webServer.createContext("/", new  MyHttpHandler(this));
			webServer.setExecutor(null);
			webServer.start();

			System.out.println(" Web Server started on port " + webServerPort);
		} catch (Exception e) { System.err.println("cannot start web server!!!"); e.printStackTrace(); }
	}

	public void startBiDirDataWebSocketServer() {
		try {
			if (bddwss != null) bddwss.stop(0);
		} catch (Exception e) { System.err.println("cannot stop prev BiDirData websocket server!!!"); e.printStackTrace();}
		try {
			bddwss = new MyWebSocketServer(bddwssPort, (String message) -> bddwss_DecodeRawMessage(message));
			bddwss.start();
		} catch (Exception e) { System.err.println("cannot start bidirdata websocket server!!!"); e.printStackTrace(); }
	}

	public void startTerminalCaptureDataWebSocketServer() {
		try {
			if (tcdwss != null) tcdwss.stop(0);
		} catch (Exception e) { System.err.println("cannot stop prev terminal capture websocket server!!!"); e.printStackTrace();}
		try {
			tcdwss = new MyWebSocketServer(tcdwssPort, (String message) -> tcdwss_DecodeRawMessage(message));
			
			tcdwss.start();
		} catch (Exception e) { System.err.println("cannot start terminal capture websocket server!!!"); e.printStackTrace(); }
	}

	private void init() {
		if (PreferencesData.getBoolean("manicken.apiWebServer.DoDisconnectOnOtherEditors", true))
			IDEhelper.DoDisconnectOnOtherEditors(editor);

		if (PreferencesData.getBoolean("manicken.apiWebServer.closeOtherEditors", autoCloseOtherEditor))
			IDEhelper.CloseOtherEditors(editor);

		//System.out.println("BaseNoGui.getToolsFolder()=" + BaseNoGui.getToolsFolder());
		//System.out.println("BaseNoGui.getSketchbookFolder()=" + BaseNoGui.getSketchbookFolder());

		System.out.println("startin API_WebServer ...");
		try{
			ideh = new IDEhelper(editor);
			midi = new MidiHelper((String message) -> {	bddwss.broadcast("midiSend(" + message + ")<br>"); });

			System.out.println("rootDir="+ ideh.GetArduinoRootDir());
			cm = new CustomMenu(this, editor, thisToolMenuTitle, 
				new JMenuItem[] {
					CustomMenu.Item("Start/Restart", event -> run()),
					CustomMenu.Item("Stop", event -> DisconnectServers()),
					CustomMenu.Item("Settings", event -> ShowConfigDialog()),
					CustomMenu.Item("Start GUI Tool", event -> StartGUItool()),
					CustomMenu.Item("Init autocomplete", event -> ideh.ActivateAutoCompleteFunctionality()),
					CustomMenu.Item("test set plotter bg", event -> TestSetPlotterBG())
				});
			cm.Init(useSeparateExtensionsMainMenu);

			LoadSettings();
			if (autostart) {
				ideh.InitCustomKeywords();
				Timer timer = new Timer();
				timer.schedule(new TimerTask(){
					@Override
					public void run() {
						StartServers();
						OpenPreviousMidi();
					}
				}, 1000);
				//ideh.SystemOutHookStart(terminalCaptureWebSocketServerPort);
				MyConsoleOutputStream.setCurrentEditorConsole(ideh.editorConsole, ideh.console_stdOutStyle, ideh.console_stdErrStyle, tcdwss);
				started = true;
			}
			
		} catch (Exception e) {
			
			System.err.println("cannot reflect:");
			e.printStackTrace();
			System.err.println("API_WebServer not started!!!");
			return;
		}
	}
	void TestSetPlotterBG()
	{
		processing.app.SerialPlotter plotter = (processing.app.SerialPlotter)Reflect.GetField("serialPlotter", editor);
		try {
			plotter.add(new javax.swing.JLabel(new javax.swing.ImageIcon(javax.imageio.ImageIO.read(new java.io.File("C:\\Users\\tde04\\Desktop\\till PJRC forum\\1200px-Clipping.svg.png")))));
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
		plotter.pack();
		plotter.setVisible(true);
	}
	public void StartGUItool() {
		try {
			File htmlFile = new File(ideh.GetArduinoRootDir() + "/hardware/teensy/avr/libraries/Audio/gui/index.html");
			Desktop.getDesktop().browse(htmlFile.toURI());
			System.out.println("Web page opened in browser");
		} catch (Exception e) {  e.printStackTrace(); }
	}

	private void refreshMidiDevices()
	{
		//cd.lstMidiDevices.clear();
		cd.lstMidiDeviceIn.setListData(midi.GetInDeviceList());
		cd.lstMidiDeviceOut.setListData(midi.GetOutDeviceList());
	}
	public void ShowConfigDialog() {
		if (cd == null)
		{
			cd = new ConfigDialog();
			cd.btnRefreshMidiDevices.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) { 
				    refreshMidiDevices();
				} 
			});
		}
		refreshMidiDevices(); // this starts with list allready populated
		cd.txtWebServerPort.setText(Integer.toString(webServerPort));
		cd.txtTermCapWebSocketServerPort.setText(Integer.toString(tcdwssPort));
		cd.txtBiDirDataWebSocketServerPort.setText(Integer.toString(bddwssPort));
		cd.chkAutostart.setSelected(autostart);
		cd.chkAutoCloseOtherEditor.setSelected(autoCloseOtherEditor);
		cd.chkAutoConvertMainCppToSketchMainIno.setSelected(autoConvertMainCppToSketchMainIno);
		cd.chkDebugMode.setSelected(debugPrint);
		cd.lstMidiDeviceIn.setSelectedIndex(midi.selectedInDeviceIndex);
		cd.lstMidiDeviceOut.setSelectedIndex(midi.selectedOutDeviceIndex);
		
	   int result = JOptionPane.showConfirmDialog(editor, cd, "API Web Server Config" ,JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			
		if (result == JOptionPane.OK_OPTION) {
			webServerPort = Integer.parseInt(cd.txtWebServerPort.getText());
			tcdwssPort = Integer.parseInt(cd.txtTermCapWebSocketServerPort.getText());
			bddwssPort = Integer.parseInt(cd.txtBiDirDataWebSocketServerPort.getText());
			autostart = cd.chkAutostart.isSelected();
			debugPrint = cd.chkDebugMode.isSelected();
			midi.selectedInDeviceIndex = cd.lstMidiDeviceIn.getSelectedIndex();
			midi.selectedOutDeviceIndex = cd.lstMidiDeviceOut.getSelectedIndex();
			autoCloseOtherEditor = cd.chkAutoCloseOtherEditor.isSelected();
			autoConvertMainCppToSketchMainIno = cd.chkAutoConvertMainCppToSketchMainIno.isSelected();

			if (midi.OpenDevices())
				System.out.println("hurray");

			SaveSettings();
		} else { System.out.println("Cancelled"); }
	}
	private void LoadSettings()
	{
		webServerPort = PreferencesData.getInteger("manicken.apiWebServer.webServerPort", webServerPort);
		tcdwssPort = PreferencesData.getInteger("manicken.apiWebServer.terminalCaptureWebSocketServerPort", tcdwssPort);
		bddwssPort = PreferencesData.getInteger("manicken.apiWebServer.biDirDataWebSocketServerPort", bddwssPort);
		autostart =	PreferencesData.getBoolean("manicken.apiWebServer.autostart", autostart);
		debugPrint = PreferencesData.getBoolean("manicken.apiWebServer.debugPrint", debugPrint);
		autoCloseOtherEditor = PreferencesData.getBoolean("manicken.apiWebServer.closeOtherEditors", autoCloseOtherEditor);
		autoConvertMainCppToSketchMainIno = PreferencesData.getBoolean("manicken.apiWebServer.autoConvertMainCppToSketchMainIno", autoConvertMainCppToSketchMainIno);
	}
	private void SaveSettings()
	{
		PreferencesData.setInteger("manicken.apiWebServer.webServerPort", webServerPort);
		PreferencesData.setInteger("manicken.apiWebServer.terminalCaptureWebSocketServerPort", tcdwssPort);
		PreferencesData.setInteger("manicken.apiWebServer.biDirDataWebSocketServerPort", bddwssPort);
		PreferencesData.setBoolean("manicken.apiWebServer.autostart", autostart);
		PreferencesData.setBoolean("manicken.apiWebServer.debugPrint", debugPrint);
		PreferencesData.setBoolean("manicken.apiWebServer.autoConvertMainCppToSketchMainIno", autoConvertMainCppToSketchMainIno);
		PreferencesData.set("manicken.apiWebServer.midiInDevice", midi.GetCurrentInDeviceNameDescr());
		PreferencesData.set("manicken.apiWebServer.midiOutDevice", midi.GetCurrentOutDeviceNameDescr());
		
	}	
	private void tcdwss_DecodeRawMessage(String message)
	{
		System.out.println("terminalCaptureWebSocketServerData : " + message);
	}
	private void bddwss_DecodeRawMessage(String message)
	{
		message = message.toLowerCase();
		if (message.startsWith("midisend")) // most common check first
			bddwss_DecodeMidiMessage(message);			
		else if (message.equals("midigetdevices"))
			bddwss_GetMidiDevices();
		else if (message.startsWith("midisetdevice"))
			bddwss_SetMidiDevice(message.substring("midisetdevice".length()));
		else
			System.out.println("biDirDataWebSocketServerData unknown command: " + message);
	}
	private String GetSubStringBetween(String in, String startToken, String endToken)
	{
		int beginIndex = in.indexOf("(");
		if (beginIndex == -1) { return null; }
		int endIndex = in.indexOf(")");
		if (endIndex == -1) { return null; }
		return in.substring(beginIndex+1, endIndex);
	}
	private void bddwss_DecodeMidiMessage(String message)
	{
		message = GetSubStringBetween(message, "(", ")");
		if (message == null)
		{
			bddwss.broadcast("err. midi send missing (first or last) parantesis");
			return; 
		}
		System.out.println("midisend " + message);
		String[] params = message.split(",");
		if (params.length != 3) {bddwss.broadcast("err. midi send params.length != 3"); return;}
		int[] intParams = new int[params.length];
		for (int i = 0; i < 3; i++)
		{
			params[i] = params[i].trim();
			if (params[i].startsWith("0x"))
			{
				params[i] = params[i].substring(2);
				intParams[i] = Integer.parseInt(params[i], 16);
			}
			else
			{
				intParams[i] = Integer.parseInt(params[i]);
			}
			//System.out.println("midisend byte" + i + ":" + intParams[i]);
		}
		midi.Send(intParams);
	}
	private void bddwss_GetMidiDevices()
	{
		String[] inDev = midi.GetInDeviceList();
		String[] outDev = midi.GetOutDeviceList();

		StringBuilder sb = new StringBuilder();
		sb.append("midiDevicesIn(");
		for (int i = 0; i < inDev.length; i++)
		{
			sb.append(inDev[i]);
			if (i < inDev.length -1)
				sb.append("\t");
		}
		sb.append(")["+midi.selectedInDeviceIndex+"]\n");
		bddwss.broadcast(sb.toString());
		
		sb = new StringBuilder();
		sb.append("midiDevicesOut(");
		for (int i = 0; i < outDev.length; i++)
		{
			sb.append(outDev[i]);
			if (i < outDev.length -1)
				sb.append("\t");
		}
		sb.append(")["+midi.selectedOutDeviceIndex+"]\n");
		bddwss.broadcast(sb.toString());
	}
	private void bddwss_SetMidiDevice(String message)
	{
		String param = GetSubStringBetween(message, "(", ")");
		if (param == null)
		{
			bddwss.broadcast("err. midi set device missing (first or last) parantesis");
			return; 
		}
		int index = 0;
		try {index = Integer.parseInt(param);} catch (Exception ex) {bddwss.broadcast("err. midi set device index is not a integer"); return;}

		if (message.startsWith("in"))
		{
			midi.selectedInDeviceIndex = index;
			if (midi.OpenInDevice())
				System.out.println("Input: " + midi.inDevice.getDeviceInfo()+" Was Opened");
		}
		else if (message.startsWith("out"))
		{
			midi.selectedOutDeviceIndex = index;
			if (midi.OpenOutDevice())
				System.out.println("Output: " + midi.outDevice.getDeviceInfo()+" Was Opened");
		}
		else
		{
			bddwss.broadcast("err. midi set device - missing 'in' or 'out'");
			return;
		}
	}

	public String parseGET(Map<String, String> query) {
		String cmd = query.get("cmd");
		//if (!cmd.equals("ping"))
		//	System.out.println("GET request params: " + cmd);
		if (cmd.equals("ping"))
		{
			// do nothing, a OK is default to send back
		}
		else if (cmd.equals("compile"))
		{
			ideh.verifyCompile();
			System.out.println("WSAPI compile");
		}
		else if (cmd.equals("upload"))
		{   
			ideh.upload();
			System.out.println("WSAPI upload");
		}
		else if (cmd.equals("renameFile"))
		{
			String from = query.get("from");
			if (from == null) { System.out.println("Missing 'from' parameter @ renameFile"); return "Missing 'from' parameter @ renameFile"; }
			String to = query.get("to");
			if (to == null) { System.out.println("Missing 'to' parameter @ renameFile"); return "Missing 'to' parameter @ renameFile"; }
			System.out.println("WSAPI renameFile from:" + from + ", to:" + to);
			ideh.renameFile(from, to);
		}
		else if (cmd.equals("removeFile"))
		{
			String name = query.get("fileName");
			if (name == null) { System.out.println("Missing 'fileName' parameter @ removeFile"); return "Missing 'fileName' parameter @ removeFile"; }
			System.out.println("WSAPI removeFile:" + name);
			ideh.removeFile(name);
		}
		else if(cmd.equals("getFile"))
		{
			String name = query.get("fileName");
			if (name == null) { System.out.println("Missing 'fileName' parameter @ getFile"); return "Missing 'fileName' parameter @ getFile"; }
			System.out.println("WSAPI getFile:" + name);
			return ideh.getFile(name);
		}
		else
			return "unknown GET cmd: " + cmd;

		return "OK"; // default
	}
	public synchronized String parsePOST(String data)
	{
		String returnStr = "";
		JSONObject jsonObj = new JSONObject(data);
		Boolean removeOtherFiles = false;
		JSONArray arrFiles = null;
		JSONArray arrKeywords = null;
		
		try { removeOtherFiles = jsonObj.getBoolean("removeOtherFiles"); } catch (Exception e) { returnStr = " >>>warning: removeOtherFiles missing in JSON<<< ";if (debugPrint)e.printStackTrace(); };
		try { arrFiles = jsonObj.getJSONArray("files"); } catch (Exception e) { returnStr += " >>>error: files array missing in JSON<<< "; if (debugPrint)e.printStackTrace();}
		try { arrKeywords = jsonObj.getJSONArray("keywords"); } catch (Exception e) { returnStr += " >>>warning: keywords array missing in JSON<<< "; if (debugPrint)e.printStackTrace();}
		
		if (removeOtherFiles)
		ideh.RemoveFilesNotInJSON(arrFiles, autoConvertMainCppToSketchMainIno);

		if (arrFiles != null)
			returnStr += parsePOST_JSONfiles(arrFiles);

		if (arrKeywords != null)
			returnStr += parsePOST_JSONkeywords(arrKeywords);
		
		if (debugPrint)	System.out.print(data + "\n");
		editor.handleSave(true);
		if (returnStr.equals("")) returnStr = "OK";
		System.out.println(returnStr);
		return returnStr;
	}
	private String parsePOST_JSONfiles(JSONArray arrFiles)
	{
		String returnStr = "";
		for (int i = 0; i < arrFiles.length(); i++)
		{
			JSONObject file = arrFiles.getJSONObject(i);
			String name = "";
			String contents = "";
			Boolean OverwriteFile = false;
			try { OverwriteFile = file.getBoolean("overwrite_file"); } catch (Exception e) { returnStr += " >>>error: fileObject don't contain overwrite_file<<< ";if (debugPrint)e.printStackTrace(); continue; }
			
			if (OverwriteFile == false) continue;
			
			try { name = file.getString("name"); } catch (Exception e) { returnStr += " >>>error: fileObject don't contain name<<< ";if (debugPrint)e.printStackTrace(); continue; }
			try { contents = file.getString("contents"); } catch (Exception e) { returnStr += " >>>error: fileObject don't contain contents<<< ";if (debugPrint)e.printStackTrace(); continue; }
			
			if (autoConvertMainCppToSketchMainIno && name.toLowerCase().equals("main.cpp"))
				name = editor.getSketch().getName() + ".ino";

			if (name.endsWith(".cpp") || name.endsWith(".c") || name.endsWith(".h") || name.endsWith(".hpp") || name.endsWith(".ino"))
				ideh.addNewFile(name, contents); // adds a new file to the sketch-project
			else
				ideh.setFile(name, contents); // this writes a file without the IDE knowing it
		}
		return returnStr;
	}
	private String parsePOST_JSONkeywords(JSONArray arrKeywords)
	{
		StringBuilder sbKeywords = new StringBuilder();
		String returnStr = "";
		for (int i = 0; i < arrKeywords.length(); i++)
		{
			JSONObject keyword = arrKeywords.getJSONObject(i);
			String token = "";
			String type = "";
			try { token = keyword.getString("token"); } catch (Exception e) { returnStr += " >>>error: keyword don't contain token<<< ";if (debugPrint)e.printStackTrace(); continue; }
			try { type = keyword.getString("type"); } catch (Exception e) { returnStr += " >>>error: keyword don't contain type<<< ";if (debugPrint)e.printStackTrace(); continue; }
			
			sbKeywords.append(token+"\t"+type+"\r\n");
			ideh.keywordOldToken.put(token, type);
		}
		ideh.pdeKeywords_fillMissingTokenType();
		editor.updateKeywords(ideh.pdeKeywords);
		String sbKeywordsContents = sbKeywords.toString();
		if (debugPrint)	System.out.println("setting new keywords:\n" + sbKeywordsContents);

		if (!sbKeywordsContents.equals(""))
			ideh.setFile(ideh.sketchKeywordsTempFileName, sbKeywordsContents);
		return returnStr;
	}
}
