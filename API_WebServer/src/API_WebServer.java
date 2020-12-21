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

import org.json.*;

import com.manicken.AutoCompleteProvider;
import com.manicken.ConfigDialog;
import com.manicken.MyConsoleOutputStream;
import com.manicken.MyWebSocketServer;
import com.manicken.MyHttpHandler;
import com.manicken.Reflect;
import com.manicken.CustomMenu;
import com.manicken.IDEhelper;

/**
 * Example Tools menu entry.
 */
public class API_WebServer implements Tool {
	boolean debugPrint = false;
	boolean useSeparateExtensionsMainMenu = true;

	ConfigDialog cd = null;

	Editor editor;

	CustomMenu cm;
	IDEhelper ideh;
	MidiHelper midi;

	HttpServer server;
	MyWebSocketServer mwss; // this is used for bi directional data
	
	String thisToolMenuTitle = "API Web Server";

	int webServerPort = 8080; // replaced by code down
	int terminalCaptureWebSocketServerPort = 3000;
	int biDirDataWebSocketServerPort = 3001;
	boolean autostart = true; // replaced by code down
	
	boolean started = false;
	
	public String getMenuTitle() {// required by tool loader
		return thisToolMenuTitle;
	}

	public void init(Editor editor) { // required by tool loader
		this.editor = editor;

		editor.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) { init(); }
		});
	}

	public void run() { // required by tool loader
		LoadSettings();
		startWebServer();
		startWebsocketServer();		
	}

	public void startWebsocketServer() {
		try {
			mwss = new MyWebSocketServer(biDirDataWebSocketServerPort, (String message) -> {
				message = message.toLowerCase();
				if (message.startsWith("midi"))
				{
					int beginIndex = message.indexOf("(");
					if (beginIndex == -1) { mwss.broadcast("midi send missing first ("); return; }
					int endIndex = message.indexOf(")");
					if (endIndex == -1) { mwss.broadcast("midi send missing last )"); return; }

					message = message.substring(beginIndex+1, endIndex);
					System.out.println("midisend " + message);
					String[] params = message.split(",");
					if (params.length != 3) {mwss.broadcast("midi send params.length != 3"); return;}
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
				else
					System.out.println("biDirDataWebSocketServerData unknown command: " + message);
			}
			);
			mwss.start();
		} catch (Exception e) { System.err.println("cannot start websocket server!!!"); e.printStackTrace(); }
	}

	private void startWebServer() {
		if (server != null)
			try { server.stop(1); } catch (Exception e) {System.err.println(e + " @ " + e.getStackTrace() + e.getStackTrace()[0].getLineNumber());}
		try {
			server = HttpServer.create(new InetSocketAddress("localhost", webServerPort), 0);
			server.createContext("/", new  MyHttpHandler(this));
			server.setExecutor(null);
			server.start();

			System.out.println(" Server started on port " + webServerPort);
		} catch (Exception e) { System.err.println("cannot start web server!!!"); e.printStackTrace(); }
	}

	private void init() {
		//System.out.println("BaseNoGui.getToolsFolder()=" + BaseNoGui.getToolsFolder());
		//System.out.println("BaseNoGui.getSketchbookFolder()=" + BaseNoGui.getSketchbookFolder());
		if (started) {
			System.out.println("Server is allready running at port " + webServerPort);
			return;
		}
		System.out.println("startin API_WebServer ...");
		try{
			ideh = new IDEhelper(editor);
			midi = new MidiHelper((String message) -> {
				mwss.broadcast("midi(" + message + ")<br>");
				});
			System.out.println("rootDir="+ ideh.GetArduinoRootDir());
			cm = new CustomMenu(editor, thisToolMenuTitle, 
				new JMenuItem[] {
					CustomMenu.Item("Start/Restart Server", event -> run()),
					CustomMenu.Item("Settings", event -> ShowConfigDialog()),
					CustomMenu.Item("Start GUI Tool", event -> StartGUItool()),
					CustomMenu.Item("Init autocomplete", event -> ideh.ActivateAutoCompleteFunctionality())
				});
			cm.Init(useSeparateExtensionsMainMenu);

			started = true;
			
			ideh.InitCustomKeywords();
			LoadSettings();
			
		} catch (Exception e) {
			
			System.err.println("cannot reflect:");
			e.printStackTrace();
			System.err.println("API_WebServer not started!!!");
			return;
		}
		
		if (autostart) {
			startWebServer();
			startWebsocketServer();
			ideh.SystemOutHookStart(terminalCaptureWebSocketServerPort);
			//if (mwss != null)
			//	MyConsoleOutputStream.setCurrentEditorConsole(ideh.editorConsole, ideh.console_stdOutStyle, ideh.console_stdErrStyle, mwss);
		}
		//ActivateAutoCompleteFunctionality();
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

		cd.txtWebServerPort.setText(Integer.toString(webServerPort));
		cd.txtTermCapWebSocketServerPort.setText(Integer.toString(terminalCaptureWebSocketServerPort));
		cd.txtBiDirDataWebSocketServerPort.setText(Integer.toString(biDirDataWebSocketServerPort));
		cd.chkAutostart.setSelected(autostart);
		cd.chkDebugMode.setSelected(debugPrint);
		
		
	   int result = JOptionPane.showConfirmDialog(editor, cd, "API Web Server Config" ,JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			
		if (result == JOptionPane.OK_OPTION) {
			webServerPort = Integer.parseInt(cd.txtWebServerPort.getText());
			terminalCaptureWebSocketServerPort = Integer.parseInt(cd.txtTermCapWebSocketServerPort.getText());
			biDirDataWebSocketServerPort = Integer.parseInt(cd.txtBiDirDataWebSocketServerPort.getText());
			autostart = cd.chkAutostart.isSelected();
			debugPrint = cd.chkDebugMode.isSelected();
			midi.selectedInDeviceIndex = cd.lstMidiDeviceIn.getSelectedIndex();
			midi.selectedOutDeviceIndex = cd.lstMidiDeviceOut.getSelectedIndex();
			if (midi.OpenDevices())
				System.out.println("hurray");

			SaveSettings();
		} else { System.out.println("Cancelled"); }
	}
	private void LoadSettings()
	{
		webServerPort = PreferencesData.getInteger("manicken.apiWebServer.webServerPort", webServerPort);
		terminalCaptureWebSocketServerPort = PreferencesData.getInteger("manicken.apiWebServer.terminalCaptureWebSocketServerPort", terminalCaptureWebSocketServerPort);
		biDirDataWebSocketServerPort = PreferencesData.getInteger("manicken.apiWebServer.biDirDataWebSocketServerPort", biDirDataWebSocketServerPort);
		autostart =	PreferencesData.getBoolean("manicken.apiWebServer.autostart", autostart);
		debugPrint = PreferencesData.getBoolean("manicken.apiWebServer.debugPrint", debugPrint);
		String midiInDevice = PreferencesData.get("manicken.apiWebServer.midiInDevice", "");
		String midiOutDevice = PreferencesData.get("manicken.apiWebServer.midiOutDevice", "");
		if (midi.OpenDevices(midiInDevice, midiOutDevice))
			System.out.println("hurray!");
	}
	private void SaveSettings()
	{
		PreferencesData.setInteger("manicken.apiWebServer.webServerPort", webServerPort);
		PreferencesData.setInteger("manicken.apiWebServer.terminalCaptureWebSocketServerPort", terminalCaptureWebSocketServerPort);
		PreferencesData.setInteger("manicken.apiWebServer.biDirDataWebSocketServerPort", biDirDataWebSocketServerPort);
		PreferencesData.setBoolean("manicken.apiWebServer.autostart", autostart);
		PreferencesData.setBoolean("manicken.apiWebServer.debugPrint", debugPrint);
		PreferencesData.set("manicken.apiWebServer.midiInDevice", midi.GetCurrentInDeviceNameDescr());
		PreferencesData.set("manicken.apiWebServer.midiOutDevice", midi.GetCurrentOutDeviceNameDescr());
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
		ideh.RemoveFilesNotInJSON(arrFiles);

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
			try { name = file.getString("name"); } catch (Exception e) { returnStr += " >>>error: fileObject don't contain name<<< ";if (debugPrint)e.printStackTrace(); continue; }
			try { contents = file.getString("contents"); } catch (Exception e) { returnStr += " >>>error: fileObject don't contain contents<<< ";if (debugPrint)e.printStackTrace(); continue; }
			
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
