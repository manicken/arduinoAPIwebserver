/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2008 Ben Fry and Casey Reas

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
import java.io.OutputStream;
import java.io.InputStream;
import java.io.*;
import java.net.InetSocketAddress;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

//import javax.swing.JOptionPane;

import processing.app.Base;
import processing.app.BaseNoGui;
import processing.app.Editor;
import processing.app.tools.Tool;
import processing.app.Sketch;
import processing.app.EditorTab;
import processing.app.syntax.SketchTextArea;
import processing.app.SketchFile;
import processing.app.EditorHeader;
import processing.app.EditorConsole;
import processing.app.syntax.PdeKeywords;

import java.util.Scanner;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import static processing.app.I18n.tr;

import javax.swing.JOptionPane;
import javax.lang.model.util.ElementScanner6;
import javax.swing.*;
import javax.swing.text.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;

import java.nio.file.Path;

import org.json.*;

import java.awt.Desktop;
import java.net.URI;


import com.manicken.AutoCompleteProvider;
import com.manicken.ConfigDialog;
import com.manicken.MyConsoleOutputStream;
import com.manicken.MyWebSocketServer;
import com.manicken.MyHttpHandler;

/**
 * Example Tools menu entry.
 */
public class API_WebServer implements Tool {
	boolean debugPrint = false;

	Base base;// for the API uses reflection to get
	Editor editor;// for the API
	Sketch sketch; // for the API
	ArrayList<EditorTab> tabs; // for the API uses reflection to get
	EditorHeader header; // for the API uses reflection to get
	Runnable runHandler; // for the API uses reflection to get
	Runnable presentHandler; // for the API uses reflection to get

	PdeKeywords pdeKeywords; // for the API uses reflection to get
	Map<String, String> keywordOldToken; // for the API uses reflection to get
	String sketchKeywordsFileName = "keywords.txt";
	String sketchKeywordsTempFileName = "keywords_temp.txt"; // updated by external editor
	
	JMenu toolsMenu; // for the API uses reflection to get
	
	HttpServer server;
	
	int DefaultServerPort = 8080;
	boolean DefaultAutoStart = true;
	String thisToolMenuTitle = "API Web Server";
	String rootDir;
	
	int serverPort = 8080; // replaced by code down
	boolean autostart = true; // replaced by code down
	
	boolean started = false;
	public MyWebSocketServer cs;

	EditorConsole editorConsole;
	
	private SimpleAttributeSet console_stdOutStyle;
	private SimpleAttributeSet console_stdErrStyle;
	String outFgColorHex;
	String outBgColorHex;
	String errFgColorHex;
	String errBgColorHex;

	public void init(Editor editor) { // required by tool loader
		this.editor = editor;

		editor.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
			  init();
			}
		});
		
	}
	private void SystemOutHookStart()
	{
		Color fgColor = StyleConstants.getForeground(console_stdOutStyle);
		outFgColorHex = "#" + Integer.toHexString(fgColor.getRGB() | 0xFF000000).substring(2);
		Color bgColor = StyleConstants.getBackground(console_stdOutStyle);
		outBgColorHex = "#" + Integer.toHexString(bgColor.getRGB() | 0xFF000000).substring(2);

		fgColor = StyleConstants.getForeground(console_stdErrStyle);
		errFgColorHex = "#" + Integer.toHexString(fgColor.getRGB() | 0xFF000000).substring(2);
		bgColor = StyleConstants.getBackground(console_stdErrStyle);
		errBgColorHex = "#" + Integer.toHexString(bgColor.getRGB() | 0xFF000000).substring(2);

		PrintStream psOut = new PrintStream(System.out, true) {
			@Override
			public void println(String x) {
				cs_SendWithStyle(outFgColorHex, outBgColorHex, x + "<br>");
				super.println(x);
			}
		};
		System.setOut(psOut);

		PrintStream psErr = new PrintStream(System.out, true) {
			@Override
			public void println(String x) {
				cs_SendWithStyle(errFgColorHex, errBgColorHex, x + "<br>");
				super.println(x);
			}
		};
		System.setErr(psErr);
	}
	private void cs_SendWithStyle(String fgColorHex, String bgColorHex, String text)
	{
		if (cs == null) return;
		cs.SendWithHtmlStyle(fgColorHex, bgColorHex, text);
	}
	public void run() {// required by tool loader
		LoadSettings();
		startWebServer();
		
		startWebsocketServer();		
		//System.out.println("Hello World!");
	}
	public void startWebsocketServer()
	{
		try {
			cs = new MyWebSocketServer(3000);
			cs.start();
			} catch (Exception e)
			{
				System.err.println("cannot start websocket server!!!");
				e.printStackTrace();
			}
	}
	public String getMenuTitle() {// required by tool loader
		return thisToolMenuTitle;
	}
	private Object ReflectGetField(String name, Object src)
	{
		try {
		Field f = src.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(src);
		}
		catch (Exception e)
		{
			System.err.println("****************************************");
			System.err.println("************cannot reflect**************");
			System.err.println("****************************************");
			e.printStackTrace();
			return null;
		}
	}
	private void init()
	{
		System.out.println("BaseNoGui.getToolsFolder()=" + BaseNoGui.getToolsFolder());
		System.out.println("BaseNoGui.getSketchbookFolder()=" + BaseNoGui.getSketchbookFolder());
		if (started)
		{
			System.out.println("Server is allready running at port " + serverPort);
			return;
		}
		System.out.println("init API_WebServer");
		rootDir = GetArduinoRootDir();
		System.out.println("rootDir="+rootDir);
		try{
			sketch = this.editor.getSketch();
			
			base = (Base) ReflectGetField("base", this.editor);
			pdeKeywords = base.getPdeKeywords(); // no need to use reflection here but using reflected base
			keywordOldToken = (Map<String, String>) ReflectGetField("keywordOldToken", this.pdeKeywords);
			editorConsole = (EditorConsole) ReflectGetField("console", this.editor);
			console_stdOutStyle = (SimpleAttributeSet) ReflectGetField("stdOutStyle", this.editorConsole);
			console_stdErrStyle = (SimpleAttributeSet) ReflectGetField("stdErrStyle", this.editorConsole);
			tabs = (ArrayList<EditorTab>) ReflectGetField("tabs", this.editor);
			header = (EditorHeader) ReflectGetField("header", this.editor);
			runHandler = (Runnable) ReflectGetField("runHandler", this.editor);
			presentHandler = (Runnable) ReflectGetField("presentHandler", this.editor);
			toolsMenu = (JMenu) ReflectGetField("toolsMenu", this.editor);

			/*
			Field f ;
			f = Editor.class.getDeclaredField("base");
			f.setAccessible(true);
			base = (Base) f.get(this.editor);

			f = PdeKeywords.class.getDeclaredField("keywordOldToken");
			f.setAccessible(true);
			keywordOldToken = (Map<String, String>) f.get(pdeKeywords);

			f = Editor.class.getDeclaredField("console");
			f.setAccessible(true);
			editorConsole = (EditorConsole) f.get(this.editor);

			f = EditorConsole.class.getDeclaredField("stdOutStyle");
			f.setAccessible(true);
			console_stdOutStyle = (SimpleAttributeSet) f.get(this.editorConsole);

			f = EditorConsole.class.getDeclaredField("stdErrStyle");
			f.setAccessible(true);
			console_stdErrStyle = (SimpleAttributeSet) f.get(this.editorConsole);

			f = Editor.class.getDeclaredField("tabs");
			f.setAccessible(true);
			tabs = (ArrayList<EditorTab>) f.get(this.editor);
			
			f = this.editor.getClass().getDeclaredField("header");
			f.setAccessible(true);
			header = (EditorHeader) f.get(this.editor);
			
			f = Editor.class.getDeclaredField("runHandler");
			f.setAccessible(true);
			runHandler = (Runnable) f.get(this.editor);
			
			f = Editor.class.getDeclaredField("presentHandler");
			f.setAccessible(true);
			presentHandler = (Runnable) f.get(this.editor);
			
			f = Editor.class.getDeclaredField("toolsMenu");
			f.setAccessible(true);
			toolsMenu = (JMenu) f.get(this.editor);
			*/
			
			int thisToolIndex = GetMenuItemIndex(toolsMenu, thisToolMenuTitle);
			JMenu thisToolMenu = new JMenu(thisToolMenuTitle);		
			toolsMenu.insert(thisToolMenu, thisToolIndex+1);
			toolsMenu.remove(thisToolIndex);
			
			JMenuItem newItem = new JMenuItem("Start/Restart Server");
			thisToolMenu.add(newItem);
			newItem.addActionListener(event -> run());
			
			newItem = new JMenuItem("Settings");
			thisToolMenu.add(newItem);
			newItem.addActionListener(event -> ShowConfigDialog());

			newItem = new JMenuItem("Start GUI Tool");
			thisToolMenu.add(newItem);
			newItem.addActionListener(event -> StartGUItool());

			newItem = new JMenuItem("Init autocomplete");
			thisToolMenu.add(newItem);
			newItem.addActionListener(event -> ActivateAutoCompleteFunctionality());

			started = true;
			
			loadSketchKeywordsFile();
			loadSketchKeywordsTempFile(); // used by the external tool/editor

			//keywordOldToken.put("Jannik", "KEYWORD2");
			//keywordOldToken.put("Svensson", "LITERAL1");
			//keywordOldToken.put("Jannik", "LITERAL1");
			//pdeKeywords_fillMissingTokenType(); // only needed after new keywords is added "manually"
			editor.updateKeywords(pdeKeywords); // this applys the changes
			
		}catch (Exception e)
		{
			sketch = null;
			tabs = null;
			System.err.println("cannot reflect:");
			e.printStackTrace();
			System.err.println("API_WebServer not started!!!");
			return;
		}
		LoadSettings();
		if (autostart)
		{
			startWebServer();
			startWebsocketServer();
			SystemOutHookStart();
			MyConsoleOutputStream.setCurrentEditorConsole(editorConsole, console_stdOutStyle, console_stdErrStyle, cs);
		}
		//ActivateAutoCompleteFunctionality();
	}
	public void ActivateAutoCompleteFunctionality()
	{
		for (int i = 0; i < tabs.size(); i++)
		{
			SketchTextArea textArea = tabs.get(i).getTextArea();
			AutoCompleteProvider acp = new AutoCompleteProvider(textArea, GetJarFileDir());
		}
	}
	public void pdeKeywords_fillMissingTokenType()
	{
		try {
			Method m = PdeKeywords.class.getDeclaredMethod("fillMissingTokenType");
			m.setAccessible(true);
			m.invoke(pdeKeywords);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke editor_addTab");
			e.printStackTrace();
		}
	}
	public void pdeKeywords_parseKeywordsTxt(File file)
	{
		try {
			Method m = PdeKeywords.class.getDeclaredMethod("parseKeywordsTxt", File.class);
			m.setAccessible(true);
			m.invoke(pdeKeywords, file);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke editor_addTab");
			e.printStackTrace();
		}
	}
	public void loadSketchKeywordsFile()
	{
		File file = new File(sketch.getFolder(), sketchKeywordsFileName);
		if (!file.exists()) return;
		pdeKeywords_parseKeywordsTxt(file);
	}
	public void loadSketchKeywordsTempFile()
	{
		File file = new File(sketch.getFolder(), sketchKeywordsTempFileName);
		if (!file.exists()) return;
		pdeKeywords_parseKeywordsTxt(file);
	}
	public void StartGUItool()
	{
		try {
			File htmlFile = new File(rootDir + "/hardware/teensy/avr/libraries/Audio/gui/index.html");
			Desktop.getDesktop().browse(htmlFile.toURI());
			System.out.println("Web page opened in browser");
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
	public void ShowConfigDialog()
	{
		ConfigDialog cd = new ConfigDialog();
		//cd.setPreferredSize(new Dimension(100, 100)); // set in ConfigDialog code
		cd.txtServerport.setText(Integer.toString(serverPort));
		cd.chkAutostart.setSelected(autostart);
		
	   int result = JOptionPane.showConfirmDialog(editor, cd, "API Web Server Config" ,JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			
		if (result == JOptionPane.OK_OPTION) {
			serverPort = Integer.parseInt(cd.txtServerport.getText());
			autostart = cd.chkAutostart.isSelected();
			debugPrint = cd.chkDebugMode.isSelected();
			System.out.println(serverPort + " " + autostart);
			SaveSettings();
		} else {
			System.out.println("Cancelled");
		}
	}
	public int GetMenuItemIndex(JMenu menu, String name)
	{
		//System.out.println("try get menu: " + name);
		for ( int i = 0; i < menu.getItemCount(); i++)
		{
			//System.out.println("try get menu item @ " + i);
			JMenuItem item = menu.getItem(i);
			if (item == null) continue; // happens on seperators
			if (item.getText() == name)
				return i;
		}
		return -1;
	}
	public String GetArduinoRootDir()
	{
	  try{
	    File file = BaseNoGui.getToolsFolder();//new File(API_WebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	    return file.getParentFile().getAbsolutePath();//.getParentFile().getParentFile().getParent();
	    }catch (Exception e) {
	    e.printStackTrace();
	      return "";
	    }
	}
	public String GetJarFileDir()
	{
	  try{
	    File file = new File(API_WebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	    return file.getParent();
	    }catch (Exception e) {
	    e.printStackTrace();
	      return "";
	    }
	}
	
	public void LoadDefaultSettings()
	{
		serverPort = DefaultServerPort;
		autostart = DefaultAutoStart;
		System.out.println("Default Settings Used, serverPort=" + serverPort + ", autostart=" + autostart);
	}
	
	public File GetSettingsFile()
	{
		//File file = new File("tools/API_WebServer/tool/settings.json"); // works on windows
		//if (file.exists()) return file;
		File file = new File(GetJarFileDir() + "/settings.json"); // works on linux and windows
		if (file.exists()) return file;
		System.out.println("setting file not found!");
		return null;
	}
	
	public void LoadSettings()
	{
		File file = GetSettingsFile();
		if (file == null) { LoadDefaultSettings(); return;}
		
		String content = "";
		try { content = new Scanner(file).useDelimiter("\\Z").next(); } 
		catch (Exception e) {e.printStackTrace(); LoadDefaultSettings(); return; }
		JSONObject jsonObj = new JSONObject(content);
			
		try {serverPort = jsonObj.getInt("serverPort");} 
		catch (Exception e) { e.printStackTrace(); serverPort = DefaultServerPort; System.out.println("Default used for serverPort=" + serverPort);}
		
		try {autostart = jsonObj.getBoolean("autostart");}
		catch (Exception e) { e.printStackTrace(); autostart = DefaultAutoStart; System.out.println("Default used for autostart=" + autostart);}
	}
	
	public void SaveSettings()
	{
		try {
            // Constructs a FileWriter given a file name, using the platform's default charset
            FileWriter file = new FileWriter(GetJarFileDir() + "/settings.json");
            StringWriter stringWriter = new StringWriter();
			JSONWriter writer = new JSONWriter(stringWriter);
			writer.object().key("serverPort").value(serverPort).key("autostart").value(autostart).endObject();

			System.out.println(stringWriter.getBuffer().toString());
			file.write(stringWriter.getBuffer().toString());

			file.close();
        } catch (IOException e) {
            e.printStackTrace();
 
        }
	}
	
	private void startWebServer()
	{
		if (server != null)
			try { server.stop(1); } catch (Exception e) {System.err.println(e + " @ " + e.getStackTrace() + e.getStackTrace()[0].getLineNumber());}
	  try {
		server = HttpServer.create(new InetSocketAddress("localhost", serverPort), 0);
		server.createContext("/", new  MyHttpHandler(this));
		server.setExecutor(null);
		server.start();

		System.out.println(" Server started on port " + serverPort);
	  } catch (Exception e) {
		e.printStackTrace();
	  }
	}
	
	public String getFile(String name)
	{
		File file = new File(sketch.getFolder(), name);
		boolean exists = file.exists();
		if (exists)
		{
			
			try {
				String content = new Scanner(file).useDelimiter("\\Z").next();
				return content;
			} catch (Exception e) {
				e.printStackTrace();
				return "";
			}
		}
		else
		{
			System.out.println(name + " file not found!");
			return "";
		}
	}
	public void setFile(String name, String contents)
	{
		try {
            // Constructs a FileWriter given a file name, using the platform's default charset
            FileWriter file = new FileWriter(sketch.getFolder() + "/" + name);
			file.write(contents);
			file.close();
        } catch (IOException e) {
            e.printStackTrace();
 
        }
	}
	
	public void RemoveFilesNotInJSON(JSONArray arr)
	{
		System.out.println("RemoveFilesNotInJSON");
		ArrayList<String> filesToRemove = new ArrayList<String>();
		
		// this removes files in the sketch that is not present in the 
		// JSONArray. To not interfere with the current sketch.getCodeCount()
		// it stores filenames to be removed in a temporary Array
		for (int i = 0; i < sketch.getCodeCount(); i++)
		{
			SketchFile sf = sketch.getFile(i);
			if (sf.isPrimary()) continue; // never remove primary sketch ino file
			
			String fileName = sf.getFileName();
			if (!CheckIfFileExistsInJsonArray(fileName, arr))
				filesToRemove.add(fileName); // store it for later
		}
		// now it can remove files 
		for (int i = 0; i < filesToRemove.size(); i++)
		{
			String fileName = filesToRemove.get(i);
			System.out.println("Removing file:" + fileName);
			removeFile(fileName);
		}
	}
	private boolean CheckIfFileExistsInJsonArray(String fileName, JSONArray arr)
	{
		//System.out.println("CheckIfFileExistsInJsonArray:" + fileName);
		for (int i = 0; i < arr.length(); i++)
		{
			JSONObject e = arr.getJSONObject(i);
			String name = e.getString("name");
			//System.out.println("against: " + name);
			if (name.equals(fileName))
				return true;
		}
		return false;
	}
	
	public void editor_addTab(SketchFile sketchFile, String contents)
	{
		try {
		Method m = Editor.class.getDeclaredMethod("addTab", SketchFile.class, String.class);
		m.setAccessible(true);
		m.invoke(editor, sketchFile, contents);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke editor_addTab");
			e.printStackTrace();
		}
	}
	public void sketch_removeFile(SketchFile sketchFile)
	{
		try {
		Method m = Sketch.class.getDeclaredMethod("removeFile", SketchFile.class);
		m.setAccessible(true);
		m.invoke(sketch, sketchFile);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke sketch_removeFile");
			e.printStackTrace();
		}
	}
	public void editor_removeTab(SketchFile sketchFile)
	{
		try {
		Method m = Editor.class.getDeclaredMethod("removeTab", SketchFile.class);
		m.setAccessible(true);
		m.invoke(editor, sketchFile);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke editor_removeTab");
			e.printStackTrace();
		}
	}
	public boolean sketchFile_delete(SketchFile sketchFile)
	{
		try {
		Method m = SketchFile.class.getDeclaredMethod("delete", Path.class);
		m.setAccessible(true);
		return (boolean)m.invoke(sketchFile, sketch.getBuildPath().toPath());
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke sketchFile_delete");
			e.printStackTrace();
			return false;
		}
	}
	public boolean sketchFile_fileExists(SketchFile sketchFile)
	{
		try {
		Method m = SketchFile.class.getDeclaredMethod("fileExists");
		m.setAccessible(true);
		return (boolean)m.invoke(sketchFile);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke sketchFile_fileExists");
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean addNewFile(String fileName, String contents) // for the API
	{
		File folder;
		try 
		{
			folder = sketch.getFolder();
		}
		catch (Exception e)
		{
			System.err.println(e);
			return false;
		}
		//System.out.println("folder: " + folder.toString());
		File newFile = new File(folder, fileName);
		int fileIndex = sketch.findFileIndex(newFile);
		if (fileIndex >= 0) { // file allready exist, just change the contents.
		  tabs.get(fileIndex).setText(contents);
		  System.out.println("file allready exists " + fileName);
		  return true;
		}
		SketchFile sketchFile;
		try {
		  sketchFile = sketch.addFile(fileName);
		} catch (IOException e) {
		  // This does not pass on e, to prevent showing a backtrace for
		  // "normal" errors.
		  e.printStackTrace();
		  
		  return false;
		}
		editor_addTab(sketchFile, contents);
		System.out.println("added new file " + fileName);
		editor.selectTab(editor.findTabIndex(sketchFile));
		
		return true;
	}
	public boolean removeFile(String fileName) // for the API, so that files could be removed
	{
		File newFile = new File(sketch.getFolder(), fileName);
		int fileIndex = sketch.findFileIndex(newFile);
		if (fileIndex >= 0) { // file exist
		    SketchFile sketchFile = sketch.getFile(fileIndex);
			boolean neverSavedTab = !sketchFile_fileExists(sketchFile);
			
			if (!sketchFile_delete(sketchFile) && !neverSavedTab) {
				System.err.println("Couldn't remove the file " + fileName);
				return false;
			}
			if (neverSavedTab) {
				// remove the file from the sketch list
				sketch_removeFile(sketchFile);
			}
			editor_removeTab(sketchFile);

			// just set current tab to the main tab
			editor.selectTab(0);

			// update the tabs
			header.repaint();
			return true;
		}
		System.err.println("file don't exists in sketch " + fileName);
		return false;
	}
	public boolean renameFile(String oldFileName, String newFileName) // for the API, so that it can rename files
	{
		File newFile = new File(sketch.getFolder(), oldFileName);
		int fileIndex = sketch.findFileIndex(newFile);
		if (fileIndex >= 0) { // file exist
		  SketchFile sketchFile = sketch.getFile(fileIndex);
		  try {
			sketchFile.renameTo(newFileName);
			// update the tabs
			header.rebuild();
			return true;
		  } catch (IOException e) {
			e.printStackTrace();
		  }
		}
		return false;
	}
	
	public void verifyCompile() 
	{
		editor.setAlwaysOnTop(false);
		editor.setAlwaysOnTop(true);
		editor.setAlwaysOnTop(false);
		editor.handleRun(false, presentHandler, runHandler);
	}
	public void upload()
	{
		editor.setAlwaysOnTop(false);
		editor.setAlwaysOnTop(true);
		editor.setAlwaysOnTop(false);
		editor.handleExport(false);
	}
	public String parseGET(Map<String, String> query)
	{
		String cmd = query.get("cmd");
		//if (!cmd.equals("ping"))
		//	System.out.println("GET request params: " + cmd);
		if (cmd.equals("ping"))
		{
			// do nothing, a OK is default to send back
		}
		else if (cmd.equals("compile"))
		{
			verifyCompile();
			System.out.println("WSAPI compile");
		}
		else if (cmd.equals("upload"))
		{   
			upload();
			System.out.println("WSAPI upload");
		}
		else if (cmd.equals("renameFile"))
		{
			String from = query.get("from");
			if (from == null) { System.out.println("Missing 'from' parameter @ renameFile"); return "Missing 'from' parameter @ renameFile"; }
			String to = query.get("to");
			if (to == null) { System.out.println("Missing 'to' parameter @ renameFile"); return "Missing 'to' parameter @ renameFile"; }
			System.out.println("WSAPI renameFile from:" + from + ", to:" + to);
			renameFile(from, to);
		}
		else if (cmd.equals("removeFile"))
		{
			String name = query.get("fileName");
			if (name == null) { System.out.println("Missing 'fileName' parameter @ removeFile"); return "Missing 'fileName' parameter @ removeFile"; }
			System.out.println("WSAPI removeFile:" + name);
			removeFile(name);
		}
		else if(cmd.equals("getFile"))
		{
			String name = query.get("fileName");
			if (name == null) { System.out.println("Missing 'fileName' parameter @ getFile"); return "Missing 'fileName' parameter @ getFile"; }
			System.out.println("WSAPI getFile:" + name);
			return getFile(name);
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
		
		if (removeOtherFiles) {
			try{RemoveFilesNotInJSON(arrFiles);}
			catch (Exception e) {e.printStackTrace();}
		}
		if (arrFiles != null)
		{
			returnStr += parsePOST_JSONfiles(arrFiles);
		}
		if (arrKeywords != null)
		{
			returnStr += parsePOST_JSONkeywords(arrKeywords);
		}
		
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
				addNewFile(name, contents); // adds a new file to the sketch-project
			else
				setFile(name, contents); // this writes a file without the IDE knowing it
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
			keywordOldToken.put(token, type);
			
		}
		
		pdeKeywords_fillMissingTokenType();
		editor.updateKeywords(pdeKeywords);
		String sbKeywordsContents = sbKeywords.toString();
		if (debugPrint)	System.out.println("setting new keywords:\n" + sbKeywordsContents);

		if (!sbKeywordsContents.equals(""))
			setFile(sketchKeywordsTempFileName, sbKeywordsContents);
		return returnStr;
	}
}
